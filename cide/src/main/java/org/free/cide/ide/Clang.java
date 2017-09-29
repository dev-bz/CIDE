package org.free.cide.ide;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.system.Os;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.myopicmobile.textwarrior.EditCallback;
import com.myopicmobile.textwarrior.TextWarriorApplication;
import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Language;
import com.myopicmobile.textwarrior.common.LanguageC;
import com.myopicmobile.textwarrior.common.LanguageCpp;
import com.myopicmobile.textwarrior.common.Lexer;

import org.free.API;
import org.free.cide.R;
import org.free.cide.callbacks.CodeCompilationDialogCallback;
import org.free.cide.callbacks.PopupList;
import org.free.cide.dialogs.FixableListDialog;
import org.free.cide.tasks.CodeCompilationTask;
import org.free.cide.utils.SystemColorScheme;
import org.free.cide.views.EditField;
import org.free.cide.views.GotoListView;
import org.free.cide.views.Tabs;
import org.free.clangide.ClangAPI;
import org.free.tools.format;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Clang implements EditCallback, CodeCompilationDialogCallback {
    private static final int CHECK_CODE = 0;
    private static final int CHECK_DONE = 1;
    private static final int CHECK_POP = 2;
    private static final int MODE_SDL = 2;
    private static final int MODE_SDL2 = 1;
    private static final int MODE_native = 3;
    public static int mainColor;
    private final MainActivity activity;
    private final Handler handler;
    private final int includeStart;
    public int lastErrors;
    public boolean notProgramLanguage = true;
    public ArrayList<String> out;
    private String prefix = "arm-linux-androideabi";
    private boolean afterDismiss;
    private boolean cancelCheck = true;
    private int diagonalDelay = 1000;
    private PopupList dialog;
    private GotoListView func = null;
    private int mode;
    private boolean mustPop;
    private String[] options;
    private CodeCompilationTask task;

    public Clang(final MainActivity activity) {
        this.activity = activity;
        mainColor = SystemColorScheme.getAppColor(activity, R.color.keyword);
        diagonalDelay = Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(activity).getString("diagonalDelay", "1000"));
        options = new String[]{"-std=c99",
                "-U__clang_major__",
                "-U__clang_minor__",
                "-D__clang_major__=3",
                "-D__clang_minor__=4",
                "-D__ANDROID__",
                "-D_GUN_SOURCE",
                "-D__STDC_CONSTANT_MACROS",
                "-D__STDC_FORMAT_MACROS",
                "-D__STDC_LIMIT_MACROS",
                "-fspell-checking",
                "-Wno-sentinel",
                "-Wno-unknown-attributes",
                "-fdiagnostics-parseable-fixits",
                "-I."
        };
        includeStart = options.length;
        updataEnv(activity);
        this.handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case CHECK_CODE:
                        if (notProgramLanguage) break;
                        out = new ArrayList<>();
                        View view = activity.main.edit;
                        if (view instanceof EditField) {
                            new CheckCode((EditField) view, true, out).execute();
                        }
                        break;
                    case CHECK_DONE:
                        if (func != null) {
                            func.setData(out);
                        }
                        FixableListDialog.dotNotFixit = false;
                        break;
                    case CHECK_POP:
                        if (dialog == null) {
                            if (task != null) task.cancel(true);
                            task = CodeCompilationTask.show(activity, Clang.this);
                        }
                        break;
                }
                return true;
            }
        });
    }

    public static boolean isMainCode(String file) {
        return file.endsWith(".c") || file.endsWith(".cpp") || file.endsWith(".cc") || file.endsWith(".cxx");
    }

    public static void setSort(final boolean sort) {
        new AsyncTask<Object, Object, Object>() {
            @Override
            protected Object doInBackground(Object[] params) {
                if (sort) {
                    ClangAPI.updatePosition(API._cmdSortOn, 0);
                } else {
                    ClangAPI.updatePosition(API._cmdSortOff, 0);
                }
                return null;
            }
        }.execute();
    }

    private static int testCodeMode(String[] strings) {
        for (String string : strings) {
            if (string.endsWith("/SDL2/SDL.h")) {
                return MODE_SDL2;
            } else if (string.endsWith("/SDL.h") || string.endsWith("/SDL/SDL.h")) {
                return MODE_SDL;
            } else if (string.endsWith("/android_native_app_glue.h")) {
                return MODE_native;
            }
        }
        return 0;
    }

    public void updataEnv(Activity activity) {
        String version = Main.getGCCVersion(activity);
        String filesPath = activity.getFilesDir().getPath();
        prefix = Main.prefix;
        options[includeStart - 1] = "-I" + filesPath + "/gcc/" + prefix + "/include/ncurses";
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Os.setenv("C_INCLUDE_PATH", filesPath + "/gcc/lib/gcc/" + prefix + "/" + version + "/include:" +
                        filesPath + "/gcc/lib/gcc/" + prefix + "/" + version + "/include-fixed:" +
                        filesPath + "/gcc/" + prefix + "/include", true);
                Os.setenv("CPLUS_INCLUDE_PATH", filesPath + "/gcc/" + prefix + "/include/c++/" + version + ":" +
                        filesPath + "/gcc/lib/gcc/" + prefix + "/" + version + "/include:" +
                        filesPath + "/gcc/lib/gcc/" + prefix + "/" + version + "/include-fixed:" +
                        filesPath + "/gcc/" + prefix + "/include", true);
            } else {
                ClangAPI.putenv("C_INCLUDE_PATH=" + filesPath + "/gcc/lib/gcc/" + prefix + "/" + version + "/include:" +
                        filesPath + "/gcc/lib/gcc/" + prefix + "/" + version + "/include-fixed:" +
                        filesPath + "/gcc/" + prefix + "/include");
                ClangAPI.putenv("CPLUS_INCLUDE_PATH=" +
                        filesPath + "/gcc/" + prefix + "/include/c++/" + version + ":" +
                        filesPath + "/gcc/lib/gcc/" + prefix + "/" + version + "/include:" +
                        filesPath + "/gcc/lib/gcc/" + prefix + "/" + version + "/include-fixed:" +
                        filesPath + "/gcc/" + prefix + "/include");
            }
        } catch (Exception ignored) {
        }
        updateInclude(null);
        ClangAPI.updateOption(options);
    }

    public void _clangOnFileOpened(boolean bHasFile, boolean mainCode) {
        if (bHasFile) {
            notProgramLanguage = false;
            View view = activity.main.edit;
            if (view instanceof EditField) {
                out = new ArrayList<>();
                new CheckCode((EditField) view, false, out).execute();
            }
        } else if (mainCode) {
            Language l = Lexer.getLanguage();
            if ((l instanceof LanguageC) || (l instanceof LanguageCpp)) {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                String newStd = "-std=" + ((l instanceof LanguageC) ? preferences.getString("c_std", "c99") : preferences.getString("cxx_std", "c++0x"));
                notProgramLanguage = false;
                if (!newStd.equals(options[0])) {
                    options[0] = newStd;
                    ClangAPI.updateOption(options);
                }
                codeChange();
            } else {
                notProgramLanguage = true;
                ((EditField) activity.main.edit).setErrLines(new ArrayList<Diagnostic>());
                clearData();
            }
        } else {
            notProgramLanguage = true;
            ((EditField) activity.main.edit).setErrLines(new ArrayList<Diagnostic>());
            clearData();
        }
    }

    public void clangOnFileOpened(final String fileName) {
        new AsyncTask<Object, Object, Boolean>() {
            @Override
            protected Boolean doInBackground(Object[] params) {
                return ClangAPI.bHasFile(fileName, true);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                _clangOnFileOpened(result, isMainCode(fileName));
            }
        }.execute();
    }

    public void clearData() {
        func.setData(new ArrayList<String>());
        ListView lv = (ListView) activity.findViewById(R.id.include);
        if (lv != null) {
            ListAdapter ad = lv.getAdapter();
            if (ad instanceof IncludeAdapter) {
                ((IncludeAdapter) ad).setData(null);
            }
        }
    }

    @Override
    public void codeChange() {
        if (diagonalDelay <= 0 || notProgramLanguage) return;
        func.setDoNotFixIt();
        FixableListDialog.dotNotFixit = true;
        cancelCheck = true;
        handler.removeMessages(CHECK_CODE);
        if (dialog == null && task == null) {
            handler.sendEmptyMessageDelayed(CHECK_CODE, diagonalDelay);
            afterDismiss = true;
        }
    }

    @Override
    public void edited(FreeScrollingTextField editField) {
        if (dialog != null) {
            dialog.yes(editField);
        }
    }

    @Override
    public void formatLine(int caretRow) {
        format.lineNumber = caretRow + 1;
        activity.main.format(false);
        format.lineNumber = 0;
    }

    public boolean onLayout() {
        if (dialog != null) {
            dialog.onLayout();
            return true;
        }
        return false;
    }

    @Override
    public void popCodeCompletion() {
        if (diagonalDelay <= 0 || notProgramLanguage) return;
        if (dialog == null) {
            if (cancelCheck) {
                handler.removeMessages(CHECK_CODE);
                out = new ArrayList<>();
                View view = activity.main.edit;
                if (view instanceof EditField) {
                    mustPop = true;
                    cancelCheck = false;
                    new CheckCode((EditField) view, true, out).execute();
                }
            } else if (task == null)
                task = CodeCompilationTask.show(activity, Clang.this);
        }
    }

    @Override
    public void codeCompilationNotify(int count, String[] data, int range, String text, String filterString, CodeHelp codeHelp) {
        dialog = EditField.show(activity, count, data, range, text, filterString, codeHelp);
        task = null;
    }

    public String[] getOptions() {
        return options;
    }

    public void makeCurrent() {
        ActionBar bar = activity.getSupportActionBar();
        if (bar != null) {
            Tabs tab = (Tabs) bar.getCustomView();
            String file = tab.getCurrentTag();
            if (file.indexOf('.') == -1) file += ".c";
            final String finalFile = file;
            new AsyncTask<Object, Object, Object>() {
                @Override
                protected Object doInBackground(Object... params) {
                    ClangAPI.bHasFile(finalFile, true);
                    return null;
                }
            }.execute();
        }
    }

    public void onCCDismiss() {
        Log.e(TextWarriorApplication.TAG, "onCCDismiss");
        dialog = null;
        if (task != null) task.cancel(true);
        else task = null;
        if (afterDismiss) {
            afterDismiss = false;
            codeChange();
        }
    }

    public void onCompileArgsChanged(String key, String value) {
        switch (key) {
            case "shell":
                break;
            case "c_std":
                updateStd(value);
                break;
            case "cxx_std":
                updateStd(value);
                break;
            case "c_arg":
                break;
            case "cxx_arg":
                break;
            case "mode_arg":
                break;
            case "sdl_arg":
                break;
            case "sdl2_arg":
                break;
            case "native_arg":
                break;
            case "box2d_arg":
                break;
            case "inc":
                updateInclude(value);
                ClangAPI.updateOption(options);
                codeChange();
                break;
        }
    }

    public void setDiagonalDelay(int diagonalDelay) {
        if (this.diagonalDelay == 0) {
            this.diagonalDelay = diagonalDelay;
            codeChange();
        } else this.diagonalDelay = diagonalDelay;
        if (this.diagonalDelay == 0) {
            func.setData(new ArrayList<String>());
            func.setDoNotFixIt();
            View f = activity.main.edit;
            if (f instanceof EditField) {
                ((EditField) f).setErrLines(new ArrayList<Diagnostic>());
                f.postInvalidate();
            }
        }
    }

    public void setup() {
        func = (GotoListView) activity.findViewById(R.id.listfunc);
        View f = activity.findViewById(R.id.textField);
        if (f instanceof EditField) {
            ((EditField) f).setCallback(this);
        }
    }

    public void updateInclude(String inc) {
        if (inc == null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
            inc = preferences.getString("inc", null);
        }
        if (TextUtils.isEmpty(inc)) return;
        ArrayList<String> newDir = new ArrayList<>();
        {
            String[] includes = inc.split(File.pathSeparator);
            for (String t : includes) {
                if (new File(t).isDirectory()) newDir.add(t);
            }
        }
        if (newDir.size() == 0) return;
        String[] tmp = new String[includeStart + newDir.size()];
        for (int i = 0; i < tmp.length; ++i) {
            if (i < includeStart) {
                tmp[i] = options[i];
            } else {
                tmp[i] = "-I" + newDir.get(i - includeStart);
            }
        }
        options = tmp;
    }

    public void updateMode() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String inc = preferences.getString("inc", "");
        switch (mode) {
            case MODE_native:
                break;
            case MODE_SDL:
                inc += ":" + activity.getFilesDir().getPath() + "/gcc/" + prefix + "/include/SDL";
                break;
            case MODE_SDL2:
                inc += ":" + activity.getFilesDir().getPath() + "/gcc/" + prefix + "/include/SDL2";
                break;
            default:
        }
        if (TextUtils.isEmpty(inc)) return;
        ArrayList<String> newDir = new ArrayList<>();
        {
            String[] includes = inc.split(File.pathSeparator);
            for (String t : includes) {
                if (new File(t).isDirectory()) newDir.add(t);
            }
        }
        if (newDir.size() == 0) return;
        String[] tmp = new String[includeStart + newDir.size()];
        for (int i = 0; i < tmp.length; ++i) {
            if (i < includeStart) {
                tmp[i] = options[i];
            } else {
                tmp[i] = "-I" + newDir.get(i - includeStart);
            }
        }
        options = tmp;
        new AsyncTask<String, Object, Object>() {
            @Override
            protected Object doInBackground(String... params) {
                ClangAPI.updateOption(params);
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                codeChange();
            }
        }.execute(options);
    }

    public void updateStd(String std) {
        String newStd = "-std=" + std;
        if (!newStd.equals(options[0])) {
            options[0] = newStd;
            whenOptionChanged();
        }
    }

    public void whenOptionChanged() {
        if (!notProgramLanguage) {
            ClangAPI.updateOption(options);
            View view = activity.main.edit;
            if (view instanceof EditField) {
                out = new ArrayList<>();
                new CheckCode((EditField) view, false, out).execute();
            }
        }
    }

    public static class Diagnostic {
        public final String fix;
        public final int note;
        public final String text;
        public int line;

        public Diagnostic(int line, String text, String fix) {
            this.line = line;
            this.text = text;
            this.note = line;
            this.fix = fix;
        }
    }

    private class CheckCode extends AsyncTask<String, String, String> {
        private final boolean newCode;
        private final List<String> out;
        public String file;
        private DocumentProvider doc;
        private EditField f;
        private String[] funcList;
        private String[] include;

        public CheckCode(EditField f, boolean codeChanged, List<String> out) {
            this.f = f;
            this.out = out;
            this.newCode = codeChanged;
            this.doc = f.createDocumentProvider();
        }

        private void takeDiagnostic(String result) {
            ArrayList<Diagnostic> errLines = new ArrayList<>();
            String[] strs = result.split("~~");
            for (String one : strs) {
                boolean errFile = false;
                if (one.startsWith(file)) {
                    errFile = true;
                    one = one.substring(file.length());
                }
                String[] str = one.split(": ", 2);
                if (str.length < 2) {
                    Log.e("CIDE", one);
                    continue;
                }
                if (out != null && !one.isEmpty()) out.add(one);
                if (errFile) {
                    String[] ll = str[0].split(":");
                    int line, column;
                    try {
                        line = Integer.parseInt(ll[1]);
                        //if (ll.length == 3) column = Integer.parseInt(ll[2]);
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    one = str[1];
                    column = one.indexOf('↔');
                    int i;
                    for (i = 0; i < errLines.size(); ++i) {
                        if (errLines.get(i).line > line) break;
                    }
                    String fix = null;
                    if (one.endsWith("</r>") && column >= 0) fix = one.substring(column + 1);
                    errLines.add(i, new Diagnostic(line, column < 0 ? one : one.substring(0, column), fix));
                }
            }
            if (out != null) {
                for (String one : funcList) {
                    if (one.startsWith(file))
                        out.add(one.substring(file.length()));
                }
            }
            if (f != null) {
                f.setErrLines(errLines);
                int size = errLines.size();
                if (size != lastErrors) {
                    /**if(size==0)Snackbar.make(f,"没有发现问题",Snackbar.LENGTH_SHORT).show();
                     else if(lastErrors==0)Snackbar.make(f,"发现"+ size +"个问题",Snackbar.LENGTH_SHORT).show();
                     else if(size>lastErrors)Snackbar.make(f,"增加"+ (size-lastErrors) +"个问题,达到"+size+"个",Snackbar.LENGTH_SHORT).show();
                     else Snackbar.make(f,"减少"+ (lastErrors-size) +"个问题,还剩"+size+"个",Snackbar.LENGTH_SHORT).show();*/
                    lastErrors = size;
                }
            }
        }

        @Override
        protected String doInBackground(String... params) {
            if (this.newCode) {
                int length = doc.docLength();
                char[] data = doc.subSequence(0, length);
                data[length - 1] = ' ';
                String code = new String(data);
                if (file.indexOf('.') == -1) {
                    if (Lexer.getLanguage() instanceof LanguageCpp) file += ".cpp";
                    else file += ".c";
                }
                ClangAPI.updateFileCode(file, code, isMainCode(file));
            }
            String out = ClangAPI.updatePosition(API._Diagnostic, 0).replace(activity.getFilesDir().toString() + File.separator, "<base>");
            this.include = ClangAPI.updatePosition(API._IncludeList, 0).replace(activity.getFilesDir().toString() + File.separator, "").split("\n");
            this.funcList = ClangAPI.updatePosition(API._FunctionList, 0).split("\n\n");
            return out;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ActionBar bar = activity.getSupportActionBar();
            if (bar != null && f != null) {
                Tabs tab = (Tabs) bar.getCustomView();
                file = tab.getCurrentTag();
                cancelCheck = false;
            } else {
                cancel(true);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (mustPop) {
                handler.sendEmptyMessage(CHECK_POP);
                mustPop = false;
            }
            if (cancelCheck) return;
            takeDiagnostic(result);
            handler.sendEmptyMessage(CHECK_DONE);
            //Log.e(TextWarriorApplication.LOG_TAG, "onPostExecute:" + result.replace("~~", "\n"));
            int new_mode = testCodeMode(include);
            if (new_mode != mode) {
                mode = new_mode;
                updateMode();
            }
            ListView lv = (ListView) activity.findViewById(R.id.include);
            if (lv != null) {
                ListAdapter ad = lv.getAdapter();
                if (ad instanceof IncludeAdapter) {
                    ((IncludeAdapter) ad).setData(include);
                } else {
                    IncludeAdapter adapter = new IncludeAdapter(include);
                    lv.setAdapter(adapter);
                    lv.setOnItemClickListener(adapter);
                }
            }
        }
    }

    private class IncludeAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
        private String[] include;

        public IncludeAdapter(String[] include) {
            this.include = include;
        }

        @Override
        public int getCount() {
            return include.length;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(activity, android.R.layout.simple_list_item_2, null);
                TextView text1 = ((TextView) convertView.findViewById(android.R.id.text1));
                text1.setTextColor(mainColor);
                TextView textView = ((TextView) convertView.findViewById(android.R.id.text2));
                textView.setSingleLine();
                textView.setEllipsize(TextUtils.TruncateAt.START);
            }
            String text = include[position];
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(text);
            int ix = text.lastIndexOf(File.separatorChar);
            if (ix != -1) {
                text = text.substring(ix + 1);
            }
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(text);
            return convertView;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            String file = include[position];
            if (file.equals("New.c")) return;
            if (file.charAt(0) != File.separatorChar) {
                file = (activity.getFilesDir().toString() + File.separator) + file;
                Intent i = new Intent(activity, ViewFileActivity.class).setData(Uri.parse("file://" + file));
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    Bundle b = ActivityOptions.makeSceneTransitionAnimation(activity, view, "include").toBundle();
                    activity.startActivity(i, b);
                } else {
                    activity.startActivity(i);
                }
            } else {
                Intent i = new Intent(activity, MainActivity.class).setData(Uri.parse("file://" + file));
                i.setAction(Intent.ACTION_VIEW);
                activity.startActivity(i);
            }
        }

        public void setData(String[] strings) {
            if (strings == null) {
                if (this.include.length > 0) {
                    this.include = new String[0];
                    notifyDataSetChanged();
                }
            } else {
                this.include = strings;
                notifyDataSetChanged();
            }
        }
    }
}
