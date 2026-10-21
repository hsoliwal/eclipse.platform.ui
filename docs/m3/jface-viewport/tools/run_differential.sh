#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Recorded differential: the official JFace jar (stock) against this build in original and viewport mode, on the
# hidden desktop. Usage: run_differential.sh <tag> <first-seed> <seed-count> [steps]   (COVER=1 adds JaCoCo)
. "$(dirname "$0")/jface_env.sh"
if [ "$1" != --inner ]; then
  . "$(dirname "$0")/snapshot.sh"; snapshot "$OUT/runs/diff-$1"
  exec bash C:/m3-superset/tools/ui_lock.sh jface-viewport bash "$0" --inner "$@"
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
# A log that differs only after a step where stock threw (the driver records "<step> exception ..." and ends the script)
# is a stock defect, not a mismatch, when every line before that step is identical: the defect goes to
# UPSTREAM_DEFECTS.tsv. Anything else that differs is a mismatch.
compare() { # compare <side>: prints the summary line, writes <side>.diff (mismatches) and <side>.defects
  local side=$1 same=0 defect=0 bad=0 f s
  : > "${D:?}/$side.diff"; : > "${D:?}/$side.defects"
  for f in $(ls "$D/stock"); do
    if cmp -s "$D/stock/$f" "$D/$side/$f"; then same=$((same + 1)); continue; fi
    s=$(grep -n -m1 '^[0-9]* exception ' "$D/stock/$f" | cut -d: -f1)
    if [ -n "$s" ] && [ -f "$D/$side/$f" ] && cmp -s <(head -n $((s - 1)) "$D/stock/$f") <(head -n $((s - 1)) "$D/$side/$f"); then
      defect=$((defect + 1)); echo "stock-defect $f line $s: $(sed -n "${s}p" "$D/stock/$f" | cut -c1-160)" >> "$D/$side.defects"
    else
      bad=$((bad + 1)); echo "$f" >> "$D/$side.diff"
    fi
  done
  echo "$side vs stock: $bad mismatches ($same identical, $defect identical up to a stock exception; $(ls "$D/$side" | wc -l)/$(ls "$D/stock" | wc -l) logs)"
}
for side in original viewport; do compare $side; done
