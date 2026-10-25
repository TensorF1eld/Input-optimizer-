/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MouseHandler
 *  net.minecraft.client.input.MouseButtonInfo
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
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={MouseHandler.class})
public class MixinMouseHandler {
    @Unique private long inputoptimizer$callbackStart = LatencyMeasurements.NO_SAMPLE;
    @Inject(method={"onButton"}, at={@At(value="HEAD")}, cancellable=true)
    private void interceptDuplicateButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        inputoptimizer$callbackStart = LatencyMeasurements.NO_SAMPLE;
        if (!RawInputManager.replaySuppression().isReplaying()
                && (RawInputManager.windowsOwnsMouse() && buttonInfo.button() < 5
                || RawInputManager.isRawMouseActive() && RawInputManager.consumeIfButtonMarked(buttonInfo.button(), action))) {
            ci.cancel();
            return;
        }
        inputoptimizer$callbackStart = InputLatencyProfiler.beginCallback();
        InputLatencyProfiler.mousePress(buttonInfo, action, inputoptimizer$callbackStart);
    }

    @Inject(method={"onButton"}, at={@At(value="TAIL")})
    private void onMouseButtonDispatch(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        if (RawInputManager.replaySuppression().isReplaying() || action != 1) {
            return;
        }
        InputDispatchManager.dispatch();
    }

    @Inject(method={"onScroll"}, at={@At(value="TAIL")})
    private void onScrollDispatch(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (RawInputManager.replaySuppression().isReplaying() || (vertical == 0.0 && horizontal == 0.0)) {
            return;
        }
        InputDispatchManager.dispatch();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void interceptDuplicateScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (!RawInputManager.replaySuppression().isReplaying()
                && (RawInputManager.windowsOwnsMouse()
                || RawInputManager.isRawMouseActive() && RawInputManager.consumeIfScrollMarked(horizontal, vertical))) {
            ci.cancel();
        }
    }
    @Inject(method = "onButton", at = @At("RETURN"))
    private void measureCallback(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        InputLatencyProfiler.endCallback(inputoptimizer$callbackStart);
        inputoptimizer$callbackStart = LatencyMeasurements.NO_SAMPLE;
    }
    @Inject(method = "onMove", at = @At("HEAD"), cancellable = true)
    private void useOrderedRawMovement(long window, double x, double y, CallbackInfo ci) {
        Minecraft c = Minecraft.getInstance();
        if (RawInputManager.isRawMouseActive() && c.screen == null && c.mouseHandler.isMouseGrabbed()) ci.cancel();
    }
}
