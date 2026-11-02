#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Ledger rows (docs/m3/jface-viewport/OPTIMIZATION_LEDGER.tsv format) from the markers bench results: for each size
# and metric, the median over the repetitions of stock (before) and m3 (after). Usage:
#   ledger_rows.sh <leaf> <commit> [results.tsv] [before-side] [after-side]   (prints rows; append them to the ledger)
LEAF=$1 COMMIT=$2 RES=${3:-S:/m3-scratch/jface/eclipse/markers-results.tsv} BEFORE=${4:-stock} AFTER=${5:-m3}
awk -F'\t' -v OFS='\t' -v leaf="$LEAF" -v commit="$COMMIT" -v before="$BEFORE" -v after="$AFTER" -v res="$RES" '
  function median(key,   n, i, j, t, v) {
    n = split(vals[key], v, " ")
    for (i = 2; i <= n; i++) for (j = i; j > 1 && v[j-1] + 0 > v[j] + 0; j--) { t = v[j]; v[j] = v[j-1]; v[j-1] = t }
    return n == 0 ? "" : (n % 2 ? v[(n + 1) / 2] : (v[n / 2] + v[n / 2 + 1]) / 2)
  }
  {
    split($1, s, "-"); side = s[1]; metric = $2; n = $3
    if (metric ~ /^(ERROR|diff-counters)/ || metric ~ /^instances / && $4 + 0 == 0) next
    key = side SUBSEP metric SUBSEP n
    vals[key] = vals[key] " " $4; seen[metric SUBSEP n] = 1
  }
  END {
    for (k in seen) {
      split(k, a, SUBSEP); metric = a[1]; n = a[2]
      b = median(before SUBSEP metric SUBSEP n); c = median(after SUBSEP metric SUBSEP n)
      if (b == "" || c == "") continue
      unit = metric ~ /bytes/ ? "bytes" : metric ~ /-ms$|^initial-build$|^create-markers$/ ? "ms" : "count"
      print leaf, metric, n, b, c, unit, "markers bench, median of the repetitions (5; 3 for 10^4-marker deltas), hidden desktop, JDK 25, same flags both sides", "order-statistic-list;interned-labels;open-addressing-id-index", commit, res
    }
  }' "$RES" | sort -t$'\t' -k3,3n -k2,2
