# JFace marker selection helper collision — receiver receipt

Date: 22 October 2026

This receiver resolves one package-local Java method signature collision
without modifying viewer behavior. It is additive to the existing Eclipse UI
superset; no competing viewer implementation has been installed.

## Provenance

- Parent: `hsoliwal/eclipse.platform.ui` `master`
  `c5526df429f908a246cde6f71f8ce32db5f97fcc`.
- Canonical Java 21 OpenRewrite recipe: private `hsoliwal/com.synexia`,
  `synexia-openrewrite-recipes/recipe-crates/m3-jface-marker-selection-20261022`,
  commit `aad7ff4a744070f70882d531b859d40273487707`, PR #13124.
- Named recipe: `com.synexia.eclipse.JFaceMarkerSelection20261022`.
- Two SHA-256 preimage/postimage receipts are in
  `resources/com/synexia/rewrite/hash-pinned-java/m3-jface-marker-selection-20261022/manifest.tsv`.
  Both original Git blobs match the actual public fork `master`.

## Exact source changes

1. `MarkersTreeViewer` renames its package-local helper
   `m3PreservingSelection(Runnable)` to
   `m3PreservingMarkerSelection(Runnable)`.
2. `M3MarkerDeltas` renames the two corresponding caller expressions.

The implementation body remains `preservingSelection(patch)`; neither
`StructuredViewer` nor `StructuredViewerInternals` is changed. No public
Eclipse API, native/JNI contract, marker filtering/sorting logic, or selection
listener behavior changes.

## Reproducible qualification

| Gate | Status |
| --- | --- |
| Java AST OpenRewrite 8.17.1 on **JDK21** | 6 JUnit tests passed, 0 failed/errors/skipped |
| Baseline exact source blob identity | Two files match `master` |
| Fixed-point, missing/drift/duplicate, permutation and unrelated-source tests | Passed |
| Source diff and whitespace validation | Three identifier changes; `git diff --check` clean |
| Full Eclipse SDK Tycho packaging | Advanced past `org.eclipse.ui.ide`, next failing `org.eclipse.compare` |
| GUI, marker concurrency, selection cardinality, native OS matrix | Pending |

The next compiler stop is an independent compatibility mismatch in
`org.eclipse.compare` (inherited `expandSelection` visibility). Its repair
belongs in `hsoliwal/eclipse.platform` with its own qualified recipe.

Open viewer PRs #27, #35, #37, #42 remain separate convergence work and must
retain their intended superset semantics; this narrow fix neither merges them
nor replaces their unresolved reviews.

Keep this PR draft until downstream reactor, regression and native runtime
gates are qualified. Avoid force pushes or rewinding unrelated commits.
