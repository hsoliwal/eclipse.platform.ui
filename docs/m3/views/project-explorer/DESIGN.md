# Project Explorer M3 delta adapter — source-specific leaf 2

Status: **draft / opt-in / not full viewer conversion**. Owned by `ResourceExtensionContentProvider` in `org.eclipse.ui.navigator.resources`, and consumes `IResourceDelta`.

## Stock contracts preserved

1. Recurse over changed children before add/remove handling.
2. Execute all additions before removals (historical selection/rename behavior).
3. Combine additions and removals per parent into existing JFace batch calls.
4. On `IResourceDelta.TYPE`, `OPEN`, `SYNC`, `DESCRIPTION`, or `REPLACED` retain exactly the existing subtree refresh and prefix-elision path.
5. Retain the old special rename redraw suppression and resource move order; do not claim equivalence for generic coalescing.
6. On UI disposal or a changed viewer/input binding, decline to apply an obsolete background result.

## M3 path and ownership

- Source callbacks can run on a workspace worker. A single immutable `Viewer/Display` binding is captured when `inputChanged` runs on the viewer's UI thread and published through a volatile reference. No worker invocation touches SWT `Control` or calls `getDisplay()` on a widget.
- Deltas are prepared as a list of existing operations, retaining source order. Only the UI-thread consumer reads the control and checks disposal.
- With `-Dorg.eclipse.ui.navigator.resources.mode=viewport`, **non-rename add/remove batches** use `M3ViewerDelta` parallel arrays and `M3Viewers.apply`. Consecutive changes for the same parent keep one viewer `add(parent,Object[])`, followed by one `remove(Object[])`.
- The default remains `original`; renames and ambiguous resource type changes use stock code.
- No new element store, per-resource cache, or entry object is introduced. A bound viewer/display reference is one per provider input.

## Regression evidence and remaining gates

| Gate | State |
| --- | --- |
| `ResourceDeltaThreadingTest` in NavigatorTestSuite | authored; not executed here |
| Explicit worker-with-empty-delta UI-thread invariant | covered by new test; execution pending |
| Add/remove equivalence, reordered moves and renames | needs stock-vs-M3 differential |
| Open/close, inaccessible folders, linked files, filters/selection and disposal | needs PDE integration suite |
| 10^4/10^5/10^6 workspace events and native paint CRC | not run |
| Java 21/25 Tycho reactor / M3JDK21 installed image | not run |

This is a receiving-source patch. A canonical self-improving Maven/OpenRewrite recipe, preimage gates and second-pass zero-change checks must be qualified in `hsoliwal/com.synexia` before promotion. Treat Claude's marks as inventory only. No default-mode promotion and no broad JFace API changes are authorized here.
