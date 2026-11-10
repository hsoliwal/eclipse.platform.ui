#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# G5 pre-check, generalised from nls_precheck.sh: the optional ECJ problems that bundles compile as errors and that the
# fork's lanes and added M3 classes have tripped (NLS tags, unused imports), found with the build's compiler (ECJ 3.47,
# --release 21, no class files) before the Tycho build. Bundles are grouped by what their JDT prefs make an error and by
# suppressOptionalErrors; one ECJ run per group:
#   suppressOptionalErrors=enabled -> the problems run as warnings with +suppress (@SuppressWarnings may hide them)
#   otherwise (JDT default)        -> the problems run as errors (@SuppressWarnings does not hide them)
# Output: a Maven-style log for fix_nls_tags.sh and fix_unused_imports.sh.
# Usage: ecj_precheck.sh <worktree> <out-prefix>   -> <out>.{groups,log,mvnlog}
set -euo pipefail
W=$(cd "$1" && pwd -W); O="$2"; export LC_ALL=C
ECJ="${ECJ:-F:/m3-scratch/m2-eclipse/org/eclipse/jdt/ecj/3.47.0-SNAPSHOT/ecj-3.47.0-SNAPSHOT.jar}"
CP="${POOL_CP:-S:/m3-scratch/jdk21/gate/pool.cp}"; JAVA="${JAVA:-C:/Program Files/Java/jdk-21/bin/java}"
# group key per bundle: <mode>:<tokens>, tokens = nls and/or unusedImport (those set to error in the bundle's prefs)
{ git -C "$W" grep -l -E 'nonExternalizedStringLiteral=error|unusedImport=error' -- '*org.eclipse.jdt.core.prefs' || true; } | sed '/^$/d' \
	| while IFS= read -r p; do b=$(printf '%s' "$p" | sed 's#/\?\.settings/org\.eclipse\.jdt\.core\.prefs$##'); t=""
		grep -q 'nonExternalizedStringLiteral=error' "$W/$p" && t="nls"
		grep -q 'unusedImport=error' "$W/$p" && t="${t:+$t,}unusedImport"
		m=strict; grep -q 'suppressOptionalErrors=enabled' "$W/$p" && m=suppress
		printf '%s:%s\t%s\n' "$m" "$t" "$b"; done > "$O.groups"
: > "$O.log"; i=0
# sourcepath = every source folder of the fork (fork-added classes in other bundles resolve, so an import of them is
# judged "never used" or used, not "cannot be resolved")
git -C "$W" ls-files -- '*build.properties' | while IFS= read -r bp; do d=$(dirname "$bp"); [ "$d" = . ] && d=""
	awk 'BEGIN{c=0} /^[ \t]*source\.[^=]*=/ {c=1; sub(/^[^=]*=/,"")} c { line=$0; c=(line ~ /\\[ \t\r]*$/); gsub(/\\[ \t\r]*$/,"",line); gsub(/\r/,"",line); n=split(line, a, ","); for(i=1;i<=n;i++){ gsub(/^[ \t]+|[ \t]+$/,"",a[i]); if(a[i]!="") print a[i] } }' "$W/$bp" \
		| sed "s|^|$W/${d:+$d/}|; s|/$||"
done | sort -u | while read -r r; do if [ -d "$r" ]; then echo "$r"; fi; done > "$O.allroots"
cut -f1 "$O.groups" | sort -u | while IFS= read -r key; do i=$((i + 1)); m=${key%%:*}; t=${key#*:}
	awk -F'\t' -v k="$key" '$1 == k { print $2 }' "$O.groups" | while IFS= read -r b; do bp="$W/${b:+$b/}build.properties"
		if [ -f "$bp" ]; then
			awk 'BEGIN{c=0} /^[ \t]*source\.[^=]*=/ {c=1; sub(/^[^=]*=/,"")} c { line=$0; c=(line ~ /\\[ \t\r]*$/); gsub(/\\[ \t\r]*$/,"",line); gsub(/\r/,"",line); n=split(line, a, ","); for(i=1;i<=n;i++){ gsub(/^[ \t]+|[ \t]+$/,"",a[i]); if(a[i]!="") print a[i] } }' "$bp" \
				| sed "s|^|$W/${b:+$b/}|; s|/$||"
		fi
	done | sort -u | while read -r r; do if [ -d "$r" ]; then echo "$r"; fi; done > "$O.g$i.roots"
	[ -s "$O.g$i.roots" ] || continue
	while read -r r; do find "$r" -name '*.java' -type f; done < "$O.g$i.roots" | sed 's|^|"|; s|$|"|' > "$O.g$i.sources"
	{ echo "-cp"; echo "\"$(tr '\n' ';' < "$CP")\""; echo "-sourcepath"; echo "\"$(tr '\n' ';' < "$O.allroots")\""; } > "$O.g$i.args"
	if [ "$m" = suppress ]; then F="-warn:+${t//,/,+},+suppress"; else F="-err:+${t//,/,+}"; fi
	echo "group $i $key: $(wc -l < "$O.g$i.roots") roots, $(wc -l < "$O.g$i.sources") files, $F" >> "$O.groups.log"
	"$JAVA" -Xmx4g -jar "$ECJ" --release 21 -encoding UTF-8 -proc:none -d none -Xemacs -maxProblems 1000000 -enableJavadoc \
		-warn:none $F @"$O.g$i.args" @"$O.g$i.sources" > "$O.g$i.log" 2>&1 || true
	# keep the rows of this group's own files: units pulled in through the sourcepath belong to other groups' flags
	tr -d '"' < "$O.g$i.sources" | tr '\\' '/' | sort -u > "$O.g$i.keep"
	tr -d '\r' < "$O.g$i.log" | awk -v kf="$O.g$i.keep" 'BEGIN { while ((getline l < kf) > 0) keep[l] = 1 }
		match($0, /^[A-Za-z]:.*\.java:[0-9]+: /) { p = $0; sub(/\.java:[0-9]+: .*$/, ".java", p); gsub(/\\/, "/", p); if (!(p in keep)) next }
		{ print }' >> "$O.log"
done
{ grep -a -E '^[A-Za-z]:.*\.java:[0-9]+: (error|warning): (Non-externalized string literal|Unnecessary \$NON-NLS\$ tag|The import .* is never used)' "$O.log" || true; } \
	| tr -d '\r' | sed -E 's#^(.*\.java):([0-9]+): (error|warning): (.*)$#[ERROR] \1:[\2]\n[ERROR] \4#' \
	| awk '/^\[ERROR\] [A-Za-z]:/ { gsub(/\//, "\\") } { print }' > "$O.mvnlog"
echo "$(basename "$W") ecj pre-check: $(wc -l < "$O.groups") bundles in $(cut -f1 "$O.groups" | sort -u | wc -l) groups; $(grep -c 'Non-externalized' "$O.mvnlog" || true) missing NLS, $(grep -c 'Unnecessary' "$O.mvnlog" || true) surplus NLS, $(grep -c 'is never used' "$O.mvnlog" || true) unused imports"
