## Problems view on the viewport: search text on the lanes, JFace indexed insert under a filter, proof harness

Branch `m3/jface-viewport-20261019` forward-merged with master (the superset of PR #45, #48, #49, #44); this PR carries
the work of 2026-10-24 on top of it.

### What changes

- **Search text on the lanes** (`M3TextMatch`, `M3MarkerLanes.applyText`, `MarkerPatternFilter.visibleFields/matchesText/matchesAny`):
  the value a standard column shows is an interned label (message, resource, path, location) or a number (line,
  creation time, id), so the pattern's word match runs once per distinct label and is remembered per label id instead
  of once per row through a `MarkerEntry` facade (10^5 facades per keystroke at 10^5 markers before). Fields of other
  classes keep the facade route, evaluated only when no lane column matched. Design: `docs/m3/jface-viewport/PROBLEMS_VIEW.md` 3.1.
- **JFace viewport: an indexed delta insert keeps its position under a filter** (`AbstractTreeViewer.insertAt`,
  `StructuredViewerInternals.insertAt`, `M3Viewers`): the stock `insert` turns an indexed insert into `add()` whenever a
  viewer filter is installed, and `add()` appends without a comparator; a filtered tree always has its pattern filter
  installed, so every viewport delta row landed at the end of its parent (category order after builds and filter
  toggles, rows added under a search text). Found by the 10^4 differential of this PR.
- **Proof harness**: `MarkerPatternFilter` joins the overlay compile set (the overlay had carried the stock class);
  the bench reaches the e4 `FilteredTree` search box reflectively; the Windows Defender prompt is skipped on the hidden
  desktop; `settle()` logs its state every 10 s and gives up after 300 s; result rows are read with awk.
- **Docs**: DELTA_GATE partVisible row (hidden views accumulate, `shown` patches); `UPSTREAM_DEFECTS` MV-8;
  donor sweep of the user's TV_5.0 structured viewers and the x86core jsonviewer (`DONORS_TV5_X86CORE.md`) with four
  queued leaves; HANDOVER log.

### Proof

- Headless: `M3MarkerLanesAlgorithmsTest`, `M3SparseExtentsTest`, `M3TextMatchTest` (lane match against the stock
  derivation over 5,000 random rows and 8 tokens): 15/15.
- UI (hidden desktop, viewport mode): `M3ViewersInsertUnderFilterTest`: 2/2.
- JFace layer A (`run_differential.sh`, 3 seeds x 150 steps, stock vs original vs viewport over list, table, tree,
  hashed and sorted-filtered variants): 0 mismatches outside the pixel hash with the insert fix (18/18 logs, both
  modes); the pixel hashes of some `update` and `reveal` steps differed today under memory starvation, and differed
  the same way in a pre-merge build (bfe9344666) run alongside, so they are load noise (the 2026-10-20 run la4 was
  identical in both modes); to be rerun on a quiet machine before merging.
- Differential stock vs m3 (seed 7, 30 steps; `docs/m3/markers-view/tools/markers_round.sh 7 30 <n>`):
  - 10^4 before the insert fix: 52 of 69 steps differed (category order, one misplaced row, MV-8).
  - 10^4 after the insert fix: RESULT_10E4.
  - 10^5: RESULT_10E5.
  - Counters (m3, 10^4): 76 deltas applied, 0 fallbacks, 1 shown patch; stock: 30 full refreshes and 30 rebuilds
    for 30 edits, 6 for 9 search steps, 16 for 4 scope steps.

### Open

- MV-8: stock gains a native `Warnings` selection when the search text changes (no code selects; consistent with
  the win32 caret moving when a focused item is deleted during the refresh); M3 keeps the empty selection. Decision
  pending: match stock or count it as a stock defect.
- The >50k pending-deltas fallback stays FAIL in DELTA_GATE until measured against the merge path.
