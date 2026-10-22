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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ICheckable;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.m3.M3ViewerDelta;
import org.eclipse.jface.viewers.m3.M3Viewers;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.FillLayout;
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
		long end = System.currentTimeMillis() + 10_000;
		while (!done.getAsBoolean() && System.currentTimeMillis() < end) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		assertTrue(done.getAsBoolean(), "timed out");
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
			for (String s : ((org.eclipse.swt.widgets.List) c).getItems()) {
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
}
