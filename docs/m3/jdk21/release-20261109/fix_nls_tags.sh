#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# BLLSE-driven repair of the two ECJ NLS errors that M3 lanes left in bundles compiled with NLS problems as errors:
#   rm   "Unnecessary $NON-NLS$ tag": a regex-hoist rewrote `s.replaceAll("\\.", "\\\\.") //$NON-NLS-1$//$NON-NLS-2$`
#        into a hoisted-Pattern call with one literal and kept both tags -> tags numbered above the line's literal
#        count are removed
#   add  "Non-externalized string literal": a StringBuilder fold wrote `return "" + n;` (upstream: buffer.append(n))
#        without a tag -> the missing //$NON-NLS-i$ (i = 1..literal count) are appended to the line
# String literals are counted on the code view (char literals, comments and text after // masked). Every other byte
# stays; CR bytes kept. Fixed point.
# Usage: fix_nls_tags.sh <worktree> <maven-log>   (rows from the log)
#        fix_nls_tags.sh <worktree> --scan        (static: lane-anchored lines only, see below)
# Prints: nls-rm|nls-add TAB path TAB line
set -euo pipefail
W=$(cd "$1" && pwd); LOG="$2"; export LC_ALL=C
WN=$(cygpath -w "$W"); L="$W/.nls-fix.tmp"
if [ "$LOG" = "--scan" ]; then
	# rm candidates: lines a lane anchored (//FLLDCIM:) that carry a $NON-NLS-n$ tag
	{ git -C "$W" grep -n -F -e '$NON-NLS-' -- '*.java' || true; } | grep -F '//FLLDCIM:' | awk -F: '{ print $1 "\t" $2 "\trm" }' > "$L"
	# (no static add mode: whether a literal needs a tag depends on @SuppressWarnings("nls") scopes; adds come from
	# ECJ's own "Non-externalized string literal" rows only)
else
	awk '/^\[ERROR\] [A-Za-z]:\\.*\.java:\[[0-9]+\]/ { s = $0; sub(/^\[ERROR\] /, "", s); sub(/[ \t\r]+$/, "", s)
			n = match(s, /:\[[0-9]+\]$/); f = substr(s, 1, n - 1); l = substr(s, n + 2); sub(/\]$/, "", l); next }
		/^\[ERROR\] Unnecessary \$NON-NLS\$ tag/ && f != "" { print f "\t" l "\trm"; f = "" }
		/^\[ERROR\] Non-externalized string literal/ && f != "" { print f "\t" l "\tadd"; f = "" }' "$LOG" \
	| while IFS=$'\t' read -r wf l k; do rel=${wf#"$WN"\\}; printf '%s\t%s\t%s\n' "${rel//\\//}" "$l" "$k"; done > "$L"
fi
sort -u -o "$L" "$L"
cut -f1 "$L" | sort -u | while IFS= read -r rel; do
	p="$W/$rel"; [ -f "$p" ] || { echo "MISSING $rel" >&2; continue; }
	want=$(awk -F'\t' -v f="$rel" '$1 == f { printf "%s:%s,", $2, $3 }' "$L")
	awk -v BINMODE=3 -v want="$want" -v rel="$rel" '
		BEGIN { n = split(want, a, ","); for (i = 1; i <= n; i++) if (a[i] != "") { split(a[i], b, ":"); fix[b[1]] = fix[b[1]] " " b[2] } }
		function literals(s,   m, i, c, c2, q, d, k, e) { m = length(s); i = 1; k = 0
			while (i <= m) { c = substr(s, i, 1); c2 = substr(s, i, 2)
				if (c2 == "//") break
				if (c2 == "/*") { e = index(substr(s, i + 2), "*/"); if (e == 0) break; i += e + 3; continue }  # inline block comment
				if (c == "\"" || c == "\x27") { q = c; if (q == "\"") k++; i++
					while (i <= m) { d = substr(s, i, 1); if (d == "\\") { i += 2; continue }; i++; if (d == q) break }
					continue }
				i++ }
			return k }
		{ line = $0
		  if (FNR in fix) {
			k = literals(line); out = line
			if (fix[FNR] ~ / rm/) for (t = 9; t > k; t--) gsub("//\\$NON-NLS-" t "\\$", "", out)
			if (fix[FNR] ~ / add/ && k > 0) { cr = ""; if (out ~ /\r$/) { cr = "\r"; sub(/\r$/, "", out) }
				add = ""; for (t = 1; t <= k; t++) if (index(out, "//$NON-NLS-" t "$") == 0) add = add "//$NON-NLS-" t "$"
				if (add != "") { sub(/[ \t]+$/, "", out); out = out " " add }; out = out cr }
			if (out != line) { kind = (length(out) < length(line)) ? "nls-rm" : "nls-add"; print kind "\t" rel "\t" FNR > "/dev/stderr" }
			line = out }
		  printf "%s%s", line, RT }' "$p" > "$p.nls.tmp" 2> "$W/.nls-rows.tmp"
	if cmp -s "$p" "$p.nls.tmp"; then rm -f "$p.nls.tmp"; else mv "$p.nls.tmp" "$p"; cat "$W/.nls-rows.tmp"; fi
done
rm -f "$L" "$W/.nls-rows.tmp"
