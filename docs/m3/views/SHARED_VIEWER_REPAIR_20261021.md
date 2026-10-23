# Shared JFace viewer repair — 2026-10-21

Status: **partial, draft, opt-in for the tree search path**. This is not a claim that all view-specific adapters have been completed.

## Source ownership and blast radius

- `org.eclipse.jface.viewers.m3.M3Viewers` is the single delta dispatch engine already shared by JFace Tree, Table and List viewer owners.
- `org.eclipse.jface.viewers.m3.M3ViewerDelta` stores operations in parallel primitive/reference arrays. No per-delta entry object is introduced.
- `AbstractTreeViewer.internalFindItem` has a new **opt-in** non-recursive pre-order traversal (A5), via `M3ViewerPaths`; default `original` mode is unchanged.
- The existing M3 Problems/Tasks/Bookmarks/All Markers implementation and its PR #21/#25/#26 are reused, not replaced.

## Verified defects and corrections

| Defect | Source-correcting change | Guard |
| --- | --- | --- |
| Worker `post()` reads SWT viewer/control/getDisplay off the UI thread | select the current/default Display without reading the control; resolve viewer/control only in the queued UI runnable | `M3ViewersTest` explicit-Display worker dispatch + existing worker/default tests |
| `flush()` reads the control before proving UI ownership | `Display.getCurrent()` short-circuit before any viewer/control access | existing `flushOffTheUiThreadDoesNothing` |
| `append(this)` observes its growing source size forever | snapshot the original source length before copying lanes | `M3ViewerDeltaTest.selfAppendCopiesOnlyTheOriginalOperations` |
| Deep non-hashed tree search uses recursive Java call stack | A5: pre-order iterative traversal, primitive offset stack and array frontier | `M3ViewersTest.nonHashedTreeSearchKeepsFirstPreorderMatch` + isolated differential |

## Verification, clearly separated

- **Executed in an isolated Java 21 harness:** 20,000 randomly generated trees/query pairs matched the recursive oracle *and every recorded data/equals/children callback in order*; a 100,000-level tree completed with `-Xss256k`.
- **Authored, not executed here:** JFace JUnit/PDE tests in this branch, including non-hashed pre-order and off-thread posting.
- **Not executed here:** full Eclipse Tycho reactor, M3JDK21 image/SWT integration, GUI pixel differential, and multi-platform native tests. Full release/gate green must not be inferred.

## Next source-specific leaves (not fixed by this PR)

| Viewer | Event-source adapter | Required separate contract proof |
| --- | --- | --- |
| Project Explorer/CommonNavigator | `IResourceDelta` to child add/remove/update lanes | selection/expansion, provider order, resource moves/links |
| Outline / JDT pages | JDT element deltas and editor reconciliation | page-owned provider callbacks, editor lifecycle |
| Properties | workbench selection and property descriptor changes | editing, descriptor merge, disposed widgets |
| Progress | job events | finish/cancel visibility, widget disposal |
| Error Log | log events, file reread | grouping, filtering, severity and history |
| Search Results | `MatchEvent` | duplicate matches, order and `AbstractTextSearchResult` |
| Quick Search / Filtered Items | generation-keyed result batches | user typing, keyboard selection, virtual item count |
| Console | document append/trim | line offsets, high-water trimming |
| e4 editor/view stacks | part model events | selection, tab lifecycle, DnD, native residency |

The existing `VIEW_INVENTORY.tsv` and Claude's `m3/marks-tags-20261020` branch are inventory inputs, **not automatic equivalence evidence**. Do not merge stale annotation branch over current viewer sources.

## Recipe custody

- Existing `docs/m3/jface-viewport/recipe/META-INF/rewrite/m3-jface-viewport.yml` has the added A5 hook for the reusable `M3InsertStatements` recipe.
- Every applied file must be replayed at the same SHA, with second-pass zero-change, Java 21/JUnit/Tycho gates and stock observable differential before opt-in promotion. Canonical reusable ownership belongs in `hsoliwal/com.synexia`.
- Preserve append-only `develop` history and existing merged PR source; no force push, rebase, or unproven whole-view refresh suppression.
