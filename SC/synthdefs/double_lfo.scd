
SynthDef(\double_lfo,
	{
		|
		run      = 0,
		dur      = 1,
		trig     = 0,
		dir      = pi,
		gain     = 1,
		offset   = 0,
		out_bus1 = nil,
		out_bus2 = nil
		|

		var ramp, x;

		dur = max(dur,0.01);

		ramp = Sweep.kr(trig, run / dur).linlin(0, 1, 0, dir, \minmax);

		x = (gain * sin(ramp)) + offset;


		// var x = Sweep.kr(trig, rate);

		Out.kr(out_bus1,   x);
		Out.kr(out_bus2,   x * (-1));


}).add;

