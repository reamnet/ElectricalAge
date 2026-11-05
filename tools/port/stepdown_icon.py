#!/usr/bin/env python3
"""Render the standalone step-down's native transformer enclosure into its inventory icon."""
from pathlib import Path
import sys
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/icons/shaft'))
from render3d import render

def main():
    verts=[];faces=[];part=''
    path=ROOT/'src/main/resources/assets/eln/model/transformator/transformatorcase.obj'
    for line in path.read_text().splitlines():
        words=line.split()
        if not words:continue
        if words[0]=='o':part=words[1]
        elif words[0]=='v':verts.append(tuple(map(float,words[1:4])))
        elif words[0]=='f' and part in ['Case','DoorL','DoorR']:
            points=[verts[int(w.split('/')[0])-1]for w in words[1:]]
            for i in range(1,len(points)-1):faces.append((part,[points[0],points[i],points[i+1]],[(.5,.5)]*3))
    result=render(faces,np.array([[[.64,.67,.68]]],dtype=np.float32),view=(-1,-.6,-1.8),size=128,margin=.08)
    rgba=np.zeros((128,128,4),dtype=np.uint8);rgba[:,:,:3]=np.clip(result['rgb']*255,0,255).astype(np.uint8);rgba[:,:,3]=result['mask'].astype(np.uint8)*255
    Image.fromarray(rgba).resize((64,64),Image.Resampling.LANCZOS).save(ROOT/'src/main/resources/assets/eln/textures/item/mains_step_down_unit.png')
if __name__=='__main__':main()
