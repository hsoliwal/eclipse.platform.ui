# JFace viewers on the M3 viewport

Status: design, 2026-10-19. Branch `m3/jface-viewport-20261019` of hsoliwal/eclipse.platform.ui.
Agent: jface-viewport. Programme plan: `C:/m3-superset/tools/PROGRAMME_PLAN.md`, section 00c.

## 1. Goal

JFace viewers (`org.eclipse.jface.viewers`) wrap SWT `Table`, `Tree` and `List`. The SWT viewport (hsoliwal/eclipse.platform.swt
`m3/viewport-integration-20261016`) already makes native work O(visible window). This track makes the JFace layer
above it match:

- `refresh`, `update`, `setSelection`, `remove`, sort, filter and expand cost O(changed + visible window), not O(model)
  and never O(model x selection);
- the JFace public API and observable behaviour stay exactly the same (BCP). This is proven by a recorded
  differential against the official JFace jar (3.40.0) on the official SWT jar (3.135.0);
- large models filter and sort on a worker over model ids, debounced. The UI thread only applies the result, a
  visible-id lane, through the viewport.

## 2. Modes

One switch is read once at class initialisation: `-Djface.viewer.mode=original|viewport`, and per class
`-Djface.viewer.mode.<SimpleClassName>=...`. It lives in `org.eclipse.jface.internal.m3.M3ViewerMode`.

- `original` (the **default for now**) is fork-master behaviour. Every hook returns to the stock code path.
- `viewport` turns on the Layer A fast paths (section 4).

The default flips to `viewport` only once every differential is identical and the user agrees. The Layer B API
(section 5) is new, opt-in API. Its behaviour does not depend on the mode.

## 3. Observable contract (what the differential records)

| Recorded | How |
|---|---|
| public answers | every query in the script: `getSelection`, `getStructuredSelection`, `getElementAt`, `getExpandedElements`, `getItemCount`, `testFindItem` result index, `getChecked` |
| events | `ISelectionChangedListener`, `IPostSelectionChangedListener`, `IOpenListener`, `ITreeViewerListener`, SWT `Selection`/`SetData` on the control |
| provider calls | content provider (`getElements`, `getChildren`, `hasChildren`, `getParent`, `updateElement`, `updateChildCount`), label provider (`getText`, `getImage`, `update(ViewerCell)`), comparator (`compare`, `category`), filter (`select`), each in call order with arguments |
| widget state | item count, item texts and data order, selection indices, focus index, top index |
| pixels | CRC of the control's `print(GC)` per step (hidden desktop) |

Not part of the contract (as in stock, which already varies them with `setUseHashlookup`): how many times and in
which order `equals`/`hashCode` or `IElementComparer` are called. Their results must follow `Object`'s contract.
JFace already relies on that unconditionally in
`TableViewer.remove(Object[])`, `CheckboxTableViewer.preservingSelection`, `AbstractTreeViewer.updateChildren` and
`AbstractTableViewer.virtualSetSelectionToWidget`.

## 4. Layer A: stock API, internal fast paths (mode `viewport`)

Each fast path is a one-line hook statement inserted by the OpenRewrite recipe
`com.synexia.openrewrite.viewport.M3InsertStatements` (YAML in `recipe/META-INF/rewrite/m3-jface-viewport.yml`; the
second pass reports UNCHANGED). The hook calls a package-private `org.eclipse.jface.viewers.M3ViewerPaths` method. That
method returns "not handled" (the stock code then runs) unless the mode is `viewport` and its preconditions hold.
The client call sequence is unchanged by construction. Only JFace-internal search work changes.

| id | stock path (file:method) | stock cost | M3 path | cost |
|---|---|---|---|---|
| A1 | AbstractTableViewer.setSelectionToWidget(List, reveal=false), run by every `preservingSelection` (refresh, remove, update with refilter, add) | O(n x k) `equals` scan, first match per element | `M3FirstIndex`: open-addressing first-index table over the item data (comparer hash), built once | O(n + k) |
| A2 | AbstractTableViewer.internalRemove: `doIndexOf(item)` per removed element | O(n x k) (SWT `Table.indexOf` is a linear scan) | `M3IdentityIndex` item -> row lane, built once when k > 8 | O(n + k) |
| A3 | AbstractTreeViewer.updateChildren: stale item disposal compacts with `System.arraycopy` per disposed item | O(n x d) | two-pointer compaction (same dispose/disassociate order) | O(n) |
| A4 | AbstractListViewer.setSelectionToWidget: `getElementIndex` (listMap.indexOf) per element | O(n x k) | `M3FirstIndex` over the list map | O(n + k) |

Preconditions for falling back to stock: `reveal == true` (keeps the `doSetSelection(Item[])` focus semantics),
a virtual table, an item limit (`ExpandableNode`), a `null` element in the list, or fewer than 8 elements (the scan is
cheaper than the table).

