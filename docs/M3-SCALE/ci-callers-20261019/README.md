<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# UI CI caller: published shared-workflow pin

This receiver applies the actual SDK output of
`com.synexia.eclipse.CiCallers20261019` to the existing
`.github/workflows/ci.yml`. The sole change replaces the existing shared-workflow
`uses` reference with the same workflow path in the authorized aggregator fork
at the already-published commit `c723152cc91c409d17e0c8702ed5bb2ad398b337`.

```yaml
    uses: hsoliwal/eclipse.platform.releng.aggregator/.github/workflows/mavenBuild.yml@c723152cc91c409d17e0c8702ed5bb2ad398b337
```

The [published shared workflow](https://github.com/hsoliwal/eclipse.platform.releng.aggregator/blob/c723152cc91c409d17e0c8702ed5bb2ad398b337/.github/workflows/mavenBuild.yml)
contains the separately qualified Windows long-path setup. Its original
source and proof remain in that commit's
[Windows correction packet](https://github.com/hsoliwal/eclipse.platform.releng.aggregator/tree/c723152cc91c409d17e0c8702ed5bb2ad398b337/docs/M3-SCALE/ci-longpaths-20261019).
That workflow commit was published and read back before the caller postimage
and SDK proof were sealed. This packet changes the existing caller reference;
the shared workflow source stays at its independently published revision.

## Exact output and receiver base

| Property | Recorded identity |
|---|---|
| Receiver base | `3db425b5da9f5dafb9d66ae91ef0cae8b271d17a` |
| Complete base tree | `feffe081814943f1a8b3156fb3d32cd7674fe66d` |
| Existing caller | 423 bytes; Git blob `fde222c38eb768bb09e2373d965627fa117a2ab6` |
| Actual emitted caller | 449 bytes; Git blob `41ac6662f18298e13a9ac7860141066c6cf350e2` |
| Actual emitted SHA-256 | `b4d17a179805598dea0a66e969819766b4d35f00cbf43778756095715bb07939` |

Replacing the new uses line with the old line restores every original byte.
Caller triggers, branch and path filters, concurrency, inputs, permissions and
all other content therefore retain their exact preimage bytes. The original
trailing whitespace and final blank line are preserved. There is one existing
workflow modification, with no newly registered workflow or gitlink change.

The receiver publication combines this **38-file caller packet** with the
separately sealed **56-file runtime packet**, for a closed
union of **94 changed blobs**. Their paths do not overlap. All four shared
owner contents are equal across the two separate documentation mirrors.
The runtime packet's sources, outputs, metadata and qualification remain
unchanged, bound to canonical `6a7bad0fe6322228fb59bcc563163b4e35fb80ce` / tree `0e34f1874a84c51ed1b64d93dc211fb224202044`.
This caller packet uses its own actual canonical publication below.

## Recorded SDK qualification and reproduction

The complete [30-file canonical crate](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ci-callers-20261019/README.md) contains the actual
six-test GREEN result, zero failures/errors/skips, and its raw Maven log, XML and
receipt. It also preserves the legitimate no-op RED result: one test and one
failure because the expected existing-caller output was absent. The tests
exercise exact line replacement, reviewed digest, fixed point, missing and
drifted inputs, duplicate and alias refusal, unrelated sources, permutations,
and direct/named serial and two-worker replay.

The same one-target recipe was invoked independently for Platform and UI.
Each invocation admitted its existing `.github/workflows/ci.yml` and produced
one actual result. The two 449-byte results are equal because their original
423-byte callers were equal. This UI receiver installs only its own result;
the complete crate retains both source lineages and both qualification results.

Use Java 21 and the dependencies pinned in the mirrored POM. From
`canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ci-callers-20261019`:

```sh
mvn -B -ntp -Dmaterialize.dir=/absolute/path/to/caller-output test
```

The four unchanged SDK owners are mirrored in the original directory layout;
the POM's `${project.basedir}/../../src/main/java` default resolves and all four
required includes are present. No source-root override is required. The
[canonical reproduction guide](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ci-callers-20261019/README.md#reproduce-the-sdk-qualification)
describes the two independently materialized receiver outputs. Receiver
packaging itself runs no SDK, compiler, native or historical runtime suite.

The local proof qualifies exact caller source and SDK behavior. Hosted Actions,
Windows checkout and Eclipse Maven/Tycho/PDE results must be assessed on the
published receiving commits. This packet makes no hosted-build or
whole-Eclipse acceptance claim.

## Published source, lineage and licenses

All **30 frozen crate files**, the four unchanged SDK owners, and the emitted
caller are bound to [hsoliwal/com.synexia at `73b1a2fc1b3a74c6fd367a0ecb27b46aec8df39e`](https://github.com/hsoliwal/com.synexia/tree/73b1a2fc1b3a74c6fd367a0ecb27b46aec8df39e/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ci-callers-20261019),
canonical tree `bf1e94ac50dfb7946a8f605dcf7325dae3eb08c8`. The closed caller set is 30 crate files, four owners,
one actual output and exactly three receiver metadata files.

[SOURCE_RECEIPT.json](SOURCE_RECEIPT.json) records each exact source path, commit,
tree, Git blob, SHA-256 and current source binding.
[FILE_PROVENANCE.tsv](FILE_PROVENANCE.tsv) preserves every original nine-column
lineage field separately from the new public mapping. The three receiver
metadata entries use their receiving Git tree for identity and omit circular
self hashes and future commit claims.

Eclipse caller payloads retain EPL-2.0. The authored recipe and qualification
material use Apache-2.0; recorded tool output retains its original attribution.
The complete [Apache license](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ci-callers-20261019/LICENSE-APACHE-2.0),
[EPL license](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ci-callers-20261019/LICENSE-EPL-2.0), [crate NOTICE](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ci-callers-20261019/NOTICE),
[Platform upstream NOTICE](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ci-callers-20261019/UPSTREAM-NOTICE-platform), and
[UI upstream NOTICE](canonical/synexia-openrewrite-recipes/recipe-crates/m3-eclipse-ci-callers-20261019/UPSTREAM-NOTICE-ui) are unchanged copies.
The shared owners' original historical notice reference remains mapped to its
[immutable public UI notice](https://github.com/hsoliwal/eclipse.platform.ui/blob/52f64fe74beb3f8763cefd28651c5d5ab01d5d92/docs/M3-SCALE/runtime-forwarding-20261018/canonical/synexia-openrewrite-recipes/recipe-crates/m3-jface-forwarding-gate-20261018/NOTICE).
