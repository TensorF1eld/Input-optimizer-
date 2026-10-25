package dev.kemmlow.inputoptimizer.rawinput;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Standalone behavior checks against the actual production queue and replay loop. */
public final class OrderedInputChecks {
    private static int checks;

    public static void main(String[] args) throws Exception {
        checkSlotClickOrder();
        checkClickBeforeSlot();
        checkScrollAndReleaseOrder();
        checkRapidClicksAndDuplicates();
        checkKeyboardAndScrollDuplicates();
        checkFocusLoss();
        checkExceptionCleanup();
        checkConcurrentArrival();
        checkEmptyQueueDoesNotWait();
        System.out.println("Ordered input checks passed: " + checks);
    }

    private static void checkSlotClickOrder() {
        Engine engine = new Engine();
        engine.add(new RawKeyEvent(2, 0, 1, 0, 100));
        engine.add(new RawButtonEvent(0, 1, 0, 100));
        engine.add(new RawKeyEvent(3, 0, 1, 0, 100));
        State state = new State();
        replay(engine, state);
        equal(state.trace, List.of("slot:2", "click:2", "slot:3"));
        checks++;
    }

    private static void checkClickBeforeSlot() {
        Engine engine = new Engine();
        engine.add(new RawButtonEvent(0, 1, 0, 20));
        engine.add(new RawKeyEvent(2, 0, 1, 0, 21));
        State state = new State();
        replay(engine, state);
        equal(state.trace, List.of("click:1", "slot:2"));
        checks++;
    }

    private static void checkScrollAndReleaseOrder() {
        Engine engine = new Engine();
        engine.add(new RawScrollEvent(0, 1, 1));
        engine.add(new RawButtonEvent(0, 1, 0, 2));
        engine.add(new RawButtonEvent(0, 0, 0, 3));
        engine.add(new RawScrollEvent(0, 1, 4));
        engine.add(new RawButtonEvent(0, 1, 0, 5));
        State state = new State();
        replay(engine, state);
        equal(state.trace, List.of("scroll:2", "click:2", "release", "scroll:3", "click:3"));
        checks++;
    }

    private static void checkRapidClicksAndDuplicates() {
        Engine engine = new Engine();
        ReplaySuppression suppression = new ReplaySuppression();
        List<Integer> actions = new ArrayList<>();
        for (int action : new int[]{1, 0, 1, 0}) {
            engine.add(new RawButtonEvent(0, action, 0, 1));
        }
        RawInputReplay.drain(engine, suppression, event -> {
            RawButtonEvent button = (RawButtonEvent) event;
            truth(!suppression.consumeButton(button.button, button.action), "Raw replay was cancelled as a duplicate");
            actions.add(button.action);
            suppression.markButton(button.button, button.action);
        }, () -> truth(!suppression.isReplaying(), "Replay guard leaked into dispatch"));
        equal(actions, List.of(1, 0, 1, 0));
        for (int action : actions) {
            truth(suppression.consumeButton(0, action), "A matching GLFW callback was not suppressed");
        }
        truth(!suppression.consumeButton(0, 1), "A fresh press was suppressed");
        truth(!suppression.consumeButton(0, 0), "A fresh release was suppressed");
        checks++;
    }

    private static void checkKeyboardAndScrollDuplicates() {
        ReplaySuppression suppression = new ReplaySuppression();
        suppression.markKey(2, 1);
        suppression.markKey(2, 0);
        suppression.markKey(2, 1);
        truth(!suppression.consumeKey(2, 2), "Key repeat consumed a press mark");
        suppression.beginReplay();
        truth(!suppression.consumeKey(2, 1), "Raw key consumed a GLFW mark");
        suppression.endReplay();
        truth(suppression.consumeKey(2, 0), "Release mark lost");
        truth(suppression.consumeKey(2, 1), "First press mark lost");
        truth(suppression.consumeKey(2, 1), "Second press mark lost");
        truth(!suppression.consumeKey(2, 1), "Fresh key press suppressed");
        suppression.markScroll(-0.0, 1);
        suppression.markScroll(0, 1);
        truth(!suppression.consumeScroll(0, -1), "Different scrolling was suppressed");
        truth(suppression.consumeScroll(0, 1), "First scroll mark lost");
        truth(suppression.consumeScroll(0, 1), "Second scroll mark lost");
        truth(!suppression.consumeScroll(0, 1), "Fresh scrolling suppressed");
        checks++;
    }

