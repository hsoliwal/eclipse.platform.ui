# Donor ideas: D:/TV_5.0 (structured viewers, 2009) and com.x86core.tools.jsonviewer

Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0

User, 2026-10-24: "d:\TV_5.0 has interesting ideas", "x86core has in it interesting ideas", "BUT the viewports UI is the most
important ui aspect". This note is the CPU sweep of both donors against the viewport programme: what each idea is, what
the programme already has, and what is still to take. Donor code is read only; nothing is copied from the IBM-copyright
files (EPL-1.0, pattern only); the user-authored files (2009, "hitesh soliwal") are adapted under the Synexia header.

## Inventory

| Donor | Where | Size | Authorship |
|---|---|---|---|
| TV_5.0 treeviewer | `D:/TV_5.0/treeviewer` (Eclipse plugin, JavaSE-1.6, `org.eclipse.jface.viewers.structured`, `src-filters`) | 154 Java files, 27,259 lines | 113 user-authored files, 41 IBM-copyright (EPL-1.0: `ViewPort`, `TreeElementMap` (CustomHashtable lineage), `TreeViewer` base, dialogs base) |
| x86core jsonviewer | hsoliwal/com.x86core.tools.jsonviewer (private, MIT, 2023, commit 56174949b0) | the user's JSON tree viewer | user-owned |

Already catalogued before this sweep (`FOSS_REUSE_DECISION.tsv`, `PROVENANCE.tsv`, sort-filter track): superseding debounce
(`ViewerFilterUI.DeferredRun`, jsonviewer `TreeFilterControl`), cancellable background updater (`TreeViewerUpdater`,
`IViewerUpdater.cancel`), lazy partial sort (`LazySortList`, deferred: it changes comparator call order), column filter text
modes and combination, the tree filter ancestor rule, `CachedLazyTreeContentProvider` (the model behind `M3TreeViewerModel`).

## The ideas of the 2009 structured-viewer contracts (IDL-LAW: the interfaces say it)

| TV_5.0 contract | The idea | In the programme today | Still to take |
|---|---|---|---|
| `IViewerEntry.setNibble/getNibble, setBits/getBits(mask, offset)` | per-entry state packed in int nibbles, no flag objects | lanes: `state[]`, `textHits` bit lane, `M3SlotOrder`; the DOOP law | nothing new; evidence that the bit-lane rule is the user's own 2009 rule |
| `IElementMap.DUMMY = "Deferred..."`, `getTestEntry(parent, element)`, `relocate(entry)` | placeholder rows for not-yet-materialised children; probe entries without allocation; relocation of an entry when its parent changes | `facade(slot)` created on first request and released when the slot leaves the window; the open-addressing id table | `relocate`: a marker whose category changes keeps its slot and only its category lane changes (check `M3MarkerLanes.apply` does this without a remove+add) |
| `IViewerUpdater` with `byte priority`, `waitForCommands/discardCommands(context, priority | entry)`, `hasPending(entry)`, `IViewerCommand[]` per priority | a prioritised, per-entry command queue: newer commands supersede older ones per entry, priorities order the work, the viewer can wait for or discard a class of work | `M3MarkerDeltas`: one superseding generation per view, one debounced job, window patches | **priority by visibility**: patches for rows inside shown windows first, counts and extents for the rest after; `discardCommands(entry)` = drop pending work for a marker that is removed before its patch runs |
| `IVisualIndexProvider.getIndex(entry) / getElement(index)` | the visual index is a provider, not a widget query | `M3SlotOrder.indexOf/get` (rank by slot), `M3SparseExtents` for pixel offsets | nothing new |
| `IExpandStateProvider`, `ISelectStateProvider`, `IFilterStateProvider`, `ICheckStateProvider` (all `(context, entry)`) | the model owns expand, selection, filter and check state; the widget asks | expand and selection are still widget state restored by `reexpandCategories` / `M3ViewerPaths.selectRows` | **selection and expansion as lanes** (bit lanes over slots, restored per window patch, never walked): the next lane after the search text |
| `IViewerLazyTool.adjustScrollBar(context, entry)` | scroll extents are adjusted lazily per entry, not recomputed | `M3SparseExtents` (variable row extents with a measurement cache) | nothing new |
| `IEntryFilter`: `andFilter/orFilter/addFilter(filter, andLogic)`, `getComponents`, `move`, enable, clone | a filter is a composable AND/OR tree with named, enable-able components | `MarkerFieldFilterGroup` (stock) evaluated per marker; the scope diff and the text lane are the only filter lanes | **filter algebra on lanes**: every filter group a bit lane over slots, the view's result the AND/OR of lanes (word-wise), a filter edit recomputes one lane; the `FiltersConfigurationDialog` edits the tree the way `ViewerFiltersDialog` did |
| `IViewerContext` (one object: input, column/element/label/image/color providers, state providers, lazy tool, updater, exception handler, visual index) | the viewer is assembled from providers by a context, not subclassed | `.m3view` template + `M3ViewSpec` (rule 27) | the context is the runtime form of the template: generate it from `.m3view` |
| `DeferrredTreeViewer` (2009, `ILazyTreeContentProvider` + deferred children) | deferred children with a placeholder and a background fetch | lazy tree content in JFace viewport mode (`M3TreeViewerModel`) | nothing new |
| `ViewerColumnsDialog`, `ViewerFiltersDialog`, `ColumnFieldModifier`, `ValidatingFieldModifier`, `ValidatingTextCellEditor` | column and filter configuration dialogs with validated in-place edits | `ViewerColumnsDialog` and `FiltersConfigurationDialog` of the markers views (stock) | behaviours catalogue (rule 29): validators and field modifiers as template behaviours |

## Leaves this sweep adds (viewport first)

1. Selection and expansion as lanes of the Problems view (state providers): bit lanes over slots, patched with the window,
   `reexpandCategories` and `selectRows` read lanes instead of walking items.
2. Filter algebra on lanes (`IEntryFilter`): one bit lane per filter group, AND/OR word-wise, one lane recomputed per edit.
3. Patch priority by visibility and per-marker discard (`IViewerUpdater`): shown-window patches first, pending work of a
   removed marker dropped.
4. `relocate`: a category change keeps the slot. **Verified 2026-10-24:** `M3MarkerLanes.apply` removes a changed
   marker from its old category order, releases its labels and writes the change into the same slot; only the category
   lane and the orders move (no free + allocate).

Each is a recipe-sized leaf with a headless test and a differential step; the order follows the Problems view proof.
