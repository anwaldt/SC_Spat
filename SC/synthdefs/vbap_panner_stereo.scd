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