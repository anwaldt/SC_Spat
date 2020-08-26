#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Holzmarkt: 2020-02
"""

import numpy as np
import matplotlib.pyplot as plt


def pol2cart(rho, phi):
    phi = phi/360 * (2*np.pi) + np.pi*0.5
    x = rho * np.cos(phi) 
    y = rho * np.sin(phi) 
    return(x, y)

speakers_ad = [
[237, 4.86],
[283, 4.48],
[331, 4.64],
[12,  3.71],
[62,   4.9],
[110, 4.84],
[152, 4.99],
[197, 4.05],
]


nSpeakers = len(speakers_ad)

speakers_xy = []

fig, ax = plt.subplots()

for i in range(nSpeakers):
    
    tmp = speakers_ad[i]
    
    out = pol2cart(tmp[1], tmp[0]-speakers_ad[1][0])
    
    speakers_xy.append(out)
    
    ax.scatter(out[0], out[1],)   
     
    plt.text(out[0],out[1], str(i), fontsize=9)
    
    
ax.set_aspect('equal', 'box')

  