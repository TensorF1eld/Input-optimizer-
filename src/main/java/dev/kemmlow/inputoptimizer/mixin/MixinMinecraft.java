package dev.kemmlow.inputoptimizer.mixin;

import dev.kemmlow.inputoptimizer.TickGate;
import dev.kemmlow.inputoptimizer.rawinput.RawInputManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Minecraft.class})
public class MixinMinecraft {
    @Inject(method={"tick"}, at={@At(value="HEAD")})
    private void tickHead(CallbackInfo ci) {
        TickGate.reset();
        dev.kemmlow.inputoptimizer.InputLatencyProfiler.tick();
        if (RawInputManager.isActive()) {
            RawInputManager.tick();
        }
    }
}
