#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Headless JUnit 5 tests of the markers lane model (no Display, no workspace) with JaCoCo, after build_overlay.sh.
# Usage: run_headless_tests.sh <exec-name> <TestClass>...   (classes of org.eclipse.ui.tests.markers)
WT=${WT:-S:/m3-scratch/worktrees/platform-ui}; OUT=${OUT:-S:/m3-scratch/jface}; B=$OUT/markers-m3
. "$WT/docs/m3/jface-viewport/tools/jface_env.sh"
EXEC=$1; shift
T="$WT/tests/org.eclipse.ui.tests/Eclipse UI Tests/org/eclipse/ui/tests/markers"
C=$B/test-classes; rm -rf "$C"; mkdir -p "$C" "$OUT/coverage"
SRC=(); SEL=()
for c in "$@"; do SRC+=("$T/$c.java"); SEL+=(--select-class "org.eclipse.ui.tests.markers.$c"); done
JDK25=${JDK25:-F:/jdks/temurin25/jdk-25.0.4.1+1}  # the overlay compiles the ide classes with --release 25 (build_overlay.sh)
"$JDK25/bin/javac" --release 25 -nowarn -encoding UTF-8 -proc:none \
  -cp "$(cygpath -w "$B/classes");$(cygpath -w "$JUNIT")" -d "$C" "${SRC[@]}" || exit 1
DEST=$(cygpath -w "$OUT/coverage/$EXEC.exec"); rm -f "$OUT/coverage/$EXEC.exec"
"$JDK25/bin/java" -Xmx1g -Djava.io.tmpdir=$(cygpath -w "$OUT/tmp") \
  "-javaagent:$(cygpath -w "$JACOCO_AGENT")=destfile=$DEST,includes=org.eclipse.ui.internal.views.markers.M3*" \
  -jar "$(cygpath -w "$JUNIT")" execute --disable-banner --details=tree --details-theme=ascii \
  -cp "$(cygpath -w "$C");$(cygpath -w "$B/classes")" "${SEL[@]}" 2>&1 \
  | sed 's/\x1b\[[0-9;]*m//g' | grep -E "tests (successful|failed)|\[X\]|=>|Exception|expected" | head -${TAILN:-30}
