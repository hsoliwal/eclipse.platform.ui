// SPDX-License-Identifier: EPL-2.0
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Random;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;

/** Same independent client runs against the frozen baseline and generated classes. */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class JFaceArrayContracts {
    private static int checks;
    private static final StringBuilder trace = new StringBuilder();
    private JFaceArrayContracts() { }
    private static void require(boolean value) {
        checks++;
        if (!value) throw new AssertionError("check " + checks);
    }
    private static Throwable thrown(Runnable operation) {
        try { operation.run(); } catch (RuntimeException error) { return error; }
        throw new AssertionError("expected exception");
    }
    private static void equal(Object a, Object b) { require(java.util.Objects.equals(a, b)); }
    private static void array(Object[] a, Object[] b) { require(Arrays.equals(a, b)); }
    public static void main(String[] args) throws Exception {
        content(); filtering(); paths(); selections(); sorting();
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(trace.toString().getBytes(StandardCharsets.UTF_8)));
        System.out.println("checks=" + checks + " traceSha256=" + digest);
    }
    private static void content() {
        var provider = ArrayContentProvider.getInstance();
        require(provider == ArrayContentProvider.getInstance());
        Object[] data = {"a", null};
        require(provider.getElements(data) == data);
        for (Object input : new Object[] {null, 42, new int[] {1}, "unsupported"}) {
            Object[] a = provider.getElements(input), b = provider.getElements(input);
            equal(a.length, 0); require(a != b); equal(a.getClass(), Object[].class);
        }
        var list = new ArrayList<>(Arrays.asList(data));
        array(provider.getElements(list), data);
        require(provider.getElements(list) != data);
        final Object[] special = {"collection-owned"};
        var custom = new ArrayList<Object>() {
            private static final long serialVersionUID = 1L;
            @Override public Object[] toArray() { return special; }
        };
        require(provider.getElements(custom) == special);
    }
    private static void filtering() {
        Random random = new Random(30217);
        for (int round = 0; round < 1500; round++) {
            Object[] source = new Object[random.nextInt(128)];
            for (int i = 0; i < source.length; i++) source[i] = random.nextInt(11) == 0 ? null : random.nextInt(31);
            Object[] snapshot = source.clone();
            Object parent = new Object();
            int[] visited = {0};
            List<Object> expected = new ArrayList<>();
            for (Object x : source) if (x == null || ((Integer) x & 1) == 0) expected.add(x);
            var filter = new ViewerFilter() {
                @Override public boolean select(Viewer viewer, Object p, Object e) {
                    require(viewer == null && p == parent);
                    require(e == source[visited[0]++]);
                    trace.append(e).append(';');
                    return e == null || ((Integer) e & 1) == 0;
                }
            };
            Object[] result = filter.filter(null, parent, source);
            array(result, expected.toArray()); array(source, snapshot); equal(visited[0], source.length);
            require(source.length == 0 ? result == source : result != source);
            equal(result.getClass(), Object[].class);
        }
        String[] empty = {};
        var all = new ViewerFilter() {
            @Override public boolean select(Viewer v, Object p, Object e) { return true; }
        };
        require(all.filter(null, (Object) null, empty) == empty);
        String[] values = {"one", "two"};
        Object[] allResult = all.filter(null, (Object) null, values);
        equal(allResult.getClass(), Object[].class); require(allResult != values); array(allResult, values);
        Object[] mutable = {"first", "second", "third"};
        var mutating = new ViewerFilter() {
            @Override public boolean select(Viewer v, Object p, Object e) {
                if (e.equals("first")) { mutable[0] = "changed past"; mutable[1] = "changed future"; }
                trace.append(e).append('|'); return true;
            }
        };
        array(mutating.filter(null, (Object) null, mutable), new Object[] {"first", "changed future", "third"});
        RuntimeException sentinel = new IllegalStateException("same callback failure");
        int[] visits = {0};
        var failure = new ViewerFilter() {
            @Override public boolean select(Viewer v, Object p, Object e) {
                if (++visits[0] == 2) throw sentinel;
                return true;
            }
        };
        require(thrown(() -> failure.filter(null, (Object) null, mutable)) == sentinel); equal(visits[0], 2);
        Object[] overrideResult = {"override"};
        Object parent = new Object();
        var override = new ViewerFilter() {
            @Override public Object[] filter(Viewer v, Object p, Object[] e) { require(p == parent); return overrideResult; }
            @Override public boolean select(Viewer v, Object p, Object e) { throw new AssertionError(); }
        };
        require(override.filter(null, new TreePath(new Object[] {parent}), mutable) == overrideResult);
        require(thrown(() -> all.filter(null, (Object) null, null)) instanceof NullPointerException);
    }
    private static IElementComparer comparer(StringBuilder log) {
        return new IElementComparer() {
            @Override public boolean equals(Object a, Object b) {
                log.append(a).append('>').append(b).append(';');
                return java.util.Objects.equals(a, b);
            }
            @Override public int hashCode(Object a) { log.append('#').append(a).append(';'); return a == null ? 0 : a.hashCode(); }
            @Override public int hashCode() { return 73; }
        };
    }
    private static void paths() {
        require(TreePath.EMPTY.getParentPath() == null);
        require(TreePath.EMPTY.getFirstSegment() == null && TreePath.EMPTY.getLastSegment() == null);
        require(new TreePath(new Object[] {"one"}).getParentPath() == TreePath.EMPTY);
        for (int n = 0; n < 64; n++) {
            Object[] input = new Object[n];
            for (int i = 0; i < n; i++) input[i] = "s" + i;
            TreePath path = new TreePath(input), copy = new TreePath(input.clone());
            equal(path.getSegmentCount(), n); require(path.equals(copy)); require(path.equals(copy, comparer(trace)));
            require(!path.equals(null)); require(!path.equals("x")); require(!path.equals((TreePath) null, null));
            int hash = 0; for (Object x : input) hash += x.hashCode();
            equal(path.hashCode(), hash); equal(path.hashCode(comparer(trace)), hash);
            for (int j = 0; j <= n; j++) {
                TreePath prefix = new TreePath(Arrays.copyOf(input, j));
                require(path.startsWith(prefix, comparer(trace)));
                require(path.startsWith(prefix, null));
                if (j < n) require(!prefix.startsWith(path, null));
            }
            TreePath child = path.createChildPath("child");
            equal(child.getSegmentCount(), n + 1); equal(child.getLastSegment(), "child");
            require(path.equals(child.getParentPath()));
            if (n != 0) {
                require(path.getFirstSegment() == input[0]); require(path.getLastSegment() == input[n - 1]);
                input[0] = "mutated";
                equal(path.getSegment(0), "mutated"); equal(child.getSegment(0), "s0");
                equal(path.hashCode(), hash); // nonzero cache retained despite aliased constructor input
            }
        }
        // Prefix calls comparer(other, this); equal-length equality calls comparer(this, other).
        IElementComparer asymmetric = new IElementComparer() {
            @Override public boolean equals(Object a, Object b) { trace.append(a).append('/').append(b); return a.toString().startsWith(b.toString()); }
            @Override public int hashCode(Object a) { return a.hashCode(); }
        };
        require(new TreePath(new Object[] {"a", "tail"}).startsWith(new TreePath(new Object[] {"ab"}), asymmetric));
        require(!new TreePath(new Object[] {"a"}).startsWith(new TreePath(new Object[] {"ab"}), asymmetric));
        Object[] corrupt = {"a", "b"}; TreePath p = new TreePath(corrupt); corrupt[0] = null;
        equal(thrown(p::getParentPath).getClass().getName(), "org.eclipse.core.runtime.AssertionFailedException");
        equal(thrown(() -> p.createChildPath("x")).getClass().getName(), "org.eclipse.core.runtime.AssertionFailedException");
        equal(thrown(() -> new TreePath(null)).getClass().getName(), "org.eclipse.core.runtime.AssertionFailedException");
        require(thrown(() -> TreePath.EMPTY.getSegment(0)) instanceof ArrayIndexOutOfBoundsException);
        require(thrown(() -> TreePath.EMPTY.startsWith(null, null)) instanceof NullPointerException);
        Object[] zero = {""}; TreePath z = new TreePath(zero); equal(z.hashCode(), 0); zero[0] = "z"; equal(z.hashCode(), 122);
    }
    private static void selections() {
        IElementComparer cmp = comparer(trace);
        Random random = new Random(21417);
        for (int round = 0; round < 900; round++) {
            Object[] input = new Object[random.nextInt(64)];
            for (int i = 0; i < input.length; i++) input[i] = random.nextInt(9) == 0 ? null : "v" + random.nextInt(13);
            Object[] snapshot = input.clone();
            StructuredSelection selection = new StructuredSelection(input);
            StructuredSelection compared = new StructuredSelection(Arrays.asList(input), cmp);
            require(selection.equals(new StructuredSelection(Arrays.asList(input))));
            require(compared.equals(new StructuredSelection(Arrays.asList(input), cmp)));
            require(compared.equals(selection)); require(selection.equals(compared));
            equal(selection.size(), input.length); equal(selection.isEmpty(), input.length == 0);
            equal(selection.hashCode(), input.length == 0 ? 31 : Arrays.hashCode(input));
            int hash = 31 + cmp.hashCode();
            for (Object value : input) hash = 31 * hash + (value == null ? 0 : cmp.hashCode(value));
            equal(compared.hashCode(), hash);
            array(selection.toArray(), snapshot); array(selection.stream().toArray(), snapshot);
            List<Object> iterated = new ArrayList<>(); selection.iterator().forEachRemaining(iterated::add);
            array(iterated.toArray(), snapshot);
            if (input.length != 0) {
                input[0] = "outside"; equal(selection.getFirstElement(), snapshot[0]);
                Object[] exported = selection.toArray(); exported[0] = "export"; equal(selection.getFirstElement(), snapshot[0]);
                selection.toList().set(0, "view mutation"); equal(selection.getFirstElement(), "view mutation");
                require(thrown(() -> selection.toList().add("x")) instanceof UnsupportedOperationException);
            }
            trace.append(selection.hashCode()).append(':').append(compared.hashCode()).append(';');
        }
        StructuredSelection empty = new StructuredSelection();
        require(empty.equals(new StructuredSelection(new Object[0])));
        require(empty.toArray() != empty.toArray()); require(empty.toList() != empty.toList());
        require(empty.getFirstElement() == null); equal(empty.hashCode(), 31);
        int[] calls = {0};
        StructuredSelection override = new StructuredSelection(new Object[] {"x"}) {
            @Override public boolean isEmpty() { calls[0]++; return true; }
        };
        require(override.equals(empty)); equal(calls[0], 1);
        equal(override.hashCode(), 31); equal(calls[0], 2);
        require(override.getFirstElement() == null); equal(calls[0], 3);
        equal(override.stream().count(), 0L); equal(calls[0], 4);
        equal(thrown(() -> new StructuredSelection((Object[]) null)).getClass().getName(), "org.eclipse.core.runtime.AssertionFailedException");
        require(!empty.equals(null)); require(!empty.equals(Collections.emptyList()));
    }
    private static void sorting() {
        Random random = new Random(91217);
        for (int round = 0; round < 700; round++) {
            Object[] input = new Object[random.nextInt(128)];
            for (int i = 0; i < input.length; i++) input[i] = random.nextInt(25);
            Object[] expected = input.clone();
            StringBuilder oracle = new StringBuilder(), actual = new StringBuilder();
            Arrays.sort(expected, (a,b) -> { oracle.append(a).append('>').append(b).append(';'); return Integer.compare((Integer)a, (Integer)b); });
            var comparator = new ViewerComparator() {
                @Override public int compare(Viewer viewer, Object a, Object b) {
                    require(viewer == null); actual.append(a).append('>').append(b).append(';');
                    return Integer.compare((Integer)a, (Integer)b);
                }
            };
            comparator.sort(null, input); array(input, expected); equal(actual.toString(), oracle.toString());
            trace.append(actual);
        }
        Object a = new Object(), b = new Object(); Object[] ties = {a,b,a,b};
        new ViewerComparator() { @Override public int compare(Viewer v, Object x, Object y) { return 0; } }.sort(null, ties);
        require(ties[0] == a && ties[1] == b && ties[2] == a && ties[3] == b);
        RuntimeException sentinel = new IllegalStateException("sort callback");
        var failure = new ViewerComparator() { @Override public int compare(Viewer v, Object x, Object y) { throw sentinel; } };
        require(thrown(() -> failure.sort(null, new Object[] {2,1})) == sentinel);
        require(thrown(() -> new ViewerComparator().sort(null, null)) instanceof NullPointerException);
    }
}
