
(
s.options.numInputBusChannels  = 8;
s.options.numOutputBusChannels = 8;
s.options.numAudioBusChannels  = 4096;

s.options.memSize = 65536;
s.boot;
);


(
{
	///////////////////////////////////////////////////////////////////////////////////
	// make an x-y control stream to a temp BUS
	~mouse.free;

	// mouse xy controll with busses
	~mouse_BUS = Bus.control(s,2);

	s.sync;

	~mouse   = {
		Out.kr(~mouse_BUS.index,   MouseX.kr(0,20000));
		Out.kr(~mouse_BUS.index+1, MouseY.kr(-48,6));
	}.play;

}.fork;
~mouse_BUS.scope;
)

(
~test = Synth.new(\ms_decoder);
~test.set(\in_bus, s.options.numOutputBusChannels);
~test.set(\mid_bus, 2, \left_bus, 2, \right_bus,2);
);

(
~test.set(\mid_bus, 0);
~test.set(\left_bus, 0);
~test.set(\right_bus, 1);
~test.free
);








(
~test.free;
~test = Synth.new(\eq_3p);
~test.set(\in_bus, s.options.numOutputBusChannels);
~test.set(\out_bus, 0);
);


~test.map(\lp_cutoff, ~mouse_BUS);
~test.map(\bp_gain, ~mouse_BUS.index+1);
~test.set(\bp_rq, 2);

