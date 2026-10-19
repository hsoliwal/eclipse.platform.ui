#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Bounded real SDK/ECJ proof. All product source checkouts remain read-only."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import time
import xml.etree.ElementTree as ET

CRATE = Path(__file__).resolve().parents[1]
PINS = json.loads((CRATE / 'SOURCE_PINS.json').read_text())
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--ui-root', required=True, type=Path)
parser.add_argument('--swt-root', required=True, type=Path)
parser.add_argument('--java-home', required=True, type=Path)
parser.add_argument('--maven', required=True, type=Path)
parser.add_argument('--maven-cache', required=True, type=Path)
parser.add_argument('--ecj', required=True, type=Path)
parser.add_argument('--eclipse-deps', required=True, type=Path)
parser.add_argument('--phase', choices=['baseline', 'sdk', 'candidate', 'all'], default='all')
args = parser.parse_args()
WORK = CRATE / 'target/qualification'
EVIDENCE = CRATE / 'evidence/qualification'
WORK.mkdir(parents=True, exist_ok=True)
EVIDENCE.mkdir(parents=True, exist_ok=True)
JAVA = str(args.java_home / 'bin/java')
JAVAC = str(args.java_home / 'bin/javac')
ENV = os.environ.copy()
ENV['JAVA_HOME'] = str(args.java_home)
ENV['TZ'] = 'UTC'

def sha(data):
    return hashlib.sha256(data).hexdigest()

def blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()

def json_write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')

def git(repository, *arguments):
    return subprocess.check_output(['git', '-C', str(repository), *arguments], env=ENV)

def fresh(path):
    assert path.resolve().is_relative_to(WORK.resolve())
    if path.exists():
        shutil.rmtree(path)
    path.mkdir(parents=True)
    return path

def run(name, command, cwd):
    log = EVIDENCE / (name + '.log')
    if log.exists():
        previous = EVIDENCE / 'earlier-attempts' / (name + '-' + sha(log.read_bytes())[:16] + '.log')
        previous.parent.mkdir(exist_ok=True)
        previous.write_bytes(log.read_bytes())
    started = time.monotonic()
    with log.open('wb') as output:
        completed = subprocess.run([str(part) for part in command], cwd=cwd, env=ENV,
                                   stdout=output, stderr=subprocess.STDOUT)
    receipt = {'command': [str(part) for part in command], 'cwd': str(cwd),
               'exit': completed.returncode, 'wall_seconds': round(time.monotonic() - started, 3),
               'log': str(log.relative_to(CRATE)), 'log_sha256': sha(log.read_bytes())}
    json_write(EVIDENCE / (name + '.json'), receipt)
    print(json.dumps({'stage': name, 'exit': completed.returncode,
                      'wall_seconds': receipt['wall_seconds']}), flush=True)
    return receipt

def verify_owners():
    canonical = CRATE.parents[2]
    for owner in PINS['owners']:
        data = (canonical / owner['path']).read_bytes()
        assert sha(data) == owner['sha256'] and blob(data) == owner['git_blob'], owner['path']

def inventory(repository, revision, prefix, selected):
    assert git(repository, 'rev-parse', 'HEAD').decode().strip() == revision
    entries = {}
    for row in git(repository, 'ls-tree', '-rz', revision, '--', prefix).split(b'\0'):
        if not row:
            continue
        metadata, path = row.split(b'\t', 1)
        mode, kind, identity = metadata.decode().split()
        if kind == 'blob':
            entries[path.decode()] = (mode, identity)
    result = []
    for relative in selected:
        data = (repository / relative).read_bytes()
        mode, expected = entries[relative]
        assert mode == '100644' and blob(data) == expected, relative
        result.append((relative, expected, sha(data), len(data)))
    return result

def write_inventory(name, rows):
    output = '# path\tgit-blob\tsha256\tbytes\n' + ''.join('\t'.join(map(str, row)) + '\n' for row in rows)
    (EVIDENCE / name).write_text(output)

def argument_file(name, paths):
    path = WORK / name
    path.write_text(''.join('"' + str(item).replace('\\', '\\\\').replace('"', '\\"') + '"\n' for item in paths))
    return path

verify_owners()
assert sha(args.ecj.read_bytes()) == '74c93172475fee0cd4e6eefb44233516dd40525642da35d009146d97ea598229'
assert 'JAVA_VERSION="21.0.12.1"' in (args.java_home / 'release').read_text()
prefs = args.ui_root / PINS['unchanged_compiler_policy']['path']
assert sha(prefs.read_bytes()) == PINS['unchanged_compiler_policy']['sha256']
assert prefs.read_bytes() == (CRATE / 'proof/inputs/org.eclipse.jface.org.eclipse.jdt.core.prefs').read_bytes()
assert 'org.eclipse.jdt.core.compiler.problem.nonExternalizedStringLiteral=error' in prefs.read_text()

