#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Run M3ViewerBench on the hidden desktop for one side (stock = official JFace jar, cand = this worktree's build).
# Usage: run_bench.sh <stock|cand> <label> <budget-seconds> <n>...   (JAVA_OPTS e.g. -Djface.viewer.mode=viewport)
# Each size runs in fresh JVMs; a TIMEOUT row restarts the JVM at the next scenario. The build outputs are frozen
# into $OUT/runs/bench-<label> before queueing, so a later rebuild cannot affect the run.
. "$(dirname "$0")/jface_env.sh"
if [ "$1" != --inner ]; then
  . "$(dirname "$0")/snapshot.sh"; snapshot "$OUT/runs/bench-$2"
  exec bash C:/m3-superset/tools/ui_lock.sh jface-viewport bash "$0" --inner "$@"
fi
shift
SIDE=$1 LABEL=$2 BUDGET=$3; shift 3
RUN=$OUT/runs/bench-$LABEL
if [ "$SIDE" = stock ]; then JF="$STOCK_JFACE"; TESTS=$RUN/stock-tests; else JF="$RUN/cand-classes"; TESTS=$RUN/cand-tests; fi
CP="$(cygpath -w "$TESTS");$(cygpath -w "$JF");$(cygpath -w "$SWT_JAR");$(echo "$DEPS" | tr ';' '\n' | while read -r d; do printf '%s;' "$(cygpath -w "$d")"; done)"
RES=$OUT/bench/$LABEL.tsv; mkdir -p "$OUT/bench"
LAST=$(( ${BENCH_LAST:-22} ))
for n in "$@"; do
  first=${BENCH_FIRST:-0}
  while [ "$first" -le "$LAST" ]; do
    bash C:/m3-superset/tools/hidden_java.sh $(( BUDGET * (LAST - first + 2) + 60 )) "$OUT/bench/$LABEL-$n-$first.log" \
      -Xmx${XMX:-1500m} -Djava.io.tmpdir=$(cygpath -w "$OUT/tmp") ${JAVA_OPTS:-} -cp "$CP" \
      org.eclipse.jface.tests.viewers.m3.M3ViewerBench "$(cygpath -w "$RES")" "$LABEL" "$n" "$first" "$BUDGET" "$LAST"
    done_idx=$(awk -F'\t' -v n="$n" '$3 == n { print $6 }' "$RES" 2>/dev/null | sort -n | tail -1)
    if [ -z "$done_idx" ] || [ "$done_idx" -lt "$first" ]; then
      echo "n=$n scenario $first produced no row (crash?): see $OUT/bench/$LABEL-$n-$first.log"; first=$((first + 1))
    else
      first=$((done_idx + 1))
    fi
  done
done
cat "$RES"
