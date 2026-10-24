# Problems / Markers view: handover (resume here)

Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
SPDX-License-Identifier: Apache-2.0 OR EPL-2.0

User, 2026-10-20: "for markers view, document what you did, so Codex can resume if you run out of tokens, or you can
remember after a token reset". Read this file first, then the files in "Read next", in order.

## Where

- Repo: hsoliwal/eclipse.platform.ui (fork of eclipse-platform/eclipse.platform.ui).
- Branch: `m3/jface-viewport-20261019`. Local worktree: `S:/m3-scratch/worktrees/platform-ui`. Commits are pushed after
  every leaf; a 20-minute sweep (K:/m3-work/logs/commit_sweep.sh) checkpoints everything.
- Package: `bundles/org.eclipse.ui.ide/src/org/eclipse/ui/internal/views/markers/`.
- Owner so far: the JFace agent of the lead's session (Claude). Codex or a later session continues from here.

## What exists (code)

| File | Role |
|---|---|
| `M3MarkerLanes.java` (new, ~800 lines) | the one lane model of the view: marker id, marker, severity, interned message/location/resource/path ids, category, counts per severity and category |
| `M3MarkerDeltas.java` | the delta path: IMarkerDelta/IResourceDelta merged per marker id into lanes, debounced job with a superseding generation, per-category merge, window patch; flat presentation and resource-scoped filters go through the lanes too |
| `M3MarkerKeys.java`, `M3MarkerCategory.java`, `M3SlotOrder.java` (new) | sort keys, categories and the slot permutation lane |
| `Markers.java`, `MarkerContentGenerator.java`, `MarkersChangeListener.java`, `MarkerViewerContentProvider.java`, `MarkersTreeViewer.java` | small hooks into stock classes (recipe-applied, see recipe/) |
| `ProblemsView.m3view` | the view template spec (rule 27) |
| `M3PartVisibility` (org.eclipse.ui.workbench) | IPartListener2 partHidden/partVisible, the stand-in until SWT.PartVisible lands (all-widgets track) |
| `M3ViewSpec` | the `.m3view` parser (view template) |

Switch: `-Dorg.eclipse.ui.ide.markers.mode=viewport` turns the delta path on; without it the stock path runs unchanged.

## What exists (docs, docs/m3/markers-view and docs/m3/jface-viewport)

- `DESIGN.md`: stock behaviour, target, equality rules, measurements plan, differential, next.
- `docs/m3/jface-viewport/PROBLEMS_VIEW.md`: the lane model, the delta path, what is still a clean rebuild, proof.
- `FALLBACKS_TO_DELTAS.md`: how each clean-rebuild fallback becomes a delta, including Bug 296695 (scope set-diff).
- `REFERENCE_TREE_VIEW.md` + `TREE_VIEW_API_MATRIX.tsv`: the Problems view as the reference tree view; 305 viewer API
  methods with lane/delta status (mechanical pass: 61 DELTA, 114 FACADE, ...); the adoption recipe for other views.
- `docs/m3/jface-viewport/DELTA_GATE.tsv`: every rebuild site with JUSTIFIED/FAIL.
- `docs/m3/jface-viewport/OPTIMIZATION_LEDGER.tsv`: before/after numbers.
- `INVENTORY.tsv`, `UPSTREAM_DEFECTS.tsv`, `recipe/` (OpenRewrite hooks), `bench-bundle/` (in-IDE bench).
- Integration branch (hsoliwal/eclipse.platform.swt, `m3/viewport-integration-20261016`): docs/m3/correction-sites
  (marked rebuild sites across 12 forks), docs/m3/ui-thread-sites, docs/m3/view-template/VIEW_TEMPLATE.md,
  docs/m3/behaviours/BEHAVIOURS.md, docs/m3/VIEWPORT_AGENT_BRIEF.md (rules 1-29), docs/m3/VIEWPORT_PROGRAMME_PLAN.md.

## Tools (docs/m3/markers-view/tools)

