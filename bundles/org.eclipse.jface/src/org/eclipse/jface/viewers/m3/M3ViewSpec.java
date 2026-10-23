/*******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package org.eclipse.jface.viewers.m3;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A view spec of the view template (brief rule 27,
 * {@code docs/m3/view-template/VIEW_TEMPLATE.md}): one small declarative file
 * next to the view, {@code <ViewClass>.m3view}, that names the view's change
 * source adapter, its lane schema, grouping, sort, filters, counts and
 * presentation. The template turns a spec into the view's lane model, delta
 * wiring and viewer setup, so that converting a view means writing a spec, not
 * view code.
 * <p>
 * Format: {@code key=value} lines; {@code #} starts a comment (a whole line, or
 * after whitespace at the end of a line); blank lines are ignored. Keys:
 * </p>
 * <ul>
 * <li>{@code view.id} (required): the view's extension id;</li>
 * <li>{@code element} (required): the type of the model element;</li>
 * <li>{@code source} (required): the change-source adapter id, one of
 * {@link #ADAPTERS};</li>
 * <li>{@code lanes} (required): {@code name:type} pairs separated by commas;
 * types are {@code long}, {@code int}, {@code byte}, {@code label} (interned
 * string), {@code image} (image id) and {@code bits};</li>
 * <li>{@code group}: grouping lanes the user can switch between, separated by
 * {@code |}, the first is the default; {@code none} means flat;</li>
 * <li>{@code sort}: lanes in sort order, each optionally followed by
 * {@code desc};</li>
 * <li>{@code filters}: filter lanes ({@code scope} and {@code text} are the
 * scope and search bit lanes);</li>
 * <li>{@code counts}: lanes whose values are counted (O(1) lane counters);</li>
 * <li>{@code presentation}: {@code tree}, {@code table} or {@code flat}
 * (alternatives separated by {@code |}, the first is the default);</li>
 * <li>{@code label}: the existing label provider the view reuses.</li>
 * </ul>
 * Any other key is an error, so a typo never passes silently. Instances are
 * immutable.
 *
 * @since 3.43 (M3 enhanced API)
 */
public final class M3ViewSpec {

