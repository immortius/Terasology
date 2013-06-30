package org.terasology.math;

import javax.vecmath.Vector2d;
import javax.vecmath.Vector2f;

/**
 * @author Immortius
 */
public class Vector2i {
    public int x = 0;
    public int y = 0;

    public Vector2i(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Vector2i(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("Requires array of 2 ints, received null");
        }
        if (values.length != 2) {
            throw new IllegalArgumentException("Requires array of 2 ints, received " + values.length);
        }
        this.x = values[0];
        this.y = values[1];
    }

    public Vector2i(Vector2i other) {
        this.x = other.x;
        this.y = other.y;
    }

    public Vector2i() {
    }

    public final void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public final void set(int[] values) {
        if (values == null) {
            throw new IllegalArgumentException("Requires array of 2 ints, received null");
        }
        if (values.length != 2) {
            throw new IllegalArgumentException("Requires array of 2 ints, received " + values.length);
        }
        this.x = values[0];
        this.y = values[1];
    }

    public final void set(Vector2i other) {
        this.x = other.x;
        this.y = other.y;
    }

    public final void get(int[] values) {
        if (values == null || values.length < 2) {
            throw new IllegalArgumentException("Values must not be null or less than length 2");
        }
        values[0] = x;
        values[1] = y;
    }

    public final void add(Vector2i t1, Vector2i t2) {
        x = t1.x + t2.x;
        y = t1.y + t2.y;
    }

    public final void add(Vector2i other) {
        x += other.x;
        y += other.y;
    }

    public final void negate(Vector2i other) {
        x = -other.x;
        y = -other.y;
    }

    public final void negate() {
        x = -x;
        y = -y;
    }

    public final void mult(int amount) {
        x *= amount;
        y *= amount;
    }

    public final void mult(int x, int y) {
        this.x *= x;
        this.y *= y;
    }

    public final void mult(Vector2i other) {
        this.x *= other.x;
        this.y *= other.y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o instanceof Vector2i) {
            Vector2i other = (Vector2i) o;
            return x == other.x && y == other.y;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 0;

        hash += (x - Integer.MIN_VALUE) & 0xFFFF;
        hash <<= 16;
        hash += (y - Integer.MIN_VALUE) & 0xFFFF;

        return hash;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }

    /**
     * @return The equivalent Vector2f
     */
    public Vector2f toVector2f() {
        return new Vector2f(x, y);
    }

    /**
     * @return The equivalent Vector2d
     */
    public Vector2d toVector2d() {
        return new Vector2d(x, y);
    }
}