    private static void checkFocusLoss() {
        Engine engine = new Engine();
        engine.add(new RawKeyEvent(2, 0, 1, 0, 1));
        engine.add(new RawButtonEvent(0, 1, 0, 2));
        engine.add(new RawScrollEvent(0, 1, 3));
        engine.setFocused(false);
        truth(engine.pollEvent() == null, "Focus loss left queued events");
        ReplaySuppression suppression = new ReplaySuppression();
        suppression.markKey(2, 1);
        suppression.markButton(0, 1);
        suppression.markScroll(0, 1);
        suppression.clear();
        truth(!suppression.consumeKey(2, 1) && !suppression.consumeButton(0, 1)
                && !suppression.consumeScroll(0, 1), "Focus cleanup left duplicate marks");
        checks++;
    }

    private static void checkExceptionCleanup() {
        Engine engine = new Engine();
        engine.add(new RawButtonEvent(0, 1, 0, 1));
        ReplaySuppression suppression = new ReplaySuppression();
        try {
            RawInputReplay.drain(engine, suppression, event -> {
                throw new IllegalArgumentException("simulated callback failure");
            }, () -> { throw new AssertionError("Failed callback was dispatched"); });
            throw new AssertionError("Expected callback failure");
        } catch (IllegalArgumentException expected) {
            truth(!suppression.isReplaying(), "Replay guard survived callback failure");
        }
        checks++;
    }

    private static void checkConcurrentArrival() throws Exception {
        Engine engine = new Engine();
        engine.add(new RawKeyEvent(2, 0, 1, 0, 1));
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch published = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            try {
                started.await();
                engine.add(new RawButtonEvent(0, 1, 0, 2));
            } catch (InterruptedException interrupted) {
                throw new AssertionError(interrupted);
            } finally {
                published.countDown();
            }
        });
        producer.start();
        State state = new State();
        RawInputReplay.drain(engine, new ReplaySuppression(), event -> {
            state.accept(event);
            if (event instanceof RawKeyEvent) {
                started.countDown();
                try {
                    truth(published.await(2, TimeUnit.SECONDS), "Producer did not publish");
                } catch (InterruptedException interrupted) {
                    throw new AssertionError(interrupted);
                }
            }
        }, state::dispatch);
        producer.join(2000);
        truth(!producer.isAlive(), "Producer did not finish");
        equal(state.trace, List.of("slot:2", "click:2"));
        checks++;
    }

    private static void checkEmptyQueueDoesNotWait() throws Exception {
        Thread drain = new Thread(() -> RawInputReplay.drain(new Engine(), new ReplaySuppression(),
                event -> { throw new AssertionError("Empty queue replayed an event"); },
                () -> { throw new AssertionError("Empty queue dispatched"); }));
        drain.setDaemon(true);
        drain.start();
        drain.join(2000);
        truth(!drain.isAlive(), "Drain waited for a future event");
        checks++;
    }

    private static void replay(Engine engine, State state) {
        RawInputReplay.drain(engine, new ReplaySuppression(), state::accept, state::dispatch);
    }

    private static void truth(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void equal(Object actual, Object expected) {
        if (!actual.equals(expected)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    private static final class State {
        private int slot = 1;
        private RawInputEvent pending;
        private final List<String> trace = new ArrayList<>();

        private void accept(RawInputEvent event) {
            truth(pending == null, "Next event was replayed before prior action was dispatched");
            pending = event;
        }

        private void dispatch() {
            if (pending instanceof RawKeyEvent key && key.action == 1) {
                slot = key.key;
                trace.add("slot:" + slot);
            } else if (pending instanceof RawScrollEvent scroll) {
                slot += (int) scroll.vertical;
                trace.add("scroll:" + slot);
            } else if (pending instanceof RawButtonEvent button) {
                trace.add(button.action == 1 ? "click:" + slot : "release");
            }
            pending = null;
        }
    }

    private static final class Engine extends RawInputEngine {
        private void add(RawInputEvent event) { eventQueue.add(event); }
        public boolean initialize(long window) { return true; }
        public void shutdown() { }
        public void centerCursor() { }
        public String platformName() { return "test"; }
    }
}
