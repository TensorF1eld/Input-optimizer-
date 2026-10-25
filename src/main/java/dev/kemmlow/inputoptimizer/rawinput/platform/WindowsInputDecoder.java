package dev.kemmlow.inputoptimizer.rawinput.platform;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import dev.kemmlow.inputoptimizer.rawinput.*;
import java.util.Arrays;
import java.util.function.Consumer;

/** Native layouts from winuser.h; decoding is independent of window registration. */
public final class WindowsInputDecoder {
    public static final int HEADER_SIZE = 8 + 2 * Native.POINTER_SIZE;
    private final boolean[] held = new boolean[512];
    private boolean capsLock, numLock;
    public void resetHeld() { Arrays.fill(held, false); }
    public void setLocks(boolean caps, boolean num) { capsLock = caps; numLock = num; }
    public static boolean supportsKey(int key) {
        return key >= 48 && key <= 57 || key >= 65 && key <= 90 || key >= 256 && key <= 269
                || key >= 280 && key <= 284 || key >= 290 && key <= 313
                || key >= 320 && key <= 336 || key >= 340 && key <= 348
                || switch (key) { case 32, 39, 44, 45, 46, 47, 59, 61, 91, 92, 93, 96 -> true; default -> false; };
    }
    public void decode(Pointer p, int offset, int size, long timestamp, boolean keyboard, boolean mouse,
                       boolean gameplay, Consumer<RawInputEvent> output) {
        int type = p.getInt(offset);
        int data = offset + HEADER_SIZE;
        if (type == 1 && keyboard && size >= HEADER_SIZE + 16) keyboard(p, data, timestamp, output);
        if (type == 0 && mouse && size >= HEADER_SIZE + 24) mouse(p, data, timestamp, gameplay, output);
    }
    private void keyboard(Pointer p, int data, long timestamp, Consumer<RawInputEvent> output) {
        int scan = p.getShort(data) & 0xffff;
        int flags = p.getShort(data + 2L) & 0xffff;
        int vk = p.getShort(data + 6L) & 0xffff;
        if (scan == 0xff || vk == 0xff || vk == 0) return;
        boolean extended = (flags & 2) != 0;
        int key = switch (vk) {
            case 16 -> scan == 0x36 ? 344 : 340;
            case 17 -> extended ? 345 : 341;
            case 18 -> extended ? 346 : 342;
            case 13 -> extended ? 335 : 257;
            case 160 -> 340; case 161 -> 344;
            case 162 -> 341; case 163 -> 345;
            case 164 -> 342; case 165 -> 346;
            case 91 -> 343; case 92 -> 347;
            default -> VKeyToGlfw.convert((short) vk);
        };
        // With Num Lock off Windows reports navigation VKs for keypad keys.
        if (!extended && vk >= 33 && vk <= 46) {
            key = switch (scan) {
                case 0x52 -> 320; case 0x4f -> 321; case 0x50 -> 322; case 0x51 -> 323;
                case 0x4b -> 324; case 0x4c -> 325; case 0x4d -> 326; case 0x47 -> 327;
                case 0x48 -> 328; case 0x49 -> 329; case 0x53 -> 330;
                default -> key;
            };
        }
        if (key < 0 || key >= held.length) return;
        boolean release = (flags & 1) != 0;
        int action = release ? 0 : held[key] ? 2 : 1;
        held[key] = !release;
        if (action == 1 && vk == 20) capsLock = !capsLock;
        if (action == 1 && vk == 144) numLock = !numLock;
        // GLFW uses the platform scan code; the extended Windows prefix is 0x100.
        int scancode = scan | (extended ? 0x100 : 0);
        output.accept(new RawKeyEvent(key, scancode, action, modifiers(), timestamp));
    }
    private int modifiers() {
        return (held[340] || held[344] ? 1 : 0) | (held[341] || held[345] ? 2 : 0)
                | (held[342] || held[346] ? 4 : 0) | (held[343] || held[347] ? 8 : 0)
                | (capsLock ? 16 : 0) | (numLock ? 32 : 0);
    }
    private void mouse(Pointer p, int data, long timestamp, boolean gameplay, Consumer<RawInputEvent> output) {
        int flags = p.getShort(data) & 0xffff;
        int buttons = p.getShort(data + 4L) & 0xffff;
        if (gameplay && (flags & 1) == 0) {
            int dx = p.getInt(data + 12L), dy = p.getInt(data + 16L);
            if (dx != 0 || dy != 0) output.accept(new RawMoveEvent(dx, dy, timestamp));
        }
        for (int button = 0; button < 5; button++) {
            int mask = 1 << (button * 2);
            if ((buttons & mask) != 0) output.accept(new RawButtonEvent(button, 1, modifiers(), timestamp));
            if ((buttons & (mask << 1)) != 0) output.accept(new RawButtonEvent(button, 0, modifiers(), timestamp));
        }
        double wheel = p.getShort(data + 6L) / 120.0;
        if ((buttons & 0x400) != 0 && wheel != 0) output.accept(new RawScrollEvent(0, wheel, timestamp));
        if ((buttons & 0x800) != 0 && wheel != 0) output.accept(new RawScrollEvent(wheel, 0, timestamp));
    }
}
