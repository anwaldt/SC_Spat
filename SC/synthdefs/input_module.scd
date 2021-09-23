SynthDef(\input_module,
	{
		|
		input_bus           = nil,
		control_BUS_spatial = nil,
		output_bus          = nil
		|

		var in1, in2, output, gain;

		for (0, 15,
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