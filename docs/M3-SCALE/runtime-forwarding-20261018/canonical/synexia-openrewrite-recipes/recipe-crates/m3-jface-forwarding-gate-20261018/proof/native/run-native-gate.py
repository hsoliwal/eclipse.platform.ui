#!/usr/bin/env python3
# SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
# SPDX-License-Identifier: Apache-2.0
"""Run genuine Surefire under the verified local Xvfb; never synthesize test XML.

Uses the existing toolchain/verify-xvfb-local.py readiness and cleanup sequence.
"""
from pathlib import Path
import hashlib
import json
import os
import select
import shutil
import subprocess
import time

work = Path(__file__).resolve().parent
root = work.parent
toolchain = root / "toolchain"
evidence = work / "evidence"
evidence.mkdir(exist_ok=True)
reports = work / "target/surefire-reports"
if reports.exists() and list(reports.glob("TEST-*.xml")):
    raise SystemExit("Refusing to reuse native test XML; preserve the earlier attempt first")

resource_count = 0
test_root = root / "forks/eclipse.platform.ui/tests/org.eclipse.jface.tests"
for source in (test_root / "src").rglob("*"):
    if source.is_file() and source.suffix != ".java":
        destination = work / "test-classes" / source.relative_to(test_root / "src")
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        resource_count += 1
for source in (test_root / "icons").rglob("*"):
    if source.is_file():
        destination = work / "test-classes" / source.relative_to(test_root)
        destination.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source, destination)
        resource_count += 1

environment = dict(os.environ)
environment.update({
    "JAVA_HOME": str(toolchain / "jdk25"),
    "PATH": str(toolchain / "jdk25/bin") + os.pathsep + environment.get("PATH", ""),
    "SWT_GTK3": "1",
    "GDK_BACKEND": "x11",
})
read_fd, write_fd = os.pipe()
xvfb_command = [
    str(toolchain / "native-bin/Xvfb-local"), "-displayfd", str(write_fd),
    "-screen", "0", "1280x1024x24", "-nolisten", "tcp", "-noreset",
    "-xkbdir", str(toolchain / "native-sysroot/usr/share/X11/xkb"),
]
test_command = [
    "/tmp/m3-toolchain/apache-maven-3.9.12/bin/mvn", "-o", "-B", "-ntp",
    "-f", str(work / "pom.xml"), "-Dmaven.repo.local=/root/.m2/repository",
    "-Dtest=M3TreeForwardingTest#testExpandedStateDelegatesToSwtWithoutWalkingColdVirtualSiblings",
    "surefire:test",
]
receipt = {"xvfbCommand": xvfb_command, "testCommand": test_command,
           "resourcesCopied": resource_count, "mode": "native GTK3, source-built SWT/JFace, generated Commands receiver",
           "fullTychoPdeRun": False, "testExit": None, "verifierExit": None}
server = None
started_at = time.monotonic()
try:
    with (evidence / "native-xvfb.log").open("w") as output:
        server = subprocess.Popen(xvfb_command, env=environment, pass_fds=(write_fd,),
                                  stdout=output, stderr=subprocess.STDOUT)
        os.close(write_fd)
        write_fd = -1
        ready = select.select([read_fd], [], [], 15)[0]
        display = os.read(read_fd, 64).decode().strip() if ready else ""
        receipt.update({"displayNumber": display, "displayReady": bool(display) and server.poll() is None})
        if not receipt["displayReady"]:
            raise RuntimeError("The actual Xvfb did not report a live display")
        environment["DISPLAY"] = ":" + display
        with (evidence / "native-jface-maven.log").open("w") as test_output:
            result = subprocess.run(test_command, env=environment, cwd=work, stdout=test_output,
                                    stderr=subprocess.STDOUT, timeout=150)
        receipt["testExit"] = result.returncode
        if result.returncode == 0:
            verifier_command = ["/tmp/m3-toolchain/jdk21/bin/java", "-cp",
                                str(root / "composition-authoring/target/classes"),
                                "M3EclipseComposition", "reports", str(reports)]
            receipt["verifierCommand"] = verifier_command
            with (evidence / "composition-reports.log").open("w") as verifier_output:
                verified = subprocess.run(verifier_command, cwd=work, stdout=verifier_output,
                                          stderr=subprocess.STDOUT, timeout=35)
            receipt["verifierExit"] = verified.returncode
finally:
    os.close(read_fd)
    if write_fd >= 0:
        os.close(write_fd)
    if server is not None:
        if server.poll() is None:
            server.terminate()
        try:
            receipt["displayExitAfterCleanup"] = server.wait(timeout=10)
        except subprocess.TimeoutExpired:
            server.kill()
            receipt["displayExitAfterCleanup"] = server.wait(timeout=5)
    receipt["wallSeconds"] = round(time.monotonic() - started_at, 3)
    receipt["status"] = "PASS" if receipt["testExit"] == 0 and receipt["verifierExit"] == 0 else "FAIL"
    receipt["reports"] = [{"path": str(path.relative_to(work)), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
                          for path in sorted(reports.glob("TEST-*.xml"))]
    receipt["inputs"] = [{"path": str(path.relative_to(root)), "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
                         for path in [work / "pom.xml", Path(__file__),
                                      root / "commands-authoring/target/org.eclipse.core.commands-m3-proof.jar",
                                      toolchain / "xvfb-source/build/hw/vfb/Xvfb",
                                      root / "composition-authoring/target/classes/M3EclipseComposition.class"]]
    (evidence / "native-jface.json").write_text(json.dumps(receipt, indent=2) + "\n")
    print(json.dumps(receipt, indent=2))
    for name in ["native-jface-maven.log", "composition-reports.log", "native-xvfb.log"]:
        path = evidence / name
        if path.exists():
            print(name + "\n" + path.read_text()[-6000:])
raise SystemExit(0 if receipt["status"] == "PASS" else 1)
