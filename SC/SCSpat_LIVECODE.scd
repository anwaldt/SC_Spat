// start server script
File.readAllString("SC/beatspat_SERVER.sc","r").interpret;


~decoder.set(\gain, 1);


////////////////////////////////////////////////////////////////////////

////////////////////////////////////////////////////////////////////////

MIDIdef(\binaural_gain,
	{

		arg ... args;

		var gain = pow(args[0]/127,2);

		postln(gain);

		~decoder.set(\gain, gain);

},62,nil,\control,nil);


////////////////////////////////////////////////////////////////////////
// scopes for different signals and controls
////////////////////////////////////////////////////////////////////////

~ambi_BUS.scope;
~automate_azim_BUS.scope;



////////////////////////////////////////////////////////////////////////
// set positions to pattern
////////////////////////////////////////////////////////////////////////

// all mono from the front
(
~control_azim_BUS.setAll(1);
~control_elev_BUS.setAll(0.5);
~control_dist_BUS.setAll(1)
)


// make a perfect circle around listener position with all sources
(
for(0,~nSpatialInputs-1, {arg i;
	~control_azim_BUS.setAt(i,(i/~nSpatialInputs)*pi-(pi/2));
});
)

(
for(0,~nSpatialInputs-1, {arg i;
	~control_elev_BUS.setAt(i,(i/~nSpatialInputs)*0.25*pi);
});
)

 // map all lfos to movements:
(

for (0, ~nVbap -1, {arg idx;
	~lfo[idx].map(\out_bus1,  ~control_vbap_azim_BUS.index + (idx*2));
	~lfo[idx].map(\out_bus2,  ~control_vbap_azim_BUS.index + (idx*2) + 1);
});

)



~lfo[0].set(\offset,0);


~mouse.free

(
{
	///////////////////////////////////////////////////////////////////////////////////
	// make an x-y control stream to a temp BUS

	// mouse xy controll with busses
	~mouse_BUS = Bus.control(s,2);

	s.sync;

	~mouse   = {
		Out.kr(~mouse_BUS.index,   MouseX.kr(-3,3));
		Out.kr(~mouse_BUS.index+1, MouseY.kr(-3,3));
	}.play;

}.fork;
)


~binaural_panners.do({arg e; e.map(\azim,~mouse_BUS.index)})
