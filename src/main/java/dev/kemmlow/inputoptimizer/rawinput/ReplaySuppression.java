package dev.kemmlow.inputoptimizer.rawinput;

import java.util.HashMap;
import java.util.Map;

/** Render-thread state: raw replay must not consume its own GLFW duplicate marks. */
public final class ReplaySuppression {
    private final Map<Long, Integer> keys = new HashMap<>();
    private final Map<Long, Integer> buttons = new HashMap<>();
    private final Map<ScrollDelta, Integer> scrolls = new HashMap<>();
    private int replayDepth;

    private record ScrollDelta(double horizontal, double vertical) {
        private ScrollDelta {
            horizontal = horizontal == 0.0 ? 0.0 : horizontal;
            vertical = vertical == 0.0 ? 0.0 : vertical;
        }
    }

    public void beginReplay() {
        replayDepth++;
    }

    public void endReplay() {
        if (replayDepth == 0) {
            throw new IllegalStateException("No input replay is active");
        }
        replayDepth--;
    }

    public boolean isReplaying() {
        return replayDepth != 0;
    }

    public void markKey(int key, int action) {
        keys.merge(identity(key, action), 1, Integer::sum);
    }

    public boolean consumeKey(int key, int action) {
        return !isReplaying() && consume(keys, identity(key, action));
    }

    public void markButton(int button, int action) {
        buttons.merge(identity(button, action), 1, Integer::sum);
    }

    public boolean consumeButton(int button, int action) {
        return !isReplaying() && consume(buttons, identity(button, action));
    }

    public void markScroll(double horizontal, double vertical) {
        scrolls.merge(new ScrollDelta(horizontal, vertical), 1, Integer::sum);
    }

    public boolean consumeScroll(double horizontal, double vertical) {
        return !isReplaying() && consume(scrolls, new ScrollDelta(horizontal, vertical));
    }

    public void clear() {
        keys.clear();
        buttons.clear();
        scrolls.clear();
    }

    private static long identity(int input, int action) {
        return ((long) input << 32) | (action & 0xffffffffL);
    }

    private static <T> boolean consume(Map<T, Integer> counts, T input) {
        Integer count = counts.get(input);
        if (count == null) {
            return false;
        }
        if (count == 1) {
            counts.remove(input);
        } else {
            counts.put(input, count - 1);
        }
        return true;
    }
}
