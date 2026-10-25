/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.Window
 *  net.minecraft.client.MouseHandler
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package dev.kemmlow.inputoptimizer.mixin;

import com.mojang.blaze3d.platform.Window;
import dev.kemmlow.inputoptimizer.Main;
import dev.kemmlow.inputoptimizer.rawinput.RawInputManager;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={MouseHandler.class})
public class MixinMouseSetup {
    @Inject(method={"setup"}, at={@At(value="TAIL")})
    private void initializeRawInput(Window window, CallbackInfo ci) {
        if (Main.externalRawInputPresent) {
            return;
        }
        RawInputManager.initialize(window.handle());
    }
}

