# Use this:

https://github.com/florian-grond/SC-HOA


# compile and install sc3 plugins (sowieso)

- https://github.com/supercollider/sc3-plugins

# compile and install faust

- https://github.com/grame-cncm/faust
- https://github.com/grame-cncm/faust/wiki/BuildingSimple


# install SC-HOA
- Quarks.install("https://github.com/florian-grond/SC-HOA")

# install Matlab / Octave


# Download the Ambisonics Decoder Toolkit
ATD: https://bitbucket.org/ambidecodertoolbox/adt.git

# build decoders


- read 'HOA Tutorial Exercise 15'
in the SC Documentation 'Libraries > HOA'


- use this for a start:
/home/studio/Schreibtisch/make_EN325_HOA.sc

-----


Maybe try this:

ATK for SuperCollider (http://www.ambisonictoolkit.net/)


## 2020-08-27

# ~/Development/SC3_HOA

- make_EN325_HOA.sc angepasst für die neuen Lautsprecherpositionen
- Decoder liegt in ~/Development/adt/decoders/


----

Octave Bugfixes:

In the file `run_dec_*.m`:

Set plot flag == false -
segfaults otherwise.

The function call for the faust compiler
has to be changed to:

% convert the faustfile generated with the abisonics decoder toolkit from above into scsynth and supernova Ugens
unix(["faust2supercollider -noprefix -sn -ks ",out_path,num2str(order,0),".dsp"]);

