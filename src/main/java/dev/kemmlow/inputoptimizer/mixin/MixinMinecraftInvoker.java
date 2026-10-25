package dev.kemmlow.inputoptimizer.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value={Minecraft.class})
public interface MixinMinecraftInvoker {
    @Invoker(value="handleKeybinds")
    public void invokeHandleKeybinds();

    @Invoker(value="startAttack")
    public boolean invokeStartAttack();

    @Invoker(value="startUseItem")
    public void invokeStartUseItem();
}
