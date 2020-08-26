// SHOULD generate the decoder



 ~spkrPos = [
[3.4959914296458443, 3.3760396804307273,	-0,   "spkr"],
 [2.7432088300900715e-16, 4.48,	-0,   "spkr"],
 [-3.448191990215108, 3.1047660135051025,	-0,   "spkr"],
 [-3.7094349490302116, 0.06474842788232064,	-0,   "spkr"],
 [-3.214689242053487, -3.698076943091582,	-0,   "spkr"],
 [0.5898476220809138, -4.803923373943999,	-0,   "spkr"],
 [3.766000805311633, -3.273734554662631,	-0,   "spkr"],
 [4.040134403552289, 0.28251371866370695,	-0,   "spkr"],
[0,0,3, "virtual"],
];




~path = "/home/anwaldt/adt/";

~hoaAmbiHelper = HOAAmbiDecoderHelper();

~hoaAmbiHelper.setAmbiToolboxPath(~path);

~hoaAmbiHelper.setSpeakerArrayName("Holzmarkt_2020");


~hoaAmbiHelper.specifySpeakersAsXYZ(~spkrPos);



~hoaAmbiHelper.setSweetSpot(~hoaAmbiHelper.centerOfGravity + [0.0,0.0,0.0]);


~hoaAmbiHelper.makeAmbiDecoderSpeakerPositionFile();


~hoaAmbiHelper.makeAmbiDecoderAllradRunFile(order:3, chanOrder:"ACN", chanNorm:"N3D");


~hoaAmbiHelper.makeWrapperClass(~path, 3);


