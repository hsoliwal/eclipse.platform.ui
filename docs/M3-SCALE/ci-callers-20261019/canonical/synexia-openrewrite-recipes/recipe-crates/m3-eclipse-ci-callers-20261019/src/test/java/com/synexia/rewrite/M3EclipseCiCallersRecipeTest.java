// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.config.YamlResourceLoader;
import org.openrewrite.internal.InMemoryLargeSourceSet;
import org.openrewrite.text.PlainTextParser;

/** Runs the same exact existing caller recipe in two independent receiver inputs. */
final class M3EclipseCiCallersRecipeTest {
    private static final String CRATE = "m3-eclipse-ci-callers-20261019";
    private static final String NAME = "com.synexia.eclipse.CiCallers20261019";
    private static final String TARGET = ".github/workflows/ci.yml";
    private static final String ROOT = "/com/synexia/rewrite/hash-pinned-text/" + CRATE + "/";
    private static final String BEFORE_SHA = "a87ca1d04beb4f49a19628db2e46b32e62b302b716c89ce408faf4d8d228bf1d";
    private static final String OLD_USES = "    uses: eclipse-platform/eclipse.platform.releng.aggregator/"
            + ".github/workflows/mavenBuild.yml@master\n";
    private static final List<String> RECEIVERS = List.of("eclipse.platform", "eclipse.platform.ui");

