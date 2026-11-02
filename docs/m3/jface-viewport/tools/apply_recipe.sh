#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Apply the JFace viewport hook recipe twice with M3PerFileApply; the second pass must report UNCHANGED.
. "$(dirname "$0")/jface_env.sh"
C=K:/m3-work/syn-recipes/synexia-openrewrite-recipes/crates/m3-viewport-catalogue-recipes
CPR="$(cygpath -w "$WT/docs/m3/jface-viewport/recipe");$C/target/classes;$(cat $C/target/cp.txt)"
V="$WT/bundles/org.eclipse.jface/src/org/eclipse/jface/viewers"
FILES=("$V/AbstractTableViewer.java" "$V/AbstractTreeViewer.java" "$V/AbstractListViewer.java" "$V/StructuredViewer.java")
for pass in 1 2; do
  echo "recipe pass $pass: $("$JAVA_HOME/bin/java" -Xmx1g -Djava.io.tmpdir="$(cygpath -w "$OUT/tmp")" -cp "$CPR" \
    com.synexia.openrewrite.viewport.M3PerFileApply com.synexia.openrewrite.viewport.JFaceViewportHooks \
    "${FILES[@]}" 2>&1 | grep -oE '^(CHANGED|UNCHANGED|ERROR)' | tr '\n' ' ')"
  # M3PerFileApply prints the changed statements with CRLF on Windows; the sources use LF
  sed -i 's/\r$//' "${FILES[@]}"
done
