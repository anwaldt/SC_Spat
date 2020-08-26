/*

sprawl_SERVER.sc

Simple version of an OSC-controllable
audio routing matrix.


Henrik von Coler
2019-11-19

*/

Server.supernova;

s = Server(\beatspat_server, NetAddr("127.0.0.1", 58009));

// s.options.device = "BEATSPAT_server";

s.options.numInputBusChannels  = 32;
s.options.numOutputBusChannels = 16;
s.options.maxLogins            = 8;
s.options.memSize              = 65536;

s.boot;

// number of buses to the spatial modules
~nStereo   = 16;


~nVbap     = 6;
~nHoa      = 6;
~nShifters = 6;


~routing_OSC  = NetAddr("127.0.0.1", 57121);
~spatial_OSC  = NetAddr("127.0.0.1", 9494);


// only for VBAP
~nSpeakers = 8;


MIDIClient.init(1,1);
MIDIIn.connectAll;




/////////////////////////////////////////////////////////////////
// THE SYNTHDEFS:
/////////////////////////////////////////////////////////////////

SynthDef(\double_lfo,
	{
		|
		run      = 0,
		dur      = 1,
		trig     = 0,
		dir      = pi,
		gain     = 1,
		offset   = 0,
		out_bus1 = nil,
		out_bus2 = nil
		|

		var ramp, x;

		ramp = Sweep.kr(trig, run / dur).linlin(0, 1, 0, dir, \minmax);

		x = (gain * sin(ramp)) + offset + 0.5;


		// var x = Sweep.kr(trig, rate);

		Out.kr(out_bus1,   x);
		Out.kr(out_bus2,   x * (-1));


}).add;



SynthDef(\input_module,
	{
		|
		input_bus           = nil,
		control_BUS_spatial = nil,
		output_bus          = nil
		|

		var in1, in2, output, gain;

		for (0, ~nStereo-1,
			{ arg cnt;

				// get the gain value from control bus:
				gain = In.kr(control_BUS_spatial + cnt);

				// get the audio input from hardware input:
				in1 = SoundIn.ar(input_bus);
				in2 = SoundIn.ar(input_bus+1);

				// audio output to dedicated bus
				Out.ar(output_bus + (cnt*2), [in1*gain, in2*gain]);

			}
		);

}).add;


// adapted to stereo from vbap_test.scd
SynthDef( \kernel_shifter_stereo,
	{
		|
		in_bus1  = 0,
		in_bus2  = 1,
		out_bus  = 0,
		mod_1    = 0
		|


		for(0, ~nSpeakers -1, {arg idx;

			Out.ar(out_bus +idx,   In.ar(in_bus1) * sin((idx * mod_1) + idx));

		});

}).add;



// adapted to stereo from vbap_test.scd
SynthDef( \vbap_panner_stereo,
	{
		|
		in_bus1  = 0,
		in_bus2  = 1,
		out_bus  = 0,
		out_buf  = 0,
		azim_1   = -0.125, elev_1 = 0, sprd_1 = 0,
		azim_2   = 0.125, elev_2 = 0, sprd_2 = 0
		|

		// scaling
		azim_1 = (azim_1 * 360) - 180;
		azim_2 = (azim_2 * 360) - 180;

		sprd_1 = sprd_1 * 100;
		sprd_2 = sprd_2 * 100;

		Out.ar(out_bus, VBAP.ar(8, In.ar(in_bus1), out_buf, azim_1, elev_1, sprd_1));
		Out.ar(out_bus, VBAP.ar(8, In.ar(in_bus2), out_buf, azim_2, elev_2, sprd_2));

}).add;





SynthDef(\hoa_stereo_encoder, {
	|
	in_bus1  = nil,
	in_bus2  = nil,
	out_bus  = 0,
	azim_1    = -45, elev_1 = 0,
	azim_2    =  45, elev_2 = 0
	|

	var sound1 = In.ar(in_bus1)*0.7;
	var sound2 = In.ar(in_bus2)*0.7;

	azim_1 = (azim_1 * (2 * pi)) -pi;
	azim_2 = (azim_2 * (2 * pi)) -pi;

	//elev1 = MouseY.kr(0,3.14);
	//elev2 = MouseY.kr(0,3.14);

	Out.ar(out_bus, HOAEncoder.ar(3, sound1,azim_1, elev_1));
	Out.ar(out_bus, HOAEncoder.ar(3, sound2,azim_2, elev_2));


}).add;


SynthDef(\hoa_octa_decoder, {

	|in_bus = 0|


	Out.ar(0,Holzmarkt20203.ar(
		In.ar(in_bus ),
		In.ar(in_bus +1),
		In.ar(in_bus +2),
		In.ar(in_bus +3),
		In.ar(in_bus +4),
		In.ar(in_bus +5),
		In.ar(in_bus +6),
		In.ar(in_bus +7),
		In.ar(in_bus +8),
		In.ar(in_bus +9),
		In.ar(in_bus +10),
		In.ar(in_bus +11),
		In.ar(in_bus +12),
		In.ar(in_bus +13),
		In.ar(in_bus +14),
		In.ar(in_bus +15),
		gain:1) );

}).add;






