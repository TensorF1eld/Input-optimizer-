/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.sun.jna.Memory
 *  com.sun.jna.Pointer
 */
package dev.kemmlow.inputoptimizer.rawinput.platform;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import dev.kemmlow.inputoptimizer.rawinput.RawButtonEvent;
import dev.kemmlow.inputoptimizer.rawinput.RawInputEngine;
import dev.kemmlow.inputoptimizer.rawinput.RawKeyEvent;
import dev.kemmlow.inputoptimizer.rawinput.RawScrollEvent;
import dev.kemmlow.inputoptimizer.rawinput.native_bindings.MacOSCoreFoundation;
import dev.kemmlow.inputoptimizer.rawinput.native_bindings.MacOSIOKit;
import dev.kemmlow.inputoptimizer.rawinput.platform.MacOSKeyToGlfw;

public class MacOSRawInputEngine
extends RawInputEngine {
    private volatile Pointer hidManager;
    private volatile Pointer runLoop;

    @Override
    public boolean initialize(long glfwWindow) {
        this.running = true;
        Thread thread = new Thread(this::hidLoop, "inputoptimizer-macOS-IOHID");
        thread.setPriority(10);
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private void hidLoop() {
        MacOSIOKit iokit = MacOSIOKit.INSTANCE;
        MacOSCoreFoundation cf = MacOSCoreFoundation.INSTANCE;
        this.hidManager = iokit.IOHIDManagerCreate(null, 0);
        if (this.hidManager == null) {
            this.running = false;
            return;
        }
        Pointer matchingArray = this.createMatchingArray();
        if (matchingArray == null) {
            cf.CFRelease(this.hidManager);
            this.hidManager = null;
            this.running = false;
            return;
        }
        iokit.IOHIDManagerSetDeviceMatchingMultiple(this.hidManager, matchingArray);
        cf.CFRelease(matchingArray);
        MacOSIOKit.HIDValueCallback callback = (context, result, sender, value) -> {
            if (!this.focused) {
                return;
            }
            this.processEvent(value);
        };
        iokit.IOHIDManagerRegisterInputValueCallback(this.hidManager, callback, null);
        this.runLoop = cf.CFRunLoopGetCurrent();
        iokit.IOHIDManagerScheduleWithRunLoop(this.hidManager, this.runLoop, MacOSCoreFoundation.kCFRunLoopDefaultMode);
        int ret = -1;
        for (int attempt = 0; attempt < 5 && this.running && (ret = iokit.IOHIDManagerOpen(this.hidManager, 0)) != 0; ++attempt) {
            try {
                Thread.sleep(200L * (long)(attempt + 1));
                continue;
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }
        if (ret != 0) {
            System.err.printf("[InputOptimizer] IOHIDManagerOpen failed after retries: 0x%08X%n", ret);
            cf.CFRelease(this.hidManager);
            this.hidManager = null;
            this.running = false;
            return;
        }
        cf.CFRunLoopRun();
        iokit.IOHIDManagerClose(this.hidManager, 0);
        cf.CFRelease(this.hidManager);
        this.hidManager = null;
    }

    private void processEvent(Pointer value) {
        int glfw;
        MacOSIOKit iokit = MacOSIOKit.INSTANCE;
        Pointer element = iokit.IOHIDValueGetElement(value);
        if (element == null) {
            return;
        }
        int page = iokit.IOHIDElementGetUsagePage(element);
        int usage = iokit.IOHIDElementGetUsage(element);
        int val = iokit.IOHIDValueGetIntegerValue(value);
        long now = System.nanoTime();
        if (page == 1 && captureMouse) {
            if (usage == 48) {
                this.queueMovement(val, 0.0, now);
            } else if (usage == 49) {
                this.queueMovement(0.0, val, now);
            } else if (usage == 56) {
                this.eventQueue.add(new RawScrollEvent(0.0, val, now));
            }
        } else if (page == 9 && captureMouse) {
            int btn = this.mapButton(usage);
            if (btn >= 0) {
                this.eventQueue.add(new RawButtonEvent(btn, val == 1 ? 1 : 0, 0, now));
            }
        } else if (page == 7 && captureKeyboard && val <= 1 && (glfw = MacOSKeyToGlfw.convert(usage)) != -1) {
            this.eventQueue.add(new RawKeyEvent(glfw, usage, val, 0, now));
        }
    }

    private int mapButton(int usage) {
        return switch (usage) {
            case 1 -> 0;
            case 2 -> 1;
            case 3 -> 2;
            default -> usage - 1;
        };
    }

    private Pointer createMatchingArray() {
        MacOSCoreFoundation cf = MacOSCoreFoundation.INSTANCE;
        Pointer mouse = this.createDeviceMatching(1, 2);
        Pointer keyboard = this.createDeviceMatching(1, 6);
        if (mouse == null || keyboard == null) {
            if (mouse != null) {
                cf.CFRelease(mouse);
            }
            if (keyboard != null) {
                cf.CFRelease(keyboard);
            }
            return null;
        }
        Pointer array = cf.CFArrayCreate(null, new Pointer[]{mouse, keyboard}, 2, MacOSCoreFoundation.kCFTypeArrayCallBacks);
        cf.CFRelease(mouse);
        cf.CFRelease(keyboard);
        return array;
    }

    private Pointer createDeviceMatching(int page, int usage) {
        MacOSCoreFoundation cf = MacOSCoreFoundation.INSTANCE;
        Memory pageMem = new Memory(4L);
        Memory usageMem = new Memory(4L);
        pageMem.setInt(0L, page);
        usageMem.setInt(0L, usage);
        Pointer pageNum = cf.CFNumberCreate(null, 3, (Pointer)pageMem);
        Pointer usageNum = cf.CFNumberCreate(null, 3, (Pointer)usageMem);
        if (pageNum == null || usageNum == null) {
            if (pageNum != null) {
                cf.CFRelease(pageNum);
            }
            if (usageNum != null) {
                cf.CFRelease(usageNum);
            }
            return null;
        }
        Pointer pageKey = cf.CFStringCreateWithCString(null, "DeviceUsagePage", 0x8000100);
        Pointer usageKey = cf.CFStringCreateWithCString(null, "DeviceUsage", 0x8000100);
        if (pageKey == null || usageKey == null) {
            cf.CFRelease(pageNum);
            cf.CFRelease(usageNum);
            if (pageKey != null) {
                cf.CFRelease(pageKey);
            }
            if (usageKey != null) {
                cf.CFRelease(usageKey);
            }
            return null;
        }
        Pointer dict = cf.CFDictionaryCreateMutable(null, 0, MacOSCoreFoundation.kCFTypeDictionaryKeyCallBacks, MacOSCoreFoundation.kCFTypeDictionaryValueCallBacks);
        if (dict == null) {
            cf.CFRelease(pageNum);
            cf.CFRelease(usageNum);
            cf.CFRelease(pageKey);
            cf.CFRelease(usageKey);
            return null;
        }
        cf.CFDictionarySetValue(dict, pageKey, pageNum);
        cf.CFDictionarySetValue(dict, usageKey, usageNum);
        cf.CFRelease(pageNum);
        cf.CFRelease(usageNum);
        cf.CFRelease(pageKey);
        cf.CFRelease(usageKey);
        return dict;
    }

    @Override
    public void centerCursor() {
    }

    @Override
    public void shutdown() {
        this.running = false;
        Pointer rl = this.runLoop;
        if (rl != null) {
            MacOSCoreFoundation.INSTANCE.CFRunLoopStop(rl);
        }
    }

    @Override
    public String platformName() {
        return "macOS IOHID";
    }
}

