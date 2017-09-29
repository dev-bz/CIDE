package org.free.tools;

import org.free.cide.tasks.MakeFileAsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MyMakeThread extends Thread {
    private final StringBuilder out;
    private final InputStream process;
    private final MakeFileAsyncTask task;

    public MyMakeThread(InputStream process, StringBuilder out, MakeFileAsyncTask makeFileAsyncTask) {
        this.process = process;
        this.out = out;
        this.task = makeFileAsyncTask;
    }

    @Override
    public void run() {
        BufferedReader r = new BufferedReader(new InputStreamReader(process));
        try {
            String l;
            boolean noFirst = false;
            while (null != (l = r.readLine())) {
                if (noFirst) out.append('\n');
                else noFirst = true;
                out.append(l);
                task.lineMessage(l);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
