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
package org.eclipse.jface.viewers.m3;

import java.util.Arrays;

/**
 * A change to the elements shown by a structured viewer, expressed as a
 * sequence of operations: add (at an index or in comparator order), remove,
 * update (labels) and refresh (structure of one subtree). A view that receives
 * an event stream (resource deltas, marker deltas, model events) translates
 * each event into a delta and hands it to {@link M3Viewers#apply} or
 * {@link M3Viewers#post}, instead of refreshing the whole viewer.
 * <p>
 * The operations are kept in lanes (one array per field, no object per
 * operation) and applied in the order they were added. Consecutive operations
 * of the same kind are applied as one batch call to the viewer.
 * </p>
 * <p>
 * This class is not thread-safe; build a delta on one thread, then pass it on.
 * </p>
 *
 * @since 3.43 (M3 enhanced API)
 */
public final class M3ViewerDelta {

	/** Operation kind: add an element under a parent. */
	public static final byte ADD = 1;
	/** Operation kind: remove an element. */
	public static final byte REMOVE = 2;
	/** Operation kind: update the labels of an element. */
	public static final byte UPDATE = 3;
	/** Operation kind: refresh the structure below an element (or the input). */
	public static final byte REFRESH = 4;

	private byte[] kinds = new byte[8];
	private Object[] elements = new Object[8];
	private Object[] parents = new Object[8];
	private int[] indexes = new int[8];
	private String[][] properties = new String[8][];
	private int size;

	/**
	 * Adds {@code element} as a child of {@code parent}.
	 *
	 * @param parent  the parent element or tree path; for tables and lists the
	 *                input (it is ignored there)
	 * @param element the new element
	 * @param index   the position among the parent's children, or -1 to let the
	 *                viewer place it (comparator order, else the end)
	 * @return this delta
	 */
	public M3ViewerDelta add(Object parent, Object element, int index) {
		return push(ADD, element, parent, index, null);
	}

	/**
	 * Removes {@code element} from the viewer.
	 *
	 * @param element the element (or tree path) that left the model
	 * @return this delta
	 */
	public M3ViewerDelta remove(Object element) {
		return push(REMOVE, element, null, -1, null);
	}

	/**
	 * Updates the labels of {@code element}.
	 *
	 * @param element    the element whose presentation changed
	 * @param properties the changed properties, or {@code null} for all (see
	 *                   {@code StructuredViewer.update(Object, String[])})
	 * @return this delta
	 */
	public M3ViewerDelta update(Object element, String[] properties) {
		return push(UPDATE, element, null, -1, properties);
	}

	/**
	 * Refreshes the structure below {@code element}: the fallback for changes
	 * the source cannot express as adds and removes.
	 *
	 * @param element the element (or tree path) to refresh, or {@code null} for
	 *                the whole viewer
	 * @return this delta
	 */
	public M3ViewerDelta refresh(Object element) {
		return push(REFRESH, element, null, -1, null);
	}

	/**
	 * Appends the operations of {@code other} to this delta, so that deltas
	 * posted in a burst are applied as one.
	 *
	 * @param other the later delta
	 * @return this delta
	 */
	public M3ViewerDelta append(M3ViewerDelta other) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		// Capture the source size once: self-append must copy exactly one
		// original run rather than extending the loop while it is traversed.
		int count = other.size;
		if (count == 0) {
			return this;
		}
		int previous = size;
		int required = Math.addExact(previous, count);
		if (required > kinds.length) {
			int capacity = Math.max(required, (int) Math.min((long) Integer.MAX_VALUE - 8, 2L * kinds.length));
			kinds = Arrays.copyOf(kinds, capacity);
			elements = Arrays.copyOf(elements, capacity);
			parents = Arrays.copyOf(parents, capacity);
			indexes = Arrays.copyOf(indexes, capacity);
			properties = Arrays.copyOf(properties, capacity);
		}
		// Parent-owned physical lanes; one bulk copy per lane, including
		// the same-array case handled by System.arraycopy.
		System.arraycopy(other.kinds, 0, kinds, previous, count);
		System.arraycopy(other.elements, 0, elements, previous, count);
		System.arraycopy(other.parents, 0, parents, previous, count);
		System.arraycopy(other.indexes, 0, indexes, previous, count);
		System.arraycopy(other.properties, 0, properties, previous, count);
		size = required;
		return this;
	}

	/** @return the number of operations */
	public int size() {
		return size;
	}

	/** @return whether the delta has no operation */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * @param i the operation index
	 * @return the kind of operation {@code i}: {@link #ADD}, {@link #REMOVE},
	 *         {@link #UPDATE} or {@link #REFRESH}
	 */
	public byte kind(int i) {
		check(i);
		return kinds[i];
	}

	/**
	 * @param i the operation index
	 * @return the element of operation {@code i}
	 */
	public Object element(int i) {
		check(i);
		return elements[i];
	}

	/**
	 * @param i the operation index
	 * @return the parent of an {@link #ADD}, else {@code null}
	 */
	public Object parent(int i) {
		check(i);
		return parents[i];
	}

	/**
	 * @param i the operation index
	 * @return the index of an {@link #ADD}, else -1
	 */
	public int index(int i) {
		check(i);
		return indexes[i];
	}

	/**
	 * @param i the operation index
	 * @return the properties of an {@link #UPDATE}, else {@code null}
	 */
	public String[] properties(int i) {
		check(i);
		return properties[i];
	}

	private void check(int i) {
		if (i < 0 || i >= size) {
			throw new IndexOutOfBoundsException(i);
		}
	}

	private M3ViewerDelta push(byte kind, Object element, Object parent, int index, String[] props) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		if (element == null && kind != REFRESH) {
			throw new IllegalArgumentException("element must not be null"); //$NON-NLS-1$
		}
		if (size == kinds.length) {
			int capacity = size << 1;
			kinds = Arrays.copyOf(kinds, capacity);
			elements = Arrays.copyOf(elements, capacity);
			parents = Arrays.copyOf(parents, capacity);
			indexes = Arrays.copyOf(indexes, capacity);
			properties = Arrays.copyOf(properties, capacity);
		}
		kinds[size] = kind;
		elements[size] = element;
		parents[size] = parent;
		indexes[size] = index;
		properties[size] = props;
		size++;
		return this;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("M3ViewerDelta["); //$NON-NLS-1$ //FLLDCIM: MARK — M14 M3STRING_COMPOSITE NEW
		for (int i = 0; i < size; i++) { //FLLDCIM: MARK — M32 LOOP_TO_LANE LOOP
			b.append(i == 0 ? "" : ", ").append(switch (kinds[i]) { //$NON-NLS-1$ //$NON-NLS-2$
			case ADD -> "add"; //$NON-NLS-1$
			case REMOVE -> "remove"; //$NON-NLS-1$
			case UPDATE -> "update"; //$NON-NLS-1$
			default -> "refresh"; //$NON-NLS-1$
			}).append(' ').append(elements[i]);
		}
		return b.append(']').toString();
	}
}
