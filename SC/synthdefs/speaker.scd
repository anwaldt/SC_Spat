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