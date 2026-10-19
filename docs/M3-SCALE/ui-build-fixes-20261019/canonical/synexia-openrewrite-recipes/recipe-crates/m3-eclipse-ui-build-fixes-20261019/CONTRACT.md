# Eclipse UI build fixes — frozen two-file contract

Copyright © 2026 Hitesh Soliwal and Contributors to the Synexia Project.
SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0

The receiver is `hsoliwal/eclipse.platform.ui` at
`52f64fe74beb3f8763cefd28651c5d5ab01d5d92`. Its macOS CI compiler rejects ten
unmarked runtime string literals in `TreeViewer`. Those source bytes and JFace's
`nonExternalizedStringLiteral=error` preference also occur in the qualified UI
base `16e944f83eec18d4cf609afd9ad9a937711211ce`. The Linux API Tools job separately
requires a minor Commands version increment after its execution environment was
raised to Java 21. Exact failed job evidence is retained under `evidence/ci/`.

## Admitted changes

1. `bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/TreeViewer.java`:
   append exactly one `//$NON-NLS-1$` marker on each of lines 77, 79, 81, 83, 85,
   87, 89, 91, 122 and 135. The string values, executable tokens, line count,
   method bodies, public/protected API, reflection names, error propagation and
   existing SWT forwarding remain unchanged.
2. `bundles/org.eclipse.core.commands/META-INF/MANIFEST.MF`:
   replace only `Bundle-Version: 3.13.100.qualifier` with
   `Bundle-Version: 3.14.0.qualifier`. Keep `JavaSE-21`, all package exports,
   requirements and other metadata byte-identical.

The recipe composes the existing `M3HashPinnedJavaSnapshotRecipe` and
`M3HashPinnedTextSnapshotRecipe`, in that order. `SOURCE_PINS.json` binds their
unchanged four source owners, target hashes and compiler policy. Java admission
requires an actual parsed compilation unit and exact source identity. Text
admission uses the existing Eclipse manifest path rule. No name substitution,
token matcher, custom parser, compiler stub or broadened path admission is added.

## Qualification contract

Execute the named recipe through OpenRewrite 8.17.1 with Java 21, exact genuine
Eclipse parser dependencies and JUnit API/engine 5.10.2. Require exactly two
emitted postimages, independent-owner and input-order agreement, exact fixed
point, partial replay, duplicate/missing/drift refusal, rejection of a plain-text
Java target, and the existing text owner's path-alias refusal. Code-looking
strings/comments in unrelated Java and text inputs remain unchanged.

Compile all 423 actual JFace production sources with real ECJ 3.46.100 using
the unchanged project preferences and genuine SWT/Eclipse dependencies. The
baseline must retain the ten NLS errors. The SDK-emitted candidate must remove
those errors without changing diagnostic severity. Warnings and additional
failures remain visible. Compile the original and emitted `TreeViewer` with the
same javac options and classpath, then compare every emitted class byte-for-byte.

Source identity is checked before and after compilation. Logs, commands,
toolchain/dependency hashes and source inventories are retained. The original
four crates and native RED/GREEN evidence stay immutable. No native test, full
Tycho/PDE reactor or API Tools rerun is implied by this bounded compiler and SDK
qualification. Receiver CI retains final product authority.
