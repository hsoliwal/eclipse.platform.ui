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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ILazyContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Stock-versus-M3 cost benchmark of JFace viewer operations, using public JFace API only so the same class runs
 * against the official JFace jar and against a build of this bundle. Each scenario builds a fresh viewer in an open
 * shell, warms up at a small size, then times one operation at the requested size, including the event
 * processing (paint) that follows it. A watchdog halts the JVM when one operation exceeds the budget, after
 * writing a TIMEOUT row; the driver script restarts at the next scenario.
 * <p>
 * Usage: {@code M3ViewerBench <out.tsv> <label> <n> <firstScenario> <budgetSeconds>}. Output rows:
 * {@code label, scenario, n, millis, note}.
 * </p>
 */
public final class M3ViewerBench {

	/** Model element with value equality, as most real models have. */
	public static final class Row {
		final int id;
		final String name;
		final List<Row> children = new ArrayList<>();

		Row(int id, String name) {
			this.id = id;
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Row r && r.id == id;
		}

		@Override
		public int hashCode() {
			return Integer.hashCode(id);
		}

		@Override
		public String toString() {
			return name;
		}
	}

	static final class TreeProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object input) {
			return ((Row) input).children.toArray();
		}

		@Override
		public Object[] getChildren(Object parent) {
			return ((Row) parent).children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return !((Row) element).children.isEmpty();
		}
	}

	static final class LazyProvider implements ILazyContentProvider {
		private final TableViewer viewer;
		private Row[] rows = new Row[0];

		LazyProvider(TableViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public void inputChanged(Viewer v, Object oldInput, Object newInput) {
			rows = newInput == null ? new Row[0] : (Row[]) newInput;
		}

		@Override
		public void updateElement(int index) {
			viewer.replace(rows[index], index);
		}
	}

	/** Keeps every element whose id is divisible by {@code modulus}. */
	static ViewerFilter keepMultiplesOf(int modulus) {
		return new ViewerFilter() {
			@Override
			public boolean select(Viewer v, Object parent, Object element) {
				return ((Row) element).id % modulus == 0;
			}
		};
	}

	static Row[] rows(int n, int base) {
		Row[] r = new Row[n];
		for (int i = 0; i < n; i++) {
			int id = base + i;
			// names in reverse order so that sorting moves every row
			r[i] = new Row(id, String.format("row %09d", n - i));
		}
		return r;
	}

	static Row[] every(Row[] rows, int step) {
		Row[] r = new Row[(rows.length + step - 1) / step];
		for (int i = 0; i < r.length; i++) {
			r[i] = rows[i * step];
		}
		return r;
	}

	static Row tree(int parents, int childrenEach) {
		Row root = new Row(-1, "root");
		int id = 0;
		for (int p = 0; p < parents; p++) {
			Row parent = new Row(id++, String.format("parent %07d", p));
			root.children.add(parent);
			for (int c = 0; c < childrenEach; c++) {
				parent.children.add(new Row(id++, String.format("child %07d.%07d", p, c)));
			}
		}
		return root;
	}

	interface Scenario {
		/** Builds the state, then returns the timed operation. */
		Runnable prepare(Shell shell, int n);
	}

	static final String[] NAMES = { "table.setInput", "table.refresh", "table.refresh.sel10", "table.setSelection10",
			"table.sort", "table.filter50", "table.reveal.last", "table.remove1pct", "table.update1pct",
			"table.add1pct.sorted", "vtable.lazy.setInput", "vtable.lazy.refresh", "vtable.lazy.setSelection1pct",
			"tree.flat.setInput", "tree.flat.refresh", "tree.flat.filter10", "tree.flat.setSelection10",
			"tree.2level.expandAll", "tree.2level.getExpandedElements", "tree.2level.refresh",
			"tree.2level.collapseAll", "table.getSelection.sel10", "table.refresh.sel10.hash" };

	static Scenario scenario(int index) {
		return switch (index) {
		case 0 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			Row[] r = rows(n, 0);
			return () -> v.setInput(r);
		};
		case 1 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			v.setInput(rows(n, 0));
			return v::refresh;
		};
		case 2 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			Row[] r = rows(n, 0);
			v.setInput(r);
			v.setSelection(new StructuredSelection(every(r, 10)));
			return v::refresh;
		};
		case 3 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			Row[] r = rows(n, 0);
			v.setInput(r);
			StructuredSelection s = new StructuredSelection(every(r, 10));
			return () -> v.setSelection(s);
		};
		case 4 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			v.setInput(rows(n, 0));
			return () -> v.setComparator(new ViewerComparator());
		};
		case 5 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			v.setInput(rows(n, 0));
			return () -> v.addFilter(keepMultiplesOf(2));
		};
		case 6 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			Row[] r = rows(n, 0);
			v.setInput(r);
			return () -> v.reveal(r[n - 1]);
		};
		case 7 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			Row[] r = rows(n, 0);
			v.setInput(r);
			Object[] gone = every(r, 100);
			return () -> v.remove(gone);
		};
		case 8 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			Row[] r = rows(n, 0);
			v.setInput(r);
			Object[] changed = every(r, 100);
			return () -> v.update(changed, null);
		};
		case 9 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			v.setComparator(new ViewerComparator());
			v.setInput(rows(n, 0));
			Object[] added = rows(Math.max(1, n / 100), n);
			return () -> v.add(added);
		};
		case 10 -> (shell, n) -> {
			TableViewer v = new TableViewer(shell, SWT.MULTI | SWT.VIRTUAL);
			v.setUseHashlookup(true);
			v.setLabelProvider(new LabelProvider());
			v.setContentProvider(new LazyProvider(v));
			Row[] r = rows(n, 0);
			return () -> {
				v.setInput(r);
				v.setItemCount(n);
			};
		};
		case 11 -> (shell, n) -> {
			TableViewer v = lazy(shell, n);
			return v::refresh;
		};
		case 12 -> (shell, n) -> {
			TableViewer v = lazy(shell, n);
			Row[] r = (Row[]) v.getInput();
			StructuredSelection s = new StructuredSelection(every(r, 100));
			return () -> v.setSelection(s);
		};
		case 13 -> (shell, n) -> {
			TreeViewer v = tree(shell);
			Row root = tree(n, 0);
			return () -> v.setInput(root);
		};
		case 14 -> (shell, n) -> {
			TreeViewer v = tree(shell);
			v.setInput(tree(n, 0));
			return v::refresh;
		};
		case 15 -> (shell, n) -> {
			TreeViewer v = tree(shell);
			v.setInput(tree(n, 0));
			return () -> v.addFilter(keepMultiplesOf(10));
		};
		case 16 -> (shell, n) -> {
			TreeViewer v = tree(shell);
			Row root = tree(n, 0);
			v.setInput(root);
			StructuredSelection s = new StructuredSelection(every(root.children.toArray(new Row[0]), 10));
			return () -> v.setSelection(s);
		};
		case 17 -> (shell, n) -> {
			TreeViewer v = tree(shell);
			int side = (int) Math.max(1, Math.round(Math.sqrt(n)));
			v.setInput(tree(side, side));
			return v::expandAll;
		};
		case 18 -> (shell, n) -> {
			TreeViewer v = expandedTree(shell, n);
			Object[][] sink = new Object[1][];
			return () -> sink[0] = v.getExpandedElements();
		};
		case 19 -> (shell, n) -> {
			TreeViewer v = expandedTree(shell, n);
			return v::refresh;
		};
		case 20 -> (shell, n) -> {
			TreeViewer v = expandedTree(shell, n);
			return v::collapseAll;
		};
		case 21 -> (shell, n) -> {
			TableViewer v = table(shell, SWT.MULTI);
			Row[] r = rows(n, 0);
			v.setInput(r);
			v.setSelection(new StructuredSelection(every(r, 10)));
			Object[] sink = new Object[1];
			return () -> sink[0] = v.getStructuredSelection();
		};
		case 22 -> (shell, n) -> {
			TableViewer v = new TableViewer(shell, SWT.MULTI);
			v.setUseHashlookup(true);
			v.setLabelProvider(new LabelProvider());
			v.setContentProvider(ArrayContentProvider.getInstance());
			Row[] r = rows(n, 0);
			v.setInput(r);
			v.setSelection(new StructuredSelection(every(r, 10)));
			return v::refresh;
		};
		default -> throw new IllegalArgumentException("scenario " + index);
		};
	}

	static TableViewer table(Shell shell, int style) {
		TableViewer v = new TableViewer(shell, style);
		v.setLabelProvider(new LabelProvider());
		v.setContentProvider(ArrayContentProvider.getInstance());
		return v;
	}

	static TableViewer lazy(Shell shell, int n) {
		TableViewer v = new TableViewer(shell, SWT.MULTI | SWT.VIRTUAL);
		v.setUseHashlookup(true);
		v.setLabelProvider(new LabelProvider());
		v.setContentProvider(new LazyProvider(v));
		v.setInput(rows(n, 0));
		v.setItemCount(n);
		return v;
	}

	static TreeViewer tree(Shell shell) {
		TreeViewer v = new TreeViewer(shell, SWT.MULTI);
		v.setLabelProvider(new LabelProvider());
		v.setContentProvider(new TreeProvider());
		return v;
	}

	static TreeViewer expandedTree(Shell shell, int n) {
		TreeViewer v = tree(shell);
		int side = (int) Math.max(1, Math.round(Math.sqrt(n)));
		v.setInput(tree(side, side));
		v.expandAll();
		return v;
	}

	/** The phase the UI thread is in, reported by the watchdog. */
	static volatile String phase = "warmup";

	static long measure(Display display, int scenario, int n) {
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(800, 600);
		shell.open();
		try {
			phase = "prepare";
			Runnable op = scenario(scenario).prepare(shell, n);
			shell.layout(true, true);
			drain(display);
			phase = "op";
			long start = System.nanoTime();
			op.run();
			drain(display);
			return System.nanoTime() - start;
		} finally {
			shell.dispose();
			drain(display);
		}
	}

	static void drain(Display display) {
		while (display.readAndDispatch()) {
			// process pending events, including paint
		}
	}

	static void append(Path out, String row) {
		try {
			Files.writeString(out, row + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE,
					StandardOpenOption.APPEND);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public static void main(String[] args) {
		Path out = Path.of(args[0]);
		String label = args[1];
		int n = Integer.parseInt(args[2]);
		int first = Integer.parseInt(args[3]);
		long budgetMillis = Long.parseLong(args[4]) * 1000L;
		int last = args.length > 5 ? Integer.parseInt(args[5]) : NAMES.length - 1;
		Display display = new Display();
		Consumer<String> row = s -> append(out, label + "\t" + s);
		for (int s = first; s <= last; s++) {
			String name = NAMES[s];
			int scenario = s;
			Thread watchdog = new Thread(() -> {
				try {
					Thread.sleep(budgetMillis);
					row.accept(name + "\t" + n + "\t-1\tTIMEOUT>" + budgetMillis / 1000 + "s in " + phase + "\t"
							+ scenario);
					Runtime.getRuntime().halt(3);
				} catch (InterruptedException e) {
					// finished in time
				}
			}, "m3-bench-watchdog");
			watchdog.setDaemon(true);
			watchdog.start();
			phase = "warmup";
			measure(display, s, Math.min(n, 2000));
			long nanos = measure(display, s, n);
			watchdog.interrupt();
			row.accept(name + "\t" + n + "\t" + nanos / 1_000_000 + "\tok\t" + scenario);
		}
		display.dispose();
	}
}
