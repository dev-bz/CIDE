package org.free.tools;

import org.free.cide.tasks.MakeFileAsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FileUtils {
    public static void find(FindFileAsyncTask.Callback cb, String... p) {
        new FindFileAsyncTask(cb).execute(p);
    }

    public static void make(MakeFileAsyncTask.MakeCallback cb, int code, String... p) {
        new MakeFileAsyncTask(cb, code).execute(p);
    }

    public static String stringFromFile(String fileName) {
        File file = new File(fileName);
        int len = (int) file.length(), r, offset = 0;
        byte[] b = new byte[len];
        try {
            FileInputStream f = new FileInputStream(file);
            while ((r = f.read(b, offset, len)) > 0) {
                offset += r;
                len -= offset;
            }
            return new String(b);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
}
