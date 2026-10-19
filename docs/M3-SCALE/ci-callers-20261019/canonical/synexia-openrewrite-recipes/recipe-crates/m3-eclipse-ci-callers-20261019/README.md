<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# Eclipse CI callers: immutable shared-workflow pin

This Synexia-owned recipe changes only the shared-workflow `uses` reference in
the existing Platform and UI `ci.yml` callers. The same one-target recipe is
applied independently to the two receiving repositories. Their exact source
commits, source identities and the already-published workflow commit are in
`SOURCE_PINS.json`.

The target remains `.github/workflows/ci.yml`. Its original caller is the
upstream aggregator's existing `mavenBuild.yml@master`. The reviewed replacement
uses the same workflow path in the authorized fork at one actual immutable
commit. Every other byte remains unchanged, preserving triggers, concurrency,
branch/path filters, inputs and permissions. `CONTRACT.md` and `SPECTRUM.md`
record the closed boundary and finite checks.

## Reproduce the SDK qualification

Use Java 21 and Maven with the dependencies pinned in this crate's POM. From
the canonical crate directory:

```sh
mvn -B -ntp -Dmaterialize.dir=/absolute/path/to/caller-output test
```

The POM compiles the four unchanged shared owner sources from the default
`${project.basedir}/../../src/main/java` location. The descriptor activates the
existing class-backed TextSnapshot recipe through the real OpenRewrite SDK.
The six-method suite materializes actual SDK results at:

- `eclipse.platform/.github/workflows/ci.yml`
- `eclipse.platform.ui/.github/workflows/ci.yml`

Each file comes from its receiver's independent SDK invocation. The two files
have the same reviewed bytes because their pinned preimages are identical.
The SDK admits one existing target per invocation; it creates no new workflow.

The baseline no-op RED and corrected full run are retained separately under
`evidence/sdk-red/` and `evidence/sdk-green/`. The receipt binds the exact
qualified source inputs, toolchain, raw log/XML, two materialized outputs and
unchanged protected source identities. The first test verifies that replacing
only the new uses line with the old line restores every original caller byte.

## Receiving scope

The shared workflow commit must exist and pass branch readback before this
crate's postimage and proof are sealed. Its source qualification is retained
in the earlier `m3-eclipse-ci-longpaths-20261019` crate. This caller crate changes
no shared-workflow source or receiving gate.

Local SDK success does not establish hosted Windows checkout or an Eclipse
Maven/Tycho/PDE build. Those gates apply after the callers receive the output.
Existing runtime/native evidence and canonical owners remain unchanged.

Payloads retain EPL-2.0 and both receivers' upstream notices. The first-party
recipe and qualification material are Apache-2.0; see `NOTICE` and the preserved
licenses. The integration owner publishes the exact outputs and owns all refs,
PRs, global provenance and canonical-home updates.
