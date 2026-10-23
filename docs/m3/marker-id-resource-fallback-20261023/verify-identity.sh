#!/usr/bin/env bash
# SPDX-License-Identifier: Apache-2.0
# Extract the exact source-backed helper into Java21 stubs; not an Eclipse build.
set -euo pipefail
module="$(cd "$(dirname "$0")/../../.." && pwd)"
source="$module/bundles/org.eclipse.ui.ide/src/org/eclipse/ui/internal/views/markers/M3MarkerLanes.java"
tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
python3 - "$source" "$tmp/M3MarkerLanes.java" <<'PY'
import pathlib,sys
source=pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")
anchor="static boolean sameIdentity(IMarker first, IMarker second) {"
if source.count(anchor)!=1:
    raise SystemExit("source-backed helper anchor missing/drifted")
start=source.index(anchor)
brace=source.index("{",start)
depth=0
end=None
for i in range(brace,len(source)):
    ch=source[i]
    if ch=="{":depth+=1
    elif ch=="}":
        depth-=1
        if depth==0:
            end=i+1
            break
if end is None:raise SystemExit("unbalanced helper")
helper=source[start:end]
out="interface IResource { }\ninterface IMarker { long getId(); IResource getResource(); }\n"
out+="final class M3MarkerLanes {\n"+helper+"\n}\n"
pathlib.Path(sys.argv[2]).write_text(out,encoding="utf-8")
PY
cat > "$tmp/IdentityOracle.java" <<'JAVA'
record Resource(String path) implements IResource { }
record Marker(long id, IResource resource) implements IMarker {
    @Override public long getId() { return id; }
    @Override public IResource getResource() { return resource; }
}
public class IdentityOracle {
    private static int assertions;
    static void check(boolean actual, boolean expected, String why) {
        assertions++;
        if (actual != expected) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        IResource a = new Resource("/project/A.java");
        IResource duplicateHandle = new Resource("/project/A.java");
        IResource b = new Resource("/project/B.java");
        IMarker base = new Marker(101L, a);
        check(M3MarkerLanes.sameIdentity(base, new Marker(101L, duplicateHandle)), true,
                "same id / resources equal across wrappers");
        check(M3MarkerLanes.sameIdentity(base, new Marker(101L, b)), false,
                "same id / distinct resources");
        check(M3MarkerLanes.sameIdentity(base, new Marker(102L, a)), false,
                "distinct id / same resource");
        check(M3MarkerLanes.sameIdentity(base, new Marker(101L, null)), false,
                "null target resource fail-closed");
        check(M3MarkerLanes.sameIdentity(new Marker(101L, null), new Marker(101L, null)), false,
                "ambiguous null resources fail-closed");
        check(M3MarkerLanes.sameIdentity(base, null), false, "null marker fail-closed");
        check(M3MarkerLanes.sameIdentity(null, null), false, "two null markers fail-closed");
        check(M3MarkerLanes.sameIdentity(base, base), true, "same marker");
        System.out.println("M3_MARKER_ID_RESOURCE_GUARD_PASS assertions="+assertions);
    }
}
JAVA
javac --release 21 -Xlint:all -Werror -d "$tmp" \
  "$tmp/M3MarkerLanes.java" "$tmp/IdentityOracle.java"
java -cp "$tmp" IdentityOracle
java -Xint -cp "$tmp" IdentityOracle
echo "SOURCE_BACKED_HELPER_PASS (not Eclipse/Equinox qualification)"
