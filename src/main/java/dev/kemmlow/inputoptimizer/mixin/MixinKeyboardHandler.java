/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.KeyboardHandler
 *  net.minecraft.client.input.KeyEvent
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.kemmlow.inputoptimizer.mixin;

import dev.kemmlow.inputoptimizer.InputDispatchManager;
import dev.kemmlow.inputoptimizer.InputLatencyProfiler;
import dev.kemmlow.inputoptimizer.LatencyMeasurements;
import dev.kemmlow.inputoptimizer.rawinput.RawInputManager;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={KeyboardHandler.class})
public class MixinKeyboardHandler {
    @Unique private long inputoptimizer$callbackStart = LatencyMeasurements.NO_SAMPLE;
    @Inject(method={"keyPress"}, at={@At(value="HEAD")}, cancellable=true)
    private void interceptDuplicate(long window, int action, KeyEvent input, CallbackInfo ci) {
        inputoptimizer$callbackStart = LatencyMeasurements.NO_SAMPLE;
        if (!RawInputManager.replaySuppression().isReplaying()
                && (RawInputManager.windowsOwnsKeyboard(input.key())
                || RawInputManager.isRawKeyboardActive() && RawInputManager.consumeIfKeyMarked(input.key(), action))) {
            ci.cancel();
            return;
        }
        inputoptimizer$callbackStart = InputLatencyProfiler.beginCallback();
        InputLatencyProfiler.keyPress(input, action, inputoptimizer$callbackStart);
    }

    @Inject(method={"keyPress"}, at={@At(value="TAIL")})
    private void dispatchOnKey(long window, int action, KeyEvent input, CallbackInfo ci) {
        if (RawInputManager.replaySuppression().isReplaying() || action == 0) {
            return;
        }
        InputDispatchManager.dispatch();
    }
    @Inject(method = "keyPress", at = @At("RETURN"))
    private void measureCallback(long window, int action, KeyEvent input, CallbackInfo ci) {
        InputLatencyProfiler.endCallback(inputoptimizer$callbackStart);
        inputoptimizer$callbackStart = LatencyMeasurements.NO_SAMPLE;
    }
}
