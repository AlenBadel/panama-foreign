/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * or visit www.oracle.com if you need additional information or have
 * questions.
 */
package jdk.incubator.vector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.ReadOnlyBufferException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;
import static jdk.incubator.vector.VectorIntrinsics.*;

@SuppressWarnings("cast")
final class ShortMaxVector extends ShortVector {
    static final ShortMaxSpecies SPECIES = new ShortMaxSpecies();

    static final ShortMaxVector ZERO = new ShortMaxVector();

    static final int LENGTH = SPECIES.length();

    private final short[] vec; // Don't access directly, use getElements() instead.

    private short[] getElements() {
        return VectorIntrinsics.maybeRebox(this).vec;
    }

    ShortMaxVector() {
        vec = new short[SPECIES.length()];
    }

    ShortMaxVector(short[] v) {
        vec = v;
    }

    @Override
    public int length() { return LENGTH; }

    // Unary operator

    @Override
    ShortMaxVector uOp(FUnOp f) {
        short[] vec = getElements();
        short[] res = new short[length()];
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec[i]);
        }
        return new ShortMaxVector(res);
    }

    @Override
    ShortMaxVector uOp(Mask<Short> o, FUnOp f) {
        short[] vec = getElements();
        short[] res = new short[length()];
        boolean[] mbits = ((ShortMaxMask)o).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec[i]) : vec[i];
        }
        return new ShortMaxVector(res);
    }

    // Binary operator

    @Override
    ShortMaxVector bOp(Vector<Short> o, FBinOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.getElements();
        short[] vec2 = ((ShortMaxVector)o).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new ShortMaxVector(res);
    }

    @Override
    ShortMaxVector bOp(Vector<Short> o1, Mask<Short> o2, FBinOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.getElements();
        short[] vec2 = ((ShortMaxVector)o1).getElements();
        boolean[] mbits = ((ShortMaxMask)o2).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i]) : vec1[i];
        }
        return new ShortMaxVector(res);
    }

    // Trinary operator

    @Override
    ShortMaxVector tOp(Vector<Short> o1, Vector<Short> o2, FTriOp f) {
        short[] res = new short[length()];
        short[] vec1 = this.getElements();
        short[] vec2 = ((ShortMaxVector)o1).getElements();
        short[] vec3 = ((ShortMaxVector)o2).getElements();
        for (int i = 0; i < length(); i++) {
            res[i] = f.apply(i, vec1[i], vec2[i], vec3[i]);
        }
        return new ShortMaxVector(res);
    }

    @Override
    ShortMaxVector tOp(Vector<Short> o1, Vector<Short> o2, Mask<Short> o3, FTriOp f) {
        short[] res = new short[length()];
        short[] vec1 = getElements();
        short[] vec2 = ((ShortMaxVector)o1).getElements();
        short[] vec3 = ((ShortMaxVector)o2).getElements();
        boolean[] mbits = ((ShortMaxMask)o3).getBits();
        for (int i = 0; i < length(); i++) {
            res[i] = mbits[i] ? f.apply(i, vec1[i], vec2[i], vec3[i]) : vec1[i];
        }
        return new ShortMaxVector(res);
    }

    @Override
    short rOp(short v, FBinOp f) {
        short[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            v = f.apply(i, v, vec[i]);
        }
        return v;
    }

    @Override
    @ForceInline
    public <F> Vector<F> cast(Species<F> s) {
        Objects.requireNonNull(s);
        if (s.length() != LENGTH)
            throw new IllegalArgumentException("Vector length this species length differ");

        return VectorIntrinsics.cast(
            ShortMaxVector.class,
            short.class, LENGTH,
            s.vectorType(),
            s.elementType(), LENGTH,
            this, s,
            (species, vector) -> vector.castDefault(species)
        );
    }

    @SuppressWarnings("unchecked")
    @ForceInline
    private <F> Vector<F> castDefault(Species<F> s) {
        int limit = s.length();

        Class<?> stype = s.elementType();
        if (stype == byte.class) {
            byte[] a = new byte[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (byte) this.get(i);
            }
            return (Vector) ByteVector.fromArray((ByteVector.ByteSpecies) s, a, 0);
        } else if (stype == short.class) {
            short[] a = new short[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (short) this.get(i);
            }
            return (Vector) ShortVector.fromArray((ShortVector.ShortSpecies) s, a, 0);
        } else if (stype == int.class) {
            int[] a = new int[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (int) this.get(i);
            }
            return (Vector) IntVector.fromArray((IntVector.IntSpecies) s, a, 0);
        } else if (stype == long.class) {
            long[] a = new long[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (long) this.get(i);
            }
            return (Vector) LongVector.fromArray((LongVector.LongSpecies) s, a, 0);
        } else if (stype == float.class) {
            float[] a = new float[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (float) this.get(i);
            }
            return (Vector) FloatVector.fromArray((FloatVector.FloatSpecies) s, a, 0);
        } else if (stype == double.class) {
            double[] a = new double[limit];
            for (int i = 0; i < limit; i++) {
                a[i] = (double) this.get(i);
            }
            return (Vector) DoubleVector.fromArray((DoubleVector.DoubleSpecies) s, a, 0);
        } else {
            throw new UnsupportedOperationException("Bad lane type for casting.");
        }
    }

    @Override
    @ForceInline
    @SuppressWarnings("unchecked")
    public <F> Vector<F> reinterpret(Species<F> s) {
        Objects.requireNonNull(s);

        if(s.elementType().equals(short.class)) {
            return (Vector<F>) reshape((Species<Short>)s);
        }
        if(s.bitSize() == bitSize()) {
            return reinterpretType(s);
        }

        return defaultReinterpret(s);
    }

    @ForceInline
    private <F> Vector<F> reinterpretType(Species<F> s) {
        Objects.requireNonNull(s);

        Class<?> stype = s.elementType();
        if (stype == byte.class) {
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                ByteMaxVector.class,
                byte.class, ByteMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == short.class) {
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                ShortMaxVector.class,
                short.class, ShortMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == int.class) {
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                IntMaxVector.class,
                int.class, IntMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == long.class) {
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                LongMaxVector.class,
                long.class, LongMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == float.class) {
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                FloatMaxVector.class,
                float.class, FloatMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else if (stype == double.class) {
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                DoubleMaxVector.class,
                double.class, DoubleMaxVector.LENGTH,
                this, s,
                (species, vector) -> vector.defaultReinterpret(species)
            );
        } else {
            throw new UnsupportedOperationException("Bad lane type for casting.");
        }
    }

    @Override
    @ForceInline
    public ShortVector reshape(Species<Short> s) {
        Objects.requireNonNull(s);
        if (s.bitSize() == 64 && (s instanceof Short64Vector.Short64Species)) {
            Short64Vector.Short64Species ts = (Short64Vector.Short64Species)s;
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                Short64Vector.class,
                short.class, Short64Vector.LENGTH,
                this, ts,
                (species, vector) -> (ShortVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 128 && (s instanceof Short128Vector.Short128Species)) {
            Short128Vector.Short128Species ts = (Short128Vector.Short128Species)s;
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                Short128Vector.class,
                short.class, Short128Vector.LENGTH,
                this, ts,
                (species, vector) -> (ShortVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 256 && (s instanceof Short256Vector.Short256Species)) {
            Short256Vector.Short256Species ts = (Short256Vector.Short256Species)s;
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                Short256Vector.class,
                short.class, Short256Vector.LENGTH,
                this, ts,
                (species, vector) -> (ShortVector) vector.defaultReinterpret(species)
            );
        } else if (s.bitSize() == 512 && (s instanceof Short512Vector.Short512Species)) {
            Short512Vector.Short512Species ts = (Short512Vector.Short512Species)s;
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                Short512Vector.class,
                short.class, Short512Vector.LENGTH,
                this, ts,
                (species, vector) -> (ShortVector) vector.defaultReinterpret(species)
            );
        } else if ((s.bitSize() > 0) && (s.bitSize() <= 2048)
                && (s.bitSize() % 128 == 0) && (s instanceof ShortMaxVector.ShortMaxSpecies)) {
            ShortMaxVector.ShortMaxSpecies ts = (ShortMaxVector.ShortMaxSpecies)s;
            return VectorIntrinsics.reinterpret(
                ShortMaxVector.class,
                short.class, LENGTH,
                ShortMaxVector.class,
                short.class, ShortMaxVector.LENGTH,
                this, ts,
                (species, vector) -> (ShortVector) vector.defaultReinterpret(species)
            );
        } else {
            throw new InternalError("Unimplemented size");
        }
    }

    // Binary operations with scalars

    @Override
    @ForceInline
    public ShortVector add(short o) {
        return add(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ShortVector add(short o, Mask<Short> m) {
        return add(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ShortVector sub(short o) {
        return sub(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ShortVector sub(short o, Mask<Short> m) {
        return sub(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ShortVector mul(short o) {
        return mul(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ShortVector mul(short o, Mask<Short> m) {
        return mul(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ShortVector min(short o) {
        return min(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ShortVector max(short o) {
        return max(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Short> equal(short o) {
        return equal(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Short> notEqual(short o) {
        return notEqual(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Short> lessThan(short o) {
        return lessThan(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Short> lessThanEq(short o) {
        return lessThanEq(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Short> greaterThan(short o) {
        return greaterThan(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public Mask<Short> greaterThanEq(short o) {
        return greaterThanEq(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ShortVector blend(short o, Mask<Short> m) {
        return blend(SPECIES.broadcast(o), m);
    }


    @Override
    @ForceInline
    public ShortVector and(short o) {
        return and(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ShortVector and(short o, Mask<Short> m) {
        return and(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ShortVector or(short o) {
        return or(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ShortVector or(short o, Mask<Short> m) {
        return or(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ShortVector xor(short o) {
        return xor(SPECIES.broadcast(o));
    }

    @Override
    @ForceInline
    public ShortVector xor(short o, Mask<Short> m) {
        return xor(SPECIES.broadcast(o), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector neg() {
        return (ShortMaxVector)zero(SPECIES).sub(this);
    }

    // Unary operations

    @ForceInline
    @Override
    public ShortMaxVector neg(Mask<Short> m) {
        return blend(neg(), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector abs() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_ABS, ShortMaxVector.class, short.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (short) Math.abs(a)));
    }

    @ForceInline
    @Override
    public ShortMaxVector abs(Mask<Short> m) {
        return blend(abs(), m);
    }


    @Override
    @ForceInline
    public ShortMaxVector not() {
        return VectorIntrinsics.unaryOp(
            VECTOR_OP_NOT, ShortMaxVector.class, short.class, LENGTH,
            this,
            v1 -> v1.uOp((i, a) -> (short) ~a));
    }

    @ForceInline
    @Override
    public ShortMaxVector not(Mask<Short> m) {
        return blend(not(), m);
    }
    // Binary operations

    @Override
    @ForceInline
    public ShortMaxVector add(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_ADD, ShortMaxVector.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (short)(a + b)));
    }

    @Override
    @ForceInline
    public ShortMaxVector add(Vector<Short> v, Mask<Short> m) {
        return blend(add(v), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector sub(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_SUB, ShortMaxVector.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (short)(a - b)));
    }

    @Override
    @ForceInline
    public ShortMaxVector sub(Vector<Short> v, Mask<Short> m) {
        return blend(sub(v), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector mul(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MUL, ShortMaxVector.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (short)(a * b)));
    }

    @Override
    @ForceInline
    public ShortMaxVector mul(Vector<Short> v, Mask<Short> m) {
        return blend(mul(v), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector min(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;
        return (ShortMaxVector) VectorIntrinsics.binaryOp(
            VECTOR_OP_MIN, ShortMaxVector.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (short) Math.min(a, b)));
    }

    @Override
    @ForceInline
    public ShortMaxVector min(Vector<Short> v, Mask<Short> m) {
        return blend(min(v), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector max(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_MAX, ShortMaxVector.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (short) Math.max(a, b)));
        }

    @Override
    @ForceInline
    public ShortMaxVector max(Vector<Short> v, Mask<Short> m) {
        return blend(max(v), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector and(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_AND, ShortMaxVector.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (short)(a & b)));
    }

    @Override
    @ForceInline
    public ShortMaxVector or(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_OR, ShortMaxVector.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (short)(a | b)));
    }

    @Override
    @ForceInline
    public ShortMaxVector xor(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;
        return VectorIntrinsics.binaryOp(
            VECTOR_OP_XOR, ShortMaxVector.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bOp(v2, (i, a, b) -> (short)(a ^ b)));
    }

    @Override
    @ForceInline
    public ShortMaxVector and(Vector<Short> v, Mask<Short> m) {
        return blend(and(v), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector or(Vector<Short> v, Mask<Short> m) {
        return blend(or(v), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector xor(Vector<Short> v, Mask<Short> m) {
        return blend(xor(v), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector shiftL(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_LSHIFT, ShortMaxVector.class, short.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (short) (a << (i & 15))));
    }

    @Override
    @ForceInline
    public ShortMaxVector shiftL(int s, Mask<Short> m) {
        return blend(shiftL(s), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector shiftR(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_URSHIFT, ShortMaxVector.class, short.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (short) ((a & 0xFFFF) >>> (i & 15))));
    }

    @Override
    @ForceInline
    public ShortMaxVector shiftR(int s, Mask<Short> m) {
        return blend(shiftR(s), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector aShiftR(int s) {
        return VectorIntrinsics.broadcastInt(
            VECTOR_OP_RSHIFT, ShortMaxVector.class, short.class, LENGTH,
            this, s,
            (v, i) -> v.uOp((__, a) -> (short) (a >> (i & 15))));
    }

    @Override
    @ForceInline
    public ShortMaxVector aShiftR(int s, Mask<Short> m) {
        return blend(aShiftR(s), m);
    }
    // Ternary operations


    // Type specific horizontal reductions

    @Override
    @ForceInline
    public short addAll() {
        return (short) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_ADD, ShortMaxVector.class, short.class, LENGTH,
            this,
            v -> (long) v.rOp((short) 0, (i, a, b) -> (short) (a + b)));
    }

    @Override
    @ForceInline
    public short andAll() {
        return (short) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_AND, ShortMaxVector.class, short.class, LENGTH,
            this,
            v -> (long) v.rOp((short) -1, (i, a, b) -> (short) (a & b)));
    }

    @Override
    @ForceInline
    public short andAll(Mask<Short> m) {
        return blend(SPECIES.broadcast((short) -1), m).andAll();
    }

    @Override
    @ForceInline
    public short minAll() {
        return (short) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MIN, ShortMaxVector.class, short.class, LENGTH,
            this,
            v -> (long) v.rOp(Short.MAX_VALUE , (i, a, b) -> (short) Math.min(a, b)));
    }

    @Override
    @ForceInline
    public short maxAll() {
        return (short) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MAX, ShortMaxVector.class, short.class, LENGTH,
            this,
            v -> (long) v.rOp(Short.MIN_VALUE , (i, a, b) -> (short) Math.max(a, b)));
    }

    @Override
    @ForceInline
    public short mulAll() {
        return (short) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_MUL, ShortMaxVector.class, short.class, LENGTH,
            this,
            v -> (long) v.rOp((short) 1, (i, a, b) -> (short) (a * b)));
    }

    @Override
    @ForceInline
    public short subAll() {
        return (short) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_SUB, ShortMaxVector.class, short.class, LENGTH,
            this,
            v -> (long) v.rOp((short) 0, (i, a, b) -> (short) (a - b)));
    }

    @Override
    @ForceInline
    public short orAll() {
        return (short) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_OR, ShortMaxVector.class, short.class, LENGTH,
            this,
            v -> (long) v.rOp((short) 0, (i, a, b) -> (short) (a | b)));
    }

    @Override
    @ForceInline
    public short orAll(Mask<Short> m) {
        return blend(SPECIES.broadcast((short) 0), m).orAll();
    }

    @Override
    @ForceInline
    public short xorAll() {
        return (short) VectorIntrinsics.reductionCoerced(
            VECTOR_OP_XOR, ShortMaxVector.class, short.class, LENGTH,
            this,
            v -> (long) v.rOp((short) 0, (i, a, b) -> (short) (a ^ b)));
    }

    @Override
    @ForceInline
    public short xorAll(Mask<Short> m) {
        return blend(SPECIES.broadcast((short) 0), m).xorAll();
    }


    @Override
    @ForceInline
    public short addAll(Mask<Short> m) {
        return blend(SPECIES.broadcast((short) 0), m).addAll();
    }

    @Override
    @ForceInline
    public short subAll(Mask<Short> m) {
        return blend(SPECIES.broadcast((short) 0), m).subAll();
    }

    @Override
    @ForceInline
    public short mulAll(Mask<Short> m) {
        return blend(SPECIES.broadcast((short) 1), m).mulAll();
    }

    @Override
    @ForceInline
    public short minAll(Mask<Short> m) {
        return blend(SPECIES.broadcast(Short.MAX_VALUE), m).minAll();
    }

    @Override
    @ForceInline
    public short maxAll(Mask<Short> m) {
        return blend(SPECIES.broadcast(Short.MIN_VALUE), m).maxAll();
    }

    @Override
    @ForceInline
    public Shuffle<Short> toShuffle() {
        short[] a = toArray();
        int[] sa = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            sa[i] = (int) a[i];
        }
        return ShortVector.shuffleFromArray(SPECIES, sa, 0);
    }

    // Memory operations

    private static final int ARRAY_SHIFT         = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_SHORT_INDEX_SCALE);
    private static final int BOOLEAN_ARRAY_SHIFT = 31 - Integer.numberOfLeadingZeros(Unsafe.ARRAY_BOOLEAN_INDEX_SCALE);

    @Override
    @ForceInline
    public void intoArray(short[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, LENGTH);
        VectorIntrinsics.store(ShortMaxVector.class, short.class, LENGTH,
                               a, (((long) ix) << ARRAY_SHIFT) + Unsafe.ARRAY_SHORT_BASE_OFFSET,
                               this,
                               a, ix,
                               (arr, idx, v) -> v.forEach((i, e) -> arr[idx + i] = e));
    }

    @Override
    @ForceInline
    public final void intoArray(short[] a, int ax, Mask<Short> m) {
        ShortVector oldVal = ShortVector.fromArray(SPECIES, a, ax);
        ShortVector newVal = oldVal.blend(this, m);
        newVal.intoArray(a, ax);
    }

    @Override
    @ForceInline
    public void intoByteArray(byte[] a, int ix) {
        Objects.requireNonNull(a);
        ix = VectorIntrinsics.checkIndex(ix, a.length, bitSize() / Byte.SIZE);
        VectorIntrinsics.store(ShortMaxVector.class, short.class, LENGTH,
                               a, ((long) ix) + Unsafe.ARRAY_BYTE_BASE_OFFSET,
                               this,
                               a, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = ByteBuffer.wrap(c, idx, c.length - idx).order(ByteOrder.nativeOrder());
                                   ShortBuffer tb = bbc.asShortBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public final void intoByteArray(byte[] a, int ix, Mask<Short> m) {
        ShortMaxVector oldVal = (ShortMaxVector) ShortVector.fromByteArray(SPECIES, a, ix);
        ShortMaxVector newVal = oldVal.blend(this, m);
        newVal.intoByteArray(a, ix);
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix) {
        if (bb.order() != ByteOrder.nativeOrder()) {
            throw new IllegalArgumentException();
        }
        if (bb.isReadOnly()) {
            throw new ReadOnlyBufferException();
        }
        ix = VectorIntrinsics.checkIndex(ix, bb.limit(), bitSize() / Byte.SIZE);
        VectorIntrinsics.store(ShortMaxVector.class, short.class, LENGTH,
                               U.getReference(bb, BYTE_BUFFER_HB), ix + U.getLong(bb, BUFFER_ADDRESS),
                               this,
                               bb, ix,
                               (c, idx, v) -> {
                                   ByteBuffer bbc = c.duplicate().position(idx).order(ByteOrder.nativeOrder());
                                   ShortBuffer tb = bbc.asShortBuffer();
                                   v.forEach((i, e) -> tb.put(e));
                               });
    }

    @Override
    @ForceInline
    public void intoByteBuffer(ByteBuffer bb, int ix, Mask<Short> m) {
        ShortMaxVector oldVal = (ShortMaxVector) ShortVector.fromByteBuffer(SPECIES, bb, ix);
        ShortMaxVector newVal = oldVal.blend(this, m);
        newVal.intoByteBuffer(bb, ix);
    }

    //

    @Override
    public String toString() {
        return Arrays.toString(getElements());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;

        ShortMaxVector that = (ShortMaxVector) o;
        return this.equal(that).allTrue();
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(vec);
    }

    // Binary test

    @Override
    ShortMaxMask bTest(Vector<Short> o, FBinTest f) {
        short[] vec1 = getElements();
        short[] vec2 = ((ShortMaxVector)o).getElements();
        boolean[] bits = new boolean[length()];
        for (int i = 0; i < length(); i++){
            bits[i] = f.apply(i, vec1[i], vec2[i]);
        }
        return new ShortMaxMask(bits);
    }

    // Comparisons

    @Override
    @ForceInline
    public ShortMaxMask equal(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;

        return VectorIntrinsics.compare(
            BT_eq, ShortMaxVector.class, ShortMaxMask.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a == b));
    }

    @Override
    @ForceInline
    public ShortMaxMask notEqual(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;

        return VectorIntrinsics.compare(
            BT_ne, ShortMaxVector.class, ShortMaxMask.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a != b));
    }

    @Override
    @ForceInline
    public ShortMaxMask lessThan(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;

        return VectorIntrinsics.compare(
            BT_lt, ShortMaxVector.class, ShortMaxMask.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a < b));
    }

    @Override
    @ForceInline
    public ShortMaxMask lessThanEq(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;

        return VectorIntrinsics.compare(
            BT_le, ShortMaxVector.class, ShortMaxMask.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a <= b));
    }

    @Override
    @ForceInline
    public ShortMaxMask greaterThan(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;

        return (ShortMaxMask) VectorIntrinsics.compare(
            BT_gt, ShortMaxVector.class, ShortMaxMask.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a > b));
    }

    @Override
    @ForceInline
    public ShortMaxMask greaterThanEq(Vector<Short> o) {
        Objects.requireNonNull(o);
        ShortMaxVector v = (ShortMaxVector)o;

        return VectorIntrinsics.compare(
            BT_ge, ShortMaxVector.class, ShortMaxMask.class, short.class, LENGTH,
            this, v,
            (v1, v2) -> v1.bTest(v2, (i, a, b) -> a >= b));
    }

    // Foreach

    @Override
    void forEach(FUnCon f) {
        short[] vec = getElements();
        for (int i = 0; i < length(); i++) {
            f.apply(i, vec[i]);
        }
    }

    @Override
    void forEach(Mask<Short> o, FUnCon f) {
        boolean[] mbits = ((ShortMaxMask)o).getBits();
        forEach((i, a) -> {
            if (mbits[i]) { f.apply(i, a); }
        });
    }



    @Override
    public ShortMaxVector rotateEL(int j) {
        short[] vec = getElements();
        short[] res = new short[length()];
        for (int i = 0; i < length(); i++){
            res[(j + i) % length()] = vec[i];
        }
        return new ShortMaxVector(res);
    }

    @Override
    public ShortMaxVector rotateER(int j) {
        short[] vec = getElements();
        short[] res = new short[length()];
        for (int i = 0; i < length(); i++){
            int z = i - j;
            if(j < 0) {
                res[length() + z] = vec[i];
            } else {
                res[z] = vec[i];
            }
        }
        return new ShortMaxVector(res);
    }

    @Override
    public ShortMaxVector shiftEL(int j) {
        short[] vec = getElements();
        short[] res = new short[length()];
        for (int i = 0; i < length() - j; i++) {
            res[i] = vec[i + j];
        }
        return new ShortMaxVector(res);
    }

    @Override
    public ShortMaxVector shiftER(int j) {
        short[] vec = getElements();
        short[] res = new short[length()];
        for (int i = 0; i < length() - j; i++){
            res[i + j] = vec[i];
        }
        return new ShortMaxVector(res);
    }

    @Override
    @ForceInline
    public ShortMaxVector rearrange(Vector<Short> v,
                                  Shuffle<Short> s, Mask<Short> m) {
        return this.rearrange(s).blend(v.rearrange(s), m);
    }

    @Override
    @ForceInline
    public ShortMaxVector rearrange(Shuffle<Short> o1) {
        Objects.requireNonNull(o1);
        ShortMaxShuffle s =  (ShortMaxShuffle)o1;

        return VectorIntrinsics.rearrangeOp(
            ShortMaxVector.class, ShortMaxShuffle.class, short.class, LENGTH,
            this, s,
            (v1, s_) -> v1.uOp((i, a) -> {
                int ei = s_.getElement(i);
                return v1.get(ei);
            }));
    }

    @Override
    @ForceInline
    public ShortMaxVector blend(Vector<Short> o1, Mask<Short> o2) {
        Objects.requireNonNull(o1);
        Objects.requireNonNull(o2);
        ShortMaxVector v = (ShortMaxVector)o1;
        ShortMaxMask   m = (ShortMaxMask)o2;

        return VectorIntrinsics.blend(
            ShortMaxVector.class, ShortMaxMask.class, short.class, LENGTH,
            this, v, m,
            (v1, v2, m_) -> v1.bOp(v2, (i, a, b) -> m_.getElement(i) ? b : a));
    }

    // Accessors

    @Override
    public short get(int i) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return (short) VectorIntrinsics.extract(
                                ShortMaxVector.class, short.class, LENGTH,
                                this, i,
                                (vec, ix) -> {
                                    short[] vecarr = vec.getElements();
                                    return (long)vecarr[ix];
                                });
    }

    @Override
    public ShortMaxVector with(int i, short e) {
        if (i < 0 || i >= LENGTH) {
            throw new IllegalArgumentException("Index " + i + " must be zero or positive, and less than " + LENGTH);
        }
        return VectorIntrinsics.insert(
                                ShortMaxVector.class, short.class, LENGTH,
                                this, i, (long)e,
                                (v, ix, bits) -> {
                                    short[] res = v.getElements().clone();
                                    res[ix] = (short)bits;
                                    return new ShortMaxVector(res);
                                });
    }

    // Mask

    static final class ShortMaxMask extends AbstractMask<Short> {
        static final ShortMaxMask TRUE_MASK = new ShortMaxMask(true);
        static final ShortMaxMask FALSE_MASK = new ShortMaxMask(false);

        private final boolean[] bits; // Don't access directly, use getBits() instead.

        public ShortMaxMask(boolean[] bits) {
            this(bits, 0);
        }

        public ShortMaxMask(boolean[] bits, int offset) {
            boolean[] a = new boolean[species().length()];
            for (int i = 0; i < a.length; i++) {
                a[i] = bits[offset + i];
            }
            this.bits = a;
        }

        public ShortMaxMask(boolean val) {
            boolean[] bits = new boolean[species().length()];
            Arrays.fill(bits, val);
            this.bits = bits;
        }

        boolean[] getBits() {
            return VectorIntrinsics.maybeRebox(this).bits;
        }

        @Override
        ShortMaxMask uOp(MUnOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i]);
            }
            return new ShortMaxMask(res);
        }

        @Override
        ShortMaxMask bOp(Mask<Short> o, MBinOp f) {
            boolean[] res = new boolean[species().length()];
            boolean[] bits = getBits();
            boolean[] mbits = ((ShortMaxMask)o).getBits();
            for (int i = 0; i < species().length(); i++) {
                res[i] = f.apply(i, bits[i], mbits[i]);
            }
            return new ShortMaxMask(res);
        }

        @Override
        public ShortMaxSpecies species() {
            return SPECIES;
        }

        @Override
        public ShortMaxVector toVector() {
            short[] res = new short[species().length()];
            boolean[] bits = getBits();
            for (int i = 0; i < species().length(); i++) {
                // -1 will result in the most significant bit being set in
                // addition to some or all other bits
                res[i] = (short) (bits[i] ? -1 : 0);
            }
            return new ShortMaxVector(res);
        }

        // Unary operations

        @Override
        @ForceInline
        public ShortMaxMask not() {
            return (ShortMaxMask) VectorIntrinsics.unaryOp(
                                             VECTOR_OP_NOT, ShortMaxMask.class, short.class, LENGTH,
                                             this,
                                             (m1) -> m1.uOp((i, a) -> !a));
        }

        // Binary operations

        @Override
        @ForceInline
        public ShortMaxMask and(Mask<Short> o) {
            Objects.requireNonNull(o);
            ShortMaxMask m = (ShortMaxMask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_AND, ShortMaxMask.class, short.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a & b));
        }

        @Override
        @ForceInline
        public ShortMaxMask or(Mask<Short> o) {
            Objects.requireNonNull(o);
            ShortMaxMask m = (ShortMaxMask)o;
            return VectorIntrinsics.binaryOp(VECTOR_OP_OR, ShortMaxMask.class, short.class, LENGTH,
                                             this, m,
                                             (m1, m2) -> m1.bOp(m2, (i, a, b) -> a | b));
        }

        // Reductions

        @Override
        @ForceInline
        public boolean anyTrue() {
            return VectorIntrinsics.test(COND_notZero, ShortMaxMask.class, short.class, LENGTH,
                                         this, this,
                                         (m, __) -> anyTrueHelper(((ShortMaxMask)m).getBits()));
        }

        @Override
        @ForceInline
        public boolean allTrue() {
            return VectorIntrinsics.test(COND_carrySet, ShortMaxMask.class, short.class, LENGTH,
                                         this, ShortVector.maskAllTrue(species()),
                                         (m, __) -> allTrueHelper(((ShortMaxMask)m).getBits()));
        }
    }

    // Shuffle

    static final class ShortMaxShuffle extends AbstractShuffle<Short> {
        ShortMaxShuffle(byte[] reorder) {
            super(reorder);
        }

        public ShortMaxShuffle(int[] reorder) {
            super(reorder);
        }

        public ShortMaxShuffle(int[] reorder, int i) {
            super(reorder, i);
        }

        public ShortMaxShuffle(IntUnaryOperator f) {
            super(f);
        }

        @Override
        public ShortMaxSpecies species() {
            return SPECIES;
        }

        @Override
        public ShortVector toVector() {
            short[] va = new short[SPECIES.length()];
            for (int i = 0; i < va.length; i++) {
              va[i] = (short) getElement(i);
            }
            return ShortVector.fromArray(SPECIES, va, 0);
        }

        @Override
        public ShortMaxShuffle rearrange(Vector.Shuffle<Short> o) {
            ShortMaxShuffle s = (ShortMaxShuffle) o;
            byte[] r = new byte[reorder.length];
            for (int i = 0; i < reorder.length; i++) {
                r[i] = reorder[s.reorder[i]];
            }
            return new ShortMaxShuffle(r);
        }
    }

    // Species

    @Override
    public ShortMaxSpecies species() {
        return SPECIES;
    }

    static final class ShortMaxSpecies extends ShortSpecies {
        static final int BIT_SIZE = Shape.S_Max_BIT.bitSize();

        static final int LENGTH = BIT_SIZE / Short.SIZE;

        @Override
        public String toString() {
           StringBuilder sb = new StringBuilder("Shape[");
           sb.append(bitSize()).append(" bits, ");
           sb.append(length()).append(" ").append(short.class.getSimpleName()).append("s x ");
           sb.append(elementSize()).append(" bits");
           sb.append("]");
           return sb.toString();
        }

        @Override
        @ForceInline
        public int bitSize() {
            return BIT_SIZE;
        }

        @Override
        @ForceInline
        public int length() {
            return LENGTH;
        }

        @Override
        @ForceInline
        public Class<Short> elementType() {
            return short.class;
        }

        @Override
        @ForceInline
        public Class<?> boxType() {
            return ShortMaxVector.class;
        }

        @Override
        @ForceInline
        public Class<?> maskType() {
            return ShortMaxMask.class;
        }

        @Override
        @ForceInline
        public int elementSize() {
            return Short.SIZE;
        }

        @Override
        @ForceInline
        @SuppressWarnings("unchecked")
        Class<?> vectorType() {
            return ShortMaxVector.class;
        }

        @Override
        @ForceInline
        public Shape shape() {
            return Shape.S_Max_BIT;
        }

        @Override
        ShortMaxVector op(FOp f) {
            short[] res = new short[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = f.apply(i);
            }
            return new ShortMaxVector(res);
        }

        @Override
        ShortMaxVector op(Mask<Short> o, FOp f) {
            short[] res = new short[length()];
            boolean[] mbits = ((ShortMaxMask)o).getBits();
            for (int i = 0; i < length(); i++) {
                if (mbits[i]) {
                    res[i] = f.apply(i);
                }
            }
            return new ShortMaxVector(res);
        }

        @Override
        ShortMaxMask opm(FOpm f) {
            boolean[] res = new boolean[length()];
            for (int i = 0; i < length(); i++) {
                res[i] = (boolean)f.apply(i);
            }
            return new ShortMaxMask(res);
        }

        // Factories

        @Override
        @ForceInline
        public ShortMaxVector zero() {
            return VectorIntrinsics.broadcastCoerced(ShortMaxVector.class, short.class, LENGTH,
                                                     0, SPECIES,
                                                     ((bits, s) -> ((ShortMaxSpecies)s).op(i -> (short)bits)));
        }

        @Override
        @ForceInline
        public ShortMaxVector broadcast(short e) {
            return VectorIntrinsics.broadcastCoerced(
                ShortMaxVector.class, short.class, LENGTH,
                e, SPECIES,
                ((bits, s) -> ((ShortMaxSpecies)s).op(i -> (short)bits)));
        }

        @Override
        @ForceInline
        public ShortMaxVector scalars(short... es) {
            Objects.requireNonNull(es);
            int ix = VectorIntrinsics.checkIndex(0, es.length, LENGTH);
            return VectorIntrinsics.load(ShortMaxVector.class, short.class, LENGTH,
                                         es, Unsafe.ARRAY_SHORT_BASE_OFFSET,
                                         es, ix, SPECIES,
                                         (c, idx, s) -> ((ShortMaxSpecies)s).op(n -> c[idx + n]));
        }

        @Override
        @ForceInline
        public <E> ShortMaxMask cast(Mask<E> m) {
            if (m.length() != LENGTH)
                throw new IllegalArgumentException("Mask length this species length differ");
            return new ShortMaxMask(m.toArray());
        }

        @Override
        @ForceInline
        public <E> ShortMaxShuffle cast(Shuffle<E> s) {
            if (s.length() != LENGTH)
                throw new IllegalArgumentException("Shuffle length this species length differ");
            return new ShortMaxShuffle(s.toArray());
        }
    }
}
