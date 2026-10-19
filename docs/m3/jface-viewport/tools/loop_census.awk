# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Loop census of a Java file in JFace/Eclipse formatting (tab indented, members at one tab).
# Output TSV: file, method_line, method, loops, max_loop_nesting, per_iteration_lookups
#   per_iteration_lookups = calls inside a loop body that are themselves O(n) scans in JFace
#   (findItem, findItems, doFindItem, doIndexOf, indexOf, getItems, getChildren, doGetItems, arraycopy, contains).
function flush() {
	if (method != "" && loops > 0) printf "%s\t%d\t%s\t%d\t%d\t%d\n", FILENAME, mline, method, loops, maxdepth, inner
}
/^\t(public|protected|private|static|final|abstract|synchronized|\/\* *package *\*\/|void|[A-Z][A-Za-z<>\[\]]*|boolean|int|Object)[^;=]*\(/ && $0 !~ /^\t\t/ && $0 !~ /(^|[^A-Za-z])(new|return|if|for|while)[ (]/ {
	flush(); method = $0; sub(/^\t+/, "", method); sub(/\(.*/, "", method); n = split(method, w, " "); method = w[n]
	mline = FNR; loops = 0; maxdepth = 0; inner = 0; delete open; depth = 0
}
{
	indent = match($0, /[^\t]/) - 1
	# close loops whose indentation we left
	while (depth > 0 && indent <= open[depth] && $0 !~ /^\s*$/ && $0 ~ /^\t*\}/ && indent == open[depth]) { depth--; break }
}
/^\t+(for|while) *\(|^\t+do *\{/ {
	loops++; depth++; open[depth] = indent; if (depth > maxdepth) maxdepth = depth
	next
}
depth > 0 && /(findItems?|doFindItem|doIndexOf|indexOf|getItems|getChildren|doGetItems|arraycopy|contains(Key)?)\(/ { inner++ }
END { flush() }
