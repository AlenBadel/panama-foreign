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

import jdk.internal.misc.Unsafe;
import jdk.internal.vm.annotation.ForceInline;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public interface Vector<E, S extends Vector.Shape> {

    Species<E, S> species();

    default Class<E> elementType() { return species().elementType(); }

    default S shape() { return species().shape(); }

    default int length() { return species().length(); }

    default int bitSize() { return species().bitSize(); }

    //Arithmetic
    Vector<E, S> add(Vector<E, S> o);

    Vector<E, S> add(Vector<E, S> o, Mask<E, S> m);

    Vector<E, S> addSaturate(Vector<E, S> o);

    Vector<E, S> addSaturate(Vector<E, S> o, Mask<E, S> m);

    Vector<E, S> sub(Vector<E, S> o);

    Vector<E, S> sub(Vector<E, S> o, Mask<E, S> m);

    Vector<E, S> subSaturate(Vector<E, S> o);

    Vector<E, S> subSaturate(Vector<E, S> o, Mask<E, S> m);

    Vector<E, S> mul(Vector<E, S> o);

    Vector<E, S> mul(Vector<E, S> o, Mask<E, S> m);

    Vector<E, S> neg();

    Vector<E, S> neg(Mask<E, S> m);

    //Maths from java.math
    Vector<E, S> abs();

    Vector<E, S> abs(Mask<E, S> m);

    Vector<E, S> min(Vector<E, S> o);

    Vector<E, S> max(Vector<E, S> o);

    //TODO: Parity

    //Comparisons
    //For now these are projected into the same element type.  False is the element 0.  True is otherwise.
    //TODO: N.B. Floating point NaN behaviors?
    //TODO: Check the JLS
    Mask<E, S> equal(Vector<E, S> o);

    Mask<E, S> notEqual(Vector<E, S> o);

    Mask<E, S> lessThan(Vector<E, S> o);

    Mask<E, S> lessThanEq(Vector<E, S> o);

    Mask<E, S> greaterThan(Vector<E, S> o);

    Mask<E, S> greaterThanEq(Vector<E, S> o);

    //Elemental shifting
    Vector<E, S> rotateEL(int i); //Rotate elements left

    Vector<E, S> rotateER(int i); //Rotate elements right

    Vector<E, S> shiftEL(int i); //shift elements left

    Vector<E, S> shiftER(int i); //shift elements right

    //Blend, etc.
    Vector<E, S> blend(Vector<E, S> o, Mask<E, S> m);

    //Shuffles
    Vector<E, S> shuffle(Vector<E, S> o, Shuffle<E, S> s); //TODO: Example

    Vector<E, S> swizzle(Shuffle<E, S> s);


    // Conversions

    // Bitwise preserving

    @ForceInline
    default <F, T extends Shape> Vector<F, T> reshape(Species<F, T> species) {
        return species.reshape(this);
    }

    @ForceInline
    default <F> Vector<F, S> rebracket(Species<F, S> species) {
        return species.reshape(this);
    }

    @ForceInline
    default <T extends Shape> Vector<E, T> resize(Species<E, T> species) {
        return species.reshape(this);
    }

    // Cast

    @ForceInline
    default <F, T extends Shape> Vector<F, T> cast(Species<F, T> species) {
        return species.cast(this);
    }

    //Array stores

    void intoByteArray(byte[] bs, int ix);

    void intoByteArray(byte[] bs, int ix, Mask<E, S> m);

    void intoByteBuffer(ByteBuffer bb);

    void intoByteBuffer(ByteBuffer bb, Mask<E, S> m);

    void intoByteBuffer(ByteBuffer bb, int ix);

    void intoByteBuffer(ByteBuffer bb, int ix, Mask<E, S> m);


    interface Species<E, S extends Shape> {
        Class<E> elementType();

        int elementSize();

        S shape();

        default int length() { return shape().length(this); }

        default int bitSize() { return shape().bitSize(); }

        // Factory

        Vector<E, S> zero();

        Vector<E, S> fromByteArray(byte[] bs, int ix);

        Vector<E, S> fromByteArray(byte[] bs, int ix, Mask<E, S> m);

        Vector<E, S> fromByteBuffer(ByteBuffer bb);

        Vector<E, S> fromByteBuffer(ByteBuffer bb, Mask<E, S> m);

        Vector<E, S> fromByteBuffer(ByteBuffer bb, int ix);

        Vector<E, S> fromByteBuffer(ByteBuffer bb, int ix, Mask<E, S> m);

        //Mask and shuffle constructions

        Mask<E, S> maskFromValues(boolean... bits);

        Mask<E, S> maskFromArray(boolean[] bits, int i);

        Mask<E, S> maskAllTrue();

        Mask<E, S> maskAllFalse();

        Shuffle<E, S> shuffleFromValues(int... ixs);

        Shuffle<E, S> shuffleFromArray(int[] ixs, int i);

        // Vector type/shape transformations

        // Reshape
        // Preserves bits, truncating if new shape is smaller in bit size than
        // old shape, or expanding (with zero bits) if new shape is larger in
        // bit size

        default <F, T extends Shape> Vector<E, S> reshape(Vector<F, T> o) {
            int blen = Math.max(o.species().bitSize(), bitSize()) / Byte.SIZE;
            ByteBuffer bb = ByteBuffer.allocate(blen).order(ByteOrder.nativeOrder());
            o.intoByteBuffer(bb, 0);
            return fromByteBuffer(bb, 0);
        }

        // Change type, not shape
        // No truncation or expansion of bits
        @ForceInline
        default <F> Vector<E, S> rebracket(Vector<F, S> o) {
            return reshape(o);
        }

        // Change shape, not type
        // Truncation or expansion of bits
        @ForceInline
        default <T extends Shape> Vector<E, S> resize(Vector<E, T> o) {
            return reshape(o);
        }

        // Cast
        // Elements will be converted as per JLS primitive conversion rules
        // If elementType == o.elementType then its equivalent to a resize
        <F, T extends Shape> Vector<E, S> cast(Vector<F, T> o);


        // Mask type/shape transformations

        default <F, T extends Shape> Mask<E, S> reshape(Mask<F, T> m) {
            return maskFromValues(m.toArray());
        }

        @ForceInline
        default <F> Mask<E, S> rebracket(Mask<F, S> m) {
            return reshape(m);
        }

        @ForceInline
        default <T extends Shape> Mask<E, S> resize(Mask<E, T> m) {
            return reshape(m);
        }


        // Shuffle type/shape transformations

        default <F, T extends Shape> Shuffle<E, S> reshape(Shuffle<F, T> m) {
            return shuffleFromValues(m.toArray());
        }

        @ForceInline
        default <F> Shuffle<E, S> rebracket(Shuffle<F, S> m) {
            return reshape(m);
        }

        @ForceInline
        default <T extends Shape> Shuffle<E, S> resize(Shuffle<E, T> m) {
            return reshape(m);
        }


        // Species/species transformations

        // Returns a species for a given element type and the length of this
        // species.
        // The length of the returned species will be equal to the length of
        // this species.
        //
        // Throws IAE if no shape exists for the element type and this species length,
//        default <F> Species<F, ?> toSpeciesWithSameNumberOfLanes(Class<F> c) {
//            // @@@ TODO implement and find better name
//            throw new UnsupportedOperationException();
//        }

    }

    interface Shape {
        int bitSize();  // usually 64, 128, 256, etc.

        default int length(Species<?, ?> s) { return bitSize() / s.elementSize(); }  // usually bitSize / sizeof(s.elementType)
    }

    abstract class Mask<E, S extends Shape> {
        public int length() { return species().length(); }

        public abstract long toLong();

        public abstract void intoArray(boolean[] bits, int i);

        public abstract boolean[] toArray();

        public abstract boolean anyTrue();

        public abstract boolean allTrue();

        public abstract int trueCount();

        // TODO: LZ count
        // numberOfLeadingZeros
        // numberOfTrailingZeros

        public abstract Mask<E, S> and(Mask<E, S> o);

        public abstract Mask<E, S> or(Mask<E, S> o);

        public abstract Mask<E, S> not();

        public abstract Species<E, S> species();

        public abstract Vector<E, S> toVector();

        public abstract boolean getElement(int i);

        @ForceInline
        public <F, T extends Shape> Mask<F, T> reshape(Species<F, T> species) {
            return species.reshape(this);
        }

        @ForceInline
        public <F> Mask<F, S> rebracket(Species<F, S> species) {
            return species.reshape(this);
        }

        @ForceInline
        public <T extends Shape> Mask<E, T> resize(Species<E, T> species) {
            return species.reshape(this);
        }
    }

    abstract class Shuffle<E, S extends Shape> {
        public int length() { return species().length(); }

        public abstract void intoArray(int[] ixs, int i);

        public abstract int[] toArray();

        public abstract Species<E, S> species();

        public abstract IntVector.IntSpecies<S> intSpecies();

        public abstract IntVector<S> toVector();

        public int getElement(int i) { return toArray()[i]; }

        @ForceInline
        public <F, T extends Shape> Shuffle<F, T> reshape(Species<F, T> species) {
            return species.reshape(this);
        }

        @ForceInline
        public <F> Shuffle<F, S> rebracket(Species<F, S> species) {
            return species.reshape(this);
        }

        @ForceInline
        public <T extends Shape> Shuffle<E, T> resize(Species<E, T> species) {
            return species.reshape(this);
        }
    }

    @SuppressWarnings("unchecked")
    static <E> Vector.Species<E, ?> preferredSpeciesInstance(Class<E> c) {
        Unsafe u = Unsafe.getUnsafe();

        int vectorLength = u.getMaxVectorSize(boxToPrimitive(c));
        int vectorBitSize = bitSizeForVectorLength(c, vectorLength);
        Shape s = shapeForVectorBitSize(vectorBitSize);
        return speciesInstance(c, s);
    }

    private static Class<?> boxToPrimitive(Class<?> c) {
        if (c == Float.class) {
            return float.class;
        }
        else if (c == Double.class) {
            return double.class;
        }
        else if (c == Byte.class) {
            return byte.class;
        }
        else if (c == Short.class) {
            return short.class;
        }
        else if (c == Integer.class) {
            return int.class;
        }
        else if (c == Long.class) {
            return long.class;
        }
        else {
            throw new IllegalArgumentException("Bad vector type: " + c.getName());
        }
    }
    // @@@ public static method on Species?
    private static int bitSizeForVectorLength(Class<?> c, int elementSize) {
        if (c == Float.class) {
            return Float.SIZE * elementSize;
        }
        else if (c == Double.class) {
            return Double.SIZE * elementSize;
        }
        else if (c == Byte.class) {
            return Byte.SIZE * elementSize;
        }
        else if (c == Short.class) {
            return Short.SIZE * elementSize;
        }
        else if (c == Integer.class) {
            return Integer.SIZE * elementSize;
        }
        else if (c == Long.class) {
            return Long.SIZE * elementSize;
        }
        else {
            throw new IllegalArgumentException("Bad vector type: " + c.getName());
        }
    }

    // @@@ public static method on Shape?
    private static Shape shapeForVectorBitSize(int bitSize) {
        switch (bitSize) {
            case 64:
                return Shapes.S_64_BIT;
            case 128:
                return Shapes.S_128_BIT;
            case 256:
                return Shapes.S_256_BIT;
            case 512:
                return Shapes.S_512_BIT;
            default:
                throw new IllegalArgumentException("Bad vector bit size: " + bitSize);
        }
    }

    @SuppressWarnings("unchecked")
    static <E, S extends Shape> Vector.Species<E, S> speciesInstance(Class<E> c, S s) {
        Vector.Species<E, S> res = null;

        //Float
        if (c == Float.class) {
            if (s == Shapes.S_64_BIT)
                res = (Vector.Species<E, S>) Float64Vector.SPECIES;
            else if (s == Shapes.S_128_BIT)
                res = (Vector.Species<E, S>) Float128Vector.SPECIES;
            else if (s == Shapes.S_256_BIT)
                res = (Vector.Species<E, S>) Float256Vector.SPECIES;
            else if (s == Shapes.S_512_BIT)
                res = (Vector.Species<E, S>) Float512Vector.SPECIES;
            //Double
        }
        else if (c == Double.class) {
            if (s == Shapes.S_64_BIT)
                res = (Vector.Species<E, S>) Double64Vector.SPECIES;
            else if (s == Shapes.S_128_BIT)
                res = (Vector.Species<E, S>) Double128Vector.SPECIES;
            else if (s == Shapes.S_256_BIT)
                res = (Vector.Species<E, S>) Double256Vector.SPECIES;
            else if (s == Shapes.S_512_BIT)
                res = (Vector.Species<E, S>) Double512Vector.SPECIES;
            //Byte
        }
        else if (c == Byte.class) {
            if (s == Shapes.S_64_BIT)
                res = (Vector.Species<E, S>) Byte64Vector.SPECIES;
            else if (s == Shapes.S_128_BIT)
                res = (Vector.Species<E, S>) Byte128Vector.SPECIES;
            else if (s == Shapes.S_256_BIT)
                res = (Vector.Species<E, S>) Byte256Vector.SPECIES;
            else if (s == Shapes.S_512_BIT)
                res = (Vector.Species<E, S>) Byte512Vector.SPECIES;
            //Short
        }
        else if (c == Short.class) {
            if (s == Shapes.S_64_BIT)
                res = (Vector.Species<E, S>) Short64Vector.SPECIES;
            else if (s == Shapes.S_128_BIT)
                res = (Vector.Species<E, S>) Short128Vector.SPECIES;
            else if (s == Shapes.S_256_BIT)
                res = (Vector.Species<E, S>) Short256Vector.SPECIES;
            else if (s == Shapes.S_512_BIT)
                res = (Vector.Species<E, S>) Short512Vector.SPECIES;
            //Int
        }
        else if (c == Integer.class) {
            if (s == Shapes.S_64_BIT)
                res = (Vector.Species<E, S>) Int64Vector.SPECIES;
            else if (s == Shapes.S_128_BIT)
                res = (Vector.Species<E, S>) Int128Vector.SPECIES;
            else if (s == Shapes.S_256_BIT)
                res = (Vector.Species<E, S>) Int256Vector.SPECIES;
            else if (s == Shapes.S_512_BIT)
                res = (Vector.Species<E, S>) Int512Vector.SPECIES;
            //Long
        }
        else if (c == Long.class) {
            if (s == Shapes.S_64_BIT)
                res = (Vector.Species<E, S>) Long64Vector.SPECIES;
            else if (s == Shapes.S_128_BIT)
                res = (Vector.Species<E, S>) Long128Vector.SPECIES;
            else if (s == Shapes.S_256_BIT)
                res = (Vector.Species<E, S>) Long256Vector.SPECIES;
            else if (s == Shapes.S_512_BIT)
                res = (Vector.Species<E, S>) Long512Vector.SPECIES;
        }
        else {
            throw new IllegalArgumentException("Bad vector type: " + c.getName());
        }
        return res;
    }
}
