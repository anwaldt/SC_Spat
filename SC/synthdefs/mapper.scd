/*
The mapper synthdef connects two buses.
*/

SynthDef(\mapper,
	{
		|
		inbus   = nil,
		outbus  = nil,
		offset  = 0,
		gain    = 1,
		lagtime = 0.005
		|
		var tmp = In.kr(inbus);
		Out.kr(outbus,   Lag.kr(offset + (tmp*gain) ,lagtime));
	}
).add;