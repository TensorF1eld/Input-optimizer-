/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Native
 *  com.sun.jna.Pointer
 *  com.sun.jna.Structure
 *  com.sun.jna.Structure$FieldOrder
 *  com.sun.jna.platform.win32.WinDef$HWND
 *  com.sun.jna.ptr.IntByReference
 *  com.sun.jna.win32.StdCallLibrary
 *  com.sun.jna.win32.W32APIOptions
 */
package dev.kemmlow.inputoptimizer.rawinput.native_bindings;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import java.util.Map;

public interface WindowsUser32Ex
extends StdCallLibrary {
    public static final WindowsUser32Ex INSTANCE = (WindowsUser32Ex)Native.load((String)"user32", WindowsUser32Ex.class, (Map)W32APIOptions.DEFAULT_OPTIONS);

    public boolean RegisterRawInputDevices(RAWINPUTDEVICE[] var1, int var2, int var3);

    public int GetRawInputBuffer(Pointer var1, IntByReference var2, int var3);
    public int GetRawInputData(Pointer handle, int command, Pointer data, IntByReference size, int headerSize);
    public short GetKeyState(int virtualKey);

    @Structure.FieldOrder(value={"usUsagePage", "usUsage", "dwFlags", "hwndTarget"})
    public static class RAWINPUTDEVICE
    extends Structure {
        public short usUsagePage;
        public short usUsage;
        public int dwFlags;
        public WinDef.HWND hwndTarget;
    }
}
