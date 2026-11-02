# Platform UI package and lifecycle contracts

Inspected source revision: `b8874e6b63390d65e47612574686cb8cf97b1fe8` (14 October 2026).
This guide documents selected current contracts from declarations and method bodies. It is not a claim that every package or API leaf has been reviewed. See the [package ledger](package-ledger.md) for source-set accounting and the [coverage ledger](README.md#coverage-and-open-gates) for remaining work.

## Dependency and ownership boundaries

[The JFace manifest](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/META-INF/MANIFEST.MF) requires and reexports SWT and core commands, requires Equinox common, and declares additional optional dependencies. The binding framework separates core observable/model behavior from the SWT/JFace adaptation bundle. The legacy workbench is another owner: [its manifest](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ui.workbench/META-INF/MANIFEST.MF) requires JFace, data-binding, SWT, E4 model/services/DI/workbench/renderers and EMF. JFace does not obtain workbench ownership of arbitrary widgets merely because both are installed.

OSGi package exports and Java public declarations are different boundaries. The manifests identify internal/friend-only exports and split packages. An `Automatic-Module-Name` is not proof of a checked-in `module-info.java` contract. Consult the [module ledger](module-ledger.md) for every bundle's identity, source roots, imports, requirements, fragment host and export evidence.

## org.eclipse.jface.viewers: the viewer/provider lifecycle

`Viewer` exposes model input and selection through a UI control. `ContentViewer` adds content and label providers: content providers turn input into elements; label providers turn elements into presentation. Read [ContentViewer](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/ContentViewer.java) together with [IContentProvider](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/IContentProvider.java) when implementing ownership.

### Input changes

[setInput](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/ContentViewer.java#L264) requires a live underlying control and an installed content provider. It throws `IllegalStateException` for an absent/disposed control, and uses an assertion for the provider precondition. The sequence is:

1. Read the previous input
2. Notify `contentProvider.inputChanged(viewer, oldInput, newInput)`
3. Store the new input
4. Invoke the viewer's `inputChanged(newInput, oldInput)` hook

The provider callback therefore happens before the viewer's input field changes. Deregister listeners on the old model and register them on the new model in that callback. The provider interface warns against updating the viewer during `inputChanged`, because the same callback also occurs during teardown. A model notification arriving later must still respect the underlying SWT control's thread and lifetime.

### Provider replacement and disposal

[setContentProvider](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/ContentViewer.java#L242) rejects null. When there was an old provider, it notifies that provider of input removal, disposes it, notifies the replacement of the existing input and refreshes. Do not assume this operation is a harmless reassignment or use it as an ownership-neutral provider-sharing mechanism.

[setLabelProvider](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/ContentViewer.java#L295) has a same-instance no-op check. Otherwise it removes the viewer's listener from the old provider, registers on the replacement, refreshes, and only then disposes the old provider. That ordering keeps displayed items from referring to already-disposed images during refresh.

Subclasses must hook the established control and preserve the superclass cleanup. [handleDispose](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/ContentViewer.java#L165) first calls the content provider with new input null, then disposes it, detaches/disposes the label provider, and clears input. Exceptions from the input-change notification are logged, but this does not make every provider cleanup step exception-proof. The [default provider methods](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/IContentProvider.java) do nothing; implementing a provider that owns model listeners requires explicit cleanup.

Typical order is: create viewer/control, install the content provider, install the label provider, set input, and let control disposal trigger viewer cleanup. Sharing providers across viewers needs a deliberate shared lifetime because the viewer can dispose its provider on replacement or teardown.

Remaining viewer scope: concrete tree/table virtual-element loading, selection preservation, comparer/hash lookup, filters, sorters, editing support and deferred viewers need separate contract review. This guide does not infer them from ContentViewer.

## org.eclipse.jface.resource: reference counts and ownership transfer

[ResourceManager](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/resource/ResourceManager.java) converts descriptors into SWT resources; registries additionally map symbolic keys. Resources returned by a manager may be shared. Never call SWT `Resource.dispose()` directly on a managed result.

The [reference-count implementation](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/resource/AbstractResourceManager.java#L75) caches by descriptor equality. `create(descriptor)` increments a count or allocates the first resource; `destroy(descriptor)` decrements and calls deallocation when the final reference is released. An equal descriptor can identify the same managed resource even if it is not the same Java descriptor object.

Use one corresponding `destroy` for each `create`, or deliberately bind an allocation to a short-lived manager's lifetime. The `get(descriptor)` Javadoc describes lifetime-scoped usage, but the inspected body only returns `find(descriptor)` on a cache hit or calls `create(descriptor)` when absent. A cache hit adds no separate pin or reference count. With AbstractResourceManager, `create(d)`, `get(d)`, then `destroy(d)` can release the resource when that create count reaches zero. Mixing `get` with independently balanced `create`/`destroy` calls for an equal descriptor therefore does not establish an additional lifetime reference. Allocations left unreleased in the global manager can remain for the application lifetime. Managed results must still never be disposed directly through SWT.

[LocalResourceManager](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/resource/LocalResourceManager.java) delegates actual allocation/release to a parent manager. Its constructor taking a Control installs a dispose listener that closes the local manager when the control dies. Do not use that local manager for resources that must outlive the control. Disposing one local manager releases its references without asserting ownership of unrelated parent allocations.

Disposal callbacks should not throw: [ResourceManager.dispose](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/resource/ResourceManager.java#L316) notifies the recorded callbacks and rethrows a collected RuntimeException after notification. The subclass invokes that superclass method before freeing its map. This is not an unconditional cleanup guarantee in the face of failing callbacks.

[ResourceManagerTest](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests/org.eclipse.jface.tests/src/org/eclipse/jface/tests/images/ResourceManagerTest.java) contains descriptor reuse, local-manager isolation and resource-count assertions. Those test bodies were inspected; they were not run.

### ImageRegistry has its own ownership contract

[ImageRegistry](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/resource/ImageRegistry.java) owns images registered with it. Clients borrow results from `get(key)`; they must not dispose those results or register an image already managed by another owner.

- `get(null)` and an unknown key return null; a descriptor entry is materialized lazily
- `put(key, ImageDescriptor)` can replace an unmaterialized descriptor but rejects replacement after an image exists
- `put(key, Image)` rejects a key already holding an image or descriptor and asserts the image is non-null
- `remove(key)` releases the manager reference when a descriptor exists; `dispose()` releases materialized entries and cancels its manager-disposal callback
- The manager-based constructor registers cleanup with that manager; the Display constructor obtains the Display-associated JFace manager

The inspected [ImageRegistry test source](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests/org.eclipse.jface.tests/src/org/eclipse/jface/tests/images/ImageRegistryTest.java) is a verification route, not an executed receipt.

## org.eclipse.jface.window: shells and nested event loops

[Window](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/window/Window.java) initially has no shell and defaults to nonblocking `open()`. [create](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/window/Window.java#L424) creates the shell/content and initializes bounds. [open](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/window/Window.java#L778) creates or recreates the shell as needed, constrains its size, opens it, and enters a nested event loop only when `setBlockOnOpen(true)` was requested.

A blocking open continues dispatching UI events. It is not a freeze of application state: listeners and other UI work can run before the call returns. The loop routes thrown errors through the configured exception handler. Subclasses such as dialogs may choose different defaults; check their constructor instead of extrapolating Window's default.

[close](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface/src/org/eclipse/jface/window/Window.java#L309) removes its font-change listener and window-manager membership, disposes the shell rather than calling `Shell.close()` recursively, and clears its shell/content references after disposal. Overrides must preserve superclass close behavior. The method's return value and subclass veto behavior must be interpreted at the concrete window/dialog type.

Remaining scope: individual Dialog, WizardDialog, preference, progress and operation contracts, cancel/save semantics and concrete window tests are not established by this base-class review.

## org.eclipse.ui and org.eclipse.ui.part: workbench-managed parts

[IWorkbenchPart](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ui.workbench/eclipseui/org/eclipse/ui/IWorkbenchPart.java) defines the lifecycle: instantiate, establish a site, initialize, optionally create controls when visible, focus/activate, close, dispose the control hierarchy, then dispose the part. A part may be initialized and later disposed without `createPartControl` ever being called.

Consequences for implementations:

- Create controls under the supplied parent; do not claim ownership of the workbench Display or unrelated shells
- Do not assume controls exist in `dispose()`, or call widget methods after the workbench has disposed them
- Tie cleanup needing a live control to its dispose listener; remove non-widget/workbench listeners and release owned non-widget resources during teardown
- A title image returned by a part is owned by the part; callers must not dispose it
- Use workbench activation APIs rather than calling lifecycle methods as an application-controlled shortcut

[WorkbenchPart.dispose](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ui.workbench/eclipseui/org/eclipse/ui/part/WorkbenchPart.java#L104) releases its managed title-image descriptor when a current Display is available and clears its property/part listeners. Subclasses extending cleanup must retain this superclass work. The [part lifecycle test](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests/org.eclipse.ui.tests/Eclipse%20UI%20Tests/org/eclipse/ui/tests/api/IWorkbenchPartTest.java#L48) checks the order ending in `widgetDisposed`, then `dispose`; it is inspected source, not a passing run.

### ViewPart

[ViewPart](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ui.workbench/eclipseui/org/eclipse/ui/part/ViewPart.java) is a subclassing entry point for contributions to `org.eclipse.ui.views`. The base `init(site)` installs the site. Its `init(site, memento)` delegates to the one-argument init and ignores the memento; `saveState` is empty. Persistence/restoration must be implemented by a view that needs it. Implement `createPartControl` and `setFocus` and respect the control-before-part disposal ordering.

### EditorPart

[EditorPart](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.ui.workbench/eclipseui/org/eclipse/ui/part/EditorPart.java) supplies the base for `org.eclipse.ui.editors` contributions. Concrete editors must implement input/site validation in init, control creation/focus, dirty tracking, save, save-as and the save-as policy. The base `isSaveOnCloseNeeded()` delegates to `isDirty()`; it does not implement a persistence transaction.

`setInput(input)` rejects null and stores the value without firing the input property event. `setInputWithNotify(input)` stores it and fires `PROP_INPUT` in the inspected body. Finish updating dependent editor state before notification, since listeners can immediately call back into the editor. Do not assume a same-input equality guard from the method name or description.

Workbench closure may be stopped by save failure or cancellation according to the part contract. This documentation does not make hiding a part equivalent to disposing it, or assert complete E4/3.x compatibility lifecycle coverage.

## org.eclipse.core.databinding.observable: realms

[Realm](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding.observable/src/org/eclipse/core/databinding/observable/Realm.java) defines the access/notification context of observables. A realm can represent a UI thread or another synchronization policy. The default realm is a thread-local construction default, not necessarily a realm in which the current code is executing.

`Realm.runWithDefault(realm, runnable)` temporarily replaces that default and restores it in a finally block. It runs the runnable directly; it does not marshal execution onto the supplied realm. Use `isCurrent()` and the realm's execution methods for access rules. `exec` runs through the safe-run path immediately if current, otherwise schedules asynchronously. Realm's safe-run path logs/handles exceptions rather than presenting the same propagation contract as arbitrary direct invocation.

Do not hold locks around blocking cross-realm execution if the target may need those locks. The [Realm test](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/tests/org.eclipse.jface.tests.databinding/src/org/eclipse/core/tests/databinding/observable/RealmTest.java) asserts default restoration, but it does not qualify every custom realm or deadlock scenario.

## org.eclipse.jface.databinding.swt: DisplayRealm

[DisplayRealm](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.jface.databinding/src/org/eclipse/jface/databinding/swt/DisplayRealm.java) adapts a particular SWT Display to Realm. Its current-context test compares `Display.getCurrent()` with that display; asynchronous work is wrapped in Realm's safe-run handler and sent to `Display.asyncExec` when the display is not disposed. A pre-disposal check does not itself guarantee success against a concurrent shutdown. Manage producers and control disposal together.

This is the UI adapter boundary. It does not make every core observable a UI observable, and merely assigning a default DisplayRealm does not move background work onto the display thread.

## org.eclipse.core.databinding: context and binding ownership

[DataBindingContext](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding/src/org/eclipse/core/databinding/DataBindingContext.java) owns bindings and validation-status providers, with a non-null validation realm. The no-argument constructor uses `Realm.getDefault()`; code without a default must supply a realm rather than assume one exists.

[Binding.init](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding/src/org/eclipse/core/databinding/Binding.java#L61) rejects disposed model/target observables, registers disposal listeners on both, invokes its initialization hooks, and registers with the context in its validation realm. If an endpoint is disposed, cleanup is scheduled through that realm.

[DataBindingContext.dispose](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding/src/org/eclipse/core/databinding/DataBindingContext.java#L412) disposes a snapshot of its bindings, then remaining non-disposed validation-status providers. [Binding.dispose](https://github.com/hsoliwal/eclipse.platform.ui/blob/b8874e6b63390d65e47612574686cb8cf97b1fe8/bundles/org.eclipse.core.databinding/src/org/eclipse/core/databinding/Binding.java#L134) removes itself from the context, unregisters endpoint disposal listeners and clears its endpoint references; the base method does not call `dispose()` on the endpoint observables. Therefore disposing a binding or context is not proof that every observable created by application code has been released. Keep endpoint ownership explicit.

The update methods represent target-to-model/model-to-target propagation; Binding's contract does not promise the update has finished by return. Conversion, validation policies, list/set/map synchronization, master-detail chains and concrete observables require separate source/test review.

## Build/test routes and unresolved coverage

Use the [repository build guide](../../README.md#how-to-build-on-the-command-line). This revision's root POM enables the UI harness and UI-thread tests; the Jenkins lane selects JDK 25 and Xvnc. A BREE of JavaSE-21 in an individual manifest is not the same claim as the CI JDK.

For JFace changes the documented focused route is:

```sh
mvn clean verify -pl :org.eclipse.jface,:org.eclipse.jface.tests
```

Include the changed production bundle alongside its test owner. `mvn test` alone does not cover the Tycho integration-test lifecycle. The main Jenkins command tolerates Maven test failures, so inspect `**/target/surefire-reports/TEST-*.xml`, compiler and API-analysis reports rather than only the process exit code.

Not run for this documentation: Maven/Tycho compilation/tests, Javadoc/API checks, native/UI execution and runtime/performance qualification. Remaining substantive contracts include core commands/undo, filebuffers, text/editor infrastructure, E4 model/renderer/DI/CSS, IDE/navigator/forms/search/refactoring, examples, fixtures and internal packages. These all remain in the ledgers; no ordinary component is excluded to raise a coverage percentage.

See [LICENSE](../../LICENSE), [NOTICE](../../NOTICE) and [CONTRIBUTING.md](../../CONTRIBUTING.md) for the existing project terms and contribution route.

