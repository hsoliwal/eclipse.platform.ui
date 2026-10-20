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
import java.util.ArrayList;
import java.util.Random;
import java.util.zip.CRC32;

import org.eclipse.jface.viewers.AbstractListViewer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * Recorded differential of JFace viewers: seeded scripts of public-API operations
 * run against one viewer and log, after every step, the public answers, the
 * selection events, the provider, comparator and filter call sequences (count
 * and CRC), the widget state and a pixel CRC (DESIGN.md, section 3). The same
 * class runs against the official JFace jar (the recording) and against this
 * build in {@code original} and {@code viewport} mode; the logs must be
 * identical. {@code equals}/{@code hashCode} calls are not logged: they are not
 * part of the contract.
 * <p>
 * Usage: {@code M3ViewerDifferential <out-dir> <first-seed> <seed-count>}; one
 * file per variant and seed.
 * </p>
 */
public final class M3ViewerDifferential {

	/** The variants: viewer kind and configuration. */
	public static final String[] VARIANTS = { "table", "table-hash", "table-sorted-filtered", "tree", "tree-hash",
			"list" };

	/** Model element with value equality; duplicates share an id. */
	public static final class Row {
		final int id;
		final int copy;
		final String name;
		final java.util.List<Row> children = new ArrayList<>();

		Row(int id, int copy) {
			this.id = id;
			this.copy = copy;
			this.name = "row " + (id * 7919 % 1009) + "/" + id;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Row r && r.id == id;
		}

		@Override
		public int hashCode() {
			return id;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	/** Call log: one CRC and count per channel, reset per step. */
	static final class Calls {
		final CRC32 content = new CRC32();
		final CRC32 label = new CRC32();
		final CRC32 compare = new CRC32();
		final CRC32 select = new CRC32();
		final CRC32 events = new CRC32();
		int contentCount;
		int labelCount;
		int compareCount;
		int selectCount;
		int eventCount;

		static void add(CRC32 crc, String text) {
			crc.update(text.getBytes(StandardCharsets.UTF_8));
			crc.update('\n');
		}

		void content(String text) {
			contentCount++;
			add(content, text);
		}

		void label(String text) {
			labelCount++;
			add(label, text);
		}

		void compare(String text) {
			compareCount++;
			add(compare, text);
		}

		void select(String text) {
			selectCount++;
			add(select, text);
		}

		void event(String text) {
			eventCount++;
			add(events, text);
		}

		String drain() {
			String s = "content=" + contentCount + "/" + Long.toHexString(content.getValue()) + " label=" + labelCount
					+ "/" + Long.toHexString(label.getValue()) + " compare=" + compareCount + "/"
					+ Long.toHexString(compare.getValue()) + " select=" + selectCount + "/"
					+ Long.toHexString(select.getValue()) + " events=" + eventCount + "/"
					+ Long.toHexString(events.getValue());
			content.reset();
			label.reset();
			compare.reset();
			select.reset();
			events.reset();
			contentCount = labelCount = compareCount = selectCount = eventCount = 0;
			return s;
		}
	}

	static String id(Object o) {
		return o instanceof Row r ? r.id + (r.copy == 0 ? "" : "'" + r.copy) : String.valueOf(o);
	}

	final Random random;
	final Calls calls = new Calls();
	final java.util.List<String> log = new ArrayList<>();
	final String variant;
	java.util.List<Row> model = new ArrayList<>();
	Row root = new Row(-1, 0);
	int nextId;

	M3ViewerDifferential(String variant, long seed) {
		this.variant = variant;
		this.random = new Random(seed);
	}

	final class Labels extends LabelProvider {
		@Override
		public String getText(Object element) {
			calls.label("text " + id(element));
			return super.getText(element);
		}
	}

	final class TreeContent implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object input) {
			calls.content("elements");
			return ((Row) input).children.toArray();
		}

		@Override
		public Object[] getChildren(Object parent) {
			calls.content("children " + id(parent));
			return ((Row) parent).children.toArray();
		}

		@Override
		public Object getParent(Object element) {
			calls.content("parent " + id(element));
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			calls.content("has " + id(element));
			return !((Row) element).children.isEmpty();
		}
	}

	final class LoggingComparator extends ViewerComparator {
		@Override
		public int compare(Viewer viewer, Object e1, Object e2) {
			calls.compare(id(e1) + "?" + id(e2));
			return super.compare(viewer, e1, e2);
		}
	}

	final class OddFilter extends ViewerFilter {
		final int modulus;

		OddFilter(int modulus) {
			this.modulus = modulus;
		}

		@Override
		public boolean select(Viewer viewer, Object parent, Object element) {
			calls.select(id(parent) + ">" + id(element));
			return ((Row) element).id % modulus != 0;
		}
	}

	Row newRow() {
		Row r = new Row(nextId++, 0);
		if (variant.startsWith("tree") && random.nextInt(3) == 0) {
			int kids = random.nextInt(12);
			for (int i = 0; i < kids; i++) {
				r.children.add(new Row(nextId++, 0));
			}
		}
		return r;
	}

