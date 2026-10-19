#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# One measurement round: stock, fork-original (overlay, delta path off) and m3 (overlay, delta path on) per size,
# each with the seeded differential script. Usage: markers_round.sh <seed> <steps> <n>...
D=$(dirname "$0"); SEED=$1 STEPS=$2; shift 2
export IDE_JAR=S:/m3-scratch/jface/eclipse/plugins/org.eclipse.ui.ide_m3.jar
for n in "$@"; do
  MARKERS_OPTS= bash "$D/run_markers.sh" stock "$n" "$SEED" "$STEPS"
  MARKERS_OPTS= bash "$D/run_markers.sh" m3orig "$n" "$SEED" "$STEPS"
  MARKERS_OPTS=-Dorg.eclipse.ui.ide.markers.mode=viewport bash "$D/run_markers.sh" m3 "$n" "$SEED" "$STEPS"
  E=S:/m3-scratch/jface/eclipse
  for side in m3orig m3; do
    if diff -q "$E/diff-stock-$n-$SEED.log" "$E/diff-$side-$n-$SEED.log" > /dev/null 2>&1; then echo "n=$n $side vs stock: IDENTICAL"; else echo "n=$n $side vs stock: DIFFERENT (diff $E/diff-stock-$n-$SEED.log $E/diff-$side-$n-$SEED.log)"; fi
  done
done
