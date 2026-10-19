<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Existing Eclipse CI caller contract

The named recipe `com.synexia.eclipse.CiCallers20261019` composes the unchanged
`com.synexia.rewrite.M3HashPinnedTextSnapshotRecipe`. Its manifest admits one
existing path, `.github/workflows/ci.yml`, independently in each of the Platform
and UI receivers pinned in `SOURCE_PINS.json`.

Both exact preimages have Git blob `fde222c38eb768bb09e2373d965627fa117a2ab6`
and SHA256 `a87ca1d04beb4f49a19628db2e46b32e62b302b716c89ce408faf4d8d228bf1d`.
Separate preserved preimage fixtures retain the two receiver lineages.

The only allowed change replaces the sole existing `jobs.build.uses` line:

```yaml
    uses: eclipse-platform/eclipse.platform.releng.aggregator/.github/workflows/mavenBuild.yml@master
```

Its replacement references `hsoliwal/eclipse.platform.releng.aggregator`, the
same existing workflow path, and the actual immutable shared-workflow commit
recorded in `bound-workflow-commit.txt` and `SOURCE_PINS.json`. That commit must
already be published and read back before a postimage or proof is sealed.
There are no future commit placeholders in the qualification.

Replacing the new line with the old line must restore every original byte.
This preserves caller triggers, branch/path filters, concurrency settings,
inputs, permissions and all other caller content, including their absence where
the original file provides none. There is no new registered workflow, trigger,
permission, action, input, build profile or gate.

## Qualification

The full finite suite uses the actual OpenRewrite SDK and the four existing
shared owners in their default canonical source location. Each receiver has an
independent named recipe invocation with one existing `ci.yml` input and exactly
one emitted result. Each materialized file must come from that real changeset.

The suite checks the reviewed output digest and exact line replacement,
closed manifest, fixed point, missing/drifted callers, duplicates and path
aliases, unrelated sources and input permutations, plus direct/named serial
and two-worker replay. The initial no-op postimage must produce a genuine RED
failure of the positive one-output assertion. The corrected full run must pass
all six JUnit methods. The canonical owner and historical crate bytes remain
unchanged; their already-qualified runtime and native suites are not rerun.

This qualifies exact caller source and SDK behavior. It does not execute hosted
Actions, Windows checkout, Eclipse Maven/Tycho/PDE, or a whole-Eclipse build.
Those receiving gates retain their existing meaning after publication.

## Rights and publication

The two caller inputs and their common reviewed postimage retain EPL-2.0.
Their separate upstream notices are preserved. First-party recipe, test, Maven
configuration and authored evidence are Apache-2.0; the recipe does not relicense
its payload. The integration owner controls all receiving branches, refs and
PRs. This crate changes no shared workflow, gitlink, global ledger or canonical
home during qualification.
