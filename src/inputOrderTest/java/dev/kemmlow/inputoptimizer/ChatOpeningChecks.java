package dev.kemmlow.inputoptimizer;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.kemmlow.inputoptimizer.mixin.MixinImmediateKeybinds;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.client.KeyMapping;

/** Executes the production chat wrapper against counted pending clicks. No game is launched. */
public final class ChatOpeningChecks {
    private static Field dispatching;
    private static Method wrapper;
    private static final MixinImmediateKeybinds MIXIN = new MixinImmediateKeybinds();
    private static int checks;

    public static void main(String[] args) throws Exception {
        dispatching = InputDispatchManager.class.getDeclaredField("dispatching");
        dispatching.setAccessible(true);
        wrapper = MixinImmediateKeybinds.class.getDeclaredMethod(
                "inputoptimizer$deferChatOpening", KeyMapping.class, Operation.class);
        wrapper.setAccessible(true);
        try {
            checkCommandOpeningCharacter();
            checkChatOpeningCharacter();
            checkPendingPressesArePreserved();
            checkNormalTickDoesNotInventClicks();
        } finally {
            dispatching.setBoolean(null, false);
        }
        System.out.println("Chat opening checks passed: " + checks);
    }

    private static void checkCommandOpeningCharacter() throws Exception {
        ChatInput input = new ChatInput(1, "/");
        dispatching.setBoolean(null, true);
        check(!consume(input), "Command chat opened during immediate dispatch");
        input.type('/');
        check(!input.open && input.calls == 0 && input.pending == 1,
                "Opening key consumed its pending click before its character callback");
        dispatching.setBoolean(null, false);
        check(consume(input), "Normal tick failed to consume the command key");
        check(input.text.toString().equals("/"), "One opening slash must produce exactly one slash");
        input.type('/');
        check(input.text.toString().equals("//"), "A deliberate second slash must still be typeable");
        checks++;
    }

    private static void checkChatOpeningCharacter() throws Exception {
        ChatInput input = new ChatInput(1, "");
        dispatching.setBoolean(null, true);
        check(!consume(input), "Message chat opened during immediate dispatch");
        input.type('t');
        dispatching.setBoolean(null, false);
        check(consume(input), "Normal tick failed to open message chat");
        check(input.text.length() == 0, "Opening T must not be inserted into message chat");
        input.type('/');
        check(input.text.toString().equals("/"), "T followed by slash must insert one slash");
        checks++;
    }

    private static void checkPendingPressesArePreserved() throws Exception {
        ChatInput input = new ChatInput(2, "/");
        dispatching.setBoolean(null, true);
        for (int event = 0; event < 8; event++) {
            check(!consume(input), "Repeated immediate dispatch opened chat");
        }
        check(input.calls == 0 && input.pending == 2, "Deferred chat clicks were lost");
        dispatching.setBoolean(null, false);
        check(consume(input) && consume(input) && !consume(input), "Normal consumption changed queued click count");
        checks++;
    }

    private static void checkNormalTickDoesNotInventClicks() throws Exception {
        ChatInput input = new ChatInput(0, "/");
        dispatching.setBoolean(null, false);
        check(!consume(input) && !input.open, "Chat opened without a pending keypress");
        checks++;
    }

    private static boolean consume(ChatInput input) throws Exception {
        Operation<Boolean> original = arguments -> input.consumePendingClick();
        return (boolean) wrapper.invoke(MIXIN, null, original);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static final class ChatInput {
        private int pending;
        private int calls;
        private boolean open;
        private final String initial;
        private final StringBuilder text = new StringBuilder();

        private ChatInput(int pending, String initial) {
            this.pending = pending;
            this.initial = initial;
        }

        private boolean consumePendingClick() {
            calls++;
            if (pending == 0) return false;
            pending--;
            open = true;
            text.setLength(0);
            text.append(initial);
            return true;
        }

        private void type(char character) {
            if (open) text.append(character);
        }
    }
}
