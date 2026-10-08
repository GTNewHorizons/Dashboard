package com.gtnewhorizon.dashboard.api;

/** A rectangle on the HUD, in GUI scaled pixels. */
public final class HudBounds {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public HudBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getRight() {
        return x + width;
    }

    public int getBottom() {
        return y + height;
    }

    public int getArea() {
        return width * height;
    }

    public boolean contains(int pointX, int pointY) {
        return pointX >= x && pointX < getRight() && pointY >= y && pointY < getBottom();
    }

    @Override
    public String toString() {
        return "HudBounds{x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + "}";
    }
}
