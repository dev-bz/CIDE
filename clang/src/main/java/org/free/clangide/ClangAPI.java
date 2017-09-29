package org.free.clangide;

public class ClangAPI {
    static {
        System.loadLibrary("clang");
        System.loadLibrary("index");
    }

    synchronized static public native boolean bHasFile(String file, boolean setCurrent);

    synchronized static public native String get(int id);

    synchronized static public native void putenv(String env);

    synchronized static public native int set(String filterString, boolean takeCodeComplete);

    synchronized static public native void updateFileCode(String file, String code, boolean main);

    synchronized static public native void updateOption(String opt[]);

    synchronized static public native String updatePosition(int line, int column);
}