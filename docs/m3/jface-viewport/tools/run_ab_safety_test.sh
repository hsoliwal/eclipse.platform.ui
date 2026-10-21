#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Isolation test for run_ab.sh; no desktop or real build required.
set -euo pipefail
SCRIPT=${1:?usage: run_ab_safety_test.sh <run_ab.sh>}
ROOT=$(mktemp -d)
trap 'rm -rf -- "$ROOT"' EXIT
mkdir -p -- "$ROOT/tools" "$ROOT/out/runs/bench-example-ab/cand-classes" \
  "$ROOT/out/runs/bench-example-ab/cand-tests" \
  "$ROOT/out/runs/bench-example-ab/stock-tests"
cp -- "$SCRIPT" "$ROOT/tools/run_ab.sh"
cat > "$ROOT/tools/jface_env.sh" <<'MOCK'
: "${OUT:?}"
mkdir -p "$OUT/tmp"
MOCK
cat > "$ROOT/tools/run_bench.sh" <<'MOCK'
#!/usr/bin/env bash
set -euo pipefail
[[ $1 == --inner && $2 == cand ]] || exit 4
printf '%s\t%s\n' "$3" "$JAVA_OPTS" > "$OUT/bench/$3.tsv"
MOCK
OUT="$ROOT/out" bash "$ROOT/tools/run_ab.sh" --inner example 2 0 1 1 100 1000
[[ -s $ROOT/out/bench/example-orig-1.tsv ]]
[[ -s $ROOT/out/bench/example-vp-2.tsv ]]
grep -q 'jface.viewer.mode=original' "$ROOT/out/bench/example-orig-1.tsv"
grep -q 'jface.viewer.mode=viewport' "$ROOT/out/bench/example-vp-2.tsv"
printf 'untouched' > "$ROOT/out/runs/bench-example-ab/provenance.txt"
if OUT="$ROOT/out" bash "$ROOT/tools/run_ab.sh" example 1 0 1 1 100 >/dev/null 2>&1; then
  echo 'expected duplicate snapshot rejection' >&2
  exit 1
fi
[[ $(cat "$ROOT/out/runs/bench-example-ab/provenance.txt") == untouched ]]
if OUT="$ROOT/out" bash "$ROOT/tools/run_ab.sh" --inner example 1 0 1 1 100 >/dev/null 2>&1; then
  echo 'expected duplicate benchmark rejection' >&2
  exit 1
fi
if OUT="$ROOT/out" bash "$ROOT/tools/run_ab.sh" ../escape 1 0 1 1 100 >/dev/null 2>&1; then
  echo 'expected invalid path rejection' >&2
  exit 1
fi
if OUT="$ROOT/out" bash "$ROOT/tools/run_ab.sh" --inner example 1 1 0 1 100 >/dev/null 2>&1; then
  echo 'expected invalid scenario range rejection' >&2
  exit 1
fi
if OUT=/tmp/.. bash "$ROOT/tools/run_ab.sh" --inner example 1 0 1 1 100 >/dev/null 2>&1; then
  echo 'expected root output rejection' >&2
  exit 1
fi
printf 'PASS: snapshot, duplicate, traversal, range, root, mode A/B checks\n'
