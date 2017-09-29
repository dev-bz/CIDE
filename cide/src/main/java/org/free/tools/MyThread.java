package org.free.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

class MyThread extends Thread {
    private final ArrayList<String> out;
    private final InputStream process;

    public MyThread(InputStream process, ArrayList<String> out) {
        this.process = process;
        this.out = out;
    }

    @Override
    public void run() {
        BufferedReader r = new BufferedReader(new InputStreamReader(process));
        try {
            String l;
            while (null != (l = r.readLine())) out.add(l);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
