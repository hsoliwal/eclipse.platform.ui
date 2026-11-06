<!--
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# DOOP key/value source receipt

Databinding Queue stores payloads in a lazy circular array, preserves FIFO/null/identity/empty behavior, and clears dequeued references. Allocation and copying finish before growth is published.

Canonical recipe and qualification: [Synexia commit cfa05713fe53](https://github.com/hsoliwal/com.synexia/commit/cfa05713fe5395694af36df74373dcfe1e7e9031).

Receiving parent: `55aac90dd550b6524135cc3b69bbe1c31f4b7b36`. Canonical datasets: `doop-key-value-eclipse-queue-20261102`. All Java files below are actual verified SDK Result outputs.

## Executed verification

110-source observable bundle compiled at Java 17; 3 unchanged tests plus 12 new tests passed onJava 21. All 108 exposed types retain public/protected ABI; 202 other classfiles are byte-identical. 857,916 independent observations matched. Steady churn allocated 0 versus 4,800,000 bytes over 200,000 operations. Real 32 MB wrapped-growth OOME recovery passed. Complete observable bundle matches the tested default revision.

## Exact output blobs

| Path | Git blob |
|---|---|
| `bundles/org.eclipse.core.databinding.observable/src/org/eclipse/core/internal/databinding/observable/Queue.java` | `93744e6af272d5c860f4d314a066cba6e51099f1` |
| `tests/org.eclipse.jface.tests.databinding/src/org/eclipse/core/tests/internal/databinding/QueueArrayStorageTest.java` | `511879fa035dc4f17f83669f95b69048a68d384c` |
| `tests/org.eclipse.jface.tests.databinding/src/org/eclipse/core/tests/internal/databinding/RealmQueueIntegrationTest.java` | `c9c08a0c30527aeec73320f9671c0b66389bbde5` |

## Acceptance scope

Full Tycho gate cannot resolve the offline eclipse-platform-parent:4.42.0-SNAPSHOT. Empty high-water array capacity is retained for reuse. Existing upstream commits remain on this branch.

The existing broader branch acceptance gates remain in force. This receipt does not claim whole-repository or fleet-wide conversion, universal throughput gains, or default-branch integration.
