<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 original-source recipe receiver

Canonical transformation: `com.synexia.eclipse.EstateAstEclipsePlatformUi20261021`, frozen at
`hsoliwal/com.synexia@b77c892d7519c6aa037c2b5cebd8658867ea712b`.

This is an **opt-in source-custody Maven profile**. The default root POM,
original Java/Maven/Tycho compiler, existing public APIs, UI and native
contracts are unchanged. To run the original-source gate without downloading
Maven plugins: `python3 .m3/m3ast-20261021/verify.py`.

The profile can be invoked with
`mvn -B -ntp -Pm3-ast-source-audit-20261021 validate`,
subject to the original reactor/toolchain and Maven profile interactions.
This gate does **not** establish Java/JNI/HotSpot or Eclipse behavioral
equivalence. M3JDK21 remains one integrated JDK, not an external layer.
Existing JFace/JDT/SWT/Resources implementations retain original authority.
