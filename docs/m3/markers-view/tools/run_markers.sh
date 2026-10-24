#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Markers view benchmark/differential inside the official Eclipse 2026-09 bundles (read from the p2 pool, never
# written), on the hidden desktop, with configuration, workspace, user area and temp on S:.
# Usage: run_markers.sh <stock|m3|m3orig> <n> [seed steps]   (non-stock sides: IDE_JAR and JFACE_JAR overlays from
# build_overlay.sh; MARKERS_OPTS=-Dorg.eclipse.ui.ide.markers.mode=viewport -Djface.viewer.mode=viewport for m3)
set -e
WT=${WT:-S:/m3-scratch/worktrees/platform-ui}
OUT=${OUT:-S:/m3-scratch/jface}; E=$OUT/eclipse; mkdir -p "$E/plugins" "$OUT/tmp"
export TEMP=$OUT/tmp TMP=$OUT/tmp
SIDE=$1 N=$2 SEED=${3:-} STEPS=${4:-30}
if [ "$5" != --inner ]; then
  INSTALL=$(cygpath -m ~/eclipse/jee-2026-09/eclipse); POOL=$(cygpath -m ~/.p2/pool/plugins)
  JDK25=F:/jdks/temurin25/jdk-25.0.4.1+1
  # one configuration per side and size: runs of one side in two desktop slots never share a configuration area;
  # the bench jar and the overlay jars are copied into it, so a rebuild never touches a queued or running run
  CFG=$E/config-$SIDE-$N; rm -rf "$CFG"; mkdir -p "$CFG/org.eclipse.equinox.simpleconfigurator" "$CFG/jars"
  # 1. bench bundle
  B=$WT/docs/m3/markers-view/bench-bundle; C=$OUT/markers-bench-$SIDE-$N; rm -rf "$C" && mkdir -p "$C/classes"
  CP=$(ls "$POOL" | grep -E '^org\.eclipse\.(ui\.workbench|ui\.ide|core\.resources|core\.runtime|equinox\.common|core\.jobs|jface|swt\.win32\.win32\.x86_64|osgi|equinox\.registry|core\.commands|ui\.views)_[0-9]' | grep '\.jar$' | sed "s|^|$POOL/|" | tr '\n' ';')
  "C:/Program Files/Java/jdk-21/bin/javac" --release 21 -nowarn -encoding UTF-8 -proc:none -cp "$CP" -d "$C/classes" $(find "$B/src" -name '*.java')
  cp -r "$B/META-INF" "$B/plugin.xml" "$C/classes/"
  (cd "$C/classes" && "C:/Program Files/Java/jdk-21/bin/jar" cfm "$CFG/jars/org.eclipse.ui.ide.m3bench_1.0.0.jar" META-INF/MANIFEST.MF .)
  # 2. configuration: platform bundles only (no EPP, Oomph, p2, intro), plus the bench bundle
  BI=$INSTALL/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info
  { grep '^#' "$BI"
    grep -v '^#' "$BI" | grep -E '^(org\.eclipse\.(osgi|equinox|core|e4|emf|ui|jface|swt|help|text|urischeme|platform|ant\.core|update|compare|debug|search|ltk|team|ecf)|org\.slf4j|ch\.qos|org\.apache\.(felix|aries|commons|batik|xmlgraphics|lucene|ant)|org\.osgi|javax|jakarta|org\.w3c|com\.ibm\.icu|com\.sun\.jna|org\.objectweb|org\.sat4j|org\.tukaani|org\.bouncycastle|bc[a-z]+,|org\.apache\.httpcomponents|com\.github\.weisj)' \
      | grep -vE '^[^,]*(equinox\.p2\.ui|p2\.updatechecker|oomph|epp|cheatsheets|ui\.views\.log|ui\.trace|e4\.tools|ui\.monitoring|team\.svn)[^,]*,'
    echo "org.eclipse.ui.ide.m3bench,1.0.0,file:/$CFG/jars/org.eclipse.ui.ide.m3bench_1.0.0.jar,4,false"
  } > "$CFG/org.eclipse.equinox.simpleconfigurator/bundles.info"
  if [ "$SIDE" != stock ]; then
    # snapshot of the overlays (build_overlay.sh) at queue time
    cp "${IDE_JAR:-$E/plugins/org.eclipse.ui.ide_m3.jar}" "$CFG/jars/org.eclipse.ui.ide_m3.jar"
    cp "${JFACE_JAR:-$E/plugins/org.eclipse.jface_m3.jar}" "$CFG/jars/org.eclipse.jface_m3.jar"
    cp "${WORKBENCH_JAR:-$E/plugins/org.eclipse.ui.workbench_m3.jar}" "$CFG/jars/org.eclipse.ui.workbench_m3.jar"
    for b in ui.ide jface ui.workbench; do
      sed -i "s|^org\.eclipse\.${b//./\\.},\([^,]*\),[^,]*,|org.eclipse.$b,\1,file:/$CFG/jars/org.eclipse.${b}_m3.jar,|" "$CFG/org.eclipse.equinox.simpleconfigurator/bundles.info"
    done
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
eclipse.pluginCustomization=${CFG/:/\:}/plugin_customization.ini
osgi.configuration.cascaded=false
CFGEOF
  # no welcome page, no workspace prompt, no update check: nothing but the workbench and the bench
  printf '%s\n' org.eclipse.ui/showIntro=false org.eclipse.ui.ide/SHOW_WORKSPACE_SELECTION_DIALOG=false \
    org.eclipse.equinox.p2.ui.sdk.scheduler/enabled=false org.eclipse.ui/windows.defender.startup.check.skip=true > "$CFG/plugin_customization.ini"   # qualifier org.eclipse.ui: WorkbenchPlugin.PI_WORKBENCH is PlatformUI.PLUGIN_ID
  echo "bundles: $(grep -vc '^#' "$CFG/org.eclipse.equinox.simpleconfigurator/bundles.info")"
  # 3. first start outside the lock, headless (no application): Equinox installs the framework extensions and asks
  #    for a restart (exit 23, which only the native launcher handles), the second start resolves; the measured run
  #    then starts without -clean
  LAUNCHER=$(ls "$POOL" | grep "^org.eclipse.equinox.launcher_" | head -1)
  for clean in -clean ""; do
    "$JDK25/bin/java.exe" -Xmx1g -XX:ErrorFile="$(cygpath -w "$OUT/tmp")\hs_err_%p.log" -Djava.io.tmpdir="$(cygpath -w "$OUT/tmp")" -jar "$POOL/$LAUNCHER" -install "$INSTALL" -configuration "$(cygpath -w "$CFG")" \
      -data "$(cygpath -w "$E/ws-warm-$SIDE-$N")" -nosplash -consoleLog $clean -application m3.none.app > "$E/warm-$SIDE-$N.log" 2>&1 \
      && rc=0 || rc=$?
    echo "warm start ${clean:-plain}: exit $rc"
  done
  echo "unresolved platform UI bundles: $(grep -cE "Could not resolve module: org\.eclipse\.(jface|swt|ui\.ide|ui\.workbench|ui\.views|e4\.ui)" "$E/warm-$SIDE-$N.log" || true)"
  [ -n "${WARM_ONLY:-}" ] && exit 0
  exec bash C:/m3-superset/tools/ui_lock.sh jface-viewport bash "$0" "$SIDE" "$N" "$SEED" "$STEPS" --inner "$INSTALL" "$POOL" "$JDK25"
