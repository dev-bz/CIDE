package org.free.tools;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;
import com.myopicmobile.textwarrior.common.DocumentProvider;

import org.free.cide.ide.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

class Replace {
    public final int nn;
    public final int pp;
    public final String ss;

    public Replace(int p, int n, String s) {
        pp = p;
        nn = n;
        ss = s;
    }
}

public class Utils {
    private static final String LOG_TAG = "Utils";
    private static final format.OpText op = new format.OpText() {
        private final ArrayList<Replace> replace = new ArrayList<>();
        public int tabWidth = 4;
        private int[] carts;
        private DocumentProvider doc;
        private FreeScrollingTextField f;
        private long n;

        @Override
        public void done(String xml) {
            int size = replace.size();
            if (size == 0) {
                if (format.lineNumber == 0)
                    MainActivity.toastMakeText(f, "无可挑剔");
                return;
            }
            doc.beginBatchEdit();
            for (Replace r : replace) {
                if (r.nn > 0) doc.deleteAt(r.pp, r.nn, n);
                if (r.ss.length() > 0) doc.insertBefore(r.ss.toCharArray(), r.pp, n);
            }
            doc.endBatchEdit();
            if (format.lineNumber == 0)
                MainActivity.toastMakeTextAndSave(f, "已替换" + size + "处");
            replace.clear();
            if (f.getCaretPosition() != carts[0]) f.moveCaret(carts[0]);
            f.setTabSpaces(tabWidth);
            f.respan();
            f.setEdited(true);
            //Toast.makeText(textField.getContext(),xml,0).show();
        }

        @Override
        public byte[] getBytes() {
            doc = f.createDocumentProvider();
            return new String(doc.subSequence(0, doc.docLength() - 1)).getBytes();
        }

        @Override
        public int[] getCarts() {
            n = System.nanoTime();
            return carts = new int[]{f.getCaretPosition()};
        }

        @Override
        public String getStyle() {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(f.getContext());
            return p.getString("format", "llvm");
        }

        @Override
        public String getStyle(String key, boolean boolType) {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(f.getContext());
            String string;
            if (boolType) string = Boolean.toString(p.getBoolean(key, false));
            else string = p.getString(key, "");
            Log.v(LOG_TAG, key + " : " + string);
            if (string.isEmpty()) return "";
            return ", " + key + ": " + string;
        }

        @Override
        public void replace(int parseInt, int parseInt1, String subSequence) {
            replace.add(new Replace(parseInt, parseInt1, subSequence));
        }

        @Override
        public void set(Object v) {
            f = (FreeScrollingTextField) v;
        }

        @Override
        public void tab(int tabWidth) {
            this.tabWidth = tabWidth;
        }
    };

    static public String exec(String cmd) {
        final StringBuilder o = new StringBuilder();
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            final InputStream i = p.getInputStream();
            p.getOutputStream().close();
            new Thread() {
                @Override
                public void run() {
                    BufferedReader r = new BufferedReader(new InputStreamReader(i));
                    String l;
                    l = null;
                    try {
                        while ((l = r.readLine()) != null) {
                            if (o.length() > 0)
                                o.append('\n');
                            o.append(l);
                        }
                    } catch (IOException ignored) {
                    }
                }
            }.start();
            p.waitFor();
        } catch (IOException | InterruptedException ignored) {
        }
        return o.toString();
    }

    static public void fixIt(String code, FreeScrollingTextField f) {
        op.set(f);
        fixit fxt = new fixit(code, op);
        fxt.startFixit();
    }

    static public format formatCode(String program, FreeScrollingTextField f) {
        op.set(f);
        format fmt = new format(program, op);
        fmt.startFormat(false);
        return fmt;
    }

    public static void formatCode(format formatter, boolean styleChanged) {
        formatter.startFormat(styleChanged);
    }

    public static void newTabWidth(Integer tabWidth) {
        op.tab(tabWidth);
    }
}
