# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Mechanical pass over TREE_VIEW_API_MATRIX.tsv (REFERENCE_TREE_VIEW.md): fills lane_delta_status, differential and
# delta_gate by method-name rules. A cell that is not TODO is kept (a later manual verdict wins). Usage:
#   awk -F'\t' -v OFS='\t' -v la=<layer-A evidence> -f matrix_status.awk TREE_VIEW_API_MATRIX.tsv > out.tsv
# Rules (the lane model of the Problems view, M3MarkerLanes/M3MarkerDeltas, and the JFace delta API):
#   DELTA     element changes that the delta path issues or that serve it (add/insert/remove/update/replace, the
#             item mapping, lazy children)
#   FACADE    answers about elements: the element is a facade created on demand (selection, find, reveal,
#             expansion, children, input)
#   STOCK_OK  no model work: listeners, events, editing support, providers, data, busy, scrolling
#   TODO      viewer-side filters and comparators: the generic tree-view test on the lane model decides them
#             (the Problems view filters and sorts in its model)
# differential: PASS where JFace Layer A (M3ViewerDifferential, 0 mismatches) drives the method, else TODO until the
# Problems view differential and the generic test cover it. delta_gate: JUSTIFIED for the full-rebuild entry points
# (initial input, user Refresh, the fallbacks of FALLBACKS_TO_DELTAS.md), PASS for every other method.
function cls(m) {
  if (m ~ /^(add|insert|remove|replace|update|internalAdd|internalRemove|internalUpdate|updateItem|refreshItem|doUpdateItem|setChildCount|setHasChildren|updatePlus|associate|disassociate|mapElement|unmapElement|unmapAllElements|createTreeItem|newItem|createChildren|updateChildren|preservingSelection|removeAll)$/) return "DELTA"
  if (m ~ /^(refresh|internalRefresh|internalRefreshStruct|setInput)$/) return "DELTA"
  if (m ~ /^(filter|hasFilters|getFilters|setFilters|addFilter|removeFilter|resetFilters|needsRefilter|setComparator|getComparator|setSorter|getSorter)$/) return "TODO"
  if (m ~ /Selection|^(reveal|showItem|findItem|findItems|doFindItem|doFindInputItem|testFindItem|testFindItems|getTreePathFromItem|getParentElement|getRawChildren|getFilteredChildren|getSortedChildren|getChildren|getRoot|getInput|inputChanged|contains|indexForElement|getItemCount|getItems|getChild|getNextItem|getPreviousItem|getParentItem|internalGetWidgetToSelect|internalInitializeTree|isExpandable|isExpandableNode)$/) return "FACADE"
  if (m ~ /[Ee]xpand|[Cc]ollapse/ && m !~ /^fire|^handle|Listener$/) return "FACADE"
  return "STOCK_OK"
}
function layerA(m) {
  return m ~ /^(setSelection|getSelection|getStructuredSelection|refresh|remove|add|update|setComparator|addFilter|removeFilter|resetFilters|setInput|reveal|setUseHashlookup|getComparator|getFilters|internalRefresh|internalRemove|internalAdd|internalUpdate|preservingSelection|setSelectionToWidget|getSelectionFromWidget|findItem|doFindItem|testFindItem)$/
}
NR == 1 { print; next }
{
  m = $3
  if ($6 == "TODO" || $6 == "") $6 = cls(m)
  if ($7 == "TODO" || $7 == "") $7 = layerA(m) ? "PASS (" la ")" : "TODO"
  if ($8 == "TODO" || $8 == "") $8 = (m ~ /^(refresh|setInput|internalRefresh|internalRefreshStruct|removeAll)$/) ? "JUSTIFIED (initial input, user Refresh, fallbacks; DELTA_GATE.tsv UIUpdateJob.java:104, ExtendedMarkersView.java:510)" : "PASS"
  print
}
