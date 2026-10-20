#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# JUnit 5 UI tests of the M3 classes on the hidden desktop, with JaCoCo. Usage: run_ui_tests.sh <exec-name> <TestClass>...
. "$(dirname "$0")/jface_env.sh"
if [ "$1" != --inner ]; then
  . "$(dirname "$0")/snapshot.sh"; snapshot "$OUT/runs/ui-$1"
  exec bash C:/m3-superset/tools/ui_lock.sh jface-viewport bash "$0" --inner "$@"
fi
shift
EXEC=$1; shift; RUN=$OUT/runs/ui-$EXEC
SEL=(); for c in "$@"; do SEL+=(--select-class "org.eclipse.jface.tests.viewers.m3.$c"); done
mkdir -p "$OUT/coverage"; rm -f "$OUT/coverage/$EXEC.exec"
deps() { echo "$DEPS" | tr ';' '\n' | while read -r d; do printf '%s;' "$(cygpath -w "$d")"; done; }
bash C:/m3-superset/tools/hidden_java.sh 900 "$OUT/$EXEC.log" -Xmx1g -Djava.io.tmpdir=$(cygpath -w "$OUT/tmp") ${JAVA_OPTS:-} \
  "-javaagent:$(cygpath -w "$JACOCO_AGENT")=destfile=$(cygpath -w "$OUT/coverage/$EXEC.exec"),includes=org.eclipse.jface.*" \
  -jar "$(cygpath -w "$JUNIT")" execute --disable-banner --details=tree --details-theme=ascii \
  -cp "$(cygpath -w "$RUN/cand-tests");$(cygpath -w "$RUN/cand-classes");$(cygpath -w "$SWT_JAR");$(deps)" "${SEL[@]}"
sed 's/\x1b\[[0-9;]*m//g' "$OUT/$EXEC.log" | grep -E "tests (successful|failed)|\[X\]|=>|Exception" | head -${TAILN:-40}
