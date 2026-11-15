#!/usr/bin/env python3
"""Build a review plan from the pinned source, native files and explicit import ledger.

Read-only with respect to both implementations. Names, byte equality and ledger
links are observations, never evidence that gameplay behavior is complete.
"""
import argparse
from collections import Counter, defaultdict
import csv
import fnmatch
import hashlib
import json
from pathlib import Path
import re
import subprocess


RULES = {
    'registration': 'registry', 'generic': 'registry',
    'node': 'mounting', 'ghost': 'mounting', 'misc': 'review-remainder',
    'sim': 'solver', 'solver': 'solver',
    'gridnode': 'wiring', 'cable': 'wiring', 'utility': 'distribution',
    'utility.Transmission': 'substations', 'utility.BreakerPanel': 'panel',
    'utility.WirelessPowerChannels': 'panel', 'gridnode.transformer': 'electronics',
    'sixnode.lampsocket': 'lighting', 'lightblock': 'lighting', 'item.lampitem': 'bulbs',
    'transparentnode.floodlight': 'other-lights', 'transparentnode.festive': 'other-lights',
    'transparentnode.utilitylight': 'other-lights', 'transparentnode.StreetLightWall': 'other-lights',
    'transparentnode.worklight': 'other-lights', 'utility.PoleStreetlight': 'other-lights',
    'transparentnode.firealarm': 'alarms', 'transparentnode.cord': 'cords',
    'transparentnode.kitchen': 'kitchen', 'compat.Refrigerator': 'kitchen',
    'transparentnode.laundry': 'laundry', 'transparentnode.fusion': 'laundry',
    'environment': 'climate', 'transparentnode.climate': 'climate',
    'eventhandlers.RoomThermal': 'climate', 'transparentnode.fan': 'fans',
    'transparentnode.evaporative': 'fans', 'transparentnode.pump': 'pumps',
    'transparentnode.growing': 'pumps', 'fluid': 'fluids', 'mechanical': 'fluids',
    'transparentnode.heatfurnace': 'fluids', 'transparentnode.FuelHeatFurnace': 'fluids',
    'transparentnode.eggincubator': 'fluids', 'transparentnode.themralheatexchanger': 'fluids',
    'transparentnode.thermaldissipator': 'fluids', 'transparentnode.turbine': 'fluids',
    'sixnode.thermalcable': 'fluids', 'vehicle': 'vehicles',
    'transparentnode.industrial': 'manufacturing',
    'transparentnode.industrial.Forklift': 'transport',
    'transparentnode.industrial.Pallet': 'transport',
    'transparentnode.industrial.EvPackCharger': 'transport',
    'transparentnode.Wire': 'manufacturing', 'transparentnode.Fabricator': 'manufacturing',
    'item.BasicComponents': 'manufacturing', 'item.WireMachine': 'manufacturing',
    'transparentnode.DcDc': 'electronics', 'transparentnode.VariableDcDc': 'electronics',
    'transparentnode.LegacyDcDc': 'electronics', 'transparentnode.OneWayDcDc': 'electronics',
    'transparentnode.powercapacitor': 'electronics', 'transparentnode.powerinductor': 'electronics',
    'transparentnode.LargeRheostat': 'electronics', 'sixnode.Power': 'electronics',
    'sixnode.VariableInductor': 'electronics', 'simplenode.energyconverter': 'electronics',
    'transparentnode.renewable': 'renewables', 'transparentnode.solarpanel': 'renewables',
    'transparentnode.waterturbine': 'renewables', 'transparentnode.windturbine': 'renewables',
    'transparentnode.offgrid': 'storage', 'transparentnode.ups': 'storage',
    'transparentnode.standby': 'storage', 'transparentnode.battery': 'storage',
    'transparentnode.FuelGenerator': 'storage', 'entity': 'entities', 'railroad': 'entities',
    'item': 'tools', 'falstad': 'tools', 'packets': 'network', 'network': 'network',
    'PacketHandler': 'network', 'ServerKeyHandler': 'network', 'server': 'network',
    'eventhandlers': 'network', 'client': 'rendering', 'sound': 'rendering',
    'gui': 'gui', 'GuiHandler': 'gui', 'craft': 'resources', 'datagen': 'resources',
    'i18n': 'resources', 'wiki': 'world-content', 'worldgen': 'world-content',
    'ore': 'world-content', 'block': 'world-content', 'Achievements': 'world-content',
    'mqtt': 'integrations', 'integration': 'integrations', 'hardware': 'integrations',
    'api': 'integrations', 'sixnode.modbusrtu': 'integrations',
    'sixnode.mqtt': 'integrations', 'sixnode.rs485': 'integrations',
    'transparentnode.computercraftio': 'integrations', 'simplenode.computerprobe': 'integrations',
    'devtest': 'verification', 'compat': 'build-compat', 'config': 'build-compat',
    'Eln': 'registry', 'Other': 'registry', 'sixnode': 'classic',
    'transparentnode': 'classic', 'simplenode': 'classic',
}


