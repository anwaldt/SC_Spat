SynthDef(\hoa_mono_encoder_3,
	{
		|
		in_bus  = nil,
		out_bus = 0,
		azim    = 0,
		elev    = 0,
		dist    = 3,
		gain    = 1
		|

		var sound = gain * SoundIn.ar(in_bus);

		var level =  (1.0/(dist+1.0))*(1.0/(dist+1.0));

		Out.ar(out_bus, HOAEncoder.ar(3, sound, azim , elev));

}).add;