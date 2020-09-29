#!/bin/sh

################################################################
#
# Copy all .sc and .so files to the SC extension directory.
#
################################################################

sudo cp adt/decoders/*.so $1 &
sudo cp adt/decoders/*.sc $1 &