// taken from vbap_test.scd
SynthDef( \speaker,
	{
		|
		in_bus  = nil,
		out_bus = nil,
		gain    = 1,
		delay   = 0
		|

		var input = In.ar(in_bus);

		Out.ar(out_bus, gain  * DelayL.ar(input, 1, delay));

}).add;




SynthDef(\output_module,
	{
		|
		audio_bus = nil,
		out_bus   = nil
		|

		Out.ar(out_bus, In.ar(audio_bus,2));

}).add;





s.waitForBoot({



	/////////////////////////////////////////////////////////////////
	// THE BUSSES:
	/////////////////////////////////////////////////////////////////



	~control_harmonic_mod_BUS = Bus.control(s,~nShifters);

	~control_vbap_azim_BUS    = Bus.control(s,~nVbap*2);
	~control_vbap_spre_BUS    = Bus.control(s,~nVbap*2);




	~control_hoa_azim_BUS     = Bus.control(s,~nHoa*2);

	~audio_BUS_vbap           = Bus.audio(s, ~nSpeakers);

	~audio_BUS_vbap_speaker   = Bus.audio(s, ~nSpeakers);



	// create a X times Y routing
	// matrix by using an array ofmultichannel
	// control busses:
	~gain_BUS_spatial = Array.fill(~nStereo,
		{
			// arg i;
			// "Creating control busses for system: ".post;
			// i.postln;
			Bus.control(s, ~nStereo);
		}
	);


	// create 2 audio buses for each spatialization module:
	~audio_BUS_spatial      = Bus.audio(s, (~nStereo * 2));


	// bus for encoded 3rd order HOA
    ~ambi_BUS = Bus.audio(s,16);



	/////////////////////////////////////////////////////////////////
	// MOD SECTION
	/////////////////////////////////////////////////////////////////

	~mod_GROUP = Group.head(s);

	for (0, ~nVbap -1, {arg idx;

		post('Adding LFO Module: ');
		idx.postln;


		~lfo = ~lfo.add(
			Synth.new(\double_lfo,
				[
					\trig, 0,
					\rate, 1,
					\dur,  1,
//					\out_bus1,  ~control_vbap_azim_BUS.index + (idx*2),
//					\out_bus2,  ~control_vbap_azim_BUS.index + (idx*2) + 1
				],
				target: ~mod_GROUP);

		);
	});




	// this MIDI function triggers


	~trig_MIDI = MIDIFunc.noteOn({|velocity, midipitch, channel|

		"Trigger: on ".post;
		midipitch.post;
		" - ".post;
		velocity.postln;

		~lfo[midipitch].set(\trig, 1);
		~lfo[midipitch].set(\run, 1);


	});


	~trig_MIDI2 = MIDIFunc.noteOff({|velocity, midipitch, channel|

		"Trigger: off ".post;
		midipitch.post;
		" - ".post;
		velocity.postln;

		~lfo[midipitch].set(\trig, 0);
		~lfo[midipitch].set(\run, 0);

	});


	/////////////////////////////////////////////////////////////////
	// INPUT SECTION

	~input_GROUP = Group.after(~mod_GROUP);

	for (0, ~nStereo -1, {arg idx;

		post('Adding Input Module: ');
		idx.postln;

		~inputs = ~inputs.add(
			Synth(\input_module,
				[
					\input_bus, idx*2,
					\output_bus,          ~audio_BUS_spatial.index,
					\control_BUS_spatial, ~gain_BUS_spatial[idx].index,
				],
				target: ~input_GROUP
		);)
	});


	/////////////////////////////////////////////////////////////////
	// SPATIAL SECTION
	/////////////////////////////////////////////////////////////////

	~spatial_GROUP = ParGroup.after(~input_GROUP);

	/////////////////////////////////////////////////////////////////
	// VBAP


	~speakers      = VBAPSpeakerArray.new(2, [-45, 0, 45, 90, 135, 180, -135, -90 ]); // 8 channel ring
	~vbap_BUFF     = ~speakers.loadToBuffer(s);

	2.wait;

	for (0, ~nVbap -1, {arg cnt;

		post('Adding VBAP Module: ');
		cnt.postln;


		~vbap_panners = ~vbap_panners.add(
			Synth(\vbap_panner_stereo,
				[
					\in_bus1, ~audio_BUS_spatial.index + (2*cnt),
					\in_bus2, ~audio_BUS_spatial.index + (2*cnt) +1,
					\out_bus, ~audio_BUS_vbap.index,
					\out_buf, ~vbap_BUFF.bufnum
				],
				target: ~spatial_GROUP
		);)
	});

	// map it:

	for (0, ~nVbap -1, {arg cnt;


		post('Mapping VBAP module: ');
		cnt.postln;

		~vbap_panners[cnt].map(\azim_1, ~control_vbap_azim_BUS.index  + (2*cnt));
		~vbap_panners[cnt].map(\azim_2, ~control_vbap_azim_BUS.index  + (2*cnt) +1);
		~vbap_panners[cnt].map(\sprd_1, ~control_vbap_spre_BUS.index  + (2*cnt));
		~vbap_panners[cnt].map(\sprd_1, ~control_vbap_spre_BUS.index  + (2*cnt) +1);

	});


	/////////////////////////////////////////////////////////////////
	// ambisonics




	for (0, ~nHoa -1, {arg cnt;

		post('Adding HOA panning Module: ');
		cnt.postln;


		~hoa_panners = ~hoa_panners.add(
			Synth(\hoa_stereo_encoder,
				[
					\in_bus1, ~audio_BUS_spatial.index + (2* ~nVbap) +  (2*cnt),
				    \in_bus2, ~audio_BUS_spatial.index + (2* ~nVbap) +  (2*cnt) +1,
					\out_bus, ~ambi_BUS.index
				],
				target: ~spatial_GROUP
		);)
	});




	for (0, ~nHoa -1, {arg cnt;


		post('Mapping HOA module: ');
		cnt.postln;

		~hoa_panners[cnt].map(\azim_1, ~control_hoa_azim_BUS.index  + (2*cnt));
		~hoa_panners[cnt].map(\azim_2, ~control_hoa_azim_BUS.index  + (2*cnt) +1);

	});



	/////////////////////////////////////////////////////////////////
	// kernel shifters


	for (0, ~nShifters -1, {arg cnt;

		post('Adding harmonic panning Module: ');
		cnt.postln;


		~kernel_shifters = ~kernel_shifters.add(
			Synth(\vbap_panner_stereo,
				[
					\in_bus1, ~audio_BUS_spatial.index + (2*(~nVbap + ~nHoa ))  + (2*cnt),
					\in_bus2, ~audio_BUS_spatial.index + (2*(~nVbap + ~nHoa ))  + 1,
					\out_bus, 0
				],
				target: ~spatial_GROUP
		);)
	});



	for (0, ~nShifters -1, {arg cnt;

		post('Mapping harmonic panning module: ');
		cnt.postln;

		~kernel_shifters[cnt].map(\mod_1, ~control_harmonic_mod_BUS.index + cnt );

	});





	/////////////////////////////////////////////////////////////////
	// OUTPUT SECTION
	/////////////////////////////////////////////////////////////////

	~output_GROUP = Group.after(~spatial_GROUP);

	// for vbap
	for (0, ~nSpeakers-1, {arg i;

		post('Adding speaker module: ');
		i.postln;

		~output_speakers = ~output_speakers.add(
			Synth(\speaker,
				[
					\in_bus, ~audio_BUS_vbap.index + i,
					\out_bus, i
				],
				target: ~output_GROUP
		);)
	});



		// decoder
	~decoder = Synth(\hoa_octa_decoder,
	[\in_bus,~ambi_BUS.index ],
	target: ~output_GROUP);





	/////////////////////////////////////////////////////////////////
	// OSC listeners:
	/////////////////////////////////////////////////////////////////




	// the routing function
	~route_spatial_OSC = OSCFunc(

		{arg msg, time, addr, recvPort;

			var r, s;

			s = msg[1];
			r = msg[2];

			// set the bus value:
			~gain_BUS_spatial[s].setAt(r,msg[3]);

	}, '/route/spatial');





	~spat_azim_OSC = OSCFunc(
		{
			arg msg, time, addr, recvPort;

			var azim = msg[2];

			azim = min(max(azim,0),1);

			~control_vbap_azim_BUS.setAt(msg[1],azim);

	}, '/vbap/azim');


	~spat_spre_OSC = OSCFunc(
		{
			arg msg, time, addr, recvPort;

			~control_vbap_spre_BUS.setAt(msg[1],msg[2]);

	}, '/vbap/spre');


	~hoa_azim_OSC = OSCFunc(
		{
			arg msg, time, addr, recvPort;

			~control_hoa_azim_BUS.setAt(msg[1],msg[2]);

	}, '/hoa/azim');





	~routing_OSC_routine = Routine({

		inf.do({

			for (0, ~nStereo-1, { arg i;

				var gains      = ~gain_BUS_spatial[i].getnSynchronous(~nVbap);

				for (0, ~nVbap-1, { arg j;

					~routing_OSC.sendMsg('/route/spatial', i, j, gains[j]);

					~routing_OSC.sendMsg('/route/spatial', i, j, gains[j]);

					//0.001.wait;
				});

			});

			0.01.wait;
		});

	});

	~routing_OSC_routine.play;





	~vbap_OSC_routine = Routine({

		inf.do({

			var azims      = ~control_vbap_azim_BUS.getnSynchronous(~nVbap);
			var spreads    = ~control_vbap_spre_BUS.getnSynchronous(~nVbap);

			for (0, ~nVbap-1, { arg i;

				~spatial_OSC.sendMsg('/vbap/azim', i, azims[i]);
				~spatial_OSC.sendMsg('/vbap/spre', i, spreads[i]);

			});

			0.01.wait;
		});

	});

	~vbap_OSC_routine.play;

	/////////////////////////////////////////////////////////////////
	//
	/////////////////////////////////////////////////////////////////


	post("Listening on port: ");
	postln(NetAddr.langPort);
	ServerMeter(s);

	s.scope(2,2);

});














