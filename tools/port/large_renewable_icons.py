#!/usr/bin/env python3
"""Reproducible inventory icons from the large renewable source meshes."""
from pathlib import Path
import sys,numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/icons/shaft'));sys.path.insert(0,str(ROOT/'tools/port'))
from render3d import render
from distribution_icons import read_groups
for model,name,water in [('utilitywind','utility_wind_turbine_3_2kv',False),('utilitywatermill','large_watermill_3_2kv',True)]:
 colors={'Case':(.77,.80,.82),'Trim':(.17,.20,.23),'Output':(.97,.37,.05),'Return':(.1,.12,.14),'Rotor':(.37,.26,.14)if water else(.88,.90,.91)}
 groups=read_groups(model);names=sorted(colors);atlas=np.array([[colors[n]for n in names]],dtype=np.float32);faces=[]
 for part,points in groups:
  if part not in colors:continue
  if part=='Rotor':points=[(x,y+(3.5 if water else 9.5),z+(-.65 if water else .8))for x,y,z in points]
  uv=((names.index(part)+.5)/len(names),.5)
  for j in range(1,len(points)-1):faces.append((part,[points[0],points[j],points[j+1]],[uv]*3))
 result=render(faces,atlas,view=(1,-.6,-1.8),size=128,margin=.06);rgba=np.zeros((128,128,4),dtype=np.uint8);rgba[:,:,:3]=np.clip(result['rgb']*255,0,255).astype(np.uint8);rgba[:,:,3]=result['mask'].astype(np.uint8)*255
 Image.fromarray(rgba).resize((64,64),Image.Resampling.LANCZOS).save(ROOT/'src/main/resources/assets/eln/textures/item'/f'{name}.png')
