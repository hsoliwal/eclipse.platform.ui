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

You need Maven 3.9.x installed. After this you can run the build via the following command:

```
mvn clean verify
```


## Issue Tracking

This project uses GitHub to track ongoing development and issues. In case you have an issue, please read the information about Eclipse being a [community project](https://github.com/eclipse-platform#community) and bear in mind that this project is almost entirely developed by volunteers. So the contributors may not be able to look into every reported issue. You will also find the information about [how to find and report issues](https://github.com/eclipse-platform#reporting-issues) in repositories of the `eclipse-platform` organization there. Be sure to search for existing issues before you create another one.

In case you want to report an issue that is specific to this `eclipse.platform.ui` repository, you can [find existing issues](https://github.com/eclipse-platform/eclipse.platform.ui/issues) or [create new issues](https://github.com/eclipse-platform/eclipse.platform.ui/issues/new) within this repository.


## Contact

Contact the project developers via the project's "dev" list.

- <https://accounts.eclipse.org/mailing-list/platform-dev>


## License

[Eclipse Public License (EPL) 2.0](https://www.eclipse.org/legal/epl-2.0/)
