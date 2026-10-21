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

import java.util.Arrays;

/**
 * The rows of one category in sort order: an order-statistic list of
 * {@code int} slots kept as sorted blocks of at most {@link #BLOCK} slots.
 * <p>
 * Insert and remove cost a binary search over the blocks, a binary search in
 * one block and one {@code arraycopy} of at most {@link #BLOCK} ints, whatever
 * the size of the category: a marker change never moves the other rows of a
 * category of 10^6 markers. {@link #get(int)} and {@link #indexOf(int)} walk
 * the block lengths (n / {@link #BLOCK} additions). Catalogue: order-statistic
 * list over blocked sorted arrays (rule 21).
 * </p>
 * <p>
 * The comparator must be a total order on the slots it is given: it returns 0
 * only for the same slot. Not thread-safe.
 * </p>
 */
public final class M3SlotOrder {

	/** Compares two slots by the lanes of the model that owns them. */
	@FunctionalInterface
	public interface SlotComparator {
		/**
		 * @param a a slot
		 * @param b a slot
		 * @return negative, zero (same slot only) or positive
		 */
		int compare(int a, int b);
	}

	/** The longest block; a full block splits in two halves. */
	static final int BLOCK = 1024;

	private final SlotComparator order;
	private int[][] blocks = new int[4][];
	private int[] lengths = new int[4];
	private int blockCount;
	private int size;

	/**
	 * @param order the total order of the slots
	 */
	public M3SlotOrder(SlotComparator order) {
		this.order = order;
	}

	/** @return the number of slots */
	public int size() {
		return size;
	}

	/**
	 * @param index a position, 0 to {@code size() - 1}
	 * @return the slot at {@code index}
	 */
	public int get(int index) {
		if (index < 0 || index >= size) {
			throw new IndexOutOfBoundsException(index);
		}
		int b = 0;
		while (index >= lengths[b]) {
			index -= lengths[b++];
		}
		return blocks[b][index];
	}

	/**
	 * Copies the first slots in order.
	 *
	 * @param limit the most slots to copy
	 * @param out   receives the slots; at least {@code min(limit, size())} long
	 * @return the number of slots copied
	 */
	public int head(int limit, int[] out) {
		int n = Math.min(limit, size);
		int copied = 0;
		for (int b = 0; copied < n; b++) {
			int take = Math.min(lengths[b], n - copied);
			System.arraycopy(blocks[b], 0, out, copied, take);
			copied += take;
		}
		return copied;
	}

	/**
	 * @param slot a slot
	 * @return its position, or -1 when it is not in the list
	 */
	public int indexOf(int slot) {
		int b = blockOf(slot);
		if (b == blockCount) {
			return -1;
		}
		int i = search(blocks[b], lengths[b], slot);
		if (i < 0) {
			return -1;
		}
		int index = i;
		for (int k = 0; k < b; k++) {
			index += lengths[k];
		}
		return index;
	}

	/**
	 * Inserts a slot at its sorted position.
	 *
	 * @param slot a slot that is not in the list
	 * @return its position
	 */
	public int insert(int slot) {
		if (blockCount == 0) {
			blocks[0] = new int[BLOCK];
			blocks[0][0] = slot;
			lengths[0] = 1;
			blockCount = 1;
			size = 1;
			return 0;
		}
		int b = blockOf(slot);
		if (b == blockCount) {
			b--; // after every slot: the end of the last block
		}
		int[] block = blocks[b];
		int i = search(block, lengths[b], slot);
		if (i >= 0) {
			throw new IllegalArgumentException("slot already present: " + slot); //$NON-NLS-1$
		}
		i = -i - 1;
		if (lengths[b] == BLOCK) {
			split(b);
			if (i > lengths[b]) {
				i -= lengths[b];
				b++;
			}
			block = blocks[b];
		}
		System.arraycopy(block, i, block, i + 1, lengths[b] - i);
		block[i] = slot;
		lengths[b]++;
		size++;
		int index = i;
		for (int k = 0; k < b; k++) {
			index += lengths[k];
		}
		return index;
	}

	/**
	 * Removes a slot. The lanes the comparator reads must still hold the values
	 * the slot was inserted with.
	 *
	 * @param slot a slot
	 * @return its former position, or -1 when it was not in the list
	 */
	public int remove(int slot) {
		int b = blockOf(slot);
		if (b == blockCount) {
			return -1;
		}
		int[] block = blocks[b];
		int i = search(block, lengths[b], slot);
		if (i < 0) {
			return -1;
		}
		int index = i;
		for (int k = 0; k < b; k++) {
			index += lengths[k];
		}
		System.arraycopy(block, i + 1, block, i, lengths[b] - i - 1);
		lengths[b]--;
		size--;
		if (lengths[b] == 0) {
			dropBlock(b);
		}
		return index;
	}

