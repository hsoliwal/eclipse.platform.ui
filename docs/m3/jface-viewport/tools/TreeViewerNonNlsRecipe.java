// SPDX-License-Identifier: EPL-2.0
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Fail-closed, idempotent Eclipse NLS annotation recipe for the exact TreeViewer preimage.
 * Source literals and method bodies are unchanged; only //\u0024NON-NLS-1\u0024 comments are added.
 */
public final class TreeViewerNonNlsRecipe {
    private static final String ORIGINAL_GIT_BLOB =
            "88a47bcd2a5f57ebae1fe71bde045daa1637122c";
    private static final String MARKER = " //$NON-NLS-1$";
    private static final List<String> EXACT_EXPRESSIONS = List.of(
            "swtTreeOperation(\"expandToLevel\", int.class);",
            "swtTreeOperation(\"expandToLevel\", TreeItem.class, int.class);",
            "swtTreeOperation(\"collapseToLevel\", int.class);",
            "swtTreeOperation(\"collapseToLevel\", TreeItem.class, int.class);",
            "swtTreeOperation(\"expandSelectionToLevel\", int.class);",
            "swtTreeOperation(\"collapseSelectionToLevel\", int.class);",
            "swtTreeQuery(\"getExpandedItems\", TreeItem[].class);",
            "swtTreeOperation(\"setExpandedItems\", TreeItem[].class);",
            "throw new IllegalStateException(\"Unable to invoke SWT Tree bulk operation\", failure);",
            "throw new IllegalStateException(\"Unable to invoke SWT Tree state query\", failure);");

    private TreeViewerNonNlsRecipe() {}

    public static String apply(final String source) {
        String result = java.util.Objects.requireNonNull(source, "source");
        for (String expression : EXACT_EXPRESSIONS) {
            final int first = result.indexOf(expression);
            if (first < 0 || result.indexOf(expression, first + 1) >= 0) {
                throw new IllegalArgumentException("NLS_EXPECTED_EXACTLY_ONE:" + expression);
            }
            final int after = first + expression.length();
            if (result.startsWith(MARKER, after)) {
                continue;
            }
            if (after < result.length()
                    && result.charAt(after) != '\n'
                    && result.charAt(after) != '\r') {
                throw new IllegalArgumentException("NLS_UNEXPECTED_SUFFIX:" + expression);
            }
            result = result.substring(0, after) + MARKER + result.substring(after);
        }
        return result;
    }

    private static String blobSha1(final byte[] bytes) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(("blob " + bytes.length + "\u0000").getBytes(StandardCharsets.US_ASCII));
            digest.update(bytes);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("GIT_SHA1_UNAVAILABLE", unavailable);
        }
    }

    public static void main(final String[] args) throws IOException {
        if (args.length != 2 || (!args[0].equals("--apply") && !args[0].equals("--check"))) {
            throw new IllegalArgumentException("usage: TreeViewerNonNlsRecipe --apply|--check path");
        }
        final Path path = Path.of(args[1]).toAbsolutePath().normalize();
        final byte[] original = Files.readAllBytes(path);
        final String source = new String(original, StandardCharsets.UTF_8);
        final String changed = apply(source);
        if (!changed.equals(apply(changed))) {
            throw new IllegalStateException("NLS_RECIPE_NOT_FIXED_POINT");
        }
        if (args[0].equals("--apply")) {
            if (!blobSha1(original).equals(ORIGINAL_GIT_BLOB)) {
                throw new IllegalStateException("NLS_SOURCE_PREIMAGE_DRIFT");
            }
            Files.writeString(path, changed, StandardCharsets.UTF_8);
        } else if (!source.equals(changed)) {
            throw new IllegalStateException("NLS_RECIPE_NOT_APPLIED");
        }
        System.out.println("sourceSha1=" + blobSha1(original));
        System.out.println("postimageSha1=" + blobSha1(changed.getBytes(StandardCharsets.UTF_8)));
        System.out.println("annotations=" + EXACT_EXPRESSIONS.size());
        System.out.println("fixedPoint=true");
    }
}
