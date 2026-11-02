/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Tom Schindl - bug 151205
 *     Lucas Bullen (Red Hat Inc.) - Bug 493357
 *******************************************************************************/
package org.eclipse.jface.tests.viewers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class VirtualLazyTreeViewerTest extends TreeViewerTest {

	protected volatile int setDataCalls = 0;

	@Override
	protected StructuredViewer createViewer(Composite parent) {
		Tree tree = new Tree(parent, SWT.VIRTUAL);
		tree.addListener(SWT.SetData, unused -> setDataCalls++);
		fTreeViewer = new TreeViewer(tree);
		fTreeViewer.setContentProvider(new TestModelLazyTreeContentProvider((TreeViewer) fTreeViewer));
		return fTreeViewer;
	}

	@BeforeEach
	@Override
	public void setUp() {
		super.setUp();
		fShell.forceActive();
		// process events because the content provider uses an asyncExec to set the item
		// count of the tree
		processEvents();
	}

	@Override
	protected void setInput() {
		super.setInput();
	}

	@Test
	public void testLeafIsExpandable() {
		TestElement leafElement = fRootElement.getChildAt(2).getChildAt(3).getChildAt(2);
		assertEquals(0, leafElement.getChildCount());
		assertFalse(fTreeViewer.isExpandable(leafElement));
	}

	@Test
	public void testRootIsExpandable() {
		TestElement rootElement = fRootElement.getChildAt(2);
		assertTrue(rootElement.getChildCount() > 0);
		assertTrue(fTreeViewer.isExpandable(rootElement));
	}

	@Test
	public void testNodeIsExpandable() {
		TestElement nodeElement = fRootElement.getChildAt(2).getChildAt(3);
		assertTrue(nodeElement.getChildCount() > 0);
		assertTrue(fTreeViewer.isExpandable(nodeElement));
	}

	@Disabled("Test leads to infinite loop. Duplicate children are a bad idea in virtual trees.")
	@Override
	public void testRefreshWithDuplicateChild() {
	}

	@Disabled("Test leads to infinite loop. Cycles are a bad idea in virtual trees.")
	@Override
	public void testSetExpandedWithCycle() {
	}

	@Disabled("no need to test since virtual trees do not support filtering")
	@Override
	public void testFilterExpanded() {
	}

	@Disabled("no need to test since virtual trees do not support filtering")
	@Override
	public void testFilter() {
	}

	@Disabled("no need to test since virtual trees do not support filtering")
	@Override
	public void testSetFilters() {
	}

	@Disabled("no need to test since virtual trees do not support filtering")
	@Override
	public void testInsertSiblingWithFilterFiltered() {
	}

	@Disabled("no need to test since virtual trees do not support filtering")
	@Override
	public void testInsertSiblingWithFilterNotFiltered() {
	}

	@Disabled("no need to test since virtual trees do not support sorting")
	@Override
	public void testInsertSiblingWithSorter() {
	}

	@Disabled("no need to test since virtual trees do not support filtering")
	@Override
	public void testRenameWithFilter() {
	}

	@Disabled("no need to test since virtual trees do not support sorting")
	@Override
	public void testRenameWithSorter() {
	}

	@Disabled("no need to test since virtual trees do not support sorting")
	@Override
	public void testSorter() {
	}

	@Disabled("test is not relevant for lazy tree viewer")
	@Override
	public void testChildIsNotDuplicatedWhenCompareEquals() {
	}

	@Test
	public void testExpandedStateDelegatesToSwtWithoutWalkingColdVirtualSiblings() throws Exception {
		try {
			Tree.class.getMethod("getExpandedItems");
			Tree.class.getMethod("setExpandedItems", org.eclipse.swt.widgets.TreeItem[].class);
		} catch (NoSuchMethodException unavailable) {
			assumeTrue(false, "requires SWT widget-owned expanded-item state");
		}

		TestElement root = TestElement.createModel(2, 200);
		TestElement parent = root.getChildAt(150);
		TestElement child = parent.getChildAt(175);
		// Expansion requires a nonleaf; keep the 200-sibling model otherwise unchanged.
		child.basicAddChild();
		fTreeViewer.setInput(root);
		processEvents();

		int before = setDataCalls;
		fTreeViewer.setExpandedElements(parent, child);
		processEvents();
		int afterSet = setDataCalls;
		assertTrue(afterSet - before < 32,
				"restoring two expansion paths must not request hundreds of cold virtual siblings");
		assertArrayEquals(new Object[] {parent, child}, fTreeViewer.getExpandedElements());
		assertEquals(afterSet, setDataCalls,
				"enumerating expanded elements must use SWT item state without new SetData");

		fTreeViewer.setExpandedElements(child);
		processEvents();
		assertFalse(fTreeViewer.getExpandedState(parent));
		assertTrue(fTreeViewer.getExpandedState(child),
				"a hidden descendant may retain expanded state below a collapsed ancestor");
		int hiddenSetData = setDataCalls;
		assertArrayEquals(new Object[] {child}, fTreeViewer.getExpandedElements());
		assertEquals(hiddenSetData, setDataCalls,
				"hidden expanded state enumeration must not traverse cold siblings");

		TreePath childPath = new TreePath(new Object[] {parent, child});
		fTreeViewer.setExpandedTreePaths(childPath);
		processEvents();
		int pathSetData = setDataCalls;
		assertArrayEquals(new TreePath[] {childPath}, fTreeViewer.getExpandedTreePaths());
		assertEquals(pathSetData, setDataCalls,
				"tree-path expansion state must reuse the same SWT-owned item state");
	}

	@Test
	@Override
	public void testExpandCollapseToLevel() {
		super.testExpandCollapseToLevel();
	}

	// Temporary overrides for bug 347491
	@Test
	@Override
	public void testRefreshWithAddedChildren() {
		assumeFalse(disableTestsBug347491, "test disabled because of bug 347491");
		assertTrue(setDataCalls > 0, "data must have been set");
		super.testRefreshWithAddedChildren();
	}

	@Test
	@Override
	public void testDeleteSibling() {
		assumeFalse(disableTestsBug347491, "test disabled because of bug 347491");
		assertTrue(setDataCalls > 0, "data must have been set");
		super.testDeleteSibling();
	}

	@Test
	@Override
	public void testInsertSibling() {
		assumeFalse(disableTestsBug347491, "test disabled because of bug 347491");
		assertTrue(setDataCalls > 0, "data must have been set");
		super.testInsertSibling();
	}

	@Test
	@Override
	public void testInsertSiblings() {
		assumeFalse(disableTestsBug347491, "test disabled because of bug 347491");
		assertTrue(setDataCalls > 0, "data must have been set");
		super.testInsertSiblings();
	}

	@Test
	@Override
	public void testSetInput() {
		assumeFalse(disableTestsBug347491, "test disabled because of bug 347491");
		assertTrue(setDataCalls > 0, "data must have been set");
		super.testSetInput();
	}

	@Test
	@Override
	public void testSomeChildrenChanged() {
		assumeFalse(disableTestsBug347491, "test disabled because of bug 347491");
		assertTrue(setDataCalls > 0, "data must have been set");
		super.testSomeChildrenChanged();
	}

	@Test
	@Override
	public void testWorldChanged() {
		assumeFalse(disableTestsBug347491, "test disabled because of bug 347491");
		assertTrue(setDataCalls > 0, "data must have been set");
		super.testWorldChanged();
	}

	@Test
	@Override
	public void testContains() {
		assumeFalse(disableTestsBug347491, "test disabled because of bug 347491");
		assertTrue(setDataCalls > 0, "data must have been set");
		super.testContains();
	}

	@Test
	@Override
	public void testAutoExpandOnSingleChildThroughEvent() {
		assumeFalse(disableTestsBug347491, "test disabled because of bug 347491");
		assertTrue(setDataCalls > 0, "data must have been set");
		super.testAutoExpandOnSingleChildThroughEvent();
	}

}
