SynthDef(\output_module,
	{
		|
		audio_bus = nil,
		out_bus   = nil,
		delay     = 0,
		gain      = 1,
		hp_cutoff = 5
		|

		var out;

		out = HPF.ar(DelayN.ar(In.ar(audio_bus,1),0.3,delay),hp_cutoff);


		Out.ar(out_bus, gain*out);

}).add;