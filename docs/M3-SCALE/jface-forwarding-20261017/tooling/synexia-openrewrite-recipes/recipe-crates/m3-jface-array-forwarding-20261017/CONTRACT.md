# Eclipse M3 forwarding — 2026-10-17

User direction: implement the Eclipse framework through M3 in Java, keeping Eclipse APIs as forwarding boundaries and reusing existing implementations. Whole-platform completion is the goal, not a claim established by this pass.

## Pinned inventory before writes

- Synexia develop: 892e3f6c51d78bc0948e8a8cf736055b4c70eafa.
- Eclipse UI master: 8eb63ef686a720207dca77b8e0e2b8fd061bbfe5; bundles tree: 277df3ebda007d4eec2a374b60712147abbc5d12 (9,277 entries, not truncated).
- Eclipse Platform master: 69291c18babd233a94b742798b3d1078979bf78c.
- SWT master: 678da68f34007af0aab0689aa72bd85fb44b800e.
- Read root AGENTS in all three receivers and current Synexia canonical recipe policy. No nested AGENTS in the UI bundles tree.
- Existing Synexia owners: MIndexViewerSession and viewer-eclipse bindings; MIndex collections, algorithms, primitive structures, plugin image/runtime, mechanics adapters. Their presence is not evidence of drop-in compatibility with all Eclipse callbacks.
- UI master contains recipe bootstrap sidecars but no compiled M3-named class in its bundles tree. The actual binding gap must be closed per operation, not represented as already complete.

## First admitted transformation

Five Eclipse JFace framework files: ArrayContentProvider, ViewerFilter, ViewerComparator, TreePath, StructuredSelection. Full source read before authoring. Extract their array operations into an internal, non-exported M3ViewerArrays atom owner. Public names, descriptors, hierarchy, fields, callbacks, locks, comparer direction, cache behavior, identity and existing donor notices remain stable. Reuse Eclipse's current algorithms, JDK array intrinsics and Synexia's unchanged Maven/OpenRewrite hash-pinned Java recipe. Keep Eclipse-specific lifecycle/error logging/virtual dispatch at the facade where required. No separate state registry, per-element object, reflection dispatcher or JNI callback trampoline.

This pass establishes explicit replaceable behavior atoms with no algorithm substitution. It does not silently substitute a canonical-ID model for arbitrary client objects or claim all Eclipse code is migrated. Other bundles, Equinox, SWT platforms, native rendering, workbench and resource lifecycles require subsequent admitted mappings and actual integration gates.

## Locked observations

- Filtering: input-empty identity; non-empty output Object[] and distinct identity; stable order; exactly one select call per visited element; original callback receiver/viewer/parent; input mutations and thrown exceptions observed in the same order; TreePath overload still dispatches virtually.
- Sorting: Arrays.sort algorithm and comparator call order; stable ties; same receiver compare dispatch and exception instance; original diagnostic catch/logging remains in ViewerComparator.
- Tree paths: caller array remains aliased; constructor validates every segment; equality versus prefix comparer argument direction stays unchanged; sum hash and nonzero cache behavior preserved; parent/child make validated copies; EMPTY singleton behavior preserved.
- Selections: snapshot constructor, mutable fixed-size toList alias, clone toArray, null handling, raw legacy descriptors, comparer identity and hash rules; subclass isEmpty dispatch preserved.
- Array provider: array identity, Collection.toArray delegation, fresh empty arrays for unsupported input, existing singleton locking preserved.

## Qualification order

Documentation/recipe patch → lint → compile → recipe tests and SDK generation → generated-source lint → generated-source compile → independent differential behavior/API tests → real Equinox bundle runtime forwarding check. Each failed stage stops its lane. Preserve pre/post SHA-256, source Git blob pins, fixed point, drift/missing/duplicate/occupied refusals, recipe resources and receipts. Generated production files are written only from actual OpenRewrite results; fix templates and rerun on failure.

No measured speedup or full Tycho/UI/platform completion claim without executed evidence. Keep review PRs draft while broader platform gates remain pending. Preserve EPL-2.0 on donor-derived Java; Apache-2.0 on existing Synexia recipe infrastructure does not relicense its payloads.
