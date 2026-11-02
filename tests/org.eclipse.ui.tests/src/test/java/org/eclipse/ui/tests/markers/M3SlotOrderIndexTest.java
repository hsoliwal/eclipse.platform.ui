/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.ui.tests.markers;

import org.junit.jupiter.api.Test;

/** Exercises the actual Eclipse slot owner across M3 index admission and release. */
public class M3SlotOrderIndexTest {
    @Test
    public void blockRankPreservesThePublicContract() {
        M3SlotOrderContract.main(new String[0]);
    }
}