- `build_overlay.sh`: rebuilds the markers classes with the M3 hooks into an overlay of the official Eclipse 2026-09 bundles.
- `apply_recipe.sh`: applies the OpenRewrite hooks (the second pass must report UNCHANGED).
- `run_markers.sh <side> <size> <seed> ...`: one bench or differential run in an isolated Equinox configuration on
  S:/m3-scratch/jface/eclipse (never the installed IDE). UI runs go through C:/m3-superset/tools/ui_lock.sh.
- `markers_round.sh`: a full round (stock and ours, sizes, seeds).
- `run_headless_tests.sh`: the headless unit tests. `COVER=1` adds JaCoCo over the M3 classes.
- `ledger_rows.sh`: bench results to OPTIMIZATION_LEDGER rows. `matrix_status.awk`: fills TREE_VIEW_API_MATRIX statuses.

## State (2026-10-20 13:40) and what is open

Done: lane model, delta path for marker deltas, resource-scoped filters through lanes, flat presentation, the Bug 296695
scope diff against lanes (commit d13d2522a5), the view-template parser and spec, the PartVisible stand-in, bench and
differential harness, API matrix mechanical pass.

Open, in order:
1. **Re-measure, not a regression (corrected 2026-10-20 14:20):** the JV-R3 `table.filter50` 10^5 timeout and the
   `add1pct.sorted` 99 -> 130 ms rows were single runs with 3 desktop slots busy. The ledger's "JV-R3 note" row
   (commit f52d18dcc8) shows viewport mode changes only `M3ViewerPaths.removeRows/selectRows`
   (AbstractTableViewer.java:744, 895); setInput, refresh and filter run the same code in both modes. Re-run
   `M3ViewerBench` as a median of 5 on quiet slots before calling it either way.
2. DELTA_GATE FAIL row: `CachedMarkerBuilder.java:597,611` (working-set listener: `group.refresh()` then a full
   update). Make it the scope set-diff.
3. **Done (verified 2026-10-24):** deferred `MarkerEntry` facades (`M3MarkerLanes.facade/releaseFacade`, kept only
   while a slot is in a shown window), the open-addressing marker-id table (`find/put/unput`, `Markers.getMarkerItem`
   goes through `lanes.item`). The last per-row facade use, the search text, is now the lane-native match
   (`M3TextMatch`, PROBLEMS_VIEW.md 3.1); `children(Category)` still materialises transient entries for copy and
   select-all by design.
4. Differential at 10^4/10^5/10^6 with zero mismatches, and the full-refresh counter = 0. Record the run ids here.
5. TREE_VIEW_API_MATRIX: no TODO; every row has a differential status.
6. Switch the PartVisible stand-in to SWT.PartVisible/PartHidden once the all-widgets track lands it (rule 25).
7. Draft PR to hsoliwal/eclipse.platform.ui once the differential passes (the user merges).
8. Then the template: generate a PDE form section and the Java Outline from specs (rule 27), and the footer,
   header-2, view bar and behaviours from docs/m3/behaviours.

## Rules that apply (brief rules 1-29; the ones that bite here)

Delta-only views, resize/layout and hidden viewports (24); own the implementation, PartVisible and Viewport listeners
(25); public model API (26); template-first views (27); threads (28); behaviours (29). Also: Java only, JUL logging,
recipe hooks, Synexia header on new files, no Co-Authored-By trailers, never merge PRs or force-push, scratch only under
S:/m3-scratch, never write to G:, D: or E:.

## Resume checklist (Codex or a fresh session)

1. `git -C S:/m3-scratch/worktrees/platform-ui fetch origin && git -C S:/m3-scratch/worktrees/platform-ui status`
   (or clone hsoliwal/eclipse.platform.ui and check out `m3/jface-viewport-20261019`).
