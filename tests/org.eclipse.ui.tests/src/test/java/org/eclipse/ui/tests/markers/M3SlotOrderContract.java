/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 */
package org.eclipse.ui.tests.markers;

import org.eclipse.ui.internal.views.markers.M3SlotOrder;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;

/** Differential public-contract exercise; run unchanged against each real source. */
public final class M3SlotOrderContract {
    private static final MessageDigest CALLS = digest();
    private static final MessageDigest RESULTS = digest();
    private static final byte[] PAIR = new byte[8];
    private static long comparisons;
    private static long checks;
    private static int failAt = -1;
    private static int localCalls;

    private M3SlotOrderContract() { }

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (java.security.NoSuchAlgorithmException failure) { throw new AssertionError(failure); }
    }

    private static int plain(int a, int b) {
        int c = Integer.compare(Integer.rotateLeft(a * 0x9E3779B9, 7) & 1023,
                Integer.rotateLeft(b * 0x9E3779B9, 7) & 1023);
        return c != 0 ? c : Integer.compare(a, b);
    }

    private static int traced(int a, int b) {
        ByteBuffer.wrap(PAIR).putInt(a).putInt(b);
        CALLS.update(PAIR);
        comparisons++;
        if (localCalls++ == failAt) { throw new DeliberateFailure(); }
        return plain(a, b);
    }

    private static void equal(int want, int got) {
        if (want != got) { throw new AssertionError("expected " + want + " but was " + got); }
        checks++;
        ByteBuffer.wrap(PAIR).putInt(want).putInt(got);
        RESULTS.update(PAIR);
    }

    private static void state(M3SlotOrder list, List<Integer> ref, boolean every) {
        equal(ref.size(), list.size());
        int[] out = new int[ref.size() + 3];
        Arrays.fill(out, Integer.MIN_VALUE);
        equal(ref.size(), list.head(Integer.MAX_VALUE, out));
        for (int i = 0; i < ref.size(); i++) {
            equal(ref.get(i), out[i]);
            if (every || i % 97 == 0) {
                equal(ref.get(i), list.get(i));
                equal(i, list.indexOf(ref.get(i)));
            }
        }
        equal(Integer.MIN_VALUE, out[ref.size()]);
        equal(0, list.head(0, null));
        equal(0, list.head(-1, null));
        for (int bad : new int[] {-1, ref.size(), Integer.MAX_VALUE}) {
            try { list.get(bad); throw new AssertionError("missing bounds exception"); }
            catch (IndexOutOfBoundsException expected) { checks++; }
        }
    }

    private static void boundaries() {
        for (int n : new int[] {0, 1, 511, 512, 513, 1023, 1024, 1025, 4095, 8192, 16385,
                65535, 65536, 65537, 131073}) {
            int[] slots = new int[n + 3];
            List<Integer> ref = new ArrayList<>();
            for (int i = 0; i < n; i++) { slots[i] = n - i; ref.add(n - i); }
            slots[n] = -991;
            ref.sort(M3SlotOrderContract::plain);
            M3SlotOrder list = new M3SlotOrder(M3SlotOrderContract::traced);
            list.sortAndFill(slots, n);
            state(list, ref, true);
            equal(-991, slots[n]);
            list.fill(slots, n);
            state(list, ref, true);
            for (int k = 0; k < 100 && !ref.isEmpty(); k++) {
                int at = (k * 509) % ref.size();
                int slot = ref.remove(at);
                equal(at, list.remove(slot));
                equal(-1, list.remove(slot));
                equal(-1, list.indexOf(slot));
            }
            state(list, ref, true);
            list.fill(new int[0], 0);
            state(list, List.of(), true);
            equal(0, list.insert(123));
            state(list, List.of(123), true);
        }
    }

    private static void randomChanges() {
        Random random = new Random(0x3f2bc59L);
        Comparator<Integer> comparator = M3SlotOrderContract::plain;
        List<Integer> ref = new ArrayList<>();
        M3SlotOrder list = new M3SlotOrder(M3SlotOrderContract::traced);
        for (int step = 0; step < 75_000; step++) {
            int slot = random.nextInt(24_000) - 12_000;
            int at = Collections.binarySearch(ref, slot, comparator);
            if (at >= 0) {
                if (step % 37 == 0) {
                    try { list.insert(slot); throw new AssertionError("missing duplicate exception"); }
                    catch (IllegalArgumentException expected) { checks++; }
                }
                equal(at, list.remove(slot));
                ref.remove(at);
            } else {
                at = -at - 1;
                equal(at, list.insert(slot));
                ref.add(at, slot);
            }
            if (step % 127 == 0) { state(list, ref, false); }
        }
        state(list, ref, true);
        while (!ref.isEmpty()) {
            int at = random.nextInt(ref.size());
            equal(at, list.remove(ref.remove(at)));
            if (ref.size() % 257 == 0) { state(list, ref, false); }
        }
        state(list, ref, true);
        equal(0, list.insert(-999));
        equal(0, list.remove(-999));
        state(list, List.of(), true);
    }

    private static void failures() {
        int n = 8193;
        List<Integer> ref = new ArrayList<>();
        for (int i = 0; i < n; i++) { ref.add(i); }
        ref.sort(M3SlotOrderContract::plain);
        int[] sorted = ref.stream().mapToInt(Integer::intValue).toArray();
        for (int operation = 0; operation < 3; operation++) {
            for (int fail = 0; fail < 18; fail++) {
                M3SlotOrder list = new M3SlotOrder(M3SlotOrderContract::traced);
                list.fill(sorted, n);
                failAt = fail;
                localCalls = 0;
                boolean threw = false;
                int result = -999;
                try {
                    result = switch (operation) {
                        case 0 -> list.indexOf(3141);
                        case 1 -> list.insert(90_001);
                        default -> list.remove(3141);
                    };
                } catch (DeliberateFailure expected) { threw = true; }
                finally { failAt = -1; }
                List<Integer> expected = new ArrayList<>(ref);
                if (!threw && operation == 1) { expected.add(-Collections.binarySearch(ref, 90_001, M3SlotOrderContract::plain) - 1, 90_001); }
                if (!threw && operation == 2) { expected.remove(Integer.valueOf(3141)); }
                equal(threw ? -999 : result, result);
                state(list, expected, false);
            }
        }
        M3SlotOrder acceptsNull = new M3SlotOrder(null);
        acceptsNull.sortAndFill(null, 0);
        equal(0, acceptsNull.size());
        equal(0, acceptsNull.insert(7));
        equal(7, acceptsNull.get(0));
    }

    private static void growAndShrinkIndex() {
        int initial = 65536;
        int total = 133120;
        int[] sorted = new int[initial];
        Arrays.setAll(sorted, i -> i);
        M3SlotOrder list = new M3SlotOrder((a, b) -> {
            traced(a, b);
            return Integer.compare(a, b);
        });
        list.fill(sorted, initial);
        for (int i = initial; i < total; i++) { equal(i, list.insert(i)); }
        for (int i = 0; i < total; i += 127) {
            equal(i, list.get(i));
            equal(i, list.indexOf(i));
        }
        for (int removed = 0; removed < total; removed++) {
            equal(0, list.remove(removed));
            if ((removed & 1023) == 0 && list.size() > 1) {
                equal(removed + 1, list.get(0));
                equal(total - 1, list.get(list.size() - 1));
                equal(list.size() - 1, list.indexOf(total - 1));
            }
        }
        equal(0, list.size());
        equal(0, list.insert(314));
        equal(314, list.get(0));
        list.fill(sorted, initial);
        equal(initial - 1, list.get(initial - 1));
    }

    private static void failedFillKeepsResetObservations() {
        int[] populated = new int[65537];
        Arrays.setAll(populated, i -> i);
        for (int length : new int[] {-1, 0, 10, 512, 513}) {
            M3SlotOrder list = new M3SlotOrder(Integer::compare);
            list.fill(populated, populated.length);
            int[] shortSource = length < 0 ? null : Arrays.copyOf(populated, length);
            try { list.fill(shortSource, 1024); throw new AssertionError("missing fill failure"); }
            catch (NullPointerException expected) {
                equal(-1, length);
            } catch (ArrayIndexOutOfBoundsException expected) {
                if (length < 0) { throw new AssertionError("wrong fill failure", expected); }
                checks++;
            }
            equal(populated.length, list.size());
            try { list.get(512); throw new AssertionError("missing partial-reset bounds failure"); }
            catch (ArrayIndexOutOfBoundsException expected) { checks++; }
            if (length >= 512) { equal(0, list.get(0)); }
            list.fill(populated, populated.length);
            equal(65536, list.get(65536));
        }
    }

    public static void main(String[] arguments) {
        boundaries();
        randomChanges();
        failures();
        growAndShrinkIndex();
        failedFillKeepsResetObservations();
        System.out.println("{\"status\":\"PASS\",\"checks\":" + checks + ",\"comparisons\":" + comparisons
                + ",\"callback_sha256\":\"" + HexFormat.of().formatHex(CALLS.digest())
                + "\",\"results_sha256\":\"" + HexFormat.of().formatHex(RESULTS.digest()) + "\"}");
    }

    private static final class DeliberateFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
