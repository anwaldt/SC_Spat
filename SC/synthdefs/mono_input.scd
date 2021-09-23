SynthDef(\mono_input,
	{
		|
		input_bus           = nil,
		gain                = nil,
		output_bus          = nil
		|

		var in;

		in = SoundIn.ar(input_bus);

		Out.ar(output_bus, in*gain);

}).add;