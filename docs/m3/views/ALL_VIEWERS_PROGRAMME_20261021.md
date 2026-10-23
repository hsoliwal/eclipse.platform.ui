# All Eclipse viewers on M3 — estate-wide convergence inventory

Status: **programme inventory, not completion certificate** (2026-10-21).

## PR provenance

- Already merged into `eclipse.platform.ui`: #21 (JFace viewport and Markers), #25 (sparse slot extents), #26 (slot rank/source range).
- Shared Tree/Table/List foundation: draft [#27](https://github.com/hsoliwal/eclipse.platform.ui/pull/27).
- Project Explorer resource source: draft [#29](https://github.com/hsoliwal/eclipse.platform.ui/pull/29), **independent master-based PR**, not stacked.
- Claude's `m3/marks-tags-20261020` source anchors and `docs/m3/marks-ast` are inventory candidates only; do not overwrite already merged viewer code from the stale branch.

## Receiver workstream checklist

| Leaf | Current source owner | Required M3 conversion | Status |
| --- | --- | --- | --- |
| Shared | `org.eclipse.jface` Tree/Table/List and SWT native Tree/Table/List | UI-dispatched M3 delta lanes, efficient lookup, viewport only native operations | #27 draft, partial |
| Markers | `org.eclipse.ui.ide` Problems, Tasks, Bookmarks, All Markers | M3 marker lanes, sparse ranks, window patches | #21/#25/#26 merged, remaining full native gates |
| Project Explorer | `org.eclipse.ui.navigator.resources`, `org.eclipse.ui.navigator` | `IResourceDelta` typed patches and CNF pipeline without per-call reindexing | #29 draft, partial |
| Outline | `org.eclipse.ui.views`; editor-owned JDT in `hsoliwal/eclipse.jdt.ui` | editor reconcile deltas, preserve page selection/lifecycle | pending |
| Properties | `org.eclipse.ui.views` PropertySheetViewer | descriptor/selection lanes; preserve editing and child merge | pending |
| Progress | `org.eclipse.ui.workbench` DetailedProgressViewer | job-event delta, deferred native controls and disposal | pending |
| Error Log | `org.eclipse.ui.views.log` LogView | log append/rotate/trim deltas, stable severity/grouping | pending |
| Search Results | `org.eclipse.search` AbstractTextSearchViewPage | MatchEvent lanes, duplicate/collision and sort order | pending |
| Filtered dialogs | `org.eclipse.ui.workbench` FilteredItemsSelectionDialog | debounced filter-mask and stable selection/keyboard events | pending |
| Quick Search | `org.eclipse.text.quicksearch` | retain SWT.VIRTUAL; stable indexed permutation instead of resort/rebuild | pending |
| Part stacks | `org.eclipse.e4.ui.workbench.renderers.swt` | bounded CTabItem/resident parts, e4 model state and DnD | pending |
| Console | `hsoliwal/eclipse.platform` UI console and document | append/truncate document lanes, high-water trimming | pending |
| Debug / Variables | `hsoliwal/eclipse.jdt.debug`, platform Debug | lazy trees, event-delta, selection and frame lifecycle | pending |
| JDT Packages | `hsoliwal/eclipse.jdt.ui`, `hsoliwal/eclipse.jdt.core` | Java element/reconcile deltas, AST-backed identity | pending |
| SWT-native | `hsoliwal/eclipse.platform.swt` | array/window and damage APIs used by every viewer | independent runtime gate |

## Hard invariants

1. No new Entry object per element. Parent-owned compact dynamic arrays, offsets, masks, low-GC indices and deferred facades; no duplicate per-view state.
2. Preserve public Eclipse/JFace/SWT contracts, Java callback/event order, selection, comparator/filter order, UI-thread ownership, resource move and editor lifecycle.
3. Treat regex/Claude annotations as bounded candidate signals. Verified compiler AST, type/flow and immutable source SHA must authorize each pass; no guessed equivalence.
4. Preserve `hsoliwal/com.synexia` as canonical tested Maven/OpenRewrite recipe owner. Every recurring transformation needs a fixed-point test, replay/refusal cases, and exact source/postimage custody receipts.
5. Work in file-local serial passes, fork-independent PRs, deterministic DAG fan-in; never rebase, squash or force-push existing `develop` archaeology.
6. Default paths stay stock where observable behavior is not proven identical. Opt-in `viewport` only for qualified paths. Performance must be demonstrated, not assumed.

## Required per-leaf exit gates

- Exact inventory of source events, currently duplicated caches and materialized SWT widgets, with file:line anchors.
- Pure atomization/delta recipe tests: trivial, ordinary, hostile, reentrant, duplicate-identity, cancellation, disposal and callback exception cases.
- Java 21 compile and the owning PDE/Tycho test suite (including Eclipse 25-specific bundle gates where required).
- A/B stock/fork/M3 differential of public answers, callback and SWT event order, selection, focus, pixels, resource or document deltas.
- 10^4/10^5/10^6 scale timings and memory measurements where feasible, including regressions and failure cases.
- GitHub source custody verified against current heads; explicit merge/review gate, then reinventory to catch uncovered viewers.

Do not mark this programme complete just because the shared JFace core is repaired or a PR is merged. Each source adapter and native runtime contract needs its own receiving proof.
