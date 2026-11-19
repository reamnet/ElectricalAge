#!/usr/bin/env python3
"""Check the import ledger against its source checkout and current target files.

This verifies provenance and exact-copy claims, not gameplay completeness. Runtime
coverage belongs in the accompanying circuit/native/regression reports.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import subprocess


def revision(root):
    return subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def contained(root, relative):
    path = (root / relative).resolve()
    path.relative_to(root)  # A ledger entry must remain inside its selected checkout.
    return path


def audit(source, target):
    ledger = json.loads((target / 'docs/forkpatch-port/imports.json').read_text())
    rows, problems = [], []
    for number, entry in enumerate(ledger):
        row = dict(index=number, source=entry['source'], target=entry['target'], mode=entry['mode'])
        try:
            original = contained(source, entry['source'])
            adapted = contained(target, entry['target'])
            expected = entry['sourceSha256']
            if not re.fullmatch(r'[0-9a-f]{64}', expected):
                raise ValueError('invalid source SHA-256')
            source_hash, target_hash = digest(original), digest(adapted)
            row.update(sourceSha256=source_hash, targetSha256=target_hash,
                       sourceMatchesLedger=source_hash == expected,
                       exactCopy=source_hash == target_hash,
                       declaredVerbatim=entry['mode'].startswith('verbatim'))
            if not row['sourceMatchesLedger']:
                problems.append(dict(index=number, problem='source hash differs from ledger'))
            if row['declaredVerbatim'] and not row['exactCopy']:
                problems.append(dict(index=number, problem='declared verbatim copy differs'))
        except (OSError, ValueError) as error:
            problems.append(dict(index=number, problem=str(error)))
        rows.append(row)
    return dict(sourceCommit=revision(source), targetCommit=revision(target),
                ledgerEntries=len(rows), uniqueSourceFiles=len({r['source'] for r in rows}),
                uniqueTargetFiles=len({r['target'] for r in rows}),
                declaredVerbatimEntries=sum(r.get('declaredVerbatim', False) for r in rows),
                exactCopies=sum(r.get('exactCopy', False) for r in rows),
                passed=not problems, problems=problems, entries=rows,
                scope='Source hashes, target existence and declared exact copies; not full runtime parity.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--target', type=Path, default=Path('.'))
    parser.add_argument('--output', type=Path, default=Path('docs/forkpatch-port/import-audit.json'))
    args = parser.parse_args()
    result = audit(args.source.resolve(), args.target.resolve())
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({key: value for key, value in result.items() if key != 'entries'}, indent=2))
    if not result['passed']:
        raise SystemExit(1)


if __name__ == '__main__':
    main()
