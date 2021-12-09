/*

A minimal binaural rendering server.

- based on SC-HOA
- individual OSC listeners for spherical coordinates
- combined aed OSC listener

- periodically sends aed states to OSC port 9494 on localhost

Henrik von Coler
2020-09-19

*/

~server_ADDRESS = 58010;

~input_OSC      = 8989;

// number of buses to the spatial modules
~nSpatialInputs   = 16;

~nDirectInputs    = 4;

// HOA Order
~hoa_order = 3;

postln(thisProcess.argv[0]);

if(size(thisProcess.argv)==1,
	{
		~nSpatialInputs   = thisProcess.argv[0].asInteger;
});
postln("Launching with "++~nSpatialInputs++" inputs!");

if(size(thisProcess.argv)==2,
	{
		~server_ADDRESS   = thisProcess.argv[1].asInteger;
});
postln("Launching with port:"++~server_ADDRESS++"!");


//Server.supernova;

Server.default = Server(\binaural_server, NetAddr("127.0.0.1", ~server_ADDRESS));

s.options.device               = "SC_BINAURAL";
s.options.numInputBusChannels  = ~nSpatialInputs;
s.options.numOutputBusChannels = 32;
s.options.maxLogins            = 32;
s.options.memSize              = 65536;
s.options.numBuffers           = 4096;

// get script's directory for relative paths
~root_DIR = thisProcess.nowExecutingPath.dirname++"/";

s.boot;

~spatial_OSC  = NetAddr("127.0.0.1", 9595);

~n_hoa_channels = (pow(~hoa_order + 1.0 ,2.0)).asInteger;

