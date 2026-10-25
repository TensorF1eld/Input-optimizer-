/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.kemmlow.inputoptimizer.mixin;

import dev.kemmlow.inputoptimizer.TickGate;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Minecraft.class})
public class MixinUseGate {
    @Inject(method={"startUseItem()V"}, at={@At(value="HEAD")}, cancellable=true)
    private void gateStartUseItem(CallbackInfo ci) {
        if (!TickGate.tryUse()) {
            ci.cancel();
        }
    }
}

