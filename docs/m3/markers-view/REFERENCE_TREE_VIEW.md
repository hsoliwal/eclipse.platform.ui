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

## Hidden viewports: one helper for every view

User, 2026-10-20: "if the viewport becomes visible from hidden it should paint; while it is hidden the model may make
changes and the viewport is dirty and renders on becoming visible", and "IPartListener can base it on this".

`org.eclipse.ui.internal.m3.M3PartVisibility` (org.eclipse.ui.workbench) is the one helper. A view calls
`M3PartVisibility.watch(part, onShown)` once. The workbench drives it: `IPartListener2.partHidden` hides the part
(behind another tab, in a minimized or inactive stack), and `partVisible` shows it. While the part is hidden, the view
applies its deltas to the lanes, keeps a coalesced dirty window (the touched categories and the changed ids), and calls
`markDirty()`. It does no tree work. On `partVisible`, `onShown` runs once, inside the event and before the first
paint, and applies ONE window patch against the current lanes.

The Problems view uses it in `M3MarkerDeltas.apply` and `shown`. The bench's hidden sweep minimizes the view, changes
up to 10^4 markers in one operation and restores the view. It records paints and item changes while hidden (both must
be 0), the rows after the restore (in the differential log, compared with stock), and `shown-patches` (one per hidden
period with changes).

### Source of truth: SWT (brief rule 25), M3PartVisibility is the interim bridge

User, 2026-10-20: "you own the implementation, you are not a layer on it", "INTRODUCE A NEW LISTENER PART VISIBLE",
"the viewport becoming [visible] should come from SWT or Swing".

- The source of truth is a new SWT event, `SWT.PartVisible` / `SWT.PartHidden`, with `Control.isPartVisible()`. The
  all-widgets agent is adding it to SWT itself. The Problems view's tree hears it directly from SWT: the viewport
  lives inside `Tree`. The view then needs no listener of its own. The tree defers its native work while part-hidden
  and applies one window patch and one paint on `SWT.PartVisible`.
- Workbench `IPartListener2.partVisible` / `partHidden` and the e4 part visibility events are built on that SWT event
  in the e4 SWT renderer, so there is one source. They are not computed separately.
- Until that event exists in the SWT this fork runs on, `M3PartVisibility` (org.eclipse.ui.workbench) stands in for
  it, on `IPartListener2`. Once SWT delivers `SWT.PartVisible`, `M3PartVisibility.isHidden()` becomes
  `!control.isPartVisible()` and its callback becomes an `SWT.PartVisible` listener on the view's tree. The
  view-side contract stays the same: lanes take deltas while hidden, and one patch runs on show. This is the change
  to make when the SWT event lands; it is tracked in TREE_VIEW_API_MATRIX.tsv.

## Adoption recipe: from TreeViewer + refresh() to deltas (rule 27 template)

A view moves in five mechanical steps. Each step is a recipe hook like the Problems view's
(docs/m3/markers-view/recipe), and the second recipe pass must report UNCHANGED.

1. **Spec.** Write `<ViewClass>.m3view` next to the view class (`M3ViewSpec`, org.eclipse.jface.viewers.m3). It
   names the adapter, the lanes, grouping, sort, filters, counts and presentation. Example: ProblemsView.m3view.
2. **Change source.** Hook the view's model listener at the line where it schedules its rebuild: offer the event to
   the adapter first, and keep the rebuild for what the adapter declines (`if (Adapter.offer(...)) return;`).
   The Problems view does this at `MarkersChangeListener.resourceChanged` before `scheduleUpdate()`.
3. **Model.** Where the view builds its element list, build the lanes instead (`Markers.updateWithNewMarkers` ->
   `M3MarkerDeltas.build`). The view's model accessors (elements, children, counts, item lookup, clone) answer from
   the lanes, and facades are created only for shown rows.
4. **Content provider.** Children and elements return the parent's window from the lanes. hasChildren comes from a
   count, never from materialized children.
5. **Patch.** The adapter's job applies the lanes change on the UI thread and patches the tree through one
   `M3ViewerDelta` (`M3Viewers.apply`) inside `preservingSelection`. While the part is hidden it only marks dirty,
   and on show it patches once (M3PartVisibility now, SWT.PartVisible next). Scope and filter changes diff the
   selected ids against the lanes instead of rebuilding.

The template runtime factors steps 3 to 5 out of M3MarkerLanes and M3MarkerDeltas into generic parts: lane schema,
adapter interface, scope/filter/search lanes, grouping and sort lanes, delta viewer and counts. After that, a view
conversion is its spec plus steps 1 and 2.
