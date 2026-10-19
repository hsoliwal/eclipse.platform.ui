# Eclipse UI compiler and bundle-version repair

Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project.
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0

This additive Synexia crate repairs two compiler/build defects at public UI
commit `52f64fe74beb3f8763cefd28651c5d5ab01d5d92`. It leaves the original Rules,
Commands, JFace native-gate and composition recipes and evidence unchanged.

The macOS job failed JFace's existing ECJ NLS rule on ten runtime literals in
`TreeViewer`. The repair marks those literals without altering their values or
the forwarding implementation. The Linux job failed Commands API Tools because
changing the execution environment to Java 21 requires a minor bundle-version
increment. The repair raises `3.13.100.qualifier` to `3.14.0.qualifier` and keeps
the existing Java 21 requirement and package contracts.

The actual failed job excerpts, source/preference identities and unchanged-base
comparison are retained in `evidence/ci/`. See `CONTRACT.md` for the exact two-file
edit boundary, `SOURCE_PINS.json` for source and shared-owner identities, and the
recipe manifests for byte-level preimage/postimage admission.

`com.synexia.eclipse.UiBuildFixes20261019` composes the existing JavaSnapshot and
TextSnapshot owners. The POM compiles those canonical sources through its normal
relative owner path. SDK execution produces the product files under
`target/recipe-output`; templates alone are not an application receipt.

## Recorded qualification

| Gate | Actual result |
| --- | --- |
| OpenRewrite 8.17.1 / JUnit 5.10.2 | Six tests, zero failures/errors/skips; exactly two emitted postimages |
| Source SWT dependency | All 546 pinned GTK Java sources compile with JDK 21 `--release 21` |
| Baseline full JFace ECJ compile | All 423 sources visited; exit 255, exactly ten NLS errors, 26 warnings |
| Candidate full JFace ECJ compile | All 423 sources compile; exit 0, zero errors, 26 warnings |
| Unchanged compiler policy | Same exact preferences, including `nonExternalizedStringLiteral=error` |
| TreeViewer class comparison | Both javac runs exit 0; direct comparison finds identical 32,414-byte class files |
| Source/owner readback | Pinned UI/SWT inputs, compiler preferences and all four shared owners remain unchanged |

The local compiler is genuine ECJ 3.46.100 on Temurin JDK 21.0.12.1+1. JFace
uses actual source-built SWT classes and the eight genuine Eclipse/OSGi artifacts
listed in `proof/inputs/eclipse-dependencies.json`; the released JFace artifact
is parser context only and is excluded from the full JFace production compile.
The local 26 warnings are recorded separately from the original macOS CI's 25
warnings. They are not suppressed or promoted to a warning-free claim.

The initial offline SDK attempt stopped before executing tests because Surefire's
JUnit Platform Launcher 1.10.2 was missing from the recovered cache. Its raw log
is preserved under `evidence/qualification/earlier-attempts/`. The later real
six-test result uses that genuine artifact; this setup refusal is not the NLS
counterexample. The original CI failure and the strict ECJ baseline supply the
actual source failure evidence.

## Reproduce

Provide read-only checkouts at the exact UI/SWT revisions in `SOURCE_PINS.json`,
the toolchains identified in `evidence/toolchain/`, and the verified dependency
jars listed in `proof/inputs/eclipse-dependencies.json`. Populate a local Maven
repository with the POM's exact dependencies/plugins and the Surefire-selected
JUnit Platform Launcher 1.10.2. The qualification itself runs Maven offline.

From this crate directory, use the real local paths:

```bash
python3 proof/qualify.py \
  --ui-root /path/to/eclipse.platform.ui \
  --swt-root /path/to/eclipse.platform.swt \
  --java-home /path/to/temurin-21.0.12.1 \
  --maven /path/to/apache-maven-3.9.12/bin/mvn \
  --maven-cache /path/to/verified-maven-cache \
  --ecj /path/to/ecj-3.46.100.jar \
  --eclipse-deps /path/to/verified-eclipse-jars \
  --phase all
```

`baseline`, `sdk` and `candidate` may also run in that order to keep one bounded
stage active at a time. The actual commands, source inventories, unedited full
compiler text logs, JUnit XML, output hashes and source readbacks are under
`evidence/qualification/`. ECJ's verbose XML is generated under `target/` and
hashed in the receipts; it is not a distributed binary or a replacement source
oracle. The same-line source diff and direct classfile comparison substantiate
the NLS repair's unchanged executable code.

This crate does not rerun the frozen native test, full Tycho/PDE reactor or API
Tools. The Commands minor-version repair matches the recorded API Tools demand;
its new version is not an API Tools pass claim. Receiver CI retains final product
acceptance authority.
