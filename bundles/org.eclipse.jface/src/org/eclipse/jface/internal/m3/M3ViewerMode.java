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
package org.eclipse.jface.internal.m3;

/**
 * The JFace viewer mode, read once from system properties.
 * <ul>
 * <li>{@code -Djface.viewer.mode=original} (default): every M3 hook falls back
 * to the original code path.</li>
 * <li>{@code -Djface.viewer.mode=viewport}: the internal fast paths of the
 * viewers are active. They keep the public behaviour and replace only internal
 * search work (see {@code docs/m3/jface-viewport/DESIGN.md}).</li>
 * </ul>
 * A viewer class can override the global mode with
 * {@code -Djface.viewer.mode.<SimpleClassName>=original|viewport}.
 */
public final class M3ViewerMode {

	/** The property that selects the mode for every viewer. */
	public static final String PROPERTY = "jface.viewer.mode"; //$NON-NLS-1$

	/** The original JFace code paths. */
	public static final String ORIGINAL = "original"; //$NON-NLS-1$

	/** The internal fast paths that ride the viewport. */
	public static final String VIEWPORT = "viewport"; //$NON-NLS-1$

	private static final String GLOBAL = read(PROPERTY, ORIGINAL);

	/** Whether the global mode is {@link #VIEWPORT}. */
	public static final boolean ENABLED = VIEWPORT.equals(GLOBAL);

	/**
	 * Whether any viewer can run in {@link #VIEWPORT} mode: the global mode is
	 * {@link #VIEWPORT} or a per-class property is set. When {@code false} every
	 * hook returns at its first check.
	 */
	public static final boolean ANY = ENABLED || hasClassProperty();

	private M3ViewerMode() {
	}

	/**
	 * Returns the mode of one viewer class: its own property when set, else the
	 * global mode.
	 *
	 * @param simpleClassName the simple name of the viewer class, for example
	 *                        {@code TableViewer}
	 * @return {@code true} when the class runs in {@link #VIEWPORT} mode
	 */
	public static boolean enabled(String simpleClassName) {
		return VIEWPORT.equals(read(PROPERTY + '.' + simpleClassName, GLOBAL)); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
	}

	private static boolean hasClassProperty() {
		try {
			return System.getProperties().stringPropertyNames().stream().anyMatch(k -> k.startsWith(PROPERTY + '.')); //FLLDCIM: MARK — M33 M3STRING_COMPOSITE CONCAT
		} catch (SecurityException e) {
			return false;
		}
	}

	static String read(String key, String fallback) {
		try {
			String value = System.getProperty(key);
			return value == null || value.isBlank() ? fallback : value.trim();
		} catch (SecurityException e) {
			return fallback;
		}
	}
}
