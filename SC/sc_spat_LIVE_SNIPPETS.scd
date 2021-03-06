




// start server script
File.readAllString("SC/beatspat_SERVER.sc","r").interpret;



// scopes for different signals and controls

~audio_BUS_spatial.scope;

~gain_BUS_spatial[0].scope;

~ambi_BUS.scope;

~control_vbap_azim_BUS.scope;

~control_hoa_azim_BUS.scope;




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





