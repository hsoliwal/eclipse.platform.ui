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
package org.eclipse.ui.ide.m3bench;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

/**
 * Markers view benchmark and differential, run inside the workbench through
 * {@code org.eclipse.ui.startup}. System properties:
 * <ul>
 * <li>{@code m3.markers.out}: result TSV (appended);</li>
 * <li>{@code m3.markers.label}: label of this run (side and size);</li>
 * <li>{@code m3.markers.n}: number of problem markers (1000 per file);</li>
 * <li>{@code m3.markers.log}: when set, the differential log of the seeded
 * script ({@code m3.markers.seed}, {@code m3.markers.steps}).</li>
 * </ul>
 * Measured per single marker change: the time until the Problems view title
 * changes, the bytes allocated by all threads, and the process CPU time; then
 * the heap after a full GC and the instance counts of the view's model classes.
 * The JVM halts at the end (no workspace save).
 */
public final class M3MarkersBench implements IStartup {

	private static final Logger LOG = Logger.getLogger("org.eclipse.ui.ide.m3bench"); //$NON-NLS-1$
	private static final String PROBLEM_VIEW = "org.eclipse.ui.views.ProblemView"; //$NON-NLS-1$
	private static final int PER_FILE = 1000;
	private static final long SETTLE_MILLIS = 800;

	private Path out;
	private String label;
	private Display display;
	private IViewPart view;
	private final List<IMarker> markers = new ArrayList<>();
	private final List<IFile> files = new ArrayList<>();

	@Override
	public void earlyStartup() {
		Thread t = new Thread(this::runSafely, "m3-markers-bench"); //$NON-NLS-1$
		t.setDaemon(true);
		t.start();
	}

	private void runSafely() {
		int status = 0;
		try {
			run();
		} catch (Throwable e) {
			LOG.log(Level.SEVERE, "markers bench failed", e); //$NON-NLS-1$
			row("ERROR", -1, -1, -1, String.valueOf(e)); //$NON-NLS-1$
			status = 2;
		}
		Runtime.getRuntime().halt(status);
	}

	private void run() throws Exception {
		out = Path.of(System.getProperty("m3.markers.out")); //$NON-NLS-1$
		label = System.getProperty("m3.markers.label", "run"); //$NON-NLS-1$ //$NON-NLS-2$
		int n = Integer.getInteger("m3.markers.n", 10000).intValue(); //$NON-NLS-1$
		display = PlatformUI.getWorkbench().getDisplay();
		waitForWindow();
		long t0 = System.nanoTime();
		IProject project = createProject(n);
		createMarkers(n);
		row("create-markers", n, millis(t0), -1, ""); //$NON-NLS-1$ //$NON-NLS-2$
		t0 = System.nanoTime();
		view = ui(() -> page().showView(PROBLEM_VIEW));
		String counted = NumberFormat.getIntegerInstance().format(countSeverity(IMarker.SEVERITY_ERROR));
		String desc = waitDescription(d -> d.contains(counted), 600_000);
		row("initial-build", n, millis(t0), -1, desc);
		settle();
		for (int rep = 0; rep < 5; rep++) {
			measureChange(n, rep);
		}
		heap(n);
		String log = System.getProperty("m3.markers.log"); //$NON-NLS-1$
		if (log != null) {
			differential(Path.of(log), Long.getLong("m3.markers.seed", 1).longValue(), //$NON-NLS-1$
					Integer.getInteger("m3.markers.steps", 30).intValue(), project); //$NON-NLS-1$
		}
	}

