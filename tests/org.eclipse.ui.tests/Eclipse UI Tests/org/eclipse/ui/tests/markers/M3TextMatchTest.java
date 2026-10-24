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

import java.text.Collator;
import java.util.Locale;
import java.util.Random;
import java.util.function.Predicate;

import org.eclipse.ui.internal.views.markers.M3LabelTable;
import org.eclipse.ui.internal.views.markers.M3TextMatch;
import org.junit.jupiter.api.Test;

/**
 * Headless: the lane-native search match against the stock derivation of
 * every column value, over random rows (no workspace, no Display).
 */
public class M3TextMatchTest {

	private static final String[] WORDS = { "alpha", "beta", "gamma", "delta", "Error", "unused", "import", "x" };
	private static final String UNKNOWN = "Unknown";

	private static Predicate<String> contains(String token) {
		String t = token.toLowerCase(Locale.ROOT);
		return v -> v != null && v.toLowerCase(Locale.ROOT).contains(t);
	}

	private static String lineLabel(int line) {
		return "line " + line;
	}

	private static String bidi(String s) {
		return "[" + s + "]"; // a visible stand-in for TextProcessor.process: paths are matched processed
	}

	/* MarkerEntry.computeLocation */
	private static String stockLocation(String location, int line) {
		if (!location.isEmpty()) {
			return location;
		}
		return line < 0 ? UNKNOWN : lineLabel(line);
	}

	@Test
	public void matchesLikeTheStockColumns() {
		Random random = new Random(42);
		M3LabelTable labels = new M3LabelTable(Collator.getInstance(Locale.ROOT));
		int n = 5000;
		int[] message = new int[n], resource = new int[n], path = new int[n], location = new int[n], line = new int[n];
		long[] created = new long[n], id = new long[n];
		for (int i = 0; i < n; i++) {
			message[i] = labels.acquire(WORDS[random.nextInt(WORDS.length)] + " " + WORDS[random.nextInt(WORDS.length)]
					+ " " + random.nextInt(50));
			resource[i] = labels.acquire(WORDS[random.nextInt(WORDS.length)] + random.nextInt(20) + ".java");
			path[i] = labels.acquire("/" + WORDS[random.nextInt(WORDS.length)] + "/src/" + random.nextInt(5));
			location[i] = labels.acquire(random.nextInt(3) == 0 ? "" : "near " + WORDS[random.nextInt(WORDS.length)]);
			line[i] = random.nextInt(4) == 0 ? -1 : random.nextInt(3000);
			created[i] = random.nextInt(5) == 0 ? -1 : 1700000000000L + random.nextInt(1000000);
			id[i] = 1000 + i;
		}
		byte[] kinds = { M3TextMatch.MESSAGE, M3TextMatch.RESOURCE, M3TextMatch.PATH, M3TextMatch.LOCATION,
				M3TextMatch.CREATED, M3TextMatch.ID };
		for (String token : new String[] { "alpha", "ERR", "line 12", "unknown", "[/gamma", "170000", "1234", "zzz" }) {
			Predicate<String> word = contains(token);
			M3TextMatch match = new M3TextMatch(word, kinds, labels, M3TextMatchTest::lineLabel, UNKNOWN,
					M3TextMatchTest::bidi);
			assertFalse(match.hasFacade());
			int hits = 0;
			for (int i = 0; i < n; i++) {
				boolean expected = word.test(labels.text(message[i])) || word.test(labels.text(resource[i]))
						|| word.test(bidi(labels.text(path[i])))
						|| word.test(stockLocation(labels.text(location[i]), line[i]))
						|| word.test(created[i] < 0 ? "" : Long.toString(created[i])) || word.test(Long.toString(id[i]));
				boolean actual = match.hit(message[i], resource[i], path[i], location[i], line[i], 0, created[i], id[i]);
				assertEquals(expected, actual, "row " + i + " token " + token);
				hits += actual ? 1 : 0;
			}
			if (token.equals("zzz")) {
				assertEquals(0, hits);
			} else {
				assertTrue(hits > 0, token);
			}
		}
	}

	@Test
	public void facadeFieldsAreCountedAndSkipped() {
		M3LabelTable labels = new M3LabelTable(Collator.getInstance(Locale.ROOT));
		byte[] kinds = { M3TextMatch.FACADE, M3TextMatch.MESSAGE, M3TextMatch.FACADE };
		M3TextMatch match = new M3TextMatch(contains("a"), kinds, labels, M3TextMatchTest::lineLabel, UNKNOWN,
				M3TextMatchTest::bidi);
		assertTrue(match.hasFacade());
		assertEquals(2, match.facadeCount());
		int a = labels.acquire("banana");
		int b = labels.acquire("kiwi");
		assertTrue(match.hit(a, 0, 0, 0, -1, 0, -1, 1));
		assertFalse(match.hit(b, 0, 0, 0, -1, 0, -1, 1));
	}

	@Test
	public void memoGrowsWithTheLabelIds() {
		M3LabelTable labels = new M3LabelTable(Collator.getInstance(Locale.ROOT));
		byte[] kinds = { M3TextMatch.MESSAGE, M3TextMatch.LOCATION };
		M3TextMatch match = new M3TextMatch(contains("line 70000"), kinds, labels, M3TextMatchTest::lineLabel,
				UNKNOWN, M3TextMatchTest::bidi);
		int empty = labels.acquire("");
		int last = empty;
		for (int i = 0; i < 300; i++) {
			last = labels.acquire("m" + i);
		}
		assertTrue(match.hit(last, 0, 0, empty, 70000, 0, -1, 1));
		assertFalse(match.hit(last, 0, 0, empty, 69999, 0, -1, 1));
		assertFalse(match.hit(last, 0, 0, empty, -1, 0, -1, 1));
	}
}
