#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Compile this worktree's org.eclipse.jface (candidate) into $OUT/cand/classes and the M3 test/bench sources into
# $OUT/cand/tests (against the candidate) and $OUT/stock/tests (against the official JFace jar).
# Run under the build lock: bash C:/m3-superset/tools/build_lock.sh jface-viewport bash .../build_jface.sh
set -e
. "$(dirname "$0")/jface_env.sh"
J="$JAVA_HOME/bin/javac"
STUBS=$OUT/stubs/src/org/eclipse/pde/api/tools/annotations
mkdir -p "$STUBS" "$OUT/stubs/classes"
for a in NoExtend NoImplement NoInstantiate NoReference; do
  printf 'package org.eclipse.pde.api.tools.annotations;\npublic @interface %s {}\n' $a > "$STUBS/$a.java"
done
"$J" --release 21 -nowarn -d "$OUT/stubs/classes" "$STUBS"/*.java
rm -rf "$OUT/cand/classes" && mkdir -p "$OUT/cand/classes"
find "$WT/bundles/org.eclipse.jface/src" -name '*.java' > "$OUT/cand/sources.txt"
"$J" --release 21 -encoding UTF-8 -nowarn -proc:none -g -cp "$SWT_JAR;$DEPS;$OUT/stubs/classes" \
  -d "$OUT/cand/classes" @"$OUT/cand/sources.txt" 2>&1 | grep -E "error|warning: \[" | head -40 || true
( cd "$WT/bundles/org.eclipse.jface/src" && find . -type f ! -name '*.java' | while read -r f; do mkdir -p "$OUT/cand/classes/$(dirname "$f")"; cp "$f" "$OUT/cand/classes/$f"; done )
echo "jface classes: $(find "$OUT/cand/classes" -name '*.class' | wc -l)"
T="$WT/tests/org.eclipse.jface.tests/src/org/eclipse/jface/tests/viewers/m3"
for side in cand stock; do
  if [ $side = cand ]; then JF="$OUT/cand/classes"; else JF="$STOCK_JFACE"; fi
  rm -rf "$OUT/$side/tests" && mkdir -p "$OUT/$side/tests"
  "$J" --release 21 -encoding UTF-8 -nowarn -proc:none -g -cp "$JF;$SWT_JAR;$DEPS;$JUNIT" \
    -d "$OUT/$side/tests" $(ls "$T"/*.java | grep -v -E "${M3_TEST_EXCLUDE_REGEX:-^$}") 2>&1 | grep -E "error" | head -20 || true
  echo "$side test classes: $(find "$OUT/$side/tests" -name '*.class' | wc -l)"
done
