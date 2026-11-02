<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Eclipse UI runtime forwarding receiver

This receiver installs fourteen outputs emitted by the canonical OpenRewrite
recipes: nine Commands files and five JFace acceptance files. The receiving base
is `hsoliwal/eclipse.platform.ui@16e944f83eec18d4cf609afd9ad9a937711211ce`.
Every existing preimage and every generated postimage is checked against the
recipe manifest before copying an actual SDK result into this worktree.

## Runtime and acceptance changes

`CommandManager.categoriesById` and `parameterTypesById` use the donated
`PackedHashMap` owner. Five existing Synexia collection classes are adapted into
the bundle's private `org.eclipse.core.internal.commands.m3` package, retaining
their original Apache-2.0 attribution. The receiver's EPL-2.0 facade, its public
types, validation, events, and listener behavior retain their established
contracts. The bundle's minimum Java version, JRE container, and compiler
source/compliance/target are aligned to Java 21 for the donated sequenced
collection contracts. The original compiler diagnostic policies are preserved.
The command-handle traversal registry, definition sets, and weak help-context
storage keep their existing implementations.

The JFace outputs register `M3TreeForwardingTest`, add the `m3-forwarding` Maven
profile, import the real Jupiter condition package, and correct the existing
lazy-tree fixture. That correction gives the selected child one real child before
`setInput`; all expansion assertions, SetData budgets, and both 200-sibling
domains remain intact. The model gains one node, from 40,201 to 40,202. The actual
native failure established that the selected original node was a leaf, and SWT's
existing contract correctly leaves a leaf collapsed.

The required property is `org.eclipse.jface.tests.m3Required=true`. The added gate
requires all eight SWT forwarding methods and then executes
`testExpandedStateDelegatesToSwtWithoutWalkingColdVirtualSiblings` using the
existing native test hierarchy. The passing run used the pinned SWT fork
`422fb369629ba47fc5c5c6ddf01794b6d7c79f42` and the actual generated Commands bundle.
No SWT or JFace production class changed between the preserved native failure and
success.

## Frozen canonical source mirror

The `canonical/` directory preserves the original
`synexia-openrewrite-recipes` layout, including both complete recipe crates and
the four selected SDK source owners:

| Component | Mirrored files |
|---|---:|
| Commands crate, including historical qualification and final compatibility evidence | 80 |
| JFace crate, including 99 core files and 46 native proof files | 145 |
| Shared Java/Text snapshot and scope owners | 4 |
| Total mirrored source and evidence files | 229 |

The complete receiver change set has 246 paths: the mirrored files,
fourteen generated outputs, and these three scoped metadata files. The original
Maven source layout is preserved, so each recipe POM resolves the four source
owners directly from this public mirror. Runtime binaries, compilation targets,
dependency caches, and private global policy/provenance files are excluded.

