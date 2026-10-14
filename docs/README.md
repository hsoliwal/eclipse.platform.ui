# Platform UI: find the layer, trace the behavior, validate the change

Platform UI combines JFace, the workbench and Eclipse 4 UI services on top of SWT.
This guide is a source-oriented entrance to the existing documentation, not a
replacement for it. See the [README](../README.md) for project identity, upstream
contribution links and the downstream fork's experimental direction.

## Architecture and documentation map

| Layer | Source location | Deeper guide |
| --- | --- | --- |
| Viewers, dialogs, actions and resources | [JFace bundle](../bundles/org.eclipse.jface) | [JFace](JFace.md), [snippets](JFaceSnippets.md) |
| Observable models and UI binding | [bundles](../bundles), org.eclipse.core.databinding* and org.eclipse.jface.databinding | [Data binding](JFaceDataBinding.md) |
| Commands and handlers | [bundles](../bundles), org.eclipse.core.commands and workbench integration | [Command framework](PlatformCommandFramework.md) |
| Workbench compatibility layer | [org.eclipse.ui.workbench](../bundles/org.eclipse.ui.workbench) | [RCP](Rich_Client_Platform.md) |
| Eclipse 4 model and services | [bundles](../bundles), org.eclipse.e4.ui.* | [Model](Eclipse4_Model.md), [dependency injection](Eclipse4_RCP_Dependency_Injection.md) |
| Styling | [bundles](../bundles), org.eclipse.e4.ui.css.* | [CSS](CSS.md) |

Use a bundle's MANIFEST.MF to distinguish exported API from internal implementation
and discover OSGi dependencies. SWT owns native widgets; JFace layers viewer and
presentation behavior over them. Workbench integration is another boundary.
Fixing a viewer behavior does not by itself validate a complete workbench workflow.

## Set up for this checkout

The current [Jenkinsfile](../Jenkinsfile) uses JDK 25 and a virtual display.
Older notes saying Java 21 are not the current CI tool selection.
[Contributor guidance](../AGENTS.md) documents Maven 3.9.12.
[extensions.xml](../.mvn/extensions.xml) selects Tycho 5.0.2.

The [root POM](../pom.xml) inherits eclipse-platform-parent 4.42.0-SNAPSHOT and
defaults tests to the UI harness and UI thread.
[maven.config](../.mvn/maven.config) enables individual-bundle builds, eager target
resolution and `tycho.localArtifacts=ignore`. Include changed production bundles
in your selected reactor; do not assume a previously installed local artifact is
the implementation under test. Parent POM and p2 target downloads need network
access. UI tests require a working display and suitable SWT native libraries.

## Quick commands

These examples are checked against the source/build instructions, but were not
executed for this documentation change. Run from the repository root.

```sh
mvn -version
git status --short
# Compile JFace only; this does not run the test suite.
mvn clean compile -pl :org.eclipse.jface
# Test the selected source and test bundle together.
mvn verify -pl :org.eclipse.jface,:org.eclipse.jface.tests
# Full repository validation, once display and toolchains are configured.
mvn clean verify -Pbree-libs -Papi-check -Pjavadoc
```

Tycho tests run later than Maven's `test` phase; use `verify`.
The JFace tests are a pomless bundle: consult their
[manifest](../tests/org.eclipse.jface.tests/META-INF/MANIFEST.MF) and
[build properties](../tests/org.eclipse.jface.tests/build.properties).
Reports live under participating modules' `target/surefire-reports`; API and
compiler diagnostics are under `target/apianalysis` and `target/compilelogs`.
CI allows test failures while collecting reports, so inspect results rather than
equating a completed job with passed tests.

## Worked exploration: refresh without losing selection

1. Start in [StructuredViewer.java](../bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/StructuredViewer.java).
   Locate `refresh(Object)`, `preservingSelection` and `internalRefresh`.
   The public refresh path wraps the subclass's refresh in selection preservation.
2. Trace how the viewer maps model elements to widgets. Read the contracts around
   `setUseHashlookup` and `setComparer` before changing equality or lookup behavior.
   A faster map is not useful if equal-but-distinct elements resolve incorrectly.
3. Browse the [viewer tests](../tests/org.eclipse.jface.tests/src/org/eclipse/jface/tests/viewers).
   `StructuredViewerElementMapTest`, `StructuredViewerTest` and
   `MultipleEqualElementsTreeViewerTest` provide relevant starting points.
4. Design a small model update that keeps the selected element, and another that
   removes it. Record selection/listener behavior before and after refresh.
   Preserve UI-thread access and resource ownership; avoid treating a disposed
   widget check as a general synchronization strategy.
5. Run the relevant bundle tests with the changed JFace source in the reactor.
   Then exercise the affected workbench context if the change reaches it.

This trace is a reading exercise, not a claim that those cases have passed.
Performance evidence should separately record update size, repeated refreshes,
latency, allocations and retained resources. Compilation, screenshots and a
microbenchmark each answer different questions; none substitutes for behavioral
and platform-specific checks.

Continue with [event processing](Event_Processing.md), [accessibility](Accessibility_Features.md)
and the existing topic guides above. Preserve upstream notices and use
[CONTRIBUTING](../CONTRIBUTING.md) for contribution requirements.