dependency_rows = json.loads((CRATE / 'proof/inputs/eclipse-dependencies.json').read_text())
dependencies = []
for row in dependency_rows:
    path = args.eclipse_deps / row['url'].rsplit('/', 1)[-1]
    assert path.is_file() and sha(path.read_bytes()) == row['sha256'], str(path)
    dependencies.append(path)
production_dependencies = [path for path in dependencies if not path.name.startswith('org.eclipse.jface-')]
swt_args = args.swt_root / 'docs/verification/tree-coordinate-visibility-20261017/gtk-source-files.args'
swt_paths = shlex.split(swt_args.read_text())
assert len(swt_paths) == 546 and len(set(swt_paths)) == 546
jface_paths = sorted(str(path.relative_to(args.ui_root))
                     for path in (args.ui_root / 'bundles/org.eclipse.jface/src').rglob('*.java'))
assert len(jface_paths) == 423
swt_before = inventory(args.swt_root, PINS['swt_commit'], 'bundles/org.eclipse.swt', swt_paths)
jface_before = inventory(args.ui_root, PINS['receiver_base_commit'], 'bundles/org.eclipse.jface/src', jface_paths)
write_inventory('swt-source-inputs.tsv', swt_before)
write_inventory('jface-source-inputs.tsv', jface_before)
json_write(EVIDENCE / 'context.json', {'ui_commit': PINS['receiver_base_commit'], 'swt_commit': PINS['swt_commit'],
           'swt_java_sources': len(swt_paths), 'jface_java_sources': len(jface_paths),
           'compiler_preferences_sha256': sha(prefs.read_bytes()), 'ecj_sha256': sha(args.ecj.read_bytes()),
           'java_release_sha256': sha((args.java_home / 'release').read_bytes()),
           'swt_source_argument_file_sha256': sha(swt_args.read_bytes()),
           'dependencies': dependency_rows, 'native_test_rerun': False, 'full_tycho_pde': 'NOT_RUN',
           'api_tools_rerun': 'NOT_RUN'})
swt_classes = WORK / 'swt-classes'
production_classpath = os.pathsep.join(map(str, [swt_classes, *production_dependencies]))
parser_classpath = os.pathsep.join(map(str, [swt_classes, *dependencies]))
(WORK / 'parser-classpath.txt').write_text(parser_classpath + '\n')

def ecj(name, sources, expected_errors):
    classes = fresh(WORK / (name + '-classes'))
    arguments = argument_file(name + '-sources.args', sources)
    xml = WORK / (name + '.xml')
    command = [JAVA, '-Xmx1g', '-jar', args.ecj, '-properties', prefs, '--release', '21',
               '-proc:none', '-encoding', 'UTF-8', '-g', '-classpath', production_classpath,
               '-d', classes, '-log', xml, '@' + str(arguments)]
    receipt = run(name, command, CRATE)
    root = ET.parse(xml).getroot()
    errors = [problem for problem in root.iter('problem') if problem.get('severity', '').lower() == 'error']
    warnings = [problem for problem in root.iter('problem') if problem.get('severity', '').lower() == 'warning']
    receipt.update({'errors': len(errors), 'warnings': len(warnings),
                    'diagnostic_xml_sha256': sha(xml.read_bytes()),
                    'diagnostic_xml_disposition': 'Generated under target; full text compiler log retained in evidence',
                    'error_details': [dict(problem.attrib) for problem in errors]})
    json_write(EVIDENCE / (name + '.json'), receipt)
    assert len(errors) == expected_errors, receipt
    assert (receipt['exit'] == 0) == (expected_errors == 0), receipt
    if expected_errors:
        assert all(problem.get('id') == 'NonExternalizedStringLiteral' for problem in errors), receipt
        assert sorted(int(problem.get('line')) for problem in errors) == [77, 79, 81, 83, 85, 87, 89, 91, 122, 135]
    return classes

if args.phase in {'baseline', 'all'}:
    receipt = run('swt-java-compile', [JAVAC, '-J-Xmx1g', '--release', '21', '-encoding', 'UTF-8',
                  '-d', fresh(swt_classes), '@' + str(swt_args)], args.swt_root)
    assert receipt['exit'] == 0, receipt
    ecj('ecj-baseline', [args.ui_root / path for path in jface_paths], 10)

