MIDIIn.connect;

MIDIdef.freeAll;

//   listener for LFO trigger events

MIDIdef(\lfo_trigger_on,
	{

		arg vel, note, channel;

		[vel,note,channel].postln;

		vel = (vel>1).asInt;

		~lfo[note].set(\trig, vel, \run, vel);

},nil,nil,\noteOn,nil);


MIDIdef(\lfo_trigger_off,
	{

		arg vel, note, channel;

		[vel,note,channel].postln;

		vel = (vel>1).asInt;

		~lfo[note].set(\trig, vel, \run, vel);

},nil,nil,\noteOff,nil);


