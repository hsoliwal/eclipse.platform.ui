/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.jface.tests.viewers.m3;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckable;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.m3.M3ViewerDelta;
import org.eclipse.jface.viewers.m3.M3Viewers;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@link M3Viewers} against tree, table and list viewers: every delta leaves the
 * widget in the state a model-driven refresh would produce. Runs on a display
 * (hidden desktop).
 */
public class M3ViewersTest {

	private Display display;
	private Shell shell;
	private final List<String> roots = new ArrayList<>();

	@BeforeEach
	public void setUp() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(300, 300);
		shell.open();
		for (int i = 0; i < 5; i++) {
			roots.add("r" + i);
		}
	}

	@AfterEach
	public void tearDown() {
		shell.dispose();
		drain();
	}

	private void drain() {
		while (display.readAndDispatch()) {
			// pending events
		}
	}

	private void waitUntil(BooleanSupplier done) {
		assertTrue(dispatchUntil(done), "timed out");
	}

	private boolean dispatchUntil(BooleanSupplier done) {
		boolean[] timedOut = { false };
		Runnable timeout = () -> timedOut[0] = true;
		display.timerExec(10_000, timeout);
		try {
			while (!done.getAsBoolean() && !timedOut[0]) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			return done.getAsBoolean();
		} finally {
			display.timerExec(-1, timeout);
		}
	}

	private TreeViewer tree() {
		TreeViewer v = new TreeViewer(shell, SWT.MULTI);
		v.setLabelProvider(new LabelProvider());
		v.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object input) {
				return roots.toArray();
			}

			@Override
			public Object[] getChildren(Object parent) {
				return new Object[0];
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
		});
		v.setInput(roots);
		return v;
	}

	private TableViewer table() {
		TableViewer v = new TableViewer(shell, SWT.MULTI);
		v.setLabelProvider(new LabelProvider());
		v.setContentProvider(ArrayContentProvider.getInstance());
		v.setInput(roots);
		return v;
	}

	private final class RefreshCountingTableViewer extends TableViewer {
		int refreshCount;
		final List<String> calls = new ArrayList<>();

		RefreshCountingTableViewer() {
			super(shell, SWT.MULTI);
			setLabelProvider(new LabelProvider());
			setContentProvider(ArrayContentProvider.getInstance());
			setInput(roots);
			refreshCount = 0;
			calls.clear();
		}

		@Override
		public void refresh() {
			refreshCount++;
			calls.add("refresh"); //$NON-NLS-1$
			super.refresh();
		}

		@Override
		public void update(Object[] elements, String[] properties) {
			calls.add("update"); //$NON-NLS-1$
			super.update(elements, properties);
		}
	}

	private ListViewer list() {
		ListViewer v = new ListViewer(shell, SWT.MULTI);
		v.setLabelProvider(new LabelProvider());
		v.setContentProvider(ArrayContentProvider.getInstance());
		v.setInput(roots);
		return v;
	}

	private static String texts(StructuredViewer v) {
		Control c = v.getControl();
		StringBuilder b = new StringBuilder();
		Item[] items = c instanceof Tree t ? t.getItems() : c instanceof Table t ? t.getItems() : null;
		if (items != null) {
			for (Item item : items) {
				b.append(item.getText()).append(' ');
			}
		} else {
			String[] labels = c instanceof Combo combo ? combo.getItems()
					: c instanceof CCombo combo ? combo.getItems() : ((org.eclipse.swt.widgets.List) c).getItems();
			for (String s : labels) {
				b.append(s).append(' ');
			}
		}
		return b.toString().trim();
	}

	/** Applies the same model change to the model and, as a delta, to the viewer. */
	private void exercise(StructuredViewer v) {
		roots.add(2, "x");
		roots.add("y");
		roots.add("z");
		roots.remove("r0");
		roots.remove("r4");
		M3ViewerDelta d = new M3ViewerDelta().add(roots, "x", 2).add(roots, "y", -1).add(roots, "z", -1).remove("r0")
				.remove("r4").update("r1", null).update("r2", null).update("x", new String[] { "p" });
		M3Viewers.apply(v, d);
		assertEquals(String.join(" ", roots), texts(v));
		M3Viewers.apply(v, new M3ViewerDelta().refresh("r1").refresh(null));
		assertEquals(String.join(" ", roots), texts(v));
	}

	@Test
	public void appliesToTreeTableAndList() {
		exercise(tree());
		roots.clear();
		setUpRoots();
		exercise(table());
		roots.clear();
		setUpRoots();
		exercise(list());
	}

	private void setUpRoots() {
		for (int i = 0; i < 5; i++) {
			roots.add("r" + i);
		}
	}

	@Test
	public void nonHashedTreeSearchKeepsFirstPreorderMatch() {
		class SearchableTreeViewer extends TreeViewer {
			SearchableTreeViewer() {
				super(shell, SWT.MULTI);
			}

			Widget lookup(Object element) {
				return doFindItem(element);
			}
		}
		SearchableTreeViewer v = new SearchableTreeViewer();
		v.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object input) {
				return roots.toArray();
			}

			@Override
			public Object[] getChildren(Object parent) {
				return new Object[0];
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return false;
			}
		});
		v.setLabelProvider(new LabelProvider());
		v.setInput(roots);
		TreeItem parent = v.getTree().getItem(0);
		TreeItem first = new TreeItem(parent, SWT.NONE);
		first.setData("needle"); //$NON-NLS-1$
		TreeItem second = new TreeItem(parent, SWT.NONE);
		second.setData("needle"); //$NON-NLS-1$
		assertSame(first, v.lookup("needle")); //$NON-NLS-1$
		assertNull(v.lookup("missing")); //$NON-NLS-1$
	}

	@Test
	public void largeDeltasRunWithRedrawOff() {
		TableViewer v = table();
		M3ViewerDelta d = new M3ViewerDelta();
		for (int i = 0; i < 100; i++) {
			roots.add("n" + i);
			d.add(roots, "n" + i, -1);
		}
		M3Viewers.apply(v, d);
		assertEquals(105, v.getTable().getItemCount());
	}

	@Test
	public void owningDisplayAcceptsWorkerPostsOnItsUiThread() throws Exception {
		TableViewer viewer = table();
		roots.add("worker");
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread worker = new Thread(() -> {
			try {
				M3Viewers.post(display, viewer, new M3ViewerDelta().add(roots, "worker", -1), 0);
			} catch (Throwable error) {
				failure.set(error);
			}
		});
		worker.start();
		worker.join();
		assertNull(failure.get(), "post may not inspect SWT controls off-thread");
		waitUntil(() -> viewer.getTable().getItemCount() == 6);
		assertEquals("r0 r1 r2 r3 r4 worker", texts(viewer));
	}

	@Test
	public void workerFlushReturnsBeforeSwtControlAccess() throws Exception {
		TableViewer viewer = table();
		roots.add("later");
		M3Viewers.post(display, viewer, new M3ViewerDelta().add(roots, "later", -1), 60_000);
		drain();
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread worker = new Thread(() -> {
			try {
				M3Viewers.flush(viewer);
			} catch (Throwable error) {
				failure.set(error);
			}
		});
		worker.start();
		worker.join();
		assertNull(failure.get(), "flush may not inspect SWT controls off-thread");
		assertEquals(5, viewer.getTable().getItemCount());
		M3Viewers.flush(viewer);
		assertEquals(6, viewer.getTable().getItemCount());
	}

	@Test
	public void postMergesABurstAndAppliesOnce() throws Exception {
		TableViewer v = table();
		Thread poster = new Thread(() -> {
			for (int i = 0; i < 3; i++) {
				String e = "p" + i;
				synchronized (roots) {
					roots.add(e);
				}
				M3Viewers.post(v, new M3ViewerDelta().add(roots, e, -1), 50);
			}
		});
		poster.start();
		poster.join();
		waitUntil(() -> v.getTable().getItemCount() == 8);
		assertEquals("r0 r1 r2 r3 r4 p0 p1 p2", texts(v));
	}

	@Test
	public void explicitDisplayPostFromWorkerTouchesSwtOnlyOnUiThread() throws Exception {
		TableViewer v = table();
		roots.add("explicit"); //$NON-NLS-1$
		AtomicReference<Throwable> thrown = new AtomicReference<>();
		Thread worker = new Thread(() -> {
			try {
				M3Viewers.post(display, v, new M3ViewerDelta().add(roots, "explicit", -1), 0); //$NON-NLS-1$
			} catch (Throwable failure) {
				thrown.set(failure);
			}
		});
		worker.start();
		worker.join();
		assertNull(thrown.get(), "post must not inspect SWT controls off-thread"); //$NON-NLS-1$
		waitUntil(() -> v.getTable().getItemCount() == 6);
		assertEquals("r0 r1 r2 r3 r4 explicit", texts(v)); //$NON-NLS-1$
	}

	@Test
	public void refreshFlushesQueuedDeltasFirst() {
		TableViewer v = table();
		roots.add("q");
		M3Viewers.post(v, new M3ViewerDelta().add(roots, "q", -1), 60_000);
		drain();
		v.refresh();
		assertEquals("r0 r1 r2 r3 r4 q", texts(v));
		M3Viewers.flush(v);
	}

	@Test
	public void refreshLaterRefreshesOnce() {
		RefreshCountingTableViewer v = new RefreshCountingTableViewer();
		roots.add("later");
		M3Viewers.refreshLater(v, 10);
		M3Viewers.refreshLater(v, 10);
		waitUntil(() -> v.getTable().getItemCount() == 6);
		assertEquals("r0 r1 r2 r3 r4 later", texts(v));
		assertEquals(1, v.refreshCount);
	}

	@Test
	public void explicitRefreshPostsRetainCardinalityAcrossDebouncedRefreshLater() {
		RefreshCountingTableViewer v = new RefreshCountingTableViewer();
		M3Viewers.refreshLater(v, 60_000);
		M3Viewers.refreshLater(v, 60_000);
		M3Viewers.post(v, new M3ViewerDelta().refresh(null), 60_000);
		M3Viewers.post(v, new M3ViewerDelta().refresh(null), 60_000);
		M3Viewers.refreshLater(v, 60_000);
		drain();
		M3Viewers.flush(v);
		assertEquals(4, v.refreshCount,
				"only consecutive dedicated refreshLater calls may coalesce"); //$NON-NLS-1$
	}

	@Test
	public void refreshPostsKeepAnInterveningUpdate() {
		RefreshCountingTableViewer v = new RefreshCountingTableViewer();
		M3Viewers.refreshLater(v, 60_000);
		M3Viewers.post(v, new M3ViewerDelta().update("r0", null), 60_000); //$NON-NLS-1$
		M3Viewers.refreshLater(v, 60_000);
		drain();
		M3Viewers.flush(v);
		assertEquals(2, v.refreshCount);
		assertEquals(List.of("refresh", "update", "refresh"), v.calls); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals("r0 r1 r2 r3 r4", texts(v)); //$NON-NLS-1$
	}

	@Test
	public void applyKeepsExplicitRefreshOperations() {
		RefreshCountingTableViewer v = new RefreshCountingTableViewer();
		M3Viewers.apply(v, new M3ViewerDelta().refresh(null).refresh(null));
		assertEquals(2, v.refreshCount);
	}

	@Test
	public void staleTimerDoesNotDrainALaterBatch() throws ReflectiveOperationException {
		TableViewer v = table();
		roots.add("first"); //$NON-NLS-1$
		M3Viewers.post(v, new M3ViewerDelta().add(roots, "first", -1), 60_000); //$NON-NLS-1$
		drain();

		// Exercise the existing UI transition without dispatching its zero-delay
		// timer until both batches are installed. No sleep or clock race is needed.
		var enqueue = M3Viewers.class.getDeclaredMethod("enqueue", StructuredViewer.class, //$NON-NLS-1$
				M3ViewerDelta.class, int.class);
		enqueue.setAccessible(true);
		enqueue.invoke(null, v, new M3ViewerDelta().update("r0", null), 0); //$NON-NLS-1$
		M3Viewers.flush(v);
		assertEquals(6, v.getTable().getItemCount());

		roots.add("later"); //$NON-NLS-1$
		enqueue.invoke(null, v, new M3ViewerDelta().add(roots, "later", -1), 60_000); //$NON-NLS-1$
		drain();
		assertEquals(6, v.getTable().getItemCount(), "a timer from the flushed batch must not apply the later batch"); //$NON-NLS-1$
		M3Viewers.flush(v);
		assertEquals("r0 r1 r2 r3 r4 first later", texts(v)); //$NON-NLS-1$
	}

	@Test
	public void queuedRefreshCoversConcreteViewerFamilies() {
		List<StructuredViewer> viewers = List.of(new TableViewer(shell, SWT.MULTI),
				new TreeViewer(shell, SWT.MULTI), new ListViewer(shell, SWT.MULTI),
				new ComboViewer(shell, SWT.DROP_DOWN), new ComboViewer(new CCombo(shell, SWT.READ_ONLY)),
				new CheckboxTableViewer(new Table(shell, SWT.CHECK | SWT.MULTI)),
				new CheckboxTreeViewer(shell, SWT.MULTI), new TableViewer(shell, SWT.MULTI | SWT.VIRTUAL),
				new TreeViewer(shell, SWT.MULTI | SWT.VIRTUAL));
		for (StructuredViewer viewer : viewers) {
			int[] rootReads = { 0 };
			viewer.setLabelProvider(new LabelProvider());
			viewer.setContentProvider(new ITreeContentProvider() {
				@Override
				public Object[] getElements(Object input) {
					rootReads[0]++;
					return roots.toArray();
				}

				@Override
				public Object[] getChildren(Object parent) {
					return new Object[0];
				}

				@Override
				public Object getParent(Object element) {
					return null;
				}

				@Override
				public boolean hasChildren(Object element) {
					return false;
				}
			});
			viewer.setInput(roots);
			viewer.setSelection(new StructuredSelection("r2")); //$NON-NLS-1$
			if (viewer instanceof ICheckable checkable) {
				checkable.setChecked("r1", true); //$NON-NLS-1$
			}
			if (viewer instanceof CheckboxTableViewer checks) {
				checks.setGrayed("r1", true); //$NON-NLS-1$
			}
			if (viewer instanceof CheckboxTreeViewer checks) {
				checks.setGrayed("r1", true); //$NON-NLS-1$
			}
			drain();
			rootReads[0] = 0;
			M3Viewers.refreshLater(viewer, 60_000);
			M3Viewers.refreshLater(viewer, 60_000);
			drain();
			M3Viewers.flush(viewer);
			String family = viewer.getClass().getSimpleName() + "/" //$NON-NLS-1$
					+ viewer.getControl().getClass().getSimpleName() + ":" + viewer.getControl().getStyle(); //$NON-NLS-1$
			assertEquals(1, rootReads[0], family);
			assertEquals("r2", viewer.getStructuredSelection().getFirstElement(), family); //$NON-NLS-1$
			if (viewer instanceof ICheckable checkable) {
				assertTrue(checkable.getChecked("r1"), family); //$NON-NLS-1$
			}
			if (viewer instanceof CheckboxTableViewer checks) {
				assertTrue(checks.getGrayed("r1"), family); //$NON-NLS-1$
			}
			if (viewer instanceof CheckboxTreeViewer checks) {
				assertTrue(checks.getGrayed("r1"), family); //$NON-NLS-1$
			}
			viewer.getControl().dispose();
		}
	}

	@Test
	public void aFailingDeltaFallsBackToRefresh() {
		ListViewer v = list();
		M3Viewers.post(v, new M3ViewerDelta().add(roots, "bad", 99), 0);
		waitUntil(() -> v.getList().getItemCount() == 5);
		drain();
		assertEquals("r0 r1 r2 r3 r4", texts(v));
	}

	@Test
	public void disposedAndEmptyAreIgnored() {
		TableViewer v = table();
		M3Viewers.apply(v, new M3ViewerDelta());
		M3Viewers.post(v, new M3ViewerDelta(), 0);
		Widget control = v.getControl();
		control.dispose();
		M3Viewers.apply(v, new M3ViewerDelta().remove("r0"));
		M3Viewers.post(v, new M3ViewerDelta().remove("r0"), 0);
		M3Viewers.flush(v);
		drain();
		assertTrue(control.isDisposed());
	}

	@Test
	public void flushOffTheUiThreadDoesNothing() throws Exception {
		TableViewer v = table();
		roots.add("t");
		M3Viewers.post(v, new M3ViewerDelta().add(roots, "t", -1), 60_000);
		drain();
		Thread t = new Thread(() -> M3Viewers.flush(v));
		t.start();
		t.join();
		assertEquals(5, v.getTable().getItemCount());
		M3Viewers.flush(v);
		assertEquals(6, v.getTable().getItemCount());
	}
	@Test
	public void laterLongDelayCannotExtendFirstBatchDeadline() {
		TableViewer v = table();
		roots.add("early");
		roots.add("late");
		M3Viewers.post(v, new M3ViewerDelta().add(roots, "early", -1), 40);
		M3Viewers.post(v, new M3ViewerDelta().add(roots, "late", -1), 2_000);
		long started = System.nanoTime();
		waitUntil(() -> v.getTable().getItemCount() == 7);
		assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 1_500,
				"a later debounce must not extend the first batch's four-delay bound");
		assertEquals("r0 r1 r2 r3 r4 early late", texts(v));
	}

	@Test
	public void obsoleteTimerCannotDrainNewBatchAfterFlush() throws InterruptedException {
		TableViewer v = table();
		roots.add("first");
		M3Viewers.post(v, new M3ViewerDelta().add(roots, "first", -1), 40);
		drain();
		M3Viewers.flush(v);
		assertEquals(6, v.getTable().getItemCount());

		roots.add("second");
		M3Viewers.post(v, new M3ViewerDelta().add(roots, "second", -1), 5_000);
		drain();
		// Delay UI timer dispatch until the obsolete timer has elapsed.
		Thread.sleep(250);
		drain();
		assertEquals(6, v.getTable().getItemCount(),
				"the first batch's timer must not drain the second batch");
		M3Viewers.flush(v);
		assertEquals("r0 r1 r2 r3 r4 first second", texts(v));
	}
	private static final String[] FAMILIES = {
			"table", "checkbox-table", "tree", "checkbox-tree", "list", "combo", "ccombo" };

	private final class CountingProvider implements ITreeContentProvider {
		int reads;

		@Override
		public Object[] getElements(Object input) {
			reads++;
			return roots.toArray();
		}

		@Override
		public Object[] getChildren(Object parent) {
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	private StructuredViewer family(String name, ITreeContentProvider provider) {
		return family(name, provider, false);
	}

	private StructuredViewer family(String name, ITreeContentProvider provider, boolean hashLookup) {
		StructuredViewer viewer = switch (name) {
		case "table" -> new TableViewer(shell, SWT.MULTI);
		case "checkbox-table" -> CheckboxTableViewer.newCheckList(shell, SWT.MULTI);
		case "tree" -> new TreeViewer(shell, SWT.MULTI);
		case "checkbox-tree" -> new CheckboxTreeViewer(shell, SWT.MULTI);
		case "list" -> new ListViewer(shell, SWT.MULTI);
		case "combo" -> new ComboViewer(shell, SWT.READ_ONLY);
		case "ccombo" -> new ComboViewer(new CCombo(shell, SWT.READ_ONLY));
		default -> throw new IllegalArgumentException(name);
		};
		viewer.setUseHashlookup(hashLookup);
		viewer.setLabelProvider(new LabelProvider());
		viewer.setContentProvider(provider);
		viewer.setInput(roots);
		return viewer;
	}

	@Test
	public void allStructuredFamiliesPreserveRowsSelectionAndChecks() {
		for (String name : FAMILIES) {
			roots.clear();
			setUpRoots();
			StructuredViewer viewer = family(name, new CountingProvider());
			viewer.setSelection(new StructuredSelection("r2"));
			if (viewer instanceof CheckboxTableViewer table) {
				table.setChecked("r2", true);
				table.setGrayed("r2", true);
			} else if (viewer instanceof CheckboxTreeViewer tree) {
				tree.setChecked("r2", true);
				tree.setGrayed("r2", true);
			}
			exercise(viewer);
			assertEquals("r2", ((IStructuredSelection) viewer.getSelection()).getFirstElement(), name);
			if (viewer instanceof CheckboxTableViewer table) {
				assertTrue(table.getChecked("r2"), name);
				assertTrue(table.getGrayed("r2"), name);
			} else if (viewer instanceof CheckboxTreeViewer tree) {
				assertTrue(tree.getChecked("r2"), name);
				assertTrue(tree.getGrayed("r2"), name);
			}
			viewer.getControl().dispose();
		}
	}

	@Test
	public void refreshLaterCoalescesProviderReadsForEveryFamily() {
		for (String name : FAMILIES) {
			CountingProvider provider = new CountingProvider();
			StructuredViewer viewer = family(name, provider);
			int before = provider.reads;
			M3Viewers.refreshLater(viewer, 60_000);
			M3Viewers.refreshLater(viewer, 60_000);
			M3Viewers.refreshLater(viewer, 60_000);
			drain();
			M3Viewers.flush(viewer);
			assertEquals(before + 1, provider.reads, name);
			viewer.getControl().dispose();
		}
	}

	@Test
	public void explicitRefreshRecordsRetainTheirMultiplicity() {
		for (String name : FAMILIES) {
			CountingProvider provider = new CountingProvider();
			StructuredViewer viewer = family(name, provider);
			int before = provider.reads;
			M3Viewers.apply(viewer, new M3ViewerDelta().refresh(null).refresh(null));
			assertEquals(before + 2, provider.reads, name + " apply");
			M3Viewers.post(viewer, new M3ViewerDelta().refresh(null).refresh(null), 60_000);
			drain();
			M3Viewers.flush(viewer);
			assertEquals(before + 4, provider.reads, name + " post");
			viewer.getControl().dispose();
		}
	}

	@Test
	public void explicitDeltaSeparatesAdjacentRefreshLaterRuns() {
		CountingProvider provider = new CountingProvider();
		StructuredViewer viewer = family("table", provider);
		int before = provider.reads;
		M3Viewers.refreshLater(viewer, 60_000);
		M3Viewers.refreshLater(viewer, 60_000);
		M3Viewers.post(viewer, new M3ViewerDelta().update("r1", null), 60_000);
		M3Viewers.refreshLater(viewer, 60_000);
		M3Viewers.refreshLater(viewer, 60_000);
		drain();
		M3Viewers.flush(viewer);
		assertEquals(before + 2, provider.reads);
		assertEquals("r0 r1 r2 r3 r4", texts(viewer));
	}

	@Test
	public void anOldShortDelayCannotDrainANewLongDelayBurst() throws Exception {
		CountingProvider provider = new CountingProvider();
		StructuredViewer viewer = family("table", provider);
		M3Viewers.post(viewer, new M3ViewerDelta().update("r1", null), 20);
		drain();
		M3Viewers.flush(viewer);
		int before = provider.reads;
		M3Viewers.refreshLater(viewer, 30_000);
		drain();
		// Make an old timer overdue without giving it an earlier dispatch opportunity.
		Thread.sleep(120);
		drain();
		assertEquals(before, provider.reads, "an earlier batch must not drain this batch");
		M3Viewers.flush(viewer);
		assertEquals(before + 1, provider.reads);
	}

	@Test
	public void disposingAQueuedViewerPreventsLaterProviderCalls() throws Exception {
		CountingProvider provider = new CountingProvider();
		StructuredViewer viewer = family("table", provider);
		int before = provider.reads;
		M3Viewers.refreshLater(viewer, 20);
		drain();
		viewer.getControl().dispose();
		Thread.sleep(60);
		drain();
		assertEquals(before, provider.reads);
	}

	@Test
	public void zeroDelayPostsAreAsynchronousAndEventuallyDrain() {
		for (String name : FAMILIES) {
			CountingProvider provider = new CountingProvider();
			StructuredViewer viewer = family(name, provider);
			int before = provider.reads;
			M3Viewers.post(viewer, new M3ViewerDelta().refresh(null), 0);
			assertEquals(before, provider.reads, name + " must enqueue asynchronously");
			waitUntil(() -> provider.reads > before);
			assertEquals(before + 1, provider.reads, name);
			viewer.getControl().dispose();
		}
	}

	@Test
	public void theFirstDelayCapsABurstWhileLongDelayPostsContinue() {
		CountingProvider provider = new CountingProvider();
		StructuredViewer viewer = family("table", provider);
		int before = provider.reads;
		boolean[] posting = { true };
		Runnable poster = new Runnable() {
			@Override
			public void run() {
				if (posting[0]) {
					M3Viewers.refreshLater(viewer, 30_000);
					display.timerExec(20, this);
				}
			}
		};
		try {
			M3Viewers.refreshLater(viewer, 100);
			poster.run();
			// A 30-second quiet period cannot finish within the bounded event loop.
			waitUntil(() -> provider.reads > before);
			assertEquals(before + 1, provider.reads);
		} finally {
			posting[0] = false;
			display.timerExec(-1, poster);
			viewer.getControl().dispose();
		}
	}

	@Test
	public void reentrantDispatchAndFlushPreserveTheOuterBatchBeforeNewPosts() {
		List<Object> removals = new ArrayList<>();
		TableViewer viewer = new TableViewer(shell, SWT.MULTI) {
			@Override
			public void remove(Object[] elements) {
				for (Object element : elements) {
					removals.add(element);
				}
				super.remove(elements);
			}
		};
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		boolean[] armed = { false };
		boolean[] entered = { false };
		boolean[] nestedTimerRan = { false };
		boolean[] nestedLoopCompleted = { false };
		String[] rowsInsideCallback = { null };
		Runnable nestedTimer = () -> nestedTimerRan[0] = true;
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				if (armed[0] && !entered[0] && "r1".equals(element)) {
					entered[0] = true;
					roots.remove("r4");
					M3Viewers.post(viewer, new M3ViewerDelta().remove("r4"), 0);
					display.asyncExec(() -> {
						M3Viewers.flush(viewer);
						display.timerExec(0, nestedTimer);
					});
					try {
						nestedLoopCompleted[0] = dispatchUntil(() -> nestedTimerRan[0]);
						rowsInsideCallback[0] = texts(viewer);
					} finally {
						display.timerExec(-1, nestedTimer);
					}
				}
				return super.getText(element);
			}
		});
		viewer.setInput(roots);
		armed[0] = true;
		roots.remove("r0");
		M3Viewers.post(viewer, new M3ViewerDelta().update("r1", null).remove("r0"), 60_000);
		drain();
		M3Viewers.flush(viewer);
		assertTrue(entered[0], "the outer update must enter the label provider");
		assertTrue(nestedLoopCompleted[0], "the nested event loop must dispatch its timer");
		assertEquals("r0 r1 r2 r3 r4", rowsInsideCallback[0], "nested work must wait for the outer batch");
		assertEquals("r1 r2 r3 r4", texts(viewer), "the outer batch must finish first");
		assertEquals(List.of("r0"), removals);
		waitUntil(() -> viewer.getTable().getItemCount() == 3);
		assertEquals(List.of("r0", "r4"), removals, "each batch must apply its removal exactly once");
		assertEquals(String.join(" ", roots), texts(viewer));
	}

	private record SelectionEvents(List<ISelection> changed, List<ISelection> postChanged) {
	}

	private static SelectionEvents selectionEvents(StructuredViewer viewer) {
		SelectionEvents events = new SelectionEvents(new ArrayList<>(), new ArrayList<>());
		viewer.addSelectionChangedListener(event -> events.changed().add(event.getSelection()));
		viewer.addPostSelectionChangedListener(event -> events.postChanged().add(event.getSelection()));
		return events;
	}

	private static void assertSelectionEvents(SelectionEvents events, int count, String context) {
		assertEquals(count, events.changed().size(), context + " selection events");
		assertEquals(count, events.postChanged().size(), context + " post-selection events");
	}

	private static void setCheckboxState(StructuredViewer viewer, Object element, boolean checked, boolean grayed) {
		if (viewer instanceof CheckboxTableViewer table) {
			table.setChecked(element, checked);
			table.setGrayed(element, grayed);
		} else if (viewer instanceof CheckboxTreeViewer tree) {
			tree.setChecked(element, checked);
			tree.setGrayed(element, grayed);
		}
	}

	private static void assertCheckboxState(StructuredViewer viewer, Object element, boolean checked, boolean grayed,
			String context) {
		if (viewer instanceof CheckboxTableViewer table) {
			assertEquals(checked, table.getChecked(element), context + " checked");
			assertEquals(grayed, table.getGrayed(element), context + " grayed");
		} else if (viewer instanceof CheckboxTreeViewer tree) {
			assertEquals(checked, tree.getChecked(element), context + " checked");
			assertEquals(grayed, tree.getGrayed(element), context + " grayed");
		}
	}

	private static void setCheckStateProvider(StructuredViewer viewer, ICheckStateProvider provider) {
		if (viewer instanceof CheckboxTableViewer table) {
			table.setCheckStateProvider(provider);
		} else if (viewer instanceof CheckboxTreeViewer tree) {
			tree.setCheckStateProvider(provider);
		}
	}

	private static IElementComparer failingComparer(RuntimeException failure) {
		return new IElementComparer() {
			@Override
			public boolean equals(Object a, Object b) {
				if ("explode".equals(a) || "explode".equals(b)) {
					throw failure;
				}
				return Objects.equals(a, b);
			}

			@Override
			public int hashCode(Object element) {
				if ("explode".equals(element)) {
					throw failure;
				}
				return Objects.hashCode(element);
			}
		};
	}

	@Test
	public void preservingDeltaRetainsReinsertedSelectionForEveryFamily() {
		for (String name : FAMILIES) {
			roots.clear();
			setUpRoots();
			StructuredViewer viewer = family(name, new CountingProvider());
			viewer.setSelection(new StructuredSelection("r2"));
			setCheckboxState(viewer, "r2", true, true);
			setCheckboxState(viewer, "r1", true, false);
			SelectionEvents events = selectionEvents(viewer);
			int[] checkEvents = { 0 };
			if (viewer instanceof CheckboxTableViewer table) {
				table.addCheckStateListener(event -> checkEvents[0]++);
			} else if (viewer instanceof CheckboxTreeViewer tree) {
				tree.addCheckStateListener(event -> checkEvents[0]++);
			}
			roots.remove("r2");
			roots.add("r2");
			M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().remove("r2").add(roots, "r2", -1));
			assertEquals(String.join(" ", roots), texts(viewer), name);
			assertEquals("r2", viewer.getStructuredSelection().getFirstElement(), name);
			assertSelectionEvents(events, 0, name);
			assertCheckboxState(viewer, "r2", true, true, name);
			assertCheckboxState(viewer, "r1", true, false, name);
			assertEquals(0, checkEvents[0], name + " must not synthesize check events");
			viewer.getControl().dispose();
		}
	}

	@Test
	public void preservingDeltaRetainsMultipleSelectedElements() {
		for (String name : new String[] { "table", "checkbox-table", "tree", "checkbox-tree", "list" }) {
			roots.clear();
			setUpRoots();
			StructuredViewer viewer = family(name, new CountingProvider());
			viewer.setSelection(new StructuredSelection(List.of("r1", "r3")));
			SelectionEvents events = selectionEvents(viewer);
			roots.remove("r1");
			roots.remove("r3");
			roots.add("r1");
			roots.add("r3");
			M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().remove("r1").remove("r3")
					.add(roots, "r1", -1).add(roots, "r3", -1));
			assertEquals(String.join(" ", roots), texts(viewer), name);
			assertEquals(List.of("r1", "r3"), viewer.getStructuredSelection().toList(), name);
			assertSelectionEvents(events, 0, name);
			viewer.getControl().dispose();
		}
	}

	@Test
	public void preservingDeltaNotifiesOnceForAPermanentlyRemovedSelection() {
		for (String name : FAMILIES) {
			roots.clear();
			setUpRoots();
			StructuredViewer viewer = family(name, new CountingProvider());
			viewer.setSelection(new StructuredSelection("r2"));
			SelectionEvents events = selectionEvents(viewer);
			roots.remove("r2");
			M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().remove("r2"));
			assertEquals(String.join(" ", roots), texts(viewer), name);
			assertTrue(viewer.getSelection().isEmpty(), name);
			assertSelectionEvents(events, 1, name);
			assertTrue(events.changed().getFirst().isEmpty(), name);
			assertTrue(events.postChanged().getFirst().isEmpty(), name);
			viewer.getControl().dispose();
		}
	}

	@Test
	public void callbackSelectionWinsAcrossNestedPreservingDeltas() {
		for (String name : FAMILIES) {
			roots.clear();
			setUpRoots();
			StructuredViewer viewer = family(name, new CountingProvider());
			boolean[] armed = { false };
			boolean[] called = { false };
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (armed[0] && !called[0] && "r1".equals(element)) {
						called[0] = true;
						viewer.setSelection(new StructuredSelection("r3"), false);
						M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().update("r0", null));
					}
					return super.getText(element);
				}
			});
			viewer.setSelection(new StructuredSelection("r2"));
			SelectionEvents events = selectionEvents(viewer);
			armed[0] = true;
			roots.remove("r4");
			M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().update("r1", null).remove("r4"));
			assertTrue(called[0], name + " must enter the update callback");
			assertEquals("r3", viewer.getStructuredSelection().getFirstElement(), name);
			assertSelectionEvents(events, 1, name);
			assertEquals(viewer.getSelection(), events.changed().getFirst(), name);
			assertEquals(viewer.getSelection(), events.postChanged().getFirst(), name);
			viewer.setSelection(new StructuredSelection("r0"));
			assertSelectionEvents(events, 2, name + " after the preserving boundary");
			viewer.getControl().dispose();
		}
	}

	@Test
	public void checkStateProvidersRemainAuthoritativeDuringPreservingDeltas() {
		for (String name : new String[] { "checkbox-table", "checkbox-tree" }) {
			roots.clear();
			setUpRoots();
			StructuredViewer viewer = family(name, new CountingProvider());
			boolean[] checked = { true };
			setCheckStateProvider(viewer, new ICheckStateProvider() {
				@Override
				public boolean isChecked(Object element) {
					return checked[0] && "r2".equals(element);
				}

				@Override
				public boolean isGrayed(Object element) {
					return checked[0] && "r2".equals(element);
				}
			});
			viewer.setSelection(new StructuredSelection("r2"));
			assertCheckboxState(viewer, "r2", true, true, name);
			SelectionEvents events = selectionEvents(viewer);
			checked[0] = false;
			roots.remove("r2");
			roots.add("r2");
			M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().remove("r2").add(roots, "r2", -1));
			assertCheckboxState(viewer, "r2", false, false, name);
			assertEquals("r2", viewer.getStructuredSelection().getFirstElement(), name);
			assertSelectionEvents(events, 0, name);
			viewer.getControl().dispose();
		}
	}

	@Test
	public void preservingDeltaPropagatesFailuresAndReleasesItsScopeForEveryFamily() {
		for (String name : FAMILIES) {
			for (boolean partial : new boolean[] { false, true }) {
				roots.clear();
				setUpRoots();
				CountingProvider provider = new CountingProvider();
				StructuredViewer viewer = family(name, provider);
				RuntimeException failure = new IllegalStateException("deliberate viewer lookup failure");
				viewer.setComparer(failingComparer(failure));
				viewer.setSelection(new StructuredSelection("r2"));
				SelectionEvents events = selectionEvents(viewer);
				int before = provider.reads;
				M3ViewerDelta delta = new M3ViewerDelta();
				if (partial) {
					roots.remove("r0");
					delta.remove("r0");
				}
				delta.update("explode", null);
				assertSame(failure, assertThrows(RuntimeException.class,
						() -> M3Viewers.applyPreservingSelection(viewer, delta)), name);
				assertEquals(String.join(" ", roots), texts(viewer), name);
				assertEquals("r2", viewer.getStructuredSelection().getFirstElement(), name);
				assertEquals(before, provider.reads, name + " must not refresh after a direct failure");
				assertSelectionEvents(events, 0, name);
				viewer.setSelection(new StructuredSelection("r3"));
				assertSelectionEvents(events, 1, name + " after failure");
				viewer.getControl().dispose();
			}
		}
	}

	@Test
	public void checkboxFailureKeepsPartialWidgetStateWithoutARollback() {
		for (String name : new String[] { "checkbox-table", "checkbox-tree" }) {
			roots.clear();
			setUpRoots();
			StructuredViewer viewer = family(name, new CountingProvider());
			RuntimeException failure = new IllegalStateException("deliberate failure after reinsertion");
			viewer.setComparer(failingComparer(failure));
			viewer.setSelection(new StructuredSelection("r2"));
			setCheckboxState(viewer, "r2", true, true);
			SelectionEvents events = selectionEvents(viewer);
			roots.remove("r2");
			roots.add("r2");
			M3ViewerDelta delta = new M3ViewerDelta().remove("r2").add(roots, "r2", -1).update("explode", null);
			assertSame(failure, assertThrows(RuntimeException.class,
					() -> M3Viewers.applyPreservingSelection(viewer, delta)), name);
			assertEquals(String.join(" ", roots), texts(viewer), name);
			assertEquals("r2", viewer.getStructuredSelection().getFirstElement(), name);
			assertCheckboxState(viewer, "r2", false, false, name + " retains the partial widget state");
			assertSelectionEvents(events, 0, name);
			viewer.setSelection(new StructuredSelection("r3"));
			assertSelectionEvents(events, 1, name + " after failure");
			viewer.getControl().dispose();
		}
	}

	@Test
	public void ordinaryApplyDoesNotRetainRemovedThenReinsertedSelection() {
		for (String name : FAMILIES) {
			roots.clear();
			setUpRoots();
			StructuredViewer viewer = family(name, new CountingProvider());
			viewer.setSelection(new StructuredSelection("r2"));
			SelectionEvents events = selectionEvents(viewer);
			roots.remove("r2");
			roots.add("r2");
			M3Viewers.apply(viewer, new M3ViewerDelta().remove("r2").add(roots, "r2", -1));
			assertEquals(String.join(" ", roots), texts(viewer), name);
			assertTrue(viewer.getSelection().isEmpty(), name);
			assertSelectionEvents(events, 1, name);
			viewer.getControl().dispose();
		}
	}

	private static final class ModelNode {
		final String id;
		final String label;
		final List<ModelNode> children = new ArrayList<>();
		ModelNode parent;

		ModelNode(String id, String label, ModelNode... children) {
			this.id = id;
			this.label = label;
			for (ModelNode child : children) {
				this.children.add(child);
				child.parent = this;
			}
		}

		@Override
		public String toString() {
			return label;
		}
	}

	private static final IElementComparer NODE_COMPARER = new IElementComparer() {
		@Override
		public boolean equals(Object a, Object b) {
			return a instanceof ModelNode left && b instanceof ModelNode right
					? left.id.equals(right.id) : a == b;
		}

		@Override
		public int hashCode(Object element) {
			return element instanceof ModelNode ? 7 : System.identityHashCode(element);
		}
	};

	private static ITreeContentProvider nodeProvider(List<ModelNode> model) {
		return new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object input) {
				return model.toArray();
			}

			@Override
			public Object[] getChildren(Object parent) {
				return parent instanceof ModelNode node ? node.children.toArray() : new Object[0];
			}

			@Override
			public Object getParent(Object element) {
				return element instanceof ModelNode node ? node.parent : null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return element instanceof ModelNode node && !node.children.isEmpty();
			}
		};
	}

	@Test
	public void preservingDeltaRestoresEqualReplacementFacadesWithHashCollisions() {
		for (String name : FAMILIES) {
			ModelNode a = new ModelNode("a", "a");
			ModelNode b = new ModelNode("b", "b");
			ModelNode c = new ModelNode("c", "c");
			ModelNode replacement = new ModelNode("b", "b-new");
			List<ModelNode> model = new ArrayList<>(List.of(a, b, c));
			StructuredViewer viewer = family(name, nodeProvider(model), true);
			viewer.setComparer(NODE_COMPARER);
			viewer.setInput(model);
			viewer.setSelection(new StructuredSelection(b));
			setCheckboxState(viewer, b, true, true);
			SelectionEvents events = selectionEvents(viewer);
			model.remove(b);
			model.add(replacement);
			M3Viewers.applyPreservingSelection(viewer,
					new M3ViewerDelta().remove(b).add(model, replacement, -1));
			assertEquals("a c b-new", texts(viewer), name);
			assertSame(replacement, viewer.getStructuredSelection().getFirstElement(), name);
			assertCheckboxState(viewer, replacement, true, true, name);
			assertSelectionEvents(events, 0, name);
			viewer.getControl().dispose();
		}
	}

	@Test
	public void preservingTreeDeltasKeepTheSelectedPathAndUnaffectedExpansion() {
		for (String name : new String[] { "tree", "checkbox-tree" }) {
			ModelNode firstLeaf = new ModelNode("leaf", "first leaf");
			ModelNode secondLeaf = new ModelNode("leaf", "second leaf");
			ModelNode sibling = new ModelNode("sibling", "sibling");
			ModelNode expanded = new ModelNode("expanded", "expanded", firstLeaf, sibling);
			ModelNode collapsed = new ModelNode("collapsed", "collapsed", secondLeaf);
			List<ModelNode> model = new ArrayList<>(List.of(expanded, collapsed));
			TreeViewer viewer = (TreeViewer) family(name, nodeProvider(model), true);
			viewer.setComparer(NODE_COMPARER);
			viewer.setInput(model);
			viewer.setExpandedState(expanded, true);
			TreePath selected = new TreePath(new Object[] { expanded, firstLeaf });
			viewer.setSelection(new TreeSelection(selected));
			setCheckboxState(viewer, firstLeaf, true, true);
			SelectionEvents events = selectionEvents(viewer);
			expanded.children.remove(firstLeaf);
			expanded.children.add(firstLeaf);
			M3Viewers.applyPreservingSelection(viewer,
					new M3ViewerDelta().remove(selected).add(expanded, firstLeaf, -1));
			assertArrayEquals(new TreePath[] { selected }, ((TreeSelection) viewer.getSelection()).getPaths(), name);
			assertTrue(viewer.getExpandedState(expanded), name);
			assertEquals(false, viewer.getExpandedState(collapsed), name);
			assertCheckboxState(viewer, firstLeaf, true, true, name);
			assertSelectionEvents(events, 0, name);
			viewer.getControl().dispose();
		}
	}

	@Test
	public void preservingSelectionDoesNotReexpandARecreatedUnselectedParent() {
		for (String name : new String[] { "tree", "checkbox-tree" }) {
			ModelNode parent = new ModelNode("parent", "parent", new ModelNode("child", "child"));
			ModelNode selected = new ModelNode("selected", "selected");
			List<ModelNode> model = new ArrayList<>(List.of(parent, selected));
			TreeViewer viewer = (TreeViewer) family(name, nodeProvider(model));
			viewer.setInput(model);
			viewer.setExpandedState(parent, true);
			viewer.setSelection(new StructuredSelection(selected));
			SelectionEvents events = selectionEvents(viewer);
			M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().remove(parent).add(model, parent, 0));
			assertSame(selected, viewer.getStructuredSelection().getFirstElement(), name);
			assertEquals(false, viewer.getExpandedState(parent), name);
			assertSelectionEvents(events, 0, name);
			viewer.getControl().dispose();
		}
	}

	@Test
	public void preservingSelectionSkipsEmptyAndDisposedViewers() {
		for (String name : FAMILIES) {
			CountingProvider provider = new CountingProvider();
			StructuredViewer viewer = family(name, provider);
			SelectionEvents events = selectionEvents(viewer);
			int before = provider.reads;
			M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta());
			assertEquals(before, provider.reads, name);
			assertSelectionEvents(events, 0, name);
			viewer.getControl().dispose();
			M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().refresh(null));
			assertEquals(before, provider.reads, name);
			assertSelectionEvents(events, 0, name);
		}
	}

	@Test
	public void preservingSelectionRetainsExistingCheckboxDisposalBehavior() {
		for (String name : new String[] { "table", "tree", "checkbox-table", "checkbox-tree" }) {
			for (boolean provider : new boolean[] { false, true }) {
				StructuredViewer viewer = family(name, new CountingProvider());
				if (provider) {
					setCheckStateProvider(viewer, new ICheckStateProvider() {
						@Override
						public boolean isChecked(Object element) {
							return false;
						}

						@Override
						public boolean isGrayed(Object element) {
							return false;
						}
					});
				}
				boolean[] armed = { false };
				boolean[] called = { false };
				viewer.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						if (armed[0] && !called[0] && "r1".equals(element)) {
							called[0] = true;
							viewer.getControl().dispose();
						}
						return super.getText(element);
					}
				});
				viewer.setSelection(new StructuredSelection("r2"));
				armed[0] = true;
				M3ViewerDelta delta = new M3ViewerDelta().update("r1", null);
				if (!provider && (viewer instanceof CheckboxTableViewer || viewer instanceof CheckboxTreeViewer)) {
					SWTException failure = assertThrows(SWTException.class,
							() -> M3Viewers.applyPreservingSelection(viewer, delta), name);
					assertEquals(SWT.ERROR_WIDGET_DISPOSED, failure.code, name);
				} else {
					M3Viewers.applyPreservingSelection(viewer, delta);
				}
				assertTrue(called[0], name + " must enter the disposing callback");
				assertTrue(viewer.getControl().isDisposed(), name);
				M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().remove("r2"));
			}
		}
	}

	private static final class RecordingComboViewer extends ComboViewer {
		final List<Boolean> widgetReveals = new ArrayList<>();
		final List<ISelection> boundarySelections = new ArrayList<>();

		RecordingComboViewer(Combo control) {
			super(control);
		}

		RecordingComboViewer(CCombo control) {
			super(control);
		}

		@Override
		protected void setSelectionToWidget(ISelection selection, boolean reveal) {
			widgetReveals.add(reveal);
			super.setSelectionToWidget(selection, reveal);
		}

		@Override
		protected void handleInvalidSelection(ISelection invalidSelection, ISelection newSelection) {
			boundarySelections.add(newSelection);
			super.handleInvalidSelection(invalidSelection, newSelection);
		}

		void clearCalls() {
			widgetReveals.clear();
			boundarySelections.clear();
		}
	}

	private RecordingComboViewer recordingCombo(String name) {
		RecordingComboViewer viewer = "combo".equals(name)
				? new RecordingComboViewer(new Combo(shell, SWT.READ_ONLY))
				: new RecordingComboViewer(new CCombo(shell, SWT.READ_ONLY));
		viewer.setLabelProvider(new LabelProvider());
		viewer.setContentProvider(new CountingProvider());
		viewer.setInput(roots);
		return viewer;
	}

	private void drainNativeSelectionEvents() {
		boolean[] dispatched = { false };
		Runnable marker = () -> dispatched[0] = true;
		display.timerExec(0, marker);
		try {
			waitUntil(() -> dispatched[0]);
			drain();
		} finally {
			display.timerExec(-1, marker);
		}
	}

	private void assertDrainedComboSelection(String context, RecordingComboViewer viewer, SelectionEvents events,
			Object expected) {
		int changedAtBoundary = events.changed().size();
		int postAtBoundary = events.postChanged().size();
		drainNativeSelectionEvents();
		assertEquals(expected, viewer.getStructuredSelection().getFirstElement(), context);
		for (ISelection selection : events.changed()) {
			assertEquals(expected, ((IStructuredSelection) selection).getFirstElement(), context + " selection payload");
		}
		for (ISelection selection : events.postChanged()) {
			assertEquals(expected, ((IStructuredSelection) selection).getFirstElement(), context + " post payload");
		}
		// Native Selection may be posted after Modify. Keep that legacy delivery
		// visible separately from the exact synchronous boundary assertions.
		System.out.println("M3_COMBO_TRANSCRIPT " + context + " beforeDrain=" + changedAtBoundary + "/"
				+ postAtBoundary + " afterDrain=" + events.changed().size() + "/" + events.postChanged().size()
				+ " final=" + viewer.getSelection() + " changed=" + events.changed() + " post=" + events.postChanged());
	}

	@Test
	public void ordinaryComboUpdatesPreserveChangedSelectedLabelsDirectlyAndWhenPosted() {
		for (String name : new String[] { "combo", "ccombo" }) {
			for (boolean posted : new boolean[] { false, true }) {
				RecordingComboViewer viewer = recordingCombo(name);
				boolean[] changed = { false };
				viewer.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						return changed[0] && "r2".equals(element) ? "r2-updated" : super.getText(element);
					}
				});
				viewer.setSelection(new StructuredSelection("r2"));
				drainNativeSelectionEvents();
				viewer.clearCalls();
				SelectionEvents events = selectionEvents(viewer);
				changed[0] = true;
				String context = name + (posted ? " posted selected label" : " direct selected label");
				M3ViewerDelta delta = new M3ViewerDelta().update("r2", null);
				if (posted) {
					M3Viewers.post(viewer, delta, 0);
					waitUntil(() -> texts(viewer).contains("r2-updated"));
				} else {
					M3Viewers.apply(viewer, delta);
					assertSelectionEvents(events, 0, context);
				}
				assertEquals("r0 r1 r2-updated r3 r4", texts(viewer), context);
				assertEquals("r2", viewer.getStructuredSelection().getFirstElement(), context);
				assertTrue(viewer.boundarySelections.isEmpty(), context);
				assertEquals("combo".equals(name) ? List.of(false) : List.of(), viewer.widgetReveals, context);
				assertDrainedComboSelection(context, viewer, events, "r2");
				viewer.getControl().dispose();
			}
		}
	}

	@Test
	public void comboLabelCallbacksKeepUpdatedDifferentAndEmptySelectionRequests() {
		ISelection[] requests = { new StructuredSelection("r2"), new StructuredSelection("r2"),
				new StructuredSelection("r3"), StructuredSelection.EMPTY, null };
		String[] initial = { "r2", "r0", "r2", "r2", "r2" };
		String[] expected = { "r2", "r2", "r3", null, null };
		for (String name : new String[] { "combo", "ccombo" }) {
			for (String delivery : new String[] { "apply", "preserving", "posted" }) {
				for (int choice = 0; choice < requests.length; choice++) {
					RecordingComboViewer viewer = recordingCombo(name);
					boolean[] armed = { false };
					boolean[] called = { false };
					ISelection request = requests[choice];
					boolean reveal = choice % 2 == 0;
					viewer.setLabelProvider(new LabelProvider() {
						@Override
						public String getText(Object element) {
							if (armed[0] && "r2".equals(element)) {
								if (!called[0]) {
									called[0] = true;
									viewer.setSelection(request, reveal);
								}
								return "r2-updated";
							}
							return super.getText(element);
						}
					});
					viewer.setSelection(new StructuredSelection(initial[choice]));
					drainNativeSelectionEvents();
					viewer.clearCalls();
					SelectionEvents events = selectionEvents(viewer);
					armed[0] = true;
					String context = name + " " + delivery + " label request " + choice;
					M3ViewerDelta delta = new M3ViewerDelta().update("r2", null);
					if ("posted".equals(delivery)) {
						M3Viewers.post(viewer, delta, 0);
						waitUntil(() -> texts(viewer).contains("r2-updated"));
					} else if ("preserving".equals(delivery)) {
						M3Viewers.applyPreservingSelection(viewer, delta);
					} else {
						M3Viewers.apply(viewer, delta);
					}
					assertTrue(called[0], context);
					assertEquals(expected[choice], viewer.getStructuredSelection().getFirstElement(), context);
					int expectedEvents = Objects.equals(initial[choice], expected[choice]) ? 0 : 1;
					assertEquals(expectedEvents, viewer.boundarySelections.size(), context);
					if (!"posted".equals(delivery)) {
						assertSelectionEvents(events, expectedEvents, context);
					}
					assertEquals(reveal, viewer.widgetReveals.getLast(), context + " reveal");
					assertDrainedComboSelection(context, viewer, events, expected[choice]);
					viewer.getControl().dispose();
				}
			}
		}
	}

	@Test
	public void nativeComboModifyCallbacksKeepTheirLastRequestDuringMutationAndRestoration() {
		for (boolean duringRestoration : new boolean[] { false, true }) {
			for (String selected : new String[] { "r2", "r3", null }) {
				RecordingComboViewer viewer = recordingCombo("combo");
				Combo combo = (Combo) viewer.getControl();
				boolean[] armed = { false };
				boolean[] called = { false };
				viewer.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						return armed[0] && "r2".equals(element) ? "r2-updated" : super.getText(element);
					}
				});
				viewer.setSelection(new StructuredSelection("r2"));
				drainNativeSelectionEvents();
				viewer.clearCalls();
				SelectionEvents events = selectionEvents(viewer);
				combo.addModifyListener(event -> {
					boolean phase = duringRestoration
							? combo.getSelectionIndex() == 2 && "r2-updated".equals(combo.getText())
							: combo.getSelectionIndex() == -1;
					if (armed[0] && !called[0] && phase) {
						called[0] = true;
						viewer.setSelection(selected == null ? StructuredSelection.EMPTY
								: new StructuredSelection(selected), true);
						M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().update("r0", null));
					}
				});
				armed[0] = true;
				String context = "combo Modify " + (duringRestoration ? "restoration " : "mutation ") + selected;
				M3Viewers.apply(viewer, new M3ViewerDelta().update("r2", null));
				assertTrue(called[0], context + " must enter the native callback");
				assertEquals(selected, viewer.getStructuredSelection().getFirstElement(), context);
				int expectedEvents = "r2".equals(selected) ? 0 : 1;
				assertSelectionEvents(events, expectedEvents, context);
				assertEquals(expectedEvents, viewer.boundarySelections.size(), context);
				assertEquals(true, viewer.widgetReveals.getLast(), context + " reveal");
				assertDrainedComboSelection(context, viewer, events, selected);
				viewer.getControl().dispose();
			}
		}
	}

	@Test
	public void comboUpdatesSkipUnneededRestorationWithoutARevealRequest() {
		for (String name : new String[] { "combo", "ccombo" }) {
			RecordingComboViewer viewer = recordingCombo(name);
			viewer.setSelection(new StructuredSelection("r2"));
			drainNativeSelectionEvents();
			viewer.clearCalls();
			M3Viewers.apply(viewer, new M3ViewerDelta().update("r1", null));
			assertEquals("r2", viewer.getStructuredSelection().getFirstElement(), name);
			assertTrue(viewer.widgetReveals.isEmpty(), name + " unchanged selection");
			viewer.getControl().dispose();

			for (String selected : new String[] { "r3", null }) {
				RecordingComboViewer callbackViewer = recordingCombo(name);
				boolean[] armed = { false };
				boolean[] called = { false };
				callbackViewer.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						if (armed[0] && !called[0] && "r1".equals(element)) {
							called[0] = true;
							callbackViewer.setSelection(selected == null ? null : new StructuredSelection(selected), false);
						}
						return super.getText(element);
					}
				});
				callbackViewer.setSelection(new StructuredSelection("r2"));
				drainNativeSelectionEvents();
				callbackViewer.clearCalls();
				SelectionEvents events = selectionEvents(callbackViewer);
				armed[0] = true;
				String context = name + " surviving request " + selected;
				M3Viewers.apply(callbackViewer, new M3ViewerDelta().update("r1", null));
				assertTrue(called[0], context);
				assertEquals(selected, callbackViewer.getStructuredSelection().getFirstElement(), context);
				assertEquals(List.of(false), callbackViewer.widgetReveals, context + " only the explicit request");
				assertSelectionEvents(events, 1, context);
				assertDrainedComboSelection(context, callbackViewer, events, selected);
				callbackViewer.getControl().dispose();
			}
		}
	}

	@Test
	public void disabledPreservationStillExecutesOrdinaryAndExplicitComboUpdates() throws ReflectiveOperationException {
		Method setter = StructuredViewer.class.getDeclaredMethod("setPreserveSelection", boolean.class);
		setter.setAccessible(true);
		for (String name : new String[] { "combo", "ccombo" }) {
			for (boolean preserving : new boolean[] { false, true }) {
				RecordingComboViewer viewer = recordingCombo(name);
				boolean[] armed = { false };
				int[] calls = { 0 };
				viewer.setLabelProvider(new LabelProvider() {
					@Override
					public String getText(Object element) {
						if (armed[0] && "r2".equals(element)) {
							calls[0]++;
							return "r2-updated";
						}
						return super.getText(element);
					}
				});
				viewer.setSelection(new StructuredSelection("r2"));
				drainNativeSelectionEvents();
				viewer.clearCalls();
				setter.invoke(viewer, false);
				try {
					armed[0] = true;
					M3ViewerDelta delta = new M3ViewerDelta().update("r2", null);
					if (preserving) {
						M3Viewers.applyPreservingSelection(viewer, delta);
					} else {
						M3Viewers.apply(viewer, delta);
					}
					assertEquals(1, calls[0], name);
					assertEquals("r0 r1 r2-updated r3 r4", texts(viewer), name);
					assertTrue(viewer.widgetReveals.isEmpty(), name + " disabled restoration");
					assertTrue(viewer.boundarySelections.isEmpty(), name);
				} finally {
					setter.invoke(viewer, true);
					viewer.getControl().dispose();
				}
			}
		}
	}

	@Test
	public void ordinaryComboUpdateFailureRestoresTheLastRequestAndReleasesItsScope() {
		for (String name : new String[] { "combo", "ccombo" }) {
			RecordingComboViewer viewer = recordingCombo(name);
			RuntimeException failure = new IllegalStateException("deliberate Combo lookup failure");
			viewer.setComparer(failingComparer(failure));
			boolean[] armed = { false };
			boolean[] called = { false };
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (armed[0] && !called[0] && "r2".equals(element)) {
						called[0] = true;
						viewer.setSelection(new StructuredSelection("r2"), false);
					}
					return super.getText(element);
				}
			});
			viewer.setSelection(new StructuredSelection("r0"));
			drainNativeSelectionEvents();
			viewer.clearCalls();
			SelectionEvents events = selectionEvents(viewer);
			int before = ((CountingProvider) viewer.getContentProvider()).reads;
			armed[0] = true;
			M3ViewerDelta delta = new M3ViewerDelta().update("r2", null).update("explode", null);
			assertSame(failure, assertThrows(RuntimeException.class, () -> M3Viewers.apply(viewer, delta)), name);
			assertTrue(called[0], name);
			assertEquals("r2", viewer.getStructuredSelection().getFirstElement(), name);
			assertEquals(before, ((CountingProvider) viewer.getContentProvider()).reads, name + " no fallback refresh");
			assertSelectionEvents(events, 1, name);
			assertDrainedComboSelection(name + " direct failure", viewer, events, "r2");
			viewer.setSelection(new StructuredSelection("r3"));
			viewer.clearCalls();
			M3Viewers.apply(viewer, new M3ViewerDelta().update("r3", null));
			assertEquals("r3", viewer.getStructuredSelection().getFirstElement(), name + " no stale request");
			viewer.getControl().dispose();
		}
	}

	@Test
	public void ordinaryComboUpdateInsideStockPreservationRetainsCallbackIntent() {
		for (String name : new String[] { "combo", "ccombo" }) {
			RecordingComboViewer viewer = recordingCombo(name);
			boolean[] armed = { false };
			boolean[] entered = { false };
			boolean[] selected = { false };
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (armed[0] && !entered[0] && "r2".equals(element)) {
						entered[0] = true;
						M3Viewers.apply(viewer, new M3ViewerDelta().update("r3", null));
					}
					if (armed[0] && entered[0] && !selected[0] && "r3".equals(element)) {
						selected[0] = true;
						viewer.setSelection(new StructuredSelection("r3"), true);
					}
					return super.getText(element);
				}
			});
			viewer.setSelection(new StructuredSelection("r0"));
			drainNativeSelectionEvents();
			viewer.clearCalls();
			SelectionEvents events = selectionEvents(viewer);
			armed[0] = true;
			viewer.refresh("r2");
			assertTrue(entered[0] && selected[0], name);
			assertEquals("r3", viewer.getStructuredSelection().getFirstElement(), name);
			assertSelectionEvents(events, 1, name);
			assertEquals(1, viewer.boundarySelections.size(), name + " outer stock owner");
			assertDrainedComboSelection(name + " nested stock preservation", viewer, events, "r3");
			viewer.getControl().dispose();
		}
	}

	@Test
	public void finalSelectionListenerStartsAFreshComboPreservationScope() {
		for (String name : new String[] { "combo", "ccombo" }) {
			RecordingComboViewer viewer = recordingCombo(name);
			boolean[] armed = { false };
			boolean[] selected = { false };
			boolean[] listenerCalled = { false };
			Object[] nestedSelection = { null };
			viewer.setLabelProvider(new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (armed[0] && !selected[0] && "r1".equals(element)) {
						selected[0] = true;
						viewer.setSelection(new StructuredSelection("r3"), false);
					}
					return super.getText(element);
				}
			});
			viewer.setSelection(new StructuredSelection("r0"));
			drainNativeSelectionEvents();
			viewer.addSelectionChangedListener(event -> {
				if (armed[0] && !listenerCalled[0]) {
					listenerCalled[0] = true;
					if (viewer.getControl() instanceof Combo combo) {
						combo.select(4);
					} else {
						((CCombo) viewer.getControl()).select(4);
					}
					M3Viewers.apply(viewer, new M3ViewerDelta().update("r4", null));
					nestedSelection[0] = viewer.getStructuredSelection().getFirstElement();
				}
			});
			armed[0] = true;
			M3Viewers.apply(viewer, new M3ViewerDelta().update("r1", null));
			assertTrue(selected[0] && listenerCalled[0], name);
			assertEquals("r4", nestedSelection[0], name + " new scope snapshot");
			assertEquals("r4", viewer.getStructuredSelection().getFirstElement(), name);
			drainNativeSelectionEvents();
			assertEquals("r4", viewer.getStructuredSelection().getFirstElement(), name + " after native events");
			viewer.getControl().dispose();
		}
	}


	@Test
	public void comboViewersClearSelectionWhenAllRequestedElementsAreAbsent() {
		for (String name : new String[] { "combo", "ccombo" }) {
			RecordingComboViewer viewer = recordingCombo(name);
			viewer.setSelection(new StructuredSelection("r2"));
			drainNativeSelectionEvents();
			viewer.clearCalls();
			SelectionEvents events = selectionEvents(viewer);
			viewer.setSelection(new StructuredSelection("absent"), false);
			assertTrue(viewer.getSelection().isEmpty(), name + " missing logical selection");
			Control control = viewer.getControl();
			int index = control instanceof Combo combo ? combo.getSelectionIndex()
					: ((CCombo) control).getSelectionIndex();
			String displayed = control instanceof Combo combo ? combo.getText() : ((CCombo) control).getText();
			assertEquals(-1, index, name + " native index after missing selection");
			assertEquals("", displayed, name + " text after missing selection");
			assertEquals(String.join(" ", roots), texts(viewer), name);
			assertSelectionEvents(events, 1, name + " missing logical selection");
			assertTrue(events.changed().getFirst().isEmpty(), name);
			assertTrue(events.postChanged().getFirst().isEmpty(), name);
			assertDrainedComboSelection(name + " missing logical selection", viewer, events, null);
			viewer.getControl().dispose();
		}
	}

	@Test
	public void ccomboDeselectAllCompletesBeforeModifyCallbacks() {
		for (boolean dispose : new boolean[] { false, true }) {
			CCombo combo = new CCombo(shell, SWT.READ_ONLY);
			combo.setItems(roots.toArray(String[]::new));
			combo.select(2);
			boolean[] called = { false };
			combo.addModifyListener(event -> {
				if (!called[0]) {
					called[0] = true;
					if (dispose) {
						combo.dispose();
					} else {
						combo.select(3);
					}
				}
			});
			combo.deselectAll();
			assertTrue(called[0], "CCombo deselectAll must enter Modify");
			if (dispose) {
				assertTrue(combo.isDisposed(), "CCombo Modify disposal must finish without later widget access");
			} else {
				assertEquals(3, combo.getSelectionIndex(),
						"CCombo Modify callback selection must survive deselectAll");
				assertEquals("r3", combo.getText(), "CCombo callback text and selected row must agree");
				combo.dispose();
			}
		}
	}

	@Test
	public void ccomboFinalRemovalRestorationHonorsModifySelectionAndDisposal() {
		for (boolean dispose : new boolean[] { false, true }) {
			roots.clear();
			setUpRoots();
			RecordingComboViewer viewer = recordingCombo("ccombo");
			CCombo combo = (CCombo) viewer.getControl();
			viewer.setSelection(new StructuredSelection("r2"));
			drainNativeSelectionEvents();
			viewer.clearCalls();
			SelectionEvents events = selectionEvents(viewer);
			boolean[] called = { false };
			int[] itemsAtCallback = { -1 };
			combo.addModifyListener(event -> {
				// The recording owner is entered only for the outer restoration;
				// nested remove preserves through the active M3 owner.
				if (!called[0] && !viewer.widgetReveals.isEmpty()) {
					called[0] = true;
					itemsAtCallback[0] = combo.getItemCount();
					if (dispose) {
						combo.dispose();
					} else {
						viewer.setSelection(new StructuredSelection("r3"), true);
					}
				}
			});
			roots.remove("r2");
			M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().remove("r2"));
			assertTrue(called[0], "CCombo must enter Modify during final missing-row restoration");
			assertEquals(4, itemsAtCallback[0], "the selected row must already have been removed");
			if (dispose) {
				assertTrue(combo.isDisposed(), "final-restoration disposal must finish");
				assertSelectionEvents(events, 0, "disposed CCombo final restoration");
				assertTrue(viewer.boundarySelections.isEmpty(), "disposed CCombo must not publish a final selection");
			} else {
				assertEquals(String.join(" ", roots), texts(viewer));
				assertEquals("r3", viewer.getStructuredSelection().getFirstElement());
				assertEquals(2, combo.getSelectionIndex(), "r3 follows the removed r2 row");
				assertEquals("r3", combo.getText());
				assertSelectionEvents(events, 1, "CCombo final-restoration callback selection");
				assertEquals(1, viewer.boundarySelections.size());
				assertEquals(viewer.getSelection(), events.changed().getFirst());
				assertEquals(viewer.getSelection(), events.postChanged().getFirst());
				assertEquals(true, viewer.widgetReveals.getLast(), "the callback's reveal request");
				assertDrainedComboSelection("ccombo final-restoration callback", viewer, events, "r3");
				combo.dispose();
			}
		}
	}

	@Test
	public void differentialScriptsHavePositiveGeometryAndCompleteEveryVariant() {
		String[] variants = { "table", "table-hash", "table-sorted-filtered", "tree", "tree-hash", "list" };
		assertArrayEquals(variants, M3ViewerDifferential.VARIANTS);
		for (String variant : variants) {
			List<String> trace = M3ViewerDifferential.script(display, variant, 0, 20);
			assertEquals(21, trace.size(), variant);
			for (int row = 0; row < trace.size(); row++) {
				String line = trace.get(row);
				String context = variant + " record " + row;
				assertTrue(line.startsWith(row == 0 ? "setInput | " : (row - 1) + " "), context);
				assertFalse(line.contains(" exception "), context);
				assertTrue(line.matches(".* \\| size=[1-9][0-9]*x[1-9][0-9]* \\| .* \\| px=[0-9a-f]+"),
						context + " must include positive control size and a pixel CRC");
			}
			if (variant.startsWith("tree")) {
				assertTrue(trace.get(5).startsWith("4 model-shrink+refresh 16 | "), variant);
				assertTrue(trace.get(5).contains("| roots=38 "), variant);
			}
		}
	}

	@Test
	public void ccomboValidSelectionHonorsModifyReselection() {
		for (int style : new int[] { SWT.READ_ONLY, SWT.NONE }) {
			for (int depth : new int[] { 1, 2 }) {
				CCombo combo = new CCombo(shell, style);
				combo.setItems(roots.toArray(String[]::new));
				combo.select(0);
				List<Integer> indices = new ArrayList<>();
				List<String> labels = new ArrayList<>();
				int[] selections = { 0 };
				combo.addListener(SWT.Selection, event -> selections[0]++);
				combo.addModifyListener(event -> {
					indices.add(combo.getSelectionIndex());
					labels.add(combo.getText());
					if (indices.size() == 1) {
						combo.select(3);
					} else if (depth == 2 && indices.size() == 2) {
						combo.select(4);
					}
				});
				combo.select(1);
				String context = "CCombo valid selection style=" + style + " depth=" + depth;
				assertEquals(depth == 1 ? List.of(1, 3) : List.of(1, 3, 4), indices, context);
				assertEquals(depth == 1 ? List.of("r1", "r3") : List.of("r1", "r3", "r4"), labels, context);
				assertEquals(depth == 1 ? 3 : 4, combo.getSelectionIndex(), context);
				assertEquals(depth == 1 ? "r3" : "r4", combo.getText(), context);
				assertEquals(0, selections[0], context + " does not synthesize Selection");
				combo.dispose();
			}
		}
	}

	@Test
	public void ccomboValidSelectionAllowsModifyDisposal() {
		for (int style : new int[] { SWT.READ_ONLY, SWT.NONE }) {
			CCombo combo = new CCombo(shell, style);
			combo.setItems(roots.toArray(String[]::new));
			combo.select(0);
			int[] indexAtCallback = { -2 };
			String[] textAtCallback = { null };
			combo.addModifyListener(event -> {
				indexAtCallback[0] = combo.getSelectionIndex();
				textAtCallback[0] = combo.getText();
				combo.dispose();
			});
			combo.select(1);
			assertTrue(combo.isDisposed(), "valid selection must finish after Modify disposal");
			assertEquals(1, indexAtCallback[0], "requested row is complete before disposal");
			assertEquals("r1", textAtCallback[0]);
		}
	}

	@Test
	public void ccomboPositiveRestorationHonorsModifySelectionAndDisposal() {
		for (String outcome : new String[] { "select", "clear", "dispose" }) {
			roots.clear();
			setUpRoots();
			RecordingComboViewer viewer = recordingCombo("ccombo");
			CCombo combo = (CCombo) viewer.getControl();
			viewer.setSelection(new StructuredSelection("r2"));
			drainNativeSelectionEvents();
			viewer.clearCalls();
			SelectionEvents events = selectionEvents(viewer);
			boolean[] called = { false };
			int[] indexAtCallback = { -2 };
			int[] itemsAtCallback = { -1 };
			combo.addModifyListener(event -> {
				if (!called[0] && !viewer.widgetReveals.isEmpty() && "r2".equals(combo.getText())) {
					called[0] = true;
					indexAtCallback[0] = combo.getSelectionIndex();
					itemsAtCallback[0] = combo.getItemCount();
					if ("dispose".equals(outcome)) {
						combo.dispose();
					} else if ("clear".equals(outcome)) {
						viewer.setSelection(StructuredSelection.EMPTY, false);
					} else {
						viewer.setSelection(new StructuredSelection("r3"), true);
					}
				}
			});
			roots.remove("r2");
			roots.add("r2");
			M3Viewers.applyPreservingSelection(viewer, new M3ViewerDelta().remove("r2").add(roots, "r2", -1));
			String context = "CCombo positive restoration " + outcome;
			assertTrue(called[0], context + " must enter final valid restoration");
			assertEquals(5, itemsAtCallback[0], context + " after reinsertion");
			assertEquals(4, indexAtCallback[0], context + " restored r2 index before Modify");
			if ("dispose".equals(outcome)) {
				assertTrue(combo.isDisposed(), context);
				assertSelectionEvents(events, 0, context);
				assertTrue(viewer.boundarySelections.isEmpty(), context);
			} else {
				boolean selected = "select".equals(outcome);
				assertEquals(String.join(" ", roots), texts(viewer), context);
				assertEquals(selected ? "r3" : null, viewer.getStructuredSelection().getFirstElement(), context);
				assertEquals(selected ? 2 : -1, combo.getSelectionIndex(), context);
				assertEquals(selected ? "r3" : "", combo.getText(), context);
				assertSelectionEvents(events, 1, context);
				assertEquals(1, viewer.boundarySelections.size(), context);
				assertEquals(viewer.getSelection(), events.changed().getFirst(), context);
				assertEquals(viewer.getSelection(), events.postChanged().getFirst(), context);
				assertEquals(selected, viewer.widgetReveals.getLast(), context + " callback reveal");
				assertDrainedComboSelection(context, viewer, events, selected ? "r3" : null);
				combo.dispose();
			}
		}
	}

	@Test
	public void ccomboValidSelectionKeepsClearRequestsForDuplicateEmptyLabels() {
		for (int style : new int[] { SWT.READ_ONLY, SWT.NONE }) {
			for (String label : new String[] { "same", "" }) {
				for (String operation : new String[] { "select-minus-one", "deselect-all", "deselect-one", "clear-text-selection" }) {
					CCombo combo = new CCombo(shell, style);
					combo.setItems(new String[] { label, label, "other" });
					combo.select(2);
					boolean[] called = { false };
					int[] indexAtCallback = { -2 };
					combo.addModifyListener(event -> {
						if (!called[0]) {
							called[0] = true;
							indexAtCallback[0] = combo.getSelectionIndex();
							switch (operation) {
							case "select-minus-one" -> combo.select(-1);
							case "deselect-all" -> combo.deselectAll();
							case "deselect-one" -> combo.deselect(1);
							case "clear-text-selection" -> combo.clearSelection();
							default -> throw new IllegalStateException(operation);
							}
						}
					});
					combo.select(1);
					String context = "CCombo duplicate clear " + style + " label=" + label + " " + operation;
					assertTrue(called[0], context + " must enter Modify");
					assertEquals(1, indexAtCallback[0], context + " before clear");
					assertEquals(-1, combo.getSelectionIndex(), context + " explicit clear survives");
					assertEquals("clear-text-selection".equals(operation) ? label : "", combo.getText(), context);
					combo.dispose();
				}

				CCombo equalText = new CCombo(shell, style);
				equalText.setItems(new String[] { label, label, "other" });
				equalText.select(0);
				List<Integer> equalIndices = new ArrayList<>();
				equalText.addModifyListener(event -> equalIndices.add(equalText.getSelectionIndex()));
				equalText.select(1);
				String context = "CCombo equal text style=" + style + " label=" + label;
				assertEquals(1, equalText.getSelectionIndex(), context + " requested duplicate row");
				assertEquals(label, equalText.getText(), context);
				for (int index : equalIndices) {
					assertEquals(1, index, context + " completes before any delivered Modify");
				}
				if ("gtk".equals(SWT.getPlatform())) {
					// GTK skips Verify and Modify when both the old and new text are empty.
					assertEquals(label.isEmpty() ? List.of() : List.of(1), equalIndices, context + " GTK delivery");
				}
				equalText.dispose();
			}
		}
	}

	@Test
	public void ccomboValidSelectionPreservesVerifyAndFailureSemantics() {
		for (int style : new int[] { SWT.READ_ONLY, SWT.NONE }) {
			CCombo unchanged = new CCombo(shell, style);
			unchanged.setItems(roots.toArray(String[]::new));
			unchanged.select(0);
			int[] noOpEvents = { 0 };
			unchanged.addListener(SWT.Verify, event -> noOpEvents[0]++);
			unchanged.addListener(SWT.Modify, event -> noOpEvents[0]++);
			unchanged.select(0);
			unchanged.select(-2);
			unchanged.select(unchanged.getItemCount());
			unchanged.select(Integer.MAX_VALUE);
			assertEquals(0, noOpEvents[0], "same and invalid indices must remain no-ops");
			assertEquals(0, unchanged.getSelectionIndex());
			assertEquals("r0", unchanged.getText());
			unchanged.dispose();

			CCombo veto = new CCombo(shell, style);
			veto.setItems(roots.toArray(String[]::new));
			veto.select(0);
			int[] vetoIndex = { -2 };
			int[] vetoModifies = { 0 };
			veto.addVerifyListener(event -> {
				vetoIndex[0] = veto.getSelectionIndex();
				event.doit = false;
			});
			veto.addModifyListener(event -> vetoModifies[0]++);
			veto.select(1);
			assertEquals(0, vetoIndex[0], "Verify observes the previous row");
			assertEquals(0, vetoModifies[0], "veto suppresses text modification");
			assertEquals(1, veto.getSelectionIndex(), "existing veto only refuses the text replacement");
			assertEquals("r0", veto.getText());
			veto.dispose();

			CCombo transformed = new CCombo(shell, style);
			transformed.setItems(roots.toArray(String[]::new));
			transformed.select(0);
			int[] transformedVerifyIndex = { -2 };
			int[] transformedModifyIndex = { -2 };
			transformed.addVerifyListener(event -> {
				transformedVerifyIndex[0] = transformed.getSelectionIndex();
				event.text = "verified \u03b2\uD83D\uDE00";
			});
			transformed.addModifyListener(event -> transformedModifyIndex[0] = transformed.getSelectionIndex());
			transformed.select(1);
			assertEquals(0, transformedVerifyIndex[0]);
			assertEquals(1, transformedModifyIndex[0], "transformed text keeps its requested row before Modify");
			assertEquals(1, transformed.getSelectionIndex());
			assertEquals("verified \u03b2\uD83D\uDE00", transformed.getText());
			transformed.dispose();

			CCombo nested = new CCombo(shell, style);
			nested.setItems(roots.toArray(String[]::new));
			nested.select(0);
			boolean[] enteredVerify = { false };
			List<Integer> nestedIndices = new ArrayList<>();
			List<String> nestedLabels = new ArrayList<>();
			nested.addVerifyListener(event -> {
				if (!enteredVerify[0] && "r1".equals(event.text)) {
					enteredVerify[0] = true;
					nested.setText("nested");
				}
			});
			nested.addModifyListener(event -> {
				nestedIndices.add(nested.getSelectionIndex());
				nestedLabels.add(nested.getText());
			});
			nested.select(1);
			assertTrue(enteredVerify[0]);
			assertEquals(List.of(-1, 1), nestedIndices, "Verify text mutation cannot consume the outer pending row");
			assertEquals(List.of("nested", "r1"), nestedLabels);
			assertEquals(1, nested.getSelectionIndex());
			assertEquals("r1", nested.getText());
			nested.dispose();

			for (int eventType : new int[] { SWT.Verify, SWT.Modify }) {
				CCombo failing = new CCombo(shell, style);
				failing.setItems(roots.toArray(String[]::new));
				failing.select(0);
				RuntimeException failure = new IllegalStateException("deliberate CCombo listener failure " + eventType);
				org.eclipse.swt.widgets.Listener listener = event -> { throw failure; };
				failing.addListener(eventType, listener);
				assertSame(failure, assertThrows(RuntimeException.class, () -> failing.select(1)));
				failing.removeListener(eventType, listener);
				failing.setText("after failure");
				assertEquals(-1, failing.getSelectionIndex(), "failed selection leaves no pending state");
				assertEquals("after failure", failing.getText());
				failing.select(3);
				assertEquals(3, failing.getSelectionIndex());
				assertEquals("r3", failing.getText());
				failing.dispose();
			}

			CCombo disposed = new CCombo(shell, style);
			disposed.setItems(roots.toArray(String[]::new));
			disposed.select(0);
			disposed.addVerifyListener(event -> disposed.dispose());
			disposed.select(1);
			assertTrue(disposed.isDisposed(), "Verify disposal cannot enter the no-Modify fallback");
		}
	}

}
