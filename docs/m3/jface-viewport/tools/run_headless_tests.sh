#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Headless JUnit 5 tests of the M3 lanes (no Display), with JaCoCo, after build_jface.sh.
# Usage: run_headless_tests.sh <exec-name> <TestClass>...
. "$(dirname "$0")/jface_env.sh"
EXEC=$1; shift
SEL=(); for c in "$@"; do SEL+=(--select-class "org.eclipse.jface.tests.viewers.m3.$c"); done
mkdir -p "$OUT/coverage"; DEST=$(cygpath -w "$OUT/coverage/$EXEC.exec"); rm -f "$OUT/coverage/$EXEC.exec"
deps() { echo "$DEPS" | tr ';' '\n' | while read -r d; do printf '%s;' "$(cygpath -w "$d")"; done; }
"$JAVA_HOME/bin/java" -Xmx512m -Djava.io.tmpdir=$(cygpath -w "$OUT/tmp") ${JAVA_OPTS:-} \
  "-javaagent:$(cygpath -w "$JACOCO_AGENT")=destfile=$DEST,includes=org.eclipse.jface.*" \
  -jar "$(cygpath -w "$JUNIT")" execute --disable-banner --details=tree --details-theme=ascii \
  -cp "$(cygpath -w "$OUT/cand/tests");$(cygpath -w "$OUT/cand/classes");$(cygpath -w "$SWT_JAR");$(deps)" "${SEL[@]}" 2>&1 \
  | sed 's/\x1b\[[0-9;]*m//g' | grep -E "tests (successful|failed)|\[X\]|=>|Exception" | head -${TAILN:-30}
