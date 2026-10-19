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

/**
 * Object identity to position, for objects such as widget items whose position
 * is otherwise found by a linear scan. Lanes: an open-addressing table with
 * linear probing over {@link System#identityHashCode(Object)} (power-of-two
 * capacity, load at most one half). No object per entry; no {@code equals}
 * or {@code hashCode} calls on the keys.
 */
public final class M3IdentityIndex {

	private final Object[] keys;
	private final int[] positions;
	private final int mask;
	private final int shift;

	/**
	 * Indexes {@code objects}: each object maps to its first position.
	 *
	 * @param objects the objects; {@code null} entries are skipped
	 */
	public M3IdentityIndex(Object[] objects) {
		int capacity = Integer.highestOneBit(Math.max(2, objects.length) * 2 - 1) << 1;
		keys = new Object[capacity];
		positions = new int[capacity];
		mask = capacity - 1;
		shift = 32 - Integer.numberOfTrailingZeros(capacity);
		for (int i = 0; i < objects.length; i++) {
			put(objects[i], i);
		}
	}

	private void put(Object key, int position) {
		if (key == null) {
			return;
		}
		int slot = slot(key);
		while (keys[slot] != null) {
			if (keys[slot] == key) {
				return;
			}
			slot = (slot + 1) & mask;
		}
		keys[slot] = key;
		positions[slot] = position;
	}

	/**
	 * Returns the first position of {@code key}.
	 *
	 * @param key the object
	 * @return its position, or -1 when it was not indexed
	 */
	public int positionOf(Object key) {
		if (key == null) {
			return -1;
		}
		int slot = slot(key);
		while (keys[slot] != null) {
			if (keys[slot] == key) {
				return positions[slot];
			}
			slot = (slot + 1) & mask;
		}
		return -1;
	}

	private int slot(Object key) {
		return (System.identityHashCode(key) * 0x9E3779B9) >>> shift;
	}
}
