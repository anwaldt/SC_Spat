
s.options.numInputBusChannels  = 8;
s.options.numOutputBusChannels = 8;
s.options.numAudioBusChannels  = 4096;

s.options.memSize = 65536;

// s.boot;

~nChannels = 4;
~nSources  = ~nChannels*2;

// HOA Order
~hoa_order       = 3;
~n_hoa_channnels = pow(~hoa_order + 1.0 ,2.0);

// get script's directory for relative paths
~root_DIR  = thisProcess.nowExecutingPath.dirname++"/";


s.boot;

s.waitForBoot(

	{

		5.wait;


		HOABinaural.loadbinauralIRs(s);
		HOABinaural.loadHeadphoneCorrections(s);
		HOABinaural.binauralIRs;
		HOABinaural.headPhoneIRs;


		~synthdef_DIR = PathName(~root_DIR++"synthdefs/");


		~synthdef_DIR.filesDo
		{
			|tmpFile|
			var tmp_path = tmpFile.pathOnly.asSymbol++tmpFile.fileName.asSymbol;
			tmp_path.load;
			postln("Loading:"+tmp_path);
		};
		s.sync;
		//////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////

		~input_GROUP = Group.head(s);
		s.sync;

		//////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////

		~eq_GROUP    = Group.after(~input_GROUP);
		s.sync;

		~eq_modules  = Array.fill(~nChannels,
			{   arg i;
				postln("Adding EQ: "+i);
				Synth.head(~eq_GROUP,\eq_3p);
			}
		);
		s.sync;



		//////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////

		~encoder_GROUP   = Group.after(~eq_GROUP);
		s.sync;

		~hoa_BUS = Array.fill(~nSources,{Bus.audio(s,~n_hoa_channnels)});
		~hoa_pfl_BUS = Bus.audio(s,~n_hoa_channnels);

		~encoders  = Array.fill(~nSources,
			{   arg i;
				postln("Adding Encoder: "+i);
				Synth.head(~encoder_GROUP,\hoa_mono_encoder_3,
					[
						~in_bus, s.options.numOutputBusChannels,
						~out_bus, ~hoa_BUS[i].index
					]
				);
			}
		);
		s.sync;


		~control_azim_BUS     = Bus.control(s,~nSources).setAll(0);
		~control_elev_BUS     = Bus.control(s,~nSources).setAll(0);
		~control_dist_BUS     = Bus.control(s,~nSources).setAll(1);


		~encoders.do({arg e,i;
			e.map(\azim,~control_azim_BUS.index+i);
			e.map(\elev,~control_elev_BUS.index+i);
			e.map(\dist,~control_dist_BUS.index+i);
		});

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


		//////////////////////////////////////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////////////////////////////////////

		~decoder_GROUP   = Group.after(~encoder_GROUP);
		s.sync;

		~binaural_pfl = Synth(\hoa_binaural_decoder_3);

		~binaural_pfl.set();


	}.fork;
);
