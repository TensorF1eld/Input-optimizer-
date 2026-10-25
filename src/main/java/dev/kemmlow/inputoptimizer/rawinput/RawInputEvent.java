package dev.kemmlow.inputoptimizer.rawinput;

/** A discrete input event in the order it was received from the OS input path. */
public sealed interface RawInputEvent permits RawKeyEvent, RawButtonEvent, RawScrollEvent, RawMoveEvent {
    long timestamp();
}
