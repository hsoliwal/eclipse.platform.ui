# Problems view on one lane model and deltas

Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0

User, 2026-10-20: "next target is the Problems view and then all the platform UI using same viewport and DOOPS",
"all delta based view", and "problems view is the example of how a tree view should [work] with all its api".
Branch `m3/jface-viewport-20261019` (hsoliwal/eclipse.platform.ui). The mode is on with
`-Dorg.eclipse.ui.ide.markers.mode=viewport`. Without it, every hook is inert and the view is stock.

## 1. What stock does on one marker change

`MarkersChangeListener` looks at the marker deltas only to decide "relevant or not", then drops them. A clean
rebuild follows: `gatherMarkers` creates a `new MarkerEntry` for every marker in scope and evaluates every filter for
each one. The view then regroups (a `TreeMap` per update), partially sorts every category, clones the arrays, and
runs `viewer.refresh(true)`. The scheduler can delay this by up to 10 s (`MarkerUpdateScheduler.LONG_DELAY`). Each
`MarkerEntry` carries a `ConcurrentHashMap` attribute cache, and the collation keys live in two static maps.
docs/m3/markers-view/INVENTORY.tsv and UPSTREAM_DEFECTS.tsv give the file:line evidence (ui#4404, #4301, #4304,
#4469).

## 2. The one model: `M3MarkerLanes`

| lane | type | content |
|---|---|---|
| `ids` | `long[]` | marker id. The open-addressing `int[]` table maps id to slot, with no `HashMap` (replaces `Markers.markerToEntryMap`) |
| `markers` | `IMarker[]` | the resource layer's handle. It is needed for the facades and the APIs |
| `severity`, `line` | `int[]` | attribute values, read once by `M3MarkerKeys.read`: one `getAttributes(String[])`, so one element-tree lookup per marker |
| `message`, `location`, `resource`, `path`, `type` | `int[]` | label ids in `M3LabelTable`: interned and reference counted. A collation key is computed once per distinct message |
| `created` | `long[]` | creation time |
| `category` | `int[]` | stable category id |
| `facades` | `MarkerEntry[]` | set only for slots in a shown window. Everything else is null |

- **Categories.** Each category keeps its slots in sort order in an `M3SlotOrder`. This is an order-statistic list
  of sorted `int` blocks of at most 1024 slots. Insert and remove cost O(log n) compares plus one `arraycopy` of at
  most one block. `get(i)` and `head(k)` walk the block lengths. Categories are ordered by the group's entries
  comparator, with `TreeMap` equality, as in stock.
- **Sort.** The lane comparator reproduces `MarkerComparator.compareFields` for the known field classes:
  severity-and-description, description, resource, path, location, id, creation time and type. It honours the
  descending fields, and ends ties on the marker id, which gives a total order. A view that uses any other field
  class does not get lanes and stays stock.
- **Counts.** Per severity bucket, per view and per category, as `int` counters. The title, the status line and the
  category labels are O(1) (ui#4404).
- **Facades** are `MarkerEntry` objects, created only for an API that asks: the rows the viewer shows (the
  content provider's window), a selection, Quick Fix, properties, copy, and the search box. `Markers` answers
  `getCategories`, `getElements`, `getMarkerCounts`, `getMarkerItem`, `getClone` and `sortMarkerEntries` from the
  lanes. The content provider asks the category for its window, not for all of its children.

## 3. Deltas end to end: `M3MarkerDeltas`

1. `offer` (resource change thread) merges the `IMarkerDelta`s by marker id into lanes and schedules one job
   (150 ms debounce). A clean rebuild supersedes it (generation counter).
2. `prepare` (worker) reads the keys of the changed markers only, evaluates the filters on one transient facade per
   changed marker (the filter API asks for a `MarkerItem`), and finds the category.
3. `apply` (one `asyncExec`) runs `M3MarkerLanes.apply`: each changed marker leaves and re-enters its category's
   order, and the counters follow. The tree patch (`patchTree`) then builds one `M3ViewerDelta`: categories removed
   and added, a label update per touched category, and for each category whose rows exist, the rows that left the
   window or changed (removed) and the rows that entered it (inserted at their window index). It is applied through
   `M3Viewers.apply` inside `preservingSelection`, the same selection contract as `refresh`. Rows outside every
   window cause no tree work at all.
4. **Hidden view** (brief rule 24): while the part is hidden, `apply` updates the lanes only and accumulates the
   touched categories and changed ids. On `partVisible` it applies one window patch (`M3PartVisibility`, see
   REFERENCE_TREE_VIEW.md; SWT.PartVisible is the coming source of truth).


### 3.1 The search text on the lanes (2026-10-24)

Stock evaluates the search box per child: `PatternFilter.isLeafMatch` calls `MarkerField.getValue(item)` for every
visible field of every marker (attribute reads through `MarkerEntry`), once per keystroke that survives the
`FilteredTree` debounce. The first lane version (`M3MarkerLanes.applyText`, 2026-10-20) kept the stock test but ran
it on a transient `MarkerEntry` per slot: 10^5 facades per keystroke at 10^5 markers.

`M3TextMatch` evaluates the standard columns on the lanes. The value a standard column shows is an interned label
(message, resource, path, location) or a number (line, creation time, marker id), so the pattern's word match
(`MarkerPatternFilter.matchesText`, the stock `wordMatches`) runs once per distinct label and is remembered per
label id for the pass. The visible fields are mapped to lanes by exact class (`M3MarkerLanes.plan`:
`MarkerDescriptionField` and its two stock subclasses, `MarkerResourceField`, `MarkerPathField`,
`MarkerLocationField`, `MarkerTypeField` when the model keeps the type lane, `MarkerCreationTimeField`,
`MarkerIDField`); any other field (a contributed column, a subclass with its own `getValue`) keeps the facade route,
evaluated only for rows no lane column matched (the stock OR over the visible fields, same order within each
route). The derivations are the stock ones: `MarkerEntry.computeLocation` (location attribute, else "line N", else
Unknown), `MarkerPathField` (bidi processor over the path), `MarkerCreationTimeField` (empty below zero),
`MarkerIDField`.

Known difference: stock reads the attributes at match time and treats a `RuntimeException` of a field as no match;
the lanes match the labels captured by the last delta. A marker whose attributes changed is a delta (the lanes are
current); a marker that vanished without a delta differs until its removal delta lands.

Proof: `M3TextMatchTest` (headless: lane match against the stock derivation over 5,000 random rows and 8 tokens,
facade counting, memo growth); the differential's `search '<text>'` steps (stock and m3 logs identical).

## 4. Still a clean rebuild (DELTA_GATE.tsv)

These still rebuild: the first input; filter, grouping, sort, scope and limit changes (user actions); and the
fallbacks of docs/m3/markers-view/FALLBACKS_TO_DELTAS.md. Search text, a resource scope (Bug 296695 set-diff), more
than 50,000 pending deltas, and errors are open. The flat presentation is being closed with one implicit category.

## 5. Proof

- Differential: the bench bundle (docs/m3/markers-view/bench-bundle) runs a seeded script of marker edits. The
  edits are severity, message, line, add and delete. The script continues with a resize sweep, a column sweep and a
  hidden sweep (minimize, 10^4 changes, restore), and logs the view's answers after every step: title, categories,
  rows, expansion and selection. Stock and M3 logs must be identical (`markers_round.sh`).
- Counters per measured delta and per phase: UI update jobs (full refreshes), clean rebuilds, `M3MarkerDeltas`
  counters (applied, fallbacks, builds, shown-patches), and paints and item changes while hidden.
- Measurements: deltas of 1, 100 and 10^4 markers at 10^4, 10^5 and 10^6 markers. Time to title, bytes allocated by
  all threads, process CPU, retained heap after GC, and class histogram. These are recorded in
  OPTIMIZATION_LEDGER.tsv.
- Headless tests: `M3MarkerLanesAlgorithmsTest` (M3SlotOrder and M3LabelTable against reference structures). JaCoCo
  on the bench runs (`COVER=1`) covers the lane model, the delta path and the viewer delta API.

## 6. Files

- `bundles/org.eclipse.ui.ide/src/org/eclipse/ui/internal/views/markers/`: `M3MarkerLanes`, `M3MarkerCategory`,
  `M3MarkerKeys`, `M3SlotOrder`, `M3LabelTable`, `M3MarkerDeltas`, `M3LongSet`. The hooks in `Markers`,
  `CachedMarkerBuilder`, `MarkersChangeListener`, `MarkerViewerContentProvider`, `MarkersTreeViewer` and
  `ExtendedMarkersView` come from the recipe docs/m3/markers-view/recipe (apply_recipe.sh, idempotent).
- `bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/m3/`: `M3ViewerDelta`, `M3Viewers`.
- `bundles/org.eclipse.ui.workbench/eclipseui/org/eclipse/ui/internal/m3/M3PartVisibility.java`.
- Harness: docs/m3/markers-view/tools (`build_overlay.sh`, `run_markers.sh`, `markers_round.sh`,
  `run_headless_tests.sh`).
