# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# snapshot <run-dir>: freeze the current build outputs for one run, so a concurrent rebuild cannot delete classes
# under a running JVM (measured 2026-10-19: rebuild during a bench run gave ClassNotFoundException).
snapshot() {
  local R=$1; rm -rf "$R"; mkdir -p "$R"
  cp -r "$OUT/cand/classes" "$R/cand-classes"; cp -r "$OUT/cand/tests" "$R/cand-tests"; cp -r "$OUT/stock/tests" "$R/stock-tests"
}