	/** One severity change (error to warning), measured until the title shows it, then reverted. */
	private void measureChange(int n, int rep) throws Exception {
		IMarker marker = markers.get(rep * 3);
		String before = description();
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory
				.getThreadMXBean();
		com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) ManagementFactory
				.getOperatingSystemMXBean();
		long bytes = threads.getTotalThreadAllocatedBytes();
		long cpu = os.getProcessCpuTime();
		long t0 = System.nanoTime();
		workspace().run((IWorkspaceRunnable) m -> marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING),
				null);
		String after = waitDescription(d -> !d.equals(before), 600_000);
		long t = millis(t0);
		long allocated = threads.getTotalThreadAllocatedBytes() - bytes;
		long cpuMillis = (os.getProcessCpuTime() - cpu) / 1_000_000;
		row("one-change-ms", n, t, rep, after);
		row("one-change-bytes", n, allocated, rep, ""); //$NON-NLS-1$
		row("one-change-cpu-ms", n, cpuMillis, rep, ""); //$NON-NLS-1$
		workspace().run((IWorkspaceRunnable) m -> marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR),
				null);
		waitDescription(d -> d.equals(before), 600_000);
		settle();
	}

	private void heap(int n) throws Exception {
		for (int i = 0; i < 3; i++) {
			System.gc();
			Thread.sleep(200);
		}
		long used = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
		row("heap-used-bytes", n, used, -1, ""); //$NON-NLS-1$
		String jcmd = Path.of(System.getProperty("java.home"), "bin", "jcmd").toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		Process p = new ProcessBuilder(jcmd, String.valueOf(ProcessHandle.current().pid()), "GC.class_histogram") //$NON-NLS-1$
				.redirectErrorStream(true).start();
		String histogram = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		p.waitFor();
		for (String line : histogram.split("\\R")) { //$NON-NLS-1$
			if (line.matches(".*(MarkerEntry|MarkerCategory|MarkerInfo|TreeItem|M3Marker\\w*)\\s*(\\(.*)?$")) { //$NON-NLS-1$
				String[] f = line.trim().split("\\s+"); //$NON-NLS-1$
				if (f.length >= 4) {
					row("instances " + f[3], n, Long.parseLong(f[1]), -1, f[2] + " bytes"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	/** Seeded marker edits; after each, the view's answers are written to the log. */
	private void differential(Path log, long seed, int steps, IProject project) throws Exception {
		Random random = new Random(seed);
		List<String> lines = new ArrayList<>();
		lines.add("start | " + answers()); //$NON-NLS-1$
		for (int step = 0; step < steps; step++) {
			String op = edit(random, project);
			settle();
			lines.add(step + " " + op + " | " + answers()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		Files.write(log, lines, StandardCharsets.UTF_8);
	}

	private String edit(Random random, IProject project) throws CoreException {
		AtomicReference<String> op = new AtomicReference<>();
		workspace().run((IWorkspaceRunnable) m -> {
			int kind = random.nextInt(10);
			IMarker marker = markers.get(random.nextInt(markers.size()));
			switch (kind) {
			case 0, 1, 2 -> {
				int severity = random.nextInt(3);
				marker.setAttribute(IMarker.SEVERITY, severity);
				op.set("severity " + marker.getId() + "=" + severity); //$NON-NLS-1$ //$NON-NLS-2$
			}
			case 3, 4 -> {
				String message = "Edited problem " + random.nextInt(1_000_000); //$NON-NLS-1$
				marker.setAttribute(IMarker.MESSAGE, message);
				op.set("message " + marker.getId()); //$NON-NLS-1$
			}
			case 5, 6 -> {
				IFile file = files.get(random.nextInt(files.size()));
				int count = 1 + random.nextInt(5);
				for (int i = 0; i < count; i++) {
					markers.add(problem(file, random.nextInt(3), "Added problem " + random.nextInt(1_000_000), //$NON-NLS-1$
							random.nextInt(5000)));
				}
				op.set("add " + count); //$NON-NLS-1$
			}
			case 7, 8 -> {
				markers.remove(marker);
				marker.delete();
				op.set("delete " + marker.getId()); //$NON-NLS-1$
			}
			default -> {
				marker.setAttribute(IMarker.LINE_NUMBER, random.nextInt(5000));
				op.set("line " + marker.getId()); //$NON-NLS-1$
			}
			}
		}, null);
		return op.get();
	}

	/** The view's answers: title, then every category with its label, expansion and shown children. */
	private String answers() {
		return ui(() -> {
			StringBuilder b = new StringBuilder(((ViewPart) view).getContentDescription());
			Tree tree = viewTree();
			if (tree != null) {
				for (TreeItem category : tree.getItems()) {
					b.append(" || ").append(text(category)).append(category.getExpanded() ? " +" : " -"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					if (category.getExpanded()) {
						for (TreeItem child : category.getItems()) {
							b.append(" | ").append(text(child)); //$NON-NLS-1$
						}
					}
				}
			}
			return b.toString();
		});
	}

	/** The markers view's tree, through its package-private {@code getViewer()} (reflection: internal class). */
	private Tree viewTree() throws ReflectiveOperationException {
		for (Class<?> c = view.getClass(); c != null; c = c.getSuperclass()) {
			try {
				Method m = c.getDeclaredMethod("getViewer"); //$NON-NLS-1$
				m.setAccessible(true);
				return ((TreeViewer) m.invoke(view)).getTree();
			} catch (NoSuchMethodException e) {
				// try the superclass
			}
		}
		return null;
	}

	private static String text(TreeItem item) {
		int columns = Math.max(1, item.getParent().getColumnCount());
		StringBuilder b = new StringBuilder();
		for (int c = 0; c < columns; c++) {
			if (c > 0) {
				b.append(" ~ "); //$NON-NLS-1$
			}
			b.append(item.getText(c));
		}
		return b.toString();
	}

	private IProject createProject(int n) throws CoreException {
		IProject project = workspace().getRoot().getProject("m3bench"); //$NON-NLS-1$
		workspace().run((IWorkspaceRunnable) m -> {
			if (!project.exists()) {
				project.create(m);
			}
			project.open(m);
			for (int f = 0; f * PER_FILE < n; f++) {
				IFile file = project.getFile(String.format("f%05d.txt", f)); //$NON-NLS-1$
				if (!file.exists()) {
					file.create(new ByteArrayInputStream(new byte[0]), true, m);
				}
				files.add(file);
			}
		}, null);
		return project;
	}

	private void createMarkers(int n) throws CoreException {
		workspace().run((IWorkspaceRunnable) m -> {
			for (int i = 0; i < n; i++) {
				IFile file = files.get(i / PER_FILE);
				markers.add(problem(file, i % 3, "Problem " + i + " in " + file.getName(), i % PER_FILE)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}, null);
	}

	private static IMarker problem(IFile file, int severity, String message, int line) throws CoreException {
		IMarker marker = file.createMarker(IMarker.PROBLEM);
		marker.setAttributes(new String[] { IMarker.SEVERITY, IMarker.MESSAGE, IMarker.LINE_NUMBER },
				new Object[] { Integer.valueOf(severity), message, Integer.valueOf(line) });
		return marker;
	}

	private int countSeverity(int severity) {
		int count = 0;
		for (IMarker m : markers) {
			if (m.getAttribute(IMarker.SEVERITY, -1) == severity) {
				count++;
			}
		}
		return count;
	}

	private String waitDescription(Predicate<String> done, long timeoutMillis) throws InterruptedException {
		long end = System.currentTimeMillis() + timeoutMillis;
		String d = description();
		while (!done.test(d)) {
			if (System.currentTimeMillis() > end) {
				throw new IllegalStateException("view did not update: " + d); //$NON-NLS-1$
			}
			Thread.sleep(2);
			d = description();
		}
		return d;
	}

	/** Waits until no markers job is pending and the title is stable for {@link #SETTLE_MILLIS}. */
	private void settle() throws InterruptedException {
		String last = description();
		long stableSince = System.currentTimeMillis();
		while (System.currentTimeMillis() - stableSince < SETTLE_MILLIS || markersJobsPending()) {
			Thread.sleep(20);
			String d = description();
			if (!d.equals(last) || markersJobsPending()) {
				last = d;
				stableSince = System.currentTimeMillis();
			}
		}
	}

	private static boolean markersJobsPending() {
		for (Job job : Job.getJobManager().find(null)) {
			String name = job.getClass().getName();
			if (name.contains(".views.markers.") && job.getState() != Job.NONE) { //$NON-NLS-1$
				return true;
			}
		}
		return false;
	}

	private String description() {
		return ui(() -> ((ViewPart) view).getContentDescription());
	}

	private void waitForWindow() throws InterruptedException {
		while (ui(() -> PlatformUI.getWorkbench().getActiveWorkbenchWindow()) == null) {
			Thread.sleep(100);
		}
	}

	private IWorkbenchPage page() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		return window.getActivePage();
	}

	interface UiCall<T> {
		T call() throws Exception;
	}

	private <T> T ui(UiCall<T> call) {
		AtomicReference<T> result = new AtomicReference<>();
		AtomicReference<Exception> failure = new AtomicReference<>();
		display.syncExec(() -> {
			try {
				result.set(call.call());
			} catch (Exception e) {
				failure.set(e);
			}
		});
		if (failure.get() != null) {
			throw new IllegalStateException(failure.get());
		}
		return result.get();
	}

	private static IWorkspace workspace() {
		return ResourcesPlugin.getWorkspace();
	}

	private static long millis(long t0) {
		return (System.nanoTime() - t0) / 1_000_000;
	}

	private void row(String metric, int n, long value, int rep, String note) {
		String line = label + '\t' + metric + '\t' + n + '\t' + value + '\t' + rep + '\t' + note.replace('\t', ' ')
				+ '\n';
		try {
			Files.writeString(out, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "cannot write " + out, e); //$NON-NLS-1$
		}
	}
}
