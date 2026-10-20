# The Problems view is the reference tree view

Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0

User, 2026-10-20 (said four times): "problems view is the example of how a tree view should [work] with all its api".

The Problems view is the template every other tree view follows: Project Explorer, Outline, Tasks, Bookmarks, Debug,
Search, Call/Type hierarchy, Synchronize. What it proves once, the others reuse:

- one lane model (ids, parent/child topology, grouping keys, sort permutation, filter and scope bit lanes, interned
  labels), with lazy parent-owned facades;
- deltas from every change source, including model deltas, scope and selection set-diffs, filter edits and search
  keystrokes. See FALLBACKS_TO_DELTAS.md;
- the window patch: rows inside the window are patched, while outside it only counts, extents and the scrollbar
  change;
- resize and layout as viewport deltas (brief rule 24);
- debounce with a superseding generation, model work off the UI thread, and one apply per result.

## All of the API

TREE_VIEW_API_MATRIX.tsv lists every public and protected method of Viewer, ContentViewer, StructuredViewer,
ColumnViewer, AbstractTreeViewer and TreeViewer: 305 methods, generated mechanically from the sources with grep and
awk. Each row has:

- `used_by_markers_view`: call sites in org.eclipse.ui.internal.views.markers.
- `lane_delta_status`: how the method is answered on the lane model, as LANES, DELTA, FACADE (an object only on
  demand), STOCK_OK (no model work) or TODO.
- `differential`: whether stock and ours give the same answers and events in the reference view's differential. The
  value is PASS, FAIL or TODO.
- `delta_gate`: whether the method can trigger a full rebuild on a change path. The value is PASS (no) or
  JUSTIFIED (with file:line).

The reference is done when every row is filled in: no TODO, every differential PASS, and every delta_gate PASS or
JUSTIFIED. Methods the Problems view does not call itself are still exercised, through a generic tree-view test
that drives the same lane model. That is how "all its API" is covered, and that test becomes the template test for
every other tree view.

Regenerate the method list after an upstream sync with the command in the commit that added this file, then merge
the status columns by `class` + `signature`.
