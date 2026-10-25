package dev.kemmlow.inputoptimizer;

public final class TickGate {
    private static boolean continueAttackThisTick = false;

    public static boolean tryAttack() {
        return true;
    }

    public static boolean tryContinueAttack() {
        if (continueAttackThisTick) {
            return false;
        }
        continueAttackThisTick = true;
        return true;
    }

    public static boolean tryUse() {
        return true;
    }

    public static void reset() {
        continueAttackThisTick = false;
    }
}
