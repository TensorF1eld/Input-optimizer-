package dev.kemmlow.inputoptimizer.mixin;

import dev.kemmlow.inputoptimizer.rawinput.OrderedMouseAccess;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value={MouseHandler.class})
public abstract class MixinMouseAccumulated implements OrderedMouseAccess {
    @Shadow
    private double accumulatedDX;
    @Shadow
    private double accumulatedDY;
    @Shadow public abstract void handleAccumulatedMovement();
    @Override @Unique public void inputoptimizer$move(double dx, double dy) {
        accumulatedDX += dx; accumulatedDY += dy;
        handleAccumulatedMovement();
    }
}
