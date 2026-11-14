#!/usr/bin/env python3
"""Inventory the complete ForkPatch source scope without treating a shared name as a port.

Run from the 26.3 checkout. No source files are copied or modified. The explicit
1.21.1 include/exclude list determines which incoming files are active.
"""
import argparse
from collections import Counter
import fnmatch
import hashlib
import json
from pathlib import Path
import re
import subprocess


def revision(root):
    return subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()


def sources(root):
    result = {}
    for language in ('java', 'kotlin'):
        base = root / 'src/main' / language
        for file in sorted(base.rglob('*')):
            if file.suffix not in ('.java', '.kt'):
                continue
            match = re.search(r'^package\s+([\w.]+)', file.read_text(errors='replace'), re.M)
            if match:
                key = match[1] + '.' + file.stem
                if key in result:
                    raise ValueError(f'Duplicate source identity: {key}')
                result[key] = file
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--target', type=Path, default=Path('.'))
    parser.add_argument('--output', type=Path, default=Path('docs/forkpatch-port/source-inventory.json'))
    args = parser.parse_args()
    source, target = args.source.resolve(), args.target.resolve()
    incoming, existing = sources(source), sources(target)
    patterns = [line.strip() for line in (source / 'tools/port/include-1.21.txt').read_text().splitlines()
                if line.strip() and not line.lstrip().startswith('#')]
    includes = [p for p in patterns if not p.startswith('!')]
    excludes = [p[1:] for p in patterns if p.startswith('!')]
    entries = []
    for key, file in sorted(incoming.items()):
        relative = file.relative_to(source).as_posix()
        package_path = file.relative_to(source / 'src/main' / relative.split('/')[2]).as_posix()
        active = any(fnmatch.fnmatchcase(package_path, p) for p in includes) and not any(
            fnmatch.fnmatchcase(package_path, p) for p in excludes)
        other = existing.get(key)
        digest = hashlib.sha256(file.read_bytes()).hexdigest()
        status = 'missing-qualified-name' if other is None else 'shared-name-needs-review'
        if other and digest == hashlib.sha256(other.read_bytes()).hexdigest():
            status = 'identical-source-needs-runtime-check'
        if not active:
            status = 'excluded-in-source-build'
        entries.append(dict(identity=key, source=relative, sourceSha256=digest, active=active,
                            target=other.relative_to(target).as_posix() if other else None, status=status))
    data = dict(sourceRepository='reamnet/ELN1.21.1ForkPatch', sourceCommit=revision(source),
                targetRepository='reamnet/ElectricalAge', targetBaselineCommit=revision(target),
                statusMeaning='Names/hashes only; no entry establishes gameplay compatibility.',
                summary=dict(Counter(e['status'] for e in entries)), entries=entries)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    # One record per line keeps the inventory diffable without an enormous pretty-printed artifact.
    with args.output.open('w') as out:
        out.write(json.dumps({k: v for k, v in data.items() if k != 'entries'}, indent=2)[:-2])
        out.write(',\n  "entries": [\n')
        out.write(',\n'.join('    ' + json.dumps(e) for e in entries))
        out.write('\n  ]\n}\n')
    print(json.dumps(data['summary'], sort_keys=True))


if __name__ == '__main__':
    main()
