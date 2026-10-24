/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 or the Eclipse Public License 2.0.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.views.markers;

import java.util.Arrays;
import java.util.function.IntConsumer;

/**
 * One bit per slot of the lane model (the selection lane, design 3.2 of
 * {@code docs/m3/jface-viewport/PROBLEMS_VIEW.md}; the TV_5.0 state providers:
 * the model owns the state, the widget asks). Packed in {@code long} words,
 * grown on demand, counted in O(words), iterated in slot order without an
 * object per set bit. Public for the headless tests; no Eclipse type.
 */
public final class M3SlotBits {

	private long[] words = new long[1];
	private int count;

	/** @return whether the slot's bit is set */
	public boolean get(int slot) {
		int w = slot >>> 6;
		return w < words.length && (words[w] & (1L << (slot & 63))) != 0;
	}

	/**
	 * Sets or clears the slot's bit.
	 *
	 * @return whether the bit changed
	 */
	public boolean set(int slot, boolean on) {
		int w = slot >>> 6;
		if (w >= words.length) {
			if (!on) {
				return false;
			}
			words = Arrays.copyOf(words, Math.max(w + 1, words.length << 1));
		}
		long mask = 1L << (slot & 63);
		boolean was = (words[w] & mask) != 0;
		if (was == on) {
			return false;
		}
		if (on) {
			words[w] |= mask;
			count++;
		} else {
			words[w] &= ~mask;
			count--;
		}
		return true;
	}

	/** Clears every bit. */
	public void clear() {
		Arrays.fill(words, 0L);
		count = 0;
	}

	/** @return the number of set bits (O(1)) */
	public int count() {
		return count;
	}

	/**
	 * Calls {@code action} for every set bit, in slot order.
	 *
	 * @param action the consumer of the slot
	 */
	public void forEach(IntConsumer action) {
		for (int w = 0; w < words.length; w++) {
			long bits = words[w];
			while (bits != 0) {
				int b = Long.numberOfTrailingZeros(bits);
				action.accept((w << 6) + b);
				bits &= bits - 1;
			}
		}
	}

	/**
	 * The set bits among the given slots, in the order given: the window rows
	 * that are selected.
	 *
	 * @param slots the slots of a window
	 * @param n     how many of them
	 * @param out   receives the positions (indices into {@code slots}) whose bit
	 *              is set; at least {@code n} long
	 * @return the number of positions written
	 */
	public int positions(int[] slots, int n, int[] out) {
		int k = 0;
		for (int i = 0; i < n; i++) {
			if (get(slots[i])) {
				out[k++] = i;
			}
		}
		return k;
	}

	/** @return whether the two lanes hold the same bits */
	public boolean sameAs(M3SlotBits other) {
		if (count != other.count) {
			return false;
		}
		int n = Math.max(words.length, other.words.length);
		for (int w = 0; w < n; w++) {
			long a = w < words.length ? words[w] : 0L;
			long b = w < other.words.length ? other.words[w] : 0L;
			if (a != b) {
				return false;
			}
		}
		return true;
	}
}
