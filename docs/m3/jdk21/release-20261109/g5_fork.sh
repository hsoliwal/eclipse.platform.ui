#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# G5 for one fork with the ECJ pre-check first: ecj_precheck.sh (ECJ rows: NLS tags, unused imports, per-bundle prefs)
# -> fix_nls_tags.sh + fix_unused_imports.sh -> re-check -> build_fork.sh.
# Usage: g5_fork.sh <repo> [mvn args]   -> gate/<repo>.ecj.*, gate/<repo>.{nls,imports}.tsv, build/<repo>.*
set -uo pipefail
R="$1"; shift; T=F:/m3-scratch/rel21/tools; G=F:/m3-scratch/rel21/gate; W=F:/m3-scratch/rel21/forks/$R
bash "$T/ecj_precheck.sh" "$W" "$G/$R.ecj"
bash "$T/fix_nls_tags.sh" "$W" "$G/$R.ecj.mvnlog" >> "$G/$R.nls.tsv"
bash "$T/fix_unused_imports.sh" "$W" "$G/$R.ecj.mvnlog" >> "$G/$R.imports.tsv"
echo "$R after fixes: $(bash "$T/ecj_precheck.sh" "$W" "$G/$R.ecj2" | sed 's/.*groups; //')"
bash "$T/build_fork.sh" "$R" "$@"
