/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.fabricmc.api.ClientModInitializer
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 */
package dev.kemmlow.inputoptimizer;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main
implements ClientModInitializer {
    public static final String MOD_ID = "inputoptimizer";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"inputoptimizer");
    public static boolean externalRawInputPresent = false;

    public void onInitializeClient() {
        InputOptimizerConfig.load();
        dev.kemmlow.inputoptimizer.rawinput.RawInputManager.configure(InputOptimizerConfig.get());
        InputLatencyProfiler.registerCommands();
        LOGGER.info("[Input Optimizer] Loaded.");
    }
}
