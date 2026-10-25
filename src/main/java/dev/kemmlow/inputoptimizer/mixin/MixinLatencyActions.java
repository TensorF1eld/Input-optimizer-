package dev.kemmlow.inputoptimizer.mixin;

import dev.kemmlow.inputoptimizer.InputLatencyProfiler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public class MixinLatencyActions {
    @Inject(method = "startAttack", at = @At("HEAD"))
    private void measureAttack(CallbackInfoReturnable<Boolean> cir) { InputLatencyProfiler.action(0); }
    @Inject(method = "startUseItem", at = @At("HEAD"))
    private void measureUse(CallbackInfo ci) { InputLatencyProfiler.action(1); }
    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void clearUnusedActionSamples(CallbackInfo ci) { InputLatencyProfiler.endDispatch(); }
}
