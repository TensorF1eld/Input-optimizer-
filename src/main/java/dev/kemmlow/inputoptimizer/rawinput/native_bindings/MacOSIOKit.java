/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Callback
 *  com.sun.jna.Library
 *  com.sun.jna.Native
 *  com.sun.jna.Pointer
 */
package dev.kemmlow.inputoptimizer.rawinput.native_bindings;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public interface MacOSIOKit
extends Library {
    public static final MacOSIOKit INSTANCE = (MacOSIOKit)Native.load((String)"IOKit", MacOSIOKit.class);

    public Pointer IOHIDManagerCreate(Pointer var1, int var2);

    public void IOHIDManagerSetDeviceMatchingMultiple(Pointer var1, Pointer var2);

    public int IOHIDManagerOpen(Pointer var1, int var2);

    public void IOHIDManagerClose(Pointer var1, int var2);

    public void IOHIDManagerScheduleWithRunLoop(Pointer var1, Pointer var2, Pointer var3);

    public Pointer IOHIDQueueCreate(Pointer var1, Pointer var2, int var3, int var4);

    public void IOHIDQueueStart(Pointer var1);

    public void IOHIDQueueStop(Pointer var1);

    public Pointer IOHIDQueueCopyNextValue(Pointer var1);

    public int IOHIDValueGetIntegerValue(Pointer var1);

    public Pointer IOHIDValueGetElement(Pointer var1);

    public Pointer IOHIDElementGetDevice(Pointer var1);

    public int IOHIDElementGetUsagePage(Pointer var1);

    public int IOHIDElementGetUsage(Pointer var1);

    public void IOHIDManagerRegisterInputValueCallback(Pointer var1, HIDValueCallback var2, Pointer var3);

    public static interface HIDValueCallback
    extends Callback {
        public void callback(Pointer var1, int var2, Pointer var3, Pointer var4);
    }
}

