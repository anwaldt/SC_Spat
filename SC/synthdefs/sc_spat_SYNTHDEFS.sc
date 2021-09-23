


/*
SynthDef(\hoa_octa_decoder, {

|in_bus = 0|


Out.ar(0,Holzmarkt20203.ar(
In.ar(in_bus ),
In.ar(in_bus +1),
In.ar(in_bus +2),
In.ar(in_bus +3),
In.ar(in_bus +4),
In.ar(in_bus +5),
In.ar(in_bus +6),
In.ar(in_bus +7),
In.ar(in_bus +8),
In.ar(in_bus +9),
In.ar(in_bus +10),
In.ar(in_bus +11),
In.ar(in_bus +12),
In.ar(in_bus +13),
In.ar(in_bus +14),
In.ar(in_bus +15),
gain:1) );

}).add;
*/












// taken from vbap_test.scd










/*SynthDef(\send_module,
	{

		|
		in_chan       = nil,
		send_bus      = nil,
		common_bus    = nil
		send_gains    = nil,
		common_gains  = nil,
		gain          = 1
		|

		var in;
		var gain_i, gains;

		in = SoundIn.ar(in_chan);

		for (0, ~nSends,

			{arg cnt;

				gain_i = In.kr(send_gains + cnt);

				Out.ar(send_bus + (cnt),     in * gain * gain_i);
			}
		);

		for (0, ~nCommonSends,

			{arg cnt;

				gain_i = In.kr(common_gains + cnt);

				Out.ar(common_bus + (cnt),     in * gain * gain_i);
			}
		);

	}
).add;*/







