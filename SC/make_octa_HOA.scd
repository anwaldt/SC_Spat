/*

Make 2-dimensional 8-channel HOA decoders
for different orders (up to 3), uing the ADT.


- adapted from SC readme: 'HOA Tutorial Exercise 15'

Henrik von Coler
2020-09-29

*/

// Define decoder name
~decoder_name = "OCTA";


// Define speaker positions:
 ~spkrPos = [
[2.93589228605197,	 0.683203536311494,	 -0,   "spkr"],
[2.32382822803067,	-1.43690698812563,	 -0,   "spkr"],
[0.353378385469945,	-1.92919991891976,	 -0,   "spkr"],
[-1.56920406418665,	-1.97842372028654,	 -0,   "spkr"],
[-2.79528982399747,	-0.317988852989665,	 -0,   "spkr"],
	[-2.31115625527173,	 2.07732531407415,	 -0,   "spkr"],
[-0.180140359624532, 2.63561514195793,	 -0,   "spkr"],
[2.08264937613841,	 2.50411369490646,	 -0,   "spkr"],
[0,0,3, "virtual"],
];


// get this script's directory for relative paths
~root_DIR = thisProcess.nowExecutingPath.dirname;

// Set the path to the ADT (included as submodule):
~path = ~root_DIR.replace("SC","")++"adt/";

~hoaAmbiHelper = HOAAmbiDecoderHelper();

~hoaAmbiHelper.setAmbiToolboxPath(~path);

~hoaAmbiHelper.setSpeakerArrayName(~decoder_name);

~hoaAmbiHelper.specifySpeakersAsXYZ(~spkrPos);

~hoaAmbiHelper.setSweetSpot(~hoaAmbiHelper.centerOfGravity + [0.0,0.0,0.0]);

~hoaAmbiHelper.makeAmbiDecoderSpeakerPositionFile();

~hoaAmbiHelper.makeAmbiDecoderAllradRunFile(order:3, chanOrder:"ACN", chanNorm:"N3D");

~hoaAmbiHelper.makeWrapperClass(~path, 3);
