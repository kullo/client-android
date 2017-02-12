/* Copyright 2015-2017 Kullo GmbH. All rights reserved. */
package net.kullo.reduce;

public class Size {
    public int width;
    public int height;

    public Size(int width, int height) {
        if (width < 0) throw new AssertionError("width must be >= 0");
        if (height < 0) throw new AssertionError("height must be >= 0");

        this.width = width;
        this.height = height;
    }

    @Override
    public boolean equals(Object other) {

        if (!(other instanceof Size)) {
            // Code branch also used when other == null
            // because null is not instance of Size
            return false;
        }

        Size otherSize = (Size) other;

        return this.width == otherSize.width
                && this.height == otherSize.height;
    }

    @Override
    public int hashCode() {
        // high bit of width and height is 0 due to non-negative values
        // So we have lowest 16 bits from every variable.
        // collisions possible when values exceed 65535,
        // so we are more or less safe when using this for images
        return (width<<16) | (0x0000FFFF & height);
    }

    @Override
    public String toString() {
        return "(" + width + "," + height + ")";
    }
}
