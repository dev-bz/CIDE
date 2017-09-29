package org.free.cide.tasks;

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import com.myopicmobile.textwarrior.TextWarriorApplication;

import org.free.cide.ide.Main;
import org.free.tools.MyMakeThread;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

public class MakeFileAsyncTask extends AsyncTask<String, String, Boolean> {
    private final MakeCallback cb;
    private final int code;
    private final String program;
    private final StringBuilder out = new StringBuilder();
    private final StringBuilder errOut = new StringBuilder();

    public MakeFileAsyncTask(@NonNull MakeCallback cb, int code) {
        this.cb = cb;
        this.code = code;
        if (code > 1) {
            this.program = "/system/bin/sh";
        } else
            this.program = "make -f - CC=@gcc CXX=@g++";
    }

    public void lineMessage(String l) {
        publishProgress(l);
    }

    @Override
    protected void onProgressUpdate(String... values) {
        cb.onProgressUpdate(values);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        String _program = program;
        if (code < 2) for (String s : params) _program += " " + s;
        String[] env = Main.readyCompileEnv(cb.getContext());
        for (String e : env) {
            Log.e(TextWarriorApplication.TAG, "env: " + e);
            if (code < 2 && e.startsWith("SHELL=")) _program += " \"" + e + "\"";
        }
        Log.e(TextWarriorApplication.TAG, "program:" + _program);
        try {
            Process process = Runtime.getRuntime().exec("sh", env, cb.fromDirectory());
            Thread thread = new MyMakeThread(process.getInputStream(), out, this);
            Thread thread_ = new MyMakeThread(process.getErrorStream(), errOut, this);
            thread.start();
            thread_.start();
            OutputStream stream = process.getOutputStream();
            stream.write(_program.getBytes());
            stream.write('\n');
            stream.flush();
            if (code == 2) stream.write(cb.getMakeCommand());
            else if (code == 3) stream.write(cb.getCleanCommand());
            else
                stream.write(("ifndef OUTPUT\n" +
                        "OUTPUT:=" + cb.getOutputName() + "\nendif\n" +
                        "RMObjects:= $(wildcard *.o)\n" +
                        "ifdef MORE\n  MoreBaseName:=$(sort $(basename $(MORE)))\nendif\n" +
                        "MoreObjects:=$(wildcard $(MoreBaseName:%=%.o) $(OUTPUT))\n" +
                        "ifdef MoreObjects\n" +
                        "  RMObjects+=$(MoreObjects)\nendif\n" +
                        "ifdef RMObjects\n" +
                        "  RMObjects:=@rm $(RMObjects)\nendif\n" +
                        "Sources:=$(notdir $(wildcard *.c *.cc *.cxx *.cpp)) $(MORE)\n" +
                        "BaseName:=$(sort $(basename $(Sources)))\n" +
                        "Objects:=$(BaseName:%=%.o)\n" +
                        "all: $(OUTPUT)\n" +
                        "$(OUTPUT):$(Objects)\n" +
                        "\t$(CC) $^ $(LDFLAGS) -o $@\n" +
                        "\t@echo $@\n" +
                        "clean:\n\t$(RMObjects)\n" +
                        "%.o: %.c %.cc %.cxx %.cpp\n\t@busybox echo $<").getBytes());
            stream.close();
            thread.join();
            thread_.join();
            process.waitFor();
            Log.e(TextWarriorApplication.TAG, "MakeFileAsyncTask:" + out.toString() + errOut.toString());
            return true;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onPostExecute(Boolean s) {
        if (!s) return;
        cb.onResult(out.toString(), errOut.toString(), code);
    }

    public interface MakeCallback {
        File fromDirectory();

        byte[] getCleanCommand();

        Context getContext();

        byte[] getMakeCommand();

        String getOutputName();

        void onProgressUpdate(String[] values);

        void onResult(String result, String errString, int code);
    }
}