s.waitForBoot({

	HOABinaural.loadbinauralIRs(s);
	HOABinaural.loadHeadphoneCorrections(s);
	HOABinaural.binauralIRs;
	HOABinaural.headPhoneIRs;

	s.sync;

	~synthdef_DIR = PathName(~root_DIR++"synthdefs/");

	~synthdef_DIR.filesDo
	{
		|tmpFile|
		var tmp_path = tmpFile.pathOnly.asSymbol++tmpFile.fileName.asSymbol;
		tmp_path.load;
		postln(tmp_path);
	};
	s.sync;

	/////////////////////////////////////////////////////////////////
	// THE BUSSES:
	/////////////////////////////////////////////////////////////////

	// create a X times Y routing
	// matrix by using an array of multichannel
	// control busses:
	~gain_BUS_spatial = Array.fill(~nSpatialInputs,
		{
			// arg i;
			// "Creating control busses for system: ".post;
			// i.postln;
			Bus.control(s, ~nSpatialInputs);
		}
	);
	s.sync;

	~gain_BUS_direct = Array.fill(~nDirectInputs,
		{
			// arg i;
			// "Creating control busses for system: ".post;
			// i.postln;
			Bus.control(s, ~nDirectInputs);
		}
	);
	s.sync;

	// buses for direct control
	~control_azim_BUS = Bus.control(s,~nSpatialInputs);
	~control_azim_BUS.setAll(0);
	~control_elev_BUS = Bus.control(s,~nSpatialInputs);
	~control_elev_BUS.setAll(0);
	~control_dist_BUS = Bus.control(s,~nSpatialInputs);
	~control_dist_BUS.setAll(1);

	// buses for LFO control
	~lfo_azim_BUS = Bus.control(s,~nSpatialInputs);
	~lfo_azim_BUS.setAll(0);
	~lfo_elev_BUS = Bus.control(s,~nSpatialInputs);
	~lfo_elev_BUS.setAll(0);
	~lfo_dist_BUS = Bus.control(s,~nSpatialInputs);
	~lfo_dist_BUS.setAll(1);

	// only for monitoring
	~monitor_azim_BUS = Bus.control(s,~nSpatialInputs);
	~monitor_elev_BUS = Bus.control(s,~nSpatialInputs);
	~monitor_dist_BUS = Bus.control(s,~nSpatialInputs);

	//
	~azim_MAPPER = Array.fill(~nSpatialInputs,{Synth.new(\mapper)});
	~elev_MAPPER = Array.fill(~nSpatialInputs,{Synth.new(\mapper)});
	~dist_MAPPER = Array.fill(~nSpatialInputs,{Synth.new(\mapper)});

	s.sync;

	~azim_MAPPER.do({arg e,i;
		e.set(\offset,  0, \gain, 1);
		e.set(\inbus,  ~control_azim_BUS.index + i);
		e.set(\outbus, ~monitor_azim_BUS.index + i);
	});

	~elev_MAPPER.do({arg e,i;
		e.set(\offset,  0, \gain, 1);
		e.set(\inbus,  ~control_elev_BUS.index + i);
		e.set(\outbus, ~monitor_elev_BUS.index + i);
	});

	~dist_MAPPER.do({arg e,i;
		e.set(\offset,  0, \gain, 1);
		e.set(\inbus,  ~control_dist_BUS.index + i);
		e.set(\outbus, ~monitor_dist_BUS.index + i);
	});

	// create a bus for each spatialization module:
	~audio_BUS_spatial = Bus.audio(s, ~nSpatialInputs);
		~audio_BUS_direct = Bus.audio(s, ~nDirectInputs);

	// bus for encoded 5th order HOA
	~ambi_BUS = Bus.audio(s, ~n_hoa_channels);

	/////////////////////////////////////////////////////////////////
	// INPUT SECTION

	~input_GROUP = Group.new;
	s.sync;


	for (0, ~nDirectInputs -1, {arg idx;

		post('Adding direct input module: ');
		idx.postln;

		~direct_inputs = ~direct_inputs.add(
			Synth(\input_module_mono,
				[
					\input_bus,  idx,
					\output_bus,          ~audio_BUS_direct.index,
					\control_BUS_spatial, ~gain_BUS_direct[idx].index,
				],
				target: ~input_GROUP
		);)
	});
	s.sync;

	for (0, ~nSpatialInputs -1, {arg idx;

		post('Adding spatial input module: ');
		idx.postln;

		~spatial_inputs = ~spatial_inputs.add(
			Synth(\input_module_mono,
				[
					\input_bus, idx+~nDirectInputs,
					\output_bus,          ~audio_BUS_spatial.index,
					\control_BUS_spatial, ~gain_BUS_spatial[idx].index,
				],
				target: ~input_GROUP
		);)
	});
	s.sync;

	/////////////////////////////////////////////////////////////////
	// MODULATOR SECTION
	/////////////////////////////////////////////////////////////////

	~mod_GROUP = Group.after(~input_GROUP);

	for (0, (~nSpatialInputs/2) -1, {arg idx;

		post('Adding LFO Module: ');
		idx.postln;

		~lfo = ~lfo.add(
			Synth.new(\double_lfo,
				[
					\trig, 0,
					\rate, 1,
					\dur,  1,
					\out_bus1,  ~lfo_azim_BUS.index + (idx*2),
					\out_bus2,  ~lfo_azim_BUS.index + (idx*2) + 1
				],
				target: ~mod_GROUP);
		);
	});

	s.sync;






	// OSC listener for LFO trigger events
	OSCdef('/lfo/trigger',
		{
			arg msg, time, addr, recvPort;

			var ind, val;

			ind = msg[1];
			val = msg[2];

			~lfo[ind].set(\trig, val, \run, val);

			// postln("Trigger "+ind);

		},'/lfo/trigger'
	);

	OSCdef('/lfo/gain',
		{
			arg msg, time, addr, recvPort;
			var ind, val;

			ind = msg[1];
			val = msg[2];

			~lfo[ind].set(\gain, val);

		},'/lfo/gain'
	);


	OSCdef('/lfo/dur',
		{
			arg msg, time, addr, recvPort;
			var ind, val;

			ind = msg[1];
			val = msg[2];

			~lfo[ind].set(\dur, val);

		},'/lfo/dur'
	);


	OSCdef('/lfo/dir',
		{
			arg msg, time, addr, recvPort;
			var ind, val;

			ind = msg[1];
			val = msg[2];

			~lfo[ind].set(\dir, val);

		},'/lfo/dir'
	);


	OSCdef('/lfo/offset',
		{
			arg msg, time, addr, recvPort;
			var ind, val;

			ind = msg[1];
			val = msg[2];

			~lfo[ind].set(\offset, val);

		},'/lfo/offset'
	);




	/////////////////////////////////////////////////////////////////
	// SPATIAL SECTION
	/////////////////////////////////////////////////////////////////

	~spatial_GROUP = Group.after(~input_GROUP);
	s.sync;

	/////////////////////////////////////////////////////////////////
	// ambisonics

	for (0, ~nSpatialInputs -1, {arg cnt;

		post('Adding HOA panning Module: ');
		cnt.postln;

		~binaural_panners = ~binaural_panners.add(
			Synth(\binaural_mono_encoder_3,
				[
					\in_bus,  ~audio_BUS_spatial.index+cnt,
					\out_bus, ~ambi_BUS.index
				],
				target: ~spatial_GROUP
		);)
	});
 	s.sync;

	// ~binaural_panners.do({arg e,i; e.set(\out_bus,~ambi_BUS.index)});

	for (0, ~nSpatialInputs -1, {arg cnt;

		post('Mapping HOA module: ');
		cnt.postln;

		~binaural_panners[cnt].map(\azim, ~control_azim_BUS.index  + cnt);
		~binaural_panners[cnt].map(\elev, ~control_elev_BUS.index  + cnt);
		~binaural_panners[cnt].map(\dist, ~control_dist_BUS.index  + cnt);

	});

	//
	OSCdef('/encoder/mode',
		{
			arg msg, time, addr, recvPort;
			var mode = msg[1];

			switch(mode,
				'direct',
				{
					postln("Setting all encoders to direct control!");
					~binaural_panners.do(
						{arg e,i;
							e.map(\azim,  ~control_azim_BUS.index + (i));


						}
					);

					~azim_MAPPER.do({arg e,i;
						e.set(\inbus,  ~control_azim_BUS.index + i);
					});
				},
				'lfo',
				{
					postln("Setting all encoders to LFO!");
					~binaural_panners.do(
						{arg e,i;
							e.map(\azim,  ~lfo_azim_BUS.index + i);



						}
					);

					~azim_MAPPER.do({arg e,i;
						e.set(\inbus,  ~lfo_azim_BUS.index + i);
					});
				}
			);
		},'/encoder/mode'
	);


	/////////////////////////////////////////////////////////////////
	// decoder
	/////////////////////////////////////////////////////////////////

	~output_GROUP	 = Group.after(~spatial_GROUP);
	s.sync;

	~direct_output1 = {Out.ar(0 ,Mix.ar(SoundIn.ar([0,1,2,3]),1))}.play;
	~direct_output2 = {Out.ar(1 ,Mix.ar(SoundIn.ar([0,1,2,3]),1))}.play;
	~direct_output3 = {Out.ar(2 ,Mix.ar(SoundIn.ar([0,1,2,3]),1))}.play;
	~direct_output4 = {Out.ar(3 ,Mix.ar(SoundIn.ar([0,1,2,3]),1))}.play;

	~hoa_output = {Out.ar(~nDirectInputs ,In.ar(~ambi_BUS.index,~n_hoa_channels))}.play;
	s.sync;


	~direct_output1.moveToTail(~output_GROUP);
	~direct_output2.moveToTail(~output_GROUP);
	~direct_output3.moveToTail(~output_GROUP);
	~direct_output4.moveToTail(~output_GROUP);

	~hoa_output.moveToTail(~output_GROUP);


	~decoder = Synth(\hoa_binaural_decoder_3,
		[
			\in_bus, ~ambi_BUS.index,
			\out_bus, ~nDirectInputs+~n_hoa_channels
		],
		target: ~output_GROUP);

	s.sync;

	~decoder.set(\in_bus,~ambi_BUS);
	~decoder.set(\out_bus,~nDirectInputs+~n_hoa_channels);

	/////////////////////////////////////////////////////////////////
	// OSC listeners:
	/////////////////////////////////////////////////////////////////


	~azim_OSC = OSCFunc(
		{
			arg msg, time, addr, recvPort;
			var azim = msg[2];
			~control_azim_BUS.setAt(msg[1],azim);

	}, '/source/azim');

	~elev_OSC = OSCFunc(
		{
			arg msg, time, addr, recvPort;
			var elev = msg[2];
			~control_elev_BUS.setAt(msg[1],elev);

	}, '/source/elev');

	~dist_OSC = OSCFunc(
		{
			arg msg, time, addr, recvPort;
			var dist = msg[2];
			~control_dist_BUS.setAt(msg[1],dist);

	}, '/source/dist');


	OSCdef('/source/aed',
		{
			arg msg, time, addr, recvPort;

			var azim = msg[2];
			var elev = msg[3];
			var dist = msg[4];

			~control_azim_BUS.setAt(msg[1],azim);
			~control_elev_BUS.setAt(msg[1],elev);
			~control_dist_BUS.setAt(msg[1],dist);

	},'/source/aed');


	~send_OSC_ROUTINE = Routine({

		inf.do({

			var azim, elev, dist;

			for (0, ~nSpatialInputs-1, {

				arg i;

				azim = ~monitor_azim_BUS.getnSynchronous(~nSpatialInputs)[i];
				elev = ~monitor_elev_BUS.getnSynchronous(~nSpatialInputs)[i];
				dist = ~monitor_dist_BUS.getnSynchronous(~nSpatialInputs)[i];

				~spatial_OSC.sendMsg('/source/aed', i, azim, elev, dist);

			});

			0.01.wait;
		});

	});

	~send_OSC_ROUTINE.play;

	/////////////////////////////////////////////////////////////////
	//
	/////////////////////////////////////////////////////////////////

	thisProcess.openUDPPort(~input_OSC);

	post("Listening on port: ");
	postln(thisProcess.openPorts);
	ServerMeter(s);



	// ~gain_BUS_direct.do({arg e,i; e.setAt(i,0)});
	~gain_BUS_spatial.do({arg e,i; e.setAt(i,1)});

});
