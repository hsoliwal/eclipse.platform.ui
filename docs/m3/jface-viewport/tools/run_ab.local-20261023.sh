#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Same-build A/B of the JFace viewer modes (original vs viewport) for a range of bench scenarios, interleaved and
# repeated under ONE desktop slot, so both modes see the same machine load (the r3 rows were one run per mode under
# 3-slot contention). Usage: run_ab.sh <tag> <reps> <first> <last> <budget-seconds> <n>...
# Results: $OUT/bench/<tag>-orig-<rep>.tsv and <tag>-vp-<rep>.tsv; medians: awk over the two sets (median per scenario and size).
. "$(dirname "$0")/jface_env.sh"
if [ "$1" != --inner ]; then
  . "$(dirname "$0")/snapshot.sh"
  snapshot "$OUT/runs/bench-$1-ab"
  exec bash C:/m3-superset/tools/ui_lock.sh jface-viewport bash "$0" --inner "$@"
fi
shift
TAG=$1 REPS=$2 FIRST=$3 LAST=$4 BUDGET=$5; shift 5
D=$(dirname "$0")
for rep in $(seq 1 "$REPS"); do
  for mode in original viewport; do
    if [ "$mode" = original ]; then label=$TAG-orig-$rep; else label=$TAG-vp-$rep; fi
    rm -rf "$OUT/runs/bench-$label"; cp -r "$OUT/runs/bench-$TAG-ab" "$OUT/runs/bench-$label"
    JAVA_OPTS=-Djface.viewer.mode=$mode BENCH_FIRST=$FIRST BENCH_LAST=$LAST \
      bash "$D/run_bench.sh" --inner cand "$label" "$BUDGET" "$@" > /dev/null
  done
done
echo "done: $OUT/bench/$TAG-{orig,vp}-*.tsv"
