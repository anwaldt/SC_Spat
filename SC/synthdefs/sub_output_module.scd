SynthDef(\sub_output_module,
	{
		|
		audio_bus = nil,
		out_bus   = nil,
		delay     = 0,
		gain      = 1,
		lp_cutoff = 100
		|

		var in, out;

		in  = DelayN.ar(In.ar(audio_bus,1),0.3,delay);
		out = LPF.ar(in, lp_cutoff);

		Out.ar(out_bus, gain*out);
}).add;