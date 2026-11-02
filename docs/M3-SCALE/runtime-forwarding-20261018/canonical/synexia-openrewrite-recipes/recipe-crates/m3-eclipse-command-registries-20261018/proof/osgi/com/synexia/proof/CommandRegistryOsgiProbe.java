// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.proof;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.framework.wiring.FrameworkWiring;

/** Resolve and execute the actual generated commands bundle in genuine Equinox. */
public final class CommandRegistryOsgiProbe {
    private static int checks;

    private CommandRegistryOsgiProbe() { }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new IllegalArgumentException("commands.jar common.jar storage-directory");
        FrameworkFactory factory = ServiceLoader.load(FrameworkFactory.class).findFirst().orElseThrow();
        Framework framework = factory.newFramework(Map.of(
                Constants.FRAMEWORK_STORAGE, Path.of(args[2]).toAbsolutePath().toString(),
                Constants.FRAMEWORK_STORAGE_CLEAN, Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT));
        try {
            framework.init();
            framework.start();
            Bundle common = framework.getBundleContext().installBundle(Path.of(args[1]).toUri().toString());
            Bundle commands = framework.getBundleContext().installBundle(Path.of(args[0]).toUri().toString());
            check(framework.adapt(FrameworkWiring.class).resolveBundles(List.of(common, commands)), "actual bundle resolution");
            common.start();
            commands.start();
            check(common.getState() == Bundle.ACTIVE, "Equinox common ACTIVE");
            check(commands.getState() == Bundle.ACTIVE, "commands ACTIVE");
            check("org.eclipse.core.commands".equals(commands.getSymbolicName()), "actual commands symbolic name");
            check("JavaSE-21".equals(commands.getHeaders().get("Bundle-RequiredExecutionEnvironment")), "Java21 BREE");
            BundleWiring wiring = commands.adapt(BundleWiring.class);
            check(wiring != null, "actual commands wiring");
            check(wiring.getRequiredWires(BundleRevision.BUNDLE_NAMESPACE).stream()
                    .anyMatch(w -> "org.eclipse.equinox.common".equals(w.getProviderWiring().getBundle().getSymbolicName())),
                    "commands uses real Equinox common provider");
            check(wiring.getCapabilities(BundleRevision.PACKAGE_NAMESPACE).stream()
                    .noneMatch(c -> "org.eclipse.core.internal.commands.m3".equals(c.getAttributes().get(BundleRevision.PACKAGE_NAMESPACE))),
                    "M3 storage remains bundle-private");

            Class<?> managerClass = commands.loadClass("org.eclipse.core.commands.CommandManager");
            Object manager = managerClass.getConstructor().newInstance();
            var getCategory = managerClass.getMethod("getCategory", String.class);
            var getParameterType = managerClass.getMethod("getParameterType", String.class);
            for (int i = 0; i < 4096; i++) {
                String id = "osgi." + i;
                Object category = getCategory.invoke(manager, id);
                Object parameter = getParameterType.invoke(manager, id);
                check(category == getCategory.invoke(manager, new String(id)), "category identity at " + i);
                check(parameter == getParameterType.invoke(manager, new String(id)), "parameter identity at " + i);
            }
            for (String name : List.of("categoriesById", "parameterTypesById")) {
                Field field = managerClass.getDeclaredField(name);
                field.setAccessible(true);
                Object owner = field.get(manager);
                check(owner.getClass().getName().equals("org.eclipse.core.internal.commands.m3.PackedHashMap"),
                        name + " uses the donated owner");
                check(owner.getClass().getClassLoader() == wiring.getClassLoader(),
                        name + " resolves from the commands bundle");
                check(((Map<?, ?>) owner).size() == 4096, name + " cardinality");
            }
            for (String type : List.of("PackedHashMap", "SlotMap", "OrderedSlotMap", "SlotArrays", "SlotSpliterators")) {
                Class<?> implementation = commands.loadClass("org.eclipse.core.internal.commands.m3." + type);
                check(implementation.getClassLoader() == wiring.getClassLoader(), "receiver-owned " + type);
            }
            System.out.println("OSGI_COMMAND_REGISTRIES_PASS checks=" + checks
                    + " commands=" + commands.getVersion() + " common=" + common.getVersion()
                    + " framework=" + framework.getVersion());
        } finally {
            framework.stop();
            FrameworkEvent stopped = framework.waitForStop(10000);
            if (stopped.getType() == FrameworkEvent.WAIT_TIMEDOUT) throw new AssertionError("framework did not stop");
        }
    }
}
