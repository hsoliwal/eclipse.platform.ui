// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.java.JavaParser;
import org.openrewrite.java.tree.J;
import org.openrewrite.text.PlainTextParser;

/** Executes the two unchanged SDK owners against the exact public UI preimages. */
final class M3EclipseUiBuildFixesRecipeTest {
    private static final String CRATE = "m3-eclipse-ui-build-fixes-20261019";
    private static final String NAME = "com.synexia.eclipse.UiBuildFixes20261019";
    private static final String JAVA = "bundles/org.eclipse.jface/src/org/eclipse/jface/viewers/TreeViewer.java";
    private static final String TEXT = "bundles/org.eclipse.core.commands/META-INF/MANIFEST.MF";

    private static String resource(String path) throws Exception {
        try (var input = M3EclipseUiBuildFixesRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Map<String, String> sources(boolean after) throws Exception {
        var result = new TreeMap<String, String>();
        for (String kind : List.of("java", "text")) {
            String root = "/com/synexia/rewrite/hash-pinned-" + kind + "/" + CRATE + "/";
            for (String line : resource(root + "manifest.tsv").lines().toList()) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] row = line.split("\t");
                result.put(row[0], resource(root + (after ? "" : "before-") + row[3]));
            }
        }
        assertEquals(java.util.Set.of(JAVA, TEXT), result.keySet());
        return result;
    }

    private static List<Path> classpath() {
        String value = System.getProperty("canonical.classpath", "");
        assertFalse(value.isBlank(), "The parser requires the genuine Eclipse dependency classpath");
        return java.util.Arrays.stream(value.split(java.io.File.pathSeparator)).map(Path::of).peek(path ->
                assertTrue(Files.exists(path), "Missing genuine parser dependency: " + path)).toList();
    }

    private static InMemoryExecutionContext context() {
        var context = new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
        context.putMessage("com.synexia.rewrite.hashPinnedJavaSnapshotRecipe.parserClasspath", classpath());
        return context;
    }

    private static List<SourceFile> parse(Map<String, String> sources) {
        var result = new ArrayList<SourceFile>();
        for (var entry : sources.entrySet()) {
            var input = List.of(Parser.Input.fromString(Path.of(entry.getKey()), entry.getValue()));
            var parsed = entry.getKey().endsWith(".java")
                    ? JavaParser.fromJavaVersion().classpath(classpath()).build().parseInputs(input, null, context()).toList()
                    : PlainTextParser.builder().build().parseInputs(input, null, context()).toList();
            assertEquals(1, parsed.size());
            assertEquals(entry.getValue(), parsed.getFirst().printAll());
            if (entry.getKey().endsWith(".java")) assertInstanceOf(J.CompilationUnit.class, parsed.getFirst());
            result.addAll(parsed);
        }
        return result;
    }

    private static Recipe recipe() throws Exception {
        try (var input = M3EclipseUiBuildFixesRecipeTest.class.getResourceAsStream("/META-INF/rewrite/" + CRATE + ".yml")) {
            assertNotNull(input);
            var loader = new org.openrewrite.config.YamlResourceLoader(input,
                    java.net.URI.create("classpath:/" + CRATE), new java.util.Properties());
            Recipe recipe = org.openrewrite.config.Environment.builder().load(loader).build().activateRecipes(NAME);
            assertTrue(recipe.validateAll().stream().allMatch(org.openrewrite.Validated::isValid));
            return recipe;
        }
    }

    private static Map<String, String> run(Recipe recipe, List<SourceFile> input) {
        var result = new TreeMap<String, String>();
        for (var change : recipe.run(new InMemoryLargeSourceSet(input), context(), 1).getChangeset().getAllResults()) {
            assertNotNull(change.getAfter());
            result.put(change.getAfter().getSourcePath().toString().replace('\\', '/'), change.getAfter().printAll());
        }
        return result;
    }

    private static Map<String, String> run(List<SourceFile> input) throws Exception {
        return run(recipe(), input);
    }

