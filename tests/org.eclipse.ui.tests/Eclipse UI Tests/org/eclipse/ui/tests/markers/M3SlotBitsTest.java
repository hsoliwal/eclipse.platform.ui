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

import java.util.BitSet;
import java.util.Random;

import org.eclipse.ui.internal.views.markers.M3SlotBits;
import org.junit.jupiter.api.Test;

/** Headless: the slot bit lane against {@link BitSet} over random operations. */
public class M3SlotBitsTest {

	@Test
	public void matchesABitSetOverRandomOperations() {
		Random random = new Random(7);
		M3SlotBits bits = new M3SlotBits();
		BitSet ref = new BitSet();
		for (int i = 0; i < 20_000; i++) {
			int slot = random.nextInt(5000);
			boolean on = random.nextInt(3) != 0;
			boolean changed = bits.set(slot, on);
			assertEquals(ref.get(slot) != on, changed, "change report at " + slot);
			ref.set(slot, on);
			if (i % 997 == 0) {
				assertEquals(ref.cardinality(), bits.count());
				int[] seen = new int[1];
				bits.forEach(s -> {
					assertTrue(ref.get(s), "iterated slot " + s);
					seen[0]++;
				});
				assertEquals(ref.cardinality(), seen[0]);
			}
		}
		assertEquals(ref.cardinality(), bits.count());
		for (int s = 0; s < 5000; s++) {
			assertEquals(ref.get(s), bits.get(s));
		}
		assertFalse(bits.get(100_000));
	}

	@Test
	public void positionsOfAWindowAndEquality() {
		M3SlotBits bits = new M3SlotBits();
		bits.set(3, true);
		bits.set(70, true);
		bits.set(129, true);
		int[] window = { 129, 5, 70, 3, 9 };
		int[] out = new int[5];
		assertEquals(3, bits.positions(window, 5, out));
		assertEquals(0, out[0]);
		assertEquals(2, out[1]);
		assertEquals(3, out[2]);
		M3SlotBits other = new M3SlotBits();
		assertFalse(bits.sameAs(other));
		other.set(3, true);
		other.set(70, true);
		other.set(129, true);
		assertTrue(bits.sameAs(other));
		bits.clear();
		assertEquals(0, bits.count());
		assertFalse(bits.sameAs(other));
		other.clear();
		assertTrue(bits.sameAs(other));
	}
}
