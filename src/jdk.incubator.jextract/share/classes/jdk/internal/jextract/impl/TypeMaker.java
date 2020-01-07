/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
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
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package jdk.internal.jextract.impl;


import jdk.incubator.jextract.Declaration;
import jdk.incubator.jextract.Type;
import jdk.incubator.jextract.Type.Primitive;
import jdk.incubator.jextract.Type.Delegated;

import java.util.ArrayList;
import java.util.List;

class TypeMaker {

    TreeMaker treeMaker;

    public TypeMaker(TreeMaker treeMaker) {
        this.treeMaker = treeMaker;
    }

    Type makeType(jdk.internal.clang.Type t) {
        switch(t.kind()) {
            case Auto:
                return makeType(t.canonicalType());
            case Void:
                return Type.void_();
            case Char_S:
            case Char_U:
                return Type.primitive(Primitive.Kind.Char, LayoutUtils.C_CHAR);
            case Short:
                return Type.primitive(Primitive.Kind.Short, LayoutUtils.C_SHORT);
            case Int:
                return Type.primitive(Primitive.Kind.Int, LayoutUtils.C_INT);
            case Long:
                return Type.primitive(Primitive.Kind.Long, LayoutUtils.C_LONG);
            case LongLong:
                return Type.primitive(Primitive.Kind.LongLong, LayoutUtils.C_LONGLONG);
            case SChar: {
                Type chType = Type.primitive(Primitive.Kind.Char, LayoutUtils.C_SCHAR);
                return Type.qualified(Delegated.Kind.SIGNED, chType);
            }
            case UShort: {
                Type chType = Type.primitive(Primitive.Kind.Short, LayoutUtils.C_USHORT);
                return Type.qualified(Delegated.Kind.UNSIGNED, chType);
            }
            case UInt: {
                Type chType = Type.primitive(Primitive.Kind.Int, LayoutUtils.C_UINT);
                return Type.qualified(Delegated.Kind.UNSIGNED, chType);
            }
            case ULong: {
                Type chType = Type.primitive(Primitive.Kind.Long, LayoutUtils.C_ULONG);
                return Type.qualified(Delegated.Kind.UNSIGNED, chType);
            }
            case ULongLong: {
                Type chType = Type.primitive(Primitive.Kind.LongLong, LayoutUtils.C_ULONGLONG);
                return Type.qualified(Delegated.Kind.UNSIGNED, chType);
            }
            case UChar: {
                Type chType = Type.primitive(Primitive.Kind.Char, LayoutUtils.C_UCHAR);
                return Type.qualified(Delegated.Kind.UNSIGNED, chType);
            }

            case Bool:
                return Type.primitive(Primitive.Kind.Bool, LayoutUtils.C_BOOL);
            case Double:
                return Type.primitive(Primitive.Kind.Double, LayoutUtils.C_DOUBLE);
            case LongDouble:
                return Type.primitive(Primitive.Kind.LongDouble, LayoutUtils.C_LONGDOUBLE);
            case Float:
                return Type.primitive(Primitive.Kind.Float, LayoutUtils.C_FLOAT);
            case Unexposed:
            case Elaborated:
                jdk.internal.clang.Type canonical = t.canonicalType();
                if (canonical.equalType(t)) {
                    throw new IllegalStateException("Unknown type with same canonical type: " + t.spelling());
                }
                return makeType(canonical);
            case ConstantArray: {
                Type elem = makeType(t.getElementType());
                return Type.array(t.getNumberOfElements(), elem);
            }
            case IncompleteArray: {
                Type elem = makeType(t.getElementType());
                return Type.array(elem);
            }
            case FunctionProto:
            case FunctionNoProto: {
                List<Type> args = new ArrayList<>();
                for (int i = 0; i < t.numberOfArgs(); i++) {
                    // argument could be function pointer declared locally
                    args.add(makeType(t.argType(i)));
                }
                return Type.function(t.isVariadic(), makeType(t.resultType()), args.toArray(new Type[0]));
            }
            case Enum:
            case Record: {
                return Type.declared((Declaration.Scoped)treeMaker.createTree(t.getDeclarationCursor()));
            }
            case BlockPointer:
            case Pointer: {
                jdk.internal.clang.Type __type = t.getPointeeType();
                return Type.pointer(() -> makeType(__type));
            }
            case Typedef: {
                Type __type = makeType(t.canonicalType());
                return Type.typedef(t.spelling(), __type);
            }
            case Complex: {
                Type __type = makeType(t.getElementType());
                return Type.qualified(Delegated.Kind.COMPLEX, __type);
            }
            case Vector: {
                Type __type = makeType(t.getElementType());
                return Type.vector(t.getNumberOfElements(), __type);
            }
            case WChar: //unsupported
            case Char16: //unsupported
            case Half: //unsupported
            case Int128: //unsupported
            case UInt128: //unsupported
            default:
                return TypeImpl.ERROR;
        }
    }
}
