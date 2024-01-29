package me.nologic.firesparks.utilities;

public class Colors {
    public static int[] fromString(String string) {

        final int[] colors = new int[3];
        final String hexString = string.replace("#", "");

        for (int i = 0; i < 3; i++) {
            colors[i] = Integer.parseInt(hexString.substring(i * 2, i * 2 + 2), 16);
        }
        return colors;
    }
}
