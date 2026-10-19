// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.jface.viewers;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Independent executable client, run unchanged against frozen and generated owners. */
public final class JFacePackedContracts {
    private static long checks;
    private static long trace = 0xcbf29ce484222325L;
    private static void event(Object value) {
        String text = String.valueOf(value);
        for (int i = 0; i < text.length(); i++) trace = (trace ^ text.charAt(i)) * 0x100000001b3L;
        trace = (trace ^ 255) * 0x100000001b3L;
    }
    private static void check(boolean value) { checks++; if (!value) throw new AssertionError(checks); }
    private static void same(Object a, Object b) { check(a == b); }
    private static final class Key {
        final int id, hash, tag;
        Runnable onString;
        Key(int id, int hash, int tag) { this.id = id; this.hash = hash; this.tag = tag; }
        @Override public int hashCode() { event("h" + id + ":" + tag); return hash; }
        @Override public boolean equals(Object other) {
            Key k = (Key) other; event("e" + id + ":" + tag + ":" + k.tag); return id == k.id;
        }
        @Override public String toString() {
            Runnable callback = onString; onString = null;
            if (callback != null) callback.run();
            return id + "/" + tag;
        }
    }
    private static final class Compare implements IElementComparer {
        Runnable callback;
        boolean onHash;
        int relation;
        private void call(boolean hash) {
            if (callback != null && onHash == hash) { Runnable action = callback; callback = null; action.run(); }
        }
        @Override public int hashCode(Object element) {
            event("H" + (element == null ? "null" : ((Key) element).tag)); call(true);
            return element == null ? 0 : ((Key) element).hash;
        }
        @Override public boolean equals(Object left, Object right) {
            event("E" + (left == null ? "null" : ((Key) left).tag) + ":" + (right == null ? "null" : ((Key) right).tag));
            call(false);
            if (left == null || right == null) return left == right;
            return relation == 0 ? ((Key) left).id == ((Key) right).id : ((Key) left).id <= ((Key) right).id;
        }
    }
    private static void outcome(Runnable action) {
        try { action.run(); event("success"); } catch (RuntimeException e) { event(e.getClass().getName()); }
    }
    private static void randomOperations() {
        for (int mode = 0; mode < 5; mode++) for (int capacity : new int[] {0, 1, 2, 13, 64}) {
            Compare cmp = new Compare();
            CustomHashtable map = new CustomHashtable(capacity, mode % 2 == 0 ? cmp : null);
            Map<Integer, Object> oracle = new HashMap<>(); Random random = new Random(81231L + mode * 31 + capacity);
            for (int i = 0; i < 20000; i++) {
                int id = random.nextInt(127);
                int hash = switch (mode) { case 0 -> 0; case 1 -> id % 7; case 2 -> Integer.MIN_VALUE + id % 3; case 3 -> -id; default -> id * 0x9e3779b9; };
                Key key = new Key(id, hash, i);
                switch (random.nextInt(6)) {
                    case 0, 1 -> { Object value = new String("value-" + i); same(map.put(key, value), oracle.put(id, value)); }
                    case 2 -> same(map.get(key), oracle.get(id));
                    case 3 -> same(map.remove(key), oracle.remove(id));
                    case 4 -> same(map.remove(key, hash), oracle.remove(id));
                    default -> check(map.containsKey(key) == oracle.containsKey(id));
                }
                check(map.size() == oracle.size());
                if (i % 503 == 0) {
                    event(map.toString());
                    CustomHashtable copy = new CustomHashtable(map, cmp);
                    check(copy.size() == map.size()); same(copy.getComparer(), cmp);
                    event(copy.toString());
                }
            }
        }
    }
    private static void edgeCases() {
        outcome(() -> new CustomHashtable(-1));
        CustomHashtable empty = new CustomHashtable(0);
        same(empty.remove(null), null); same(empty.remove(null, 0), null);
        outcome(() -> empty.get(null)); outcome(() -> empty.containsKey(null));
        outcome(() -> empty.put(null, new Object())); outcome(() -> empty.put(new Object(), null));
        Compare cmp = new Compare(); CustomHashtable table = new CustomHashtable(cmp);
        same(table.get(null), null); check(!table.containsKey(null));
        Key first = new Key(1, 0, 1), replacement = new Key(1, 0, 2);
        same(table.put(first, "first"), null); same(table.put(replacement, "second"), "first");
        check(table.toString().equals("{1/2=second}"));
        table.put(new Key(2, 0, 3), "third");
        cmp.relation = 1; event(table.get(new Key(0, 0, 9))); event(table.toString());
        cmp.relation = 0;
        RuntimeException sentinel = new IllegalArgumentException("sentinel");
        for (boolean hash : new boolean[] {false, true}) {
            cmp.onHash = hash; cmp.callback = () -> { throw sentinel; };
            try { table.get(new Key(2, 0, 22)); throw new AssertionError(); }
            catch (RuntimeException actual) { same(actual, sentinel); }
            same(table.get(new Key(2, 0, 23)), "third");
        }
    }
    private static void reentrantOperations() {
        for (int outer = 0; outer < 7; outer++) for (int nested = 0; nested < 6; nested++) for (boolean hash : new boolean[] {false, true}) {
            Compare cmp = new Compare(); CustomHashtable map = new CustomHashtable(2, cmp);
            Key a = new Key(1, 0, 1), b = new Key(2, 0, 2), c = new Key(3, 0, 3);
            map.put(a, "a"); map.put(b, "b"); map.put(c, "c");
            cmp.onHash = hash;
            final int operation = nested;
            cmp.callback = () -> {
                switch (operation) {
                    case 0 -> event(map.remove(c));
                    case 1 -> { event(map.remove(c)); map.put(new Key(7, 0, 7), "new"); }
                    case 2 -> { for (int i = 10; i < 50; i++) map.put(new Key(i, i % 3, i), "grow"); }
                    case 3 -> { event(map.remove(a)); event(map.remove(b)); event(map.remove(c)); }
                    case 4 -> map.put(new Key(3, 0, 33), "replacement");
                    default -> { event(map.remove(c)); throw new IllegalStateException("callback"); }
                }
            };
            final int op = outer;
            outcome(() -> {
                switch (op) {
                    case 0 -> event(map.get(new Key(3, 0, 30)));
                    case 1 -> event(map.get(new Key(8, 0, 80)));
                    case 2 -> event(map.put(new Key(3, 0, 30), "outer"));
                    case 3 -> event(map.put(new Key(8, 0, 80), "outer"));
                    case 4 -> event(map.remove(new Key(3, 0, 30)));
                    case 5 -> event(map.remove(new Key(8, 0, 80), 0));
                    default -> event(map.containsKey(new Key(3, 0, 30)));
                }
            });
            cmp.callback = null;
            event(map.size()); event(map.toString());
            for (int i = 0; i < 55; i++) event(map.get(new Key(i, i < 10 ? 0 : i % 3, i)));
            for (int i = 60; i < 80; i++) map.put(new Key(i, i, i), "tail");
            event(map.size()); event(map.toString());
        }
        for (int mode = 0; mode < 3; mode++) {
            CustomHashtable map = new CustomHashtable();
            Key a = new Key(1, 0, 1), b = new Key(2, 0, 2);
            map.put(a, "a"); map.put(b, "b");
            final int m = mode;
            b.onString = () -> { event(map.remove(b)); if (m == 1) map.put(new Key(3, 0, 3), "c"); if (m == 2) map.remove(a); };
            event(map.toString()); event(map.toString());
        }
        Compare destination = new Compare(); CustomHashtable source = new CustomHashtable();
        Key a = new Key(1, 0, 1), b = new Key(2, 0, 2);
        source.put(a, "a"); source.put(b, "b");
        destination.onHash = true;
        destination.callback = () -> { source.remove(b); source.put(new Key(3, 0, 3), "c"); };
        event(new CustomHashtable(source, destination).toString()); event(source.toString());
    }
    private static void selections() {
        Compare cmp = new Compare(); Random random = new Random(711);
        for (int n = 0; n < 1000; n++) {
            TreePath[] paths = new TreePath[n % 129];
            for (int i = 0; i < paths.length; i++) {
                int id = random.nextInt(11);
                paths[i] = i % 17 == 0 ? TreePath.EMPTY : new TreePath(new Object[] {new Key(i, 0, i), new Key(id, 0, i)});
            }
            TreeSelection selection = new TreeSelection(paths, cmp);
            TreeSelection equal = new TreeSelection(paths.clone(), cmp);
            check(selection.equals(equal)); check(selection.hashCode() == equal.hashCode());
            check(selection.getPaths() != paths); check(selection.getPaths().length == paths.length);
            for (int id = 0; id < 11; id++) {
                TreePath[] actual = selection.getPathsFor(new Key(id, 0, 999)); int count = 0;
                for (TreePath path : paths) if (path.getLastSegment() instanceof Key k && k.id == id) same(actual[count++], path);
                check(count == actual.length);
            }
        }
    }
    public static void main(String[] args) {
        randomOperations(); edgeCases(); reentrantOperations(); selections();
        System.out.println("PACKED_CONTRACTS checks=" + checks + " trace=" + Long.toUnsignedString(trace, 16));
    }
}
