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
package org.eclipse.ui.internal.views.markers;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.osgi.util.TextProcessor;
import org.eclipse.ui.views.markers.MarkerField;
import org.eclipse.ui.views.markers.MarkerViewUtil;
import org.eclipse.ui.views.markers.internal.MarkerMessages;
import org.eclipse.ui.views.markers.internal.MarkerType;
import org.eclipse.ui.views.markers.internal.MarkerTypesModel;

/**
 * The sort keys of one marker, read with one attribute lookup
 * ({@link IMarker#getAttributes(String[])}: one element-tree lookup, where a
 * {@link MarkerEntry} pays one per attribute, ui#4404), and the field kinds the
 * lane comparator knows.
 * <p>
 * Every key equals what the stock field reads through a {@link MarkerEntry}:
 * </p>
 * <ul>
 * <li>severity: {@code getAttributeValue(SEVERITY, -1)}
 * (MarkerSupportInternalUtilities.getSeverity);</li>
 * <li>message: {@code getAttributeValue(MESSAGE, Unknown)}, compared by
 * collation key (MarkerDescriptionField);</li>
 * <li>resource: {@code TextProcessor.process(NAME_ATTRIBUTE or resource name)}
 * (MarkerResourceField);</li>
 * <li>path: the container path of the resource (MarkerEntry.computePath);</li>
 * <li>location and line (MarkerLocationField), id (MarkerIDField), creation
 * time (MarkerCreationTimeField), type label (MarkerTypeField).</li>
 * </ul>
 */
final class M3MarkerKeys {

	/** Field kind: a field the lanes cannot answer; the lane model is not used. */
	static final byte UNKNOWN = 0;
	/** Field kind: severity descending, then description (both severity-and-description fields). */
	static final byte SEVERITY_DESCRIPTION = 1;
	/** Field kind: description. */
	static final byte DESCRIPTION = 2;
	/** Field kind: resource name. */
	static final byte RESOURCE = 3;
	/** Field kind: container path. */
	static final byte PATH = 4;
	/** Field kind: location, then line. */
	static final byte LOCATION = 5;
	/** Field kind: marker id. */
	static final byte ID = 6;
	/** Field kind: creation time. */
	static final byte CREATED = 7;
	/** Field kind: marker type label. */
	static final byte TYPE = 8;

	private static final String[] ATTRIBUTES = { IMarker.SEVERITY, IMarker.MESSAGE, IMarker.LOCATION,
			IMarker.LINE_NUMBER, MarkerViewUtil.NAME_ATTRIBUTE };

	long id;
	int severity;
	String message;
	String location;
	int line;
	String resource;
	String path;
	long created;
	String type;

	private M3MarkerKeys() {
	}

	/**
	 * @param field a sort field of the view
	 * @return its kind; {@link #UNKNOWN} for a field whose compare the lanes do
	 *         not reproduce (any class other than these exact ones)
	 */
	static byte kindOf(MarkerField field) { //FLLDCIM: MARK — ML METHOD_SPLIT METHOD
		Class<?> c = field.getClass();
		if (c == MarkerProblemSeverityAndMessageField.class || c == MarkerSeverityAndDescriptionField.class) {
			return SEVERITY_DESCRIPTION;
		}
		if (c == MarkerDescriptionField.class) {
			return DESCRIPTION;
		}
		if (c == MarkerResourceField.class) {
			return RESOURCE;
		}
		if (c == MarkerPathField.class) {
			return PATH;
		}
		if (c == MarkerLocationField.class) {
			return LOCATION;
		}
		if (c == MarkerIDField.class) {
			return ID;
		}
		if (c == MarkerCreationTimeField.class) {
			return CREATED;
		}
		if (c == MarkerTypeField.class) {
			return TYPE;
		}
		return UNKNOWN;
	}

	/**
	 * Reads the keys of a marker.
	 *
	 * @param marker   the marker
	 * @param needType whether the type label is needed (a TYPE sort field)
	 * @param into     the keys to fill (a clean rebuild reuses one), or {@code null} for new keys
	 * @return its keys
	 * @throws CoreException when the marker no longer exists
	 */
	static M3MarkerKeys read(IMarker marker, boolean needType, M3MarkerKeys into) throws CoreException {
		M3MarkerKeys k = into != null ? into : new M3MarkerKeys();
		Object[] v = marker.getAttributes(ATTRIBUTES);
		IResource resource = marker.getResource();
		k.id = marker.getId();
		k.severity = v[0] instanceof Integer i ? i.intValue() : -1;
		k.message = v[1] == null ? MarkerSupportInternalUtilities.UNKNOWN_ATRRIBTE_VALUE_STRING : v[1].toString();
		k.location = v[2] == null ? MarkerItemDefaults.LOCATION_DEFAULT : v[2].toString();
		k.line = v[3] instanceof Integer i ? i.intValue() : -1;
		k.resource = TextProcessor.process(v[4] == null ? resource.getName() : v[4].toString());
		k.path = path(resource);
		k.created = marker.getCreationTime();
		k.type = needType ? typeLabel(marker) : null;
		return k;
	}

	/* MarkerEntry.getMarkerTypeName */
	private static String typeLabel(IMarker marker) throws CoreException {
		MarkerType type = MarkerTypesModel.getInstance().getType(marker.getType());
		return type == null ? NLS.bind(MarkerMessages.FieldMessage_WrongType, marker.toString()) : type.getLabel();
	}

	/* MarkerEntry.getPath(IResource) */
	private static String path(IResource resource) {
		IPath full = resource.getFullPath();
		if (full.segmentCount() - 1 <= 0) {
			return MarkerItemDefaults.PATH_DEFAULT;
		}
		return full.removeLastSegments(1).removeTrailingSeparator().toString();
	}
}
