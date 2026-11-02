/*******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.internal.databinding;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.databinding.observable.Realm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

public class RealmQueueIntegrationTest {

	@Test
	@Timeout(30)
	public void realAsyncWorkerPreservesBacklogFifoAndWakesAfterDrain() throws Exception {
		LockRealm realm = new LockRealm();
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		try {
			realm.asyncExec(() -> {
				entered.countDown();
				try {
					release.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
			assertTrue(entered.await(10, TimeUnit.SECONDS));
			for (int round = 0; round < 4; round++) {
				List<Object> expected = new ArrayList<>();
				List<Object> observed = new ArrayList<>();
				CountDownLatch completed = new CountDownLatch(2048);
				AtomicBoolean allCurrent = new AtomicBoolean(true);
				for (int i = 0; i < 2048; i++) {
					Object payload = new Object();
					expected.add(payload);
					realm.asyncExec(() -> {
						if (!realm.isCurrent()) {
							allCurrent.set(false);
						}
						observed.add(payload);
						completed.countDown();
					});
				}
				release.countDown();
				assertTrue(completed.await(10, TimeUnit.SECONDS));
				assertTrue(allCurrent.get());
				for (int i = 0; i < expected.size(); i++) {
					assertSame(expected.get(i), observed.get(i));
				}
			}
		} finally {
			release.countDown();
			stopWorker(realm);
		}
	}

	@Test
	@Timeout(30)
	public void runningTaskCanEnqueueItsSuccessorThroughTheRealQueue() throws Exception {
		LockRealm realm = new LockRealm();
		CountDownLatch entered = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		CountDownLatch completed = new CountDownLatch(3);
		Object first = new Object();
		Object second = new Object();
		Object successor = new Object();
		List<Object> observed = new ArrayList<>();
		try {
			realm.asyncExec(() -> {
				entered.countDown();
				try {
					release.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				observed.add(first);
				realm.asyncExec(() -> {
					observed.add(successor);
					completed.countDown();
				});
				completed.countDown();
			});
			assertTrue(entered.await(10, TimeUnit.SECONDS));
			realm.asyncExec(() -> {
				observed.add(second);
				completed.countDown();
			});
			release.countDown();
			assertTrue(completed.await(10, TimeUnit.SECONDS));
			assertSame(first, observed.get(0));
			assertSame(second, observed.get(1));
			assertSame(successor, observed.get(2));
		} finally {
			release.countDown();
			stopWorker(realm);
		}
	}

	private static void stopWorker(Realm realm) throws Exception {
		Field field = Realm.class.getDeclaredField("workerThread");
		field.setAccessible(true);
		Thread worker = (Thread) field.get(realm);
		if (worker != null) {
			worker.interrupt();
			worker.join(10_000);
			assertFalse(worker.isAlive(), "Test must not leave a Realm worker running");
		}
	}

	private static class LockRealm extends Realm {
		private final Object lock = new Object();

		@Override
		public boolean isCurrent() {
			return Thread.holdsLock(lock);
		}

		@Override
		protected void syncExec(Runnable runnable) {
			synchronized (lock) {
				safeRun(runnable);
			}
		}
	}
}
