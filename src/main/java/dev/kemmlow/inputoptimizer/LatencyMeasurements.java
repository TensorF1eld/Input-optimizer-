package dev.kemmlow.inputoptimizer;

import java.util.Arrays;
import java.util.Locale;

/** Render-thread-only, bounded storage. No logging, sorting or allocation while sampling. */
public final class LatencyMeasurements {
    public static final long NO_SAMPLE = Long.MIN_VALUE;
    public static final long MAX_PENDING_NANOS = 2_000_000_000L;
    private final Pending[] presses = new Pending[11];
    private final long[] actions = new long[11];
    public final Samples callbacks = new Samples();
    public final Samples keybinds = new Samples();
    public final Samples actionStarts = new Samples();
    public final Samples rawQueue = new Samples();

    public LatencyMeasurements() {
        for (int i = 0; i < presses.length; i++) presses[i] = new Pending();
        clearPending();
    }
    public void press(int binding, long now) { presses[binding].add(now); }
    public void consumed(int binding, long now) {
        long start = presses[binding].take(now);
        actions[binding] = start;
        if (start != NO_SAMPLE) keybinds.add(now - start);
    }
    public void action(int binding, long now) {
        long start = actions[binding];
        actions[binding] = NO_SAMPLE;
        if (start != NO_SAMPLE && now - start >= 0 && now - start <= MAX_PENDING_NANOS) actionStarts.add(now - start);
    }
    public void clearActions() { Arrays.fill(actions, NO_SAMPLE); }
    public void clearPending() { for (Pending p : presses) p.clear(); clearActions(); }
    public void reset() { clearPending(); callbacks.clear(); keybinds.clear(); actionStarts.clear(); rawQueue.clear(); }

    private static final class Pending {
        private final long[] times = new long[64];
        private int head, size;
        void add(long timestamp) {
            if (size == times.length) { head = (head + 1) % times.length; size--; }
            times[(head + size++) % times.length] = timestamp;
        }
        long take(long now) {
            while (size != 0) {
                long value = times[head]; head = (head + 1) % times.length; size--;
                long age = now - value;
                if (age >= 0 && age <= MAX_PENDING_NANOS) return value;
            }
            return NO_SAMPLE;
        }
        void clear() { head = size = 0; }
    }
    public static final class Samples {
        private final long[] values = new long[4096];
        private int next, size;
        public void add(long nanos) {
            if (nanos < 0) return;
            values[next] = nanos; next = (next + 1) % values.length;
            size = Math.min(size + 1, values.length);
        }
        public int size() { return size; }
        public long percentile(int percent) {
            if (size == 0) return 0;
            long[] copy = Arrays.copyOf(values, size); Arrays.sort(copy);
            return copy[Math.max(0, (int) Math.ceil(size * percent / 100.0) - 1)];
        }
        public String summary(String name) {
            if (size == 0) return name + ": no samples";
            double total = 0; long max = 0;
            for (int i = 0; i < size; i++) { total += values[i]; max = Math.max(max, values[i]); }
            return String.format(Locale.ROOT, "%s: n=%d avg %.3f p50 %.3f p95 %.3f p99 %.3f max %.3f ms",
                    name, size, total / size / 1e6, percentile(50) / 1e6, percentile(95) / 1e6,
                    percentile(99) / 1e6, max / 1e6);
        }
        public void clear() { next = size = 0; }
    }
}
