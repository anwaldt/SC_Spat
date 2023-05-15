/*

A minimal HOA and binaural rendering server.

- based on SC-HOA
- individual OSC listeners for spherical coordinates
- combined aed OSC listener

- periodically sends aed states to OSC port 9494 on localhost

Henrik von Coler
2020-09-19

*/

~server_ADDRESS = 58010;

~input_OSC      = 7878;

// number of buses to the spatial modules
~nSpatialInputs   = 14;

~n_stereo = (~nSpatialInputs/2).asInteger;

~nDirectInputs    = 2;

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

Server.default = Server(\scspat, NetAddr("127.0.0.1", ~server_ADDRESS));

s.options.device               = "SC_Spat";
s.options.numInputBusChannels  = 32;
s.options.numOutputBusChannels = 32;
s.options.maxLogins            = 2;
s.options.memSize              = 65536;
s.options.numBuffers           = 4096;

// get script's directory for relative paths
~root_DIR = thisProcess.nowExecutingPath.dirname++"/";

s.boot;



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
	~gain_BUS = Array.fill(~nDirectInputs+~nSpatialInputs,
		{
			// arg i;
			// "Creating control busses for system: ".post;
			// i.postln;
			Bus.control(s, ~nDirectInputs+~nSpatialInputs);
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
	~automate_azim_BUS = Bus.control(s,~nSpatialInputs);
	~automate_azim_BUS.setAll(0);
	~automate_elev_BUS = Bus.control(s,~nSpatialInputs);
	~automate_elev_BUS.setAll(0);
	~automate_dist_BUS = Bus.control(s,~nSpatialInputs);
	~automate_dist_BUS.setAll(1);

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
	~audio_send_BUS = Bus.audio(s, ~nDirectInputs+~nSpatialInputs);

	// bus for encoded 5th order HOA
	~ambi_BUS = Bus.audio(s, ~n_hoa_channels);

	/////////////////////////////////////////////////////////////////
	// INPUT SECTION

	~input_GROUP = Group.new;
	s.sync;


	for (0, ~nDirectInputs+~nSpatialInputs -1, {arg idx;

		post('Adding spatial input module: ');
		idx.postln;

		~spatial_inputs = ~spatial_inputs.add(
			Synth(\input_module_mono,
				[
					\input_bus,           idx,
					\output_bus,          ~audio_send_BUS.index,
					\control_BUS_spatial, ~gain_BUS[idx].index,
				],
				target: ~input_GROUP
		);)
	});
	s.sync;


	/////////////////////////////////////////////////////////////////
	// MODULATOR SECTION
	/////////////////////////////////////////////////////////////////

	~mod_GROUP = Group.after(~input_GROUP);
	s.sync;

	for (0, (~n_stereo) -1, {arg idx;

		post('Adding LFO Module: ');
		idx.postln;

		~lfo = ~lfo.add(
			Synth.new(\double_lfo,
				[
					\trig, 0,
					\rate, 1,
					\dur,  1,
					\out_bus1,  ~automate_azim_BUS.index + (idx*2),
					\out_bus2,  ~automate_azim_BUS.index + (idx*2) + 1
				],
				target: ~mod_GROUP);
		);
	});
	s.sync;

	~lfo_duration_BUS  = Bus.control(s, (~n_stereo).asInteger);
	~lfo_direction_BUS = Bus.control(s, (~n_stereo).asInteger);
	~lfo_gain_BUS      = Bus.control(s, (~n_stereo).asInteger);
	~lfo_offset_BUS    = Bus.control(s, (~n_stereo).asInteger);
	s.sync;

	~lfo.do({arg e,i; e.map(\dur,    ~lfo_duration_BUS.index+i)});
	~lfo.do({arg e,i; e.map(\dir,    ~lfo_direction_BUS.index.asInteger +i)});
	~lfo.do({arg e,i; e.map(\gain,   ~lfo_gain_BUS.index.asInteger+i)});
	~lfo.do({arg e,i; e.map(\offset, ~lfo_offset_BUS.index.asInteger+i)});

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

		~hoa_panners = ~hoa_panners.add(
			Synth(\binaural_mono_encoder_3,
				[
					\in_bus,  ~audio_send_BUS.index+~nDirectInputs+cnt,
					\out_bus, ~ambi_BUS.index
				],
				target: ~spatial_GROUP
		);)
	});
	s.sync;

	// ~hoa_panners.do({arg e,i; e.set(\out_bus,~ambi_BUS.index)});

	for (0, ~nSpatialInputs -1, {arg cnt;

		post('Mapping HOA module: ');
		cnt.postln;

		~hoa_panners[cnt].map(\azim, ~monitor_azim_BUS.index  + cnt);
		~hoa_panners[cnt].map(\elev, ~monitor_elev_BUS.index  + cnt);
		~hoa_panners[cnt].map(\dist, ~monitor_dist_BUS.index  + cnt);

	});


	/////////////////////////////////////////////////////////////////
	// decoder
	/////////////////////////////////////////////////////////////////

	~output_GROUP	 = Group.after(~spatial_GROUP);
	s.sync;


	~direct_output1 = {|gain = 1| Out.ar(0 ,Mix.ar(In.ar(~audio_send_BUS.index,4)))}.play;
	~direct_output2 = {|gain = 1| Out.ar(1 ,Mix.ar(In.ar(~audio_send_BUS.index,4)))}.play;
	~direct_output3 = {|gain = 1| Out.ar(2 ,Mix.ar(In.ar(~audio_send_BUS.index,4)))}.play;
	~direct_output4 = {|gain = 1| Out.ar(3 ,Mix.ar(In.ar(~audio_send_BUS.index,4)))}.play;


	~hoa_output = {|gain=1| Out.ar(~nDirectInputs ,gain*In.ar(~ambi_BUS.index,~n_hoa_channels))}.play;
	s.sync;

	~hoa_output.set(\gain,0.5);

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
	~decoder.set(\gain, 1);

	/////////////////////////////////////////////////////////////////
	// Listeners:
	/////////////////////////////////////////////////////////////////

	(~root_DIR++"SCSpat_OSC.scd").load;

	// (~root_DIR++"SCSpat_MIDI.scd").load;

	/////////////////////////////////////////////////////////////////
	//
	/////////////////////////////////////////////////////////////////


	ServerMeter(s);


	/////////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////////

	thisProcess.openUDPPort(~input_OSC);

	post("Listening on port: ");
	postln(thisProcess.openPorts);



	// ~gain_BUS_direct.do({arg e,i; e.setAt(i,0)});
	// ~gain_BUS.do({arg e,i; e.setAll(0)});
	// ~gain_BUS[0].setAt(0,1);

	// set to identity matrix
	~gain_BUS.do({arg e,i; e.setAt(i,1)});

	for(0,~nSpatialInputs-1, {arg i;
	~control_azim_BUS.setAt(i,(i/~nSpatialInputs)*2*pi);
});
});
