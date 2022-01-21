// outgoing OSC connection for spatial data:
~spatial_OSC  = NetAddr("192.168.1.31", 9595);

// outgoing OSC connection for send gain data:
~gain_OSC     = NetAddr("192.168.1.31", 9596);

thisProcess.openUDPPort(~input_OSC);

post("Listening on port: ");
postln(thisProcess.openPorts);

/////////////////////////////////////////////////////////////////
// Set source control to LFO or direct
/////////////////////////////////////////////////////////////////

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


/////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////




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

/////////////////////////////////////////////////////////////////


~send_positions_ROUTINE = Routine({

	inf.do({

		var azim, elev, dist;

		for (0, ~nSpatialInputs-1, {

			arg i;

			azim = ~monitor_azim_BUS.getnSynchronous(~nSpatialInputs)[i];
			elev = ~monitor_elev_BUS.getnSynchronous(~nSpatialInputs)[i];
			dist = ~monitor_dist_BUS.getnSynchronous(~nSpatialInputs)[i];

			~spatial_OSC.sendMsg('/source/aed', i, azim, elev, dist);

		});

		0.05.wait;
	});

});

~send_positions_ROUTINE.play;



~send_sends_ROUTINE = Routine({

	inf.do({

		for (0, ~nDirectInputs+~nSpatialInputs-1,
			{ arg i;

				var array = ~gain_BUS[i].getnSynchronous(~nDirectInputs+~nSpatialInputs);

				for (0, ~nSpatialInputs-1,
					{ arg j;

						~gain_OSC.sendMsg("/send/level", i, j, array[j]);
				});

				0.01.wait;
		});
	});
});

~send_sends_ROUTINE.play;


~send_sends_ROUTINE.stop;