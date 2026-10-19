#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Build the measured "ours" org.eclipse.ui.ide: the official 3.23.200 jar from the p2 pool with the markers classes
# that this branch changes (hooked files + M3 classes) compiled from this worktree against the official bundles.
# The signature files are dropped (the content changed). Output: $OUT/eclipse/plugins/org.eclipse.ui.ide_m3.jar
set -e
WT=${WT:-S:/m3-scratch/worktrees/platform-ui}; OUT=${OUT:-S:/m3-scratch/jface}; E=$OUT/eclipse; B=$OUT/markers-m3
POOL=$(cygpath -m ~/.p2/pool/plugins); JDK="C:/Program Files/Java/jdk-21"
STOCK=$(ls "$POOL" | grep '^org.eclipse.ui.ide_3' | head -1)
mkdir -p "$B" "$E/plugins"
grep -v '^#' "$E/config-stock/org.eclipse.equinox.simpleconfigurator/bundles.info" | cut -d, -f3 | sed 's|^file:/||' | grep '\.jar$' | grep -v m3bench > "$B/cp.list"
M=$WT/bundles/org.eclipse.ui.ide/src/org/eclipse/ui/internal/views/markers
rm -rf "$B/classes" "$B/jar" && mkdir -p "$B/classes" "$B/jar"
"$JDK/bin/javac" --release 21 -nowarn -encoding UTF-8 -proc:none -cp "$(tr '\n' ';' < "$B/cp.list")" -d "$B/classes" \
  "$M/M3MarkerDeltas.java" "$M/M3LongSet.java" "$M/MarkersChangeListener.java" "$M/CachedMarkerBuilder.java" "$M/Markers.java"
(cd "$B/jar" && "$JDK/bin/jar" xf "$POOL/$STOCK" && rm -f META-INF/*.SF META-INF/*.RSA META-INF/*.DSA META-INF/*.EC)
cp -r "$B/classes/." "$B/jar/"
rm -f "$E/plugins/org.eclipse.ui.ide_m3.jar"
(cd "$B/jar" && "$JDK/bin/jar" cfm "$E/plugins/org.eclipse.ui.ide_m3.jar" META-INF/MANIFEST.MF .)
echo "overlay: $E/plugins/org.eclipse.ui.ide_m3.jar from $STOCK + $(find "$B/classes" -name '*.class' | wc -l) classes"
