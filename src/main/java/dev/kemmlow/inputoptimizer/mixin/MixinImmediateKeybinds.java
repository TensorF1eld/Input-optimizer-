package dev.kemmlow.inputoptimizer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.kemmlow.inputoptimizer.InputDispatchManager;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinImmediateKeybinds {
    // Only the chat/command consumeClick calls lie between these fields in 1.21.11.
    // Leave their clicks pending until the normal tick, after the opening key's
    // character callback. Opening chat here would insert that character a second time.
    @WrapOperation(
            method = "handleKeybinds",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/KeyMapping;consumeClick()Z"),
            slice = @Slice(
                    from = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyChat:Lnet/minecraft/client/KeyMapping;"),
                    to = @At(value = "FIELD", target = "Lnet/minecraft/client/Options;keyUse:Lnet/minecraft/client/KeyMapping;")
            ),
            require = 2,
            allow = 2
    )
    private boolean inputoptimizer$deferChatOpening(KeyMapping keyMapping, Operation<Boolean> original) {
        if (InputDispatchManager.isDispatching()) {
            return false;
        }
        return original.call(keyMapping);
    }

    // In 1.21.11, isDown ordinal 3 starts the held-use/continue-attack tail.
    // Fresh consumeClick actions and the item-release check precede this point.
    @Inject(method = "handleKeybinds", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;isDown()Z", ordinal = 3), cancellable = true)
    private void inputoptimizer$leaveHeldRepeatsToTicks(CallbackInfo ci) {
        if (InputDispatchManager.isDispatching()) {
            ci.cancel();
        }
    }
}
