#!/usr/bin/env bash
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# BLLSE-driven repair of ECJ "The import X is never used" errors (bundles whose JDT prefs make unusedImport an error):
# the reported line is removed when it is exactly that single import declaration; anything else is refused (stderr).
# Usage: fix_unused_imports.sh <worktree> <maven-or-precheck-log>   Prints: import TAB path TAB line TAB name
set -euo pipefail
W=$(cd "$1" && pwd); LOG="$2"; export LC_ALL=C
WN=$(cygpath -w "$W"); L="$W/.imp-fix.tmp"
awk '/^\[ERROR\] [A-Za-z]:\\.*\.java:\[[0-9]+\]/ { s = $0; sub(/^\[ERROR\] /, "", s); sub(/[ \t\r]+$/, "", s)
		n = match(s, /:\[[0-9]+\]$/); f = substr(s, 1, n - 1); l = substr(s, n + 2); sub(/\]$/, "", l); next }
	/^\[ERROR\] The import .* is never used/ && f != "" { m = $0; sub(/^\[ERROR\] The import /, "", m); sub(/ is never used.*$/, "", m)
		print f "\t" l "\t" m; f = "" }' "$LOG" \
| while IFS=$'\t' read -r wf l m; do rel=${wf#"$WN"\\}; printf '%s\t%s\t%s\n' "${rel//\\//}" "$l" "$m"; done | sort -u > "$L"
cut -f1 "$L" | sort -u | while IFS= read -r rel; do
	p="$W/$rel"; [ -f "$p" ] || { echo "MISSING $rel" >&2; continue; }
	want=$(awk -F'\t' -v f="$rel" '$1 == f { printf "%s=%s|", $2, $3 }' "$L")
	awk -v BINMODE=3 -v want="$want" -v rel="$rel" '
		BEGIN { n = split(want, a, "|"); for (i = 1; i <= n; i++) if (a[i] != "") { split(a[i], b, "="); imp[b[1]] = b[2] } }
		(FNR in imp) { s = $0; sub(/\r$/, "", s); x = imp[FNR]; gsub(/\./, "\\.", x)
			if (s ~ ("^[ \t]*import[ \t]+(static[ \t]+)?" x "[ \t]*;[ \t]*$")) { print "import\t" rel "\t" FNR "\t" imp[FNR] > "/dev/stderr"; next }
			print "REFUSED\t" rel "\t" FNR "\t" imp[FNR] > "/dev/stderr" }
		{ printf "%s%s", $0, RT }' "$p" > "$p.imp.tmp" 2> "$W/.imp-rows.tmp"
	if cmp -s "$p" "$p.imp.tmp"; then rm -f "$p.imp.tmp"; else mv "$p.imp.tmp" "$p"; fi
	grep -v '^REFUSED' "$W/.imp-rows.tmp" || true; grep '^REFUSED' "$W/.imp-rows.tmp" >&2 || true
done
rm -f "$L" "$W/.imp-rows.tmp"
