/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 or the Eclipse Public License 2.0.
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.views.markers;

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * The search text of a markers view evaluated on the lanes. The value a
 * standard column shows for a row is an interned label of the lane model
 * (message, resource, path, location) or a number of it (line, creation time,
 * marker id), so the word match of the pattern runs once per distinct label
 * and is remembered per label id, instead of once per row through a
 * {@code MarkerEntry} facade (stock: {@code PatternFilter.isLeafMatch} over
 * every child, {@code MarkerField.getValue} per visible field).
 *
 * <p>
 * The caller maps the visible fields to kinds by exact class
 * ({@code M3MarkerLanes.plan}); a field of any other class keeps the facade
 * route ({@link #FACADE}): the caller evaluates those fields on a facade only
 * when no lane kind hit, which is the stock OR over the visible fields. The
 * derivations are the stock ones: {@code MarkerEntry.computeLocation} (the
 * location attribute, else "line N", else Unknown), {@code MarkerPathField}
 * (the bidi processor over the path), {@code MarkerCreationTimeField} (empty
 * below zero), {@code MarkerIDField}, {@code MarkerTypeField} (the type label
 * lane, when the model keeps it).
 *
 * <p>
 * One instance serves one pass over the lanes (one search text, one
 * generation): the memo is keyed by label id and label ids are reused after a
 * release, so an instance is never kept across passes. Public for the headless
 * tests; no Eclipse type is referenced, so it loads without the platform.
 */
public final class M3TextMatch {

	public static final byte FACADE = 0;
	public static final byte MESSAGE = 1;
	public static final byte RESOURCE = 2;
	public static final byte PATH = 3;
	public static final byte LOCATION = 4;
	public static final byte TYPE = 5;
	public static final byte CREATED = 6;
	public static final byte ID = 7;

	private static final byte UNKNOWN = 0;
	private static final byte HIT = 1;
	private static final byte MISS = 2;

	private final Predicate<String> word;
	private final byte[] kinds;
	private final M3LabelTable labels;
	private final IntFunction<String> lineLabel;
	private final String unknownLocation;
	private final UnaryOperator<String> bidi;
	private final int facadeCount;
	private byte[] message = new byte[64];
	private byte[] resource = new byte[64];
	private byte[] path = new byte[64];
	private byte[] location = new byte[64];
	private byte[] type = new byte[64];
	private byte[] line = new byte[64];
	private byte unknownHit = UNKNOWN;

	/**
	 * @param word            the pattern's word match over one column value
	 *                        ({@code MarkerPatternFilter.matchesText})
	 * @param kinds           one kind per visible field, in the view's order
	 * @param labels          the label table of the lanes
	 * @param lineLabel       the "line N" label of a line number
	 *                        ({@code MarkerMessages.label_lineNumber})
	 * @param unknownLocation the location of a row without location and line
	 *                        ({@code MarkerMessages.Unknown})
	 * @param bidi            the bidi processor applied to a path
	 *                        ({@code TextProcessor.process})
	 */
	public M3TextMatch(Predicate<String> word, byte[] kinds, M3LabelTable labels, IntFunction<String> lineLabel,
			String unknownLocation, UnaryOperator<String> bidi) {
		this.word = word;
		this.kinds = kinds;
		this.labels = labels;
		this.lineLabel = lineLabel;
		this.unknownLocation = unknownLocation;
		this.bidi = bidi;
		int n = 0;
		for (byte k : kinds) {
			n += k == FACADE ? 1 : 0;
		}
		this.facadeCount = n;
	}

	/** Whether any visible field keeps the facade route. */
	public boolean hasFacade() {
		return facadeCount > 0;
	}

	/** The number of visible fields that keep the facade route. */
	public int facadeCount() {
		return facadeCount;
	}

	/**
	 * Whether a row matches through one of its lane columns (the facade
	 * columns, if any, are the caller's).
	 *
	 * @param messageId  the row's message label
	 * @param resourceId the row's resource label (bidi processed when interned)
	 * @param pathId     the row's path label
	 * @param locationId the row's location attribute label (empty when absent)
	 * @param lineNo     the row's line number, negative when absent
	 * @param typeId     the row's type label (meaningful with a type lane)
	 * @param created    the row's creation time, negative when unknown
	 * @param id         the marker id
	 * @return whether a lane column matches
	 */
	public boolean hit(int messageId, int resourceId, int pathId, int locationId, int lineNo, int typeId,
			long created, long id) {
		for (byte k : kinds) {
			boolean hit = switch (k) {
			case MESSAGE -> label(message, messageId, false);
			case RESOURCE -> label(resource, resourceId, false);
			case PATH -> label(path, pathId, true);
			case LOCATION -> location(locationId, lineNo);
			case TYPE -> label(type, typeId, false);
			case CREATED -> word.test(created < 0 ? "" : Long.toString(created)); //$NON-NLS-1$
			case ID -> word.test(Long.toString(id));
			default -> false;
			};
			if (hit) {
				return true;
			}
		}
		return false;
	}

	private boolean label(byte[] memo, int id, boolean processed) {
		memo = ensure(memo, id);
		if (memo[id] == UNKNOWN) {
			String text = labels.text(id);
			memo[id] = word.test(processed ? bidi.apply(text) : text) ? HIT : MISS;
		}
		return memo[id] == HIT;
	}

	private boolean location(int locationId, int lineNo) {
		String text = labels.text(locationId);
		if (!text.isEmpty()) {
			return label(location, locationId, false);
		}
		if (lineNo < 0) {
			if (unknownHit == UNKNOWN) {
				unknownHit = word.test(unknownLocation) ? HIT : MISS;
			}
			return unknownHit == HIT;
		}
		line = ensure(line, lineNo);
		if (line[lineNo] == UNKNOWN) {
			line[lineNo] = word.test(lineLabel.apply(lineNo)) ? HIT : MISS;
		}
		return line[lineNo] == HIT;
	}

	/* grows the memo a field refers to and re-points the field at the grown array */
	private byte[] ensure(byte[] memo, int index) {
		if (index < memo.length) {
			return memo;
		}
		byte[] grown = Arrays.copyOf(memo, Math.max(index + 1, memo.length << 1));
		if (memo == message) {
			message = grown;
		} else if (memo == resource) {
			resource = grown;
		} else if (memo == path) {
			path = grown;
		} else if (memo == location) {
			location = grown;
		} else if (memo == type) {
			type = grown;
		} else {
			line = grown;
		}
		return grown;
	}
}
