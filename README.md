<!-- fork-homepage-detail:v1:start -->
# Eclipse Platform UI: viewers, workbench and rich-client applications

Platform UI connects application models to SWT controls and supplies the Eclipse
workbench: views, editors, commands, perspectives, dialogs, data binding and E4
application services. Use JFace for reusable presentation components, and the
workbench/RCP layers when building an Eclipse application. Native SWT widgets
are maintained in a separate repository.

[Layer map](#ui-layer-map) · [Concepts](#ui-key-concepts) ·
[Quickstart](#ui-quickstart) · [Example](#ui-worked-example) ·
[Architecture reference](docs/architecture/README.md)

## UI layer map

| Layer | Start here | Typical question |
| --- | --- | --- |
| JFace | [org.eclipse.jface](bundles/org.eclipse.jface/) | How does model input become viewer content, labels or selection? |
| Data binding | [Core binding](bundles/org.eclipse.core.databinding/) and [SWT/JFace binding](bundles/org.eclipse.jface.databinding/) | Which observable, realm and owner controls an update? |
| Workbench / IDE | [Workbench](bundles/org.eclipse.ui.workbench/) and [IDE](bundles/org.eclipse.ui.ide/) | Who owns an editor, view, site or selection service? |
| E4 model | [Workbench model](bundles/org.eclipse.e4.ui.model.workbench/) | How is application structure represented? |
| Commands | [Core commands](bundles/org.eclipse.core.commands/) | Which command and handler define an action? |
| Tests / examples | [tests/](tests/) and [examples/](examples/) | Where is the smallest matching reproduction? |

## UI key concepts

- **Viewer:** exposes model input/selection through a control.
- **Content / label provider:** supplies model elements versus their presentation.
  Providers have replacement and disposal lifecycles.
- **Workbench part:** an editor or view managed by workbench services; its controls,
  listeners and resources still need a clear lifetime.
- **Realm:** the execution context for observable access and notifications; it is
  not permission to access arbitrary SWT widgets from a worker thread.
- **Resource manager / registry:** shared-resource ownership. Do not call SWT
  disposal directly on a resource borrowed from a JFace manager or registry.
- **RCP:** the Eclipse rich-client application platform; some bundles are reusable
  there while other bundles specifically serve the IDE.

## UI fork status

This fork derives from [Eclipse Platform UI](https://github.com/eclipse-platform/eclipse.platform.ui).
The established JFace/workbench implementations and
[M3 sidecar](.m3/README.md) coexist with a source-backed
[architecture and coverage guide](docs/architecture/README.md).
Its module/package inventory and selected lifecycle contracts are documented;
complete per-package semantics and runtime qualification remain separate work.
The M3 direction below is not a claim of measured responsiveness, memory savings
or complete runtime integration in this checkout.

## UI quickstart

The inspected [Jenkinsfile](Jenkinsfile) selects **JDK 25**. Prepare Maven 3.9.x,
network access to Eclipse parent/p2 dependencies and a working SWT/native display.
The Oomph setup below creates an upstream workspace; verify fork remotes before
editing. [Maven defaults](.mvn/maven.config) configure individual-bundle/target
resolution; a plain Java classpath is not the PDE target platform.

```sh
git clone https://github.com/hsoliwal/eclipse.platform.ui.git
cd eclipse.platform.ui
git rev-parse HEAD
java -version
mvn -version
# Focused JFace source-plus-test lane; not the whole repository.
mvn clean verify -pl :org.eclipse.jface,:org.eclipse.jface.tests
```

Use `mvn clean verify` for the ordinary full reactor. Tycho's integration-test
lifecycle requires `verify`; a `test` invocation alone is not equivalent.
These commands were checked against source/configuration, not executed here.

## UI command and ownership cheat sheet

```sh
git grep -n -e 'setContentProvider' -e 'setInput' -- bundles/org.eclipse.jface/src
git grep -n 'handleDispose' -- bundles/org.eclipse.jface/src/org/eclipse/jface/viewers
find . -path '*/target/surefire-reports/TEST-*.xml' -print
```

- Include the modified production bundle alongside its tests so the selected run
  does not silently exercise only a target-platform copy.
- For a stale label, distinguish model mutation, provider notification and viewer
  refresh before modifying SWT painting.
- For shutdown failures, trace provider listeners, control disposal and delayed
  callbacks. A repaint cannot repair invalid ownership.

## UI worked example

**Replace a viewer's input without retaining the previous model.** Read
[ContentViewer.java](bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/ContentViewer.java)
with [the lifecycle guide](docs/architecture/lifecycle-contracts.md).

1. Create the viewer/control, install its content and label providers, then set input.
2. In `setInput`, the provider receives old/new input before the viewer stores the
   new input. Detach old-model listeners and attach new-model listeners there.
3. Keep that provider callback safe for teardown as well: input removal is also
   part of disposal. Do not assume it is always a normal presentation update.
4. Provider replacement may dispose the old provider; sharing it across viewers
   requires an explicit shared-lifetime design.
5. Test input A → input B → control disposal, checking that A no longer notifies
   this viewer and cleanup releases listeners. The homepage describes the case;
   it does not claim those tests were run.

Continue with [JFace](docs/JFace.md), [data binding](docs/JFaceDataBinding.md),
[RCP](docs/Rich_Client_Platform.md) and
[the architecture coverage boundaries](docs/architecture/README.md).
The original project, contribution and licensing sections remain below.

> Source tour checked at [`119b433438c6`](https://github.com/hsoliwal/eclipse.platform.ui/tree/119b433438c6cca2e3d21365cf6054b7874a8d14). Commands describe that revision; recheck build configuration when changing revisions. Build/UI/performance verification was not run for this README update.

---

<!-- fork-homepage-detail:v1:end -->

<div align="center">

## M³ × Eclipse Platform UI

**Test runtime ideas against the real IDE workbench—not only isolated microbenchmarks.**

[![M3 research target](https://img.shields.io/badge/M%C2%B3-desktop%20research%20target-4b62b7?style=flat-square)](https://github.com/hsoliwal/M3jdk21)
[![Evidence first](https://img.shields.io/badge/acceptance-tests%20%2B%20measurements-287b57?style=flat-square)](https://github.com/hsoliwal/M3jdk21/blob/master/m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md)

**[M3 runtime programme](https://github.com/hsoliwal/M3jdk21)** · **[Technical paper](https://github.com/hsoliwal/M3jdk21/blob/master/m3/papers/M3_SHARED_STRUCTURE_AND_REUSABLE_COMPUTATION.md)** · **[Source invitation](#come-inspect-the-code)**

</div>

> [!NOTE]
> **This is an experimental downstream fork of [the upstream Eclipse Platform UI project](https://github.com/eclipse-platform/eclipse.platform.ui).** M3 String, regex, precompute and collection integration are **engineering directions**, not verified deployment, performance, compatibility or upstream-endorsement claims. Preserve original licensing, copyright, API behavior and native/UI contracts.

### Engineering questions for this fork

| Surface | What to investigate | Non-negotiable contract |
| --- | --- | --- |
| **Workbench state** | Measure lifecycle, selection, editor and view-state access | RCP/IDE extension points and public behavior |
| **SWT handoff** | Characterize event-loop responsiveness, model-to-widget transitions and native resources | Thread affinity, disposal and UI responsiveness |
| **Long sessions** | Reproduce open/edit/search/close cycles | No retained-state leaks or correctness regressions |

**Approach:** inventory first; copy/derive only with license and provenance; qualify reusable recipes in Synexia; port into the appropriate runtime/product owner; then test with representative desktop projects and publish CPU, allocation, retained-memory, startup and responsiveness numbers, including regressions. No Synexia runtime dependency.

### Come inspect the code

> **Java, JNI, SWT, workbenches, widgets, real code: open the repositories.** Bring a failing case, a performance counterexample, a profiler trace or a better algorithm. Challenge the design on the merits of source, tests and evidence—not slogans or a résumé.

**Explore the family:** [M3JDK21](https://github.com/hsoliwal/M3jdk21) · [SWT](https://github.com/hsoliwal/eclipse.platform.swt) · [Eclipse Platform](https://github.com/hsoliwal/eclipse.platform) · [Platform UI](https://github.com/hsoliwal/eclipse.platform.ui) · [Nebula](https://github.com/hsoliwal/nebula) · [GEF Classic](https://github.com/hsoliwal/gef-classic).

**Contribute here:** [issues](https://github.com/hsoliwal/eclipse.platform.ui/issues) · [pull requests](https://github.com/hsoliwal/eclipse.platform.ui/pulls) · [M3 compatibility and benchmark contract](https://github.com/hsoliwal/M3jdk21/blob/master/m3/release/BENCHMARK_AND_COMPATIBILITY_SPEC.md).

The remainder of this README retains the existing M3 fork notes and the **original upstream Eclipse Platform UI documentation**. Nothing in this introduction rebrands upstream work as M3-authored code.

---

## M3 direction in this fork

M3 is based on the idea that shared immutable structure and indexed metadata can
provide a basis for reusable computation. The programme starts with M3 String,
regex, and precompute in M3JDK21, then collections and SWT/Eclipse integration.

This Eclipse Platform UI fork is a target for evaluating qualified runtime and collection improvements in RCP and IDE workloads, including responsiveness and resource use.

**Explore the idea:** [M3 technical design paper](https://github.com/hsoliwal/M3jdk21/blob/master/m3/papers/M3_SHARED_STRUCTURE_AND_REUSABLE_COMPUTATION.md) ·
[Programme goals and roadmap](https://github.com/hsoliwal/M3jdk21#project-goals-and-plan).
The paper explains the proposed architecture, cost tradeoffs, and evaluation plan.
Readers are invited to examine the work and contribute representative workloads.

This section describes the direction of the hsoliwal fork. Upstream documentation
follows below; upstream authorship, licenses, and project identity remain intact.

---

# Eclipse Platform UI Project

Thanks for your interest in this project.


## Project Description

Platform UI provides the basic building blocks for user interfaces built with Eclipse.

Some of these form the Eclipse Rich Client Platform (RCP) and can be used for arbitrary rich client applications, while others are specific to the [Eclipse IDE](https://www.eclipse.org/eclipseide/). The Platform UI codebase is built on top of the Eclipse Standard Widget Toolkit ([SWT](https://www.eclipse.org/swt/)), which is developed as an independent project.

For more information, refer to the [Eclipse Platform project page](https://projects.eclipse.org/projects/eclipse.platform) and the [Platform UI wiki page](https://wiki.eclipse.org/Platform_UI).


## Source and documentation map

- [JFace](bundles/org.eclipse.jface/) provides viewers, dialogs and actions;
  start with the [JFace guide](docs/JFace.md)
- [Core data binding](bundles/org.eclipse.core.databinding/) and
  [JFace data binding](bundles/org.eclipse.jface.databinding/) have different
  owners; see [JFace data binding](docs/JFaceDataBinding.md)
- [The workbench](bundles/org.eclipse.ui.workbench/) and
  [IDE integration](bundles/org.eclipse.ui.ide/) sit alongside the
  [E4 workbench model](bundles/org.eclipse.e4.ui.model.workbench/);
  see the [RCP guide](docs/Rich_Client_Platform.md) and
  [E4 RCP FAQ](docs/Eclipse4_RCP_FAQ.md)
- [Commands](bundles/org.eclipse.core.commands/),
  [text infrastructure](bundles/org.eclipse.jface.text/), and
  [CSS styling](docs/CSS.md) have their own APIs and test owners
- [tests/](tests/), [examples/](examples/) and [features/](features/) are
  separate reactor areas declared in the [root POM](pom.xml)

SWT's native widget implementations are maintained in the separate
[SWT repository](https://github.com/hsoliwal/eclipse.platform.swt).
A workbench or JFace regression should first be reproduced in its UI test
bundle; a native-widget issue may need a smaller SWT reproduction.

The Oomph configuration below provisions the upstream Platform UI development
workspace. Verify the Git remote and branch before editing this fork.
Fork-specific changes belong in
[hsoliwal/eclipse.platform.ui pull requests](https://github.com/hsoliwal/eclipse.platform.ui/pulls);
upstream changes follow [CONTRIBUTING.md](CONTRIBUTING.md).

## Package and lifecycle documentation

The [architecture and coverage guide](docs/architecture/README.md) connects
[source-backed lifecycle contracts](docs/architecture/lifecycle-contracts.md),
[project/module ownership](docs/architecture/module-ledger.md),
[Java package and source-set coverage](docs/architecture/package-ledger.md), and
[export boundaries](docs/architecture/export-boundaries.md).
It pins the inspected source revision and keeps unreviewed semantics, generated
or native boundaries, and unrun verification visible. Inventory coverage is
reported separately from complete package/API documentation.

## How to Contribute

Contributions are most welcome. There are many ways to contribute, from entering high quality bug reports, to contributing code or documentation changes.

For a complete guide, see the [CONTRIBUTING](https://github.com/eclipse-platform/.github/blob/main/CONTRIBUTING.md) page.

[![Create Eclipse Development Environment for Eclipse Platform UI](https://download.eclipse.org/oomph/www/setups/svg/Eclipse_Platform_UI.svg)](
https://www.eclipse.org/setups/installer/?url=https://raw.githubusercontent.com/eclipse-platform/eclipse.platform.ui/master/releng/org.eclipse.ui.releng/platformUIConfiguration.setup&show=true
"Click to open Eclipse-Installer Auto Launch or drag into your running installer")


## Test Dependencies

Several test plug-ins have a dependency to the Mockito and Hamcrest libraries.
Please install them by installing "Eclipse Test Framework" from the [current release stream p2 repo](https://download.eclipse.org/eclipse/updates/I-builds/).


## How to Build on the Command Line

Use Maven 3.9.x and the JDK selected by the checked-in [Jenkinsfile](Jenkinsfile),
currently JDK 25, to reproduce this revision's CI. From the repository root:

```
mvn clean verify
```


### Focused UI tests

Tycho tests need the `verify` lifecycle; `mvn test` alone is not an
equivalent test run. For example, to build JFace and its test bundle together:

```sh
mvn clean verify -pl :org.eclipse.jface,:org.eclipse.jface.tests
```

The source and tests are in [bundles/org.eclipse.jface/](bundles/org.eclipse.jface/)
and [tests/org.eclipse.jface.tests/](tests/org.eclipse.jface.tests/).
Include the changed production bundle when selecting tests so the run exercises
your source changes, rather than only a bundle resolved from the target platform.
A focused run does not cover other workbench, text or data-binding test bundles.

The [root POM](pom.xml) enables the UI harness and UI thread by default.
Provide a working native SWT/display environment; Jenkins uses Xvnc on Linux.
[.mvn/maven.config](.mvn/maven.config) enables the individual-bundle profile,
eager target resolution and `tycho.localArtifacts=ignore`. The parent and
target dependencies must be reachable in the configured Eclipse repositories;
a plain Java classpath is not a substitute for the PDE target platform.

Test reports are written under `**/target/surefire-reports/TEST-*.xml`.
When reproducing the Jenkins lane, also inspect its API analysis and compiler
reports: that lane uses `maven.test.failure.ignore=true`, so a successful
Maven process alone does not prove passing tests. Report the exact command,
JDK, OS/display backend and any skipped or unavailable UI coverage.

## Issue Tracking

This project uses GitHub to track ongoing development and issues. In case you have an issue, please read the information about Eclipse being a [community project](https://github.com/eclipse-platform#community) and bear in mind that this project is almost entirely developed by volunteers. So the contributors may not be able to look into every reported issue. You will also find the information about [how to find and report issues](https://github.com/eclipse-platform#reporting-issues) in repositories of the `eclipse-platform` organization there. Be sure to search for existing issues before you create another one.

In case you want to report an issue that is specific to this `eclipse.platform.ui` repository, you can [find existing issues](https://github.com/eclipse-platform/eclipse.platform.ui/issues) or [create new issues](https://github.com/eclipse-platform/eclipse.platform.ui/issues/new) within this repository.


## Contact

Contact the project developers via the project's "dev" list.

- <https://accounts.eclipse.org/mailing-list/platform-dev>


## License

[Eclipse Public License (EPL) 2.0](https://www.eclipse.org/legal/epl-2.0/)

