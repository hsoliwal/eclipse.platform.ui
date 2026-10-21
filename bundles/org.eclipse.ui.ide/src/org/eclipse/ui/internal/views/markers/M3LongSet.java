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
package org.eclipse.ui.internal.views.markers;

import java.util.function.LongConsumer;

/**
 * A set of marker ids: one {@code long[]} open-addressing lane with linear
 * probing (power-of-two capacity, load at most one half). No boxing, no entry
 * objects. Id 0 is stored in a separate flag.
 */
final class M3LongSet {

	private long[] keys;
	private int shift;
	private int size;
	private boolean zero;

	/**
	 * @param expected the number of ids that will be added
	 */
	M3LongSet(int expected) {
		int capacity = Integer.highestOneBit(Math.max(2, expected) * 2 - 1) << 1;
		keys = new long[capacity];
		shift = 64 - Integer.numberOfTrailingZeros(capacity);
	}

	void add(long id) {
		if (id == 0) {
			zero = true;
			return;
		}
		int mask = keys.length - 1;
		int i = slot(id);
		while (keys[i] != 0) {
			if (keys[i] == id) {
				return;
			}
			i = (i + 1) & mask;
		}
		keys[i] = id;
		if (++size << 1 > keys.length) {
			grow();
		}
	}

	boolean contains(long id) {
		if (id == 0) {
			return zero;
		}
		int mask = keys.length - 1;
		for (int i = slot(id); keys[i] != 0; i = (i + 1) & mask) {
			if (keys[i] == id) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param action receives every id of the set
	 */
	void forEach(LongConsumer action) {
		if (zero) {
			action.accept(0);
		}
		for (long id : keys) {
			if (id != 0) {
				action.accept(id);
			}
		}
	}

	/** @return whether no id was added */
	boolean isEmpty() {
		return size == 0 && !zero;
	}

	/* doubles the table once it is half full (sets that accumulate, e.g. while the view is hidden) */
	private void grow() {
		long[] old = keys;
		keys = new long[old.length << 1];
		shift--;
		int mask = keys.length - 1;
		for (long id : old) {
			if (id != 0) {
				int i = slot(id);
				while (keys[i] != 0) {
					i = (i + 1) & mask;
				}
				keys[i] = id;
			}
		}
	}

	private int slot(long id) {
		return (int) ((id * 0x9E3779B97F4A7C15L) >>> shift);
	}
}
