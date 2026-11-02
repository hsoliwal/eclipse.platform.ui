/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * Copyright (c) 2026 Hitesh Soliwal and Synexia contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
 * and is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Peter Shipton - original hashtable implementation
 *     Nick Edgar - element comparer support
 *     Hitesh Soliwal - M3 packed array storage and forwarding, 2026
 *******************************************************************************/
package org.eclipse.jface.internal.m3;

import java.util.Arrays;

import org.eclipse.jface.viewers.IElementComparer;

/**
 * JFace's comparer map stored in reference and primitive index lanes. The original
 * bucket order and cached hashes are retained; there is no entry object per row.
 * Zero is the end-of-chain sentinel; physical rows start at one.
 * <p>
 * Client callbacks can reenter the map. Retired rows keep their reference/hash/link
 * state until the outermost operation finishes, just as an old entry remains alive
 * on a Java stack. Only then are references cleared and rows returned to the free
 * chain. This class adds no thread-safety contract to the original viewer map.
 * </p>
 */
public final class M3ViewerMap {
	private static final Object[] EMPTY_OBJECTS = new Object[0];
	private static final int[] EMPTY_INTS = new int[0];

	private int[] buckets;
	private Object[] keys = EMPTY_OBJECTS;
	private Object[] values = EMPTY_OBJECTS;
	private int[] hashes = EMPTY_INTS;
	private int[] next = EMPTY_INTS;
	private int[] retired = EMPTY_INTS;
	private int retiredCount;
	private int highWater;
	private int free;
	private int depth;
	private int count;
	private int threshold;
	private final IElementComparer comparer;

	public M3ViewerMap(int capacity, IElementComparer comparer) {
		if (capacity < 0) {
			throw new IllegalArgumentException();
		}
		buckets = new int[capacity == 0 ? 1 : capacity];
		threshold = (int) (buckets.length * 0.75f);
		this.comparer = comparer;
	}

	public M3ViewerMap(M3ViewerMap table, IElementComparer comparer) {
		this(table.size() * 2, comparer);
		table.depth++;
		try {
			for (int i = table.buckets.length; --i >= 0;) {
				int entry = table.buckets[i];
				while (entry != 0) {
					put(table.keys[entry], table.values[entry]);
					entry = table.next[entry];
				}
			}
		} finally {
			table.leave();
		}
	}

	public IElementComparer getComparer() {
		return comparer;
	}

	public int size() {
		return count;
	}

	public boolean containsKey(Object key) {
		depth++;
		try {
			return entry(key) != 0;
		} finally {
			leave();
		}
	}

	public Object get(Object key) {
		depth++;
		try {
			int entry = entry(key);
			return entry == 0 ? null : values[entry];
		} finally {
			leave();
		}
	}

	private int entry(Object key) {
		int hash = hashCode(key);
		int entry = buckets[indexFor(hash)];
		while (entry != 0) {
			if (hashes[entry] == hash && keyEquals(key, keys[entry])) {
				return entry;
			}
			entry = next[entry];
		}
		return 0;
	}

	private int indexFor(int hash) {
		return (hash & 0x7FFFFFFF) % buckets.length;
	}

	private int hashCode(Object key) {
		return comparer == null ? key.hashCode() : comparer.hashCode(key);
	}

	private boolean keyEquals(Object a, Object b) {
		return comparer == null ? a.equals(b) : comparer.equals(a, b);
	}

	public Object put(Object key, Object value) {
		if (key == null || value == null) {
			throw new NullPointerException();
		}
		depth++;
		try {
			int hash = hashCode(key);
			int index = indexFor(hash);
			int entry = buckets[index];
			while (entry != 0 && !(hashes[entry] == hash && keyEquals(key, keys[entry]))) {
				entry = next[entry];
			}
			if (entry == 0) {
				if (++count > threshold) {
					rehash();
					index = indexFor(hash);
				}
				entry = allocate();
				keys[entry] = key;
				values[entry] = value;
				hashes[entry] = hash;
				next[entry] = buckets[index];
				buckets[index] = entry;
				return null;
			}
			Object result = values[entry];
			keys[entry] = key; // Preserve equal-key replacement identity (bug 30607).
			values[entry] = value;
			return result;
		} finally {
			leave();
		}
	}

