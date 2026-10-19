# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Scoped reproduction; reuses the preceding JFace recipe/qualification sequence."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, zipfile
parser=argparse.ArgumentParser()
parser.add_argument('phase',choices=['recipe','product'])
for option in ['work','jdk','maven','m2','deps']: parser.add_argument('--'+option,type=Path,required=True)
a=parser.parse_args(); crate=Path(__file__).resolve().parent.parent; work=a.work.resolve()
assert work != crate and crate not in work.parents and work not in crate.parents
work.mkdir(parents=True,exist_ok=True); evidence=work/'evidence'; evidence.mkdir(exist_ok=True)
env=os.environ.copy();env['JAVA_HOME']=str(a.jdk);env['PATH']=str(a.jdk/'bin')+os.pathsep+env['PATH']
def run(name,cmd):
    cmd=list(map(str,cmd)); log=evidence/(name+'.log')
    assert not log.exists(), 'immutable stage log already exists: '+name
    with log.open('w') as f: result=subprocess.run(cmd,cwd=work,env=env,stdout=f,stderr=subprocess.STDOUT)
    (evidence/(name+'.json')).write_text(json.dumps(dict(command=cmd,exit=result.returncode,logSha256=hashlib.sha256(log.read_bytes()).hexdigest()),indent=2)+'\n')
    print(name,result.returncode,'\n'.join(log.read_text().splitlines()[-12:]),flush=True)
    if result.returncode: raise SystemExit('HOLD '+name)
def lint(name,files):
    cp=os.pathsep.join(str(a.m2/p) for p in (crate/'proof/lint-classpath.txt').read_text().splitlines())
    run(name,['java','-cp',cp,'com.puppycrawl.tools.checkstyle.Main','-c',crate/'proof/checkstyle.xml',*files])
r=crate/'resources/com/synexia/rewrite/hash-pinned-java/m3-jface-packed-forwarding-20261019'
rows=[s.split('\t') for s in (r/'manifest.tsv').read_text().splitlines()]
inputs=crate/'proof/inputs'; source='bundles/org.eclipse.jface/src'
jars=[next(a.deps.glob(p+'*.jar')) for p in ['org.eclipse.jface_','org.eclipse.swt.gtk.linux.x86_64_','org.eclipse.core.commands_','org.eclipse.equinox.common_','org.eclipse.osgi_']]
unsigned=work/'unsigned-jface.jar'
if not unsigned.exists():
    with zipfile.ZipFile(jars[0]) as src,zipfile.ZipFile(unsigned,'w',zipfile.ZIP_DEFLATED) as dst:
        for item in src.infolist():
            if item.filename.upper().startswith('META-INF/') and item.filename.upper().endswith(('.SF','.RSA','.DSA','.EC')): continue
            dst.writestr(item,src.read(item.filename))
cp=os.pathsep.join(map(str,[unsigned]+jars[1:]+[work/'annotations']))
probes=[crate/'proof/JFaceArrayContracts.java',crate/'proof/JFacePackedContracts.java']
if a.phase=='recipe':
    lintdir=work/'lint';lintdir.mkdir()
    for _,_,after,template in rows:
        data=(r/template).read_bytes();assert hashlib.sha256(data).hexdigest()==after
        (lintdir/template[:-4]).write_bytes(data)
    for path,before,_,template in rows:
        if before!='ABSENT': assert hashlib.sha256((inputs/path).read_bytes()).hexdigest()==before
    annotations=list((crate/'proof/deps').rglob('*.java'))
    lint('01-lint',list(lintdir.glob('*.java'))+list(crate.glob('src/test/**/*.java'))+annotations+probes)
    run('02-annotation-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',work/'annotations',*annotations])
    sources=list((inputs/source).rglob('*.java'))
    run('03-baseline-compile',['javac','--release','21','-proc:none','-Xlint:all,-rawtypes,-unchecked,-deprecation','-cp',cp,'-d',work/'baseline-classes',*sources,*probes])
    command=[a.maven,'-o','-B','-ntp','-Dmaven.repo.local='+str(a.m2),'-f',crate/'pom.xml','-Dcanonical.classpath='+cp]
    run('04-recipe-compile',command+['test-compile'])
    run('05-recipe-sdk',command+['surefire:test','-Dmaterialize.dir='+str(work/'generated')])
else:
    assert json.loads((evidence/'05-recipe-sdk.json').read_text())['exit']==0
    generated=list((work/'generated').rglob('*.java'));assert len(generated)==len(rows)
    for path,_,after,_ in rows: assert hashlib.sha256((work/'generated'/path).read_bytes()).hexdigest()==after
    lint('06-product-lint',generated+probes)
    overlaid={p.relative_to(inputs/source).as_posix():p for p in (inputs/source).rglob('*.java')}
    overlaid.update({p.relative_to(work/'generated'/source).as_posix():p for p in generated})
    run('07-product-compile',['javac','--release','21','-proc:none','-Xlint:all,-rawtypes,-unchecked,-deprecation','-cp',cp,'-d',work/'candidate-classes',*overlaid.values(),*probes])
    for variant in ['baseline','candidate']:
        classpath=str(work/(variant+'-classes'))+os.pathsep+cp
        for name in ['JFaceArrayContracts','org.eclipse.jface.viewers.JFacePackedContracts']:
            run('08-'+variant+'-'+name,['java','-ea','-Xmx256m','-cp',classpath,name])
        run('09-'+variant+'-api',['javap','-public','-s','-classpath',classpath,*['org.eclipse.jface.viewers.'+n for n in ['CustomHashtable','ArrayContentProvider','ViewerFilter','ViewerComparator','TreePath','StructuredSelection','TreeSelection','StructuredViewer','AbstractTreeViewer','TreeViewer']]])
    for suffix in ['JFaceArrayContracts','org.eclipse.jface.viewers.JFacePackedContracts']:
        assert (evidence/('08-baseline-'+suffix+'.log')).read_bytes()==(evidence/('08-candidate-'+suffix+'.log')).read_bytes(),suffix+' differential mismatch'
    assert (evidence/'09-baseline-api.log').read_bytes()==(evidence/'09-candidate-api.log').read_bytes()
    (evidence/'10-parity.json').write_text(json.dumps(dict(traceParity=True,publicDescriptorsIdentical=True,generatedFiles=len(rows)))+'\n')