if args.phase in {'sdk', 'all'}:
    assert swt_classes.is_dir() and any(swt_classes.rglob('*.class')), 'Run baseline phase first'
    output = CRATE / 'target/recipe-output'
    command = [args.maven, '-o', '-B', '-ntp', '-C', '-Dmaven.repo.local=' + str(args.maven_cache),
               '-f', CRATE / 'pom.xml', '-Dcanonical.classpath=' + parser_classpath,
               '-Dmaterialize.dir=' + str(output), 'test']
    receipt = run('recipe-sdk', command, CRATE)
    assert receipt['exit'] == 0, receipt
    report = CRATE / 'target/surefire-reports/TEST-com.synexia.rewrite.M3EclipseUiBuildFixesRecipeTest.xml'
    suite = ET.parse(report).getroot()
    counts = {key: int(suite.get(key)) for key in ['tests', 'failures', 'errors', 'skipped']}
    assert counts == {'tests': 6, 'failures': 0, 'errors': 0, 'skipped': 0}, counts
    (EVIDENCE / report.name).write_bytes(report.read_bytes())
    actual = sorted(str(path.relative_to(output)) for path in output.rglob('*') if path.is_file())
    assert actual == sorted(row['path'] for row in PINS['outputs']), actual
    generated = []
    for row in PINS['outputs']:
        data = (output / row['path']).read_bytes()
        assert sha(data) == row['after_sha256'] and blob(data) == row['after_git_blob'], row['path']
        generated.append({'path': row['path'], 'bytes': len(data), 'git_blob': blob(data), 'sha256': sha(data)})
    receipt.update({'kind': 'SDK_EXECUTION', 'junit': counts, 'outputs': generated,
                    'junit_xml_sha256': sha(report.read_bytes())})
    json_write(EVIDENCE / 'recipe-sdk.json', receipt)

if args.phase in {'candidate', 'all'}:
    output = CRATE / 'target/recipe-output'
    for row in PINS['outputs']:
        assert sha((output / row['path']).read_bytes()) == row['after_sha256']
    tree = next(row for row in PINS['outputs'] if row['kind'] == 'java')
    manifest = next(row for row in PINS['outputs'] if row['kind'] == 'text')
    before_lines = (args.ui_root / tree['path']).read_bytes().splitlines(keepends=True)
    after_lines = (output / tree['path']).read_bytes().splitlines(keepends=True)
    assert len(before_lines) == len(after_lines)
    changed = []
    for number, (before, after) in enumerate(zip(before_lines, after_lines), 1):
        if before != after:
            assert before.endswith(b'\n') and after == before[:-1] + b' //$NON-NLS-1$\n'
            changed.append(number)
    assert changed == [77, 79, 81, 83, 85, 87, 89, 91, 122, 135]
    before_manifest = (args.ui_root / manifest['path']).read_bytes()
    after_manifest = (output / manifest['path']).read_bytes()
    assert after_manifest == before_manifest.replace(b'Bundle-Version: 3.13.100.qualifier\n', b'Bundle-Version: 3.14.0.qualifier\n')
    json_write(EVIDENCE / 'exact-output-diff.json', {'nls_comment_lines': changed,
               'literal_or_executable_bytes_changed': False, 'line_count_unchanged': True,
               'manifest_only_change': 'Bundle-Version: 3.13.100.qualifier -> 3.14.0.qualifier',
               'bree_unchanged': 'JavaSE-21'})
    candidate_sources = [output / path if path == tree['path'] else args.ui_root / path for path in jface_paths]
    candidate_classes = ecj('ecj-candidate', candidate_sources, 0)
    bytecode = {}
    class_bytes = {}
    for label, source in [('before', args.ui_root / tree['path']), ('after', output / tree['path'])]:
        destination = fresh(WORK / ('javac-' + label + '-classes'))
        command = [JAVAC, '--release', '21', '-proc:none', '-encoding', 'UTF-8', '-g',
                   '-sourcepath', '', '-classpath', str(candidate_classes) + os.pathsep + production_classpath,
                   '-d', destination, source]
        receipt = run('javac-treeviewer-' + label, command, CRATE)
        assert receipt['exit'] == 0, receipt
        class_bytes[label] = {str(path.relative_to(destination)): path.read_bytes()
                              for path in sorted(destination.rglob('*.class'))}
        bytecode[label] = {path: sha(data) for path, data in class_bytes[label].items()}
        assert bytecode[label], 'No TreeViewer classfiles emitted'
    assert class_bytes['before'] == class_bytes['after'], 'Direct classfile byte comparison failed'
    assert bytecode['before'] == bytecode['after'], bytecode
    json_write(EVIDENCE / 'treeviewer-bytecode-identity.json', {'status': 'PASS',
               'comparison': 'byte-for-byte equality of every emitted class under identical javac/classpath/options',
               'class_count': len(bytecode['before']), 'classes': bytecode['before'],
               'before_source_sha256': tree['before_sha256'], 'after_source_sha256': tree['after_sha256'],
               'native_rerun': False})

assert swt_before == inventory(args.swt_root, PINS['swt_commit'], 'bundles/org.eclipse.swt', swt_paths)
assert jface_before == inventory(args.ui_root, PINS['receiver_base_commit'], 'bundles/org.eclipse.jface/src', jface_paths)
assert sha(prefs.read_bytes()) == PINS['unchanged_compiler_policy']['sha256']
verify_owners()
json_write(EVIDENCE / ('phase-' + args.phase + '-source-readback.json'),
           {'phase': args.phase, 'source_readback': 'UNCHANGED', 'ui_sources': 423, 'swt_sources': 546,
            'compiler_policy_unchanged': True, 'four_shared_owners_unchanged': True})
print(json.dumps({'phase': args.phase, 'completed': True}), flush=True)
