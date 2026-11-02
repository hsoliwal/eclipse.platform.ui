/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0, which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jface.tests.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.eclipse.jface.action.LegacyActionTools;
import org.junit.jupiter.api.Test;

/** Public contracts for the existing mnemonic range-copy paths. */
public class LegacyActionToolsRangeTest {

	@Test
	public void nullAndEmptyReturnCanonicalEmpty() {
		assertSame("", LegacyActionTools.removeMnemonics(null));
		assertSame("", LegacyActionTools.removeMnemonics(""));
		assertSame("", LegacyActionTools.removeMnemonics(new String("")));
	}

	@Test
	public void textWithoutAmpersandsRetainsItsReference() {
		for (String content : new String[] { "Plain text", "()", "\u00e9", "\ud800", "\udc00",
				"\ud83d\ude00", "\u0000\n\uffff" }) {
			String text = new String(content.toCharArray());
			assertSame(text, LegacyActionTools.removeMnemonics(text));
		}
	}

	@Test
	public void trailingAmpersandIsRetainedInANewEqualString() {
		for (String content : new String[] { "&", "Open&", "\ud83d\ude00&", "\ud800&" }) {
			String text = new String(content.toCharArray());
			String result = LegacyActionTools.removeMnemonics(text);
			assertEquals(text, result);
			assertNotSame(text, result);
			assertNotSame(result, LegacyActionTools.removeMnemonics(text));
		}
	}

	@Test
	public void mnemonicAndEscapeExamplesRetainTheirExistingMeaning() {
		String[][] examples = { { "&Open", "Open" }, { "Save &As", "Save As" },
				{ "&&", "&" }, { "&&&", "&&" }, { "&&&&", "&&" }, { "a&&b", "a&b" },
				{ "a&b&", "ab&" }, { "(&)", "()" }, { "(&&X)", "(&X)" } };
		for (String[] example : examples) {
			assertEquals(example[1], LegacyActionTools.removeMnemonics(example[0]));
		}
	}

	@Test
	public void dbcsMnemonicsUseOneUtf16CodeUnit() {
		String[][] examples = { { "Open(&O)", "Open" }, { "Text (&X)", "Text " },
				{ "(&X)", "" }, { "(&X)(&Y)", "" }, { "((&X))", "()" },
				{ "(&\ud800)", "" }, { "(&\udc00)", "" },
				{ "(&\ud83d\ude00)", "(\ud83d\ude00)" },
				{ "\ud800&\udc00", "\ud800\udc00" }, { "\udc00&\ud800", "\udc00\ud800" } };
		for (String[] example : examples) {
			assertEquals(example[1], LegacyActionTools.removeMnemonics(example[0]));
		}
	}

	@Test
	public void rangeCopiesPreserveEveryUtf16CodeUnit() {
		for (int value = Character.MIN_VALUE; value <= Character.MAX_VALUE; value++) {
			char unit = (char) value;
			assertEquals("prefix" + unit + "suffix",
					LegacyActionTools.removeMnemonics("prefix&" + unit + "suffix"));
		}
	}

	@Test
	public void allThreeRangePathsComposeAcrossLongInputs() {
		String left = "a".repeat(4097);
		String middle = "\ud83d\ude00".repeat(2049);
		String right = "\ud800".repeat(4095);
		String input = left + '&' + middle + "(&X)" + right + '&';
		assertEquals(left + middle + right + '&', LegacyActionTools.removeMnemonics(input));
	}
}
