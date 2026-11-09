#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# SWT for the G5 builds that need SWT fork API (platform.ui: SWT.PartVisible, Control.isPartVisible): the SWT release
# candidate (hsoliwal/eclipse.platform.swt m3/release-candidate-20261109, PR #335) is built on JDK 21 and installed into
# the shared local repository; a later build with -Dtycho.localArtifacts=default resolves org.eclipse.swt 3.138.0 from it
# (newer qualifier than the upstream I-build). Host bundle + the fragments of the 8 environments of eclipse-platform-parent.
# Usage: build_swt_rc.sh   -> F:/m3-scratch/rel21/build/swt-rc.{log,result.tsv}
set -uo pipefail
W=F:/m3-scratch/rel21/forks/eclipse.platform.swt; B=F:/m3-scratch/rel21/build; M2=F:/m3-scratch/m2-eclipse
export JAVA_HOME="C:/Program Files/Java/jdk-21" TEMP=F:/m3-scratch/tmp TMP=F:/m3-scratch/tmp
export MAVEN_OPTS="-Xmx4g -Djava.io.tmpdir=F:/m3-scratch/tmp"
MVN=C:/ProgramData/chocolatey/lib/maven/apache-maven-3.9.14/bin/mvn
PL=bundles/org.eclipse.swt
for f in gtk.linux.x86_64 gtk.linux.ppc64le gtk.linux.riscv64 gtk.linux.aarch64 win32.win32.x86_64 win32.win32.aarch64 cocoa.macosx.x86_64 cocoa.macosx.aarch64; do
	PL="$PL,binaries/org.eclipse.swt.$f"; done
t0=$(date +%s); sha=$(git -C "$W" rev-parse HEAD); echo "$(date +%T) SWT $sha install start"
( cd "$W" && "$MVN" -B -s F:/m3-scratch/rel21/tools/settings-central-first.xml clean install -DskipTests -nsu \
	-Dmaven.repo.local="$M2" -Djava.io.tmpdir=F:/m3-scratch/tmp -pl "$PL" -am ) > "$B/swt-rc.log" 2>&1
rc=$?; st=$(grep -a -o -E 'BUILD (SUCCESS|FAILURE)' "$B/swt-rc.log" | tail -1)
printf 'swt_commit\tstatus\trc\tseconds\n%s\t%s\t%s\t%s\n' "$sha" "${st:-NO_RESULT}" "$rc" "$(( $(date +%s) - t0 ))" > "$B/swt-rc.result.tsv"
gzip -c "$B/swt-rc.log" > "$B/swt-rc.log.gz"
git -C "$W" ls-files --others --ignored --exclude-standard --directory 2>/dev/null | grep -E '(^|/)target/$' | while read -r d; do rm -rf "${W:?}/$d"; done
echo "$(date +%T) SWT $sha install ${st:-NO_RESULT} rc=$rc"
