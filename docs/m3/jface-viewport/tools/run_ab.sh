#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Same-build A/B of JFace viewer modes on one desktop slot.
# Usage: run_ab.sh <tag> <reps> <first> <last> <budget-seconds> <n>...
# Never discard an existing benchmark snapshot or a previous output set.
set -euo pipefail
D=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)
die() { printf 'run_ab: %s\n' "$*" >&2; exit 2; }

INNER=false
if [[ ${1:-} == --inner ]]; then
  INNER=true
  shift
fi
(( $# >= 6 )) || die 'usage: run_ab.sh <tag> <reps> <first> <last> <budget-seconds> <n>...'
TAG=$1 REPS=$2 FIRST=$3 LAST=$4 BUDGET=$5
shift 5
[[ $TAG =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$ ]] || die 'invalid benchmark tag'
positive() { [[ $1 =~ ^[0-9]{1,9}$ ]] && (( 10#$1 > 0 )); }
nonnegative() { [[ $1 =~ ^[0-9]{1,9}$ ]]; }
positive "$REPS" && (( 10#$REPS <= 100 )) || die 'repetitions must be 1..100'
nonnegative "$FIRST" && nonnegative "$LAST" && (( 10#$FIRST <= 10#$LAST && 10#$LAST <= 1000 )) || die 'invalid scenario range'
positive "$BUDGET" && (( 10#$BUDGET <= 3600 )) || die 'budget must be 1..3600 seconds'
for n in "$@"; do
  positive "$n" && (( 10#$n <= 100000000 )) || die 'invalid size n'
done

# The environment script defines OUT and the owning build locations.
. "$D/jface_env.sh"
[[ -n ${OUT:-} && $OUT != / && $OUT != . && $OUT != .. ]] || die 'unsafe output root'
[[ -d $OUT && ! -L $OUT/runs ]] || die 'unsafe or missing output root'
OUTPUT_ROOT=$(cd -- "$OUT" && pwd -P)
[[ $OUTPUT_ROOT != / ]] || die 'refusing filesystem root as output'
mkdir -p -- "$OUTPUT_ROOT/runs" "$OUTPUT_ROOT/bench"
RUN_ROOT=$(cd -- "$OUTPUT_ROOT/runs" && pwd -P)
[[ $RUN_ROOT == "$OUTPUT_ROOT/runs" ]] || die 'unsafe runs directory'
BASE="$RUN_ROOT/bench-$TAG-ab"

if [[ $INNER == false ]]; then
  [[ ! -e $BASE && ! -L $BASE ]] || die "snapshot already exists: $BASE (choose a new tag)"
  # mkdir is atomic. Refuse to replace another process's run or historical results.
  mkdir -- "$BASE" || die "snapshot was created concurrently: $BASE"
  cp -R -- "$OUT/cand/classes" "$BASE/cand-classes"
  cp -R -- "$OUT/cand/tests" "$BASE/cand-tests"
  cp -R -- "$OUT/stock/tests" "$BASE/stock-tests"
  exec bash C:/m3-superset/tools/ui_lock.sh jface-viewport bash "$0" --inner "$TAG" "$REPS" "$FIRST" "$LAST" "$BUDGET" "$@"
fi

[[ -d $BASE/cand-classes && -d $BASE/cand-tests && -d $BASE/stock-tests ]] || die 'missing frozen A/B source snapshot'
for (( rep=1; rep<=10#$REPS; rep++ )); do
  for mode in original viewport; do
    if [[ $mode == original ]]; then label="$TAG-orig-$rep"; else label="$TAG-vp-$rep"; fi
    DEST="$RUN_ROOT/bench-$label"
    [[ ! -e $DEST && ! -L $DEST && ! -e "$OUT/bench/$label.tsv" ]] || die "benchmark already exists: $label (choose a new tag)"
    mkdir -- "$DEST" || die "benchmark directory was created concurrently: $DEST"
    cp -R -- "$BASE/." "$DEST/"
    JAVA_OPTS="-Djface.viewer.mode=$mode" BENCH_FIRST=$FIRST BENCH_LAST=$LAST \
      bash "$D/run_bench.sh" --inner cand "$label" "$BUDGET" "$@" > /dev/null
  done
done
printf 'done: %s/bench/%s-{orig,vp}-*.tsv\n' "$OUT" "$TAG"
