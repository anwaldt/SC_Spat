
/////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////

// outgoing OSC connection for spatial data:
~spatial_OSC = [];

// add connection via OSC command
OSCdef('/addlistener/position',
	{
		arg msg, time, addr, recvPort;

		var port    = msg[1];

		postln('Added source position listener: '++addr.ip++' '++port.asString);

		~spatial_OSC = ~spatial_OSC.add(NetAddr(addr.ip, port));

},'/addlistener/position');




// outgoing OSC connections for send gain data:
~gain_OSC     = [];

// add connection via OSC command
OSCdef('/addlistener/gains',
	{
		arg msg, time, addr, recvPort;

		var port    = msg[1];

		postln('Added send gain listener: '++addr.ip++' '++port.asString);

		~gain_OSC = ~gain_OSC.add(NetAddr(addr.ip, port));

},'/addlistener/gains');


/////////////////////////////////////////////////////////////////
// sd
/////////////////////////////////////////////////////////////////

OSCdef('/send/gain',
	{
		arg msg, time, addr, recvPort;

		var src  = msg[1];
		var dest = msg[2];
		var gain = msg[3];

		~gain_BUS[src].setAt(dest,gain);

},'/send/gain');


/////////////////////////////////////////////////////////////////
// Set source control to LFO or direct
/////////////////////////////////////////////////////////////////

OSCdef('/encoder/mode',
	{
		arg msg, time, addr, recvPort;
		var src  = msg[1];
		var mode = msg[2];

		switch(mode,
			'direct',
			{
				postln("Setting encoder to direct control!");
				//~binaural_panners[src].map(\azim,  ~control_azim_BUS.index + (src));
				~azim_MAPPER[src].set(\inbus,  ~control_azim_BUS.index + src);
			},
			'lfo',
			{
				postln("Setting encoder to LFO!");
				//~binaural_panners[src].map(\azim,  ~automate_azim_BUS.index + (src));
				~azim_MAPPER[src].set(\inbus,  ~automate_azim_BUS.index + src);
			}
		);
	},'/encoder/mode'
);



/////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////


OSCdef('/gain/hoa',
	{
		arg msg, time, addr, recvPort;

		var gain = msg[1];

		~hoa_output.set(\gain,gain);

},'/gain/hoa');



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

		// ~lfo[ind].set(\gain, val);

		~lfo_gain_BUS.setAt(ind,val);

	},'/lfo/gain'
);


OSCdef('/lfo/dur',
	{
		arg msg, time, addr, recvPort;
		var ind, val;

		ind = msg[1];
		val = msg[2];

		// ~lfo[ind].set(\dur, val);
		~lfo_duration_BUS.setAt(ind,val);

	},'/lfo/dur'
);


OSCdef('/lfo/dir',
	{
		arg msg, time, addr, recvPort;
		var ind, val;

		ind = msg[1];
		val = msg[2];

		//~lfo[ind].set(\dir, val);

		~lfo_direction_BUS.setAt(ind,val);

	},'/lfo/dir'
);


OSCdef('/lfo/offset',
	{
		arg msg, time, addr, recvPort;
		var ind, val;

		ind = msg[1];
		val = msg[2];

		// ~lfo[ind].set(\offset, val);

		~lfo_offset_BUS.setAt(ind,val);

	},'/lfo/offset'
);


/////////////////////////////////////////////////////////////////

/////////////////////////////////////////////////////////////////


~send_positions_ROUTINE = Routine({

	inf.do({

		var azim, elev, dist;

		azim = ~monitor_azim_BUS.getnSynchronous(~nSpatialInputs);
		elev = ~monitor_elev_BUS.getnSynchronous(~nSpatialInputs);
		dist = ~monitor_dist_BUS.getnSynchronous(~nSpatialInputs);

		for (0, ~nSpatialInputs-1, { arg i;

			~spatial_OSC.do({arg e; e.sendMsg('/source/aed', i, azim[i], elev[i], dist[i])});

		});

		0.005.wait;
	});

});

~send_positions_ROUTINE.play;

/////////////////////////////////////////////////////////////////

~send_sends_ROUTINE = Routine({

	inf.do({

		for (0, ~nDirectInputs+~nSpatialInputs-1,
			{ arg i;

				var array = ~gain_BUS[i].getnSynchronous(~nDirectInputs+~nSpatialInputs);

				for (0, ~nDirectInputs+~nSpatialInputs-1, { arg j;

					~gain_OSC.do({arg e; e.sendMsg("/send/level", i, j, array[j])});
				});

				0.001.wait;
		});
	});
});

~send_sends_ROUTINE.play;




/////////////////////////////////////////////////////////////////

~send_lfo_ROUTINE = Routine({

	inf.do({

		var a1 = ~lfo_duration_BUS.getnSynchronous(~n_stereo);
		var a2 = ~lfo_direction_BUS.getnSynchronous(~n_stereo);
		var a3 = ~lfo_gain_BUS.getnSynchronous(~n_stereo);
		var a4 = ~lfo_offset_BUS.getnSynchronous(~n_stereo);

		for(0, ~n_stereo -1, { arg i;

			~spatial_OSC.do({arg e;

				e.sendMsg("/lfo/dur",    i, a1[i]);
				e.sendMsg("/lfo/dir",    i, a2[i]);
				e.sendMsg("/lfo/gain",   i, a3[i]);
				e.sendMsg("/lfo/offset", i, a4[i]);

			});

			0.001.wait;
		});
	});
});

~send_lfo_ROUTINE.play;

// ~send_lfo_ROUTINE.stop;



