/*
 *  Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *  Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 */

package jdk.internal.foreign.abi;

import jdk.internal.access.JavaLangInvokeAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.foreign.Util;
import jdk.internal.foreign.memory.BoundedPointer;
import jdk.internal.vm.annotation.Stable;

import java.foreign.Library;
import java.foreign.NativeMethodType;
import java.foreign.memory.Callback;
import java.foreign.memory.LayoutType;
import java.foreign.memory.Pointer;
import java.foreign.memory.Struct;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.stream.IntStream;

import static java.lang.invoke.MethodHandles.*;
import static java.lang.invoke.MethodType.*;

/**
 * This class implements upcall invocation from native code through a set of specialized entry points. A specialized
 * entry point is a Java method which takes a number N of long arguments followed by a number M of double arguments;
 * possible return types for the entry point are either long, double or void.
 */
public class LinkToNativeUpcallHandler implements Library.Symbol {
    private static final MethodHandle MH_Pointer_set;

    static {
        Lookup lookup = lookup();
        try {
            MH_Pointer_set = lookup.findVirtual(Pointer.class, "set", methodType(void.class, Object.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static final JavaLangInvokeAccess JLI = SharedSecrets.getJavaLangInvokeAccess();
    @Stable
    private final MethodHandle mh;
    private final Pointer<?> entryPoint;

    public LinkToNativeUpcallHandler(MethodHandle target, CallingSequence callingSequence, NativeMethodType nmt) {
        try {
            Util.checkNoArrays(target.type());
            LinkToNativeSignatureShuffler shuffler =
                    LinkToNativeSignatureShuffler.nativeToJavaShuffler(callingSequence, nmt);

            if (callingSequence.returnsInMemory()) {
                // e.g.
                // target = (int,int)MyStruct
                // native sig = (Pointer<MyStruct>, int, int)Pointer<MyStruct>

                target = collectArguments(MH_Pointer_set, 1, target.asType(target.type().changeReturnType(Object.class))); // erase return type
                int[] reorder = IntStream.range(-1, target.type().parameterCount()).toArray();
                reorder[0] = 0;
                target = collectArguments(identity(Pointer.class), 1, target); // need to return pointer as well for Windows
                target = permuteArguments(target, target.type().dropParameterTypes(0, 1), reorder);
            }

            this.mh = shuffler.adapt(target);
            JLI.ensureCustomized(this.mh); // FIXME: consider more flexible scheme to customize upcall entry points
            long addr = allocateUpcallStub(mh);
            this.entryPoint = BoundedPointer.createNativeVoidPointer(addr);
        } catch (Throwable ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public Pointer<?> getAddress() {
        return entryPoint;
    }

    // natives

    native long allocateUpcallStub(MethodHandle handler);

    private static native void registerNatives();
    static {
        registerNatives();
    }
}
