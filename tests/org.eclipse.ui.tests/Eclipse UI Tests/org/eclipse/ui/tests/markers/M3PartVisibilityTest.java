/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 or the Eclipse Public License 2.0.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.tests.markers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.internal.m3.M3PartVisibility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * The hidden viewport of a view (brief rule 24) driven by SWT's part visibility
 * (rule 25): a tree in a tab of a CTabFolder, the way the workbench stacks
 * views. Needs a Display.
 */
public class M3PartVisibilityTest {

	private Display display;
	private Shell shell;
	private CTabFolder folder;
	private Tree tree;
	private int shown;

	@BeforeEach
	public void open() {
		display = Display.getDefault();
		shell = new Shell(display);
		shell.setLayout(new FillLayout());
		folder = new CTabFolder(shell, SWT.NONE);
		tree = new Tree(tab("Problems"), SWT.VIRTUAL);
		tab("Tasks");
		folder.setSelection(0);
		shell.setSize(400, 300);
		shell.open();
		settle();
	}

	@AfterEach
	public void close() {
		shell.dispose();
	}

	private Composite tab(String name) {
		CTabItem item = new CTabItem(folder, SWT.NONE);
		item.setText(name);
		Composite page = new Composite(folder, SWT.NONE);
		page.setLayout(new FillLayout());
		item.setControl(page);
		return page;
	}

	private void settle() {
		while (display.readAndDispatch()) {
			// drain
		}
	}

	private M3PartVisibility watch() {
		return M3PartVisibility.watch(tree, () -> shown++);
	}

	@Test
	public void anotherTabHidesAndOnePatchRunsOnReturn() {
		M3PartVisibility watch = watch();
		assertFalse(watch.isHidden());
		folder.setSelection(1);
		assertTrue(watch.isHidden());
		watch.markDirty();
		watch.markDirty();
		assertEquals(0, shown);
		folder.setSelection(0);
		assertEquals(1, shown, "one window patch, inside the PartVisible event");
		assertEquals(1, watch.patches());
		assertFalse(watch.isDirty());
	}

	@Test
	public void aHiddenPeriodWithoutChangesPatchesNothing() {
		M3PartVisibility watch = watch();
		folder.setSelection(1);
		folder.setSelection(0);
		assertFalse(watch.isHidden());
		assertEquals(0, shown);
		assertEquals(0, watch.patches());
	}

	@Test
	public void aMinimizedShellHides() {
		M3PartVisibility watch = watch();
		shell.setMinimized(true);
		settle();
		assertTrue(watch.isHidden());
		watch.markDirty();
		shell.setMinimized(false);
		settle();
		assertEquals(1, shown);
	}

	@Test
	public void aCollapsedSashHides() {
		M3PartVisibility watch = watch();
		Composite page = tree.getParent();
		page.setSize(0, 0);
		assertTrue(watch.isHidden());
		watch.markDirty();
		page.setSize(200, 100);
		assertEquals(1, shown);
	}

	@Test
	public void aReparentOutOfViewHides() {
		M3PartVisibility watch = watch();
		Shell limbo = new Shell(display); // never opened, like the workbench's limbo shell
		Composite page = tree.getParent();
		page.setParent(limbo);
		assertTrue(watch.isHidden());
		watch.markDirty();
		page.setParent(folder);
		assertEquals(1, shown);
		limbo.dispose();
	}

	@Test
	public void aMissedEventIsHealedByTheNextQuery() {
		M3PartVisibility watch = watch();
		watch.dispose(); // no PartVisible events from here on
		folder.setSelection(1);
		watch.markDirty();
		folder.setSelection(0);
		assertEquals(0, shown);
		assertFalse(watch.isHidden());
		assertEquals(1, shown, "the pending patch runs before the caller's own patch");
	}

	@Test
	public void aDisposedViewportIsHidden() {
		M3PartVisibility watch = watch();
		tree.dispose();
		assertTrue(watch.isHidden());
		watch.dispose();
	}
}
