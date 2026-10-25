package dev.kemmlow.inputoptimizer.mixin;

import dev.kemmlow.inputoptimizer.InputDispatchManager;
import dev.kemmlow.inputoptimizer.InputLatencyProfiler;
import dev.kemmlow.inputoptimizer.rawinput.RawMoveEvent;
import dev.kemmlow.inputoptimizer.rawinput.OrderedMouseAccess;
import dev.kemmlow.inputoptimizer.rawinput.RawButtonEvent;
import dev.kemmlow.inputoptimizer.rawinput.RawInputManager;
import dev.kemmlow.inputoptimizer.rawinput.RawInputEvent;
import dev.kemmlow.inputoptimizer.rawinput.RawInputReplay;
import dev.kemmlow.inputoptimizer.rawinput.RawKeyEvent;
import dev.kemmlow.inputoptimizer.rawinput.RawScrollEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={Minecraft.class})
public class MixinInputDrain {
    @Inject(method={"runTick"}, at={@At(value="HEAD")})
    private void ultraDrain(boolean advanceGameTime, CallbackInfo ci) {
        if (!RawInputManager.isActive()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.getWindow() == null) {
            return;
        }

        // Keep engine focus and screen states perfectly frame-synchronized
        RawInputManager.syncFocusState(client);

        RawInputReplay.drain(RawInputManager.getEngine(), RawInputManager.replaySuppression(),
                event -> inputoptimizer$replay(client, event), InputDispatchManager::dispatch);
    }

    @Unique
    private static void inputoptimizer$replay(Minecraft client, RawInputEvent event) {
        InputLatencyProfiler.rawQueue(event.timestamp());
        if (event instanceof RawMoveEvent move) {
            if (client.screen == null && client.mouseHandler.isMouseGrabbed() && client.player != null) {
                ((OrderedMouseAccess) client.mouseHandler).inputoptimizer$move(move.dx(), move.dy());
            }
            return;
        }
        if (client.screen == null && client.player != null && client.level != null
                && client.mouseHandler.isMouseGrabbed()) client.gameRenderer.pick(1.0F);
        if (event instanceof RawKeyEvent kEvent) {
            ((MixinKeyboardHandlerInvoker)client.keyboardHandler).invokeKeyPress(
                client.getWindow().handle(),
                kEvent.action,
                new KeyEvent(kEvent.key, kEvent.scancode, kEvent.modifiers)
            );
            RawInputManager.markKeyConsumed(kEvent.key, kEvent.action);
        } else if (event instanceof RawButtonEvent bEvent) {
            ((MixinMouseHandlerInvoker)client.mouseHandler).invokeOnButton(
                client.getWindow().handle(),
                new MouseButtonInfo(bEvent.button, bEvent.modifiers),
                bEvent.action
            );
            RawInputManager.markButtonConsumed(bEvent.button, bEvent.action);
        } else if (event instanceof RawScrollEvent sEvent) {
            ((MixinMouseHandlerInvoker)client.mouseHandler).invokeOnScroll(
                client.getWindow().handle(),
                sEvent.horizontal,
                sEvent.vertical
            );
            RawInputManager.markScrollConsumed(sEvent.horizontal, sEvent.vertical);
        }
    }
}
