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
import java.util.concurrent.atomic.AtomicInteger;
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
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
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
		// phase lines in the run log: a run that stops before the bench shows where (2026-10-24: two runs ended their
		// log at the workbench theme line and hit the 1800 s timeout without a row)
		LOG.info("bench start: label " + label + ", n " + n); //$NON-NLS-1$ //$NON-NLS-2$
		waitForWindow();
		LOG.info("bench: window up"); //$NON-NLS-1$
		long t0 = System.nanoTime();
		IProject project = createProject(n);
		createMarkers(n);
		row("create-markers", n, millis(t0), -1, ""); //$NON-NLS-1$ //$NON-NLS-2$
		LOG.info("bench: markers created in " + millis(t0) + " ms"); //$NON-NLS-1$ //$NON-NLS-2$
		t0 = System.nanoTime();
		view = ui(() -> page().showView(PROBLEM_VIEW));
		String counted = NumberFormat.getIntegerInstance().format(countSeverity(IMarker.SEVERITY_ERROR));
		String desc = waitDescription(d -> d.contains(counted), 600_000);
		row("initial-build", n, millis(t0), -1, desc);
		LOG.info("bench: initial build " + millis(t0) + " ms, " + desc); //$NON-NLS-1$ //$NON-NLS-2$
		settle();
		ui(() -> {
			viewer().expandAll(); // the categories as a user works with them: windows of rows shown
			return null;
		});
		settle();
		Job.getJobManager().addJobChangeListener(jobCounter);
		if (!Boolean.getBoolean("m3.markers.skipMeasure")) { //$NON-NLS-1$
			for (int k : new int[] { 1, 100, 10_000 }) {
				if (k > n) {
					continue;
				}
				for (int rep = 0; rep < (k >= 10_000 ? 3 : 5); rep++) {
					measureBatch(n, k, rep);
				}
			}
			heap(n);
		}
		String log = System.getProperty("m3.markers.log"); //$NON-NLS-1$
		if (log != null) {
			LOG.info("bench: differential to " + log); //$NON-NLS-1$
			differential(Path.of(log), n, Long.getLong("m3.markers.seed", 1).longValue(), //$NON-NLS-1$
					Integer.getInteger("m3.markers.steps", 30).intValue(), project); //$NON-NLS-1$
		}
	}

	/** Counts the markers view's UI updates (each one a full tree refresh) and clean rebuild jobs. */
	private final JobChangeAdapter jobCounter = new JobChangeAdapter() {
		@Override
		public void done(IJobChangeEvent event) {
			String name = event.getJob().getClass().getName();
			if (name.endsWith(".views.markers.UIUpdateJob")) { //$NON-NLS-1$
				refreshes.incrementAndGet();
			} else if (name.endsWith(".views.markers.MarkerUpdateJob")) { //$NON-NLS-1$
				rebuilds.incrementAndGet();
			}
		}
	};
	private final AtomicInteger refreshes = new AtomicInteger();
	private final AtomicInteger rebuilds = new AtomicInteger();

	/**
	 * One delta of {@code k} markers (error to warning, in one workspace operation, so one resource change event
	 * carries {@code k} marker deltas), measured until the title shows it, then restored to the exact former
	 * severities (not measured).
	 */
	private void measureBatch(int n, int k, int rep) throws Exception {
		List<IMarker> batch = new ArrayList<>(k);
		int stride = k * 3 <= n ? 3 : 1;
		for (int i = 0; i < k; i++) {
			// i * 3 + 2 has severity error (i % 3) while the stride is 3
			batch.add(markers.get(stride == 3 ? i * 3 + 2 : i));
		}
		int[] former = new int[k];
		for (int i = 0; i < k; i++) {
			former[i] = batch.get(i).getAttribute(IMarker.SEVERITY, -1);
		}
		String before = description();
		int refreshed = refreshes.get();
		int rebuilt = rebuilds.get();
		com.sun.management.ThreadMXBean threads = (com.sun.management.ThreadMXBean) ManagementFactory
				.getThreadMXBean();
		com.sun.management.OperatingSystemMXBean os = (com.sun.management.OperatingSystemMXBean) ManagementFactory
				.getOperatingSystemMXBean();
		long bytes = threads.getTotalThreadAllocatedBytes();
		long cpu = os.getProcessCpuTime();
		long t0 = System.nanoTime();
		workspace().run((IWorkspaceRunnable) m -> {
			for (IMarker marker : batch) {
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			}
		}, null);
		String after = waitDescription(d -> !d.equals(before), 600_000);
		long t = millis(t0);
		long allocated = threads.getTotalThreadAllocatedBytes() - bytes;
		long cpuMillis = (os.getProcessCpuTime() - cpu) / 1_000_000;
		settle();
		row("delta" + k + "-ms", n, t, rep, after); //$NON-NLS-1$
		row("delta" + k + "-bytes", n, allocated, rep, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("delta" + k + "-cpu-ms", n, cpuMillis, rep, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("delta" + k + "-full-refreshes", n, refreshes.get() - refreshed, rep, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("delta" + k + "-rebuilds", n, rebuilds.get() - rebuilt, rep, ""); //$NON-NLS-1$ //$NON-NLS-2$
		workspace().run((IWorkspaceRunnable) m -> {
			for (int i = 0; i < k; i++) {
				batch.get(i).setAttribute(IMarker.SEVERITY, former[i]);
			}
		}, null);
		waitDescription(d -> d.equals(before), 600_000);
		settle();
	}

	/** Types into the view's search box (the private {@code filteredTree} field of ExtendedMarkersView). */
	private void filterText(String text) throws ReflectiveOperationException {
		for (Class<?> c = view.getClass(); c != null; c = c.getSuperclass()) {
			try {
				java.lang.reflect.Field f = c.getDeclaredField("filteredTree"); //$NON-NLS-1$
				f.setAccessible(true);
				Object tree = f.get(view);
				ui(() -> {
					// MarkersFilteredTree extends the e4 FilteredTree, not org.eclipse.ui.dialogs.FilteredTree: reach
					// the search box through the accessor both trees declare
					java.lang.reflect.Method control = tree.getClass().getMethod("getFilterControl"); //$NON-NLS-1$
					control.setAccessible(true);
					((org.eclipse.swt.widgets.Text) control.invoke(tree)).setText(text);
					return null;
				});
				return;
			} catch (NoSuchFieldException e) {
				// try the superclass
			}
		}
		throw new NoSuchFieldException("filteredTree"); //$NON-NLS-1$
	}

	/** The markers view's viewer, through its package-private {@code getViewer()} (reflection: internal class). */
	private TreeViewer viewer() throws ReflectiveOperationException {
		for (Class<?> c = view.getClass(); c != null; c = c.getSuperclass()) {
			try {
				Method m = c.getDeclaredMethod("getViewer"); //$NON-NLS-1$
				m.setAccessible(true);
				return (TreeViewer) m.invoke(view);
			} catch (NoSuchMethodException e) {
				// try the superclass
			}
		}
		throw new NoSuchMethodException("getViewer"); //$NON-NLS-1$
	}

	private void heap(int n) throws Exception {
		for (int i = 0; i < 3; i++) {
			System.gc();
			Thread.sleep(200);
		}
		long used = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed();
		row("heap-used-bytes", n, used, -1, ""); //$NON-NLS-1$
		// GC.class_histogram in process (DiagnosticCommand MBean): jcmd cannot attach to a JVM on the hidden desktop
		// and blocked the run forever
		String histogram = String.valueOf(ManagementFactory.getPlatformMBeanServer().invoke(
				new javax.management.ObjectName("com.sun.management:type=DiagnosticCommand"), "gcClassHistogram", //$NON-NLS-1$ //$NON-NLS-2$
				new Object[] { new String[0] }, new String[] { String[].class.getName() }));
		for (String line : histogram.split("\\R")) { //$NON-NLS-1$
			if (line.matches(
					".*\\s(\\S*\\.(MarkerEntry|MarkerCategory|MarkerInfo|MarkerAttributeMap|M3Marker\\w*)|org\\.eclipse\\.swt\\.widgets\\.TreeItem|java\\.text\\.\\w*CollationKey|java\\.util\\.concurrent\\.ConcurrentHashMap(\\$Node)?)\\s*(\\(.*)?$")) { //$NON-NLS-1$
				String[] f = line.trim().split("\\s+"); //$NON-NLS-1$
				if (f.length >= 4) {
					row("instances " + f[3], n, Long.parseLong(f[1]), -1, f[2] + " bytes"); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
	}

	/**
	 * Seeded marker edits; after each, the view's answers are written to the log. Then a resize sweep and a
	 * column-resize sweep (viewport deltas, brief rule 24) with their answers. The number of full tree refreshes
	 * (UI update jobs) and clean rebuilds during each phase go to the results, not to the log.
	 */
	private void differential(Path log, int n, long seed, int steps, IProject project) throws Exception {
		Random random = new Random(seed);
		List<String> lines = new ArrayList<>();
		ui(() -> {
			TreeViewer viewer = viewer();
			List<Object> rows = new ArrayList<>();
			for (TreeItem category : viewer.getTree().getItems()) {
				for (TreeItem child : category.getItems()) {
					if (rows.size() < 3 && child.getData() != null) {
						rows.add(child.getData());
					}
				}
			}
			viewer.setSelection(new org.eclipse.jface.viewers.StructuredSelection(rows));
			return null;
		});
		settle();
		lines.add("start | " + answers()); //$NON-NLS-1$
		int refreshed = refreshes.get();
		int rebuilt = rebuilds.get();
		for (int step = 0; step < steps; step++) {
			String op = edit(random, project);
			settle();
			lines.add(step + " " + op + " | " + answers()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		row("diff-edits-full-refreshes", n, refreshes.get() - refreshed, steps, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("diff-edits-rebuilds", n, rebuilds.get() - rebuilt, steps, ""); //$NON-NLS-1$ //$NON-NLS-2$
		refreshed = refreshes.get();
		int[] items = { ui(() -> Integer.valueOf(itemCount(viewer().getTree()))).intValue(), 0 };
		for (int step = 0; step < 6; step++) {
			int s = step;
			ui(() -> {
				org.eclipse.swt.widgets.Shell shell = viewer().getTree().getShell();
				org.eclipse.swt.graphics.Point size = shell.getSize();
				shell.setSize(size.x + (s % 2 == 0 ? -120 : 120), size.y + (s % 2 == 0 ? -90 : 90));
				return null;
			});
			settle();
			lines.add("resize " + step + " | " + answers()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		for (int step = 0; step < 6; step++) {
			int s = step;
			ui(() -> {
				Tree tree = viewer().getTree();
				org.eclipse.swt.widgets.TreeColumn column = tree.getColumn(s % tree.getColumnCount());
				column.setWidth(Math.max(20, column.getWidth() + (s % 2 == 0 ? 60 : -60)));
				return null;
			});
			settle();
			lines.add("column " + step + " | " + answers()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		items[1] = ui(() -> Integer.valueOf(itemCount(viewer().getTree()))).intValue();
		row("diff-resize-full-refreshes", n, refreshes.get() - refreshed, 12, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("diff-resize-items-delta", n, items[1] - items[0], 12, ""); //$NON-NLS-1$ //$NON-NLS-2$
		// the search box: typed text (stock: PatternFilter over every child, then the limit; M3: a hit lane), with
		// marker edits while the text is active, then the text cleared
		refreshed = refreshes.get();
		rebuilt = rebuilds.get();
		for (String text : new String[] { "Added", "problem 1", "" }) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			filterText(text);
			Thread.sleep(600); // the FilteredTree refresh job runs after its own delay
			settle();
			lines.add("search '" + text + "' | " + answers()); //$NON-NLS-1$ //$NON-NLS-2$
			if (!text.isEmpty()) {
				for (int step = 0; step < 3; step++) {
					String op = edit(random, project);
					settle();
					lines.add("search '" + text + "' " + step + " " + op + " | " + answers()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
			}
		}
		row("diff-search-full-refreshes", n, refreshes.get() - refreshed, 9, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("diff-search-rebuilds", n, rebuilds.get() - rebuilt, 9, ""); //$NON-NLS-1$ //$NON-NLS-2$
		hiddenSweep(lines, n);
		scopeSweep(lines, n);
		Files.write(log, lines, StandardCharsets.UTF_8);
		row("diff-counters", n, -1, -1, deltaCounters()); //$NON-NLS-1$
	}

	/**
	 * Hidden viewport (brief rule 24): the view is minimized, a build-like batch changes up to 10^4 markers in one
	 * operation, then the view is restored. While hidden the tree must do nothing (no paint, no item created or
	 * disposed); after the restore its rows must equal stock's, after one window patch.
	 */
	private void hiddenSweep(List<String> lines, int n) throws Exception {
		int k = Math.min(10_000, markers.size() / 2);
		int[] paints = { 0 };
		org.eclipse.swt.widgets.Listener counter = e -> paints[0]++;
		ui(() -> {
			viewer().getTree().addListener(org.eclipse.swt.SWT.Paint, counter);
			page().setPartState(page().getReference(view), IWorkbenchPage.STATE_MINIMIZED);
			return null;
		});
		settle();
		int itemsBefore = ui(() -> Integer.valueOf(itemCount(viewer().getTree()))).intValue();
		paints[0] = 0;
		int refreshed = refreshes.get();
		workspace().run((IWorkspaceRunnable) m -> {
			for (int i = 0; i < k; i++) {
				IMarker marker = markers.get(i * (markers.size() / k));
				marker.setAttribute(IMarker.SEVERITY, (marker.getAttribute(IMarker.SEVERITY, 0) + 1) % 3);
			}
		}, null);
		settle();
		int hiddenPaints = paints[0];
		int hiddenItems = ui(() -> Integer.valueOf(itemCount(viewer().getTree()))).intValue() - itemsBefore;
		int hiddenRefreshes = refreshes.get() - refreshed;
		paints[0] = 0;
		refreshed = refreshes.get();
		ui(() -> {
			page().setPartState(page().getReference(view), IWorkbenchPage.STATE_RESTORED);
			page().activate(view);
			return null;
		});
		settle();
		lines.add("hidden " + k + " then shown | " + answers()); //$NON-NLS-1$ //$NON-NLS-2$
		row("diff-hidden-paints", n, hiddenPaints, k, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("diff-hidden-items-delta", n, hiddenItems, k, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("diff-hidden-ui-updates", n, hiddenRefreshes, k, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("diff-shown-paints", n, paints[0], k, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("diff-shown-ui-updates", n, refreshes.get() - refreshed, k, ""); //$NON-NLS-1$ //$NON-NLS-2$
		ui(() -> {
			viewer().getTree().removeListener(org.eclipse.swt.SWT.Paint, counter);
			return null;
		});
	}

	/**
	 * Scope and filter changes (Bug 296695): up to four declared filter groups are enabled in turn, with the
	 * selection on one file and then on the project, and disabled again; each step logs the answers. Stock rebuilds
	 * on each; the M3 path diffs a gather against its lanes. Reached through the generator's package-private API.
	 */
	private void scopeSweep(List<String> lines, int n) throws Exception {
		Object generator = invoke(view, "getGenerator"); //$NON-NLS-1$
		List<Object> groups = new ArrayList<>((java.util.Collection<?>) invoke(generator, "getAllFilters")); //$NON-NLS-1$
		int refreshed = refreshes.get();
		int rebuilt = rebuilds.get();
		int steps = Math.min(4, groups.size());
		for (int g = 0; g < steps; g++) {
			Object group = groups.get(g);
			String name = String.valueOf(invoke(group, "getName")); //$NON-NLS-1$
			ui(() -> invoke(generator, "toggleFilter", group)); //$NON-NLS-1$
			settle();
			lines.add("filter toggled " + name + " | " + answers()); //$NON-NLS-1$ //$NON-NLS-2$
			for (Object[] selection : new Object[][] { { files.get(Math.min(1, files.size() - 1)) },
					{ files.get(0).getProject() } }) {
				ui(() -> invoke(generator, "updateSelectedResource", selection, Boolean.TRUE)); //$NON-NLS-1$
				settle();
				lines.add("select " + selection[0] + " | " + answers()); //$NON-NLS-1$ //$NON-NLS-2$
			}
			ui(() -> invoke(generator, "toggleFilter", group)); //$NON-NLS-1$
			settle();
			lines.add("filter toggled back " + name + " | " + answers()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		row("diff-scope-full-refreshes", n, refreshes.get() - refreshed, steps, ""); //$NON-NLS-1$ //$NON-NLS-2$
		row("diff-scope-rebuilds", n, rebuilds.get() - rebuilt, steps, ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/* a declared method of the object's class or a superclass, made accessible (internal package-private API) */
	private static Object invoke(Object target, String name, Object... args) throws ReflectiveOperationException {
		for (Class<?> c = target.getClass(); c != null; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.getName().equals(name) && m.getParameterCount() == args.length) {
					m.setAccessible(true);
					return m.invoke(target, args);
				}
			}
		}
		throw new NoSuchMethodException(target.getClass().getName() + '.' + name);
	}

	private static int itemCount(Tree tree) {
		int count = 0;
		for (TreeItem category : tree.getItems()) {
			count += 1 + category.getItemCount();
		}
		return count;
	}

	/** The M3 delta path's counters (applied, fallbacks, builds), when the M3 bundle is installed; else "-". */
	private String deltaCounters() {
		try {
			// the view class lives in org.eclipse.ui.ide: its loader sees the internal markers package
			Class<?> c = Class.forName("org.eclipse.ui.internal.views.markers.M3MarkerDeltas", true, //$NON-NLS-1$
					view.getClass().getClassLoader());
			Method m = c.getDeclaredMethod("counters"); //$NON-NLS-1$
			m.setAccessible(true);
			return String.valueOf(m.invoke(null));
		} catch (ReflectiveOperationException | LinkageError e) {
			return "-"; //$NON-NLS-1$
		}
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

	/**
	 * The view's answers: title, then every category with its label, expansion and shown children, then the
	 * selection (widget and viewer).
	 */
	private String answers() {
		return ui(() -> {
			StringBuilder b = new StringBuilder(((ViewPart) view).getContentDescription());
			TreeViewer viewer = viewer();
			Tree tree = viewer.getTree();
			for (TreeItem category : tree.getItems()) {
				b.append(" || ").append(text(category)).append(category.getExpanded() ? " +" : " -"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (category.getExpanded()) {
					for (TreeItem child : category.getItems()) {
						b.append(" | ").append(text(child)); //$NON-NLS-1$
					}
				}
			}
			b.append(" || selection"); //$NON-NLS-1$
			for (TreeItem item : tree.getSelection()) {
				b.append(" | ").append(text(item)); //$NON-NLS-1$
			}
			b.append(" || viewer selection ").append(viewer.getStructuredSelection().size()); //$NON-NLS-1$
			return b.toString();
		});
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
		long since = System.currentTimeMillis();
		long stableSince = since;
		long logged = since;
		while (System.currentTimeMillis() - stableSince < SETTLE_MILLIS || markersJobsPending()) {
			Thread.sleep(20);
			String d = description();
			if (!d.equals(last) || markersJobsPending()) {
				last = d;
				stableSince = System.currentTimeMillis();
			}
			long now = System.currentTimeMillis();
			if (now - logged >= 10_000) {
				// a hang shows up in the run log with its state (2026-10-24: a stock side settled for 30 minutes)
				logged = now;
				String state = "settle: " + (now - since) / 1000 + " s, description '" + d + "', pending " + pendingJobNames(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				LOG.info(state);
			}
			if (now - since > 300_000) {
				throw new IllegalStateException("settle: markers jobs still pending after 300 s: " + pendingJobNames()); //$NON-NLS-1$
			}
		}
	}

	/*
	 * A markers job: the stock jobs are classes of org.eclipse.ui.internal.views.markers; the M3 jobs ("M3 marker
	 * deltas", "M3 marker scope") are Job.create instances, anonymous classes of org.eclipse.core.runtime.jobs, so
	 * they are matched by name (2026-10-24: at 10^6 the scope job outlived the 800 ms settle window and the m3 side
	 * was captured one step behind on every scope and filter change).
	 */
	private static boolean isMarkersJob(Job job) {
		return job.getClass().getName().contains(".views.markers.") || job.getName().startsWith("M3 marker"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private static boolean markersJobsPending() {
		for (Job job : Job.getJobManager().find(null)) {
			if (isMarkersJob(job) && job.getState() != Job.NONE) {
				return true;
			}
		}
		return false;
	}

	/* the markers jobs that are not NONE, with their state (for the settle log) */
	private static String pendingJobNames() {
		StringBuilder b = new StringBuilder();
		for (Job job : Job.getJobManager().find(null)) {
			if (isMarkersJob(job) && job.getState() != Job.NONE) {
				String name = job.getClass().getName().contains(".views.markers.") ? job.getClass().getName() : job.getName(); //$NON-NLS-1$
				b.append(name.substring(name.lastIndexOf('.') + 1)).append('=').append(job.getState()).append(' ');
			}
		}
		return b.length() == 0 ? "none" : b.toString().trim(); //$NON-NLS-1$
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
