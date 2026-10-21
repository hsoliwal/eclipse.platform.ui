# Problems view: closing the clean-rebuild fallbacks (Bug 296695 and the rest)

Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0

User, 2026-10-20: "next target is the Problems view and then all the platform UI using same viewport and DOOPS",
"all delta based view", and (pasting the Bug 296695 comment from MarkerContentGenerator) TV_5.0 and the x86core
JSON viewer "might provide useful tips on how to debounce and keep the UI interactive".

## Where the delta path stands

`M3MarkerDeltas` (commit d435632489) answers marker add/change/remove with lanes, a debounced job and a per-category
merge. It still falls back to the original clean rebuild (`gatherMarkers` -> `new MarkerEntry` per marker -> regroup ->
sort -> `setInput`) in four cases. Rule 24 (delta-only views) needs each one closed or justified with a measurement.

## What Bug 296695 says, and why deltas make it unnecessary

`MarkerContentGenerator.updateSelectedResource` calls `requestMarkerUpdate()` (a full rebuild) on every selection
change that a resource-scoped filter cares about. The commented-out `contentChanged()` and its note record that
Eclipse never found a safe way to tell whether the content actually changed. Its suggestion was to compare freshly
gathered markers with the cached ones, perhaps with a binary search after sorting. That still pays the full gather.

The delta answer needs no gather. A scope is a set of resource ids, and a scope change is a set difference:

1. Keep the current scope as a sorted `int[]` of resource ids (interned per resource path), built from
   `MarkerResourceUtil.computeResources(selected, enabledFilters, andFilters)` once per change.
2. Diff old and new with a two-pointer merge, O(n + m): `entered` ids and `left` ids. Catalogue: sorted-array set
   difference / merge-two-sorted-arrays.
3. Markers are already grouped by resource in lanes. For `left`, clear the scope bit of their markers. For `entered`,
   read markers of those resources only (`IResource.findMarkers` per entered resource), then evaluate the remaining
   filters for them only.
4. The visible result is `scopeMask & filterMask`. Report ENTERED/LEFT rows as `old ^ new` word by word, and turn
   them into one `M3Patch` per category. Rows inside the shown window are inserted or removed. Outside the window,
   only the category counts and the scroll extents change.

An empty diff, with the same resources selected, makes no update at all. That is the `contentChanged() == false`
answer Bug 296695 wanted, obtained in O(n + m) on ids.

## The four fallbacks, each as a delta

| Fallback today | Delta path |
|---|---|
| Filters with a resource scope (selection, working set, project) | Scope set-diff above. Working-set content and remove events (`CachedMarkerBuilder` property listener, `group.refresh()`) feed the same diff. |
| Active search text | One filter bit lane over the description/resource/path text, as in sort-filter's `M3ColumnFilter`: compile the pattern on the UI thread, superseding generation, evaluate on a worker, `old ^ new` gives ENTERED/LEFT. Text is interned label ids, so the match cost is per distinct string. |
| Flat (ungrouped) presentation | The same patch with one implicit category. The sort permutation lane takes binary inserts for entered rows, and left rows are removed by rank. |
| More than `MAX_PENDING` (50,000) deltas | Keep the merge path and measure it against the clean rebuild at 10^4, 10^5 and 10^6. Keep the fallback only if the ledger shows the rebuild is faster, and record that in DELTA_GATE.tsv with the numbers. |

## Debounce and keeping the UI interactive (TV_5.0, x86core jsonviewer)

The sort-filter track already extracted the pattern from both donors (C:/m3-superset/swt-sortfilter docs/m3/sort-filter
DESIGN.md, section "Asynchronous filtering"; FOSS rows `filter-debounce-supersede`, `filter-on-model`,
`model-work-cancellable`). Apply it to every change source of the view: marker deltas, scope changes, filter edits and
search keystrokes.

1. **One superseding generation**, an `int` per view. Every change records its input and increments it. A single
   preallocated runnable is re-armed, and when it fires it returns unless its generation is still current. Nothing
   is allocated per keystroke or per event.
2. **Model work off the UI thread.** One worker evaluates only what changed: the merged delta lanes, the scope diff,
   or one filter lane. It checks the generation every 4096 rows and drops the work when a newer one exists. The UI
   thread never walks the markers.
3. **One apply per result.** One `asyncExec` applies the `M3Patch` if its generation is still current. The tree is
   patched in the window only. Counts come from lane counters (O(1); ui#4404/#4301/#4304/#4469).
4. The 150 ms `SHORT_DELAY` stays as the debounce default for marker deltas. Search keystrokes use the sort-filter
   default, and the ledger records the input-to-pixel latency for both.

## DOOP: the entries themselves

`M3MarkerDeltas` still creates a `MarkerEntry` for each added or changed marker, and `Markers` keeps `markerToEntryMap`
(a HashMap, Markers.java:333). Target:
- marker id, resource id, severity, type, line and category as lanes;
- message and location as interned label ids;
- `MarkerEntry` created lazily only when an API asks for one (selection, Quick Fix, properties, copy), parent-owned
  and released under pressure;
- the id-to-slot map as the open-addressing `int[]` table `M3MarkerDeltas` already uses, instead of `HashMap`.

## Proof

- Stock-vs-ours differential for each change source: marker deltas, selection-scope changes, working-set changes,
  filter edits, search typing, grouping and presentation switches. Zero mismatches in visible rows, counts,
  selection and expansion.
- Full-rebuild counter = 0 for every case except a fallback that is measured and justified.
- Re-run `C:/m3-superset/tools/delta_census.sh` and record the drop for org.eclipse.ui.ide.
