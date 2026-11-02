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
package org.eclipse.jface.tests.viewers.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jface.viewers.m3.M3ViewSpec;
import org.eclipse.jface.viewers.m3.M3ViewSpec.LaneType;
import org.junit.jupiter.api.Test;

/** Headless tests of the view template's spec parser (brief rule 27). */
public class M3ViewSpecTest {

	private static final String PROBLEMS = """
			# a comment line
			view.id=org.eclipse.ui.views.ProblemView
			element=IMarker
			source=markers                 # adapter id
			lanes=id:long, severity:byte, message:label, resource:label, line:int, type:label, flags:bits, icon:image
			group=severity|type|none
			sort=severity desc, message, resource asc, line
			filters=scope, severity, type, text
			counts=severity
			presentation=tree|flat
			label=MarkerColumnLabelProvider
			""";

	@Test
	public void parsesTheProblemsViewSpec() {
		M3ViewSpec spec = M3ViewSpec.parse(PROBLEMS);
		assertEquals("org.eclipse.ui.views.ProblemView", spec.viewId());
		assertEquals("IMarker", spec.element());
		assertEquals("markers", spec.source());
		assertEquals(8, spec.lanes().size());
		assertEquals(LaneType.BYTE, spec.lane("severity").type());
		assertEquals(LaneType.LABEL, spec.lane("message").type());
		assertEquals(LaneType.BITS, spec.lane("flags").type());
		assertEquals(LaneType.IMAGE, spec.lane("icon").type());
		assertNull(spec.lane("nope"));
		assertEquals(List.of("severity", "type", "none"), spec.groups());
		assertTrue(spec.sort().get(0).descending());
		assertFalse(spec.sort().get(2).descending());
		assertEquals("line", spec.sort().get(3).lane());
		assertEquals(List.of("scope", "severity", "type", "text"), spec.filters());
		assertEquals(List.of("severity"), spec.counts());
		assertEquals(List.of("tree", "flat"), spec.presentations());
		assertEquals("MarkerColumnLabelProvider", spec.label());
		assertTrue(spec.toString().contains("source=markers"));
	}

	@Test
	public void defaultsForOptionalKeys() {
		M3ViewSpec spec = M3ViewSpec.parse("view.id=v\nelement=E\nsource=observables\nlanes=id:long\n");
		assertEquals(List.of("none"), spec.groups());
		assertEquals(List.of("tree"), spec.presentations());
		assertTrue(spec.sort().isEmpty());
		assertTrue(spec.filters().isEmpty());
		assertNull(spec.label());
	}

	@Test
	public void theCommittedProblemsViewSpecIsValid() throws IOException {
		Path file = Path.of(System.getProperty("m3.wt", "S:/m3-scratch/worktrees/platform-ui"),
				"bundles/org.eclipse.ui.ide/src/org/eclipse/ui/internal/views/markers/ProblemsView.m3view");
		if (Files.exists(file)) {
			M3ViewSpec spec = M3ViewSpec.parse(Files.readString(file));
			assertEquals("markers", spec.source());
			assertEquals(LaneType.LONG, spec.lane("id").type());
		}
	}

	@Test
	public void loadFindsTheSpecNextToTheClass() {
		assertThrows(IOException.class, () -> M3ViewSpec.load(M3ViewSpecTest.class));
	}

	@Test
	public void errorsNameTheProblem() {
		String base = "view.id=v\nelement=E\nsource=markers\nlanes=id:long, sev:byte\n";
		assertMessage("line 5: unknown key: colour", base + "colour=red");
		assertMessage("line 1: expected key=value: nonsense", "nonsense");
		assertMessage("line 5: duplicate key: source", base + "source=markers");
		assertMessage("missing key: lanes", "view.id=v\nelement=E\nsource=markers\n");
		assertMessage("unknown source adapter: telepathy", "view.id=v\nelement=E\nsource=telepathy\nlanes=id:long\n");
		assertMessage("lane without a type: id", "view.id=v\nelement=E\nsource=markers\nlanes=id\n");
		assertMessage("unknown lane type: id:float", "view.id=v\nelement=E\nsource=markers\nlanes=id:float\n");
		assertMessage("unknown presentation: carousel", base + "presentation=carousel");
		assertMessage("group names an undeclared lane: kind", base + "group=kind|none");
		assertMessage("sort names an undeclared lane: x", base + "sort=x");
		assertMessage("sort key must be '<lane> [asc|desc]': sev up", base + "sort=sev up");
		assertMessage("filters names an undeclared lane: f", base + "filters=scope, f");
		assertMessage("counts names an undeclared lane: c", base + "counts=c");
		assertMessage("duplicate lane: id", "view.id=v\nelement=E\nsource=markers\nlanes=id:long, id:int\n");
	}

	private static void assertMessage(String expected, String spec) {
		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> M3ViewSpec.parse(spec));
		assertTrue(e.getMessage().startsWith(expected), e.getMessage());
	}
}
