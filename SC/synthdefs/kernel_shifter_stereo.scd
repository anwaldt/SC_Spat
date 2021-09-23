// adapted to stereo from vbap_test.scd
SynthDef(\kernel_shifter_stereo,
	{
		|
		in_bus1  = 0,
		in_bus2  = 1,
		out_bus  = 0,
		mod_1    = 0
		|


		for(0, 15, {arg idx;

			Out.ar(out_bus +idx,   In.ar(in_bus1) * sin((idx * mod_1) + idx));

		});

}).add;