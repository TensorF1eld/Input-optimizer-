/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Library
 *  com.sun.jna.Native
 *  com.sun.jna.NativeLibrary
 *  com.sun.jna.Pointer
 */
package dev.kemmlow.inputoptimizer.rawinput.native_bindings;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

public interface MacOSCoreFoundation
extends Library {
    public static final NativeLibrary CF_LIB = NativeLibrary.getInstance((String)"CoreFoundation");
    public static final MacOSCoreFoundation INSTANCE = (MacOSCoreFoundation)Native.load((String)"CoreFoundation", MacOSCoreFoundation.class);
    public static final int kCFNumberSInt32Type = 3;
    public static final int kCFNumberSInt64Type = 4;
    public static final int kCFStringEncodingUTF8 = 0x8000100;
    public static final Pointer kCFRunLoopDefaultMode = CF_LIB.getGlobalVariableAddress("kCFRunLoopDefaultMode").getPointer(0L);
    public static final Pointer kCFTypeDictionaryKeyCallBacks = CF_LIB.getGlobalVariableAddress("kCFTypeDictionaryKeyCallBacks");
    public static final Pointer kCFTypeDictionaryValueCallBacks = CF_LIB.getGlobalVariableAddress("kCFTypeDictionaryValueCallBacks");
    public static final Pointer kCFTypeArrayCallBacks = CF_LIB.getGlobalVariableAddress("kCFTypeArrayCallBacks");

    public Pointer CFRunLoopGetCurrent();

    public void CFRunLoopRun();

    public void CFRunLoopStop(Pointer var1);

    public Pointer CFDictionaryCreateMutable(Pointer var1, int var2, Pointer var3, Pointer var4);

    public void CFDictionarySetValue(Pointer var1, Pointer var2, Pointer var3);

    public Pointer CFNumberCreate(Pointer var1, int var2, Pointer var3);

    public Pointer CFStringCreateWithCString(Pointer var1, String var2, int var3);

    public Pointer CFArrayCreate(Pointer var1, Pointer[] var2, int var3, Pointer var4);

    public void CFRelease(Pointer var1);

    public Pointer CFRetain(Pointer var1);
}