	/** A model of {@code n} rows with a few equal duplicates (first-match semantics). */
	void newModel(int n) {
		model = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			model.add(newRow());
		}
		for (int d = n / 20; d > 0; d--) {
			Row original = model.get(random.nextInt(model.size()));
			model.add(random.nextInt(model.size()), new Row(original.id, d));
		}
		root = new Row(-1, 0);
		root.children.addAll(model);
	}

	/** A random sample of model elements, sometimes as equal copies, sometimes including absent ones. */
	java.util.List<Object> sample(int size) {
		java.util.List<Object> s = new ArrayList<>();
		for (int i = 0; i < size && !model.isEmpty(); i++) {
			Row r = model.get(random.nextInt(model.size()));
			int pick = random.nextInt(10);
			s.add(pick == 0 ? new Row(r.id, 99) : pick == 1 ? new Row(nextId + 1000 + i, 0) : r);
		}
		return s;
	}

	StructuredViewer create(Shell shell) {
		StructuredViewer v;
		switch (variant) {
		case "tree", "tree-hash" -> {
			TreeViewer t = new TreeViewer(shell, SWT.MULTI);
			t.setContentProvider(new TreeContent());
			v = t;
		}
		case "list" -> {
			ListViewer l = new ListViewer(shell, SWT.MULTI | SWT.V_SCROLL);
			l.setContentProvider(ArrayContentProvider.getInstance());
			v = l;
		}
		default -> {
			TableViewer t = new TableViewer(shell, SWT.MULTI);
			t.setContentProvider(ArrayContentProvider.getInstance());
			v = t;
		}
		}
		v.setUseHashlookup(variant.endsWith("-hash"));
		v.setLabelProvider(new Labels());
		if (variant.equals("table-sorted-filtered")) {
			v.setComparator(new LoggingComparator());
			v.addFilter(new OddFilter(5));
		}
		v.addSelectionChangedListener(e -> calls.event("sel " + ids(e.getStructuredSelection().toList())));
		v.addPostSelectionChangedListener(e -> calls.event("post " + ids(e.getStructuredSelection().toList())));
		return v;
	}

	Object input() {
		return variant.startsWith("tree") ? root : model;
	}

	static String ids(java.util.List<?> list) {
		StringBuilder b = new StringBuilder("[");
		for (Object o : list) {
			b.append(id(o)).append(' ');
		}
		return b.append(']').toString();
	}

	/** Runs the script and returns the log. */
	java.util.List<String> run(Display display, int steps) {
		Shell shell = new Shell(display);
		shell.setLayout(new FillLayout());
		shell.setSize(420, 320);
		shell.open();
		try {
			newModel(40 + random.nextInt(400));
			StructuredViewer v = create(shell);
			v.setInput(input());
			record(display, v, "setInput");
			for (int step = 0; step < steps; step++) {
				String op;
				try {
					op = operate(v);
				} catch (RuntimeException e) {
					// a defect of the viewer under test is part of its behaviour: record it and stop this script
					log.add(step + " exception " + e.getClass().getName() + " " + e.getMessage());
					break;
				}
				record(display, v, step + " " + op);
			}
		} finally {
			shell.dispose();
		}
		return log;
	}

	String operate(StructuredViewer v) {
		int kind = random.nextInt(14);
		switch (kind) {
		case 0, 1, 2 -> {
			java.util.List<Object> s = sample(random.nextInt(40));
			boolean reveal = random.nextInt(4) == 0;
			v.setSelection(new StructuredSelection(s), reveal);
			return "setSelection " + s.size() + (reveal ? " reveal" : "");
		}
		case 3, 4 -> {
			v.refresh();
			return "refresh";
		}
		case 5 -> {
			int n = Math.min(model.size(), random.nextInt(30));
			java.util.List<Object> gone = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				gone.add(model.remove(random.nextInt(model.size())));
			}
			root.children.removeAll(gone);
			removeFromViewer(v, gone);
			return "remove " + n;
		}
		case 6 -> {
			int n = Math.min(model.size(), random.nextInt(60));
			for (int i = 0; i < n; i++) {
				Row r = model.remove(random.nextInt(model.size()));
				root.children.remove(r);
			}
			v.refresh();
			return "model-shrink+refresh " + n;
		}
		case 7 -> {
			int n = 1 + random.nextInt(20);
			java.util.List<Object> added = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				Row r = newRow();
				model.add(r);
				root.children.add(r);
				added.add(r);
			}
			addToViewer(v, added);
			return "add " + n;
		}
		case 8 -> {
			java.util.List<Object> s = sample(random.nextInt(20));
			v.update(s.toArray(), null);
			return "update " + s.size();
		}
		case 9 -> {
			if (v.getComparator() == null) {
				v.setComparator(new LoggingComparator());
				return "sort on";
			}
			v.setComparator(null);
			return "sort off";
		}
		case 10 -> {
			if (v.getFilters().length == 0) {
				v.addFilter(new OddFilter(2 + random.nextInt(4)));
				return "filter on";
			}
			v.resetFilters();
			return "filter off";
		}
		case 11 -> {
			if (v instanceof TreeViewer t) {
				t.expandAll();
				return "expandAll";
			}
			v.refresh(false);
			return "refresh(false)";
		}
		case 12 -> {
			if (v instanceof TreeViewer t) {
				t.collapseAll();
				return "collapseAll";
			}
			java.util.List<Object> s = sample(1);
			if (!s.isEmpty()) {
				v.reveal(s.get(0));
			}
			return "reveal";
		}
		default -> {
			newModel(random.nextInt(300));
			v.setInput(input());
			return "setInput";
		}
		}
	}

	void removeFromViewer(StructuredViewer v, java.util.List<Object> gone) {
		if (v instanceof TableViewer t) {
			t.remove(gone.toArray());
		} else if (v instanceof TreeViewer t) {
			t.remove(gone.toArray());
		} else if (v instanceof AbstractListViewer l) {
			l.remove(gone.toArray());
		}
	}

	void addToViewer(StructuredViewer v, java.util.List<Object> added) {
		if (v instanceof TableViewer t) {
			t.add(added.toArray());
		} else if (v instanceof TreeViewer t) {
			t.add(root, added.toArray());
		} else if (v instanceof AbstractListViewer l) {
			l.add(added.toArray());
		}
	}

	void record(Display display, StructuredViewer v, String op) {
		while (display.readAndDispatch()) {
			// let paint and deferred events run
		}
		StringBuilder b = new StringBuilder(op);
		b.append(" | sel=").append(ids(v.getStructuredSelection().toList()));
		b.append(" | ").append(calls.drain());
		b.append(" | ").append(widgetState(v.getControl()));
		b.append(" | px=").append(pixels(v.getControl()));
		log.add(b.toString());
	}

	static String widgetState(Control c) {
		StringBuilder b = new StringBuilder();
		if (c instanceof Table t) {
			b.append("rows=").append(t.getItemCount()).append(" top=").append(t.getTopIndex()).append(" sel=");
			for (int i : t.getSelectionIndices()) {
				b.append(i).append(',');
			}
			b.append(" data=").append(dataCrc(t.getItems()));
		} else if (c instanceof Tree t) {
			b.append("roots=").append(t.getItemCount()).append(" sel=").append(t.getSelectionCount()).append(" data=")
					.append(treeCrc(t.getItems()));
		} else if (c instanceof org.eclipse.swt.widgets.List l) {
			b.append("rows=").append(l.getItemCount()).append(" top=").append(l.getTopIndex()).append(" sel=");
			for (int i : l.getSelectionIndices()) {
				b.append(i).append(',');
			}
			CRC32 crc = new CRC32();
			for (String s : l.getItems()) {
				Calls.add(crc, s);
			}
			b.append(" items=").append(Long.toHexString(crc.getValue()));
		}
		return b.toString();
	}

	static String dataCrc(Item[] items) {
		CRC32 crc = new CRC32();
		for (Item item : items) {
			Calls.add(crc, id(item.getData()) + "=" + item.getText());
		}
		return Long.toHexString(crc.getValue());
	}

	static String treeCrc(TreeItem[] items) {
		CRC32 crc = new CRC32();
		treeCrc(items, crc);
		return Long.toHexString(crc.getValue());
	}

	static void treeCrc(TreeItem[] items, CRC32 crc) {
		for (TreeItem item : items) {
			Calls.add(crc, id(item.getData()) + "=" + item.getText() + (item.getExpanded() ? "+" : ""));
			if (item.getExpanded()) {
				treeCrc(item.getItems(), crc);
			}
		}
	}

	static String pixels(Control c) {
		org.eclipse.swt.graphics.Point size = c.getSize();
		if (size.x <= 0 || size.y <= 0) {
			return "-";
		}
		Image image = new Image(c.getDisplay(), size.x, size.y);
		GC gc = new GC(image);
		try {
			c.print(gc);
		} finally {
			gc.dispose();
		}
		ImageData data = image.getImageData();
		image.dispose();
		CRC32 crc = new CRC32();
		crc.update(data.data);
		return Long.toHexString(crc.getValue());
	}

	/** Runs one variant and seed. */
	public static java.util.List<String> script(Display display, String variant, long seed, int steps) {
		return new M3ViewerDifferential(variant, seed).run(display, steps);
	}

	public static void main(String[] args) throws IOException {
		Path out = Path.of(args[0]);
		int first = Integer.parseInt(args[1]);
		int count = Integer.parseInt(args[2]);
		int steps = args.length > 3 ? Integer.parseInt(args[3]) : 60;
		Files.createDirectories(out);
		Display display = new Display();
		for (String variant : VARIANTS) {
			for (int seed = first; seed < first + count; seed++) {
				Files.write(out.resolve(variant + "-" + seed + ".log"), script(display, variant, seed, steps),
						StandardCharsets.UTF_8);
			}
		}
		display.dispose();
	}
}
