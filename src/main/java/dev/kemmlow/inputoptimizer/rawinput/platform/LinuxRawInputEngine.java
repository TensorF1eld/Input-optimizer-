/*
 * Decompiled with CFR 0.152.
 */
package dev.kemmlow.inputoptimizer.rawinput.platform;

import dev.kemmlow.inputoptimizer.rawinput.RawButtonEvent;
import dev.kemmlow.inputoptimizer.rawinput.RawInputEngine;
import dev.kemmlow.inputoptimizer.rawinput.RawKeyEvent;
import dev.kemmlow.inputoptimizer.rawinput.RawScrollEvent;
import dev.kemmlow.inputoptimizer.rawinput.native_bindings.LinuxEvdev;
import dev.kemmlow.inputoptimizer.rawinput.platform.LinuxKeyToGlfw;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class LinuxRawInputEngine
extends RawInputEngine {
    private final List<Integer> fds = new ArrayList<Integer>();

    @Override
    public boolean initialize(long glfwWindow) {
        File inputDir = new File("/dev/input");
        File[] devices = inputDir.listFiles((dir, name) -> name.startsWith("event"));
        if (devices == null) {
            return false;
        }
        for (File dev : devices) {
            int fd = LinuxEvdev.INSTANCE.open(dev.getAbsolutePath(), 2048);
            if (fd < 0) continue;
            this.fds.add(fd);
        }
        if (this.fds.isEmpty()) {
            return false;
        }
        this.running = true;
        Thread t = new Thread(this::readLoop, "inputoptimizer-Linux-evdev");
        t.setPriority(10);
        t.setDaemon(true);
        t.start();
        return true;
    }

    private void readLoop() {
        byte[] buffer = new byte[1536];
        while (this.running) {
            boolean readAny = false;
            for (int fd : this.fds) {
                int bytes = LinuxEvdev.INSTANCE.read(fd, buffer, buffer.length);
                if (bytes <= 0 || !this.focused) continue;
                readAny = true;
                this.processEvents(buffer, bytes);
            }
            if (readAny) continue;
            try {
                Thread.sleep(0L, 500000);
            }
            catch (Exception exception) {}
        }
    }

    private void processEvents(byte[] data, int bytes) {
        ByteBuffer bb = ByteBuffer.wrap(data, 0, bytes).order(ByteOrder.nativeOrder());
        while (bb.remaining() >= 24) {
            bb.getLong();
            bb.getLong();
            short type = bb.getShort();
            short code = bb.getShort();
            int val = bb.getInt();
            long now = System.nanoTime();
            if (type == 2 && captureMouse) {
                if (code == 0) {
                    this.queueMovement(val, 0.0, now);
                    continue;
                }
                if (code == 1) {
                    this.queueMovement(0.0, val, now);
                    continue;
                }
                if (code == 8) {
                    this.eventQueue.add(new RawScrollEvent(0.0, val, now));
                    continue;
                }
                if (code != 6) continue;
                this.eventQueue.add(new RawScrollEvent(val, 0.0, now));
                continue;
            }
            if (type != 1 || val == 2) continue;
            if (code >= 272 && code <= 276) {
                if (captureMouse) this.eventQueue.add(new RawButtonEvent(code - 272, val, 0, now));
                continue;
            }
            if (!captureKeyboard) continue;
            int glfw = LinuxKeyToGlfw.convert(code);
            if (glfw == -1) continue;
            this.eventQueue.add(new RawKeyEvent(glfw, code, val, 0, now));
        }
    }

    @Override
    public void centerCursor() {
    }

    @Override
    public String platformName() {
        return "Linux evdev";
    }

    @Override
    public void shutdown() {
        this.running = false;
        for (int fd : this.fds) {
            LinuxEvdev.INSTANCE.close(fd);
        }
        this.fds.clear();
    }
}

