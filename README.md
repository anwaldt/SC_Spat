# SC_SPAT

SC_Spat is a framework for real-time sound spatialization, based 
on SuperCollider. It combines different technologies and
toolboxes, allowing an easy customization of rendering servers.


### Binaural

The binaural part is based on the SC-HOA library:

https://github.com/florian-grond/SC-HOA

### HOA

The HOA part is also based on the SC-HOA library.

Using the SC-HOA library requires the SC3-Plugins  
and the Ambisonic Decoder Toolbox:

https://bitbucket.org/ambidecodertoolbox/adt/src/master/

Building decoders depends on Faust and Octave.
It is a little bit tricky  
the first time but actually well documented   
in the help files of the SC-HOA externals.


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