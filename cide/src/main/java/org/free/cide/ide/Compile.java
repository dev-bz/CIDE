package org.free.cide.ide;

import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.myopicmobile.textwarrior.common.DocumentProvider;

import org.free.cide.dialogs.CompileDialog;
import org.free.cide.utils.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.StringTokenizer;

public class Compile extends AsyncTask<String, Object, Boolean> implements View.OnClickListener {
    final StringBuilder msg = new StringBuilder();
    final StringBuilder msgErr = new StringBuilder();
    private final Main activity;
    private final boolean runIfDone;
    private CompileDialog dlg;
    private DocumentProvider doc = null;
    private Process p;
    private boolean runIt;

    public Compile(Main activity, String file, boolean runIfDone) {
        this.activity = activity;
        this.runIfDone = runIfDone;
        if (file.endsWith(" -")) {
            doc = activity.edit.createDocumentProvider();
        }
    }

    public static String[] tokenize(String commandLine) {
        int count = 0;
        String[] arguments = new String[10];
        StringTokenizer tokenizer = new StringTokenizer(commandLine, " \"", true);
        String token;
        boolean insideQuotes = false;
        boolean startNewToken = true;
        while (tokenizer.hasMoreTokens()) {
            token = tokenizer.nextToken();
            switch (token.charAt(0)) {
                case ' ':
                    if (insideQuotes) {
                        arguments[count - 1] = arguments[count - 1] + token;
                        startNewToken = false;
                    } else {
                        startNewToken = true;
                    }
                    break;
                case '"':
                    if (!insideQuotes && startNewToken) {
                        if (count == arguments.length) {
                            System.arraycopy(arguments, 0, arguments = new String[count * 2], 0, count);
                        }
                        arguments[count++] = Util.EMPTY_STRING;
                    }
                    insideQuotes = !insideQuotes;
                    startNewToken = false;
                    break;
                default:
                    if (insideQuotes) {
                        arguments[count - 1] = arguments[count - 1] + token;
                    } else if (token.length() > 0 && !startNewToken) {
                        arguments[count - 1] = arguments[count - 1] + token;
                    } else {
                        if (count == arguments.length) {
                            System.arraycopy(arguments, 0, arguments = new String[count * 2], 0, count);
                        }
                        String trimmedToken = token.trim();
                        if (trimmedToken.length() != 0) {
                            arguments[count++] = trimmedToken;
                        }
                    }
                    startNewToken = false;
                    break;
            }
        }
        System.arraycopy(arguments, 0, arguments = new String[count], 0, count);
        return arguments;
    }

    public void destroy() {
        if (p != null)
            p.destroy();
    }

    @Override
    protected Boolean doInBackground(String... params) {
        String[] env = Main.readyCompileEnv(activity.activity);
        try {
            this.p = Runtime.getRuntime().exec("/system/bin/sh", env, activity.getFilesDir());
            OutputStream o = p.getOutputStream();
            for (String param : params) {
                o.write(param.getBytes());
                o.write('\n');
                o.flush();
            }
            //o.write(("\"gcc -pie -std=c99 -lm -ldl -llog -lz -Wfatal-errors\" -o temp "+file).getBytes());o.flush();
            if (doc != null) {
                int rowCount = doc.getRowCount();
                for (int row = 0; row < rowCount; ++row) {
                    String line = doc.getRow(row);//.replace("(", "\\(").replace(")","\\)");
                    int index = line.length() - 1;
                    if (line.charAt(index) == Character.MAX_VALUE) {
                        if (index > 0) line = line.substring(0, index);
                        else continue;
                    }
                    o.write(line.getBytes());
                }
                o.flush();
            }
            o.close();
            final BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            final BufferedReader rr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            Thread runInput = new Thread() {
                @Override
                public void run() {
                    String l;
                    try {
                        while (null != (l = r.readLine())) {
                            synchronized (msg) {
                                if (msg.length() > 0) msg.append('\n');
                                msg.append(l);
                            }
                        }
                    } catch (IOException ignored) {
                    }
                }
            };
            Thread runError = new Thread() {
                @Override
                public void run() {
                    String l;
                    try {
                        while (null != (l = rr.readLine())) {
                            synchronized (msgErr) {
                                if (msgErr.length() > 0) msgErr.append('\n');
                                msgErr.append(l);
                            }
                        }
                    } catch (IOException ignored) {
                    }
                }
            };
            runInput.start();
            runError.start();
            runInput.join();
            runError.join();
            p.waitFor();
            p = null;
            return true;
        } catch (IOException | InterruptedException ignored) {
            Log.e("CIDE", ignored.getMessage());
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        activity.showErrors("编译失败", msgErr.toString());
    }

    @Override
    protected void onPreExecute() {
        dlg = CompileDialog.show(activity.activity, this);
    }

    @Override
    protected void onPostExecute(Boolean done) {
        activity.lastResultString = msg.toString();
        activity.lastErrorString = msgErr.toString();
        dlg.dismissAllowingStateLoss();
        if (!TextUtils.isEmpty(msgErr.toString())) {
            Snackbar.make(activity.edit, "编译失败", Snackbar.LENGTH_SHORT).setAction("详情", this).show();
        } else if (runIfDone) {
            activity.exec();
        } else {
            SnackbarCallback callback = new SnackbarCallback();
            Snackbar snackbar = Snackbar.make(activity.edit, "编译成功", Snackbar.LENGTH_SHORT).setAction("运行", callback);
            snackbar.setCallback(callback);
            snackbar.show();
        }
    }

    class SnackbarCallback extends Snackbar.Callback implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (v == null) {
                activity.exec();
            } else {
                runIt = true;
            }
        }

        @Override
        public void onDismissed(Snackbar snackbar, int event) {
            if (runIt) {
                onClick(null);
                runIt = false;
            }
      /*FloatingActionButton fab = (FloatingActionButton)activity.activity.findViewById(R.id.fab);
      if (fab != null) {
        fab.show();
      }*/
            super.onDismissed(snackbar, event);
        }
    }
}
