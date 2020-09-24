# SC_SPAT

SC_Spat is a framework for real-time sound spatialization, based 
on SuperCollider. It combines different technologies and
toolboxes, allowing an easy customization of rendering servers.


### Binaural

The binaural part is based on the SC-HOA library:

https://github.com/florian-grond/SC-HOA

### HOA

The HOA part is also based on the SC-HOA library.
Using the SC-HOA library requires the SC3-Plugins.



### ADT

Building custom decoders depends on the 
[Ambisonic Decoder Toolbox](https://bitbucket.org/ambidecodertoolbox/adt/src/master/), using Faust and Octave.
It is a little bit tricky the first time but actually well documented
in the help files of the SC-HOA externals.

**ADT Bugfixes:**

There are some necessary changes 
to the Octave script `run_dec_*.m`, 
created by the SC-HOA tools.

- Set plot flag == false segfaults otherwise.

- The function call for the faust compiler
 has to be changed to:

```
% convert the faustfile generated with the abisonics decoder toolkit from above into scsynth and supernova Ugens
unix(["faust2supercollider -noprefix -sn -ks ",out_path,num2str(order,0),".dsp"]);
```

## Puredata

**Control::**

Puredata examples can be used to control the SC_SPAT server.
They require the PD external 'mrpeach'
for OSC communication.

**Visualization:**

The visualization patches included in the repository
are designed to get a simple impression of the
sources' spatial distribution.
Install [GEM](https://puredata.info/downloads/gem/) for using 
the visualization patches.