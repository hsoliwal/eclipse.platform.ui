# SPDX-License-Identifier: Apache-2.0
from pathlib import Path
import hashlib, json, os, shutil, subprocess, sys, zipfile

import argparse
parser=argparse.ArgumentParser(description='Reproduce the scoped JFace M3 recipe, behavior and live Equinox qualification')
parser.add_argument('phase',choices=['recipe','product','runtime','all'])
for option in ['work','jdk','maven','m2','deps','p2']:
    parser.add_argument('--'+option,required=True,type=Path)
args=parser.parse_args()
crate=Path(__file__).resolve().parent.parent
root=args.work.resolve()
if root==crate or root in crate.parents or crate in root.parents:
    raise SystemExit('Use a separate scratch directory for qualification output')
root.mkdir(parents=True,exist_ok=True)
evidence=root/'evidence'; evidence.mkdir(exist_ok=True)
jdk=args.jdk.resolve(); mvn=args.maven.resolve(); cache=args.m2.resolve(); deps=args.deps.resolve()
resources=crate/'resources/com/synexia/rewrite/hash-pinned-java/m3-jface-array-forwarding-20261017'
for row in (resources/'manifest.tsv').read_text().splitlines():
    target,before,after,template=row.split('\t')
    if before=='ABSENT': continue
    payload=(resources/('before-'+template)).read_bytes()
    assert hashlib.sha256(payload).hexdigest()==before
    destination=root/'before'/target
    assert destination.resolve().is_relative_to(root/'before')
    destination.parent.mkdir(parents=True,exist_ok=True)
    if destination.exists(): assert destination.read_bytes()==payload, 'baseline drift'
    else: destination.write_bytes(payload)
for source in (crate/'proof/inputs').rglob('*'):
    if source.is_file() and '.rsync-tmp' not in source.parts:
        destination=root/'before'/source.relative_to(crate/'proof/inputs')
        destination.parent.mkdir(parents=True,exist_ok=True)
        if destination.exists(): assert destination.read_bytes()==source.read_bytes(), 'integration input drift'
        else: destination.write_bytes(source.read_bytes())
if args.phase=='all':
    for phase in ['recipe','product','runtime']:
        command=[sys.executable,__file__,phase]
        for key in ['work','jdk','maven','m2','deps','p2']: command+=['--'+key,str(getattr(args,key))]
        subprocess.run(command,check=True)
    raise SystemExit(0)
names=['org.eclipse.jface_', 'org.eclipse.swt.gtk.linux.x86_64_', 'org.eclipse.core.commands_', 'org.eclipse.equinox.common_', 'org.eclipse.osgi_']
jars=[next(deps.glob(n+'*.jar')) for n in names]
env=os.environ.copy(); env['JAVA_HOME']=str(jdk); env['PATH']=str(jdk/'bin')+os.pathsep+env['PATH']
cp=os.pathsep.join(map(str,jars+[root/'annotation-classes']))
def run(name,cmd):
    cmd=list(map(str,cmd)); log=evidence/(name+'.log')
    with log.open('w') as f: process=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,env=env,cwd=root)
    (evidence/(name+'.json')).write_text(json.dumps(dict(stage=name,command=cmd,exit=process.returncode,logSha256=hashlib.sha256(log.read_bytes()).hexdigest()),indent=2)+'\n')
    print(name,process.returncode,'\n'.join(log.read_text().splitlines()[-10:]),flush=True)
    if process.returncode: raise SystemExit('HOLD '+name)
def lint(name,files):
    cmd=['java','-cp',os.pathsep.join(str(cache/p) for p in (crate/'proof/lint-classpath.txt').read_text().splitlines()),'com.puppycrawl.tools.checkstyle.Main','-c',str(crate/'proof/checkstyle.xml')]
    cmd[5]=str(crate/'proof/checkstyle.xml')
    run(name,cmd+list(files))
