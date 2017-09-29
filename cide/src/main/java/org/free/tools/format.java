/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.free.tools;

import android.support.annotation.NonNull;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author zzzhang
 */
public class format {
    private static final String LOG_TAG = "format";
    public static int lineNumber = 0;
    private final OpText jTextCode;
    private final String program;
    private String llvm = null;
    private Process p;
    private String style = null;
    private String xml;

    public format(String dir, OpText op) {
        program = dir;
        jTextCode = op;
    }

    void startFormat(boolean styleChanged) {
        if (styleChanged) {
            llvm = null;
            style = null;
        }
        xml = "";
        byte[] bytes = jTextCode.getBytes();
        int[] carts = jTextCode.getCarts();
        if (llvm == null) llvm = jTextCode.getStyle();
        if (style == null) style = getStyle(jTextCode);
        Log.v(LOG_TAG, program);
        try {
            //String program = program+"-output-replacements-xml -fallback-style=llvm -style=\"{MaxEmptyLinesToKeep: 0}\"";
            String[] exec;
            if (lineNumber > 0) {
                exec = new String[]{program, "-output-replacements-xml", "-fallback-style=" + llvm, "-style={" + style + "}", "-lines=" + Math.max(1, lineNumber - 1) + ":" + lineNumber};
            } else {
                exec = new String[]{program, "-output-replacements-xml", "-fallback-style=" + llvm, "-style={" + style + "}"};
            }/*
      clang-format -output-replacements-xml -fallback-style=llvm -style="{MaxEmptyLinesToKeep: 0}" execpty.c
			*/
            p = Runtime.getRuntime().exec(exec);
            //op.msg(program);
        } catch (IOException ex1) {
            xml += ex1.getMessage();
            p = null;
        }
        if (p == null) return;
        new Thread() {
            @Override
            public void run() {
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getErrorStream()));
                String l;
                try {
                    while ((l = br.readLine()) != null)
                        xml += "\n" + l;
                } catch (IOException ignored) {
                }
            }
        }.start();
        {
            xml += "Start";
            //System.out.println("================");
            //javax.swing.text.Document jdoc=jTextCode.getDocument();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            int killYet = 0, oldPos = 0, tl = 0;
            String l;
            try {
                {
                    p.getOutputStream().write(bytes);
                    p.getOutputStream().flush();
                    p.getOutputStream().close();
                }
                while ((l = br.readLine()) != null) {
                    if (!xml.isEmpty()) xml += "\n";
                    xml += "==" + l;
                    Log.v(LOG_TAG, "-   " + l);
                    if (l.endsWith("</replacement>")) {
                        String[] v = l.split("'");
                        if (v.length == 5) {
                            String subSequence = v[4].substring(1, v[4].indexOf("<"));
                            int parseInt = Integer.parseInt(v[1]);
                            int parseInt1 = Integer.parseInt(v[3]);
                            parseInt1 = new String(bytes, parseInt, parseInt1).length();
                            tl = new String(bytes, oldPos, parseInt - oldPos).length() + tl;
                            oldPos = parseInt;
                            parseInt = tl + killYet;
                            //if(parseInt1>0)jTextCode.remove(parseInt,parseInt1);
                            if (subSequence.length() > 0)
                                subSequence = subSequence.replace("&#10;", "\n").replace("&#13;", "\r").replace("&lt;", "<"); //jTextCode.insert(parseInt,subSequence);
                            jTextCode.replace(parseInt, parseInt1, subSequence);
                            for (int i = 0; i < carts.length; ++i)
                                if (carts[i] >= parseInt + parseInt1) {
                                    carts[i] -= parseInt1;
                                    carts[i] += subSequence.length();
                                } else if (carts[i] > parseInt)
                                    carts[i] = parseInt;//+subSequence.length();
                            killYet -= parseInt1;
                            killYet += subSequence.length();
                        }
                    }
                }
                xml += "\nwhile end";
            } catch (IOException ex) {
                xml += "\n" + ex.getMessage();
            }
        }
        try {
            p.waitFor();
        } catch (InterruptedException ex) {
            xml += "\n" + ex.getMessage();
        }
        jTextCode.done(xml);
    }

    @NonNull
    private String getStyle(OpText op) {
        StringBuilder vb = new StringBuilder();
        vb.append(op.getStyle("MaxEmptyLinesToKeep", false)).
                append(op.getStyle("ColumnLimit", false)).
                append(op.getStyle("KeepEmptyLinesAtTheStartOfBlocks", true)).
                append(op.getStyle("BreakBeforeBraces", false)).
                append(op.getStyle("TabWidth", false)).
                append(op.getStyle("IndentWidth", false)).
                append(op.getStyle("SortIncludes", true)).
                append(op.getStyle("UseTab", true));
        vb.delete(0, 2);
        return vb.toString();
    }

    public interface OpText {
        void done(String xml);

        byte[] getBytes();

        int[] getCarts();

        String getStyle();

        String getStyle(String key, boolean boolType);

        void replace(int parseInt, int parseInt1, String subSequence);

        void set(Object v);

        void tab(int tabWidth);
    }
}