def rev(root):
    return subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=root, text=True).strip()


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stream(identity):
    name = identity.removeprefix('mods.eln.')
    for prefix in sorted(RULES, key=len, reverse=True):
        if name.startswith(prefix):
            return RULES[prefix]
    return 'review-remainder'


def source_files(root):
    rows = {}
    for language in ('java', 'kotlin'):
        base = root / 'src/main' / language
        for path in sorted(base.rglob('*')):
            if path.suffix not in ('.java', '.kt'):
                continue
            match = re.search(r'^package\s+([\w.]+)', path.read_text(errors='replace'), re.M)
            identity = (match[1] + '.' + path.stem) if match else '.'.join(path.relative_to(base).with_suffix('').parts)
            if identity in rows:
                raise ValueError('Duplicate identity: ' + identity)
            rows[identity] = path
    return rows


def write_tsv(path, rows, columns):
    with path.open('w', newline='') as file:
        writer = csv.DictWriter(file, fieldnames=columns, delimiter='\t', lineterminator='\n')
        writer.writeheader()
        writer.writerows(rows)


def run(source, target, output):
    config = json.loads((target / 'docs/forkpatch-port/completion-workstreams.json').read_text())
    if rev(source) != config['sourceCommit']:
        raise ValueError('Source revision differs from the reviewed pin')
    if subprocess.check_output(['git', 'status', '--porcelain'], cwd=source, text=True).strip():
        raise ValueError('Source checkout has edits; review the pinned snapshot before generating')
    workstreams = {w['id']: w for w in config['workstreams']}
    for work in workstreams.values():
        assert all(dep in workstreams for dep in work['dependsOn']), work['id']
        assert all((target / 'docs/forkpatch-port' / p).is_file() for p in work['evidence']), work['id']
    def visit(key, active):
        if key in active:
            raise ValueError('Dependency cycle: ' + ' -> '.join(active + [key]))
        for dependency in workstreams[key]['dependsOn']:
            visit(dependency, active + [key])
    for key in workstreams:
        visit(key, [])
    imports = defaultdict(set)
    for row in json.loads((target / 'docs/forkpatch-port/imports.json').read_text()):
        if not (source / row['source']).is_file() or not (target / row['target']).is_file():
            raise ValueError('Broken import reference: ' + str(row))
        imports[row['source']].add(row['target'])
    incoming, native = source_files(source), source_files(target)
    patterns = [p.strip() for p in (source / 'tools/port/include-1.21.txt').read_text().splitlines() if p.strip() and not p.lstrip().startswith('#')]
    includes = [p for p in patterns if not p.startswith('!')]
    excludes = [p[1:] for p in patterns if p.startswith('!')]
    source_rows, grouped = [], defaultdict(list)
    for identity, path in sorted(incoming.items()):
        relative = path.relative_to(source).as_posix()
        package_path = '/'.join(Path(relative).parts[3:])
        active = any(fnmatch.fnmatchcase(package_path, p) for p in includes) and not any(fnmatch.fnmatchcase(package_path, p) for p in excludes)
        match = native.get(identity)
        digest = sha(path)
        observation = 'no-same-identity-candidate' if match is None else ('same-identity-exact-bytes' if digest == sha(match) else 'same-identity-different-bytes')
        key = stream(identity)
        assert key in workstreams
        row = dict(workstream=key, activeInSourceBuild=str(active).lower(), identity=identity, source=relative, sourceSha256=digest, sameIdentityTarget=match.relative_to(target).as_posix() if match else '', targetSha256=sha(match) if match else '', observation=observation, recordedAdapterTargets=';'.join(sorted(imports[relative])), review='open-behavior-review' if active else 'source-excluded-review-intent')
        source_rows.append(row)
        grouped[key].append(row)
    output.mkdir(parents=True, exist_ok=True)
    write_tsv(output / 'completion-source-audit.tsv', source_rows, list(source_rows[0]))
    resource_rows = []
    for path in sorted((source / 'src/main/resources').rglob('*')):
        if path.is_file():
            relative = path.relative_to(source).as_posix()
            other = target / relative
            resource_rows.append(dict(source=relative, sourceSha256=sha(path), samePathTarget=relative if other.is_file() else '', observation=('same-path-exact-bytes' if sha(path)==sha(other) else 'same-path-different-bytes') if other.is_file() else 'no-same-path-candidate', recordedAdapterTargets=';'.join(sorted(imports[relative])), review='open-reference-and-runtime-review'))
    write_tsv(output / 'completion-resource-audit.tsv', resource_rows, list(resource_rows[0]))
    test_rows = []
    test_patterns = [p.strip() for p in (source / 'tools/port/include-1.21-test.txt').read_text().splitlines() if p.strip() and not p.lstrip().startswith('#')]
    for path in sorted((source / 'src/test').rglob('*')):
        if path.is_file():
            relative = path.relative_to(source).as_posix()
            package_path = '/'.join(Path(relative).parts[3:])
            selected = any(fnmatch.fnmatchcase(package_path, p) for p in test_patterns if not p.startswith('!')) and not any(fnmatch.fnmatchcase(package_path, p[1:]) for p in test_patterns if p.startswith('!'))
            test_rows.append(dict(source=relative, sourceSha256=sha(path), selectedBySourceIncludePatterns=str(selected).lower(), samePathTarget=relative if (target / relative).is_file() else '', review='map-independent-oracle-and-native-campaign'))
    write_tsv(output / 'completion-test-audit.tsv', test_rows, list(test_rows[0]))
    tracked = set(subprocess.check_output(['git', 'ls-files', '-z'], cwd=source).decode().rstrip('\0').split('\0'))
    classified = {row['source'] for rows in (source_rows, resource_rows, test_rows) for row in rows}
    support_rows = []
    for relative in sorted(tracked - classified):
        if relative.startswith('src/generated/resources/'):
            kind, key, review = 'generated-runtime-data', 'resources', 'review-generator-inputs-and-regenerate-native-equivalent'
        elif relative.startswith(('.github/', 'script/', 'tools/')):
            kind, key, review = 'tooling-and-campaigns', 'verification', 'map-build-export-and-independent-source-oracle-tools'
        elif relative.startswith('artwork/'):
            kind, key, review = 'original-artwork', 'resources', 'retain-provenance-and-export-required-runtime-assets'
        elif relative.startswith('docs/') or relative.endswith('.md'):
            kind, key, review = 'documentation-reference', 'verification', 'review-design-evidence-license-and-handoff-references'
        else:
            kind, key, review = 'build-and-repository-support', 'build-compat', 'review-platform-contract-do-not-copy-source-pins'
        other = target / relative
        support_rows.append(dict(source=relative, sourceSha256=sha(source/relative), category=kind, workstream=key, samePathTarget=relative if other.is_file() else '', review=review))
    assert classified | {r['source'] for r in support_rows} == tracked, 'Tracked source coverage is incomplete'
    assert not (classified - tracked), 'Unexpected nontracked source inputs'
    write_tsv(output / 'completion-support-audit.tsv', support_rows, list(support_rows[0]))
    # A searchable aid for literal names, not an execution-derived registry export.
    names = defaultdict(set)
    for path in incoming.values():
        text = path.read_text(errors='replace')
        for name in re.findall(r'(?:I18N\.)?TR_NAME\(\s*(?:I18N\.)?Type\.(?:NONE|ENTITY|ITEM|BLOCK)\s*,\s*"([^"\n]+)"', text):
            names[name].add(path.relative_to(source).as_posix())
    ids = json.loads((target / 'src/main/resources/eln-ids.json').read_text())
    native_names = defaultdict(list)
    for key, row in ids.items():
        native_names[row['display']].append(key)
    name_rows = [dict(sourceLiteralName=name, sourceFiles=';'.join(sorted(paths)), nativeExactDisplayKeys=';'.join(sorted(native_names[name])), review='runtime-registration-and-semantic-review') for name, paths in sorted(names.items())]
    write_tsv(output / 'completion-name-audit.tsv', name_rows, list(name_rows[0]))
    lighting = lighting_matrix(source, target, output, ids)
    fingerprint = hashlib.sha256('\n'.join(f'{key}\t{sha(path)}' for key, path in sorted(native.items())).encode()).hexdigest()
    summary = dict(sourceCommit=rev(source), targetObservedCommit=rev(target), targetMainSourceFingerprint=fingerprint,
        meaning='Exhaustive file/name observations within the listed directories; no row certifies semantic parity or feature completion.',
        mainSourceFiles=len(source_rows), sourceBuildActive=sum(r['activeInSourceBuild']=='true' for r in source_rows), sourceBuildExcluded=sum(r['activeInSourceBuild']=='false' for r in source_rows), observations=dict(Counter(r['observation'] for r in source_rows)),
        trackedSourceFiles=len(tracked), supportFiles=len(support_rows), supportCategories=dict(Counter(r['category'] for r in support_rows)), allTrackedFilesAccountedFor=True, sourceResources=len(resource_rows), resourceObservations=dict(Counter(r['observation'] for r in resource_rows)), sourceTestFiles=len(test_rows), testsSelectedBySourcePatterns=sum(r['selectedBySourceIncludePatterns']=='true' for r in test_rows), literalSourceNames=len(name_rows),
        workstreamCount=len(workstreams), workstreams=[dict(id=w['id'], title=w['title'], state=w['state'], sourceFiles=len(grouped[w['id']]), sourceBuildActive=sum(r['activeInSourceBuild']=='true' for r in grouped[w['id']]), dependsOn=w['dependsOn']) for w in config['workstreams']], lighting=lighting,
        excludedScope='Other repositories/addons and unreferenced assets are not assumed required; build tools, source runtime catalog exports, optional dependencies and release campaigns remain explicit tasks in the plan.')
    (output / 'completion-audit.json').write_text(json.dumps(summary, indent=2)+'\n')
    render_plan(config, summary, grouped, output)
    return summary


