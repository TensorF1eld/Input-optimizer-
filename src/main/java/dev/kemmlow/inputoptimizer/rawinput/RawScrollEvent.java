/*
 * Decompiled with CFR 0.152.
 */
package dev.kemmlow.inputoptimizer.rawinput;

public final class RawScrollEvent implements RawInputEvent {
    @Override public long timestamp() { return timestamp; }
    public final double horizontal;
    public final double vertical;
    public final long timestamp;

    public RawScrollEvent(double horizontal, double vertical, long timestamp) {
        this.horizontal = horizontal;
        this.vertical = vertical;
        this.timestamp = timestamp;
    }
}
