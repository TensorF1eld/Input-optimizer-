/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Library
 *  com.sun.jna.Native
 *  com.sun.jna.Structure
 *  com.sun.jna.Structure$FieldOrder
 */
package dev.kemmlow.inputoptimizer.rawinput.native_bindings;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Structure;

public interface LinuxEvdev
extends Library {
    public static final LinuxEvdev INSTANCE = (LinuxEvdev)Native.load((String)"c", LinuxEvdev.class);
    public static final int O_RDONLY = 0;
    public static final int O_NONBLOCK = 2048;
    public static final short EV_REL = 2;
    public static final short EV_KEY = 1;
    public static final short REL_X = 0;
    public static final short REL_Y = 1;
    public static final short REL_WHEEL = 8;
    public static final short REL_HWHEEL = 6;
    public static final short BTN_LEFT = 272;
    public static final short BTN_RIGHT = 273;
    public static final short BTN_MIDDLE = 274;
    public static final short BTN_SIDE = 275;
    public static final short BTN_EXTRA = 276;

    public int open(String var1, int var2);

    public int close(int var1);

    public int read(int var1, byte[] var2, int var3);

    public int ioctl(int var1, long var2, byte[] var4);

    @Structure.FieldOrder(value={"tv_sec", "tv_usec", "type", "code", "value"})
    public static class InputEvent
    extends Structure {
        public long tv_sec;
        public long tv_usec;
        public short type;
        public short code;
        public int value;
        public static final int SIZE = 24;
    }
}

