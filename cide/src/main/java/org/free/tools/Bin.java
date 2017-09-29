package org.free.tools;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import com.myopicmobile.textwarrior.TextWarriorApplication;

import org.free.cide.ide.Main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Bin {
    private static Context ctx;

    /**
     * public static String getClang() {
     * return new File(ctx.getFilesDir(), "bin/clang").toString();
     * }
     */
    public static String getClangFormat() {
        return new File(ctx.getApplicationInfo().nativeLibraryDir + "/libclang-format.so").toString();
    }

    public static File getBusybox() {
        return new File(ctx.getFilesDir(), "busybox");
    }

    /*public static String getPty(){
      File dataDir =ctx.getFilesDir();
      File binDir = new File(dataDir,"bin");
      File binary = new File(binDir,"execpty");
      return binary.toString();
    }*/
    public static void setupBinDir(Context ctx) {
        Bin.ctx = ctx;
        File dataDir = ctx.getFilesDir();
        File binDir = new File(dataDir, "bin");
        if (!binDir.exists()) {
            if (binDir.mkdir()) {
                try {
                    chmod("755", binDir.getAbsolutePath());
                } catch (Exception ignored) {
                }
            }
        }
        //readyBinFromAssets(binDir, "clang-format", true, true);
        readyBinFromAssets(dataDir, "busybox", true, true);
    }

    private static void chmod(String... args) throws IOException {
        String[] cmdline = new String[args.length + 1];
        cmdline[0] = "/system/bin/chmod";
        System.arraycopy(args, 0, cmdline, 1, args.length);
        new ProcessBuilder(cmdline).start();
    }

    private static void readyBinFromAssets(File binDir, String v, boolean hasArch, boolean reload) {
        File binary = new File(binDir, v);
        if (reload && binary.exists()) {
            long binTime = binary.lastModified();
            long pkgTime = new File(ctx.getPackageCodePath()).lastModified();
            if (binTime > pkgTime)
                return;
        }
        String arch = getArch();
        if (hasArch)
            v = v + "-" + arch;
        AssetManager assets = ctx.getAssets();
        try {
            InputStream src = assets.open(v);
            FileOutputStream dst = new FileOutputStream(binary);
            copyStream(dst, src);
            chmod("755", binary.getAbsolutePath());
            Log.e(TextWarriorApplication.TAG, "update:" + v);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String getArch() {
        Main.readyArch();
        return Main.arch;
    }

    private static void copyStream(OutputStream dst, InputStream src) throws IOException {
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = src.read(buffer)) >= 0) {
            dst.write(buffer, 0, bytesRead);
        }
        dst.close();
    }
}
