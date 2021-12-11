SynthDef(\input_module_mono,
	{
		|
		input_bus           = nil,
		control_BUS_spatial = nil,
		output_bus          = nil
		|

		var in, output, gain;

		for (0, 15,
			{ arg cnt;

				// get the gain value from control bus:
				gain = In.kr(control_BUS_spatial + cnt);

				// get the audio input from hardware input:
				in = SoundIn.ar(input_bus);

				// audio output to dedicated bus
				Out.ar(output_bus + cnt, in*gain);

			}
		);

}).add;

 