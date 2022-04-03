SynthDef(\binaural_mono_encoder_3,
	{
		|
		in_bus  = nil,
		out_bus = 0,
		azim    = 0,
		elev    = 0,
		dist    = 3,
		gain    = 0.5
		|

		var sound = gain * In.ar(in_bus);

		var level = (1/(max(0,dist)+1.0));

		var bform = HOASphericalHarmonics.coefN3D(3, azim, elev) * sound * level;

		Out.ar(out_bus, bform);

}).add;