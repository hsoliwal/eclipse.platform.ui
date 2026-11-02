# JFace queue repair and focused qualification

SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
SPDX-License-Identifier: Apache-2.0

This selection fixes two defects in the existing `M3Viewers.Queue`: duplicate adjacent refresh posts ran repeated refresh callbacks, and a timer belonging to an already flushed batch could drain a newer pending batch. It preserves the existing queue, viewer dispatch, SWT display ownership, viewport/session owners, and public/protected methods. No new viewer or runtime engine is added.

## Exact ownership

The canonical recipe owner is Synexia PR12639, branch `m3/eclipse-structures-20261021-b38a`, parent `20ed2a74122214ea301fc70fe5dac1d61f85f822`. This selection extends its existing Maven crate and unchanged Java/text snapshot engines, SDK tests, and receiver. It does not restore a duplicate crate onto develop.

The target is Eclipse UI PR27, branch `m3/jface-all-viewers-core-repair-20261021`, revision `67cd89ec3ea55e6bf13811666ffe90fe2c6adc08`. The earlier baseline used revision `8a6d6d03abb6f091c7cf4c5c465bb8b577d9fbc3`. Both runtime owner blobs are unchanged between those revisions; the upstream test's import-order correction is retained. Source blobs and byte hashes are in `synexia-openrewrite-recipes/recipe-crates/m3-eclipse-structures-20261021/evidence/jface-queue/SOURCE_PROVENANCE.json` and `sources/CURRENT_SOURCE_PINS.json` below that evidence directory.

## Contract of the repair

A post is coalesced only when it contains one refresh, the pending delta's last operation is also a refresh, and both target the identical object. Identity comparison invokes no `equals` callback. Intervening updates, additions and removals remain ordering barriers. Mixed deltas retain their operations, and direct `apply` with two refresh operations still invokes two refreshes. Every post continues to advance the existing generation/debounce scheduling.

The existing private queue gains one primitive batch counter. New pending batches advance it; each timer captures the counter and can drain only a still-pending matching batch. This prevents a stale timer from acting on later work without capturing and retaining the old operation buffer. PR27's bounded self-append repair in `M3ViewerDelta` is unchanged.

## Executed gates

| Gate | Result |
| --- | --- |
| Actual existing OpenRewrite SDK tests | 8 passed, 0 failures/errors/skips |
| Independent replay schedules | 5 schedules produced root `ecf2a006f1ce1dbb097e9f004f5299381f8f86596161935dd60e63c44c3f7f43` |
| Existing strict receiver | Check 2 changes; apply 2; check 0 |
| Before repair, real SWT/JFace | 5 tests: 2 controls passed; 3 expected regression failures retained |
| Actual emitted source compile | JDK21 `--release 21 -Xlint:all -Werror` passed |
| After repair, complete focused owner suites | `M3ViewersTest` 14/14 and unchanged `M3ViewerDeltaTest` 4/4 passed |
| Public/protected runtime API | `javap -protected` output byte-identical for both owners |

The baseline failures show refresh callbacks of 2 rather than 1, premature later-batch row count of 7 rather than 6, and repeated content-provider reads in the first family case. The after suite checks order barriers, direct-apply behavior, deterministic stale-timer order, disposal, off-thread post/flush behavior, fallback refresh, and the preserved self-append bound. The invalid-index fallback test intentionally logs a warning while passing.

The focused run used Temurin21.0.8+9, published JFace3.40.0 and SWT GTK3 3.135.0 with real native libraries, JUnit Jupiter5.14.4, and an isolated official Xvfb server. Source owners under repair were emitted by the actual canonical recipe and applied by its existing receiver. Provider jars/native binaries are not distributed in this source packet; official source URLs and byte hashes are recorded. This run does not build the full current Eclipse source repository or qualify the new iterative tree algorithm integration.

## Viewer coverage and remaining gates

The new family regression executes nine concrete variants through the shared queue: table, tree, list, native combo, CCombo, checkbox table, checkbox tree, virtual table with a non-lazy provider, and virtual tree with a non-lazy provider. Each receives two refresh posts and must perform one provider read while preserving selection. Checkbox variants also preserve checked and grayed state.

`synexia-openrewrite-recipes/recipe-crates/m3-eclipse-structures-20261021/evidence/jface-queue/VIEWER_COVERAGE.tsv` maps the exact reviewed owners, existing family tests, executed scope, and remaining gates. Lazy providers, full expansion/path/editing semantics, native platform matrices, and family PDE suites were not executed. The owning current test bundle requires Java25; full Tycho/PDE `verify` remains a separate unfulfilled gate.

