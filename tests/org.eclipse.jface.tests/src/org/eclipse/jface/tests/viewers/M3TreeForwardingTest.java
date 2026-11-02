// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.jface.tests.viewers;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

/**
 * Runs the existing native lazy-tree contract with all SWT forwarding methods
 * required. The m3-forwarding Maven profile enables this suite in the forked
 * Eclipse test VM. Missing methods fail before the optional upstream assumption.
 */
@EnabledIfSystemProperty(named = "org.eclipse.jface.tests.m3Required", matches = "true")
public class M3TreeForwardingTest extends VirtualLazyTreeViewerTest {

	@Test
	@Override
	public void testExpandedStateDelegatesToSwtWithoutWalkingColdVirtualSiblings() throws Exception {
		requireSwtMethod("expandToLevel", void.class, int.class);
		requireSwtMethod("expandToLevel", void.class, TreeItem.class, int.class);
		requireSwtMethod("collapseToLevel", void.class, int.class);
		requireSwtMethod("collapseToLevel", void.class, TreeItem.class, int.class);
		requireSwtMethod("expandSelectionToLevel", void.class, int.class);
		requireSwtMethod("collapseSelectionToLevel", void.class, int.class);
		requireSwtMethod("getExpandedItems", TreeItem[].class);
		requireSwtMethod("setExpandedItems", void.class, TreeItem[].class);
		super.testExpandedStateDelegatesToSwtWithoutWalkingColdVirtualSiblings();
	}

	private static void requireSwtMethod(String name, Class<?> returnType, Class<?>... parameterTypes)
			throws NoSuchMethodException, IllegalAccessException {
		MethodHandles.publicLookup().findVirtual(Tree.class, name,
				MethodType.methodType(returnType, parameterTypes));
	}
}
