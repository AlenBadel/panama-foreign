/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

import com.oracle.java.testlibrary.cli.predicate.AndPredicate;
import com.oracle.java.testlibrary.cli.predicate.NotPredicate;
import rtm.predicate.SupportedCPU;
import rtm.predicate.SupportedVM;

/**
 * @test
 * @bug 8031320
 * @summary Verify RTMTotalCountIncrRate option processing on CPU without
 *          rtm support and/or on VM without rtm locking support.
 * @library /testlibrary /../../test/lib /compiler/testlibrary
 * @modules java.base/sun.misc
 *          java.management
 * @build TestRTMTotalCountIncrRateOptionOnUnsupportedConfig
 * @run main ClassFileInstaller sun.hotspot.WhiteBox
 *                              sun.hotspot.WhiteBox$WhiteBoxPermission
 * @run main/othervm -Xbootclasspath/a:. -XX:+UnlockDiagnosticVMOptions
 *                   -XX:+WhiteBoxAPI
 *                   TestRTMTotalCountIncrRateOptionOnUnsupportedConfig
 */

public class TestRTMTotalCountIncrRateOptionOnUnsupportedConfig
        extends RTMGenericCommandLineOptionTest {
    private static final String DEFAULT_VALUE = "64";

    private TestRTMTotalCountIncrRateOptionOnUnsupportedConfig() {
        super(new NotPredicate(new AndPredicate(new SupportedCPU(),
                        new SupportedVM())),
                "RTMTotalCountIncrRate", false, true,
                TestRTMTotalCountIncrRateOptionOnUnsupportedConfig
                        .DEFAULT_VALUE,
                "-1", "0", "42", "128");
    }

    public static void main(String args[]) throws Throwable {
        new TestRTMTotalCountIncrRateOptionOnUnsupportedConfig().test();
    }
}