Native cost (TableItem/TreeItem text, image, insert, remove, clearAll, setItemCount) is the SWT viewport's job: the same
JFace build runs on the stock SWT jar and on the viewport integration jar.

Inventory of every O(n) path with its classification: `INVENTORY.tsv`. Stock measurements: `MEASUREMENTS.tsv`.

## 5. Layer B: model-side filter and sort, on a worker, debounced (new API, `org.eclipse.jface.viewers.m3`)

User directive (2026-10-19): "the filter must run on the MODEL, be debounced, and keep the UI responsive".

Stock `ViewerFilter.select` and `ViewerComparator.compare` are client callbacks with a UI-thread contract. They often
call label providers, which may touch SWT resources. Running them on another thread would change observable
behaviour, so the stock API keeps running them synchronously on the UI thread, in stock order. The worker pipeline is
therefore new API whose callbacks are declared thread-safe:

- `M3RowFilter<E>`: `boolean test(E element)`. Pure, thread-safe, no SWT access. Factories `M3RowFilter.text(...)` for
  substring, wildcard and regex matching over a text extractor.
- `M3SortKey<E>`: comparator or `ToLongFunction` key plus direction. A chain gives a multi-column, stable sort; ties keep
  model order.
- `M3TableViewerModel<E>`: binds a `TableViewer` created with `SWT.VIRTUAL`. It owns the viewer's
  `ILazyContentProvider` and holds the DOOP lanes:
  - `Object[] elements`: the model snapshot; the id of an element is its index;
  - `int[] visible`: row -> id, the filtered and sorted projection;
  - `int[] rowOf`: id -> row, or -1.

  There is no per-row wrapper object.
- `M3TreeViewerModel<E>`: binds a `TreeViewer` with `SWT.VIRTUAL` and an `ILazyTreeContentProvider`. It flattens the
  tree once into pre-order lanes (`parent`, `subtreeEnd`). The filter keeps a node when it or a descendant matches:
  one reverse pre-order pass. Kept children per parent are stored in CSR form (`childStart`, `children`), sorted by
  the key chain.

### 5.1 Pipeline and generations

```
UI thread                          worker (one daemon thread, M3ViewerWorker)
setFilter / setSortKeys / setInput
  gen = ++generation                compute(gen): mask -> sort -> visible/rowOf (or CSR)
  debounce: timerExec(delay, r)       checks generation every 4096 ids; stale -> abandon
  r runs only if r is still latest    result -> display.asyncExec(apply)
apply(result): drop if result.gen != generation
  map selected rows -> ids -> new rows (O(selection))
  table.setItemCount(visible.length); table.clearAll(); table.setSelection(rows)
  -> SetData asks only for the visible window: O(window) label calls
```

- The **superseding debounce** follows the user's donors. The latest scheduled `Runnable` (or generation) wins; an
  older one finds itself superseded and returns. Donors:
  - `com.x86core.tools.jsonviewer` `TreeFilterControl.handleFilterChange`;
  - TV_5.0 `ViewerFilterUI.DeferredRun`.
- **Cancellation:** a newer generation makes the running computation abandon at its next check. This is the pattern
  of TV_5.0 `TreeViewerUpdater` / `IViewerUpdater.cancel()`.
- **Synchronous path.** `refresh()` on the model, and any call made outside a debounce context, computes on the
  calling (UI) thread and applies before it returns. That supersedes every pending generation. A caller that
  reads the viewer right after `refresh()` therefore sees the result, as in stock JFace. `refreshLater()`,
  `setFilterLater()` and the live filter text field use the debounce.
- **Timing difference (documented):** with the debounced calls, the viewer shows the previous projection until the
  worker finishes, and the latest request wins. The stock API has no equivalent call, so no stock behaviour changes.

### 5.2 Coalesced `refresh()` for plain viewers

`M3Viewers.refreshLater(StructuredViewer, delayMillis)` schedules a stock `refresh()` through the same superseding
debounce, with one pending refresh per viewer. In `viewport` mode a hook at `StructuredViewer.refresh()` entry
supersedes a pending one: the synchronous refresh already shows the newest state, so the pending refresh is dropped.

## 6. Layer C: riding the SWT viewport

- With the integration jar on the class path, `Table.setItemCount`/`clearAll`/`SetData` and item text writes are lane
  writes plus O(window) native work. Layer B uses exactly these calls.
- The enhanced SWT API (`M3ItemRange`, `M3ScrollInfo`) is reached later through an optional bridge, so the same JFace
  jar keeps running on stock SWT. This is a next leaf.

## 7. DOOP lanes and facades (rule 18)

