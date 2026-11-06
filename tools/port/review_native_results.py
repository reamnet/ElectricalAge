#!/usr/bin/env python3
"""Summarize a real native report and retain golden differences without changing expectations."""
import argparse
from collections import Counter, defaultdict
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET


def review(report, log):
    cases = list(ET.parse(report).getroot().iter('testcase'))
    failures = []
    for case in cases:
        problem = case.find('failure')
        if problem is None:
            problem = case.find('error')
        if problem is not None:
            failures.append(dict(name=case.get('name'), message=problem.get('message', problem.text or '')))
    port = [c for c in cases if c.get('name', '').startswith('eln:forkpatch_')]
    assertion_failures = defaultdict(list)
    for line in Path(log).read_text(errors='replace').splitlines():
        match = re.search(r'\[eln/\]: \[([^]]+)\] FAIL (.*)', line)
        if match:
            assertion_failures[match[1]].append(match[2])
    golden = {}
    for name in ('golden_descriptors', 'golden_slots'):
        rows = []
        for message in assertion_failures.get(name, []):
            if 'known in 1.12' in message:
                category = 'identity_absent_from_1_12_reference'
            elif 'descriptor numbers as in 1.12' in message:
                category = 'descriptor_values_changed'
            elif re.search(r' slot \d+:', message):
                category = 'slot_acceptance_changed'
            elif 'has no 1.12 container' in message:
                category = 'device_without_1_12_container'
            elif 'in the 1.12 dump' in message:
                category = 'descriptor_class_without_1_12_container'
            elif 'slots match:' in message:
                category = 'slot_comparison_summary'
            else:
                category = 'requires_individual_review'
            rows.append(dict(category=category, message=message))
        golden[name] = dict(failedAssertions=len(rows), categories=dict(Counter(r['category'] for r in rows)), differences=rows)
    non_golden = {k: v for k, v in assertion_failures.items() if k not in golden}
    return dict(
        scope='Observed native results; no golden expectations or production behavior are changed by this tool.',
        selected=len(cases), passed=len(cases)-len(failures)-sum(c.find('skipped') is not None for c in cases),
        failed=len(failures), skipped=sum(c.find('skipped') is not None for c in cases), failures=failures,
        portCases=dict(selected=len(port), failed=sum(c.find('failure') is not None or c.find('error') is not None for c in port)),
        golden=golden, nonGoldenAssertions=non_golden,
        command='./gradlew --no-daemon --max-workers=2 runGameTestServer --args=\'net.neoforged.fml.startup.GameTestServer --report REPORT.xml --universe NEW_UNIQUE_WORLD\'',
        report=str(report), log=str(log),
        limitations=['Failure categories describe observed differences, not approved exceptions.',
                     'Review against pinned ForkPatch source and intentional destination changes before updating historical checks.'])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', type=Path, required=True)
    parser.add_argument('--log', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    result = review(args.report, args.log)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2)+'\n')
    print(json.dumps({k: result[k] for k in ('selected', 'passed', 'failed', 'skipped', 'portCases')}, indent=2))


if __name__ == '__main__':
    main()
