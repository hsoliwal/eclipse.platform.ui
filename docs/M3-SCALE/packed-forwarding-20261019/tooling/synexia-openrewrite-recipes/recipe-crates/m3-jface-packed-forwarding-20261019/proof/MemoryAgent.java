// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package proof;

import java.lang.instrument.Instrumentation;

public final class MemoryAgent {
    private static Instrumentation instrumentation;
    private MemoryAgent() { }
    public static void premain(String options, Instrumentation value) { instrumentation = value; }
    public static long sizeOf(Object value) { return instrumentation.getObjectSize(value); }
}
