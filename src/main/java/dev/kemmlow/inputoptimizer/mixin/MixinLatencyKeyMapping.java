package dev.kemmlow.inputoptimizer.mixin;

import dev.kemmlow.inputoptimizer.InputLatencyProfiler;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public class MixinLatencyKeyMapping {
    @Inject(method = "click", at = @At("HEAD"))
    private static void measureAcceptedClick(InputConstants.Key key, CallbackInfo ci) { InputLatencyProfiler.clicked(key); }
    @Inject(method = "consumeClick", at = @At("RETURN"))
    private void measureConsumption(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) InputLatencyProfiler.consumed((KeyMapping) (Object) this);
    }
}
