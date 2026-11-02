<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3 Eclipse command identity registries

This source-owned packet donates the existing packed collection implementation into Eclipse core.commands. The only substituted collection owners are `CommandManager.categoriesById` and `parameterTypesById`. Both are private, point-access identity registries. The public Eclipse objects, checks, state changes and listener dispatch remain in their established owners.

The runtime is owned and compiled by the receiving Eclipse bundle. It has no Synexia runtime dependency. The five donated classes retain their original Apache-2.0 notices, receive an explicit package-adaptation notice, and add Eclipse NLS comments without changing their storage algorithms. The receiver's EPL-2.0 source retains its original notices.

## Exact source custody

- Canonical donor: `hsoliwal/com.synexia@140c263a90f9803b9c46103c63355a286e0515f0`, `synexia-common/src/main/java/com/synexia/common/collections/jdk/`.
- Already-public source receiver: `hsoliwal/M3jdk21@ed0d5435eb8796cfd4c9375edfc05b72bb16c8c6`, `m3/collections/src/main/java/com/m3/collections/`. All five bodies are exactly equal after the established package/type mapping and leading-notice removal for comparison. Original notices remain in the actual donation.
- Eclipse receiver: `hsoliwal/eclipse.platform.ui@16e944f83eec18d4cf609afd9ad9a937711211ce`.
- Exact blobs, target pre/post SHA-256 values and public URLs are in `inventory/target-mapping.json` and `inventory/public-donor-lineage.json`.

## Generated scope

The named recipe `com.synexia.eclipse.CommandRegistryForwarding20261018` composes the existing canonical hash-pinned Java and text snapshot owners. It emits nine files: the five donated Java owners, the reviewed CommandManager constructor substitutions, and the bundle's three aligned Java21 metadata owners. Every other file in the bundle remains byte-identical to the receiver pin.

The donated implementation uses Java21's sequenced collection contracts. The bundle minimum execution environment, JRE container and compiler source/compliance/target levels therefore all become Java21 together. Existing compiler diagnostic policies remain unchanged. The M3 package is private to the bundle and is not exported.

The prepared resources are the reviewed recipe inputs. Only actual OpenRewrite SourceFile Results create the qualified candidate image. The real SDK proof checks exact generation, fixed point, missing/drift/duplicate/occupied-target refusal, input permutation, independent composition, unrelated inputs and partial replay.

## Contract boundary and refused expansion

The inherited command-handle map and all defined-ID sets retain their current implementations. Bulk handler updates traverse the command-handle map while listeners can inspect other commands. A frozen external counterexample creates z before a and inspects a from z's callback. The original and admitted implementations observe true; the broader insertion-order substitution observes false.

That broader substitution exists only under test resources as an SDK-generated refusal fixture. It is absent from the named production recipe. ContextManager state sets and the help-context WeakHashMap also remain unchanged. No callback traversal or weak-lifetime substitution is admitted by this packet.

## Executed qualification

The receipt records exact commands, dependencies and hashes:

| Gate | Observed result |
| --- | --- |
| Original behavioral baseline | 12 tests, 0 failures/errors/skips |
| Original density discriminator | 1 intended failure: 4096 retained entries |
| Real OpenRewrite SDK | 6 tests, 0 failures/errors/skips |
| Generated entire bundle | 84 Java sources; 13 tests, 0 failures/errors/skips |
| Density in each generated registry | 0 retained Map.Entry objects after 4096 identities |
| Refused broader map | 1 intended failure: callback [true] becomes [false] |
| Existing public/protected API | All 83 pre-existing class declarations/descriptors equal |
| Donor executable bodies | All 15 compiled classes equal after package normalization |
| Genuine ECJ with bundle policies | Original79 sources at release17 and generated84 at release21 compile; generated donor has two permitted unused-parameter warnings |
| Genuine Equinox | Bundle resolves and starts; 8211 assertion checks pass |

The behavioral suite covers identity, equal/colliding IDs, growth, namespace separation, null/empty validation, definition events and views, snapshots, listener identity/removal, reentrancy, callback effects, protected fields and weak help-context storage. The original upstream CommandManager test is preserved byte-for-byte. The independent tests remain proof sources because the canonical Java owner does not admit the receiver's legacy Eclipse UI Tests path.

The Equinox probe uses the genuine framework and common bundles, the actual ECJ-compiled generated classes and the generated manifest. It checks classloader ownership and the unexported M3 package as well as registry identity. This is a direct JAR integration proof; a full Tycho build, complete SDK/workbench run and all supported-platform qualification are separate gates.

## Reproduction and limits

Run the Maven proof POM with Java21 and OpenRewrite8.17.1. Its default `recipe.owner.root` resolves to the canonical module's `src/main/java` via `${project.basedir}/../../src/main/java`; an explicit `-Drecipe.owner.root` can bind a pinned authoring mirror. Supply the real baseline commands classes and Equinox common through `-Dcanonical.classpath`; optional `-Dmaterialize.dir` and `-Dmaterialize.refused.dir` write only actual SDK Results into separate fresh directories. Exact executed arguments are retained in the evidence command receipts.

Run `runtime-pom.xml` with `-Dcommands.source.root` pointing to the complete source image and `-Dequinox.common.jar` pointing to the verified real bundle. The original behavioral baseline excludes the density tag; the generated candidate runs all tests. The refusal image runs only the frozen cross-command test and must fail for the specified observation, with no unrelated error.

The packed owner has expected constant-time access and linear collision worst case. The zero retained entry result is a representation observation, not a total-byte or throughput benchmark. This packet does not replace the Jobs scheduler, resources engine, OSGi registry, workbench, editors, JDT or PDE, and it does not claim full Eclipse migration.

## Publication archive

`RECEIPT.json` and the original command logs remain byte-identical to the qualified authoring packet. Six original Surefire XML files are retained under `evidence/junit`: the six-test SDK run, the twelve-test baseline, the thirteen-test generated candidate, and the expected one-test refusal. The original density discriminator's expected failure is retained in its command receipt and console log; no replacement XML is synthesized.

Canonical packaging normalizes only the SPDX comments in the recipe proof POM and SDK test, adds the missing notice to the refusal-fixture README, and adds source/archive metadata. These comment and documentation changes do not change any of the nine generated product files. `inventory/publication-archive.json` records the qualified and archived hashes separately; the archive does not claim a new execution of the recorded tests.
