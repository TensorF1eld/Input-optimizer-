package dev.kemmlow.inputoptimizer.rawinput;

import java.util.function.Consumer;

public final class RawInputReplay {
    private RawInputReplay() {
    }

    /** Poll only available events; commit each event before polling the next. */
    public static void drain(RawInputEngine engine, ReplaySuppression suppression,
                             Consumer<RawInputEvent> replay, Runnable dispatch) {
        RawInputEvent event;
        double dx = 0, dy = 0;
        long movementTime = 0;
        boolean moved = false;
        while ((event = engine.pollEvent()) != null) {
            if (event instanceof RawMoveEvent movement) {
                if (!moved) movementTime = movement.timestamp();
                moved = true;
                dx += movement.dx(); dy += movement.dy();
                continue;
            }
            if (moved) {
                replayOne(new RawMoveEvent(dx, dy, movementTime), suppression, replay);
                moved = false; dx = dy = 0;
            }
            replayOne(event, suppression, replay);
            dispatch.run();
        }
        if (moved) replayOne(new RawMoveEvent(dx, dy, movementTime), suppression, replay);
    }

    private static void replayOne(RawInputEvent event, ReplaySuppression suppression, Consumer<RawInputEvent> replay) {
        suppression.beginReplay();
        try {
            replay.accept(event);
        } finally {
            suppression.endReplay();
        }
    }
}
