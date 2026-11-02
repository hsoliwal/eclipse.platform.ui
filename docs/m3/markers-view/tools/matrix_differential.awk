# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Second mechanical pass over TREE_VIEW_API_MATRIX.tsv: the `differential` column. A method the Problems view calls
# (used_by_markers_view > 0) is driven by the Problems view differential (markers_round.sh, stock vs m3 logs
# identical), so its cell becomes PASS with the run id; a method the view never calls keeps no claim: the cell says
# so (NOT_USED) until the generic tree-view test covers it. A cell that is not TODO is kept (a manual verdict wins).
# Usage: awk -F'\t' -v OFS='\t' -v run=<run id, e.g. "10^4 seed 7 + 10^5 seed 7, 2026-10-24"> -f matrix_differential.awk TREE_VIEW_API_MATRIX.tsv > out.tsv
NR == 1 { print; next }
{
  if ($7 == "TODO" || $7 == "") {
    if ($5 + 0 > 0) $7 = "PASS (Problems view differential " run ")"
    else $7 = "NOT_USED (the Problems view never calls it; the generic tree-view test decides)"
  }
  print
}