	/** The change-source adapter ids of the template (VIEW_TEMPLATE.md, "Adapters needed"). */
	public static final Set<String> ADAPTERS = Set.of("markers", "resources", "java-elements", "pde-model", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"search-results", "emf-notifications", "debug-events", "team-sync", "observables", "console-document"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$

	/** Lane types. */
	public enum LaneType {
		/** 64-bit integer (ids, times). */
		LONG,
		/** 32-bit integer. */
		INT,
		/** small integer (severity, kinds). */
		BYTE,
		/** interned string: a label id. */
		LABEL,
		/** image id. */
		IMAGE,
		/** state bits. */
		BITS
	}

	/**
	 * One lane of the schema.
	 *
	 * @param name the lane name
	 * @param type the lane type
	 */
	public record Lane(String name, LaneType type) {
	}

	/**
	 * One sort key.
	 *
	 * @param lane       the lane name
	 * @param descending whether the order is reversed
	 */
	public record SortKey(String lane, boolean descending) {
	}

	private static final Set<String> KEYS = Set.of("view.id", "element", "source", "lanes", "group", "sort", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			"filters", "counts", "presentation", "label"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	private static final Set<String> PRESENTATIONS = Set.of("tree", "table", "flat"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	private static final Set<String> BUILT_IN_FILTERS = Set.of("scope", "text"); //$NON-NLS-1$ //$NON-NLS-2$
	private static final String NONE = "none"; //$NON-NLS-1$

	private final String viewId;
	private final String element;
	private final String source;
	private final List<Lane> lanes;
	private final List<String> groups;
	private final List<SortKey> sort;
	private final List<String> filters;
	private final List<String> counts;
	private final List<String> presentations;
	private final String label;

	private M3ViewSpec(Map<String, String> v) {
		viewId = v.get("view.id"); //$NON-NLS-1$
		element = v.get("element"); //$NON-NLS-1$
		source = v.get("source"); //$NON-NLS-1$
		lanes = lanes(v.get("lanes")); //$NON-NLS-1$
		groups = split(v.getOrDefault("group", NONE), "\\|"); //$NON-NLS-1$ //$NON-NLS-2$
		sort = sort(v.getOrDefault("sort", "")); //$NON-NLS-1$ //$NON-NLS-2$
		filters = split(v.getOrDefault("filters", ""), ","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		counts = split(v.getOrDefault("counts", ""), ","); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		presentations = split(v.getOrDefault("presentation", "tree"), "\\|"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		label = v.get("label"); //$NON-NLS-1$
	}

	/**
	 * Parses a spec.
	 *
	 * @param text the spec text
	 * @return the spec
	 * @throws IllegalArgumentException naming the line of the first error
	 */
	public static M3ViewSpec parse(String text) {
		Map<String, String> values = new LinkedHashMap<>();
		String[] lines = text.split("\\R", -1); //$NON-NLS-1$
		for (int i = 0; i < lines.length; i++) {
			String line = stripComment(lines[i]).strip();
			if (line.isEmpty()) {
				continue;
			}
			int eq = line.indexOf('=');
			if (eq <= 0) {
				throw error(i, "expected key=value: " + line); //$NON-NLS-1$
			}
			String key = line.substring(0, eq).strip();
			if (!KEYS.contains(key)) {
				throw error(i, "unknown key: " + key); //$NON-NLS-1$
			}
			if (values.put(key, line.substring(eq + 1).strip()) != null) {
				throw error(i, "duplicate key: " + key); //$NON-NLS-1$
			}
		}
		for (String required : new String[] { "view.id", "element", "source", "lanes" }) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			if (values.get(required) == null || values.get(required).isEmpty()) {
				throw new IllegalArgumentException("missing key: " + required); //$NON-NLS-1$
			}
		}
		M3ViewSpec spec = new M3ViewSpec(values);
		spec.validate();
		return spec;
	}

	/**
	 * Loads {@code <ViewClass>.m3view} next to a view class.
	 *
	 * @param viewClass the view class; its loader reads the spec
	 * @return the spec
	 * @throws IOException              when the spec cannot be read
	 * @throws IllegalArgumentException when it is not a valid spec
	 */
	public static M3ViewSpec load(Class<?> viewClass) throws IOException {
		String name = viewClass.getSimpleName() + ".m3view"; //$NON-NLS-1$
		try (InputStream in = viewClass.getResourceAsStream(name)) {
			if (in == null) {
				throw new IOException("no spec " + name + " next to " + viewClass.getName()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			return parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	/* a '#' at the start of a line, or after whitespace, starts a comment ('#' inside a value stays) */
	private static String stripComment(String line) {
		for (int i = 0; i < line.length(); i++) {
			if (line.charAt(i) == '#' && (i == 0 || Character.isWhitespace(line.charAt(i - 1)))) {
				return line.substring(0, i);
			}
		}
		return line;
	}

	private static IllegalArgumentException error(int line, String message) {
		return new IllegalArgumentException("line " + (line + 1) + ": " + message); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static List<String> split(String value, String separator) {
		List<String> out = new ArrayList<>();
		for (String part : value.split(separator)) {
			String p = part.strip();
			if (!p.isEmpty()) {
				out.add(p);
			}
		}
		return Collections.unmodifiableList(out);
	}

	private static List<Lane> lanes(String value) {
		List<Lane> out = new ArrayList<>();
		for (String part : split(value, ",")) { //$NON-NLS-1$
			int colon = part.indexOf(':');
			if (colon <= 0) {
				throw new IllegalArgumentException("lane without a type: " + part); //$NON-NLS-1$
			}
			String type = part.substring(colon + 1).strip().toUpperCase(Locale.ROOT);
			try {
				out.add(new Lane(part.substring(0, colon).strip(), LaneType.valueOf(type)));
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("unknown lane type: " + part, e); //$NON-NLS-1$
			}
		}
		return Collections.unmodifiableList(out);
	}

	private static List<SortKey> sort(String value) {
		List<SortKey> out = new ArrayList<>();
		for (String part : split(value, ",")) { //$NON-NLS-1$
			String[] words = part.split("\\s+"); //$NON-NLS-1$
			if (words.length > 2 || words.length == 2 && !"desc".equals(words[1]) && !"asc".equals(words[1])) { //$NON-NLS-1$ //$NON-NLS-2$
				throw new IllegalArgumentException("sort key must be '<lane> [asc|desc]': " + part); //$NON-NLS-1$
			}
			out.add(new SortKey(words[0], words.length == 2 && "desc".equals(words[1]))); //$NON-NLS-1$
		}
		return Collections.unmodifiableList(out);
	}

	private void validate() {
		if (!ADAPTERS.contains(source)) {
			throw new IllegalArgumentException("unknown source adapter: " + source + " (known: " + ADAPTERS + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		for (String p : presentations) {
			if (!PRESENTATIONS.contains(p)) {
				throw new IllegalArgumentException("unknown presentation: " + p); //$NON-NLS-1$
			}
		}
		for (String g : groups) {
			if (!NONE.equals(g)) {
				requireLane(g, "group"); //$NON-NLS-1$
			}
		}
		for (SortKey k : sort) {
			requireLane(k.lane(), "sort"); //$NON-NLS-1$
		}
		for (String f : filters) {
			if (!BUILT_IN_FILTERS.contains(f)) {
				requireLane(f, "filters"); //$NON-NLS-1$
			}
		}
		for (String c : counts) {
			requireLane(c, "counts"); //$NON-NLS-1$
		}
		for (int i = 0; i < lanes.size(); i++) {
			for (int j = i + 1; j < lanes.size(); j++) {
				if (lanes.get(i).name().equals(lanes.get(j).name())) {
					throw new IllegalArgumentException("duplicate lane: " + lanes.get(i).name()); //$NON-NLS-1$
				}
			}
		}
	}

	private void requireLane(String name, String key) {
		if (lane(name) == null) {
			throw new IllegalArgumentException(key + " names an undeclared lane: " + name); //$NON-NLS-1$
		}
	}

	/**
	 * @param name a lane name
	 * @return the lane, or {@code null}
	 */
	public Lane lane(String name) {
		for (Lane l : lanes) {
			if (l.name().equals(name)) {
				return l;
			}
		}
		return null;
	}

	/** @return the view id */
	public String viewId() {
		return viewId;
	}

	/** @return the element type */
	public String element() {
		return element;
	}

	/** @return the change-source adapter id */
	public String source() {
		return source;
	}

	/** @return the lanes, in declaration order */
	public List<Lane> lanes() {
		return lanes;
	}

	/** @return the grouping alternatives, the default first ({@code none} = flat) */
	public List<String> groups() {
		return groups;
	}

	/** @return the sort keys in order */
	public List<SortKey> sort() {
		return sort;
	}

	/** @return the filter lanes ({@code scope} and {@code text} built in) */
	public List<String> filters() {
		return filters;
	}

	/** @return the counted lanes */
	public List<String> counts() {
		return counts;
	}

	/** @return the presentations, the default first */
	public List<String> presentations() {
		return presentations;
	}

	/** @return the label provider the view reuses, or {@code null} */
	public String label() {
		return label;
	}

	@Override
	public String toString() {
		return "M3ViewSpec[" + viewId + " source=" + source + " lanes=" + lanes.size() + " group=" + groups + "]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}
}
