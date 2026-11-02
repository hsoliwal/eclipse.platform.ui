# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Shared paths for the JFace viewport track. Source it: . docs/m3/jface-viewport/tools/jface_env.sh
# All build and test output goes to S:/m3-scratch/jface (C:, K:, D:, G: are never written).
export WT=${WT:-S:/m3-scratch/worktrees/platform-ui}
export OUT=${OUT:-S:/m3-scratch/jface}
export TEMP=$OUT/tmp TMP=$OUT/tmp
export JAVA_HOME="C:/Program Files/Java/jdk-21"
M2=$(cygpath -m "$HOME/.m2/repository")
P=$M2/org/eclipse/platform
# official stock references (Maven Central latest releases)
export STOCK_JFACE=$P/org.eclipse.jface/3.40.0/org.eclipse.jface-3.40.0.jar
export STOCK_SWT=$P/org.eclipse.swt.win32.win32.x86_64/3.135.0/org.eclipse.swt.win32.win32.x86_64-3.135.0.jar
# SWT under test (viewport integration jar when given, else stock)
export SWT_JAR=${SWT_JAR:-$STOCK_SWT}
export DEPS="$P/org.eclipse.core.commands/3.13.0/org.eclipse.core.commands-3.13.0.jar;$P/org.eclipse.equinox.common/3.21.0/org.eclipse.equinox.common-3.21.0.jar;$P/org.eclipse.equinox.bidi/1.5.400/org.eclipse.equinox.bidi-1.5.400.jar;$P/org.eclipse.equinox.preferences/3.12.100/org.eclipse.equinox.preferences-3.12.100.jar;$P/org.eclipse.osgi/3.24.300/org.eclipse.osgi-3.24.300.jar;$M2/org/osgi/org.osgi.service.prefs/1.1.2/org.osgi.service.prefs-1.1.2.jar"
export JUNIT=$M2/org/junit/platform/junit-platform-console-standalone/1.11.4/junit-platform-console-standalone-1.11.4.jar
export JACOCO_AGENT=$M2/org/jacoco/org.jacoco.agent/0.8.15/org.jacoco.agent-0.8.15-runtime.jar
export JACOCO_CLI=$M2/org/jacoco/org.jacoco.cli/0.8.15/org.jacoco.cli-0.8.15-nodeps.jar
mkdir -p "$OUT/tmp"
