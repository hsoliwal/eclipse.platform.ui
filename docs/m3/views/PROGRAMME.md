# Platform UI views on the event stream (programme)

User, 2026-10-19: "look at the UI views; you will receive a stream of events; all Eclipse, same problem."
Rule: memory `feedback-one-model-delta-processing.md`.

## The pattern (every view)

```
event stream (IResourceDelta, IMarkerDelta, MatchEvent, job events, UIEvents, log events, selection)
  -> view delta: insert / remove / update / move runs over int ids in ONE lane model (no per-view copies)
  -> M3Patch applied through the viewer:
       inside the visible window: rows (native updates, O(changed))
       outside it:                extents and the scroll bar only
  -> deltas coalesced by the superseding-generation debounce (jface-viewport DESIGN.md section 5.1)
```

A view keeps no element store of its own: no entry objects per element, no per-entry hash maps, no cloned arrays.
Facades (entries, items) are deferred: created when an API asks, parent-owned, released under pressure.

## Order and leaves

`VIEW_INVENTORY.tsv` lists every view with its event source, what it does today on an event (rebuild or delta),
the stores it keeps per element, the stock cost at 10^5 elements where measured, and the upstream issues it answers
(from the GitHub harvest, com.synexia `docs/M3-SCALE/runs/20261017-ui-donor-atomize/GITHUB_PR_HARVEST.tsv`,
theme one-model-deltas).

| leaf | view | branch / PR |
|---|---|---|
| 1 | Markers (Problems, Tasks, Bookmarks, All Markers) | `m3/jface-viewport-20261019` (this branch), `docs/m3/markers-view/` |
| 2 | Project Explorer (CNF) | own branch, one PR |
| 3 | Outline (host; JDT pages are external) | pattern only |
| 4 | Properties | own branch |
| 5 | Progress | own branch |
| 6 | Error Log | own branch |
| 7 | Search results | own branch |
| 8 | e4 part stacks (editor and view stacks) | own branch; 10^4 editors, LRU residency |
| 9 | FilteredItemsSelectionDialog, Quick Search | own branch |

One PR per view. Each leaf carries: inventory rows with file:line, a stock measurement at 10^4/10^5/10^6, the
differential of the view's answers and events against stock, JUL logging, docs and examples.

## Method per leaf

1. Stock evidence: the listener, the rebuild call chain and the stores, with file:line (grep).
2. Design doc under `docs/m3/<view>/DESIGN.md`: lanes, facades, the delta mapping, the fallback to the stock path.
3. Opt-in property (`-D<bundle>.<view>.mode=viewport`), hooks by recipe, new M3 classes.
4. Harness on the official 2026-09 bundles on the hidden desktop (`docs/m3/markers-view/tools/run_markers.sh` is the
   template): stock, fork-original, m3; differential logs; measurements TSV.
5. Flip the default only on an identical differential and the user's go.
