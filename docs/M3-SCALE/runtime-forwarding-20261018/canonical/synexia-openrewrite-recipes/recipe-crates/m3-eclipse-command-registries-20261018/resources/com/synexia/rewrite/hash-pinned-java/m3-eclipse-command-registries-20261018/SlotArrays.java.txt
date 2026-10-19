// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project: package donation to Eclipse core.commands and Eclipse NLS comments; storage algorithms unchanged.
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.core.internal.commands.m3;

/** Checked array capacities and integer hash spreading shared by storage owners. */
final class SlotArrays {
    // Zero-length lanes are safe to share; insertion always installs writable storage.
    static final Object[] EMPTY_OBJECTS = new Object[0];
    static final int[] EMPTY_INTS = new int[0];
    static final byte[] EMPTY_BYTES = new byte[0];

    private SlotArrays() { }
    static int grow(int old) {
        if (old >= Integer.MAX_VALUE - 8) { throw new OutOfMemoryError("collection capacity exhausted"); } //$NON-NLS-1$
        return (int) Math.min(Integer.MAX_VALUE - 8L, Math.max(8L, old + (old >> 1) + 1L));
    }
    static int doubled(int old) {
        if (old >= 1 << 30) { throw new OutOfMemoryError("hash index capacity exhausted"); } //$NON-NLS-1$
        return old << 1;
    }
    static int hash(Object key) { int h = key == null ? 0 : key.hashCode(); return h ^ h >>> 16; }
    @SuppressWarnings("unchecked") static <T> T get(Object[] values, int slot) { return (T) values[slot]; }
}
