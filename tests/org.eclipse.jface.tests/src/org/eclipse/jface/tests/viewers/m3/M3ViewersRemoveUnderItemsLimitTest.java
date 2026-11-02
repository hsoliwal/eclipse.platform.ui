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
 * A viewport delta that removes several siblings of a viewer with an items
 * limit (the workbench's large-view limit, set on every view through
 * {@code WorkbenchViewerSetup}). The stock remove refreshes the parent once per
 * removed element when a limit is set, which disposes the items of the other
 * elements of the batch and then fails on them ("Widget is disposed",
 * Problems view differential 2026-10-24). With no partial display under the
 * parent the plain removal is exact and keeps the siblings.
 */
public class M3ViewersRemoveUnderItemsLimitTest {

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

	private TreeViewer viewer(Node root, int limit) {
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
		viewer.setDisplayIncrementally(limit);
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
	public void removingSiblingsUnderAnItemsLimitKeepsTheOthers() {
		Assumptions.assumeTrue("viewport".equals(System.getProperty("jface.viewer.mode", "")), "viewport mode");
		Node root = new Node("root");
		Node parent = new Node("p");
		root.children.add(parent);
		Node a = new Node("a");
		Node b = new Node("b");
		Node c = new Node("c");
		Node d = new Node("d");
		parent.children.add(a);
		parent.children.add(b);
		parent.children.add(c);
		parent.children.add(d);
		TreeViewer viewer = viewer(root, 1000);
		viewer.setExpandedState(parent, true);
		assertEquals("a b c d", labels(viewer.getTree().getItems()[0].getItems()));
		parent.children.remove(b);
		parent.children.remove(c);
		// one batch with two siblings: the stock path refreshed the parent for "b" and then met the disposed item of "c"
		M3Viewers.apply(viewer, new M3ViewerDelta().remove(b).remove(c));
		assertEquals("a d", labels(viewer.getTree().getItems()[0].getItems()));
		// top-level siblings, the same way
		Node q = new Node("q");
		Node r = new Node("r");
		root.children.add(q);
		root.children.add(r);
		M3Viewers.apply(viewer, new M3ViewerDelta().add(root, q, 1).add(root, r, 2));
		assertEquals("p q r", labels(viewer.getTree().getItems()));
		root.children.remove(q);
		root.children.remove(r);
		M3Viewers.apply(viewer, new M3ViewerDelta().remove(q).remove(r));
		assertEquals("p", labels(viewer.getTree().getItems()));
		assertEquals("a d", labels(viewer.getTree().getItems()[0].getItems()));
	}
}
