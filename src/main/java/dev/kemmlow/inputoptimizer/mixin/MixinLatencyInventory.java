package dev.kemmlow.inputoptimizer.mixin;

import dev.kemmlow.inputoptimizer.InputLatencyProfiler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class MixinLatencyInventory {
    @Inject(method = "setSelectedSlot", at = @At("HEAD"))
    private void measureSlot(int slot, CallbackInfo ci) {
        if (!InputLatencyProfiler.enabled() || slot < 0 || slot >= 9) return;
        Minecraft c = Minecraft.getInstance();
        if (c.player != null && c.player.getInventory() == (Object) this) InputLatencyProfiler.action(slot + 2);
    }
}
