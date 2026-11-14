#!/usr/bin/env python3
"""Render the pinned compact UPS mesh into its native inventory icon."""
from pathlib import Path
import sys
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/icons/shaft'))
sys.path.insert(0,str(ROOT/'tools/port'))
from render3d import render
from distribution_icons import read_groups
colors={'Case':(.14,.16,.18),'Trim':(.28,.31,.34),'Vents':(.04,.05,.06),
        'Input':(.18,.36,.65),'Output':(.85,.40,.12),'Display':(.015,.045,.055),
        'Socket120':(.035,.04,.045),'MainsLed':(.1,.8,.2),'BackupLed':(.25,.15,.02),
        'ChargeLed':(.02,.1,.2),'FaultLed':(.2,.02,.01),'BypassLed':(.16,.06,.2)}
names=sorted(colors)
atlas=np.array([[colors[n] for n in names]],dtype=np.float32)
faces=[]
for part,points in read_groups('compactups'):
    if part not in colors:continue
    uv=((names.index(part)+.5)/len(names),.5)
    for i in range(1,len(points)-1):faces.append((part,[points[0],points[i],points[i+1]],[uv]*3))
result=render(faces,atlas,view=(1,-.6,-1.8),size=128,margin=.08)
rgba=np.zeros((128,128,4),dtype=np.uint8)
rgba[:,:,:3]=np.clip(result['rgb']*255,0,255).astype(np.uint8)
rgba[:,:,3]=result['mask'].astype(np.uint8)*255
Image.fromarray(rgba).resize((64,64),Image.Resampling.LANCZOS).save(ROOT/'src/main/resources/assets/eln/textures/item/four_cell_plug_in_ups.png')
