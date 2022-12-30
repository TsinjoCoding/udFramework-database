package com.udframework.database.generic.utils;

public class StringUtils {

    public static String wrap(String col) {
        return String.format("\"%s\"", col);
    }

    public static String lPad(int len, String value, char padChar) {
        return pad(len, value, padChar, true);
    }

    private static String pad(int len, String value, char padChar, boolean lPad) {
        len -= value.length();
        String valuePad = repeat(padChar, Math.max(0, len));
        return lPad ? valuePad + value : value + valuePad;
    }

    private static String repeat (char c, int count) {
        if (count <= 0) return "";
        StringBuilder val = new StringBuilder("" + c);
        for (int i = 1; i < count; i++) {
            val.append(c);
        }
        return val.toString();
    }

    public static String rPad(int len, String value, char padChar) {
        return pad(len, value, padChar, false);
    }

}
