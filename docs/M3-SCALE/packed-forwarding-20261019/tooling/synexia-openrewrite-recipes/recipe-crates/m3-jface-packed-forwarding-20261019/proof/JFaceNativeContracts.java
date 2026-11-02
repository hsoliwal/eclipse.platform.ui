// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
import java.util.Arrays;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;

/** Runs on the real Display with native Table/Tree controls, preserving all SWT owners. */
public final class JFaceNativeContracts {
    private static int checks;
    private static long events;
    private static void check(boolean condition) { checks++; if (!condition) throw new AssertionError(checks); }
    private record Row(int id, String label) { @Override public String toString() { return label; } }
    private static IElementComparer comparer() {
        return new IElementComparer() {
            @Override public int hashCode(Object element) { events++; return element instanceof Row row ? row.id() % 7 : System.identityHashCode(element); }
            @Override public boolean equals(Object a, Object b) { events++; return a instanceof Row left && b instanceof Row right ? left.id() == right.id() : a == b; }
        };
    }
    private static void table(Shell shell, int style) {
        TableViewer viewer = new TableViewer(shell, style); viewer.setUseHashlookup(true);
        viewer.setContentProvider(ArrayContentProvider.getInstance()); viewer.setLabelProvider(new LabelProvider());
        viewer.setComparer(comparer());
        Row[] rows = new Row[513]; for (int i = 0; i < rows.length; i++) rows[i] = new Row(i, "row-" + i);
        var model = new java.util.ArrayList<>(Arrays.asList(rows));
        viewer.setInput(model);
        viewer.setComparator(new ViewerComparator() {
            @Override public int compare(Viewer v, Object a, Object b) { return Integer.compare(((Row) b).id(), ((Row) a).id()); }
        });
        ViewerFilter filter = new ViewerFilter() {
            @Override public boolean select(Viewer v, Object parent, Object value) { events++; return (((Row) value).id() & 3) == 0; }
        };
        viewer.addFilter(filter); check(viewer.getTable().getItemCount() == 129);
        for (int i = 0; i < 129; i++) {
            viewer.getTable().getItem(i).getText(); // Trigger original SWT.VIRTUAL SetData before observing data.
            check(((Row) viewer.getElementAt(i)).id() == 512 - 4 * i);
        }
        for (int id = 0; id < 513; id += 4) {
            viewer.setSelection(new StructuredSelection(new Row(id, "equivalent")), true);
            check(((Row) viewer.getStructuredSelection().getFirstElement()).id() == id);
        }
        viewer.setComparer(comparer()); // rebuilds the map using its copy constructor
        viewer.setSelection(new StructuredSelection(new Row(128, "copy lookup")), true);
        check(((Row) viewer.getStructuredSelection().getFirstElement()).id() == 128);
        viewer.removeFilter(filter); check(viewer.getTable().getItemCount() == 513);
        for (TableItem item : viewer.getTable().getItems()) item.getText();
        model.removeAll(Arrays.asList(Arrays.copyOf(rows, 100)));
        viewer.remove(Arrays.copyOf(rows, 100)); check(viewer.getTable().getItemCount() == 413);
        for (TableItem item : viewer.getTable().getItems()) item.getText();
        Row added = new Row(700, "added"); model.add(added);
        viewer.add(added); check(viewer.getTable().getItemCount() == 414);
        viewer.setSelection(new StructuredSelection(new Row(700, "equivalent")), true);
        // Frozen baseline observation: incremental insertion in a sorted virtual table
        // leaves the new row unmapped until refresh. Preserve, do not hide, this existing
        // behavior in a contract-preserving map pass (see QUALIFICATION_CORRECTIONS.md).
        if ((style & SWT.VIRTUAL) != 0) check(viewer.getStructuredSelection().isEmpty());
        else check(((Row) viewer.getStructuredSelection().getFirstElement()).id() == 700);
        viewer.refresh();
        for (TableItem item : viewer.getTable().getItems()) item.getText();
        viewer.setSelection(new StructuredSelection(new Row(700, "after refresh")), true);
        check(((Row) viewer.getStructuredSelection().getFirstElement()).id() == 700);
        viewer.getControl().dispose(); check(viewer.getControl().isDisposed());
    }
    private static void tree(Shell shell) {
        TreeViewer viewer = new TreeViewer(shell, SWT.MULTI); viewer.setUseHashlookup(true); viewer.setComparer(comparer());
        viewer.setLabelProvider(new LabelProvider());
        Row root = new Row(0, "root"); Row[] parents = new Row[32]; Row[][] children = new Row[32][8];
        for (int p = 0; p < parents.length; p++) {
            parents[p] = new Row(p + 1, "p" + p);
            for (int c = 0; c < 8; c++) children[p][c] = new Row(100 + p * 8 + c, "c" + p + ":" + c);
        }
        viewer.setContentProvider(new ITreeContentProvider() {
            @Override public Object[] getElements(Object input) { return parents; }
            @Override public Object[] getChildren(Object element) {
                int id = ((Row) element).id(); return id > 0 && id <= 32 ? children[id - 1] : new Object[0];
            }
            @Override public Object getParent(Object element) {
                int id = ((Row) element).id(); return id >= 100 ? parents[(id - 100) / 8] : root;
            }
            @Override public boolean hasChildren(Object element) { return ((Row) element).id() <= 32; }
        });
        viewer.setInput(root); viewer.expandAll(); check(viewer.getExpandedTreePaths().length == 32);
        TreePath[] paths = new TreePath[32];
        for (int i = 0; i < 32; i++) paths[i] = new TreePath(new Object[] {parents[i], children[i][i % 8]});
        viewer.setSelection(new TreeSelection(paths, viewer.getComparer()), true);
        check(viewer.getStructuredSelection().size() == 32);
        TreePath[] expanded = viewer.getExpandedTreePaths(); viewer.collapseAll();
        viewer.setExpandedTreePaths(expanded); check(viewer.getExpandedTreePaths().length == 32);
        viewer.setComparer(comparer()); viewer.refresh();
        viewer.setSelection(new TreeSelection(paths, viewer.getComparer()), true); check(viewer.getStructuredSelection().size() == 32);
        viewer.remove(children[0][0]); check(viewer.getTree().getItem(0).getItemCount() == 7);
        viewer.getControl().dispose(); check(viewer.getControl().isDisposed());
    }
    public static void main(String[] args) {
        Display display = new Display();
        try {
            Shell shell = new Shell(display); shell.setSize(640, 480); shell.open();
            table(shell, SWT.MULTI); table(shell, SWT.MULTI | SWT.VIRTUAL); tree(shell);
            shell.dispose();
            System.out.println("NATIVE_CONTRACTS checks=" + checks + " callbacks=" + events + " swt=" + SWT.getVersion());
        } finally { display.dispose(); }
    }
}
