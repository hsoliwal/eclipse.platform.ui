#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# One measurement round per size: stock (official bundles) and m3 (overlays from build_overlay.sh: JFace in viewport
# mode with the delta API, org.eclipse.ui.ide with the lane model and delta path on), each with the seeded
# differential script; then the two logs are compared, exactly and modulo the e4 FilteredTree's time-bounded expansion
# after a search text (MV-9, diff_modulo_expansion.awk). Usage: markers_round.sh <seed> <steps> <n>...
# SIDES="stock m3" by default (SIDES=m3 reruns one side against the kept stock log); m3orig (overlays, both modes off)
# can be added. OUT (default S:/m3-scratch/jface) selects the output tree, as in run_markers.sh.
D=$(dirname "$0"); SEED=$1 STEPS=$2; shift 2
E=${OUT:-S:/m3-scratch/jface}/eclipse
for n in "$@"; do
  for side in ${SIDES:-stock m3}; do
    case $side in
      m3) opts="-Dorg.eclipse.ui.ide.markers.mode=viewport -Djface.viewer.mode=viewport" ;;
      *) opts="" ;;
    esac
    MARKERS_OPTS="$opts ${EXTRA_OPTS:-}" bash "$D/run_markers.sh" "$side" "$n" "$SEED" "$STEPS"
  done
  for side in ${SIDES:-stock m3}; do
    [ "$side" = stock ] && continue
    if diff -q "$E/diff-stock-$n-$SEED.log" "$E/diff-$side-$n-$SEED.log" > /dev/null 2>&1; then
      echo "n=$n $side vs stock: IDENTICAL ($(wc -l < "$E/diff-$side-$n-$SEED.log") steps)"
    else
      echo "n=$n $side vs stock: DIFFERENT (diff $E/diff-stock-$n-$SEED.log $E/diff-$side-$n-$SEED.log)"
      awk -f "$D/diff_modulo_expansion.awk" "$E/diff-stock-$n-$SEED.log" "$E/diff-$side-$n-$SEED.log" | tail -n 2 | sed "s/^/n=$n $side: /"
    fi
  done
done