2. Read: this file, PROBLEMS_VIEW.md, DESIGN.md, FALLBACKS_TO_DELTAS.md, DELTA_GATE.tsv, OPTIMIZATION_LEDGER.tsv.
3. Run `tools/run_headless_tests.sh`; then one `run_markers.sh` round for stock and ours at 10^4 to confirm the setup.
4. Take "Open" item 1 and work down the list. Append each result to this file under "Log".

## Log

- 2026-10-20 13:40: handover written by the lead (Claude) from branch head af302a4438.
- 2026-10-20 14:05 (JFace agent): item 2 code done in d13d2522a5 (WorkingSetListener and requestMarkerUpdate offer scope changes to M3MarkerDeltas.scopeChanged: gather diff against the lanes, one delta); DELTA_GATE rows now PASS pending the differential scope sweep. Harness fixes since: m3Building readiness (fcdb7394b8: every earlier delta fell back), osgi.configuration.cascaded=false (2c12e1b555: every earlier m3 run had loaded the stock pool jars), in-process class histogram (3f4203ea90). Runs queued: round 1000 (stock+m3), stock 10^4 (done, 60-line log), stock 10^5. Item 1: same-build A/B with 5 interleaved repetitions under one slot (tools/run_ab.sh) queued; source check says viewport mode only changes remove/select paths (ledger note aab7823a2f).
- 2026-10-20 14:50 (lead, Fable): search text as a lane. M3MarkerLanes: textHits bit lane, applyText(filter), textHit/matching, windowSlots filters before the limit; Change.textHit/textGeneration set in write(); M3MarkerDeltas: ready() no longer treats a search text as a fallback, build() applies the text after finishBuild, prepareOne evaluates MarkerPatternFilter.matches per changed row, apply() re-evaluates rows prepared before a newer text, textChanged(view, filter) hook; MarkerPatternFilter.setPattern calls it; MarkerViewerContentProvider serves lane windows under a search; M3MarkerCategory.getShownChildrenCount = applyMarkerLimit(lanes.matching). Bench: filterText + search steps ('Added', 'problem 1', clear; 3 edits each) + diff-search-* rows. run_headless_tests.sh now uses JDK 25 (overlay ide classes are --release 25): 6/6 tests pass. Differential at n=1000 seed 7 (stock, then m3 with this build) queued on the UI lock; result to be logged here.
- 2026-10-24 04:05 UTC (lead, Fable): branch forward-merged with the fork master (135799b8bb: PR #45 JFace superset, #48 marker identity, #49 M3SparseExtents inline, #44 JDK21 compat); build_overlay.sh now compiles the ide-bundle com.m3.collections copy M3SlotOrder imports (97dc0f9dbe). Setup confirmed: build_jface 636 classes, overlay ok, 12 headless tests green, differential round 10^4 seed 7 started (see the next entry for its result). Leaf: the search text on the lanes (M3TextMatch, PROBLEMS_VIEW.md 3.1): one word match per distinct label instead of a MarkerEntry facade per row; facade route only for fields without a lane; M3TextMatchTest headless.
- 2026-10-24 05:00 UTC (lead): DELTA_GATE row UIUpdateJob:104 updated: hidden views accumulate and M3MarkerDeltas.shown patches (no refresh on partVisible), search text a lane, scope a set-diff, flat one category; only the >50k fallback stays FAIL until measured. Harness: the 10^4 round of 04:06 lost 30 min on a stale desktop lock (ui-test.lock.0 from the earlier timed-out run); its stock side started 09:37 local, logged nothing after the workbench theme line and hit the 1800 s timeout (no bench rows, cause open); the m3 side runs since 10:08 local. Next: rerun the stock side alone and read its log live, then 10^5.
- 2026-10-24 05:30 UTC (lead): user: "d:\TV_5.0 has interesting ideas", "x86core has in it interesting ideas", "the JFace view and all of Eclipse base projects", and the viewport priority six times. Donor sweep committed (docs/m3/jface-viewport/DONORS_TV5_X86CORE.md, 6e4b218ac5): the 2009 structured-viewer contracts map to the lanes; four leaves queued (selection/expansion lanes, filter algebra lanes, patch priority by visibility, relocate verified). Bench: settle() instrumented (a2b72aa420); the stock side of the 10^4 round reruns alone on slot 0 since 10:17 local with the instrumented bench; the m3 side of 04:08 completed (diff-m3-10000-7.log, 76 deltas applied, 0 fallbacks, 1 shown patch). JFace selection preservation in viewport mode still goes through getSelection()/setSelectionToWidget (element lists): the selection-lane leaf is real on the JFace side too.
- 2026-10-24 06:05 UTC (lead): first full 10^4 differential of the day (seed 7, 30 steps, stock 04:46-04:52 UTC with the instrumented bench, m3 04:08-04:11): 52 of 69 steps differ, three causes. (A) category order: m3 showed Warnings,Errors,Infos after the first build and after a filter toggle while stock shows Errors,Warnings,Infos; (B) a row added while a search text is active landed at the end of its category; both because AbstractTreeViewer.insert turns an indexed insert into add() whenever a filter is installed (the filtered tree always has its pattern filter) and add() appends without a comparator: fixed by AbstractTreeViewer.insertAt + StructuredViewerInternals.insertAt, used by M3Viewers for tree deltas in viewport mode. (C) stock gains a native Warnings selection at step 48 (text change) that nothing in the code sets (MV-8 in UPSTREAM_DEFECTS.tsv): m3 keeps the empty selection; decision pending. Rebuild + headless + 10^4 round rerunning with the fix.
- 2026-10-24 06:20 UTC (lead): insertAt fix committed (1ea008543c); M3ViewersInsertUnderFilterTest 2/2 green (viewport mode, hidden desktop). 10^4 round with the fix running.
- 2026-10-24 05:30 UTC (lead): the m3 side of the 10^4 round (started 10:44 local) hung: its JVM shows 1695 s of CPU then no progress with a 29 MB working set; at that time RAM was down to 310 MB of 32 GB with a harness git status on the E: clone holding 12 GB (killed, user-approved for git status storms); the earlier stock-side hang (09:39) had the same signature (no rows, log ending at the workbench theme line). Memory starvation of the hidden-desktop JVM is the leading cause; the bench now logs its phases so the next run shows where it stops. Rounds need a free-RAM check before they start (to add to run_markers.sh).
- 2026-10-24 05:40 UTC (lead): JFace layer A with insertAt: 0 non-pixel mismatches in both modes (18/18 logs, 3 seeds x 150); the px-only mismatches (14 original / 15 viewport) also appear in a pre-merge build (11 / 7) run alongside under memory starvation: paint-timing noise, not the change (la4 of 10-20 had 0). Rerun on a quiet machine before the PR claims it. Pre-merge tooling: jface_env.sh defines P (the platform Maven dir), so a chain must not use P for its own path; OUT=S:/m3-scratch/jface-premerge keeps the pre-merge build apart.
- 2026-10-24 06:10 UTC (lead): m3 rerun of the 10^4 round (phase-logging bench): category order now right; 23 steps still differ, two causes. (1) Every viewport delta remove went through AbstractTreeViewer.internalRemove's items-limit branch (the markers viewer carries the workbench large-view limit through WorkbenchViewerSetup.setupViewer): one parent refresh per removed element, which disposes the items of the other elements of the batch and then throws Widget is disposed on them; the delta path fell back ten times (fallbacks=10, builds=12) and the Warnings window kept 101-102 rows. Fix: in viewport mode the branch runs only when the parent shows an expandable node (a partial display), disposed items are skipped and unmapped; test M3ViewersRemoveUnderItemsLimitTest. (2) The Windows Defender prompt still opened: WorkbenchPlugin.PI_WORKBENCH is PlatformUI.PLUGIN_ID (org.eclipse.ui), so the preference qualifier was wrong; run_markers.sh writes org.eclipse.ui/windows.defender.startup.check.skip=true now. Rebuild + UI tests + 10^4 round rerunning.
