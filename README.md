# SC_SPAT


## SC_SPAT Server

A SuperCollider spatialization server,
using different rendering techniques.



### HOA

The HOA part is based on the SC-HOA library:

https://github.com/florian-grond/SC-HOA

Using the SC-HOA library requires the SC3-Plugins  
and the Ambisonic Decoder Toolbox:

https://bitbucket.org/ambidecodertoolbox/adt/src/master/

Building decoders depends on Faust and Octave.
It is a little bit tricky  
the first time but actually well documented   
in the help files of the SC-HOA externals.


## Puredata

Puredata examples can be used 
to control the SC_SPAT server.
They require the PD external 'mrpeach'
for OSC communication.

