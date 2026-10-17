// SPDX-License-Identifier: Apache-2.0
import java.lang.management.ManagementFactory;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/** Allocation and CPU observation only; not a universal performance claim. */
public final class JFaceArrayCostProbe {
    private static volatile Object[] sink;
    private JFaceArrayCostProbe() { }
    public static void main(String[] args) {
        int mask = Integer.parseInt(args[0]);
        Object[] input = new Object[1024];
        for (int i = 0; i < input.length; i++) input[i] = i;
        var filter = new ViewerFilter() {
            @Override public boolean select(Viewer v, Object parent, Object element) { return ((Integer) element & mask) == 0; }
        };
        var bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (!bean.isThreadAllocatedMemorySupported() || !bean.isCurrentThreadCpuTimeSupported()) throw new AssertionError("missing counters");
        bean.setThreadAllocatedMemoryEnabled(true); bean.setThreadCpuTimeEnabled(true);
        for (int i = 0; i < 20000; i++) sink = filter.filter(null, (Object) null, input);
        long id = Thread.currentThread().threadId();
        long allocated = bean.getThreadAllocatedBytes(id), cpu = bean.getCurrentThreadCpuTime();
        int loops = 30000;
        for (int i = 0; i < loops; i++) sink = filter.filter(null, (Object) null, input);
        long elapsed = bean.getCurrentThreadCpuTime() - cpu;
        long bytes = bean.getThreadAllocatedBytes(id) - allocated;
        System.out.println("mask=" + mask + " rows=1024 loops=" + loops + " result=" + sink.length
                + " allocatedBytesPerCall=" + ((double)bytes/loops) + " cpuNsPerCall=" + ((double)elapsed/loops));
    }
}
