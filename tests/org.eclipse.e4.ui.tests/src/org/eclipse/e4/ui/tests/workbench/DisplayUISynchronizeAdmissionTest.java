/*******************************************************************************
 * Copyright (c) 2026 Synexia contributors.
 *
 * This program and the accompanying materials are made available under
 * the Eclipse Public License 2.0.
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.e4.ui.tests.workbench;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.progress.UIJob;
import org.eclipse.e4.ui.workbench.swt.DisplayUISynchronize;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Display;
import org.junit.jupiter.api.Test;

/** Admission and exception boundaries of the existing SWT E4 UI synchronizer. */
public class DisplayUISynchronizeAdmissionTest {

	@Test
	public void missingOrDisposedDisplayRejectsWithoutPosting() {
		Runnable callback = () -> { };
		assertFalse(new DisplayUISynchronize(null).tryAsyncExec(callback));

		Display display = mock(Display.class);
		when(display.isDisposed()).thenReturn(true);
		assertFalse(new DisplayUISynchronize(display).tryAsyncExec(callback));
		verify(display, never()).asyncExec(any(Runnable.class));
	}

	@Test
	public void liveSubmissionAcknowledgesAndPreservesLegacyAsyncExec() {
		Display display = mock(Display.class);
		when(display.isDisposed()).thenReturn(false);
		DisplayUISynchronize synchronizer = new DisplayUISynchronize(display);
		Runnable callback = () -> { };
		assertTrue(synchronizer.tryAsyncExec(callback));
		synchronizer.asyncExec(callback);
		verify(display, times(2)).asyncExec(callback);
	}

	@Test
	public void disposedDuringAdmissionRefusesButLegacyPathStillThrows() {
		Display display = mock(Display.class);
		when(display.isDisposed()).thenReturn(false);
		SWTException disposed = new SWTException(SWT.ERROR_DEVICE_DISPOSED);
		Runnable callback = () -> { };
		doThrow(disposed).when(display).asyncExec(callback);
		DisplayUISynchronize synchronizer = new DisplayUISynchronize(display);
		assertFalse(synchronizer.tryAsyncExec(callback));
		assertSame(disposed, assertThrows(SWTException.class, () -> synchronizer.asyncExec(callback)));
	}

	@Test
	public void unrelatedSwtSubmissionFailurePropagatesExactly() {
		Display display = mock(Display.class);
		when(display.isDisposed()).thenReturn(false);
		SWTException other = new SWTException(SWT.ERROR_FAILED_EXEC);
		Runnable callback = () -> { };
		doThrow(other).when(display).asyncExec(callback);
		DisplayUISynchronize synchronizer = new DisplayUISynchronize(display);
		assertSame(other, assertThrows(SWTException.class, () -> synchronizer.tryAsyncExec(callback)));
	}

	/** The adapter and model snapshot are obtained before Job.run starts. */
	private static UIJob jobUsing(UISynchronize synchronizer, AtomicBoolean entered) {
		return new UIJob("admission-contract") { //$NON-NLS-1$
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				entered.set(true);
				return Status.OK_STATUS;
			}

			@Override
			protected UISynchronize getUiSynchronize() {
				return synchronizer;
			}
		};
	}

	@Test
	public void e4JobRefusalReturnsTerminalCancelWithoutInvokingUiBody() {
		Display display = mock(Display.class);
		when(display.isDisposed()).thenReturn(true);
		AtomicBoolean entered = new AtomicBoolean();
		UIJob job = jobUsing(new DisplayUISynchronize(display), entered);
		IStatus result = job.run(mock(IProgressMonitor.class));
		assertSame(Status.CANCEL_STATUS, result,
				"the Jobs manager must receive a terminal cancellation, not ASYNC_FINISH");
		assertFalse(entered.get());
		verify(display, never()).asyncExec(any(Runnable.class));
	}

	@Test
	public void e4JobAcceptedCallbackRemainsAsynchronous() {
		Display display = mock(Display.class);
		when(display.isDisposed()).thenReturn(false);
		AtomicReference<Runnable> accepted = new AtomicReference<>();
		doAnswer(invocation -> {
			accepted.set(invocation.getArgument(0));
			return null;
		}).when(display).asyncExec(any(Runnable.class));
		AtomicBoolean entered = new AtomicBoolean();
		UIJob job = jobUsing(new DisplayUISynchronize(display), entered);
		assertSame(Job.ASYNC_FINISH, job.run(mock(IProgressMonitor.class)));
		assertNotNull(accepted.get(), "the submission must be accepted by the Display");
		assertFalse(entered.get(), "the UI body may not run on the publishing CPU worker");
		verify(display).asyncExec(any(Runnable.class));
	}

	@Test
	public void e4JobDispositionRaceReturnsTerminalCancel() {
		Display display = mock(Display.class);
		when(display.isDisposed()).thenReturn(false);
		doThrow(new SWTException(SWT.ERROR_DEVICE_DISPOSED))
				.when(display).asyncExec(any(Runnable.class));
		AtomicBoolean entered = new AtomicBoolean();
		UIJob job = jobUsing(new DisplayUISynchronize(display), entered);
		assertSame(Status.CANCEL_STATUS, job.run(mock(IProgressMonitor.class)));
		assertFalse(entered.get());
	}

	@Test
	public void e4JobRetainsCustomSynchronizerDispatchContract() {
		UISynchronize custom = mock(UISynchronize.class);
		AtomicBoolean entered = new AtomicBoolean();
		UIJob job = jobUsing(custom, entered);
		assertSame(Job.ASYNC_FINISH, job.run(mock(IProgressMonitor.class)));
		verify(custom).asyncExec(any(Runnable.class));
		assertFalse(entered.get());
	}
}
