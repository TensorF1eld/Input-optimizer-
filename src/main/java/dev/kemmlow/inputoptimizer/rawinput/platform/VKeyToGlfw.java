/*
 * Decompiled with CFR 0.152.
 */
package dev.kemmlow.inputoptimizer.rawinput.platform;

public final class VKeyToGlfw {
    private VKeyToGlfw() {
    }

    public static int convert(short vkey) {
        return switch (vkey & 0xFFFF) {
            case 65 -> 65;
            case 66 -> 66;
            case 67 -> 67;
            case 68 -> 68;
            case 69 -> 69;
            case 70 -> 70;
            case 71 -> 71;
            case 72 -> 72;
            case 73 -> 73;
            case 74 -> 74;
            case 75 -> 75;
            case 76 -> 76;
            case 77 -> 77;
            case 78 -> 78;
            case 79 -> 79;
            case 80 -> 80;
            case 81 -> 81;
            case 82 -> 82;
            case 83 -> 83;
            case 84 -> 84;
            case 85 -> 85;
            case 86 -> 86;
            case 87 -> 87;
            case 88 -> 88;
            case 89 -> 89;
            case 90 -> 90;
            case 48 -> 48;
            case 49 -> 49;
            case 50 -> 50;
            case 51 -> 51;
            case 52 -> 52;
            case 53 -> 53;
            case 54 -> 54;
            case 55 -> 55;
            case 56 -> 56;
            case 57 -> 57;
            case 32 -> 32;
            case 13 -> 257;
            case 27 -> 256;
            case 8 -> 259;
            case 9 -> 258;
            case 16 -> 340;
            case 17 -> 341;
            case 18 -> 342;
            case 20 -> 280;
            case 37 -> 263;
            case 38 -> 265;
            case 39 -> 262;
            case 40 -> 264;
            case 45 -> 260;
            case 46 -> 261;
            case 36 -> 268;
            case 35 -> 269;
            case 33 -> 266;
            case 34 -> 267;
            case 112 -> 290;
            case 113 -> 291;
            case 114 -> 292;
            case 115 -> 293;
            case 116 -> 294;
            case 117 -> 295;
            case 118 -> 296;
            case 119 -> 297;
            case 120 -> 298;
            case 121 -> 299;
            case 122 -> 300;
            case 123 -> 301;
            case 187 -> 61;
            case 189 -> 45;
            case 219 -> 91;
            case 221 -> 93;
            case 220 -> 92;
            case 186 -> 59;
            case 222 -> 39;
            case 188 -> 44;
            case 190 -> 46;
            case 191 -> 47;
            case 192 -> 96;
            case 144 -> 282;
            case 19 -> 284;
            case 44 -> 283;
            case 93 -> 348;
            case 146 -> 336;
            case 145 -> 281;
            case 96, 97, 98, 99, 100, 101, 102, 103, 104, 105 -> 320 + (vkey & 0xffff) - 96;
            case 106 -> 332;
            case 107 -> 334;
            case 109 -> 333;
            case 110 -> 330;
            case 111 -> 331;
            case 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135 -> 302 + (vkey & 0xffff) - 124;
            default -> -1;
        };
    }
}
