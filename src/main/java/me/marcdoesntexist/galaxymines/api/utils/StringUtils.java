package me.marcdoesntexist.galaxymines.api.utils;

public final class StringUtils {
    private StringUtils() {
    }

    public static String arrayToMessage(final String[] array) {
        return arrayToMessage(0, array);
    }

    public static String secondsToString(final int pTime) {
        return String.format("%02d:%02d", pTime / 60, pTime % 60);
    }

    public static String arrayToMessage(final int start, final String[] array) {
        if (start >= array.length) {
            return "";
        }
        return String.join(" ", java.util.Arrays.copyOfRange(array, start, array.length));
    }
}
