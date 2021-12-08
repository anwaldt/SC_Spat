




// start server script
File.readAllString("SC/beatspat_SERVER.sc","r").interpret;



// scopes for different signals and controls

~audio_BUS_spatial.scope;

~gain_BUS_spatial[0].scope;

~ambi_BUS.scope;

~control_vbap_azim_BUS.scope;

~control_azim_BUS.scope;




 // map all lfos to movements:
(

for (0, ~nVbap -1, {arg idx;
	~lfo[idx].map(\out_bus1,  ~control_vbap_azim_BUS.index + (idx*2));
	~lfo[idx].map(\out_bus2,  ~control_vbap_azim_BUS.index + (idx*2) + 1);
});

)


 // set lfo rates:
(

for (0, ~nVbap -1, {arg idx;
	~lfo[idx].set(\dur, 1);

});

)


~lfo[0].set(\offset,0.9);


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

~mouse_BUS.scope

~binaural_panners.do({arg e; e.map(\azim,~mouse_BUS.index)})

