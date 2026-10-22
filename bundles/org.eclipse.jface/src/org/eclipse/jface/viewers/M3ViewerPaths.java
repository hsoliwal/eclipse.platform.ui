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
package org.eclipse.jface.viewers;

import java.util.Arrays;
import java.util.List;
import java.util.RandomAccess;

import org.eclipse.jface.internal.m3.M3FirstIndex;
import org.eclipse.jface.internal.m3.M3IdentityIndex;
import org.eclipse.jface.internal.m3.M3ViewerMode;
import org.eclipse.jface.viewers.internal.ExpandableNode;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Widget;

/**
 * The internal fast paths of the viewers in {@code viewport} mode (see
 * {@link M3ViewerMode}). Each method is the target of one hook statement in a
 * viewer method. It either does the method's work with the same client-visible
 * calls in the same order and returns "handled", or returns "not handled" so
 * that the original code runs. Only internal search work differs: linear
 * {@code equals} and item scans become lookups in tables built once per call.
 * <p>
 * Design and the observable contract: {@code docs/m3/jface-viewport/DESIGN.md},
 * section 4 (paths A1 to A4).
 * </p>
 */
final class M3ViewerPaths {

	/**
	 * Below this many elements the original scans are as cheap as building a
	 * table, so the original code runs.
	 */
	static final int MIN_BATCH = 8;

