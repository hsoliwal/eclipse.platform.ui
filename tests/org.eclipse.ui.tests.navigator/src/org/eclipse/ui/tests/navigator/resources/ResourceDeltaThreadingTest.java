/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and Synexia contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0, which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.tests.navigator.resources;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.navigator.resources.workbench.ResourceExtensionContentProvider;
import org.junit.jupiter.api.Test;

/**
 * The navigator resource source can run on a workspace worker while its
 * receiving TreeViewer exists exclusively on the SWT display thread.
 */
public class ResourceDeltaThreadingTest {

	@Test
	public void emptyResourceEventOnWorkerDoesNotInspectSwtControl() throws Exception {
		Display display = Display.getDefault();
		Shell shell = new Shell(display);
		try {
			class Receiver extends ResourceExtensionContentProvider {
				void deliver(IResourceDelta delta) {
					processDelta(delta);
				}
			}
			Receiver provider = new Receiver();
			TreeViewer viewer = new TreeViewer(shell, SWT.NONE);
			viewer.setContentProvider(provider);
			viewer.setInput(ResourcesPlugin.getWorkspace().getRoot());
			IResourceDelta delta = (IResourceDelta) Proxy.newProxyInstance(
					IResourceDelta.class.getClassLoader(), new Class<?>[] { IResourceDelta.class },
					(proxy, method, args) -> switch (method.getName()) {
						case "getAffectedChildren" -> new IResourceDelta[0]; //$NON-NLS-1$
						case "getResource" -> ResourcesPlugin.getWorkspace().getRoot(); //$NON-NLS-1$
						case "getFlags" -> 0; //$NON-NLS-1$
						default -> null;
					});
			AtomicReference<Throwable> failure = new AtomicReference<>();
			Thread worker = new Thread(() -> {
				try {
					provider.deliver(delta);
				} catch (Throwable error) {
					failure.set(error);
				}
			});
			worker.start();
			worker.join();
			assertNull(failure.get(), "resource delta must not touch SWT off-thread"); //$NON-NLS-1$
		} finally {
			shell.dispose();
		}
	}
}
