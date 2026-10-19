# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Native SWT and measured heap/cost gates after differential qualification."""
from pathlib import Path
import argparse, hashlib, json, os, socket, struct, subprocess, time, secrets, zipfile
p=argparse.ArgumentParser()
for option in ['work','jdk','m2','deps','xvfb']: p.add_argument('--'+option,required=True,type=Path)
p.add_argument('--native-only',action='store_true')
p.add_argument('--run-prefix',default='19')
a=p.parse_args(); c=Path(__file__).resolve().parent.parent; w=a.work.resolve();e=w/'evidence'
assert (e/'10-parity.json').exists()
env=os.environ.copy();env['PATH']=str(a.jdk/'bin')+os.pathsep+env['PATH']
def run(name,cmd):
    cmd=list(map(str,cmd));log=e/(name+'.log');assert not log.exists(),name
    with log.open('w') as f: result=subprocess.run(cmd,cwd=w,env=env,stdout=f,stderr=subprocess.STDOUT,timeout=120)
    (e/(name+'.json')).write_text(json.dumps(dict(command=cmd,exit=result.returncode,logSha256=hashlib.sha256(log.read_bytes()).hexdigest()),indent=2)+'\n')
    print(name,result.returncode,'\n'.join(log.read_text().splitlines()[-10:]),flush=True)
    if result.returncode: raise SystemExit('HOLD '+name)
jars=[next(a.deps.glob(prefix+'*.jar')) for prefix in ['org.eclipse.swt.gtk.linux.x86_64_','org.eclipse.core.commands_','org.eclipse.equinox.common_','org.eclipse.osgi_']]
cp=os.pathsep.join(map(str,[w/'unsigned-jface.jar']+jars))
proofs=[c/'proof'/name for name in ['MemoryAgent.java','JFacePackedCostProbe.java','JFaceArrayCostProbe.java','JFaceNativeContracts.java']]
lcp=os.pathsep.join(str(a.m2/path) for path in (c/'proof/lint-classpath.txt').read_text().splitlines())
run(a.run_prefix+'-runtime-lint' if a.native_only else '11-runtime-lint',['java','-cp',lcp,'com.puppycrawl.tools.checkstyle.Main','-c',c/'proof/checkstyle.xml',*proofs])
if not a.native_only:
    for variant in ['baseline','candidate']:
        classes=w/(variant+'-classes');classpath=str(classes)+os.pathsep+cp
        run('12-'+variant+'-proof-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',classpath,'-d',classes,*proofs])
        agent=w/(variant+'-agent.jar')
        with zipfile.ZipFile(agent,'w') as z:
            z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nPremain-Class: proof.MemoryAgent\n\n')
            z.write(classes/'proof/MemoryAgent.class','proof/MemoryAgent.class')
        run('13-'+variant+'-protected-api',['javap','-protected','-s','-classpath',classpath,*['org.eclipse.jface.viewers.'+name for name in ['CustomHashtable','TreeSelection','StructuredViewer','AbstractTreeViewer','TreeViewer','ViewerFilter','ViewerComparator','TreePath','StructuredSelection','ArrayContentProvider']]])
        for fork in range(2):
            run('14-'+variant+'-map-cost-'+str(fork),['java','-ea','-Xms128m','-Xmx128m','-javaagent:'+str(agent),'-cp',classpath,'org.eclipse.jface.viewers.JFacePackedCostProbe'])
            for mask in [0,15,1023]:
                run('15-'+variant+'-filter-'+str(mask)+'-'+str(fork),['java','-ea','-Xms64m','-Xmx64m','-cp',classpath,'JFaceArrayCostProbe',mask])
    assert (e/'13-baseline-protected-api.log').read_bytes()==(e/'13-candidate-protected-api.log').read_bytes()
else:
    for variant in ['baseline','candidate']:
        classes=w/(variant+'-classes')
        run(a.run_prefix+'-'+variant+'-native-compile',['javac','--release','21','-proc:none','-Xlint:all','-Werror','-cp',str(classes)+os.pathsep+cp,'-d',classes,c/'proof/JFaceNativeContracts.java'])
number=next(n for n in range(150,180) if not Path('/tmp/.X'+str(n)+'-lock').exists())
auth=w/'Xauthority'
def field(data):return struct.pack('>H',len(data))+data
auth.write_bytes(struct.pack('>H',65535)+field(b'')+field(str(number).encode())+field(b'MIT-MAGIC-COOKIE-1')+field(secrets.token_bytes(16)));auth.chmod(0o600)
env['LD_LIBRARY_PATH']=str(a.xvfb/'usr/lib/x86_64-linux-gnu');env['XAUTHORITY']=str(auth);env['DISPLAY']='127.0.0.1:'+str(number)
xkb=Path('/usr/bin/xkbcomp'); linked=False
if not xkb.exists():
    compiler=(a.xvfb/'usr/bin/xkbcomp').resolve()
    assert compiler.is_file(), 'missing pinned xkbcomp toolchain'
    xkb.symlink_to(compiler);linked=True
with (e/(a.run_prefix+'-xvfb.log' if a.native_only else '16-xvfb.log')).open('w') as log:
    server=subprocess.Popen([str(a.xvfb/'usr/bin/Xvfb'),':'+str(number),'-screen','0','1280x800x24','-nolisten','unix','-nolisten','local','-listen','tcp','-auth',str(auth)],env=env,stdout=log,stderr=log)
    try:
        for attempt in range(100):
            if server.poll() is not None:raise RuntimeError('Xvfb stopped')
            try:
                with socket.create_connection(('127.0.0.1',6000+number),timeout=.1): pass
                time.sleep(.25)
                if server.poll() is not None: raise RuntimeError('Xvfb keyboard initialization failed')
                break
            except OSError: time.sleep(.05)
        else: raise RuntimeError('Xvfb unavailable')
        for variant in ['baseline','candidate']:
            run((a.run_prefix+'-' if a.native_only else '17-')+variant+'-native',['java','-ea','-Xmx256m','-cp',str(w/(variant+'-classes'))+os.pathsep+cp,'JFaceNativeContracts'])
    finally:
        server.terminate();server.wait(timeout=10);auth.unlink(missing_ok=True)
        if linked: xkb.unlink()
lines=[]
for variant in ['baseline','candidate']:
    results=[line for line in (e/((a.run_prefix+'-' if a.native_only else '17-')+variant+'-native.log')).read_text().splitlines() if line.startswith('NATIVE_CONTRACTS')]
    assert len(results)==1;lines.append(results)
assert lines[0]==lines[1]
(e/'18-runtime-parity.json').write_text(json.dumps(dict(nativeParity=True,publicProtectedParity=True,scope='Real GTK SWT 3.130/JFace 3.37 dependencies with pinned current selected JFace sources; not full Tycho/PDE or current OSGi manifest proof'))+'\n')
(e/'DEPENDENCIES.json').write_text(json.dumps([dict(path=str(j),sha256=hashlib.sha256(j.read_bytes()).hexdigest()) for j in [next(a.deps.glob('org.eclipse.jface_*.jar'))]+jars],indent=2)+'\n')
