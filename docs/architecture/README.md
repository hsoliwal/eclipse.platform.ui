# Platform UI architecture and coverage

This guide extends the repository's [contributor README](../../README.md) with source-backed package/API contracts and an explicit coverage ledger. It documents the current fork; it does not declare every package complete or convert proposed architecture into implemented behavior.

## Revision and provenance

- Fork: [hsoliwal/eclipse.platform.ui](https://github.com/hsoliwal/eclipse.platform.ui)
- Verified upstream: [eclipse-platform/eclipse.platform.ui](https://github.com/eclipse-platform/eclipse.platform.ui)
- Source inspected: `b8874e6b63390d65e47612574686cb8cf97b1fe8`, branch `docs/fork-contributor-navigation-20261014`, on 14 October 2026
- Existing draft: [PR #10](https://github.com/hsoliwal/eclipse.platform.ui/pull/10), targeting the fork's `master`
- Base of that draft when read: `9858a8505fd7b36705936f178e448bc8e44cf267`
- Only Markdown is changed by this extension. Existing source, API, native files, generated files, dependencies, build configuration, workflows, license and attribution are preserved
- Source evidence uses immutable repository permalinks and Git blob identities. Private task material and unrelated project details are not part of these pages

The inspected revision precedes this documentation commit. The publishing comparison must therefore show only documentation changes; source-sensitive updates require a fresh source baseline rather than silently reusing these claims.

## Navigation

1. [Lifecycle and package contracts](lifecycle-contracts.md): viewer/provider ownership, JFace resource counts and registries, Window event loops, workbench views/editors and data-binding realms
2. [Project and module ledger](module-ledger.md): descriptor-backed identities, dependencies, source roots, exports and build boundaries
3. [Java package/source-set ledger](package-ledger.md): every Java-containing source directory, representative package declaration, blob identity, existing docs and explicit gaps
4. [Exports without a local Java witness](export-boundaries.md): fragment, split/resource/fixture export boundaries not represented by a local Java file
5. [Supporting sources](supporting-sources.md): native/script units, root-tree ownership, model/extension and opaque artifact boundaries

## Build and test routes

The [root README](../../README.md#how-to-build-on-the-command-line) documents `mvn clean verify` and the focused JFace source-plus-test command. [The root POM](../../pom.xml) enables the UI harness and UI thread by default. [The Jenkinsfile](../../Jenkinsfile) selects JDK 25 and an Xvnc session; [Maven configuration](../../.mvn/maven.config) enables individual-bundle resolution, eager targets and ignoring local Tycho artifacts.

A focused route is `mvn clean verify -pl :org.eclipse.jface,:org.eclipse.jface.tests`. It does not cover unrelated modules. Include the changed production owner with its test bundle, and inspect test/compiler/API reports: the Jenkins command uses `maven.test.failure.ignore=true`. JavaSE-21 bundle declarations do not override that JDK 25 CI selection.

Build instructions above were read from public source. No Maven, compiler, test, native library, example, script, model generator or application was run for this documentation extension.

## Coverage and open gates

| Level | Discovered/included | Excluded | Reviewed-complete | Partial | Missing / unresolved depth |
| --- | ---: | ---: | ---: | ---: | --- |
| Repository | 1 | 0 | 0 | 1 | Full lower-level semantics and verification remain open |
| Descriptor-backed projects/build units | 154 | 0 | 0 | 154 | Complete project responsibilities/configuration/artifacts and active build membership require deeper review |
| OSGi modules/fragments | 143 | 0 | 0 | 143 | Manifest boundaries recorded; complete exported/internal behavior and test mapping remain open |
| Java source-directory package occurrences | 767 | 0 | 0 | 157 | 610 have no substantive package review in this pass |
| Export clauses without a same-owner Java witness | 44 | 0 | 0 | 44 | Resolve shared-source, resource/generated or fixture semantics using the export ledger |

Reviewed-complete coverage is **0%** at each nonempty level under the strict all-content/all-verification gate. This does not mean there is no useful documentation: the new contract guide explains selected method bodies, and 10 source occurrences link to those contracts. It means an inventory, existing package page, draft PR or unrun test is not being counted as complete documentation.

The 7549 Java-file tree entries map to 767 physical source directories. Representative declarations witness 713 distinct named namespaces. Repeated namespaces are kept separate by source path/owner/variant. Every discovered directory has a scope disposition, but every Java declaration has not yet been scanned; differently declared fixtures or mixed directories can add namespace identities. Package counts are consequently an explicit discovery baseline, not a claimed exhaustive semantic denominator.

Additional unresolved boundaries are included in [supporting sources](supporting-sources.md): opaque archives/binaries were not unpacked, generated code correspondence was not verified, and source/script/model metadata is not full behavioral documentation. These gaps prevent an overall completeness claim.

### What was inspected

- Complete Git trees, without a truncated subtree; all discovered conventional project descriptors and alternate analysis/Ant descriptors called out in the module ledger
- Bundle manifests and available build.properties, including source roots, module identities, dependency/import/export declarations and platform filters
- One complete representative Java file per source directory for namespace/type discovery; existing package.html/package-info.java locations
- Full relevant declarations and method bodies behind the selected lifecycle contracts, plus specifically linked test bodies and build/CI entry points
- Existing repository instructions and README content; external edits are preserved

### Verification receipt and limits

| Stage | Result | Scope |
| --- | --- | --- |
| Immutable source/tree and descriptor retrieval | OBSERVED | Connector reads at the source revision above |
| Documentation navigation and path/anchor checks | PASS | 3254 links checked in generated architecture pages against the fetched tree and generated headings; no external-site availability guarantee |
| Final remote Markdown-only diff/read-back | Publication receipt on the draft PR | Separate from this source snapshot; not a runtime or semantic completeness claim |
| Java compiler / Maven / Tycho / Javadoc / API analysis | NOT_RUN | Source-only connector work |
| Tests / native builds / UI / performance / memory | NOT_RUN | No runtime execution performed |
| Cross-platform equivalence or compatibility qualification | NOT_RUN | A GTK method read or one package test source cannot establish other platform outcomes |

### Next review units

1. Finish concrete JFace viewers, dialogs/wizards/preferences/actions/bindings and all data-binding observable/property/conversion/validation variants
2. Review workbench services, commands/undo, E4 model/DI/renderer/CSS, IDE integration, navigator/forms/search/refactoring and text/filebuffer/editor lifecycles
3. Establish generated EMF/source correspondence, extension-point/configuration contracts, resource-only exports and split-package ownership
4. Complete examples, manual/performance tests, dynamic fixture bundles and legacy/stub projects; do not infer production behavior from fixture descriptors

For every unit above, inspect complete relevant implementation and tests, document inputs/outputs/errors/state/effects/ownership/concurrency, reconcile existing inline/package docs, and record exact executed or unrun checks. Markdown-only scope leaves Java package-info/Javadoc edits and runtime checks unperformed; it does not waive their eventual completion gates.

## Project terms

Existing [LICENSE](../../LICENSE), [NOTICE](../../NOTICE) and [CONTRIBUTING.md](../../CONTRIBUTING.md) remain authoritative for this repository. No license compatibility, upstream adoption, performance, production-readiness or broad implementation-completeness claim is made by these documentation pages.

