#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# G5 pre-check: the ECJ NLS problems a Tycho build would stop on, found before the build, with the build's compiler
# (ECJ 3.47, --release 21, no class files) over the source roots of every bundle whose JDT prefs make non-externalized
# strings an error. Two runs, as the bundles' prefs decide whether @SuppressWarnings may hide an optional error:
#   suppressOptionalErrors=enabled  -> `-warn:none -warn:+nls,+suppress` (NLS as a suppressible problem)
#   otherwise (the JDT default)     -> `-warn:none -err:+nls`            (@SuppressWarnings("nls"/"all") does not hide it)
# Output is a Maven-style log that fix_nls_tags.sh reads, so the fixes come from the compiler's own rows.
# Usage: nls_precheck.sh <worktree> <out-prefix>   -> <out>.{bundles,log,mvnlog}
#   env: ECJ (default: ecj 3.47.0-SNAPSHOT of the shared Maven repo), POOL_CP (classpath list, cuts type noise)
set -euo pipefail
W=$(cd "$1" && pwd -W); O="$2"; export LC_ALL=C
ECJ="${ECJ:-F:/m3-scratch/m2-eclipse/org/eclipse/jdt/ecj/3.47.0-SNAPSHOT/ecj-3.47.0-SNAPSHOT.jar}"
CP="${POOL_CP:-S:/m3-scratch/jdk21/gate/pool.cp}"; JAVA="${JAVA:-C:/Program Files/Java/jdk-21/bin/java}"
{ git -C "$W" grep -l -F 'nonExternalizedStringLiteral=error' -- '*org.eclipse.jdt.core.prefs' || true; } | sed '/^$/d' \
	| while IFS= read -r p; do b=$(printf '%s' "$p" | sed 's#/\?\.settings/org\.eclipse\.jdt\.core\.prefs$##')
		if grep -q 'suppressOptionalErrors=enabled' "$W/$p"; then printf 'suppress\t%s\n' "$b"; else printf 'strict\t%s\n' "$b"; fi
	done > "$O.bundles"
roots() { # roots <mode>: source folders of the bundles of that mode, from build.properties source..
	awk -F'\t' -v m="$1" '$1 == m { print $2 }' "$O.bundles" | while IFS= read -r b; do bp="$W/${b:+$b/}build.properties"
		if [ -f "$bp" ]; then
			awk 'BEGIN{c=0} /^[ \t]*source\.[^=]*=/ {c=1; sub(/^[^=]*=/,"")} c { line=$0; c=(line ~ /\\[ \t\r]*$/); gsub(/\\[ \t\r]*$/,"",line); gsub(/\r/,"",line); n=split(line, a, ","); for(i=1;i<=n;i++){ gsub(/^[ \t]+|[ \t]+$/,"",a[i]); if(a[i]!="") print a[i] } }' "$bp" \
				| sed "s|^|$W/${b:+$b/}|; s|/$||"
		fi
	done | sort -u | while read -r r; do if [ -d "$r" ]; then echo "$r"; fi; done; }
: > "$O.log"
for mode in suppress strict; do
	roots "$mode" > "$O.$mode.roots"; [ -s "$O.$mode.roots" ] || continue
	while read -r r; do find "$r" -name '*.java' -type f; done < "$O.$mode.roots" | sed 's|^|"|; s|$|"|' > "$O.$mode.sources"
	{ echo "-cp"; echo "\"$(tr '\n' ';' < "$CP")\""; echo "-sourcepath"; echo "\"$(tr '\n' ';' < "$O.$mode.roots")\""; } > "$O.$mode.args"
	if [ "$mode" = suppress ]; then NLS="-warn:+nls,+suppress"; else NLS="-err:+nls"; fi
	"$JAVA" -Xmx4g -jar "$ECJ" --release 21 -encoding UTF-8 -proc:none -d none -Xemacs -maxProblems 1000000 \
		-warn:none $NLS @"$O.$mode.args" @"$O.$mode.sources" >> "$O.log" 2>&1 || true
done
# Xemacs rows: <path>:<line>: error|warning: <message>  ->  the two-line Maven form fix_nls_tags.sh parses
{ grep -a -E '^[A-Za-z]:.*\.java:[0-9]+: (error|warning): (Non-externalized string literal|Unnecessary \$NON-NLS\$ tag)' "$O.log" || true; } \
	| tr -d '\r' | sed -E 's#^(.*\.java):([0-9]+): (error|warning): (.*)$#[ERROR] \1:[\2]\n[ERROR] \4#' | sed 's#/#\\#g' > "$O.mvnlog"
echo "$(basename "$W") nls pre-check: $(cut -f1 "$O.bundles" | sort | uniq -c | tr -s ' \n' ' ')bundles, $(cat "$O".*.sources 2>/dev/null | wc -l) files, $(grep -c 'Non-externalized' "$O.mvnlog" || true) missing + $(grep -c 'Unnecessary' "$O.mvnlog" || true) surplus tags"
