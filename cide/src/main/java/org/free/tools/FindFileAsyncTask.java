package org.free.tools;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class FindFileAsyncTask extends AsyncTask<String, Object, String[]> {
    private final FindFileAsyncTask.Callback cb;

    public FindFileAsyncTask(FindFileAsyncTask.Callback cb) {
        this.cb = cb;
    }

    @Override
    protected String[] doInBackground(String... params) {
        String prog = "busybox find -name";
        for (String s : params) prog += " " + s;
        try {
            Process process = Runtime.getRuntime().exec("/system/bin/sh", cb.getEnv(), cb.fromDirectory());
            OutputStream stream = process.getOutputStream();
            stream.write(prog.getBytes());
            stream.close();
            ArrayList<String> out = new ArrayList<>();
            Thread thread = new MyThread(process.getInputStream(), out);
            Thread thread_ = new MyThread(process.getErrorStream(), out);
            thread_.start();
            thread.start();
            thread.join();
            thread_.join();
            process.waitFor();
            return out.toArray(new String[out.size()]);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return new String[0];
    }

    @Override
    protected void onPostExecute(String[] strings) {
        cb.onResult(strings, true);
    }

    public interface Callback {
        File fromDirectory();

        String[] getEnv();

        void onResult(String[] strings, boolean addFile);
    }
}
