#!/bin/sh

################################################################
#
# Start a 24 channel binaural server and a PD/GEM visualizer.
#
# - All processes running in the background.
# - Can be used with GLOOO_synth.
#
################################################################

sclang -u 57121 SC/sc_spat_binaural_SERVER.sc 16 &


puredata -noaudio -nogui PD/source_visualizer.pd &
