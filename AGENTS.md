# AGENTS.md

## Clean output rule (owner directive, 2026-11-14) - read first

This rule overrides any older text in this file.

- This fork is clean output: one coherent Eclipse Platform UI repository. M3 work lives
  inside the project's own classes and packages, written the way the project
  writes its code. It is not stamped across folders.
- Do not add `m3/`, `.m3/`, `docs/m3/` or `tools/m3*` folders.
- Do not add any of the following. They belong in `hsoliwal/com.synexia`, the
  convergence workspace:
  - qualification bundles;
  - evidence, receipts and benchmark tables;
  - work orders;
  - donor copies;
  - recipe sources or recipe resources.
- Do not add parallel `M3`-prefixed types or `*.m3` packages where an existing
  class owns the behaviour. Change that class in place, keep it internal unless
  the owner admits new API, and follow the project's naming.
- A pull request here contains only the product change, plus its tests in the
  project's own test layout. Put the proof material in the paired `com.synexia`
  pull request and link to it.
- This fork's merged pull-request history shows the intended direction.



This file provides guidance to AI Agents.


## Priority 0 — repository-wide full-history additive superset merge rule (2026-11-18)

For every merge, preserve the historically accepted **backward-compatible optimized superset**: public API/ABI, observable behavior, cross-platform capability, native/ownership contracts, tests, bug fixes, proven algorithms and performance. Evaluate all incumbent, donor, branch and PR candidates, select the best *validated* design, allow additive API extensions, and prohibit silent regression or lost code. Inspect ancestry to the **earliest reachable commits**, relevant heads/tags, merged/closed/reverted PRs, branch/merge bases, both merge parents, renames, deletions and abandoned optimizations; never rebase `develop`. Before accepting a merge, checkpoint per ref the last checked date, exact SHA, history/PR/branch coverage, earliest ancestry reached, last verified anchor and detected losses; never label a HEAD-only sample as full-history PASS. Use existing Synexia history-loss and Maven/OpenRewrite recipe gates, with clean postimage-only public receiving repos where applicable; do not add recipe files/evidence crates to product forks. Prefer first-party Apache-2.0 and compatible Apache/permissive donors, but preserve upstream original licenses, notices and attribution: algorithm ideas can be reimplemented independently; LLM rewriting does not erase copyright or derivative-license obligations. Canonical policy and explicitly **PENDING** 236-repo registry: [com.synexia PR #18764](https://github.com/hsoliwal/com.synexia/pull/18764).

## Repository Overview

Eclipse Platform UI provides the building blocks for Eclipse IDE and Eclipse Rich Client Platform (RCP). 
This includes JFace, workbench, commands framework, data binding, dialogs, editors, views, perspectives, and more. Built on top of SWT (Eclipse Standard Widget Toolkit).

**Key Facts:**
- **Language:** Java 21
- **Build System:** Maven 3.9.12 with Tycho (OSGi/Eclipse plugin build)
- **Architecture:** OSGi bundles, E4 application model

## Project Structure

```
eclipse.platform.ui/
├── bundles/          # OSGi bundles (production code)
│   ├── org.eclipse.ui.workbench          # Main workbench implementation
│   ├── org.eclipse.jface                 # JFace toolkit (viewers, dialogs, etc.)
│   ├── org.eclipse.jface.databinding     # Data binding framework
│   ├── org.eclipse.jface.text            # Text editing framework
│   ├── org.eclipse.core.commands         # Commands framework
│   ├── org.eclipse.core.databinding*     # Core data binding
│   ├── org.eclipse.e4.ui.*               # E4 workbench, CSS, DI, model
│   └── org.eclipse.ui.*                  # UI components (IDE, editors, views, etc.)
├── tests/            # Test bundles (mirror structure of bundles/)
├── examples/         # Example bundles
├── features/         # Eclipse feature definitions
├── releng/           # Release engineering artifacts
├── docs/             # Documentation (JFace, RCP, Commands, etc.)
└── .github/          # GitHub workflows and CI configuration
```

### Key Architectural Components

**E4 Platform (Modern):**
- `org.eclipse.e4.ui.model.workbench` - E4 application model
- `org.eclipse.e4.ui.workbench*` - E4 workbench implementation
- `org.eclipse.e4.ui.di` - Dependency injection
- `org.eclipse.e4.ui.css.*` - CSS styling engine
- `org.eclipse.e4.core.commands` - Command framework

**JFace Toolkit:**
- `org.eclipse.jface` - Viewers, dialogs, resources, actions
- `org.eclipse.jface.databinding` - Data binding for UI
- `org.eclipse.jface.text` - Text editing infrastructure

**Legacy Workbench (3.x compatibility):**
- `org.eclipse.ui.workbench` - Workbench implementation
- `org.eclipse.ui.ide` - IDE-specific components
- `org.eclipse.ui.editors` - Editor framework

### OSGi Bundle Structure

Each bundle contains:
- `META-INF/MANIFEST.MF` - Bundle metadata and dependencies
- `build.properties` - Build configuration (what to include in binary)
- `plugin.xml` - Extension point declarations and contributions (optional)
- `src/` or `eclipseui/` - Java source code
- `.settings/` - Eclipse compiler settings


## Build System

### Building Individual Bundles

```bash
# Compile single bundle such as org.eclipse.jface
mvn clean compile -pl :org.eclipse.jface
```

### Test Properties

From `pom.xml`:
- `tycho.surefire.useUIHarness=true` - Use Eclipse UI test harness
- `tycho.surefire.useUIThread=true` - Run tests on UI thread
- `failOnJavadocErrors=true` - Fail build on Javadoc errors

### Running Tests

**⚠️ IMPORTANT:** Use `mvn verify` (NOT `mvn test`) for Tycho projects. 
Due to Maven Tycho lifecycle binding, tests run in the `integration-test` phase, not the `test` phase. Running `mvn test` will NOT execute tests.

```bash
# Run tests for a specific test bundle from repository root
mvn clean verify -pl :org.eclipse.jface.tests

# Run a specific test class within a bundle
mvn clean verify -pl :org.eclipse.jface.tests -Dtest=ViewerTest
```

If classes under test have been modified, include both the changed source bundle and the test bundle in `-pl`:
```bash
mvn clean verify -pl :org.eclipse.jface,:org.eclipse.jface.tests
```


## Development Hints

### Finding Code

```bash
# Find test files for a bundle
ls tests/org.eclipse.jface.tests/src

# Find bundle MANIFEST
cat bundles/org.eclipse.jface/META-INF/MANIFEST.MF

# Search for specific code pattern
grep -r "pattern" bundles/org.eclipse.jface/src
```

**Finding test bundles:** Test bundles mirror production bundles:
- Production: `bundles/org.eclipse.jface`
- Tests: `tests/org.eclipse.jface.tests`

### JUnit Guidelines

- Prefer JUnit 5 (`org.junit.jupiter.api.*`) for new tests


## Critical Development Rules

### 1. OSGi Dependencies

**Always check `META-INF/MANIFEST.MF` before adding imports.** If a package is not in `Import-Package` or `Require-Bundle`, the import will fail at runtime.

```
Require-Bundle: org.eclipse.core.runtime,
 org.eclipse.swt,
 org.eclipse.jface
Import-Package: org.osgi.service.event
```

### 2. SWT Resource Disposal

**Must dispose SWT resources** (except colors and system fonts):

### 3. UI Thread Requirements

**All SWT/JFace UI code must run on the Display thread:**

```java
// Run asynchronously on UI thread
Display.getDefault().asyncExec(() -> {
    label.setText("Updated");
});

// Check if on UI thread
if (Display.getCurrent() != null) {
    // Already on UI thread
} else {
    // Need to use asyncExec/syncExec
}
```

### 4. API Compatibility

**Do not break API compatibility.** The build includes API tools that verify:
- No removal of public API
- No changes to method signatures
- No changes to class hierarchies

Breaking changes will fail CI with errors in `**/target/apianalysis/*.xml`.

### 5. Update build.properties

When adding new files or packages, update `build.properties`:

```properties
# Include in binary build
bin.includes = plugin.xml,\
               META-INF/,\
               .,\
               icons/

# Source folders
source.. = src/

# Output folder
output.. = bin/
```


## CI/GitHub Workflows

- CI definition: `.github/workflows/ci.yml` (runs full aggregator build)
- PRs are gated by: compiler checks, API compatibility, Javadoc, and unit/UI tests
- UI tests run with the Eclipse UI harness in headless mode

## Troubleshooting

### "Package does not exist"
Check `META-INF/MANIFEST.MF` - add missing package to `Import-Package` or bundle to `Require-Bundle`.

### "API baseline errors"
You've broken API compatibility. Revert the breaking change or mark it appropriately.

### Test hangs
UI tests must run on Display thread. Use `Display.asyncExec()` or ensure `useUIHarness=true`.

### "Widget is disposed"
Attempting to access disposed SWT widget. Check disposal order and lifecycle.

## Documentation

Key docs in `docs/` directory:
- `JFace.md` - JFace framework overview
- `JFaceDataBinding.md` - Data binding guide
- `Eclipse4_RCP_FAQ.md` - E4 RCP frequently asked questions
- `PlatformCommandFramework.md` - Command framework
- `CSS.md` - CSS styling for E4

External links:
- [Platform UI Wiki](https://wiki.eclipse.org/Platform_UI)
- [Contributing Guide](https://github.com/eclipse-platform/.github/blob/main/CONTRIBUTING.md)
- [Eclipse Platform Project](https://projects.eclipse.org/projects/eclipse.platform)

## Additive historical-convergence owner directive (2026-11-18)

Before a merge, loss repair or API enhancement, survey all accessible commits to history roots, all branches/tags, and paginated accepted/rejected/closed/merged PRs and head/merge refs. Compare actual pre/post class/member/atom source and tests: a merged PR or ancestry alone is not delivered code. Identify and restore lost working APIs, behavior and measured optimizations; never erase history to make a bad commit disappear.

Choose the strongest *compatible* correct algorithms and atoms from every qualifying revision; the outcome is an additive, backward-compatible superset. Preserve source/binary/reflective behavior, callbacks, concurrency, native lifetime, resource budgets and regression tests for this toolkit. All reusable atomization/patternization and Java/Maven/OpenRewrite recipes remain canonically in `hsoliwal/com.synexia`; this product fork receives only legitimate code/test postimages.

Prefer Apache-2.0 for separately original eligible contributions; preserve original upstream and per-file licenses (EPL/GPL/Apache/other), NOTICE, provenance and license exceptions. Algorithmic ideas may be independently reimplemented from lawful specifications, but LLM rewriting copyrighted code does not waive its rights or relevant patents.

Central last-checked checkpoint: `hsoliwal/com.synexia/docs/M3-SCALE/invariants/SYNEXIA-HISTORY-REVIEW-LAST-CHECKED.tsv`. Record exact checked SHA/date and root/ref/PR coverage; label unverified/lost-source reviews PARTIAL. Do not store history audit bundles in this clean product repository.
