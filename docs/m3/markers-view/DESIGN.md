# Markers view on deltas and one model

Status: design and first leaf, 2026-10-19. Branch `m3/jface-viewport-20261019` (hsoliwal/eclipse.platform.ui).
Governing rule: memory `feedback-one-model-delta-processing.md`, user 2026-10-19. The user's words:
- "IResource supports resource deltas yet the markers view rebuilds the whole tree";
- "SWT has its model, JFace has its own, data binding too; very wasteful".

## 1. Stock behaviour (measured from source, `INVENTORY.tsv`)

One marker change runs this chain, in `org.eclipse.ui.internal.views.markers`:

1. **K1.** `MarkersChangeListener.resourceChanged` looks at the marker deltas only to ask "is any of them of a type I
   show?". The deltas are then dropped.
2. **K2/K3.** A clean `MarkerUpdateJob` starts from a new `LinkedList<MarkerEntry>`; its comment says "this is not
   incremental clean every time".
3. **K4.** `MarkerContentGenerator.gatherMarkers` calls `findMarkers` for every scope and does `new MarkerEntry(marker)`
   for every marker in the workspace (line 940). It also evaluates every filter for every marker.
4. **K5/K6.** `Markers.updateWithNewMarkers` copies the entries to an array, counts them and regroups them. It then
   partially sorts each category (the first `limit` entries, default 100).
5. **K7.** The UI update clones the model array (`Markers.getClone`).
6. **K8.** `viewer.refresh(true)` relabels every shown row and re-expands the categories.

The cost of one marker change is therefore O(n) allocations, O(n) filter calls, O(n log k) compares and a full
relabel. The models stack up: IDE `MarkerInfo` (resources), `MarkerEntry` (view), the cloned array, the JFace element
map and the SWT `TreeItem`s.

## 2. Target: deltas end to end over one model

```
IResourceChangeEvent --findMarkerDeltas--> IMarkerDelta[] (ADDED / REMOVED / CHANGED, marker id)
   -> M3MarkerDeltas (lanes: long id, byte kind, resource) coalesced under the existing 150 ms debounce;
      a newer generation supersedes an older one
   -> the model patch: per category, a sorted slot run; each changed marker is removed and re-inserted by
      binary search with the view's own comparator, O(log n) compares + one arraycopy
   -> the viewer patch per category window [0, min(size, limit)):
        - inside the window:  viewer.remove / viewer.insert(category, entry, index) / viewer.update(entry)
        - outside the window: only the category label (its count) and the title counts change
```

- **Persistent model.** `MarkerEntry` objects persist across changes. Only added or changed markers create or refresh
  an entry: O(changed) allocations, where stock has O(n).
- **One store.** The lanes carry what the view needs to locate a marker: `long` marker id to slot (open addressing),
  slot to category, and per-category sorted slot arrays. The `MarkerEntry` is the facade.
  - Next leaf: deferred `MarkerEntry` facades. Rows outside every category window then have no entry object at all.
    The comparator reads lanes (severity, path id from M3TreePathTrie, message id from M3LabelPool).
- **Fallback to the stock clean rebuild.** The stock rebuild still runs:
  - for the first build;
  - when filters, grouping, sorting, the scope or the limits change;
  - when the scope depends on the selection (`ON_SELECTED_*`);
  - when a delta's resource cannot be placed;
  - on any exception in the delta path (logged with JUL).

## 3. Equality with stock (BCP)

The view's answers must equal those of a stock clean rebuild after the same changes:
- the category list and labels;
- the shown children per category, in order, with their labels;
- the title counts;
- selection and expansion.

The differential (section 5) runs the same seeded script of marker changes on stock and on ours and compares those
answers after every step.

Tie handling: stock partially sorts with a heap (`MarkerSortUtil`). With ties under the comparator, the order of equal
entries can depend on the scan order. Ours fully sorts each category, stable in marker-id order. Any divergence the
differential finds is recorded in `UPSTREAM_DEFECTS.tsv` or fixed; it is never ignored.

## 4. Measurements (`MEASUREMENTS.tsv`)

At 10^4, 10^5 and 10^6 problem markers (one project, 1000 markers per file), on stock and on ours:

- the time from `IMarker.setAttribute` to the view showing the change (title counts changed, polled every 5 ms);
- the bytes allocated by all threads for that one change (`com.sun.management.ThreadMXBean`);
- retained heap after a full GC and `MarkerEntry` instance counts (`jcmd GC.class_histogram`);
- the CPU time of the update jobs.

Harness: `tools/markers_bench/` holds an OSGi bundle with an `org.eclipse.ui.startup` hook. It runs inside the
official Eclipse 2026-09 bundles on the hidden desktop, with its own configuration, workspace and user area on
`S:/m3-scratch/jface/eclipse`. The installed IDE is never written. "Ours" is the same bundle set with
`org.eclipse.ui.ide`'s markers classes rebuilt from source with the M3 hooks.

## 5. Differential

The bench bundle records, after every step of a seeded script, the view's answers listed in section 3
(add/remove/change severity/change message/delete file). It writes `<side>-<seed>.log`; `diff` decides.

## 6. Next

- Deferred `MarkerEntry` facades; a lane comparator; a virtual tree (`SWT.VIRTUAL`) so no limit is needed.
- Project Explorer (CNF) and Outline with the same delta pattern.
