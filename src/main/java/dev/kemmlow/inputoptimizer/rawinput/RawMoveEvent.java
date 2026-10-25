package dev.kemmlow.inputoptimizer.rawinput;

/** Relative gameplay movement at this position in the discrete input queue. */
public record RawMoveEvent(double dx, double dy, long timestamp) implements RawInputEvent { }
