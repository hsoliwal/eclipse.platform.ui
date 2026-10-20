#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Recorded differential: the official JFace jar (stock) against this build in original and viewport mode, on the
# hidden desktop. Usage: run_differential.sh <tag> <first-seed> <seed-count> [steps]   (COVER=1 adds JaCoCo)
. "$(dirname "$0")/jface_env.sh"
if [ "$1" != --inner ]; then
  . "$(dirname "$0")/snapshot.sh"; snapshot "$OUT/runs/diff-$2"
  exec bash "$(dirname "$0")/ui_lock_local.sh" jface-viewport bash "$0" --inner "$@"
fi
shift
TAG=$1 FIRST=$2 COUNT=$3 STEPS=${4:-60}
RUN=$OUT/runs/diff-$TAG; D=$OUT/diff/$TAG; rm -rf "$D"; mkdir -p "$D" "$OUT/coverage"
deps() { echo "$DEPS" | tr ';' '\n' | while read -r d; do printf '%s;' "$(cygpath -w "$d")"; done; }
for side in stock original viewport; do
  if [ $side = stock ]; then JF="$STOCK_JFACE"; T="$RUN/stock-tests"; MODE=original; else JF="$RUN/cand-classes"; T="$RUN/cand-tests"; MODE=$side; fi
  AGENT=""; [ $side != stock ] && [ -n "$COVER" ] && AGENT="-javaagent:$(cygpath -w "$JACOCO_AGENT")=destfile=$(cygpath -w "$OUT/coverage/diff-$TAG-$side.exec"),includes=org.eclipse.jface.*"
  bash C:/m3-superset/tools/hidden_java.sh 1800 "$D/$side.log" -Xmx1g -Djava.io.tmpdir=$(cygpath -w "$OUT/tmp") \
    -Djface.viewer.mode=$MODE $AGENT -cp "$(cygpath -w "$T");$(cygpath -w "$JF");$(cygpath -w "$SWT_JAR");$(deps)" \
    org.eclipse.jface.tests.viewers.m3.M3ViewerDifferential "$(cygpath -w "$D/$side")" "$FIRST" "$COUNT" "$STEPS"
done
for side in original viewport; do
  n=$(ls "$D/stock" 2>/dev/null | wc -l); m=$(ls "$D/$side" 2>/dev/null | wc -l)
  if [ "$n" -gt 0 ] && diff -rq "$D/stock" "$D/$side" > "$D/$side.diff" 2>&1; then echo "$side vs stock: IDENTICAL ($m/$n logs)"; else echo "$side vs stock: $(wc -l < "$D/$side.diff") differing logs ($m/$n)"; fi
done
