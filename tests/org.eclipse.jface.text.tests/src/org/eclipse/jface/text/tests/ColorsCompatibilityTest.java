// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.jface.text.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.eclipse.jface.internal.text.revisions.Colors;
import org.eclipse.swt.graphics.RGB;
import org.junit.jupiter.api.Test;

/** Direct callers retain the original float entry point beside the newer RGB API. */
public class ColorsCompatibilityTest {
    @Test
    public void legacyBlendRetainsTruncationAndFreshValues() {
        RGB background = new RGB(0, 1, 2);
        RGB foreground = new RGB(1, 2, 3);
        assertEquals(new RGB(0, 1, 2), Colors.blend(background, foreground, 0.5f));
        RGB first = Colors.blend(background, foreground, 0.0f);
        RGB last = Colors.blend(background, foreground, 1.0f);
        assertEquals(background, first);
        assertEquals(foreground, last);
        assertNotSame(background, first);
        assertNotSame(foreground, last);
    }

    @Test
    public void legacyBlendRejectsTheOriginalInvalidInputs() {
        RGB color = new RGB(17, 31, 127);
        assertThrows(IllegalArgumentException.class, () -> Colors.blend(null, color, 0.5f));
        assertThrows(IllegalArgumentException.class, () -> Colors.blend(color, null, 0.5f));
        for (float factor : new float[] {-0.01f, 1.01f, Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> Colors.blend(color, color, factor));
        }
    }

    @Test
    public void currentPaletteRetainsRoundedRgbInterpolation() {
        RGB[] palette = Colors.palette(new RGB(0, 1, 2), new RGB(1, 2, 3), 3);
        assertEquals(new RGB(1, 2, 3), palette[1]);
    }
}