	private int allocate() {
		if (free != 0) {
			int entry = free;
			free = next[entry];
			return entry;
		}
		int entry = highWater + 1;
		if (entry >= keys.length) {
			int old = keys.length;
			int capacity = Math.max(entry + 1, old == 0 ? 8 : old + (old >> 1));
			Object[] newKeys = Arrays.copyOf(keys, capacity);
			Object[] newValues = Arrays.copyOf(values, capacity);
			int[] newHashes = Arrays.copyOf(hashes, capacity);
			int[] newNext = Arrays.copyOf(next, capacity);
			keys = newKeys;
			values = newValues;
			hashes = newHashes;
			next = newNext;
		}
		highWater = entry;
		return entry;
	}

	private void rehash() {
		int length = buckets.length << 1;
		if (length == 0) {
			length = 1;
		}
		int[] newBuckets = new int[length];
		for (int i = buckets.length; --i >= 0;) {
			int entry = buckets[i];
			while (entry != 0) {
				int index = (hashes[entry] & 0x7FFFFFFF) % length;
				int following = next[entry];
				next[entry] = newBuckets[index];
				newBuckets[index] = entry;
				entry = following;
			}
		}
		buckets = newBuckets;
		threshold = (int) (buckets.length * 0.75f);
	}

	public Object remove(Object key) {
		if (count == 0) {
			return null;
		}
		depth++;
		try {
			return remove(key, hashCode(key));
		} finally {
			leave();
		}
	}

	public Object remove(Object key, int hash) {
		if (count == 0) {
			return null;
		}
		depth++;
		try {
			int last = 0;
			int index = indexFor(hash);
			int entry = buckets[index];
			while (entry != 0 && !(hashes[entry] == hash && keyEquals(key, keys[entry]))) {
				last = entry;
				entry = next[entry];
			}
			if (entry == 0) {
				return null;
			}
			if (retiredCount == retired.length) {
				retired = Arrays.copyOf(retired, Math.max(4, retiredCount << 1));
			}
			if (last == 0) {
				buckets[index] = next[entry];
			} else {
				next[last] = next[entry];
			}
			count--;
			retired[retiredCount++] = entry;
			return values[entry];
		} finally {
			leave();
		}
	}

	private void leave() {
		if (--depth != 0) {
			return;
		}
		for (int i = 0; i < retiredCount; i++) {
			int entry = retired[i];
			// Reentrant removal may retire the same old entry more than once.
			if (keys[entry] != null) {
				keys[entry] = null;
				values[entry] = null;
				next[entry] = free;
				free = entry;
			}
		}
		retiredCount = 0;
		// A completely emptied large map retains only its original bucket table.
		// Small lanes stay reusable for single-row churn without fresh arrays.
		if (count == 0 && highWater >= 64) {
			keys = EMPTY_OBJECTS;
			values = EMPTY_OBJECTS;
			hashes = EMPTY_INTS;
			next = EMPTY_INTS;
			retired = EMPTY_INTS;
			highWater = 0;
			free = 0;
		}
	}

	@Override
	public String toString() {
		if (size() == 0) {
			return "{}"; //$NON-NLS-1$
		}
		depth++;
		try {
			StringBuilder buffer = new StringBuilder();
			buffer.append('{');
			for (int i = buckets.length; --i >= 0;) {
				int entry = buckets[i];
				while (entry != 0) {
					buffer.append(keys[entry]);
					buffer.append('=');
					buffer.append(values[entry]);
					buffer.append(", "); //$NON-NLS-1$
					entry = next[entry];
				}
			}
			if (count > 0) {
				buffer.setLength(buffer.length() - 2);
			}
			buffer.append('}');
			return buffer.toString();
		} finally {
			leave();
		}
	}
}
