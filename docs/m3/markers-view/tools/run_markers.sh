#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Markers view benchmark/differential inside the official Eclipse 2026-09 bundles (read from the p2 pool, never
# written), on the hidden desktop, with configuration, workspace, user area and temp on S:.
# Usage: run_markers.sh <stock|m3> <n> [seed steps]      (IDE_JAR=<overlay org.eclipse.ui.ide jar> for m3)
set -e
WT=${WT:-S:/m3-scratch/worktrees/platform-ui}
OUT=${OUT:-S:/m3-scratch/jface}; E=$OUT/eclipse; mkdir -p "$E/plugins" "$OUT/tmp"
export TEMP=$OUT/tmp TMP=$OUT/tmp
SIDE=$1 N=$2 SEED=${3:-} STEPS=${4:-30}
if [ "$5" != --inner ]; then
  INSTALL=$(cygpath -m ~/eclipse/jee-2026-09/eclipse); POOL=$(cygpath -m ~/.p2/pool/plugins)
  JDK25=F:/jdks/temurin25/jdk-25.0.4.1+1
  # 1. bench bundle
  B=$WT/docs/m3/markers-view/bench-bundle; C=$OUT/markers-bench; rm -rf "$C" && mkdir -p "$C/classes"
  CP=$(ls "$POOL" | grep -E '^org\.eclipse\.(ui\.workbench|ui\.ide|core\.resources|core\.runtime|equinox\.common|core\.jobs|jface|swt\.win32\.win32\.x86_64|osgi|equinox\.registry|core\.commands|ui\.views)_[0-9]' | grep '\.jar$' | sed "s|^|$POOL/|" | tr '\n' ';')
  "C:/Program Files/Java/jdk-21/bin/javac" --release 21 -nowarn -encoding UTF-8 -proc:none -cp "$CP" -d "$C/classes" $(find "$B/src" -name '*.java')
  cp -r "$B/META-INF" "$B/plugin.xml" "$C/classes/"
  (cd "$C/classes" && "C:/Program Files/Java/jdk-21/bin/jar" cfm "$E/plugins/org.eclipse.ui.ide.m3bench_1.0.0.jar" META-INF/MANIFEST.MF .)
  # 2. configuration: platform bundles only (no EPP, Oomph, p2, intro), plus the bench bundle
  CFG=$E/config-$SIDE; rm -rf "$CFG"; mkdir -p "$CFG/org.eclipse.equinox.simpleconfigurator"
  BI=$INSTALL/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info
  { grep '^#' "$BI"
    grep -v '^#' "$BI" | grep -E '^(org\.eclipse\.(osgi|equinox|core|e4|emf|ui|jface|swt|help|text|urischeme|platform|ant\.core|update|compare\.core)|org\.apache\.(felix|aries|commons|batik|xmlgraphics|lucene)|org\.osgi|javax|jakarta|org\.w3c|com\.ibm\.icu|com\.sun\.jna|org\.objectweb|org\.sat4j|org\.tukaani|org\.bouncycastle|org\.apache\.httpcomponents)' \
      | grep -vE '^[^,]*(intro|equinox\.p2|oomph|cheatsheets|ui\.views\.log|ui\.trace|e4\.tools|ui\.monitoring)[^,]*,'
    echo "org.eclipse.ui.ide.m3bench,1.0.0,file:/$E/plugins/org.eclipse.ui.ide.m3bench_1.0.0.jar,4,false"
  } > "$CFG/org.eclipse.equinox.simpleconfigurator/bundles.info"
  if [ "$SIDE" != stock ]; then
    sed -i "s|^org\.eclipse\.ui\.ide,\([^,]*\),[^,]*,|org.eclipse.ui.ide,\1,file:/${IDE_JAR},|" "$CFG/org.eclipse.equinox.simpleconfigurator/bundles.info"
  fi
  SC=$(ls "$POOL" | grep '^org.eclipse.equinox.simpleconfigurator_' | head -1)
  FW=$(ls "$POOL" | grep '^org.eclipse.osgi_3' | head -1); CS=$(ls "$POOL" | grep '^org.eclipse.osgi.compatibility.state_' | head -1)
  cat > "$CFG/config.ini" <<CFGEOF
osgi.bundles=reference\:file\:${POOL/:/\:}/$SC@1\:start
osgi.bundles.defaultStartLevel=4
osgi.framework=file\:${POOL/:/\:}/$FW
osgi.framework.extensions=reference\:file\:${POOL/:/\:}/$CS
org.eclipse.equinox.simpleconfigurator.configUrl=file\:${CFG/:/\:}/org.eclipse.equinox.simpleconfigurator/bundles.info
eclipse.application=org.eclipse.ui.ide.workbench
eclipse.product=org.eclipse.platform.ide
osgi.splashPath=platform\:/base/plugins/org.eclipse.platform
CFGEOF
  echo "bundles: $(grep -vc '^#' "$CFG/org.eclipse.equinox.simpleconfigurator/bundles.info")"
  exec bash C:/m3-superset/tools/ui_lock.sh jface-viewport bash "$0" "$SIDE" "$N" "$SEED" "$STEPS" --inner "$INSTALL" "$POOL" "$JDK25"
fi
INSTALL=$6 POOL=$7 JDK25=$8
CFG=$E/config-$SIDE; WS=$E/ws-$SIDE-$N; rm -rf "$WS" "$E/user-$SIDE"; mkdir -p "$WS"
LAUNCHER=$(ls "$POOL" | grep '^org.eclipse.equinox.launcher_' | head -1)
RES=$E/markers-results.tsv; LABEL=$SIDE-$N
DIFF=""; [ -n "$SEED" ] && DIFF="-Dm3.markers.log=$(cygpath -w "$E/diff-$SIDE-$N-$SEED.log") -Dm3.markers.seed=$SEED -Dm3.markers.steps=$STEPS"
w() { cygpath -w "$1"; }
CMD="\"$(w "$JDK25/bin/java.exe")\" -Xmx${XMX:-4g} -XX:+UseG1GC --add-modules=ALL-SYSTEM -Djava.io.tmpdir=$(w "$OUT/tmp") -Dosgi.requiredJavaVersion=21 -Dosgi.user.area=$(w "$E/user-$SIDE") -Dm3.markers.out=$(w "$RES") -Dm3.markers.label=$LABEL -Dm3.markers.n=$N $DIFF ${MARKERS_OPTS:-} -jar $(w "$POOL/$LAUNCHER") -install $(w "$INSTALL") -configuration $(w "$CFG") -data $(w "$WS") -nosplash -consoleLog -clean"
powershell -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w C:/m3-superset/tools/run_hidden_desktop.ps1)" -TimeoutSec ${MARKERS_TIMEOUT:-1800} -Log "$(w "$E/run-$LABEL.log")" -CommandLine "$CMD" -Desktop "${M3_UI_DESKTOP:-m3-ui-tests}"
grep -P "^$LABEL\t" "$RES" || { echo "no rows for $LABEL; log tail:"; tail -30 "$E/run-$LABEL.log"; }
