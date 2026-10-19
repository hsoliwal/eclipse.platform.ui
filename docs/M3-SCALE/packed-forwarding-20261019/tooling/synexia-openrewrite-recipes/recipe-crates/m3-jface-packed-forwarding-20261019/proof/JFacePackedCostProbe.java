// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.jface.viewers;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import proof.MemoryAgent;

public final class JFacePackedCostProbe {
    private static volatile Object sink;
    private static long[] retained(Object root) throws Exception {
        var seen = new IdentityHashMap<Object, Boolean>(); var queue = new ArrayDeque<Object>();
        queue.add(root); long bytes = 0, nodes = 0, arrays = 0;
        while (!queue.isEmpty()) {
            Object value = queue.remove(); if (seen.put(value, Boolean.TRUE) != null) continue;
            Class<?> type = value.getClass(); String name = type.getName();
            if (type.isArray()) {
                bytes += MemoryAgent.sizeOf(value); arrays++;
                if (value instanceof Object[] refs) for (Object ref : refs) if (ref != null) queue.add(ref);
            } else if (name.startsWith("org.eclipse.jface.viewers.CustomHashtable") || name.startsWith("org.eclipse.jface.internal.m3.M3ViewerMap")) {
                bytes += MemoryAgent.sizeOf(value); nodes++;
                for (var field : type.getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                    field.setAccessible(true); Object ref = field.get(value); if (ref != null) queue.add(ref);
                }
            }
        }
        return new long[] {bytes, nodes, arrays};
    }
    public static void main(String[] args) throws Exception {
        int n = 10000; Object[] keys = new Object[n];
        for (int i = 0; i < n; i++) keys[i] = Integer.valueOf(i);
        CustomHashtable table = new CustomHashtable();
        for (int i = 0; i < n; i++) table.put(keys[i], keys[i]);
        long[] full = retained(table);
        for (int i = 0; i < n; i++) table.remove(keys[i]);
        long[] empty = retained(table);
        for (int i = 0; i < n; i++) table.put(keys[i], keys[i]);
        long[] reused = retained(table);
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true); long thread = Thread.currentThread().threadId();
        for (int i = 0; i < 500000; i++) sink = table.get(keys[i % n]);
        for (int trial = 0; trial < 3; trial++) {
            long allocated = bean.getThreadAllocatedBytes(thread), start = System.nanoTime();
            for (int i = 0; i < 2000000; i++) sink = table.get(keys[i % n]);
            long elapsed = System.nanoTime() - start; allocated = bean.getThreadAllocatedBytes(thread) - allocated;
            System.out.println("LOOKUP trial=" + trial + " nsPerOp=" + elapsed / 2000000.0 + " bytesPerOp=" + allocated / 2000000.0);
        }
        for (int i = 0; i < 200000; i++) { Object key = keys[i % n]; table.remove(key); table.put(key, key); }
        long allocated = bean.getThreadAllocatedBytes(thread), start = System.nanoTime();
        for (int i = 0; i < 1000000; i++) { Object key = keys[i % n]; table.remove(key); table.put(key, key); }
        long elapsed = System.nanoTime() - start; allocated = bean.getThreadAllocatedBytes(thread) - allocated;
        System.out.println("CHURN nsPerPair=" + elapsed / 1000000.0 + " bytesPerPair=" + allocated / 1000000.0);
        System.out.println("RETAINED fullBytes=" + full[0] + " fullObjects=" + full[1] + " fullArrays=" + full[2]
                + " emptyBytes=" + empty[0] + " emptyObjects=" + empty[1] + " reusedBytes=" + reused[0]);
        // No removed user reference may remain reachable through the implementation lanes.
        for (Object key : keys) table.remove(key);
        var todo = new ArrayDeque<Object>(); var seen = new IdentityHashMap<Object, Boolean>(); todo.add(table);
        while (!todo.isEmpty()) {
            Object value = todo.remove(); if (seen.put(value, Boolean.TRUE) != null) continue;
            if (value instanceof Object[] refs) {
                for (Object ref : refs) if (ref != null) throw new AssertionError("retained removed payload");
            } else if (!value.getClass().isArray()) {
                for (var field : value.getClass().getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers()) && !field.getType().isPrimitive()) {
                    field.setAccessible(true); Object ref = field.get(value); if (ref != null) todo.add(ref);
                }
            }
        }
        System.out.println("REMOVED_REFERENCES_RELEASED");
    }
}