mode=args.phase
if mode=='recipe':
    lintdir=root/'lint-input'; lintdir.mkdir(exist_ok=True)
    for t in crate.glob('resources/com/synexia/rewrite/hash-pinned-java/*/*.txt'):
        if not t.name.startswith('before-'): (lintdir/t.name[:-4]).write_bytes(t.read_bytes())
    proof=crate/'proof/JFaceArrayContracts.java'
    annotation=list((crate/'proof/deps').rglob('*.java'))
    lint('01-template-and-proof-lint',list(lintdir.glob('*.java'))+list(crate.glob('src/test/**/*.java'))+annotation+[proof])
    run('02-annotation-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-d',root/'annotation-classes',*annotation])
    sources=list((root/'before/bundles/org.eclipse.jface/src').rglob('*.java'))
    run('03-baseline-compile',['javac','--release','21','-proc:none','-Xlint:all,-rawtypes','-Werror','-cp',cp,'-d',root/'baseline-classes',*sources,proof])
    base=[mvn,'-o','-B','-ntp','-Dmaven.repo.local='+str(cache),'-f',crate/'pom.xml','-Dcanonical.classpath='+cp]
    run('04-recipe-compile',base+['test-compile'])
    run('05-recipe-sdk-test',base+['surefire:test','-Dmaterialize.dir='+str(root/'generated')])
elif mode=='product':
    # Changed classes cannot share a classloader/package with signed donor classes.
    # Build a local unsigned qualification copy; retain the original archive and its hash.
    unsigned=root/'unsigned-jface.jar'
    with zipfile.ZipFile(jars[0]) as source, zipfile.ZipFile(unsigned,'w',zipfile.ZIP_DEFLATED) as target:
        for entry in source.infolist():
            if entry.filename.upper().startswith('META-INF/') and entry.filename.upper().endswith(('.SF','.RSA','.DSA','.EC')): continue
            target.writestr(entry,source.read(entry.filename))
    (evidence/'dependency-jface.json').write_text(json.dumps({'original':str(jars[0]),'sha256':hashlib.sha256(jars[0].read_bytes()).hexdigest(),'qualificationUnsignedSha256':hashlib.sha256(unsigned.read_bytes()).hexdigest()})+'\n')
    cp=os.pathsep.join(map(str,[unsigned]+jars[1:]+[root/'annotation-classes']))
    files=list((root/'generated').rglob('*.java'))
    assert len(files)==6,'require complete SDK output'
    lint('06-generated-lint',files+[crate/'proof/JFaceArrayContracts.java'])
    run('06b-baseline-proof-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',str(root/'baseline-classes')+os.pathsep+cp,'-d',root/'baseline-classes',crate/'proof/JFaceArrayContracts.java'])
    run('07-generated-compile',['javac','--release','21','-proc:none','-Xlint:all,-rawtypes','-Werror','-cp',cp,'-d',root/'candidate-classes',*files,crate/'proof/JFaceArrayContracts.java'])
    for variant in ['baseline','candidate']:
        classpath=str(root/(variant+'-classes'))+os.pathsep+cp
        run('08-'+variant+'-contracts',['java','-ea','-Xmx128m','-cp',classpath,'JFaceArrayContracts'])
        run('09-'+variant+'-public-api',['javap','-public','-s','-classpath',classpath,*['org.eclipse.jface.viewers.'+n for n in ['ArrayContentProvider','ViewerFilter','ViewerComparator','TreePath','StructuredSelection']]])
    for suffix in ['contracts','public-api']:
        prefix='08-' if suffix=='contracts' else '09-'
        assert (evidence/(prefix+'baseline-'+suffix+'.log')).read_bytes()==(evidence/(prefix+'candidate-'+suffix+'.log')).read_bytes(),suffix+' differs'
    (evidence/'10-differential-parity.json').write_text(json.dumps({'traceParity':True,'publicDescriptorsIdentical':True})+'\n')
