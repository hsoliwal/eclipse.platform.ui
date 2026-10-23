/*******************************************************************************
 * Copyright (c) 2026 Hitesh Soliwal and contributors.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0, which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.jface.tests.viewers.m3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.m3.M3ViewerDelta;
import org.eclipse.jface.viewers.m3.M3Viewers;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Original SWT/JFace selection owners after the source-sealed M3 atom promotion.
 * This is intentionally separate from the already qualified queue test class:
 * it adds no dependency on private queue implementation fields or timer races.
 */
public class M3SelectionReceiverTest {
    private static final class Row {
        String label;
        Row(String label) { this.label = label; }
    }

    private Shell shell;

    @BeforeEach
    public void setUp() {
        shell = new Shell(Display.getDefault());
        shell.setLayout(new FillLayout());
        shell.setSize(320, 180);
        shell.open();
    }

    @AfterEach
    public void tearDown() {
        if (!shell.isDisposed()) {
            shell.dispose();
        }
    }

    private static <V extends StructuredViewer> V initialize(V viewer, List<Row> rows) {
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                return ((Row) element).label;
            }
        });
        viewer.setInput(rows);
        return viewer;
    }

    @Test
    public void explicitPreservationKeepsTheSamePhysicalRowAfterRemoveAndReinsert() {
        Row kept = new Row("kept");
        Row other = new Row("other");
        List<Row> rows = new ArrayList<>(List.of(kept, other));
        TableViewer viewer = initialize(new TableViewer(shell, SWT.MULTI), rows);
        viewer.setSelection(new StructuredSelection(kept));
        rows.remove(kept);
        rows.add(kept);
        M3Viewers.applyPreservingSelection(viewer,
                new M3ViewerDelta().remove(kept).add(rows, kept, -1));
        assertSame(kept, viewer.getStructuredSelection().getFirstElement());
        assertEquals(2, viewer.getTable().getItemCount());
    }

    @Test
    public void comboUpdatePreservesOwnerSelectionAcrossLabelMutation() {
        Row selected = new Row("before");
        Row other = new Row("other");
        List<Row> rows = new ArrayList<>(List.of(selected, other));
        ComboViewer viewer = initialize(new ComboViewer(shell, SWT.READ_ONLY), rows);
        viewer.setSelection(new StructuredSelection(selected));
        selected.label = "after";
        M3Viewers.apply(viewer, new M3ViewerDelta().update(selected, null));
        assertSame(selected, viewer.getStructuredSelection().getFirstElement());
        assertEquals("after", viewer.getCombo().getItem(0));
    }

    @Test
    public void ccomboEmptyExplicitSelectionClearsNativeIndex() {
        Row selected = new Row("first");
        List<Row> rows = new ArrayList<>(List.of(selected, new Row("second")));
        ComboViewer viewer = initialize(new ComboViewer(new CCombo(shell, SWT.READ_ONLY)), rows);
        viewer.setSelection(new StructuredSelection(selected));
        assertEquals(0, viewer.getCCombo().getSelectionIndex());
        viewer.setSelection(StructuredSelection.EMPTY);
        assertEquals(-1, viewer.getCCombo().getSelectionIndex());
        assertTrue(viewer.getStructuredSelection().isEmpty());
    }

    @Test
    public void callbackSelectionDuringComboUpdateOutranksPriorSelection() {
        Row previous = new Row("old");
        Row requested = new Row("new");
        List<Row> rows = new ArrayList<>(List.of(previous, requested));
        ComboViewer viewer = new ComboViewer(shell, SWT.READ_ONLY);
        AtomicBoolean reselect = new AtomicBoolean();
        viewer.setContentProvider(ArrayContentProvider.getInstance());
        viewer.setLabelProvider(new LabelProvider() {
            @Override
            public String getText(Object element) {
                if (element == previous && reselect.compareAndSet(true, false)) {
                    viewer.setSelection(new StructuredSelection(requested));
                }
                return ((Row) element).label;
            }
        });
        viewer.setInput(rows);
        viewer.setSelection(new StructuredSelection(previous));
        previous.label = "changed";
        reselect.set(true);
        M3Viewers.apply(viewer, new M3ViewerDelta().update(previous, null));
        assertSame(requested, viewer.getStructuredSelection().getFirstElement());
    }
}