	private static final ClassValue<Boolean> ON = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			Class<?> named = type;
			while (named.getSuperclass() != null && !named.getName().startsWith("org.eclipse.jface.viewers.")) { //$NON-NLS-1$
				named = named.getSuperclass();
			}
			return Boolean.valueOf(M3ViewerMode.enabled(named.getSimpleName()));
		}
	};

	private M3ViewerPaths() {
	}

	/**
	 * Whether {@code viewer} runs in {@code viewport} mode.
	 *
	 * @param viewer the viewer
	 * @return {@code true} in {@code viewport} mode
	 */
	static boolean on(Object viewer) {
		return M3ViewerMode.ANY && ON.get(viewer.getClass()).booleanValue();
	}

	/**
	 * A1: {@link AbstractTableViewer#setSelectionToWidget(List, boolean)} without
	 * reveal. The original selects, for each element, the first row whose
	 * element is equal, found by an {@code equals} scan from row 0 (O(rows x
	 * elements)). Here a first-position table over the rows is built once.
	 *
	 * @param viewer     the viewer
	 * @param list       the elements to select
	 * @param reveal     whether to reveal (the original handles it)
	 * @param notVirtual whether the viewer has no virtual manager
	 * @return {@code true} when handled
	 */
	static boolean selectRows(AbstractTableViewer viewer, List<?> list, boolean reveal, boolean notVirtual) {
		if (!on(viewer) || reveal || !notVirtual || list == null || list.size() < MIN_BATCH
				|| viewer.getItemsLimit() > 0 || list.contains(null)) {
			return false;
		}
		viewer.doDeselectAll();
		Item[] items = viewer.doGetItems();
		M3FirstIndex rows = new M3FirstIndex(items.length, viewer.getComparer());
		for (int i = 0; i < items.length; i++) {
			rows.add(items[i].getData(), i);
		}
		int[] indices = new int[list.size()];
		int count = 0;
		for (Object element : list) {
			int row = rows.first(element);
			if (row >= 0) {
				indices[count++] = row;
			}
		}
		viewer.doSelect(count < indices.length ? Arrays.copyOf(indices, count) : indices);
		return true;
	}

	/**
	 * A2: the non-virtual part of {@code AbstractTableViewer.internalRemove}. The
	 * original asks the table for the row of each removed item (a linear scan in
	 * SWT). Here an identity table over the items is built once. Removing the
	 * input and the item limit stay with the original code.
	 *
	 * @param viewer     the viewer
	 * @param elements   the elements to remove
	 * @param notVirtual whether the viewer has no virtual manager
	 * @return {@code true} when handled
	 */
	static boolean removeRows(AbstractTableViewer viewer, Object[] elements, boolean notVirtual) {
		if (!on(viewer) || !notVirtual || elements.length < MIN_BATCH) {
			return false;
		}
		Object input = viewer.getInput();
		for (Object element : elements) {
			if (viewer.equals(element, input)) {
				return false;
			}
		}
		if (viewer.getItemsLimit() > 0 && viewer.getLastElement() instanceof ExpandableNode) {
			return false;
		}
		M3IdentityIndex rows = new M3IdentityIndex(viewer.doGetItems());
		int[] indices = new int[elements.length];
		int count = 0;
		for (Object element : elements) {
			Widget w = viewer.findItem(element);
			if (w instanceof Item item) {
				viewer.disassociate(item);
				int row = rows.positionOf(item);
				indices[count++] = row >= 0 ? row : viewer.doIndexOf(item);
			}
		}
		viewer.doRemove(count < indices.length ? Arrays.copyOf(indices, count) : indices);
		if (viewer.doGetItemCount() == 0) {
			viewer.doRemoveAll();
		}
		return true;
	}

	/**
	 * A3: the disposal of stale child items in
	 * {@code AbstractTreeViewer.updateChildren}. The original closes each gap with
	 * {@code System.arraycopy} (O(items x disposed)). Here one two-pointer pass
	 * disposes the same items in the same order and compacts the survivors to the
	 * front of {@code items}.
	 *
	 * @param viewer   the viewer
	 * @param items    the child items, compacted in place
	 * @param children the new child elements
	 * @param count    how many items to dispose
	 * @return the count the original loop still has to dispose: 0 when handled
	 */
	static int disposeStale(AbstractTreeViewer viewer, Item[] items, CustomHashtable children, int count) {
		if (count <= 0 || !on(viewer)) {
			return count;
		}
		int write = 0;
		int read = 0;
		for (; read < items.length && count > 0; read++) {
			Item item = items[read];
			Object data = item.getData();
			if (data == null || !children.containsKey(data)) {
				if (data != null) {
					viewer.disassociate(item);
				}
				item.dispose();
				count--;
			} else {
				items[write++] = item;
			}
		}
		if (write < read) {
			System.arraycopy(items, read, items, write, items.length - read);
		}
		return 0;
	}

	/**
	 * A5: the existing non-hashed tree lookup walks materialized items in
	 * pre-order. Keep every getData, comparer and getChildren call in that
	 * exact order, but represent the recursion stack as primitive offsets
	 * over child arrays. Deep expanded trees no longer consume the Java
	 * call stack; no extra per-node entry object or cached stale index.
	 *
	 * @param viewer tree viewer
	 * @param parent first item to visit
	 * @param element sought element
	 * @return the first matching widget, or null
	 */
	static Widget findTreeItem(AbstractTreeViewer viewer, Item parent, Object element) {
		Object data = parent.getData();
		if (data != null && viewer.equals(data, element)) {
			return parent;
		}
		Item[][] levels = new Item[8][];
		int[] offsets = new int[8];
		levels[0] = viewer.getChildren(parent);
		int depth = 0;
		while (depth >= 0) {
			if (offsets[depth] == levels[depth].length) {
				levels[depth--] = null;
				continue;
			}
			Item item = levels[depth][offsets[depth]++];
			Object childData = item.getData();
			if (childData != null && viewer.equals(childData, element)) {
				return item;
			}
			Item[] children = viewer.getChildren(item);
			if (children.length > 0) {
				++depth;
				if (depth == levels.length) {
					levels = Arrays.copyOf(levels, levels.length << 1);
					offsets = Arrays.copyOf(offsets, offsets.length << 1);
				}
				levels[depth] = children;
				offsets[depth] = 0;
			}
		}
		return null;
	}

	/**
	 * A4: {@link AbstractListViewer#setSelectionToWidget(List, boolean)}. The
	 * original finds each element with a scan of the list map. Here a
	 * first-position table over the list map is built once.
	 *
	 * @param viewer  the viewer
	 * @param in      the elements to select
	 * @param reveal  whether to show the selection
	 * @param listMap the viewer's list of elements, in list order
	 * @return {@code true} when handled
	 */
	static boolean selectListRows(AbstractListViewer viewer, List<?> in, boolean reveal, List<Object> listMap) {
		if (!on(viewer) || in == null || in.size() < MIN_BATCH || in.contains(null)
				|| !(listMap instanceof RandomAccess)) {
			return false;
		}
		int size = listMap.size();
		M3FirstIndex rows = new M3FirstIndex(size, viewer.getComparer());
		for (int i = 0; i < size; i++) {
			rows.add(listMap.get(i), i);
		}
		int[] ixs = new int[in.size()];
		int count = 0;
		for (Object element : in) {
			int ix = rows.first(element);
			if (ix >= 0) {
				ixs[count++] = ix;
			}
		}
		viewer.listSetSelection(count < ixs.length ? Arrays.copyOf(ixs, count) : ixs);
		if (reveal) {
			viewer.listShowSelection();
		}
		return true;
	}
}