def lighting_matrix(source, target, output, ids):
    source_path = source / 'src/main/kotlin/mods/eln/sixnode/lampsocket/DecorativeLampStyle.kt'
    text = source_path.read_text()
    starts = list(re.finditer(r'^    ([A-Z][A-Z_0-9]*)\("', text, re.M))
    records = []
    for index, start in enumerate(starts):
        entry = text[start.start():starts[index+1].start() if index+1<len(starts) else len(text)]
        match = re.match(r'    ([A-Z_0-9]+)\("([^"]+)",\s*I18N.TR_NAME\(I18N.Type.NONE,\s*"([^"]+)"\),\s*(\d+),\s*([.\d]+),\s*(\d+),', entry)
        if not match:
            raise ValueError('Cannot parse source style: '+entry[:160])
        symbol, model, name, bulbs, depth, distance = match.groups()
        lookup = 4116+index if index<30 else (4352+index-30 if index<88 else 4480+index-88)
        native = ids.get(f'eln:six_node@{lookup}')
        registered = native is not None and native['display']==name and native['descriptor']=='DecorativeLampDescriptor'
        needs = (['implemented adapter; shared parity, partial-support and technology/configuration review'] if registered else ['fixed data, exact placement/clearance, recipe/assets and shared parity checks'])
        if symbol.startswith('FAN_'): needs.append('fan motor/rotor/airflow and hazard rules')
        if symbol=='MOTION': needs.append('motion modes, loaded entity/LOS sampling and 30 s hold')
        if symbol=='WAREHOUSE_HIGH_BAY': needs.append('adjustable suspension, moved light/clearance and larger controls')
        if symbol.startswith('POOL_'): needs.append('water-preserving light leases and submerged picking')
        if symbol.startswith(('CHRISTMAS_','HALLOWEEN_','PARTY_')): needs.append('source seasonal/party palette and emissive-part rules')
        if symbol.startswith(('LUXURY_','PARKING_','STREET_','SIDEWALK_')) or symbol=='PARKING': needs.append('large/tall reservation, picking and render reach')
        if symbol in {'DESK','TABLE_DRUM','TABLE_BANKER','TABLE_TASK','WINDOW_CANDLE','HALLOWEEN_JACK','DECK_POST_CAP'}: needs.append('portable/carry and cord ownership')
        if symbol.startswith(('DOT','PARKING','STREET_')) or symbol in {'POT_GIMBAL','WAREHOUSE','HEAVY_FLOOD','RIG_FLOOD','EAVE_TWIN','AREA_FLOOD','PARTY_WASH'}: needs.append('per-head aim; parking/street axial pan where applicable')
        if symbol.startswith('SIDEWALK_'): needs.append('review exact source aiming helper before registering')
        model_present = (target / f'src/main/resources/assets/eln/model/{model}').is_dir()
        records.append(dict(symbol=symbol, sourceOrdinal=index, lookup=lookup, name=name, bulbs=int(bulbs), depth=float(depth), range=int(distance), nativeAdapter=registered, model=model, targetIdentityAtSourceLookup=native['display'] if native else None, modelDirectoryPresent=model_present, minimumWork=needs))
    native_text=(target/'src/main/kotlin/mods/eln/sixnode/lampsocket/DecorativeLampStyle.kt').read_text()
    native_symbols=re.findall(r'^    ([A-Z][A-Z_0-9]*)\(\d+,',native_text,re.M)
    assert {r['symbol'] for r in records if r['nativeAdapter']}==set(native_symbols)
    assert len(records)==108 and sum(r['nativeAdapter'] for r in records)==21
    lines=['# Decorative lighting and bulb completion matrix','',
        'Pinned source: `'+rev(source)+'`. The source enum order supplies the ordinals below; the native manifest supplies registration observations. Model-directory presence alone does not establish drawing, picking or behavior.', '',
        '**Before appending styles:** extend the current first-range lookup helper. Source ordinals 0–29 map to 4116–4145; 30–87 map to 4352–4409; 88–107 map to 4480–4499. Using `4116 + sourceOrdinal` for later styles would collide with native lamp supplies, including 4160. Use the source ordinal, not the ordinal of a smaller native subset.', '',
        'The first 21 adapters retain shared cross-cutting review work. Every other row is unregistered; minimum work below is a starting contract, not a claim that other source rules can be omitted.', '',
        '| Source lookup (six-node) | Symbol / fixture | Bulbs / beam range | Model asset path | Native state | Minimum work |','|---|---|---|---|---|---|']
    for r in records:
        state='Adapter registered; shared checks remain' if r['nativeAdapter'] else 'Unregistered'
        if not r['nativeAdapter'] and r['targetIdentityAtSourceLookup']:state+='; occupied by '+r['targetIdentityAtSourceLookup']
        lines.append(f"| {r['lookup']} | `{r['symbol']}` — {r['name']} | {r['bulbs']} / {r['range']} blocks | `{r['model']}` ({'native files present' if r['modelDirectoryPresent'] else 'source-only at this path'}) | {state} | {'; '.join(r['minimumWork'])} |")
    lines += ['', '## Shared implementation gates','',
        '- Preserve actual parallel bulb demand and per-bulb wear. New motors/controls need their own real electrical demand; a visible rotor does not supply fan behavior.',
        '- Aim must transform both mesh and emitted light using the same pivot and side/front frame. Allocate nonconflicting native events: source motion event 4 collides with native channel event 4.',
        '- Match exact source place-direction and ghost rules. Strips and pool fittings do not all share a ceiling-only mounting policy. Extend render reach, selection and cached ghost changes for large or moving fixtures.',
        '- The existing fixed panel is 176×166; source aimed panels use 176×242 and warehouse suspension uses 176×198. Adapt server menus and controls together while preserving overlap/slot checks.',
        '- Air-only native light blocks need an adapter for source waterlogged leases, source-water preservation and expiry before underwater fixtures can have full lighting behavior.',
        '- Compare source and native lit/unlit reference views, partial bulb stacks, all facings, obstruction/unload, automation and actual paid source power. Do not register fans, motion or suspension as inert static lamps.', '', '## Source bulb identities','',
        'These are the 18 source registrations, not replacements for established 50/200 V native bulbs. The two LED adapters are implemented at 302/303; the remaining source ratings/technologies require mapping and source behavior/configuration review. Native aging and the source per-technology system still differ.', '',
        '| Source shared lookup | Source bulb | Exact-display native key | Identity currently at source lookup |','|---|---|---|---|']
    item_text=(source/'src/main/kotlin/mods/eln/registration/ItemRegistration.kt').read_text()
    at=item_text.index('private fun registerLampItem');end=item_text.index('\n    private fun ',at+10)
    bulbs=re.findall(r'subId = (\d+)[\s\S]*?TR_NAME\(I18N.Type.NONE,\s*"([^"]+)"',item_text[at:end])
    assert len(bulbs)==18
    for subid,name in bulbs:
        lookup=256+int(subid);matches=[k for k,v in ids.items() if v['display']==name];occupied=ids.get(f'eln:shared_item@{lookup}')
        lines.append(f"| {lookup} | {name} | {', '.join(matches) or 'None observed'} | {occupied['display'] if occupied else 'No native identity at this lookup'} |")
    lines += ['', 'Use [the completion plan](port-completion-plan.md#lighting) for dependencies and [current lighting evidence](decorative-lighting-validation.json) for the implemented scope. This table does not replace source registration or a runtime catalog export.', '']
    (output/'lighting-port-matrix.md').write_text('\n'.join(lines))
    return dict(sourceStyles=len(records),nativeAdapters=sum(r['nativeAdapter'] for r in records),unregistered=sum(not r['nativeAdapter'] for r in records),sourceBulbRegistrations=len(bulbs))