    @Test void namedGenerationAndFixedPoint() throws Exception {
        var generated = run(parse(sources(false)));
        assertEquals(sources(true), generated);
        assertTrue(run(parse(generated)).isEmpty());
        assertEquals(generated, run(parse(sources(false))));
        String destination = System.getProperty("materialize.dir", "");
        if (!destination.isBlank()) {
            Path root = Path.of(destination).toAbsolutePath().normalize();
            for (var entry : generated.entrySet()) {
                Path output = root.resolve(entry.getKey()).normalize();
                assertTrue(output.startsWith(root));
                Files.createDirectories(output.getParent());
                Files.writeString(output, entry.getValue());
            }
        }
    }

    @Test void missingAndDriftRefuse() throws Exception {
        for (String path : sources(false).keySet()) {
            var missing = sources(false);
            missing.remove(path);
            assertThrows(IllegalStateException.class, () -> run(parse(missing)));
            var drift = sources(false);
            drift.put(path, drift.get(path) + "\n// unauthorized source drift\n");
            assertThrows(IllegalStateException.class, () -> run(parse(drift)));
        }
    }

    @Test void duplicateTargetsAndWrongJavaKindRefuse() throws Exception {
        for (String path : sources(false).keySet()) {
            var input = parse(sources(false));
            input.add(input.stream().filter(source -> source.getSourcePath().toString().equals(path)).findFirst().orElseThrow());
            assertThrows(IllegalStateException.class, () -> run(input));
        }
        var wrong = parse(sources(false));
        wrong.removeIf(source -> source.getSourcePath().toString().equals(JAVA));
        wrong.addAll(PlainTextParser.builder().build().parseInputs(
                List.of(Parser.Input.fromString(Path.of(JAVA), sources(false).get(JAVA))), null, context()).toList());
        assertThrows(IllegalStateException.class, () -> run(wrong));
    }

    @Test void sourceOrderAndIndependentOwnersAgree() throws Exception {
        var reversed = parse(sources(false));
        java.util.Collections.reverse(reversed);
        assertEquals(sources(true), run(reversed));
        for (boolean reverseOwners : List.of(false, true)) {
            var merged = new TreeMap<String, String>();
            var owners = new ArrayList<Recipe>(List.of(
                    new M3HashPinnedJavaSnapshotRecipe(CRATE), new M3HashPinnedTextSnapshotRecipe(CRATE)));
            if (reverseOwners) java.util.Collections.reverse(owners);
            for (Recipe owner : owners) merged.putAll(run(owner, parse(sources(false))));
            assertEquals(sources(true), merged);
        }
    }

    @Test void unrelatedSourcesAndPartialReplayPreserved() throws Exception {
        var input = sources(false);
        input.put("src/test/java/decoy/Carrier.java", "package decoy;\nrecord Carrier(String value) {\n"
                + "  static final String TEXT = \"swtTreeOperation(\\\"expandToLevel\\\", int.class);\";\n"
                + "  // Bundle-Version: 3.13.100.qualifier\n}\n");
        input.put("misc/decoy.txt", "TreeViewer.java //$NON-NLS-1$\nBundle-Version: 3.13.100.qualifier\n");
        assertEquals(sources(true), run(parse(input)));
        for (String path : sources(false).keySet()) {
            var partial = sources(false);
            partial.put(path, sources(true).get(path));
            var expected = sources(true);
            expected.remove(path);
            assertEquals(expected, run(parse(partial)));
        }
    }

    @Test void noncanonicalTextAliasesRefuse() throws Exception {
        var aliases = new ArrayList<>(List.of("/" + TEXT, "bundles/./org.eclipse.core.commands/META-INF/MANIFEST.MF",
                "bundles/unused/../org.eclipse.core.commands/META-INF/MANIFEST.MF"));
        if (!Path.of("").getFileSystem().getSeparator().equals("\\")) aliases.add(TEXT.replace('/', '\\'));
        for (String alias : aliases) {
            var input = parse(sources(false));
            input.replaceAll(source -> source.getSourcePath().toString().equals(TEXT)
                    ? source.withSourcePath(Path.of(alias)) : source);
            assertThrows(IllegalStateException.class, () -> run(input));
        }
    }
}