| structure | lanes | facades |
|---|---|---|
| M3FirstIndex | `int[] slots` (open addressing, linear probe, power-of-two), `int[] hashes` | none |
| M3IdentityIndex | `Object[] keys`, `int[] values` (identity hash, linear probe) | none |
| M3TableViewerModel | `Object[] elements`, `int[] visible`, `int[] rowOf`, `long[]` filter mask | TableItem (SWT virtual, created by SWT on demand) |
| M3TreeViewerModel | `Object[] elements`, `int[] parent`, `int[] subtreeEnd`, `long[] keep`, CSR `int[] childStart`, `int[] children` | TreeItem (virtual) |
| sort | `int[]` ids, `int[]` merge buffer, `long[]` radix keys (adapted from sort-filter `M3SortLanes`) | none |

## 8. Proof

- **Differential:** `tests/.../viewers/m3/M3ViewerDifferential`. Seeded scripts drive TableViewer, TreeViewer and
  ListViewer with comparators, filters, refresh/update/add/remove/expand/selection. Each run records the section 3
  log. The stock side runs the official 3.40.0 JFace jar and writes a recording. The candidate (this build,
  `-Djface.viewer.mode=viewport`) compares against it step by step, and any difference prints the op log.
- **Benchmarks:** `M3ViewerBench` at 10^4, 10^5 and 10^6 elements, on stock and candidate (`MEASUREMENTS.tsv`).
- **Coverage:** JaCoCo agent; report under `coverage/`. Target 99% line and branch for every class added or changed.
- Everything runs on the hidden desktop: `ui_lock.sh` + `hidden_java.sh`.

## 9. Donors

See `FOSS_REUSE_DECISION.tsv` and `PROVENANCE.tsv`. In short:
- the user's TV_5.0 structured viewers: debounce, cancellable updater, lazy sort;
- the user's jsonviewer: superseding debounce, cached lazy tree provider;
- sort-filter `M3SortLanes`: stable merge and radix sort over int ids;
- NatTable and Glazed Lists: patterns only.

## 10. Next leaves

1. Bridge to the enhanced SWT API (`M3ItemRange`) for bulk range clears instead of `clearAll`.
2. Layer A for `AbstractTreeViewer.setSelectionToWidget` without hash lookup, and `internalFindItem` (recursive).
3. Make the Layer B worker incremental on `add`/`remove`/`update` (M3ListDiff/M3Patch from M3jdk21 #1403), not a full
   recompute.
4. FilteredTree (`org.eclipse.ui.dialogs` / `org.eclipse.e4.ui.dialogs.filteredtree`) on `M3TreeViewerModel` when the
   content provider is declared thread-safe.

## 11. Revision 2 (user, 2026-10-19): one model, deltas end to end

Governing rule: memory `feedback-one-model-delta-processing.md`. The user's words:
- "the JFace viewer should support delta updates";
- "if the update falls in the visible region the viewport should update, otherwise the scrollbar changes";
- "why have 10 models and 10 hash maps; against DOOPS".

What changes in this design:

- **One model.** A viewer in the M3 path keeps no element store of its own: no `CustomHashtable` element map and no
  cached sorted or filtered children. Elements map to int ids in one lane store: the M3 viewer model of Layer B,
  section 5. Row <-> id goes through the view lanes (`visible`, `rowOf`). The widget side is the SWT viewport, through
  the enhanced-API seam:
  - `M3BulkUpdates` (map row M3-BULK-UPDATES, leaf 8);
  - `M3DiffApply` (M3-DIFF-APPLY, leaf 10);
  - `M3RowExtents` (M3-ESTIMATED-HEIGHTS, leaf 16), in the integration jar.

  On stock SWT, the same model drives a `SWT.VIRTUAL` widget through `setItemCount`/`clear`/`SetData`. Items and
  element facades are deferred: they are created only when an API asks.
- **Deltas.** A viewer delta is runs of insert/remove/update/move over ids (`M3ViewerDelta`, int lanes). Applying it
  patches the view lanes incrementally:
  - removal is one compaction pass;
  - insertion is a binary search in the sorted order;
  - a changed sort key is a remove plus an insert.

  The widget patch then splits by the visible window:
  - rows inside it are cleared and refilled, so `SetData` asks only for those rows;
  - rows outside it change nothing but the item count, so only the scroll bar and the extents change;
  - the top row is kept by shifting `topIndex` by the net insertions above it.
- **refresh() without a delta** stays supported. In the M3 path it diffs the old and new id sequences (LIS, as in
  `M3DiffApply`) and applies the same patch, never a rebuild.
- **Coalescing.** Deltas queue under the superseding-generation debounce of section 5.1. Consecutive runs merge
  before the patch.
- **Layer A** (section 4) stays: it is the stock-API fast path for clients that keep their own model and call the
  stock viewer API.
- **First consumer:** the Markers view (`docs/m3/markers-view/DESIGN.md`). It turns `IMarkerDelta`s into these
  deltas. Project Explorer (CNF) and Outline follow.
