/*

A minimal binaural rendering server.

- based on SC-HOA
- OSC listeners for spherical coordinates

Henrik von Coler
2020-09-19

*/


~server_ADDRESS = 58010;

// number of buses to the spatial modules
~nInputs   = 2;

// HOA Order
~hoa_order = 3;


/////////////////////////////////////////////////////////////////
// THE BUSSES:
/////////////////////////////////////////////////////////////////

postln(thisProcess.argv[0]);

if(size(thisProcess.argv)==1,
	{
		~nInputs   = thisProcess.argv[0].asInteger;
});
postln("Launching with "++~nInputs++" inputs!");

if(size(thisProcess.argv)==2,
	{
		~server_ADDRESS   = thisProcess.argv[1].asInteger;
});
postln("Launching with port:"++~server_ADDRESS++"!");


//Server.supernova;

Server.default = Server(\binaural_server, NetAddr("127.0.0.1", ~server_ADDRESS));

s.options.device               = "SC_BINAURAL";
s.options.numInputBusChannels  = ~nInputs;
s.options.numOutputBusChannels = 4;
s.options.maxLogins            = 8;
s.options.memSize              = 65536;
s.options.numBuffers           = 4096;

// get script's directory for relative paths
~root_DIR = thisProcess.nowExecutingPath.dirname++"/";








s.boot;

~routing_OSC  = NetAddr("127.0.0.1", 9595);
~spatial_OSC  = NetAddr("127.0.0.1", 9494);

~n_hoa_channnels = pow(~hoa_order + 1.0 ,2.0);

s.waitForBoot({

	HOABinaural.loadbinauralIRs(s);
	HOABinaural.loadHeadphoneCorrections(s);
	HOABinaural.binauralIRs;
	HOABinaural.headPhoneIRs;


	s.sync;

	~set = File.readAllString(~root_DIR++"sc_spat_SYNTHDEFS.sc","r");
	~set.interpret;

	/////////////////////////////////////////////////////////////////
	// THE BUSSES:
	/////////////////////////////////////////////////////////////////


	s.sync;

	~control_azim_BUS     = Bus.control(s,~nInputs);
	~control_elev_BUS     = Bus.control(s,~nInputs);
	~control_dist_BUS     = Bus.control(s,~nInputs);


	s.sync;



	// create 2 audio buses for each spatialization module:
	~audio_BUS_spatial      = Bus.audio(s, ~nInputs);


	// bus for encoded 5th order HOA
	~ambi_BUS = Bus.audio(s, ~n_hoa_channnels);



	/////////////////////////////////////////////////////////////////
	// INPUT SECTION

	~input_GROUP = Group.new;



	/////////////////////////////////////////////////////////////////
	// SPATIAL SECTION
	/////////////////////////////////////////////////////////////////

	~spatial_GROUP = ParGroup.after(~input_GROUP);


	/////////////////////////////////////////////////////////////////
	// ambisonics




	for (0, ~nInputs -1, {arg cnt;

		post('Adding HOA panning Module: ');
		cnt.postln;

		~binaural_panners = ~binaural_panners.add(
			Synth(\binaural_mono_encoder_3,
				[
					\in_bus, cnt,
					\out_bus, ~ambi_BUS.index
				],
				target: ~spatial_GROUP
		);)
	});




	for (0, ~nInputs -1, {arg cnt;

		post('Mapping HOA module: ');
		cnt.postln;

		~binaural_panners[cnt].map(\azim, ~control_azim_BUS.index  + cnt);
		~binaural_panners[cnt].map(\elev, ~control_elev_BUS.index  + cnt);
		~binaural_panners[cnt].map(\dist, ~control_dist_BUS.index  + cnt);

	});




	/////////////////////////////////////////////////////////////////
	// decoder
	/////////////////////////////////////////////////////////////////

	~output_GROUP	 = ParGroup.after(~spatial_GROUP);


	~decoder = Synth(\hoa_binaural_decoder_3,
		[
			\in_bus,~ambi_BUS.index,
			\out_bus, 0
		],
		target: ~output_GROUP);


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


	~aed_OSC = OSCFunc(
		{
			arg msg, time, addr, recvPort;

			var azim = msg[2] / 360.0 * (2.0*pi);
			var elev = msg[3] / 360.0 * (2.0*pi);
			var dist = msg[4];

			~control_azim_BUS.setAt(msg[1],azim);
			~control_elev_BUS.setAt(msg[1],elev);
			~control_dist_BUS.setAt(msg[1],dist);

	}, '/source/aed');




	~send_OSC_ROUTINE = Routine({

		inf.do({

			var azim, elev, dist;


			for (0, ~nInputs-1, {

				arg i;

				azim = ~control_azim_BUS.getnSynchronous(~nInputs)[i];
				elev = ~control_elev_BUS.getnSynchronous(~nInputs)[i];
				dist = ~control_dist_BUS.getnSynchronous(~nInputs)[i];

				~spatial_OSC.sendMsg('/source/aed', i, azim, elev, dist);

			});

			0.01.wait;
		});

	});

	~send_OSC_ROUTINE.play;




	/////////////////////////////////////////////////////////////////
	//
	/////////////////////////////////////////////////////////////////

	post("Listening on port: ");
	postln(NetAddr.langPort);
	// ServerMeter(s);

});




{

	s.scope(12,~control_azim_BUS.index);

};


{


	// mouse xy controll with busses
	~mouse_BUS = Bus.control(s,2);


	~mouse   = {
		Out.kr(~control_azim_BUS.index,   MouseX.kr(-pi, pi));
		Out.kr(~control_elev_BUS.index, MouseY.kr(-1,1));
	}.play;

};







