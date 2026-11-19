#!/usr/bin/env python3
"""Reproducible source-mesh icons for the six independent-pole switchgear devices."""
from pathlib import Path
import json,sys
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/icons/shaft'))
sys.path.insert(0,str(ROOT/'tools/port'))
from render3d import render
from distribution_icons import read_groups
KINDS=['contactor','contactor_three','relay_two','relay_four','ssr','ssr_three']
COLORS={'Base':(.14,.16,.18),'Case':(.72,.74,.73),'Terminal':(.66,.48,.20),'Heatsink':(.22,.24,.26),'Coil':(.58,.27,.12),'Control':(.12,.35,.67),'Armature':(.48,.51,.53),'NO':(.92,.49,.10),'NC':(.60,.29,.68),'Indicator':(.08,.16,.06)}
def main():
    ids=json.loads((ROOT/'src/main/resources/eln-ids.json').read_text())
    for i,kind in enumerate(KINDS):
        groups=read_groups('switchgear_'+kind);names=sorted({n for n,_ in groups if n in COLORS});faces=[]
        atlas=np.array([[COLORS[n]for n in names]],dtype=np.float32)
        for part,points in groups:
            if part not in names:continue
            uv=((names.index(part)+.5)/len(names),.5)
            for j in range(1,len(points)-1):faces.append((part,[points[0],points[j],points[j+1]],[uv]*3))
        result=render(faces,atlas,view=(1,-.6,-1.8),size=128,margin=.08)
        rgba=np.zeros((128,128,4),dtype=np.uint8);rgba[:,:,:3]=np.clip(result['rgb']*255,0,255).astype(np.uint8);rgba[:,:,3]=result['mask'].astype(np.uint8)*255
        name=ids[f'eln:transparent_node@{8037+i}']['name'];Image.fromarray(rgba).resize((64,64),Image.Resampling.LANCZOS).save(ROOT/'src/main/resources/assets/eln/textures/item'/f'{name}.png')
if __name__=='__main__':main()
