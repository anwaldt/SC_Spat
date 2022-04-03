/*
HvC
2021-08-29
*/

SynthDef(\ms_decoder,
	{
		|
		  in_bus    = nil,
		  mid_bus   = nil,
		  left_bus  = nil,
		  right_bus = nil
		|

		var in, mid, side, left, right;

		in    = In.ar(in_bus, 2);

		side  = 0.5*(in[0] - in[1]);

		mid   = 0.5*(in[0] + in[1]);

		left  = in[0]-mid;
		right = in[1]-mid;

		//mid   = mid-side;

		Out.ar(mid_bus,   mid);
		Out.ar(left_bus,  left);
		Out.ar(right_bus, right);

}).send;