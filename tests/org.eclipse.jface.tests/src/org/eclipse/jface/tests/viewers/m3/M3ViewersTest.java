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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.m3.M3ViewerDelta;
import org.eclipse.jface.viewers.m3.M3Viewers;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
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
		int[] refreshes = { 0 };
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
		assertEquals(0, refreshes[0]);
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
		TableViewer v = table();
		roots.add("later");
		M3Viewers.refreshLater(v, 10);
		M3Viewers.refreshLater(v, 10);
		waitUntil(() -> v.getTable().getItemCount() == 6);
		assertEquals("r0 r1 r2 r3 r4 later", texts(v));
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
}