	/**
	 * Replaces the contents with slots that are already sorted.
	 *
	 * @param sorted the slots in order
	 * @param n      how many of them
	 */
	public void fill(int[] sorted, int n) {
		int count = (n + BLOCK / 2 - 1) / (BLOCK / 2);
		blocks = new int[Math.max(4, count)][];
		lengths = new int[blocks.length];
		blockCount = 0;
		for (int from = 0; from < n; from += BLOCK / 2) {
			int len = Math.min(BLOCK / 2, n - from);
			int[] block = new int[BLOCK];
			System.arraycopy(sorted, from, block, 0, len);
			blocks[blockCount] = block;
			lengths[blockCount++] = len;
		}
		size = n;
	}

	/**
	 * Sorts the slots with the comparator (a stable merge sort over {@code int},
	 * no boxing) and replaces the contents with them.
	 *
	 * @param slots the slots; reordered in place
	 * @param n     how many of them
	 */
	public void sortAndFill(int[] slots, int n) {
		sort(slots, n, order);
		fill(slots, n);
	}

	/**
	 * Sorts {@code slots[0, n)} with {@code order}: a stable merge sort over
	 * {@code int} with one scratch array.
	 *
	 * @param slots the slots; reordered in place
	 * @param n     how many of them
	 * @param order the order
	 */
	static void sort(int[] slots, int n, SlotComparator order) {
		if (n < 2) {
			return;
		}
		int[] scratch = new int[n];
		int[] from = slots;
		int[] to = scratch;
		for (int run = 1; run < n; run <<= 1) {
			for (int lo = 0; lo < n; lo += run << 1) {
				int mid = Math.min(lo + run, n);
				int hi = Math.min(lo + (run << 1), n);
				int i = lo;
				int j = mid;
				int k = lo;
				while (i < mid && j < hi) {
					to[k++] = order.compare(from[j], from[i]) < 0 ? from[j++] : from[i++];
				}
				while (i < mid) {
					to[k++] = from[i++];
				}
				while (j < hi) {
					to[k++] = from[j++];
				}
			}
			int[] t = from;
			from = to;
			to = t;
		}
		if (from != slots) {
			System.arraycopy(from, 0, slots, 0, n);
		}
	}

	/* the first block whose last slot is not before slot; blockCount when slot sorts after all */
	private int blockOf(int slot) {
		int lo = 0;
		int hi = blockCount - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			if (order.compare(blocks[mid][lengths[mid] - 1], slot) < 0) {
				lo = mid + 1;
			} else {
				hi = mid - 1;
			}
		}
		return lo;
	}

	/* binary search in one block: the index, or -(insertion point) - 1 */
	private int search(int[] block, int length, int slot) {
		int lo = 0;
		int hi = length - 1;
		while (lo <= hi) {
			int mid = (lo + hi) >>> 1;
			int c = order.compare(block[mid], slot);
			if (c < 0) {
				lo = mid + 1;
			} else if (c > 0) {
				hi = mid - 1;
			} else {
				return mid;
			}
		}
		return -lo - 1;
	}

	private void split(int b) {
		if (blockCount == blocks.length) {
			blocks = Arrays.copyOf(blocks, blockCount << 1);
			lengths = Arrays.copyOf(lengths, blockCount << 1);
		}
		System.arraycopy(blocks, b + 1, blocks, b + 2, blockCount - b - 1);
		System.arraycopy(lengths, b + 1, lengths, b + 2, blockCount - b - 1);
		int half = lengths[b] >>> 1;
		int[] right = new int[BLOCK];
		System.arraycopy(blocks[b], half, right, 0, lengths[b] - half);
		blocks[b + 1] = right;
		lengths[b + 1] = lengths[b] - half;
		lengths[b] = half;
		blockCount++;
	}

	private void dropBlock(int b) {
		System.arraycopy(blocks, b + 1, blocks, b, blockCount - b - 1);
		System.arraycopy(lengths, b + 1, lengths, b, blockCount - b - 1);
		blockCount--;
		blocks[blockCount] = null;
		lengths[blockCount] = 0;
	}
}
