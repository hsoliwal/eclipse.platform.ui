/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 or the Eclipse Public License 2.0.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.jface.tests.viewers.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.m3.M3ViewerDelta;
import org.eclipse.jface.viewers.m3.M3Viewers;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * A viewport delta inserts a row at the position its model computed, also when
 * the viewer has a filter installed. The stock {@code AbstractTreeViewer.insert}
 * turns an indexed insert into {@code add} whenever a filter is installed, and
 * {@code add} appends when the viewer has no comparator; a filtered tree always
 * has its pattern filter installed, so every delta row of the Problems view
 * landed at the end of its category (differential 2026-10-24, seed 7).
 */
public class M3ViewersInsertUnderFilterTest {

	private static final class Node {
		final String label;
		final List<Node> children = new ArrayList<>();

		Node(String label) {
			this.label = label;
		}
	}

	private Shell shell;

	@BeforeEach
	public void setUp() {
		shell = new Shell(Display.getDefault());
		shell.setLayout(new FillLayout());
		shell.setSize(320, 180);
		shell.open();
	}

	@AfterEach
	public void tearDown() {
		if (!shell.isDisposed()) {
			shell.dispose();
		}
	}

	private TreeViewer viewer(Node root) {
		TreeViewer viewer = new TreeViewer(shell, SWT.SINGLE);
		viewer.setContentProvider(new ITreeContentProvider() {
			@Override
			public Object[] getElements(Object input) {
				return ((Node) input).children.toArray();
			}

			@Override
			public Object[] getChildren(Object element) {
				return ((Node) element).children.toArray();
			}

			@Override
			public Object getParent(Object element) {
				return null;
			}

			@Override
			public boolean hasChildren(Object element) {
				return !((Node) element).children.isEmpty();
			}
		});
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element) {
				return ((Node) element).label;
			}
		});
		// a filter is installed, as the pattern filter of a filtered tree always is; it keeps every row
		viewer.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer v, Object parent, Object element) {
				return true;
			}
		});
		viewer.setInput(root);
		return viewer;
	}

	private static String labels(TreeItem[] items) {
		StringBuilder b = new StringBuilder();
		for (TreeItem item : items) {
			b.append(item.getText()).append(' ');
		}
		return b.toString().trim();
	}

	@Test
	public void indexedInsertKeepsItsPositionUnderAFilter() {
		Assumptions.assumeTrue("viewport".equals(System.getProperty("jface.viewer.mode", "")), "viewport mode");
		Node root = new Node("root");
		Node a = new Node("a");
		Node c = new Node("c");
		root.children.add(a);
		root.children.add(c);
		TreeViewer viewer = viewer(root);
		Node b = new Node("b");
		root.children.add(1, b);
		M3Viewers.apply(viewer, new M3ViewerDelta().add(root, b, 1));
		assertEquals("a b c", labels(viewer.getTree().getItems()));
		// a child row under an expanded parent, the same way
		Node x = new Node("x");
		Node z = new Node("z");
		c.children.add(x);
		c.children.add(z);
		viewer.setExpandedState(c, true);
		Node y = new Node("y");
		c.children.add(1, y);
		M3Viewers.apply(viewer, new M3ViewerDelta().add(c, y, 1));
		assertEquals("x y z", labels(viewer.getTree().getItems()[2].getItems()));
	}

	@Test
	public void aViewerComparatorStillOrdersTheInsert() {
		Node root = new Node("root");
		Node a = new Node("a");
		Node c = new Node("c");
		root.children.add(a);
		root.children.add(c);
		TreeViewer viewer = viewer(root);
		viewer.setComparator(new ViewerComparator());
		Node b = new Node("b");
		root.children.add(0, b);
		// the model index says first; the viewer's own comparator (label order) decides, as stock does
		M3Viewers.apply(viewer, new M3ViewerDelta().add(root, b, 0));
		assertEquals("a b c", labels(viewer.getTree().getItems()));
	}
}