def render_plan(config, summary, grouped, output):
    lines=['# ForkPatch 3.0.77 → NeoForge 26.3 completion plan','',
        'Source pin: `'+summary['sourceCommit']+'`. Target reviewed: `'+summary['targetObservedCommit']+'`. This is a work breakdown for the whole fork, including shared inherited behavior, assets, optional systems and verification. It does not certify that every implementation has already received a line-by-line semantic review.', '',
        'Every source main file is assigned to an open review stream. Same-name classes, byte-identical files, model directories and import-ledger links are useful evidence locations; none alone proves a feature is ported. Source-excluded scaffolding needs an explicit retain/exclude decision, not automatic registration. Conditional integration status preserves source feature gates and does not authorize silently dropping active source features.', '',
        '## What a device port needs','',
        'An existing model saves mesh-creation work. A working 26.3 device also needs stable registration, a native electrical/mechanical/fluid connection, real material/energy costs, coherent placement/collision/rendering, an authoritative menu/packet path, preserved save/item state and paid recipes. These are separate checks; copying a model or compiling a class does not finish them.', '',
        'Simple fixed fixtures can reuse the new shared lighting adapter. Adjustable heads, fan motors, motion sensors, underwater leases and movable suspension add behavior that the fixed adapter does not provide. Likewise, existing native machines should keep their validated simulation and receive the actual source behavior changes, rather than being rewritten solely to match source filenames.', '',
        'For a first implementation slice, resolve the later lighting ID ranges and static variant data, then add independent aim/motion/water/fan/suspension adapters. In parallel, shared mounting, panel channels, portable ownership and room climate/fluid services unlock larger appliance groups. The workstreams below identify the dependencies and tests for each.', '',
        '## Audit coverage','',
        f"- **{summary['mainSourceFiles']} main Java/Kotlin files**: {summary['sourceBuildActive']} selected by the pinned source include list and {summary['sourceBuildExcluded']} excluded.",
        f"- **{summary['sourceResources']} main resource files**, **{summary['sourceTestFiles']} source test files** and **{summary['literalSourceNames']} distinct literal source names** have searchable review indexes.",
        f"- **All {summary['trackedSourceFiles']} tracked source-repository files are accounted for**, including {summary['supportFiles']} generated-data/tooling/artwork/reference/build files outside those directories. In particular, {summary['supportCategories'].get('generated-runtime-data',0)} generated runtime data files require generator/native-output review. This is coverage, not a count of things to copy.",
        f"- **{summary['workstreamCount']} workstreams** below define existing foundations, remaining implementation, dependencies and acceptance checks.",
        '- File/name observations are **not missing-feature counts**. Renamed/split adapters can have no same-name candidate; byte-identical helpers still need runtime integration checks. Full migration is new cross-cutting work and has no assigned incoming source file.',
        '- [Source review rows](completion-source-audit.tsv): exact source hashes, same-identity candidates, explicit adapted targets, active/excluded source status and assigned stream.',
        '- [Resource review rows](completion-resource-audit.tsv): exact-path byte observations and recorded renamed/adapted targets. Unreferenced assets are not automatically required registrations.',
        '- [Test/oracle review rows](completion-test-audit.tsv): the source test include-pattern observation and any exact-path target. Different test names require semantic/oracle mapping.',
        '- [Generated data and support review rows](completion-support-audit.tsv): every other tracked source path, including generated runtime content, workflows, scripts, artwork, license/documentation and build contracts.',
        '- [Literal-name review rows](completion-name-audit.tsv): search aid for named content. Loop-generated/dynamic names, source feature gates and entity identities still require actual runtime registration export.',
        '- [Lighting matrix](lighting-port-matrix.md): all 108 decorative fixtures, their source lookups, minimum special adapters and all 18 source bulb registrations.',
        '- [Audit summary](completion-audit.json) and [curated task definitions](completion-workstreams.json). Existing [adapter backlog/evidence](remaining-work.md) remains the entry point for completed checkpoints.', '',
        '## Recommended implementation order','',
        'Resolve identities, mounting/ownership and semantic solver differences first. Continue fixed lighting variants while building the aim/motion/underwater/fan/suspension adapters. Portable carry and the generalized panel unlock the remaining appliance families. Room climate/fluid services precede cooling and growing behavior. Road vehicles reuse pack/charger foundations but still require entities and controls. Run GUI, assets, network, migration and source-oracle reviews alongside each family; finish loaded-world and release campaigns after the intended scope is implemented.', '',
        'Existing native save compatibility is required throughout. Full source-world conversion is a separate deliverable; do not block all feature development on it or claim native save round trips implement it.', '',
        '| Stream | Current assessment | Source files | Prerequisites |','|---|---|---:|---|']
    for work in config['workstreams']:
        lines.append(f"| [{work['title']}](#{work['id']}) | {work['state']} | {len(grouped[work['id']])} | {', '.join(work['dependsOn']) or 'Identity baseline'} |")
    for work in config['workstreams']:
        lines += ['', '<a id="'+work['id']+'"></a>', '## '+work['title'], '', '**Assessment:** '+work['state']+'. **Prerequisites:** '+(', '.join(work['dependsOn']) or 'none beyond the pinned source/native baseline')+'.', '', '**Reuse:** '+work['reuse'], '', '**Needed:**', '']
        lines += ['- '+text for text in work['needed']]
        lines += ['', '**Completion checks:**', ''] + ['- '+text for text in work['acceptance']]
        source_rows=grouped[work['id']]
        if source_rows:
            # Prefer owner/descriptor/service entry points over client checks and small status carriers.
            ranked=sorted(source_rows,key=lambda r:(next((i for i,words in enumerate((('Registration',),('Type','Kind','Style'),('Element',),('Manager','Support','Descriptor'))) if any(word in r['identity'] for word in words)),4),r['source']))[:4]
            links=['['+Path(r['source']).name+'](https://github.com/reamnet/ELN1.21.1ForkPatch/blob/'+config['sourceCommit']+'/'+r['source']+')' for r in ranked]
            lines += ['', '**Source entry points:** '+', '.join(links)+'. Filter `'+work['id']+'` in [the full source audit](completion-source-audit.tsv) for every assigned file.']
        if work['evidence']:
            lines += ['', '**Existing review/evidence:** '+', '.join('['+name+']('+name+')' for name in work['evidence'])+'. Historical counts in those reports apply to their recorded commits.']
    lines += ['', '## Handoff and evidence requirements','',
        'For every implementation batch record: pinned source files/hashes, source behavior and native counterpart, explicit identity/recipe decisions, changed simulation/ownership/render/menu paths, tests with independent expected results, runtime captures and remaining limitations. Keep old native IDs, goldens, tolerances and skips intact. Preserve exact component/condition, energy/heat/material ledgers, cached matrices and resident-only queries.', '',
        'Use the established full JDKs outside tracked files: JDK 21 for the pinned source and JDK 25 for the target. Keep both projects’ pinned Gradle configuration. Do not compile changed main classes while a client/test JVM reads the same build outputs. Commit code first, assemble from that clean code commit, verify packaged class/resource bytes and record its marker/checksum in an evidence-only follow-up.', '',
        'The current production evidence is [the lighting checkpoint](decorative-lighting-validation.json): 690 root passes, three existing optional preview skips, 15 CAD passes, and 1,310/1,312 native passes with all 846 ForkPatch cases passing. Only the two historical goldens fail; [their exact delta](decorative-lighting-golden-delta.json) remains open. This documentation audit does not rerun or extend that runtime claim.', '',
        'Regenerate the indexes after a feature batch and review status manually; the generator never marks a stream complete:', '',
        '```sh', 'python3 tools/port/plan_completion.py --source ../ELN1.21.1ForkPatch', '```', '',
        'Generated data, build/tool scripts and references in the support index still require the explicit tasks above; that file coverage does not turn them all into runtime port requirements. Optional dependency availability, external EAMedia code and source-world fixture exports require separate review. Hardware tests and behavioral review can reveal further deltas; those remain accountable work rather than an unsupported claim that all unknown issues are solved.', '']
    (output/'port-completion-plan.md').write_text('\n'.join(lines))


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source',type=Path,required=True)
    parser.add_argument('--target',type=Path,default=Path('.'))
    parser.add_argument('--output-dir',type=Path,default=Path('docs/forkpatch-port'))
    args=parser.parse_args()
    result=run(args.source.resolve(),args.target.resolve(),args.output_dir.resolve())
    print(json.dumps({key:result[key] for key in ('sourceCommit','targetObservedCommit','mainSourceFiles','sourceBuildActive','sourceBuildExcluded','sourceResources','sourceTestFiles','trackedSourceFiles','supportFiles','literalSourceNames','workstreamCount','lighting')},indent=2))


if __name__=='__main__':
    main()
