SynthDef(\hoa_stereo_encoder, {
	|
	in_bus1  = nil,
	in_bus2  = nil,
	out_bus  = 0,
	azim_1    = -45, elev_1 = 0,
	azim_2    =  45, elev_2 = 0
	|

	var sound1 = In.ar(in_bus1)*0.7;
	var sound2 = In.ar(in_bus2)*0.7;

	azim_1 = (azim_1 * (2 * pi)) -pi;
	azim_2 = (azim_2 * (2 * pi)) -pi;

	//elev1 = MouseY.kr(0,3.14);
	//elev2 = MouseY.kr(0,3.14);

	Out.ar(out_bus, HOAEncoder.ar(3, sound1,azim_1, elev_1));
	Out.ar(out_bus, HOAEncoder.ar(3, sound2,azim_2, elev_2));


}).add;