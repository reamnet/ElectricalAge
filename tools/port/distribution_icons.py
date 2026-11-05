#!/usr/bin/env python3
"""Render the adapted source pole parts into reproducible native inventory icons."""
from pathlib import Path
import json,re,sys
import numpy as np
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/icons/shaft'))
from render3d import render

KINDS={7970:'single',7971:'three',7972:'transformer_single',7973:'transformer_three',7974:'feeder',7975:'cutout_single',7976:'cutout_three',7977:'recloser',7978:'secondary',7980:'capacitor',7981:'regulator_single',7982:'regulator_three',7983:'switch',7993:'tap',7979:'secondary_light',7984:'transformer_light_single',7985:'transformer_light_three',7989:'fed_light_single',7990:'fed_light_three',8131:'feeder_single',8133:'transformer_riser_single',8032:'pedestal',8033:'underground_single'}

def parts(kind):
    out={'main','SecondaryRack','Secondary0','Secondary1','Secondary2'}
    if not kind.startswith('secondary'):
        out.add('SingleArm' if kind.endswith('single') or kind=='single' else 'Crossarm');out.add('TopPin')
        if not(kind.endswith('single') or kind=='single'):out.update(('Pin0','Pin2'))
    if 'light'in kind:out.update(('LampBracket','LampHousing','LampLens'))
    if kind.startswith('transformer'):out.update(('Transformer','TransformerPin'))
    if kind=='transformer_riser_single':out.add('SecondaryServiceRiser')
    if kind.startswith(('cutout','regulator','transformer')) or kind in ('tap','feeder_single'):
        for i in ([1]if kind.endswith('single') or kind=='tap' or kind.startswith('transformer') else range(3)):
            out.update(f'{name}{i}'for name in ('CutoutBase','CutoutInsulator','FuseDoor','FuseJumper'))
    if kind.startswith('regulator'):
        for i in ([1]if kind.endswith('single') else range(3)):out.update(f'{name}{i}'for name in ('Regulator','RegulatorPin','RegulatorLoadPin','RegulatorFeed'))
    if kind.startswith('feeder'):
        for i in ([1]if kind.endswith('single') else range(3)):out.update((f'Riser{i}','RiserFuseJumper'if kind.endswith('single') else f'RiserJumper{i}'))
    if kind=='capacitor':
        out.add('CapacitorRack')
        for i in range(3):out.update(f'{name}{i}'for name in ('Capacitor','CapacitorPin','CapacitorFeed'))
    if kind in ('recloser','switch'):
        out.add('Handle')
        for i in range(3):out.update(f'{name}{i}'for name in ('SwitchInsulator','SwitchFeed','Knife'))
    if kind=='recloser':out.update(('Recloser','Control'))
    return out

def color(name):
    if name=="Case":return(.22,.34,.28)
    if name=="Trim":return(.45,.48,.46)
    if name=="Services":return(.72,.68,.35)
    if name in ('main','SingleArm','Crossarm'):return(.39,.27,.14)
    if 'Knife'in name:return(.73,.61,.38)
    if name.startswith('FuseDoor'):return(.86,.86,.77)
    if 'Feed'in name or 'Jumper'in name:return(.13,.15,.16)
    if 'Pin'in name or 'Insulator'in name or name.startswith('Secondary'):return(.57,.66,.65)
    if name.startswith('Capacitor'):return(.76,.77,.72)
    return(.57,.61,.58)

def read_groups(model):
    vertices=[];groups=[];part=''
    for line in(ROOT/"src/main/resources/assets/eln/model"/model/(model+".obj")).read_text().splitlines():
        w=line.split()
        if not w:continue
        if w[0]=='v':vertices.append(tuple(map(float,w[1:4])))
        elif w[0]=='o':part=w[1]
        elif w[0]=='f':groups.append((part,[vertices[int(t.split('/')[0])-1]for t in w[1:]]))
    return groups

def main():
    groups=read_groups("distributionpole")
    ids=json.loads((ROOT/'src/main/resources/eln-ids.json').read_text())
    for identity,kind in KINDS.items():
        selected=read_groups("undergroundservicepedestal"if kind=="pedestal"else "undergroundfused1")if kind in ("pedestal","underground_single")else groups
        names=sorted(set(part for part,_ in selected))if kind in ("pedestal","underground_single")else sorted(parts(kind));atlas=np.array([[color(p)for p in names]],dtype=np.float32);faces=[]
        for part,points in selected:
            if part not in names:continue
            uv=((names.index(part)+.5)/len(names),.5)
            for i in range(1,len(points)-1):faces.append((part,[points[0],points[i],points[i+1]],[uv]*3))
        result=render(faces,atlas,view=(1,-.6,-1.8),size=128,margin=.08)
        rgba=np.zeros((128,128,4),dtype=np.uint8);rgba[:,:,:3]=np.clip(result['rgb']*255,0,255).astype(np.uint8);rgba[:,:,3]=result['mask'].astype(np.uint8)*255
        path=ROOT/'src/main/resources/assets/eln/textures/item'/f"{ids[f'eln:transparent_node@{identity}']['name']}.png"
        path.parent.mkdir(parents=True,exist_ok=True);Image.fromarray(rgba).resize((64,64),Image.Resampling.LANCZOS).save(path)
if __name__=='__main__':main()
