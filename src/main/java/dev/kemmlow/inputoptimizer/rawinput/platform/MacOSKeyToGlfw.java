/*
 * Decompiled with CFR 0.152.
 */
package dev.kemmlow.inputoptimizer.rawinput.platform;

public final class MacOSKeyToGlfw {
    private MacOSKeyToGlfw() {
    }

    public static int convert(int macKey) {
        return switch (macKey) {
            case 0 -> 65;
            case 11 -> 66;
            case 8 -> 67;
            case 2 -> 68;
            case 14 -> 69;
            case 3 -> 70;
            case 5 -> 71;
            case 4 -> 72;
            case 34 -> 73;
            case 38 -> 74;
            case 40 -> 75;
            case 37 -> 76;
            case 46 -> 77;
            case 45 -> 78;
            case 31 -> 79;
            case 35 -> 80;
            case 12 -> 81;
            case 15 -> 82;
            case 1 -> 83;
            case 17 -> 84;
            case 32 -> 85;
            case 9 -> 86;
            case 13 -> 87;
            case 7 -> 88;
            case 16 -> 89;
            case 6 -> 90;
            case 29 -> 48;
            case 18 -> 49;
            case 19 -> 50;
            case 20 -> 51;
            case 21 -> 52;
            case 23 -> 53;
            case 22 -> 54;
            case 26 -> 55;
            case 28 -> 56;
            case 25 -> 57;
            case 49 -> 32;
            case 36 -> 257;
            case 53 -> 256;
            case 51 -> 259;
            case 48 -> 258;
            case 56 -> 340;
            case 60 -> 344;
            case 59 -> 341;
            case 62 -> 345;
            case 58 -> 342;
            case 61 -> 346;
            case 55 -> 343;
            case 54 -> 347;
            case 57 -> 280;
            case 123 -> 263;
            case 126 -> 265;
            case 124 -> 262;
            case 125 -> 264;
            case 114 -> 260;
            case 117 -> 261;
            case 115 -> 268;
            case 119 -> 269;
            case 116 -> 266;
            case 121 -> 267;
            case 122 -> 290;
            case 120 -> 291;
            case 99 -> 292;
            case 118 -> 293;
            case 96 -> 294;
            case 97 -> 295;
            case 98 -> 296;
            case 100 -> 297;
            case 101 -> 298;
            case 109 -> 299;
            case 103 -> 300;
            case 111 -> 301;
            case 24 -> 61;
            case 27 -> 45;
            case 33 -> 91;
            case 30 -> 93;
            case 42 -> 92;
            case 41 -> 59;
            case 39 -> 39;
            case 43 -> 44;
            case 47 -> 46;
            case 44 -> 47;
            case 50 -> 96;
            default -> -1;
        };
    }
}

