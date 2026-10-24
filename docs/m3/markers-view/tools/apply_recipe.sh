#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Apply the markers view hook recipe twice (second pass UNCHANGED) to the given source root
# (default: this worktree's org.eclipse.ui.ide/src).
WT=${WT:-S:/m3-scratch/worktrees/platform-ui}; OUT=${OUT:-S:/m3-scratch/jface}
SRC=${1:-$WT/bundles/org.eclipse.ui.ide/src}
JAVA_HOME="C:/Program Files/Java/jdk-21"
C=K:/m3-work/syn-recipes/synexia-openrewrite-recipes/crates/m3-viewport-catalogue-recipes
CPR="$(cygpath -w "$WT/docs/m3/markers-view/recipe");$C/target/classes;$(cat $C/target/cp.txt)"
M="$SRC/org/eclipse/ui/internal/views/markers"
FILES=("$M/MarkersChangeListener.java" "$M/CachedMarkerBuilder.java" "$M/Markers.java" "$M/MarkerViewerContentProvider.java" "$M/MarkersTreeViewer.java" "$M/ExtendedMarkersView.java" "$M/MarkerContentGenerator.java" "$M/MarkerPatternFilter.java")
# MarkerPatternFilter: the setPattern hook (M3MarkerDeltas.textChanged) and the lane-native match helpers
# (visibleFields, matchesText, matchesAny) are edited in the source; listed here so build_overlay.sh compiles it
# (until 2026-10-24 the overlay carried the stock class: the keystroke path never reached the lanes in a run)
# The OpenRewrite Java 21 parser rejects unnamed variables (Java 22, e.g. "_ ->"): such a file parses as an error and
# stays UNCHANGED. Each isolated "_" token is swapped for a unique placeholder before the passes and back after.
sed -i "s/\b_\b/m3UnnamedVar0/g" "${FILES[@]}"
for pass in 1 2; do
  echo "recipe pass $pass: $("$JAVA_HOME/bin/java" -Xmx1g -Djava.io.tmpdir="$(cygpath -w "$OUT/tmp")" -cp "$CPR" \
    com.synexia.openrewrite.viewport.M3PerFileApply com.synexia.openrewrite.viewport.MarkersViewDeltaHooks \
    "${FILES[@]}" 2>&1 | grep -oE '^(CHANGED|UNCHANGED|ERROR)' | tr '\n' ' ')"
  # M3PerFileApply prints the changed statements with CRLF on Windows; the sources use LF
  sed -i 's/\r$//' "${FILES[@]}"
done
sed -i "s/m3UnnamedVar0/_/g" "${FILES[@]}"
