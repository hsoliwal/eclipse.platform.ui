// SPDX-License-Identifier: Apache-2.0
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.namespace.PackageNamespace;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

/** Real OSGi resolution and execution, without native controls or display claims. */
public final class JFaceEquinoxProof {
    private JFaceEquinoxProof() { }
    public static void main(String[] args) throws Exception {
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).findFirst().orElseThrow();
        Framework framework = factory.newFramework(Map.of(Constants.FRAMEWORK_STORAGE, args[0],
                Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT));
        framework.init(); framework.start();
        try {
            var context = framework.getBundleContext();
            List<Bundle> dependencies = new ArrayList<>();
            for (String path : Files.readAllLines(Path.of(args[1]))) {
                if (!path.isBlank()) dependencies.add(context.installBundle(Path.of(path).toUri().toString()));
            }
            Bundle host = context.installBundle(Path.of(args[2]).toUri().toString());
            Bundle client = context.installBundle(Path.of(args[3]).toUri().toString());
            if (!framework.adapt(FrameworkWiring.class).resolveBundles(List.of(host, client))) {
                for (Bundle b : context.getBundles()) System.err.println(b.getSymbolicName() + " state=" + b.getState());
                throw new AssertionError("real bundle resolution failed");
            }
            host.start(); client.start();
            Class<?> facade = host.loadClass("org.eclipse.jface.viewers.ViewerFilter");
            Class<?> clientFacade = client.loadClass(facade.getName());
            if (facade != clientFacade) throw new AssertionError("client is not using host facade");
            boolean candidate = Boolean.parseBoolean(args[4]);
            try {
                Class<?> atom = host.loadClass("org.eclipse.jface.internal.m3.M3ViewerArrays");
                if (!candidate || atom.getClassLoader() != facade.getClassLoader()) throw new AssertionError("atom owner mismatch");
            } catch (ClassNotFoundException error) {
                if (candidate) throw error;
            }
            var caps = host.adapt(BundleWiring.class).getCapabilities(PackageNamespace.PACKAGE_NAMESPACE);
            if (caps.stream().anyMatch(c -> "org.eclipse.jface.internal.m3".equals(c.getAttributes().get(PackageNamespace.PACKAGE_NAMESPACE)))) {
                throw new AssertionError("internal atom package was exported");
            }
            client.loadClass("JFaceArrayContracts").getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            System.out.println("EQUINOX_RESOLVED runtime=" + framework.getVersion() + " host=" + host.getVersion()
                    + " candidate=" + candidate + " installedDependencies=" + dependencies.size() + " internalPackageExported=false");
        } finally {
            framework.stop();
            if (framework.waitForStop(30000).getType() == org.osgi.framework.FrameworkEvent.WAIT_TIMEDOUT) throw new AssertionError("framework stop timeout");
        }
    }
}
