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
package org.eclipse.jface.internal.m3;

import org.eclipse.jface.viewers.IElementComparer;

/**
 * The first position of each element in a sequence, under a viewer's element
 * equality. It answers "the first row whose element equals {@code e}", which
 * the viewers otherwise compute with a linear {@code equals} scan per element.
 * <p>
 * Equality is the viewer's: {@code comparer.equals(key, element)} and
 * {@code comparer.hashCode(element)} when a comparer is set, else
 * {@code key.equals(element)} and {@code element.hashCode()}. Hash codes must
 * be consistent with equality, as {@link Object#hashCode()} requires.
 * {@code null} elements are never found, as a {@code null} element never
 * equals a non-{@code null} key.
 * </p>
 * <p>
 * Lanes: an open-addressing table with linear probing (power-of-two capacity,
 * load at most one half) holding, per distinct element, its first element
 * object, its hash and its first position. There is no object per entry.
 * </p>
 */
public final class M3FirstIndex {

	private final IElementComparer comparer;
	private final Object[] keys;
	private final int[] hashes;
	private final int[] firsts;
	private final int mask;
	private final int shift;

	/**
	 * Creates an empty index for up to {@code expected} elements.
	 *
	 * @param expected the number of elements that will be added
	 * @param comparer the viewer's comparer, or {@code null}
	 */
	public M3FirstIndex(int expected, IElementComparer comparer) {
		int capacity = Integer.highestOneBit(Math.max(2, expected) * 2 - 1) << 1;
		this.comparer = comparer;
		keys = new Object[capacity];
		hashes = new int[capacity];
		firsts = new int[capacity];
		mask = capacity - 1;
		shift = 32 - Integer.numberOfTrailingZeros(capacity);
	}

	/**
	 * Records that {@code element} is at {@code position}, unless an equal
	 * element was recorded before. Positions must be added in ascending order.
	 *
	 * @param element  the element, ignored when {@code null}
	 * @param position its position
	 */
	public void add(Object element, int position) {
		if (element == null) {
			return;
		}
		int hash = hash(element);
		int slot = (hash * 0x9E3779B9) >>> shift;
		while (keys[slot] != null) {
			if (hashes[slot] == hash && same(element, keys[slot])) {
				return;
			}
			slot = (slot + 1) & mask;
		}
		keys[slot] = element;
		hashes[slot] = hash;
		firsts[slot] = position;
	}

	/**
	 * Returns the first position of an element equal to {@code key}.
	 *
	 * @param key the element to look for, not {@code null}
	 * @return the first position, or -1
	 */
	public int first(Object key) {
		int hash = hash(key);
		int slot = (hash * 0x9E3779B9) >>> shift;
		while (keys[slot] != null) {
			if (hashes[slot] == hash && same(key, keys[slot])) {
				return firsts[slot];
			}
			slot = (slot + 1) & mask;
		}
		return -1;
	}

	private int hash(Object element) {
		return comparer == null ? element.hashCode() : comparer.hashCode(element);
	}

	private boolean same(Object key, Object element) {
		return comparer == null ? key.equals(element) : comparer.equals(key, element);
	}
}