fi
INSTALL=$6 POOL=$7 JDK25=$8
# a 4 GB-heap workbench on a starved machine blocks silently (2026-10-24: two runs hit the timeout at 310 MB free):
# wait up to 10 minutes for 6 GB of free RAM, then run anyway with a line in the output
for i in $(seq 1 20); do
  ram=$(powershell.exe -NoProfile -Command "[int]((Get-CimInstance Win32_OperatingSystem).FreePhysicalMemory/1KB)" 2>/dev/null | tr -d '\r')
  [ "${ram:-0}" -ge 6000 ] && break
  echo "free RAM ${ram:-?} MB under 6 GB: waiting ($i/20)"; sleep 30
done
CFG=$E/config-$SIDE-$N; [ -d "$CFG" ] || CFG=$E/config-$SIDE
WS=$E/ws-$SIDE-$N; rm -rf "$WS" "$E/user-$SIDE-$N"; mkdir -p "$WS"
LAUNCHER=$(ls "$POOL" | grep '^org.eclipse.equinox.launcher_' | head -1)
RES=$E/markers-results.tsv; LABEL=$SIDE-$N
COV=""
if [ -n "${COVER:-}" ] && [ "$SIDE" != stock ]; then
  # JaCoCo over the M3 classes the run exercises (markers lanes and delta path, the viewer delta API, the part visibility bridge)
  AG=$(cygpath -m "$HOME/.m2/repository")/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar; mkdir -p "$OUT/coverage"
  COV="-javaagent:$(cygpath -w "$AG")=destfile=$(cygpath -w "$OUT/coverage/markers-$SIDE-$N.exec"),includes=org.eclipse.ui.internal.views.markers.M3*:org.eclipse.jface.viewers.m3.*:org.eclipse.ui.internal.m3.*"
fi
DIFF=""; [ -n "$SEED" ] && DIFF="-Dm3.markers.log=$(cygpath -w "$E/diff-$SIDE-$N-$SEED.log") -Dm3.markers.seed=$SEED -Dm3.markers.steps=$STEPS"
w() { cygpath -w "$1"; }
CMD="\"$(w "$JDK25/bin/java.exe")\" -Xmx${XMX:-4g} -XX:+UseG1GC -XX:ErrorFile=$(w "$OUT/tmp")\hs_err_%p.log --add-modules=ALL-SYSTEM -Djava.io.tmpdir=$(w "$OUT/tmp") -Dosgi.requiredJavaVersion=21 -Dosgi.user.area=$(w "$E/user-$SIDE-$N") -Dm3.markers.out=$(w "$RES") -Dm3.markers.label=$LABEL -Dm3.markers.n=$N $DIFF $COV ${MARKERS_OPTS:-} -jar $(w "$POOL/$LAUNCHER") -install $(w "$INSTALL") -configuration $(w "$CFG") -data $(w "$WS") -nosplash -consoleLog ${MARKERS_ARGS:-}"
powershell -NoProfile -ExecutionPolicy Bypass -File "$(cygpath -w C:/m3-superset/tools/run_hidden_desktop.ps1)" -TimeoutSec ${MARKERS_TIMEOUT:-1800} -Log "$(w "$E/run-$LABEL.log")" -CommandLine "$CMD" -Desktop "${M3_UI_DESKTOP:-m3-ui-tests}" || echo "hidden desktop run: exit $?"
rows=$(awk -F'\t' -v l="$LABEL" '$1 == l' "$RES" 2>/dev/null); if [ -n "$rows" ]; then printf '%s\n' "$rows"; else echo "no rows for $LABEL; log tail:"; tail -30 "$E/run-$LABEL.log"; fi   # awk, not grep -P: the latter fails under LC_ALL=C
