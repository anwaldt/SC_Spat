#!/bin/sh

################################

killall sclang
killall scsynth
# killall jackd

sleep 2

################################

# jackd  -P 95 -d alsa -d hw:UFX23208936 -r 48000 -p 256 -n 2    &

sleep 4

# sclang -u 57120 SC/beatspat_SERVER.sc &

sleep 2

# sclang -u 57121 SC/beatspat_REMOTE.sc &

slep 2

bitwig-studio /media/anwaldt/ANWALDT_2TB/SOUND/PROJECTS/2020/Die_Gams/GAMS/GAMS.bwproject &

sleep 8

jmess -D -c config/audio.jmess