elif mode=='runtime':
    assert (evidence/'10-differential-parity.json').exists(), 'behavior gate required'
    p2=args.p2.resolve()
    framework=next(p2.glob('org.eclipse.osgi_*.jar'))
    original=next(p2.glob('org.eclipse.jface_*.jar'))
    prefixes=['org.eclipse.swt_','org.eclipse.swt.gtk.linux.x86_64_','org.eclipse.swt.svg_',
              'org.eclipse.equinox.common_','org.eclipse.core.commands_','com.github.weisj.jsvg_',
              'org.apache.aries.spifly.dynamic.bundle_','org.objectweb.asm_', 'org.objectweb.asm.commons_',
              'org.objectweb.asm.util_','org.objectweb.asm.tree_','org.objectweb.asm.tree.analysis_']
    runtime_deps=[next(p2.glob(p+'*.jar')) for p in prefixes]
    depfile=root/'runtime-dependencies.txt'; depfile.write_text('\n'.join(map(str,runtime_deps))+'\n')
    (evidence/'runtime-dependencies.json').write_text(json.dumps([{'name':p.name,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in [framework,original]+runtime_deps],indent=2)+'\n')
    launcher=crate/'proof/JFaceEquinoxProof.java'; probe=crate/'proof/JFaceArrayCostProbe.java'
    lint('11-runtime-proof-lint',[launcher,probe])
    run('12-runtime-proof-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',framework,'-d',root/'runtime-classes',launcher])
    for variant in ['baseline','candidate']:
        classes=root/(variant+'-classes')
        run('13-'+variant+'-cost-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',str(classes)+os.pathsep+cp,'-d',classes,probe])
        host=root/(variant+'-jface.jar'); client=root/(variant+'-client.jar')
        additions={p.relative_to(classes).as_posix():p.read_bytes() for p in (classes/'org').rglob('*.class') if '.rsync-tmp' not in p.parts}
        with zipfile.ZipFile(original) as src, zipfile.ZipFile(host,'w',zipfile.ZIP_DEFLATED) as dst:
            for entry in src.infolist():
                n=entry.filename
                if n in additions or n.upper()=='META-INF/MANIFEST.MF': continue
                if n.upper().startswith('META-INF/') and n.upper().endswith(('.SF','.RSA','.DSA','.EC')): continue
                dst.writestr(entry,src.read(n))
            dst.writestr('META-INF/MANIFEST.MF',(root/'before/bundles/org.eclipse.jface/META-INF/MANIFEST.MF').read_bytes())
            for n,b in additions.items(): dst.writestr(n,b)
        with zipfile.ZipFile(client,'w',zipfile.ZIP_DEFLATED) as dst:
            dst.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nBundle-ManifestVersion: 2\nBundle-SymbolicName: com.synexia.proof.jfacearrays\nBundle-Version: 1.0.0\nRequire-Bundle: org.eclipse.jface\nBundle-RequiredExecutionEnvironment: JavaSE-21\n\n')
            for p in classes.glob('JFaceArrayContracts*.class'): dst.writestr(p.name,p.read_bytes())
        run('14-'+variant+'-equinox',['java','-ea','-Xmx256m','-cp',str(root/'runtime-classes')+os.pathsep+str(framework),'JFaceEquinoxProof',root/(variant+'-osgi'),depfile,host,client,str(variant=='candidate').lower()])
        run('15-'+variant+'-bytecode',['javap','-c','-p','-classpath',classes,*['org.eclipse.jface.viewers.'+n for n in ['ArrayContentProvider','ViewerFilter','ViewerComparator','TreePath','StructuredSelection']]])
        for mask in [0,15,1023]:
            for fork in range(2):
                run('16-'+variant+'-mask-'+str(mask)+'-fork-'+str(fork),['java','-ea','-Xms64m','-Xmx64m','-cp',str(classes)+os.pathsep+str(root/'unsigned-jface.jar')+os.pathsep+os.pathsep.join(map(str,jars[1:])),'JFaceArrayCostProbe',mask])
    calls=(evidence/'15-candidate-bytecode.log').read_text().count('invokestatic')
    forwarded=(evidence/'15-candidate-bytecode.log').read_text().count('Method org/eclipse/jface/internal/m3/M3ViewerArrays.')
    assert forwarded==14, (forwarded,calls)
    (evidence/'17-forwarding-receipt.json').write_text(json.dumps({'forwardedCallSites':forwarded,'atoms':13,'frameworkFacades':5,'realEquinox':True,'exportedNewPackages':0})+'\n')
else: raise SystemExit('unknown mode')
