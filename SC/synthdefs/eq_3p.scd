/*
HvC
2021-08-29
*/

SynthDef(\eq_3p,
	{
		|
		in_bus    = nil,
		out_bus   = nil,
		//
		in_gain   = 1,
		//
		lp_cutoff = 3000,
		lp_slope  = 1,
		lp_gain   = 0,
		//
		bp_cutoff = 1000,
		bp_gain   = 0,
		bp_rq     = 1,
		//
		hp_cutoff = 300,
		hp_slope  = 1,
		hp_gain   = 0
		|

	var in, out;

	in  = in_gain*In.ar(in_bus,2);

	out = HighShelf.ar(in, lp_cutoff,lp_slope, lp_gain);
	out = MidEQ.ar(out, bp_cutoff, bp_rq, bp_gain);
	out = LowShelf.ar(out, hp_cutoff,hp_slope, hp_gain);

	Out.ar(out_bus,out);

	}
).send;
