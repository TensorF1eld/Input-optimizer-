package dev.kemmlow.inputoptimizer.rawinput.platform;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.ptr.IntByReference;
import dev.kemmlow.inputoptimizer.Main;
import dev.kemmlow.inputoptimizer.rawinput.RawInputEngine;
import dev.kemmlow.inputoptimizer.rawinput.RawInputEvent;
import dev.kemmlow.inputoptimizer.rawinput.native_bindings.WindowsUser32Ex;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WindowsRawInputEngine extends RawInputEngine {
    private static final int WM_INPUT = 0xff, WM_CLOSE = 0x10, RID_INPUT = 0x10000003;
    private static final int HEADER_SIZE = WindowsInputDecoder.HEADER_SIZE;
    private static final int BUFFER_SIZE = 8192;
    private final Memory current = new Memory(BUFFER_SIZE);
    private final Memory batch = new Memory(BUFFER_SIZE);
    private final IntByReference size = new IntByReference(BUFFER_SIZE);
    private final WindowsInputDecoder decoder = new WindowsInputDecoder();
    private final Consumer<RawInputEvent> output = eventQueue::add;
    private final CountDownLatch ready = new CountDownLatch(1);
    private volatile WinDef.HWND target;
    private volatile boolean started;
    private volatile boolean resetKeys;
    private volatile int centerX, centerY;
    private String windowClass;

    @Override public boolean initialize(long glfwWindow) {
        windowClass = "inputoptimizer_Win32_" + glfwWindow;
        running = true;
        Thread thread = new Thread(this::messageLoop, "inputoptimizer-Win32-Raw");
        thread.setPriority(Thread.NORM_PRIORITY + 1);
        thread.setDaemon(true); thread.start();
        try {
            if (!ready.await(2, TimeUnit.SECONDS) || !started) { shutdown(); return false; }
            return true;
        } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); shutdown(); return false; }
    }
    private void messageLoop() {
        WinDef.HMODULE module = Kernel32.INSTANCE.GetModuleHandle(null);
        WinUser.WNDCLASSEX window = new WinUser.WNDCLASSEX();
        window.lpfnWndProc = (WinUser.WindowProc) (hwnd, message, wParam, lParam) -> {
            if (message == WM_INPUT) {
                long received = System.nanoTime();
                // GetMessage already removed this message from the buffered queue.
                // Read its lParam first, then drain any remaining messages in order.
                size.setValue(BUFFER_SIZE);
                int bytes = WindowsUser32Ex.INSTANCE.GetRawInputData(Pointer.createConstant(lParam.longValue()),
                        RID_INPUT, current, size, HEADER_SIZE);
                if (bytes >= HEADER_SIZE && bytes <= BUFFER_SIZE) decode(current, 0, bytes, received);
                drainBuffer();
            } else if (message == WM_CLOSE) {
                running = false;
                return new WinDef.LRESULT(0);
            }
            return User32.INSTANCE.DefWindowProc(hwnd, message, wParam, lParam);
        };
        window.lpszClassName = windowClass;
        window.hInstance = module;
        window.cbSize = window.size();
        boolean registered = false;
        try {
            if (User32.INSTANCE.RegisterClassEx(window).intValue() == 0) throw new IllegalStateException("RegisterClassEx failed");
            target = User32.INSTANCE.CreateWindowEx(0, windowClass, "Input Optimizer", 0, 0, 0, 0, 0,
                    null, null, (WinDef.HINSTANCE) module, null);
            if (target == null || Pointer.nativeValue(target.getPointer()) == 0) throw new IllegalStateException("CreateWindowEx failed");
            register(false); registered = true;
            decoder.setLocks((WindowsUser32Ex.INSTANCE.GetKeyState(20) & 1) != 0, (WindowsUser32Ex.INSTANCE.GetKeyState(144) & 1) != 0);
            started = true; ready.countDown();
            WinUser.MSG message = new WinUser.MSG();
            while (running && User32.INSTANCE.GetMessage(message, null, 0, 0) > 0) {
                User32.INSTANCE.TranslateMessage(message); User32.INSTANCE.DispatchMessage(message);
            }
        } catch (Throwable failure) { Main.LOGGER.warn("[Input Optimizer] Windows raw input stopped; normal input remains available", failure); }
        finally {
            running = false; started = false; ready.countDown();
            if (registered) register(true);
            if (target != null) User32.INSTANCE.DestroyWindow(target);
            User32.INSTANCE.UnregisterClass(windowClass, (WinDef.HINSTANCE) module);
            current.close(); batch.close();
        }
    }
    private void decode(Pointer p, int offset, int bytes, long timestamp) {
        if (resetKeys) { decoder.resetHeld(); resetKeys = false; }
        if (!focused) return;
        decoder.decode(p, offset, bytes, timestamp, captureKeyboard, captureMouse, gameFocused, output);
        if (captureMouse && gameFocused && bytes >= HEADER_SIZE + 24 && p.getInt(offset) == 0
                && (p.getInt(offset + HEADER_SIZE + 12L) != 0 || p.getInt(offset + HEADER_SIZE + 16L) != 0)) centerCursor();
    }
    private void drainBuffer() {
        while (running) {
            size.setValue(BUFFER_SIZE);
            int count = WindowsUser32Ex.INSTANCE.GetRawInputBuffer(batch, size, HEADER_SIZE);
            if (count <= 0) return;
            int offset = 0;
            for (int i = 0; i < count; i++) {
                if (offset + HEADER_SIZE > BUFFER_SIZE) return;
                int bytes = batch.getInt(offset + 4L);
                if (bytes < HEADER_SIZE || bytes > BUFFER_SIZE - offset) return;
                decode(batch, offset, bytes, System.nanoTime());
                offset += (bytes + Native.POINTER_SIZE - 1) & ~(Native.POINTER_SIZE - 1);
            }
        }
    }
    private void register(boolean remove) {
        int count = (captureMouse ? 1 : 0) + (captureKeyboard ? 1 : 0);
        if (count == 0) return;
        WindowsUser32Ex.RAWINPUTDEVICE[] devices = (WindowsUser32Ex.RAWINPUTDEVICE[])
                new WindowsUser32Ex.RAWINPUTDEVICE().toArray(count);
        int index = 0;
        if (captureMouse) setDevice(devices[index++], 2, remove);
        if (captureKeyboard) setDevice(devices[index], 6, remove);
        if (!WindowsUser32Ex.INSTANCE.RegisterRawInputDevices(devices, count, devices[0].size()) && !remove)
            throw new IllegalStateException("RegisterRawInputDevices failed: " + Kernel32.INSTANCE.GetLastError());
    }
    private void setDevice(WindowsUser32Ex.RAWINPUTDEVICE device, int usage, boolean remove) {
        device.usUsagePage = 1; device.usUsage = (short) usage;
        device.dwFlags = remove ? 1 : 0x100;
        device.hwndTarget = remove ? null : target;
    }
    @Override public void setFocused(boolean value) {
        if (focused != value) resetKeys = true;
        super.setFocused(value);
    }
    public void updateCenter(int x, int y) { centerX = x; centerY = y; }
    @Override public void centerCursor() { if (centerX != 0) User32.INSTANCE.SetCursorPos(centerX, centerY); }
    @Override public void shutdown() {
        running = false;
        WinDef.HWND hwnd = target;
        if (hwnd != null) User32.INSTANCE.PostMessage(hwnd, WM_CLOSE, new WinDef.WPARAM(0), new WinDef.LPARAM(0));
    }
    @Override public String platformName() { return "Windows Raw"; }
}
