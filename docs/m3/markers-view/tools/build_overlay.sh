#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Build the measured "ours" bundles from the official 2026-09 jars in the p2 pool (signature files dropped, the
# content changed). Output in $OUT/eclipse/plugins:
#   org.eclipse.jface_m3.jar  - the official jar with this worktree's JFace classes (build_jface.sh output,
#                               $OUT/cand/classes: viewport mode, the M3ViewerDelta/M3Viewers delta API) and
#                               org.eclipse.jface.viewers.m3 exported;
#   org.eclipse.ui.ide_m3.jar - the official jar with the markers classes this branch adds or hooks (every M3*.java
#                               of the package and the recipe-hooked files) compiled against the official bundles and
#                               the JFace above.
set -e
WT=${WT:-S:/m3-scratch/worktrees/platform-ui}; OUT=${OUT:-S:/m3-scratch/jface}; E=$OUT/eclipse; B=$OUT/markers-m3
POOL=$(cygpath -m ~/.p2/pool/plugins); JDK="C:/Program Files/Java/jdk-21"
STOCK=$(ls "$POOL" | grep '^org.eclipse.ui.ide_3' | head -1)
BI=$(ls -d "$E"/config-stock*/org.eclipse.equinox.simpleconfigurator/bundles.info | head -1)
JFACE=$(grep -E '^org\.eclipse\.jface,' "$BI" | cut -d, -f3 | sed 's|^file:/||')
mkdir -p "$B" "$E/plugins"
# 1. JFace
J=$B/jface-jar; rm -rf "$J" && mkdir -p "$J"
(cd "$J" && "$JDK/bin/jar" xf "$JFACE" && rm -f META-INF/*.SF META-INF/*.RSA META-INF/*.DSA META-INF/*.EC)
cp -r "$OUT/cand/classes/." "$J/"
sed -i 's/^Export-Package: /Export-Package: org.eclipse.jface.viewers.m3,/' "$J/META-INF/MANIFEST.MF"
rm -f "$E/plugins/org.eclipse.jface_m3.jar"
(cd "$J" && "$JDK/bin/jar" cfm "$E/plugins/org.eclipse.jface_m3.jar" META-INF/MANIFEST.MF .)
# 2. org.eclipse.ui.workbench: the M3 helpers (org.eclipse.ui.internal.m3, e.g. M3PartVisibility) added and exported
WB=$(grep -E '^org\.eclipse\.ui\.workbench,' "$BI" | cut -d, -f3 | sed 's|^file:/||')
W=$B/workbench-jar; rm -rf "$W" "$B/wb-classes" && mkdir -p "$W" "$B/wb-classes"
grep -v '^#' "$BI" | cut -d, -f3 | sed 's|^file:/||' | grep '\.jar$' | grep -v m3bench > "$B/cp.list"
{ echo "-cp"; echo "$OUT/cand/classes;$(tr '\n' ';' < "$B/cp.list")"; } > "$B/javac.args"
"$JDK/bin/javac" --release 21 -nowarn -encoding UTF-8 -proc:none @"$B/javac.args" -d "$B/wb-classes" \
  "$WT"/bundles/org.eclipse.ui.workbench/eclipseui/org/eclipse/ui/internal/m3/*.java
(cd "$W" && "$JDK/bin/jar" xf "$WB" && rm -f META-INF/*.SF META-INF/*.RSA META-INF/*.DSA META-INF/*.EC)
cp -r "$B/wb-classes/." "$W/"
sed -i 's/^Export-Package: /Export-Package: org.eclipse.ui.internal.m3;x-internal:=true,/' "$W/META-INF/MANIFEST.MF"
rm -f "$E/plugins/org.eclipse.ui.workbench_m3.jar"
(cd "$W" && "$JDK/bin/jar" cfm "$E/plugins/org.eclipse.ui.workbench_m3.jar" META-INF/MANIFEST.MF .)
# 3. org.eclipse.ui.ide
M=$WT/bundles/org.eclipse.ui.ide/src/org/eclipse/ui/internal/views/markers
HOOKED=$(grep -o '\$M/[A-Za-z]*\.java' "$WT/docs/m3/markers-view/tools/apply_recipe.sh" | sed "s|\\\$M|$M|")
rm -rf "$B/classes" "$B/jar" && mkdir -p "$B/classes" "$B/jar"
# the class path (425 jars) goes through an argument file: the command line is too long for Windows
{ echo "-cp"; echo "$OUT/cand/classes;$B/wb-classes;$(tr '\n' ';' < "$B/cp.list")"; } > "$B/javac.args"
# org.eclipse.ui.ide needs JavaSE-25 on master (unnamed variables in ExtendedMarkersView): compile it with JDK 25
"F:/jdks/temurin25/jdk-25.0.4.1+1/bin/javac" --release 25 -nowarn -encoding UTF-8 -proc:none @"$B/javac.args" -d "$B/classes" "$M"/M3*.java $HOOKED
(cd "$B/jar" && "$JDK/bin/jar" xf "$POOL/$STOCK" && rm -f META-INF/*.SF META-INF/*.RSA META-INF/*.DSA META-INF/*.EC)
cp -r "$B/classes/." "$B/jar/"
rm -f "$E/plugins/org.eclipse.ui.ide_m3.jar"
(cd "$B/jar" && "$JDK/bin/jar" cfm "$E/plugins/org.eclipse.ui.ide_m3.jar" META-INF/MANIFEST.MF .)
echo "overlay: org.eclipse.jface_m3.jar ($(find "$OUT/cand/classes" -name '*.class' | wc -l) classes), org.eclipse.ui.workbench_m3.jar (+$(find "$B/wb-classes" -name '*.class' | wc -l)), org.eclipse.ui.ide_m3.jar ($(find "$B/classes" -name '*.class' | wc -l) classes from $STOCK)"
