#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Archive already-executed native observations and pin their genuine inputs.

This is a packaging operation. It does not run tests or manufacture JUnit XML.
"""
from pathlib import Path
import hashlib
import json
import re
import shlex
import shutil
import subprocess

root = Path('/tmp/eclipse-m3-20261018')
work = root / 'native-gate'
crate_rel = Path('synexia-openrewrite-recipes/recipe-crates/m3-jface-forwarding-gate-20261018')
crate = root / 'synexia' / crate_rel
proof = crate / 'proof/native'
evidence = crate / 'evidence/native'
proof.mkdir(parents=True, exist_ok=True)
evidence.mkdir(parents=True, exist_ok=True)

def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def blob(path):
    data = path.read_bytes()
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()

def copy(source, destination):
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, destination)
    assert sha(source) == sha(destination)

for name in ['pom.xml', 'run-native-gate.py', 'stage-native-packet.py']:
    copy(work / name, proof / name)
for name in ['gtk-java-sources.args', 'jface-java-sources.args']:
    copy(work / name, proof / name)
for name in ['native-green', 'native-leaf-fixture-failure']:
    for source in sorted((work / 'evidence' / name).iterdir()):
        if source.is_file():
            copy(source, evidence / name / source.name)
for name in ['gtk-java-compile.json', 'gtk-java-compile.log', 'jface-java-compile.json',
             'jface-java-compile.log', 'test-java-compile.json', 'test-java-compile.log']:
    copy(work / 'evidence' / name, evidence / 'compile' / name)
copy(work / 'native-handoff.json', evidence / 'native-handoff.json')
copy(root / 'diagnostics/jface-native-leaf-fixture-review.json',
     evidence / 'jface-native-leaf-fixture-review.json')
for name in ['toolchain-native-handoff.json', 'swt-native-gtk3/build-receipt.json',
             'swt-native-gtk3/source-collection.json', 'xvfb-source/build-receipt.json',
             'xvfb-source/configure-receipt.json', 'xvfb-source/source-acquisition.json',
             'xvfb-source/patches.json', 'xvfb-local-startup-receipt.json',
             'jdk25-acquisition.json', 'eclipse-deps/acquisition.json',
             'eclipse-deps/additional-acquisition.json']:
    copy(root / 'toolchain' / name, evidence / 'toolchain' / name)
copy(root / 'toolchain/native-bin/Xvfb-local', evidence / 'toolchain/Xvfb-local.txt')

repos = {
    'swt': (root / 'forks/eclipse.platform.swt', 'hsoliwal/eclipse.platform.swt',
            '422fb369629ba47fc5c5c6ddf01794b6d7c79f42'),
    'ui': (root / 'forks/eclipse.platform.ui', 'hsoliwal/eclipse.platform.ui',
           '16e944f83eec18d4cf609afd9ad9a937711211ce'),
}
indices = {}
for key, (repo, repository, commit) in repos.items():
    head = subprocess.run(['git', '-C', str(repo), 'rev-parse', 'HEAD'],
                          check=True, text=True, capture_output=True).stdout.strip()
    assert head == commit, (repository, head)
    data = subprocess.run(['git', '-C', str(repo), 'ls-files', '-s', '-z'],
                          check=True, capture_output=True).stdout
    indices[key] = {entry.split(b'\t', 1)[1].decode(): entry.split(b' ', 2)[1].decode()
                    for entry in data.split(b'\0') if entry}

def source_pin(path, repo_key, role):
    repo, repository, commit = repos[repo_key]
    assert indices[repo_key][str(path.relative_to(repo))] == blob(path), str(path)
    return {'repository': repository, 'commit': commit,
            'path': str(path.relative_to(repo)), 'sha256': sha(path),
            'gitBlob': blob(path), 'role': role}

sources = []
for argument_file, key in [('gtk-java-sources.args', 'swt'), ('jface-java-sources.args', 'ui')]:
    for line in (work / argument_file).read_text().splitlines():
        if line.strip():
            names = shlex.split(line)
            assert len(names) == 1
            sources.append(source_pin(Path(names[0]), key, 'production compiler input'))

test_sources = []
for line in (work / 'evidence/test-java-compile.log').read_text().splitlines():
    match = re.match(r'\[parsing started DirectoryFileObject\[(.+):(.+)\]\]', line)
    if match:
        path = Path(match.group(1)) / match.group(2)
        test_sources.append(source_pin(path, 'ui', 'unchanged original test compiler input'))
assert len(test_sources) == 14
generated = []
for name in ['M3TreeForwardingTest.java', 'VirtualLazyTreeViewerTest.java']:
    relative = Path('tests/org.eclipse.jface.tests/src/org/eclipse/jface/tests/viewers') / name
    path = root / 'ui-final-generated' / relative
    generated.append({'path': str(relative), 'sha256': sha(path), 'gitBlob': blob(path),
                      'role': 'actual five-target SDK output compiled for passing native gate'})

resources = []
for directory, repo_key in [('swt-classes', 'swt'), ('jface-classes', 'ui'), ('test-classes', 'ui')]:
    repo = repos[repo_key][0]
    paths = subprocess.run(['rg', '--files', str(repo)], check=True, text=True,
                           capture_output=True).stdout.splitlines()
    for path in sorted((work / directory).rglob('*')):
        if not path.is_file() or path.suffix == '.class':
            continue
        relative = str(path.relative_to(work / directory))
        candidates = [Path(candidate) for candidate in paths if candidate.endswith('/' + relative)]
        matches = [candidate for candidate in candidates if sha(candidate) == sha(path)]
        assert matches, (directory, relative)
        resources.append({'runtimeDirectory': directory, 'runtimePath': relative,
                          'sha256': sha(path), 'sourceCandidates':
                          [source_pin(candidate, repo_key, 'unchanged runtime resource') for candidate in matches]})

dependencies = []
for path in sorted((root / 'toolchain/eclipse-deps').glob('*.jar')):
    if path.name in {'org.eclipse.jface-3.40.0.jar', 'assertj-core-3.27.5.jar', 'ecj-3.46.100.jar'}:
        continue
    dependencies.append({'path': str(path.relative_to(root)), 'sha256': sha(path),
                         'bytes': path.stat().st_size,
                         'role': ('test compilation API only; runtime uses generated Commands jar'
                                  if path.name == 'org.eclipse.core.commands-3.13.0.jar'
                                  else 'genuine upstream compiler/runtime dependency')})
candidate = root / 'commands-authoring/target/org.eclipse.core.commands-m3-proof.jar'
dependencies.append({'path': str(candidate.relative_to(root)), 'sha256': sha(candidate),
                     'bytes': candidate.stat().st_size, 'role': 'actual generated Commands receiver at native runtime'})
native_libraries = [{'path': str(path.relative_to(root)), 'sha256': sha(path),
                     'bytes': path.stat().st_size}
                    for path in sorted((root / 'toolchain/swt-native-gtk3').glob('*.so'))]

pins = {'schema': 'M3_JFACE_NATIVE_GATE_PINS_V1', 'date': '2026-10-18',
        'scope': 'One real native GTK3 JFace method; no full Tycho/PDE/reactor or platform-matrix claim',
        'counts': {'swtJavaSources': 546, 'jfaceJavaSources': 423, 'testJavaSources': 15,
                   'testClasses': 31, 'passingUnchangedUpstreamTestSources': 13,
                   'passingGeneratedTestSources': 2, 'runtimeResources': len(resources)},
        'productionSources': sources, 'originalTestSources': test_sources,
        'passingGeneratedTestSources': generated, 'resources': resources,
        'dependencies': dependencies, 'nativeLibraries': native_libraries,
        'originalClassHashManifests':
        ['../../evidence/native/native-leaf-fixture-failure/' + name + '-input-hashes.json'
         for name in ['swt-classes', 'jface-classes', 'test-classes']],
        'passingClassDelta': '../../evidence/native/native-green/corrected-fixture-compile.json',
        'compilerCommands': ['../../evidence/native/compile/' + name + '-compile.json'
                            for name in ['gtk-java', 'jface-java', 'test-java']],
        'recordingNote': 'Original logs and JUnit XML are copied byte for byte from actual executions. This packaging script never invents test results.'}
assert len(sources) == 969
(proof / 'SOURCE_PINS.json').write_text(json.dumps(pins, indent=2) + '\n')

files = []
for folder in [proof, evidence]:
    for path in sorted(folder.rglob('*')):
        if path.is_file():
            assert path.suffix not in {'.jar', '.class', '.so', '.png', '.gif', '.zip'}
            files.append({'path': str(path.relative_to(root / 'synexia')), 'bytes': path.stat().st_size,
                          'sha256': sha(path), 'gitBlob': blob(path)})
(work / 'native-publication-files.json').write_text(json.dumps(files, indent=2) + '\n')
print(json.dumps({'files': len(files), 'bytes': sum(f['bytes'] for f in files),
                  'proof': str(proof), 'evidence': str(evidence),
                  'productionSources': len(sources), 'testSources': 15,
                  'resources': len(resources)}, indent=2))
