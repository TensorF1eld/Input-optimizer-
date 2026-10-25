/*
 * Decompiled with CFR 0.152.
 */
package dev.kemmlow.inputoptimizer.rawinput;

import java.util.concurrent.atomic.AtomicLong;

public final class PollingRateOptimizer {
    private final AtomicLong frameAccumX = new AtomicLong(0L);
    private final AtomicLong frameAccumY = new AtomicLong(0L);

    public void addDelta(double dx, double dy, long timestampNanos) {
        PollingRateOptimizer.addAtomic(this.frameAccumX, dx);
        PollingRateOptimizer.addAtomic(this.frameAccumY, dy);
    }

    public double[] consumeWeighted(long frameTimeNanos) {
        double x = Double.longBitsToDouble(this.frameAccumX.getAndSet(0L));
        double y = Double.longBitsToDouble(this.frameAccumY.getAndSet(0L));
        return new double[]{x, y};
    }

    public void clear() {
        this.frameAccumX.set(0L);
        this.frameAccumY.set(0L);
    }

    private static void addAtomic(AtomicLong bits, double value) {
        long next;
        long prev;
        while (!bits.compareAndSet(prev = bits.get(), next = Double.doubleToLongBits(Double.longBitsToDouble(prev) + value))) {
        }
    }
}

