# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Two differential logs (stock, m3) step by step, modulo the time-bounded expansion of the e4 FilteredTree: after a
# search text it expands categories until SOFT_MAX_EXPAND_TIME (200 ms) runs out, so at 10^6 markers the slower side
# expands fewer categories than the faster one with the same model. A step whose only differences are a category's
# expansion flag (" +" / " -") and the rows listed under a category that is collapsed on one side is EXPANSION_ONLY;
# any other difference stays DIFFERENT. Usage: awk -f diff_modulo_expansion.awk <stock-log> <m3-log>
BEGIN { OFS = "\t" }
NR == FNR { stock[FNR] = $0; n = FNR; next }
{
	m3[FNR] = $0; m = FNR
}
END {
	total = (n > m) ? n : m
	for (i = 1; i <= total; i++) {
		s = stock[i]; t = m3[i]
		if (s == t) { identical++; continue }
		ns = split(s, a, / \|\| /); nt = split(t, b, / \|\| /)
		verdict = "EXPANSION_ONLY"
		if (ns != nt) verdict = "DIFFERENT"
		for (k = 1; k <= ns && verdict == "EXPANSION_ONLY"; k++) {
			if (a[k] == b[k]) continue
			# a category segment: "<name> (<x> of <y> items) ~ ~ ~ ~ <+|->[ | row ...]"
			if (match(a[k], / ~  ~  ~  ~  [+-]/) == 0 || match(b[k], / ~  ~  ~  ~  [+-]/) == 0) { verdict = "DIFFERENT"; continue }
			ha = substr(a[k], 1, index(a[k], " ~  ~  ~  ~  ") - 1); hb = substr(b[k], 1, index(b[k], " ~  ~  ~  ~  ") - 1)
			if (ha != hb) { verdict = "DIFFERENT"; continue }
			fa = substr(a[k], index(a[k], " ~  ~  ~  ~  ") + 13, 1); fb = substr(b[k], index(b[k], " ~  ~  ~  ~  ") + 13, 1)
			if (fa == fb) { verdict = "DIFFERENT"; continue }   # same expansion, different rows: a real difference
			# the rows of the expanded side are not compared: the collapsed side lists none
		}
		step = s; sub(/ \| .*/, "", step)
		if (verdict == "EXPANSION_ONLY") expansion++; else different++
		print i, verdict, step
	}
	printf "steps %d: identical %d, expansion-only %d, different %d\n", total, identical, expansion + 0, different + 0
	printf "verdict modulo expansion: %s\n", (different + 0 == 0 ? "IDENTICAL" : "DIFFERENT")
}