The complete frozen source mirror is bound to
[hsoliwal/com.synexia at 2b39b8232652ad86c16b239779a0c0cc205e5ecd](https://github.com/hsoliwal/com.synexia/tree/2b39b8232652ad86c16b239779a0c0cc205e5ecd/synexia-openrewrite-recipes),
canonical tree `95208a8a3069129f30a82fed48a00ef6ff07b383`. `SOURCE_RECEIPT.json` and
`FILE_PROVENANCE.tsv` bind all 229 mirrored files and the recipe sources for all
fourteen emitted outputs to this publication. Historical source and execution
pins inside the frozen crates remain unchanged.
The selected shared Text owner has
SHA256 `58c94f4b87a6e4a53268808bf0c635fe7bd56e2864cb8fe46483c85efba771d0`
and Git blob `cc5f86ab579d2f4b1b9bc4c26b4592d2d8142244`.

The Commands crate's original 77 files preserve their historical Text `35d09`
binding and original test receipts. Its separate compatibility artifacts record
the final shared owner; the publication inventory was refreshed to include those
three additional files. Historical source pins are not rewritten as if an earlier
run used the newer owner. The JFace crate likewise preserves its actual earlier
parser and path-alias refusals, original fixture failure, and later successful
SDK/native observations.

The Java owner admits exact source hashes and parses reviewed Java postimages
into OpenRewrite compilation units. The Text owner admits exact whole-file
postimages for the selected metadata. Its one Java 25 fixture bootstrap is sealed
to the exact crate, path, preimage hash, and postimage hash; the generated fixture
was compiled with Java 25 and executed in the native gate.

## Actual bounded qualification

| Gate | Recorded result and scope |
|---|---|
| Final JFace recipe/admission SDK | 15 passed, zero failures/errors/skips |
| Final Commands shared-owner compatibility | 6 passed, zero failures/errors/skips, final shared Text owner |
| Final Rules shared-owner compatibility | 5 passed, zero failures/errors/skips; retained in JFace compatibility evidence |
| Required native JFace method | 1 passed, zero failures/errors/skips, genuine GTK3 and Xvfb |
| Native report admission | Existing composition report verifier exited 0 for the original passing Surefire XML |
| Commands historical behavior | 12 baseline tests and 13 generated-bundle tests passed |
| Commands compiler and API | Entire 84-source generated bundle; genuine ECJ policies retained; all 83 pre-existing public/protected class APIs equal |
| Commands Equinox probe | Real bundle resolved and started; 8,211 assertion checks passed |

The native gate used 546 actual SWT and 423 actual JFace production sources,
genuine SWT GTK3 JNI libraries, and a fifteen-source Java test closure. The test
hierarchy requires Java 25; its production SWT and JFace classes were compiled
for Java 21. The passing Surefire XML SHA256 is
`bc984d3d195c38ef37619a0954b73ee35bea0b2585b587e25c734256a6a3e52a`.

The original failing native XML is retained alongside the passing XML. The
original Commands density discriminator and refused broader-map counterexample
also remain recorded as intentional failures. They are not counted as passing
tests. A retained `Map.Entry` count of zero in the generated identity registries
is a representation observation, not a throughput or total-memory benchmark.

These are exact carried execution records. Receiver preparation verifies source
identity, provenance, the closed path set, and readback; it runs no tests. Full
Tycho/PDE reactor testing, the complete Eclipse SDK/workbench, other native
platform configurations, and complete Eclipse-to-M3 migration remain separate
qualification boundaries.

## Reproduction from this public source

Start with separate clean checkouts at the pinned UI and SWT commits. Use genuine
dependencies with the hashes in each crate's `SOURCE_PINS.json` and Java 21 for
the recipes. Replay the recorded native toolchains: SWT used JDK 21 with
`--release 21`; JFace used JDK 25 with `--release 21`; the native test hierarchy
used JDK 25 with `--release 25` and executed on the Java 25 VM. Recipe generation
writes into an empty output directory; use actual `RecipeRun` results when
constructing a fresh receiving source image.

The two recipe roots are:

- [Commands recipe and reproduction](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-command-registries-20261018/README.md)
- [JFace recipe and reproduction](canonical/synexia-openrewrite-recipes/recipe-crates/m3-jface-forwarding-gate-20261018/README.md)

Their ordinary Maven proof POMs execute SDK tests in the `test` phase. The Commands
proof requires the real baseline Commands classes and Equinox common through
`canonical.classpath`; the recorded command receipts identify these inputs. The
JFace recipe POM uses its preserved relative default for the shared source owners.
The recipe proofs are distinct from the receiving Eclipse Tycho lifecycle, whose
test execution uses `verify`.

The [native reproduction notes](canonical/synexia-openrewrite-recipes/recipe-crates/m3-jface-forwarding-gate-20261018/proof/native/README.md)
describe the exact compiler inputs, native dependencies, display driver, Maven
adapter, and report verifier. Those preserved workspace adapters retain their
recorded `/tmp/eclipse-m3-20261018` paths. Reconstruct the documented layout or
explicitly rebind those inputs when reproducing elsewhere. Their raw receipts
retain the original commands and source identities.

The five-test Rules compatibility record concerns the separate Platform Rules
receiver. It does not install Rules into this UI repository or imply a complete
cross-repository build.

## Provenance, licenses, and review

[SOURCE_RECEIPT.json](SOURCE_RECEIPT.json) records every mirrored source and
generated output, the exact baseline/preimage/postimage bindings, and the closed
receiver path set. [FILE_PROVENANCE.tsv](FILE_PROVENANCE.tsv) preserves the
approved source packets' origin classification, source location, license,
notice, disposition, and evidence for every file. The three receiver-owned
metadata files are versioned by the receiver commit and do not claim circular
hashes of themselves.

The six Commands/JFace metadata admission fixtures retain their real upstream
EPL-2.0 provenance. Adapted Eclipse source and metadata remain EPL-2.0. Donated
collection owners, canonical recipes, and independently authored proof tooling
retain their stated Apache-2.0 grants. Full license texts and every crate/native
notice are copied unchanged; mirroring does not relicense upstream material.

Raw evidence, source snapshots, and inherited license whitespace are preserved
exactly. The complete `git diff --check` returns **exit 2**, with **194 inherited
findings across 21 frozen files**: two EPL license copies, two XML source
snapshots, and seventeen raw logs/reports. The separate check of the three
authored metadata files and fourteen generated outputs returns **exit 0**. All
remaining paths also pass. `SOURCE_RECEIPT.json` lists the exact preserved files
and hashes. No whitespace policy or real Git index is changed by receiver
preparation.