    private static String resource(String path) throws Exception {
        try (var input = M3EclipseCiCallersRecipeTest.class.getResourceAsStream(path)) {
            assertNotNull(input, path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String before(String receiver) throws Exception {
        assertTrue(RECEIVERS.contains(receiver));
        return resource(ROOT + (receiver.equals("eclipse.platform") ? "before-platform-ci.yml.txt" : "before-ui-ci.yml.txt"));
    }

    private static String before() throws Exception { return before(RECEIVERS.getFirst()); }
    private static String after() throws Exception { return resource(ROOT + "ci.yml.txt"); }

    private static String sha(String text) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(text.getBytes(StandardCharsets.UTF_8)));
    }

    private static String newUses() throws Exception {
        String commit = resource(ROOT + "bound-workflow-commit.txt").strip();
        assertEquals(40, commit.length());
        assertEquals(20, HexFormat.of().parseHex(commit).length);
        assertEquals(commit, commit.toLowerCase(Locale.ROOT));
        return "    uses: hsoliwal/eclipse.platform.releng.aggregator/"
                + ".github/workflows/mavenBuild.yml@" + commit + "\n";
    }

    private static InMemoryExecutionContext context() {
        return new InMemoryExecutionContext(error -> { throw new AssertionError(error); });
    }

    private static List<SourceFile> parse(Map<String, String> input) {
        var sources = new ArrayList<SourceFile>();
        for (var item : input.entrySet()) {
            sources.addAll(PlainTextParser.builder().build().parseInputs(
                    List.of(Parser.Input.fromString(Path.of(item.getKey()), item.getValue())),
                    null, context()).toList());
        }
        return sources;
    }

    private static Recipe namedRecipe() throws Exception {
        try (var input = M3EclipseCiCallersRecipeTest.class.getResourceAsStream(
                "/META-INF/rewrite/" + CRATE + ".yml")) {
            assertNotNull(input);
            var loader = new YamlResourceLoader(input, URI.create("classpath:/" + CRATE), new Properties());
            var recipe = Environment.builder().load(loader).build().activateRecipes(NAME);
            assertTrue(recipe.validateAll().stream().allMatch(org.openrewrite.Validated::isValid));
            return recipe;
        }
    }

    private static Map<String, String> run(Recipe recipe, List<SourceFile> sources) {
        var output = new TreeMap<String, String>();
        for (var result : recipe.run(new InMemoryLargeSourceSet(sources), context(), 1)
                .getChangeset().getAllResults()) {
            assertNotNull(result.getBefore(), "No new workflow is admitted");
            assertNotNull(result.getAfter(), "No workflow deletion is admitted");
            var path = result.getAfter().getSourcePath().toString().replace('\\', '/');
            assertNull(output.put(path, result.getAfter().printAll()), "duplicate output path");
        }
        return output;
    }

    private static Map<String, String> run(List<SourceFile> sources) throws Exception {
        return run(namedRecipe(), sources);
    }

    private static void assertCallerContract(String original, String output) throws Exception {
        assertEquals(BEFORE_SHA, sha(original));
        String replacement = newUses();
        assertEquals(resource(ROOT + "reviewed-after.sha256").strip(), sha(output));
        assertTrue(original.contains(OLD_USES));
        assertEquals(original.indexOf(OLD_USES), original.lastIndexOf(OLD_USES));
        assertTrue(output.contains(replacement));
        assertEquals(output.indexOf(replacement), output.lastIndexOf(replacement));
        assertEquals(original.replace(OLD_USES, replacement), output);
        assertEquals(original, output.replace(replacement, OLD_USES),
                "Every caller trigger, concurrency setting, input, permission and other byte is preserved");
    }

    @Test void namedRecipePinsEachReceiverAndMaterializes() throws Exception {
        for (String receiver : RECEIVERS) {
            String original = before(receiver);
            var output = run(parse(Map.of(TARGET, original)));
            assertEquals(1, output.size(), "The SDK must pin the existing caller for " + receiver);
            assertEquals(Map.of(TARGET, after()), output);
            assertCallerContract(original, output.get(TARGET));
            String destination = System.getProperty("materialize.dir", "");
            if (!destination.isBlank()) {
                Path target = Path.of(destination).resolve(receiver).resolve(TARGET);
                Files.createDirectories(target.getParent());
                Files.writeString(target, output.get(TARGET), StandardCharsets.UTF_8);
            }
        }
    }

    @Test void eachReceiverPostimageIsFixedPointAndManifestIsClosed() throws Exception {
        var owner = new M3HashPinnedTextSnapshotRecipe(CRATE);
        assertEquals(List.of(TARGET), owner.targetPaths());
        assertEquals(Map.of(TARGET, BEFORE_SHA), owner.preimageManifest());
        for (String receiver : RECEIVERS) {
            assertCallerContract(before(receiver), after());
            assertTrue(run(parse(Map.of(TARGET, after()))).isEmpty());
        }
        assertTrue(run(owner, parse(Map.of(TARGET, after()))).isEmpty());
    }

    @Test void missingAndDriftedCallersRefuse() throws Exception {
        assertThrows(IllegalStateException.class, () -> run(List.of()));
        assertThrows(IllegalStateException.class,
                () -> run(parse(Map.of(".github/workflows/CI.yml", before()))));
        String baseline = before();
        for (String changed : List.of(
                baseline + "\npermissions: write-all\n",
                baseline.replace("cancel-in-progress: true", "cancel-in-progress: false"),
                baseline.replace("branches: [ master ]", "branches: [ main ]"),
                baseline.replace("      - 'docs/**'\n", ""),
                baseline.replace("@master", "@develop"),
                baseline.replace("\n", "\r\n"))) {
            assertNotEquals(baseline, changed);
            assertThrows(IllegalStateException.class, () -> run(parse(Map.of(TARGET, changed))));
        }
    }

    @Test void duplicatesAndNoncanonicalTargetPathsRefuse() throws Exception {
        var duplicate = parse(Map.of(TARGET, before()));
        duplicate.addAll(parse(Map.of(TARGET, before())));
        assertThrows(IllegalStateException.class, () -> run(duplicate));
        for (String alias : List.of(".github/./workflows/ci.yml", ".github/workflows/../workflows/ci.yml")) {
            assertThrows(IllegalStateException.class, () -> run(parse(Map.of(alias, before()))));
        }
        if (Path.of(TARGET).getFileSystem().getSeparator().equals("/")) {
            assertThrows(IllegalStateException.class,
                    () -> run(parse(Map.of(".github\\workflows\\ci.yml", before()))));
        }
    }

    private static Map<String, String> withDecoys() throws Exception {
        return Map.of(TARGET, before(), ".github/workflows/other.yml", before(),
                "notes/ci.yml", "literal: |\n" + OLD_USES + "  this is an unrelated source\n");
    }

    @Test void unrelatedSourcesAndInputPermutationsArePreserved() throws Exception {
        var input = parse(withDecoys());
        assertEquals(Map.of(TARGET, after()), run(input));
        Collections.reverse(input);
        assertEquals(Map.of(TARGET, after()), run(input));
    }

    @Test void directNamedSerialAndTwoWorkerReplaysAgree() throws Exception {
        var expected = Map.of(TARGET, after());
        assertEquals(expected, run(new M3HashPinnedTextSnapshotRecipe(CRATE), parse(withDecoys())));
        assertEquals(expected, run(parse(withDecoys())));
        var trials = new ArrayList<Callable<Map<String, String>>>();
        for (int i = 0; i < 4; i++) trials.add(() -> run(parse(withDecoys())));
        try (var pool = Executors.newFixedThreadPool(2)) {
            for (var future : pool.invokeAll(trials)) assertEquals(expected, future.get());
        }
        assertEquals(expected, run(parse(withDecoys())));
    }
}
