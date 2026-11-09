#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# G5 for one fork with the NLS pre-check first: nls_precheck.sh (ECJ rows) -> fix_nls_tags.sh (log mode + lane scan)
# -> build_fork.sh. Usage: g5_fork.sh <repo> [mvn args]   -> gate/<repo>.precheck.*, gate/<repo>.nls.tsv, build/<repo>.*
set -uo pipefail
R="$1"; shift; T=F:/m3-scratch/rel21/tools; G=F:/m3-scratch/rel21/gate; W=F:/m3-scratch/rel21/forks/$R
bash "$T/nls_precheck.sh" "$W" "$G/$R.precheck"
{ bash "$T/fix_nls_tags.sh" "$W" "$G/$R.precheck.mvnlog"; bash "$T/fix_nls_tags.sh" "$W" --scan; } > "$G/$R.nls.tsv"
echo "$R nls fixed before the build: $(wc -l < "$G/$R.nls.tsv") lines; re-check: $(bash "$T/nls_precheck.sh" "$W" "$G/$R.precheck2" | sed 's/.*files, //')"
bash "$T/build_fork.sh" "$R" "$@"
