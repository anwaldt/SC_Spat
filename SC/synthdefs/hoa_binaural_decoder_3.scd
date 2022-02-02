
SynthDef(\hoa_binaural_decoder_3,
	{
		|
		in_bus  = 0,
		out_bus = 0,
		gain    = 1
		|

		var sig = HOABinaural.ar(3, In.ar(in_bus,16));
		Out.ar(out_bus, gain*sig);

}).add;