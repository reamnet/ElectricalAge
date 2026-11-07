#!/usr/bin/env python3
"""Write the model index from shipped OBJs and the native generated CAD models.
Obj3DFolder reads it to load all models (scanning the mod jar is not possible under the 26.x module system).
Run ./gradlew generateCadModels first; re-run after adding or removing models."""
import os
root = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'eln', 'model')
cad = os.path.join(os.path.dirname(__file__), '..', 'build', 'generated', 'cad', 'assets', 'eln', 'model')
if not os.path.isdir(cad):
    raise SystemExit('Run ./gradlew generateCadModels before regenerating the model index')
paths = sorted({os.path.relpath(os.path.join(d, f), folder).replace(os.sep, '/')
                for folder in (root, cad) for d, _, fs in os.walk(folder)
                for f in fs if f.lower().endswith('.obj')})
open(os.path.join(root, 'index.txt'), 'w').write('\n'.join(paths) + '\n')
print(len(paths), 'models')
