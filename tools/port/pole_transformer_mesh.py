#!/usr/bin/env python3
"""Extract only the ForkPatch can/bushing submeshes for native transformer items."""
from pathlib import Path
import argparse
ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--source', type=Path, default=ROOT.parent / 'ELN1.21.1ForkPatch')
args = parser.parse_args()
vertices, groups, part = [], {}, None
for line in (args.source / 'src/main/resources/assets/eln/model/distributionpole/distributionpole.obj').read_text().splitlines():
    words = line.split()
    if not words:
        continue
    if words[0] == 'o':
        part = words[1]
    elif words[0] == 'v':
        vertices.append(tuple(map(float, words[1:4])))
    elif words[0] == 'f' and part in ('Transformer', 'TransformerPin'):
        groups.setdefault(part, []).append([int(x.split('/')[0]) - 1 for x in words[1:]])
output = ['# Source can/bushing submeshes, centered for native item display.', 'mtllib fixture.mtl']
offset = 0
for part, faces in groups.items():
    output.extend(['o ' + part, 'usemtl fixture'])
    used = sorted({i for face in faces for i in face})
    mapping = {old: offset + i + 1 for i, old in enumerate(used)}
    for i in used:
        x, y, z = vertices[i]
        output.append(f'v {x+.4:.6f} {y-5.35:.6f} {z:.6f}')
    for face in faces:
        for i in range(1, len(face) - 1):
            output.append('f ' + ' '.join(str(mapping[j]) for j in (face[0], face[i], face[i + 1])))
    offset += len(used)
assert set(groups) == {'Transformer', 'TransformerPin'}
(ROOT / 'src/main/resources/assets/eln/model/poletransformer/poletransformer.obj').write_text('\n'.join(output) + '\n')

(ROOT / 'src/main/resources/assets/eln/model/poletransformer/fixture.mtl').write_text('newmtl fixture\nKd 1 1 1\nKa 1 1 1\nd 1\n')
