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
  - 10^4 after the insert and remove fixes (run of 05:54-06:03 UTC, c49919094b): IDENTICAL, 69 of 69 steps,
    m3 counters applied=76 fallbacks=0 builds=2 shown-patches=1.
  - 10^5: IDENTICAL, 69 of 69 steps (stock 06:04-06:10 UTC; m3 06:59-07:04 UTC after a first start that came up without its extension registry on a starved machine, see the 07:05 log entry). m3 counters applied=76 fallbacks=0 builds=2 shown-patches=1; 0 rebuilds for 30 edits, 9 search and 4 scope steps (stock 30, 9 and 4 rebuilds); create 1119 ms vs 856, initial build 1575 ms vs 1514.
  - Selection lane (3.2 step 2, -Dorg.eclipse.ui.ide.markers.selection=lane, c28418d387): 10^4 IDENTICAL, 69 of 69 steps, every one of
    the 76 patches restored its selection from the lane by position (lane-selections=76, selection-lane-mismatches=0); default
    unchanged until the 10^5 lane-mode run.
  - Counters (m3, 10^4): 76 deltas applied, 0 fallbacks, 1 shown patch; stock: 30 full refreshes and 30 rebuilds
    for 30 edits, 6 for 9 search steps, 16 for 4 scope steps.

### Open

- MV-8 withdrawn: the stock-side `Warnings` selection on a text change appeared only while the Windows Defender
  prompt was open on the hidden desktop (the bench then ran inside the dialog's nested event loop); with the prompt
  skipped under its real preference qualifier both sides agree on every selection segment.
- The >50k pending-deltas fallback stays FAIL in DELTA_GATE until measured against the merge path.
