<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Eclipse UI: compiler and Commands version repair

This receiver installs the two actual outputs of `com.synexia.eclipse.UiBuildFixes20261019`:
ten NLS marker comments in `TreeViewer.java`, and the Commands bundle-version
increment required by the recorded API Tools failure. The string values,
executable tokens, forwarding code and existing compiler severity are preserved.

## Current receiver base and admitted outputs

The new receiver descends from UI master `3db425b5da9f5dafb9d66ae91ef0cae8b271d17a`, tree
`feffe081814943f1a8b3156fb3d32cd7674fe66d`. The canonical two-file recipe was qualified at
`52f64fe74beb3f8763cefd28651c5d5ab01d5d92`. Both admitted preimages and the JFace compiler preferences are
unchanged at the current base, so its exact outputs remain applicable.

| Product path | Actual emitted Git blob | Change |
|---|---|---|
| `bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/TreeViewer.java` | `8e9c66c447f4477845a3179eaa2d5414107b428e` | Exactly ten same-line `//$NON-NLS-1$` comments |
| `bundles/org.eclipse.core.commands/META-INF/MANIFEST.MF` | `cedff7292f838a1128774926f333d3ff5419f175` | `3.13.100.qualifier` to `3.14.0.qualifier`, retaining `JavaSE-21` |

The current base is twelve commits and 161 paths beyond the original qualified
UI revision. The base preserves all 246 earlier receiver entries. This repair
intentionally advances the Commands manifest among those entries and preserves
the other 245 exactly. All unrelated current-base changes remain present,
including the three newer JFace production changes to `M3ViewerArrays`,
`M3ViewerMap` and `CustomHashtable`.

## Original qualification and current-base integration

| Evidence scope | Recorded result |
|---|---|
| Frozen recipe at `52f64fe` | 6 SDK tests, zero failures/errors/skips; exactly two actual outputs |
| Original full JFace source gate | 423 sources; baseline exit 255 with 10 NLS errors; candidate exit 0 with no errors; 26 warnings in each |
| Original TreeViewer class comparison | Identical 32,414-byte class files from both javac runs |
| Separate current-base integration at `3db425b` | 424 sources; baseline exit 255 with the same 10 NLS errors; candidate exit 0 with no errors; the same 26 warning identities |

The [frozen 47-file crate](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ui-build-fixes-20261019/README.md) preserves the original 423-source
qualification and its raw reports unchanged. It uses actual source-built SWT
classes, genuine Eclipse/OSGi dependencies and ECJ 3.46.100 on Java 21. The
original macOS CI recorded 25 warnings; the local qualification recorded 26.
Neither result is presented as warning-free.

The separate current-base qualification covers all 424 actual JFace sources,
including the three newer production changes, under the unchanged strict NLS
compiler preference. [SOURCE_RECEIPT.json](SOURCE_RECEIPT.json), in
`current_receiver_qualification`, embeds all **16 original evidence files** as
exact UTF-8 content with their paths, byte counts, Git blobs and SHA-256 values.
Those files include source inventories, compiler commands, full text diagnostics,
the result receipt, original licenses and NOTICE. They can be recovered by
writing each `embedded_files[].content` value as UTF-8 at its recorded relative
path. This later integration evidence is receiver metadata; it is not relabeled
as part of the canonical 47-file proof.

The current gate verifies Commands manifest preimage/postimage bytes. ECJ does
not validate OSGi metadata or perform API Tools. The minor-version correction
matches the recorded API Tools demand and is not an API Tools pass claim.
The SDK tests, native gate and full Tycho/PDE reactor were not rerun for the
current-base compatibility check. Hosted receiving CI retains product acceptance
authority.

The canonical [reproduction guide](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ui-build-fixes-20261019/README.md#reproduce) gives the
original pinned 423-source qualification procedure; current-base commands and
source identities are preserved in the separately embedded integration evidence.
Receiver preparation itself runs no tests or compilers. A separate caller recipe
handles the shared workflow pin after its actual commit exists.

## Published source and reproduction layout

The complete source mirror and the two output recipe bindings use
[hsoliwal/com.synexia at `6a7bad0fe6322228fb59bcc563163b4e35fb80ce`](https://github.com/hsoliwal/com.synexia/tree/6a7bad0fe6322228fb59bcc563163b4e35fb80ce/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ui-build-fixes-20261019),
canonical tree `0e34f1874a84c51ed1b64d93dc211fb224202044`. The mirror retains all **47 frozen
crate files** and exactly **four unchanged SDK owners** in their original
`synexia-openrewrite-recipes` directory layout. The POM's
`${project.basedir}/../../src/main/java` default still resolves; this crate
compiles its 4 required shared owners from that location.

[SOURCE_RECEIPT.json](SOURCE_RECEIPT.json) binds each public source and output to
its exact canonical path, commit, tree, Git blob, SHA-256 and license.
[FILE_PROVENANCE.tsv](FILE_PROVENANCE.tsv) preserves every original nine-column
lineage field and records public license/notice mappings separately. The three
receiver metadata files are versioned by the receiver Git tree; their records
make no circular self-hash or future-commit claim.

The closed receiver set is **56 blobs**: 47 crate files,
four SDK owners, two actual SDK product outputs and three scoped metadata files.
The copied canonical evidence retains its original execution revisions and
timestamps. Runtime source bindings remain fixed at the canonical publication
above when a later, separately qualified CI caller packet is added.

The original and derived Eclipse sources retain EPL-2.0. First-party recipe,
proof and receiver metadata use Apache-2.0. The complete
[Apache license](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ui-build-fixes-20261019/LICENSE-APACHE-2.0),
[EPL license](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ui-build-fixes-20261019/LICENSE-EPL-2.0), and
[crate NOTICE](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ui-build-fixes-20261019/NOTICE) are unchanged copies. Existing source headers
remain intact. The shared owner lineage's historical notice reference resolves
to the [immutable public UI notice](https://github.com/hsoliwal/eclipse.platform.ui/blob/52f64fe74beb3f8763cefd28651c5d5ab01d5d92/docs/M3-SCALE/runtime-forwarding-20261018/canonical/synexia-openrewrite-recipes/recipe-crates/m3-jface-forwarding-gate-20261018/NOTICE). Recorded tool output
retains its original attribution and does not acquire a new software license
through a metadata label.