The Problems reference has its own existing marker-job and async-preparation pipeline, then calls `M3Viewers.apply` inside its selection-preserving wrapper. That direct-apply path remains unchanged. This repair does not claim marker-pipeline end-to-end acceptance. The existing inventory also identifies Navigator, Outline, Properties, Progress, Error Log, Search, dialogs, e4 stacks, external Console/JDT/debug/team views and Nebula grids; a candidate inventory entry is not evidence of a defect or a completed repair.

No performance speedup or whole-product completion is asserted by these correctness results.

The full source-bound evidence and recipe resources are carried by the companion Synexia selection. Local summary and coverage records are in `docs/m3/views/jface-queue-20261021/`.

## Portable preparation and bound evidence

The published configuration is `JFACE_QUEUE_INPUTS.json`, which contains relative resource paths and exact source/provider hashes. It contains no task-directory path. `prepare-jface-queue.py` is limited to this one selection: it verifies the fixed host and four canonical engines, exact target source preimages/postimages, seven published dependency JARs, and the pinned Temurin21.0.8+9 image files. It compiles only genuine PR27 `M3Viewers` and `M3ViewerDelta` source to the three parser-provider classes. It neither downloads dependencies nor changes the Eclipse checkout.

Supply a Synexia checkout containing this selection, an Eclipse checkout containing the pinned source bytes, the seven exact JARs listed in `JFACE_QUEUE_INPUTS.json`, and the recorded JDK image. Official artifact URLs and checksums are in `synexia-openrewrite-recipes/recipe-crates/m3-eclipse-structures-20261021/evidence/jface-queue/dependencies/PROVENANCE.json`. This tested preparation profile is Linux x86_64 with the exact JDK image recorded; another toolchain requires a separately pinned and qualified profile.

Set the following variables to local absolute paths. The provider-work directory must be new and outside the Eclipse target. The preparation step creates local `contract/jface-queue/TARGETS.json`, `parser-classpath.txt`, and `PREPARATION.json`; these generated files are excluded from source publication. The unchanged SDK and receiver still require these explicit absolute bindings.

```sh
python3 "$m3_crate/prepare-jface-queue.py" \
  --input-root "$m3_ui" --dependencies "$m3_viewer_jars" \
  --java-home "$m3_java_home" --work-root "$m3_provider_work"

JAVA_HOME="$m3_java_home" "$m3_maven" -o -B -ntp \
  -Dmaven.repo.local="$m3_m2" -f "$m3_crate/pom.xml" \
  -Dm3.selection=jface-queue -Dm3.input.root="$m3_ui" test

python3 "$m3_crate/receive.py" --selection jface-queue --target "$m3_ui" --check
python3 "$m3_crate/receive.py" --selection jface-queue --target "$m3_ui"
python3 "$m3_crate/receive.py" --selection jface-queue --target "$m3_ui" --check
```

`m3_crate` names this existing crate, `m3_ui` the target checkout, `m3_viewer_jars` the pinned JAR directory, `m3_java_home` the pinned JDK, `m3_provider_work` the new output directory, `m3_maven` the Maven executable, and `m3_m2` the populated Maven dependency cache. Offline mode reproduces the recorded run and requires that cache. Preparation rejects source/provider drift, symlinks, and newline/classpath-separator paths; inherited Java option variables are removed from the provider compiler process. Pinned sources, JARs and JDK inputs are rechecked before local bindings are written.

The clean relocation evidence records this exact preparation followed by the unchanged eight-test SDK and receiver in a fresh task directory, without network access. Its emitted Java bytes must equal the previously compiled and 18-test-qualified source. Original absolute-path receipts are retained as historical execution evidence, not ready-to-run configuration. The generated contract's initial qualification state is separate from the completed result in `synexia-openrewrite-recipes/recipe-crates/m3-eclipse-structures-20261021/evidence/jface-queue/QUALIFICATION.json`; evidence reporting does not mutate bound SDK inputs.

## Licensing

This narrative and new metadata are first-party Apache-2.0. Embedded Eclipse preimages, postimages, patches, the corrupt-template source fixture, and the historical baseline test retain their original copyright and `Apache-2.0 OR EPL-2.0` declarations. Nothing in this metadata relicenses copied source. Existing canonical engines and the host are unchanged; the crate's existing license/provenance materials continue to apply.
