package org.free.cide.utils;

public class Util {
    public static final String EMPTY_STRING = new String(new char[0]);

    public static boolean isOpenable(String name) {
        return name.matches("(.*\\.(c|cc|cpp|cxx|h|hpp|hxx|inc|i|txt))|[Mm]ake[Ff]ile");
    }
}
