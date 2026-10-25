/*
 * Decompiled with CFR 0.152.
 */
package dev.kemmlow.inputoptimizer.rawinput;

public final class RawButtonEvent implements RawInputEvent {
    @Override public long timestamp() { return timestamp; }
    public final int button;
    public final int action;
    public final int modifiers;
    public final long timestamp;

    public RawButtonEvent(int button, int action, int modifiers, long timestamp) {
        this.button = button;
        this.action = action;
        this.modifiers = modifiers;
        this.timestamp = timestamp;
    }
}
