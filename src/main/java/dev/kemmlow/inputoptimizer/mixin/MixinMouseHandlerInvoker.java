/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.MouseHandler
 *  net.minecraft.client.input.MouseButtonInfo
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Invoker
 */
package dev.kemmlow.inputoptimizer.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={MouseHandler.class})
public interface MixinMouseHandlerInvoker {
    @Invoker(value="onButton")
    public void invokeOnButton(long var1, MouseButtonInfo var3, int var4);

    @Invoker(value="onScroll")
    public void invokeOnScroll(long var1, double var3, double var5);
}

