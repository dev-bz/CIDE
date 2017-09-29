package org.free.cide.ide;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.PopupMenu;
import android.widget.TabHost;
import android.widget.Toast;

import com.myopicmobile.textwarrior.TextWarriorApplication;
import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Language;
import com.myopicmobile.textwarrior.common.LanguageC;
import com.myopicmobile.textwarrior.common.LanguageCpp;
import com.myopicmobile.textwarrior.common.Lexer;
import com.termux.app.TermuxService;

import org.free.API;
import org.free.cide.BuildConfig;
import org.free.cide.R;
import org.free.cide.app.App;
import org.free.cide.app.FileManager;
import org.free.cide.dialogs.CreateProjectDialog;
import org.free.cide.utils.BuildParameter;
import org.free.cide.utils.Util;
import org.free.cide.views.EditField;
import org.free.cide.views.FileListView;
import org.free.cide.views.OpenedListView;
import org.free.cide.views.ProjectView;
import org.free.cide.views.Tabs;
import org.free.clangide.ClangAPI;
import org.free.tools.Bin;
import org.free.tools.DocumentState;
import org.free.tools.Utils;
import org.free.tools.format;
import org.libsdl.app.SDLActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main implements Tabs.Callback, PopupMenu.OnMenuItemClickListener, TabHost.OnTabChangeListener, View.OnLayoutChangeListener, FileListView.Callback, OpenedListView.Callback {
    public static final int MODE_SDL = 1;
    public static final int MODE_SDL2 = 2;
    public static final int MODE_native = 3;
    public static final int MODE_qt = 4;
    public static String arch;
    public static String prefix = "arm-linux-androideabi";
    private static boolean hasBox2D;
    private static ArrayList<String> mainFile;
    private static String location = "aHR0cHM6Ly9maWxlcy5mZHMuYXBpLnhpYW9taS5jb20vZGF0YS9bZmlsZV0/YXBwSWQ9Mjg4MjMwMzc2MTUxNzEzODU1NyZzZXJ2aWNlVG9rZW49Q2dRbWNoOHo2dksyaFZRTjlpVUtDRVJ4UlVramlQdFJmbkx4VjRLaDdQY3ZUU1lMTElFUnZQWjF2QTFDYThubmpRTVlwMWtHL0xHMk1HT2ZQbjNlaUNkRGducllGckt5WVMzdzVPMEVwS2tha3ROeVNReERRcVZTV3lSS3ZMQjhSYm15b24vUkpXaC93VURKSTBRc1F4OWt1NmU0cFBKLzB0OS9MdFcvb1A4N0dEUVZqRERpZUgrUGd1TTE3bE5rVnk2QTR3NllpejAvcHdsTWUwZGFsaE1POEVRcFd2eFNPeXdMcUFCQXRSST0mc2lkPWRldmVsb3BlciZBdXRoZW50aWNhdGlvbj1TU08=";
    public final MainActivity activity;
    private final FileListView list;
    private final OpenedListView opened;
    private final ProjectView project;
    public File gccDir;
    public String lastResultString;
    public String lastErrorString;
    FreeScrollingTextField edit = null;
    private FileManager files = null;
    private format formatter;
    private View needShow;
    private Tabs tab = null;

    public Main(final MainActivity activity) {
        readyArch();
        this.activity = activity;
        if (activity.getApplication() instanceof App) {
            files = ((App) activity.getApplication()).getFileManager();
        }
        list = (FileListView) activity.findViewById(R.id.listFiles);
        if (list != null) {
            list.setCallback(this);
        }
        opened = (OpenedListView) activity.findViewById(R.id.openedList);
        if (opened != null) {
            opened.setCallback(this);
        }
        project = (ProjectView) activity.findViewById(R.id.projects);
        View view = activity.findViewById(R.id.textField);
        if (view instanceof FreeScrollingTextField) {
            edit = (FreeScrollingTextField) view;
        }
        final ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            tab = (Tabs) actionBar.getCustomView();
            tab.setCallback(this);
            tab.setOnTabChangedListener(this);
            ///onFileLoad(tab.getCurrentTag());
        }
        View content = activity.findViewById(R.id.keyboardTest);
        if (content != null) {
            content.addOnLayoutChangeListener(this);
        }
        Bin.setupBinDir(activity);
        installGcc(activity, false);
    }

    private static void installGccFromWeb(final Activity activity) {
        //File gcc = new File(activity.getFilesDir(), "gcc");
        new AsyncTask<String, String, Object>() {
            public MyDialog dialog;

            @Override
            protected Object doInBackground(String[] params) {
                Callback cb = new Callback() {
                    @Override
                    public void upziping(String name) {
                        publishProgress(name);
                    }
                };
                installGcc(activity, cb);
                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                dialog.onProgressUpdate(values);
            }

            @Override
            protected void onPreExecute() {
                dialog = MyDialog.show(activity);
            }

            @Override
            protected void onPostExecute(Object o) {
                ((MainActivity) activity).clang.updataEnv(activity);
                dialog.dismissAllowingStateLoss();
            }
        }.execute();
    }

    private static void initGccFiles(String version) {
        mainFile = new ArrayList<>();
        if (TextUtils.isEmpty(version)) {
            version = "6.1.0";
        }
        mainFile.add("tcc");
        mainFile.add("indent");
        mainFile.add("busybox");
        mainFile.add("gcc/bin/make");
        mainFile.add("gcc/bin/m4");
        mainFile.add("gcc/bin/sdl-config");
        mainFile.add("gcc/bin/pkg-config");
        mainFile.add("gcc/qt/bin/qmake");
        mainFile.add("gcc/qt/bin/rcc");
        mainFile.add("gcc/qt/bin/moc");
        mainFile.add("gcc/qt/bin/uic");
        mainFile.add("gcc/debugger/debugproxy");
        mainFile.add("gcc/debugger/gdb");
        mainFile.add("gcc/debugger/strip");
        if (!TextUtils.isEmpty(prefix)) {
            mainFile.add("gcc/bin/" + prefix + "-ar");
            mainFile.add("gcc/bin/" + prefix + "-as");
            mainFile.add("gcc/bin/" + prefix + "-g++");
            mainFile.add("gcc/bin/" + prefix + "-gcc");
            mainFile.add("gcc/bin/" + prefix + "-ld");
            mainFile.add("gcc/bin/" + prefix + "-nm");
            mainFile.add("gcc/bin/" + prefix + "-ranlib");
            mainFile.add("gcc/bin/" + prefix + "-strip");
            mainFile.add("gcc/libexec/gcc/" + prefix + "/" + version + "/cc1");
            mainFile.add("gcc/libexec/gcc/" + prefix + "/" + version + "/cc1plus");
            mainFile.add("gcc/libexec/gcc/" + prefix + "/" + version + "/collect2");
            mainFile.add("gcc/" + prefix + "/bin/ar");
            mainFile.add("gcc/" + prefix + "/bin/as");
            mainFile.add("gcc/" + prefix + "/bin/g++");
            mainFile.add("gcc/" + prefix + "/bin/gcc");
            mainFile.add("gcc/" + prefix + "/bin/ld");
            mainFile.add("gcc/" + prefix + "/bin/nm");
            mainFile.add("gcc/" + prefix + "/bin/ranlib");
            mainFile.add("gcc/" + prefix + "/bin/strip");
            mainFile.add("gcc/debugger/" + prefix + "-g++");
            mainFile.add("gcc/debugger/" + prefix + "-gcc");
        }
    }

    public static String getGCCVersion(Activity activity) {
        String version = null;
        String filesPath = activity.getFilesDir().getPath();
        try {
            BufferedReader b = new BufferedReader(new FileReader(filesPath + "/gcc/plugin_version"));
            String l = b.readLine();
            if (!TextUtils.isEmpty(l)) version = l;
        } catch (IOException ignored) {
        }
        readyArch();
        switch (arch) {
            case "arm":
                if (new File(filesPath, "gcc/arm-linux-androideabi/bin/gcc").isFile())
                    prefix = "arm-linux-androideabi";
                break;
            case "aarch64":
                if (new File(filesPath, "gcc/aarch64-linux-android/bin/gcc").isFile())
                    prefix = "aarch64-linux-android";
                break;
            case "i686":
                if (new File(filesPath, "gcc/i686-linux-android/bin/gcc").isFile())
                    prefix = "i686-linux-android";
                break;
        }
        return version;
    }

    public static void readyArch() {
        if (arch != null) return;
        arch = System.getProperty("os.arch");
        Log.e(TextWarriorApplication.TAG, "start ARCH=" + arch);
        if (arch.startsWith("arm")) {
            // Handle different arm variants such as armv7l:
            arch = "arm";
        } else if (arch.equals("x86_64")) {
            arch = "i686";
        }
        Log.e(TextWarriorApplication.TAG, "end ARCH=" + arch);
    }

    public static void installSigleVersion(Activity activity, boolean dont_install) {
        readyArch();
        String replacement = "cide-" + arch + "-" + BuildConfig.VERSION_CODE + ".apk";
        if (!BuildConfig.FLAVOR.equals("out")) {
            File file = new File(Environment.getExternalStorageDirectory(), replacement);
            if (file.delete()) {
                Toast.makeText(activity, R.string.delete_apk, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (dont_install) return;
        String flags = new String(Base64.decode(location, Base64.NO_WRAP));
        Message message = Message.show(activity, "下载更新，请稍等", "正在连接...");
        new AsyncTask<Object, String, File>() {
            public Activity _activty;
            public Message _message;
            public Callback _callback = new Callback() {
                @Override
                public void upziping(String name) {
                    publishProgress(name);
                }
            };

            @Override
            protected void onProgressUpdate(String... values) {
                if (values.length > 0)
                    if (_message != null && _message.getDialog().isShowing()) {
                        _message.setMessage(values[0]);
                    }
            }

            @Override
            protected File doInBackground(Object... params) {
                _message = (Message) params[0];
                _message.setDismissCallback(new Runnable() {
                    @Override
                    public void run() {
                        _message = null;
                        Toast.makeText(_activty, R.string.user_cancel, Toast.LENGTH_SHORT).show();
                        cancel(true);
                    }
                });
                _activty = (Activity) params[2];

                return getDownloadFile(_callback, String.valueOf(params[1]), true);
            }

            @Override
            protected void onCancelled(File file) {
                super.onCancelled(file);
                if (file.exists())
                    if (file.delete()) {
                        Toast.makeText(_activty, R.string.delete_apk, Toast.LENGTH_SHORT).show();
                    }
            }

            @Override
            protected void onPostExecute(File s) {
                if (s == null || !s.exists()) {
                    if (_message != null && _message.getDialog().isShowing()) {
                        _message.setMessage("下载失败");
                    }
                    return;
                }
                if (_message != null && _message.getDialog().isShowing()) {
                    _message.setDismissCallback(null);
                    _message.dismiss();
                }
                MainActivity.installApk(_activty, s);
            }
        }.execute(message, flags.replace("[file]", replacement), activity);
    }

    private static void installGcc(Activity activity, Callback cb) {
        String flags = new String(Base64.decode(location, Base64.NO_WRAP));
        String[] urls = new String[4];
        String ver = "610";
        ArrayList<String> lines = new ArrayList<>();
        try {
            URL url = new URL(flags.replace("[file]", "gcc.txt"));
            BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()));
            String l = r.readLine();
            if (l != null) ver = l;
            while (null != (l = r.readLine())) lines.add(l);
            r.close();
        } catch (IOException ignored) {
        }
        urls[0] = ("gcc" + ver + "/" + arch + "/gcc.zip");
        urls[1] = ("gcc" + ver + "/" + arch + "/sdl2.zip");
        urls[2] = ("gcc" + ver + "/" + arch + "/configs.zip");
        urls[3] = ("gcc" + ver + "/" + arch + "/box2d.zip");
        File gcc = new File(activity.getFilesDir(), "gcc");
        if (gcc.isFile()) gcc.delete();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            try {
//                Os.remove(gcc.getPath());
//            } catch (Exception ignored) {
//                ignored.printStackTrace();
//            }
//        }
        deleteFiles(gcc);
        for (String pkg : urls) {
            File tmpfile = null;
            try {
                tmpfile = getDownloadFile(cb, flags.replace("[file]", pkg), false);
                unzipData(activity, new FileInputStream(tmpfile), cb);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (tmpfile.exists()) tmpfile.delete();
        }
        String version = getGCCVersion(activity);
        initGccFiles(version);
        for (String name : mainFile) {
            File file = new File(activity.getFilesDir(), name);
            if (file.exists()) file.setExecutable(true);
        }
    }

    private static boolean deleteFiles(File file) {
        if (file.isDirectory()) {
            File[] lst = file.listFiles();
            for (File i : lst) deleteFiles(i);
        }
        return file.exists() && file.delete();
    }

    @Nullable
    private static File getDownloadFile(Callback cb, String pkg, boolean isApk) {
        Log.e("Main", isApk + " : " + pkg);
        URL url;
        try {
            url = new URL(pkg);
        } catch (MalformedURLException e) {
            return null;
        }
        File tmpfile;
        if (isApk) {
            tmpfile = new File(Environment.getExternalStorageDirectory(), new File(url.getPath()).getName());
            if (tmpfile.exists()) return tmpfile;
        } else {
            try {
                tmpfile = File.createTempFile("cide-", ".zip");
            } catch (IOException e) {
                return null;
            }
        }
        Log.e("Main", tmpfile + "");
        URLConnection urlConnection;
        try {
            urlConnection = url.openConnection();
        } catch (IOException e) {
            return tmpfile;
        }
        long lengthLong;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            lengthLong = urlConnection.getContentLengthLong();
        } else {
            lengthLong = urlConnection.getContentLength();
        }
        cb.upziping("downloading... " + url.getPath());
        int l;
        long loaded = 0;
        String last = null;
        byte[] buffer = new byte[4096];
        try {
            InputStream open = urlConnection.getInputStream();
            FileOutputStream f = new FileOutputStream(tmpfile);
            while ((l = open.read(buffer, 0, 4096)) > 0) {
                f.write(buffer, 0, l);
                loaded += l;
                String bfb = String.valueOf(loaded * 100 / lengthLong);
                if (!bfb.equals(last)) {
                    cb.upziping("downloading... [" + bfb + " %]" + url.getPath());
                    last = bfb;
                }
            }
            open.close();
            f.close();
        } catch (IOException e) {
            return tmpfile;
        }
        Log.e(TextWarriorApplication.TAG, tmpfile.getPath() + " size: " + tmpfile.length());
        return tmpfile;
    }

    private static void unzipData(Activity activity, InputStream open, Callback callback) throws IOException {
        int l;
        byte[] buffer = new byte[4096];
        ZipInputStream z = new ZipInputStream(open);
        ZipEntry n;
        while ((n = z.getNextEntry()) != null) {
            String name = n.getName();
            File file = new File(activity.getFilesDir(), name);
            if (n.isDirectory()) {
                if (!file.exists()) file.mkdirs();
            } else {
                if (file.exists() && file.length() == n.getSize() && file.lastModified() == n.getTime()) {
                    z.closeEntry();
                    continue;
                }
                //Log.e(TextWarriorApplication.TAG,name+" zip : "+n.getSize());
                //Log.e(TextWarriorApplication.TAG,name+" src : "+file.length());
                callback.upziping(file.getName());
                if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
                if (!file.exists()) file.createNewFile();
                if (n.getSize() > 0) {
                    FileOutputStream f = new FileOutputStream(file);
                    while ((l = z.read(buffer, 0, 4096)) > 0) {
                        f.write(buffer, 0, l);
                    }
                    f.close();
                }
                file.setLastModified(n.getTime());
                //Log.e(TextWarriorApplication.TAG,name+" aft : "+file.length());
            }
            z.closeEntry();
        }
    }

    private static void browseTieba(Activity activity) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://pan.baidu.com/s/1nuWllqT"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        } else {
            i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        }
        activity.startActivity(i);
    }

    public static void gotoCursor(FreeScrollingTextField textField, String line_string, String column_string) {
        int parseInt = Integer.parseInt(line_string);
        DocumentProvider ddd = textField.createDocumentProvider();
        int lineOffset = ddd.getLineOffset(parseInt - 1);
        if (column_string != null) {
            int parseLne = Integer.parseInt(column_string);
            int l2 = ddd.getLineOffset(parseInt);
            if (l2 <= parseLne) l2 = ddd.docLength() - 1;
            byte[] bt = new String(ddd.subSequence(lineOffset, l2 - lineOffset)).getBytes();
            lineOffset += new String(bt, 0, parseLne - 1).length();
        }
        ddd.seekChar(lineOffset);
        if (ddd.hasNext())
            if (Character.isJavaIdentifierStart(ddd.next())) {
                ++lineOffset;
                while (ddd.hasNext())
                    if (Character.isJavaIdentifierPart(ddd.next())) ++lineOffset;
                    else break;
            }
        textField.selectText(false);
        textField.moveCaret(lineOffset);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public static void installApp(Activity activity, String name) {
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + name));
        i.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        activity.startActivity(i);
    }

    public static String[] readyCompileEnv(Context activity) {
        String path = System.getenv("PATH");
        String tmpdir = readyTmpdir(activity);
        String path1 = activity.getFilesDir().getPath();
        return new String[]{
                "PATH=" + path1 + "/gcc/bin:" + path1 + "/gcc/" + prefix + "/bin" + ":" + path,
                "TMPDIR=" + tmpdir, "SHELL=" + getShell(activity), "OUTPUT=" + activity.getFilesDir().getPath() + "/temp"
        };
    }

    public static String getShell(Context activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String shell = preferences.getString("shell", null);
        if (shell != null) {
            if (shell.endsWith(" -")) shell = shell.substring(0, shell.length() - 2);
            String file = "null";
            Language language = DocumentState.getLanguage(new File(file), activity);
            String std;
            if (language instanceof LanguageCpp) {
                std = preferences.getString("cxx_std", activity.getString(R.string.cxx_std));
            } else {
                std = preferences.getString("c_std", activity.getString(R.string.c_std));
            }
            shell = new BuildParameter(((MainActivity) activity).main, preferences, shell, file, std, file, 0).invoke().get();
        } else shell = "/system/bin/sh";
        return shell;
    }

    private static String readyTmpdir(Context activity) {
        String tmpdir = activity.getFilesDir().getPath() + "/tmpdir";
        if (!new File(tmpdir).exists()) new File(tmpdir).mkdir();
        return tmpdir;
    }

    private static int testCodeMode(String string) {
        if (TextUtils.isEmpty(string)) return 0;
        hasBox2D = string.contains("/Box2D/Box2D.h\n");
        if (string.contains("/SDL2/SDL.h\n")) {
            return MODE_SDL2;
        }
        if (string.contains("/SDL.h\n") || string.contains("/SDL/SDL.h\n")) {
            return MODE_SDL;
        }
        if (string.contains("/android_native_app_glue.h\n")) {
            return MODE_native;
        }/*
    if (string.contains("/QApplication\n") || string.contains("/QtWidgets\n")) {
			return MODE_qt;
		}*/
        return 0;
    }

    private boolean installGcc(final MainActivity activity, boolean reinstall) {
        if (gccDir == null)
            gccDir = new File(activity.getFilesDir(), "gcc");
        if (gccDir.isFile()) gccDir.delete();
        Resources gccPackage2 = null;
        try {
            PackageManager manager = activity.getPackageManager();
            ApplicationInfo ai = manager.getApplicationInfo("com.n0n3m4.gcc4droid", 0);
            PackageInfo pi = manager.getPackageInfo(ai.packageName, 0);
            if (gccDir.isDirectory()) {
                String version = getGCCVersion(activity);
                if (version.compareTo(pi.versionName) < 0) {
                    reinstall = true;
                }
            }
            gccPackage2 = manager.getResourcesForApplication(ai);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        if (!gccDir.isDirectory() || reinstall) {
            boolean has_gcc = false;
            boolean has_sdl2 = false;
            boolean has_configs = false;
            boolean has_box2d = false;
            try {
                String[] names = activity.getAssets().list("");
                for (String i : names) {
                    if (i.equals("gcc.zip")) has_gcc = true;
                    else if (i.equals("sdl2.zip")) has_sdl2 = true;
                    else if (i.equals("configs.zip")) has_configs = true;
                    else if (i.equals("box2d-" + arch + ".zip")) has_box2d = true;
                    Log.e(TextWarriorApplication.TAG, "getAssets: " + i);
                }
                Log.e(TextWarriorApplication.TAG, "has_box2d: " + has_box2d);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Resources gccPackage = null;
            if (has_gcc || has_sdl2 || has_configs || has_box2d) gccPackage = activity.getResources();
            if (gccPackage2 == null && !(has_gcc && has_sdl2 && has_configs)) {
                gccPackage = null;
                installApp("gcc插件未安装", "请下载gcc插件安装后重试,需网络传输建议开启Wifi", "com.n0n3m4.gcc4droid");
            }
            Log.e(TextWarriorApplication.TAG, "Package: " + gccPackage + "," + gccPackage2);
            if (gccPackage != null || gccPackage2 != null) {
                new AsyncTask<Resources, String, Object>() {
                    public MyDialog dialog;

                    @Override
                    protected Object doInBackground(Resources[] params) {
                        Callback cb = new Callback() {
                            @Override
                            public void upziping(String name) {
                                publishProgress(name);
                            }
                        };
                        installGcc(params, cb);
                        return null;
                    }

                    @Override
                    protected void onProgressUpdate(String... values) {
                        dialog.onProgressUpdate(values);
                    }

                    @Override
                    protected void onPreExecute() {
                        dialog = MyDialog.show(activity);
                    }

                    @Override
                    protected void onPostExecute(Object o) {
                        activity.clang.updataEnv(activity);
                        dialog.dismissAllowingStateLoss();
                    }
                }.execute(gccPackage2, gccPackage);
            }
            return true;
        }
        return false;
    }

    private void installGcc(Resources[] param, Callback cb) {
        File gcc = new File(activity.getFilesDir(), "gcc");
        if (gcc.exists()) gcc.delete();
        String[] packages = new String[]{"gcc.zip", "sdl2.zip", "configs.zip", "box2d-" + arch + ".zip"};
        for (String pkg : packages) {
            Log.e(TextWarriorApplication.TAG, "open: " + pkg);
            for (Resources res : param) {
                if (res != null) {
                    AssetManager assets = res.getAssets();
                    try {
                        InputStream open = assets.open(pkg);
                        unzipData(activity, open, cb);
                        Log.e(TextWarriorApplication.TAG, "open done: " + pkg);
                        break;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        String version = getGCCVersion(activity);
        initGccFiles(version);
        for (String name : mainFile) {
            File file = new File(activity.getFilesDir(), name);
            if (file.exists()) file.setExecutable(true);
        }
    }

    private void installApp(String title, String message, String name) {
        InstallApp.show(activity, title, message, name);
    }

    @Override
    public void browse(String path) {
        DrawerLayout drawer = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.openDrawer(GravityCompat.START);
            TabHost tabhost = (TabHost) activity.findViewById(R.id.tabhost);
            if (tabhost != null) {
                tabhost.setCurrentTab(0);
                File parentFile = new File(path).getParentFile();
                if (parentFile != null) list.into(parentFile);
            }
        }
    }

    public boolean checkAndInstall(String packageName) {
        try {
            activity.getPackageManager().getResourcesForApplication(packageName);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            installApp("gcc插件未安装", "请下载gcc插件安装后重试", packageName);
        }
        return false;
    }

    public void closeFile(String file) {
        tab.removeTab(file);
        files.onCloseFileTab(file);
    }

    public void exec() {
        ProjectView pv = (ProjectView) activity.findViewById(R.id.projects);
        String runDir;
        File temp = new File(activity.getFilesDir(), "temp");
        if (!temp.exists()) {
            compile(true);
            return;
        }
        String modeString = null;
        long time = files.saveAll(true);
        if (pv != null && pv.isOpened()) {
            if (time > temp.lastModified()) {
                compile(true);
                return;
            }
            runDir = pv.getProjectDir();
            modeString = pv.getProject().mode;
        } else {
            String currentFile = currentFile();
            File source = new File(currentFile);
            if (currentFile.equals("New")) {
                if (edit.isEdited()) {
                    time = Long.MAX_VALUE;
                    edit.setEdited(false);
                } else time = 0;
            } else {
                time = source.lastModified();
            }
            if (time > temp.lastModified()) {
                compile(true);
                return;
            }
            runDir = source.getParent();
        }
        if (TextUtils.isEmpty(runDir)) runDir = Environment.getExternalStorageDirectory().getPath();
        //mode=Integer.parseInt(p.getString("mode","0"));
        int mode = 0;
        if (TextUtils.isEmpty(modeString))
            mode = testCodeMode(ClangAPI.updatePosition(API._IncludeList, 0));
        else if (modeString.equals("SDL")) mode = MODE_SDL;
        else if (modeString.equals("SDL2")) mode = MODE_SDL2;
        else if (modeString.equals("Native")) mode = MODE_native;
        String libFile = temp.getPath();
        switch (mode) {
            case MODE_SDL2: {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(activity, org.libsdl.app.SDL2Activity.class));
                intent.putExtra("fname", libFile);
                intent.putExtra("currdir", runDir);
                intent.putExtra("cmdline", libFile);
                Log.e(TextWarriorApplication.TAG, "startActivity SDL2Activity");
                try {
                    activity.startActivity(intent);
                } catch (Exception e) {
                    if (e.getMessage().startsWith("Permission")) {
                        showMessage("无法访问 SDL plugin for C4droid_2.0.4.");
                    } else showMessage("是否忘记安装 SDL plugin for C4droid_2.0.4 ？");
                }
            }
            break;
            case MODE_SDL: {
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.n0n3m4.droidsdl", "com.n0n3m4.droidsdl.SDL4droidMain"));
                intent.putExtra("fname", libFile);
                intent.putExtra("currdir", runDir);
                intent.putExtra("cmdline", libFile);
                Log.e(TextWarriorApplication.TAG, "startActivity SDL4droidMain");
                try {
                    activity.startActivity(intent);
                } catch (Exception e) {
                    if (e.getMessage().startsWith("Permission")) {
                        showMessage("无法访问 SDL plugin for C4droid_2.0.4.");
                    } else showMessage("是否忘记安装 SDL plugin for C4droid_2.0.4 ？");
                }
            }
            break;
            case MODE_native: {
                System.loadLibrary("sdl2util");
                SDLActivity.nativechdir(runDir);
                Intent intent = new Intent(activity, android.app.NativeActivity.class);
                Log.e(TextWarriorApplication.TAG, "startActivity NativeActivity");
                try {
                    activity.startActivity(intent);
                } catch (Exception e) {
                    showMessage("执行失败");
                }
            }
            break;
            case MODE_qt:
                showMessage("尚未支持qt,也不打算支持了");
                break;
            default:
                Intent i = new Intent(activity, TermuxService.class);
                i.setAction("com.termux.service_execute");
                i.setData(Uri.parse(libFile));
                String[] arguments = {"-l"};
                i.putExtra("com.termux.execute.arguments", arguments);
                Log.e(TextWarriorApplication.TAG, "startService TermuxService");
                activity.startService(i);
        }
    }

    private String currentFile() {
        return tab.getCurrentTag();
    }

    private void showMessage(String text) {
        DrawerLayout dl = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
        if (dl != null && (dl.isDrawerOpen(GravityCompat.START) || dl.isDrawerOpen(GravityCompat.END)))
            Snackbar.make(dl, text, Snackbar.LENGTH_SHORT).show();
        else Snackbar.make(edit, text, Snackbar.LENGTH_SHORT).show();
    }

    public void compile(boolean runIfDone) {
        //installGcc(activity);
        if (installGcc(activity, false)) return;
        ProjectView pv = (ProjectView) activity.findViewById(R.id.projects);
        if (pv != null) {
            if (pv.build(runIfDone)) return;
        }
        if (!Lexer.getLanguage().isProgLang()) {
            Snackbar.make(edit, R.string.no_program, Snackbar.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);

        String run = preferences.getString("env", "");
        String std;
        String file = tab.getCurrentTag();
        Language language = DocumentState.getLanguage(new File(file), activity);
        String code_std;
        String currentSrcDir = ".";
        boolean isNew = "New".equals(file);
        if (!isNew) {
            currentSrcDir = new File(file).getParent();
        }
        if (language instanceof LanguageCpp) {
            code_std = preferences.getString("cxx_std", activity.getString(R.string.cxx_std));
            std = preferences.getString("cxx_arg", activity.getString(R.string.cxx_arg));
            if (isNew) file = "-xc++ -";
        } else {
            code_std = preferences.getString("c_std", activity.getString(R.string.c_std));
            std = preferences.getString("c_arg", activity.getString(R.string.c_arg));
            if (isNew) file = "-xc -";
        }


        int mode = testCodeMode(ClangAPI.updatePosition(API._IncludeList, 0));
        switch (mode) {
            case MODE_SDL:
                std += " " + preferences.getString("sdl_arg", activity.getString(R.string.sdl_arg)).trim();
                break;
            case MODE_SDL2:
                std += " " + preferences.getString("sdl2_arg", activity.getString(R.string.sdl2_arg)).trim();
                break;
            case MODE_native:
                std += " " + preferences.getString("native_arg", activity.getString(R.string.native_arg)).trim();
                break;
        }
        if (hasBox2D) {
            std += " " + preferences.getString("box2d_arg", activity.getString(R.string.box2d_arg)).trim();
        }
        std = new BuildParameter(this, preferences, std, file, code_std, currentSrcDir, mode).invoke().get();
        run = new BuildParameter(this, preferences, run, file, code_std, currentSrcDir, mode).invoke().get();
        new Compile(this, file, runIfDone).execute(run, std);
    }

    public String getShell() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        String shell = preferences.getString("shell", null);
        String file = currentFile();
        Language language = DocumentState.getLanguage(new File(file), activity);
        String std;
        if (language instanceof LanguageCpp) {
            std = preferences.getString("cxx_std", activity.getString(R.string.cxx_std));
        } else {
            std = preferences.getString("c_std", activity.getString(R.string.c_std));
        }
        if (shell != null)
            shell = new BuildParameter(this, preferences, shell, file, std, currentFile(), 0).invoke().get();
        return shell;
    }

    public void format(boolean styleChanged) {
        if (!Lexer.getLanguage().isProgLang()) {
            Snackbar.make(edit, "不支持当前文件", Snackbar.LENGTH_SHORT);
        } else if (formatter == null) formatter = Utils.formatCode(Bin.getClangFormat(), edit);
        else Utils.formatCode(formatter, styleChanged);
    }

    public File getFilesDir() {
        return activity.getFilesDir();
    }

    @Override
    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (bottom == oldBottom) return;
//        Rect r = new Rect();
//        View rootView = v.getRootView();
//        rootView.getWindowVisibleDisplayFrame(r);
        TextWarriorApplication.KeyboardHided = oldBottom < bottom;//(r.bottom == rootView.getHeight());
        //Log.e(TextWarriorApplication.LOG_TAG, "onLayoutChange bottom:" + bottom + " oldBottom:" + oldBottom + " hided:" + TextWarriorApplication.KeyboardHided);
        if (TextWarriorApplication.KeyboardHided) {
            if (edit.isFocused()) {
                edit.clearFocus();
            }
            if (needShow != null) {
                needShow.setVisibility(View.VISIBLE);
                needShow = null;
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        File file = new File(tab.getCurrentTag());
        switch (item.getItemId()) {
            case R.id.cleanup:
                edit.selectAll();
                edit.paste("");
                break;
            case R.id.copy_all:
                ClipboardManager cm = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                DocumentProvider documentProvider = edit.createDocumentProvider();
                cm.setPrimaryClip(ClipData.newPlainText("code", new String(documentProvider.subSequence(0, documentProvider.docLength() - 1))));
                Toast.makeText(activity, R.string.copy_ok, Toast.LENGTH_SHORT).show();
                break;
            case R.id.language:
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
                boolean languageCpp = !preferences.getBoolean("NewLanguageCpp", false);
                preferences.edit().putBoolean("NewLanguageCpp", languageCpp).apply();
                if (languageCpp) {
                    Lexer.setLanguage(LanguageCpp.getInstance());
                } else {
                    Lexer.setLanguage(LanguageC.getInstance());
                }

                edit.setEdited(true);
                activity.clang._clangOnFileOpened(false, true);
                edit.respan();
                break;
            case R.id.goto_dir:
                if (file.exists()) {
                    DrawerLayout drawer = (DrawerLayout) activity.findViewById(R.id.drawer_layout);
                    if (drawer != null) {
                        drawer.openDrawer(GravityCompat.START);
                        TabHost tabhost = (TabHost) activity.findViewById(R.id.tabhost);
                        if (tabhost != null) {
                            tabhost.setCurrentTab(0);
                            list.into(file.getParentFile());
                        }
                    }
                }
                break;
            case R.id.copy_path:
                if (file.exists()) {
                    ClipboardManager m = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    m.setPrimaryClip(ClipData.newPlainText("path", tab.getCurrentTag()));
                    MainActivity.toastMakeText(edit, "已复制");
                }
                break;
            case R.id.close_current:
                files.onCloseFileTab(tab.getCurrentTag());
                tab.removeTab(tab.getCurrentTag());
                break;
            case R.id.close_other:
                files.onCloseOtherTab(tab.getCurrentTag());
                tab.removeOthers(tab.getCurrentTag());
                break;
            case R.id.add_to_project:
                addToProjects(file);
                break;
            case R.id.save:
                files.saveAll(false);
                break;
        }
        return true;
    }

    @Override
    public void addToProjects(File path) {
        ProjectView pv = (ProjectView) activity.findViewById(R.id.projects);
        if (pv != null) {
            pv.addFile(path);
        }
    }

    @Override
    public void onFileDeleted(File file) {
        ProjectView pv = (ProjectView) activity.findViewById(R.id.projects);
        if (pv != null) {
            pv.onFileDeleted(file);
        }
    }

    public void reinstallGCC() {
        installGcc(activity, true);
    }

    @Override
    public void select(File file) {
        if (file == null) return;
        if (file.isDirectory()) return;
        if (file.getName().equals(".cide")) {
            openProject(file);
        } else if (file.exists()) {
            if (Util.isOpenable(file.getName())) {
                onFileLoad(file.getPath());
            } else {
                String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName());
                if (!TextUtils.isEmpty(extension)) {
                    extension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                }
                if (TextUtils.isEmpty(extension))
                    extension = "*/*";
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(file), extension);
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                try {
                    activity.startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(activity, R.string.fail_to_open, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public boolean isProjectClosed() {
        return !project.isOpened();
    }

    private void openProject(File file) {
        if (!file.exists()) {
            CreateProjectDialog.show(activity, file.getParentFile());
        } else {
            project.open(file, true);
        }
    }

    public void onFileLoad(String filename) {
        View view = activity.findViewById(R.id.textField);
        if (view instanceof EditField) {
            EditField edit = (EditField) view;
            if (edit.setCurrent(files, filename)) {
                tab.addTab(filename);
                activity.fileOpened(filename);
                if (!filename.equals("New")) {
                    opened.opened(filename);
                }
            }
        }
    }

    public void onStart() {
        files.reloadAll();
    }

    public void onStop() {
        files.saveAll(true);
    }

    @Override
    public void onTabChanged(String tabId) {
        onFileLoad(tabId);
    }

    @Override
    public void onRemoveTab(String d) {
        files.onCloseFileTab(d);
    }

    @Override
    public void openMenu(View tab, boolean isNew) {
        PopupMenu pop = new PopupMenu(tab.getContext(), tab);
        if (isNew) {
            pop.inflate(R.menu.tab_new);
            if (PreferenceManager.getDefaultSharedPreferences(activity).getBoolean("NewLanguageCpp", false))
                pop.getMenu().findItem(R.id.language).setTitle(R.string.languageC);
        } else {
            pop.inflate(R.menu.tab);
            if (isProjectClosed()) pop.getMenu().findItem(R.id.add_to_project).setEnabled(false);
        }
        pop.setOnMenuItemClickListener(this);
        pop.show();
    }

    public void paste(String text) {
        edit.paste(text);
    }

    public void saveAll() {
        files.saveAll(false);
    }

    public void showErrors(String title, String message) {
        new AlertDialog.Builder(activity)/*.setTitle(title)*/.setMessage(message).show();
    }

    public void showHelp() {
        onFileLoad("help");
    }

    public void showViewOnKeyboardHided(View view) {
        needShow = view;
    }

    public int countNeedSave() {
        return files.countNeedSave();
    }

    interface Callback {
        void upziping(String name);
    }

    public static class MyDialog extends DialogFragment {
        public static MyDialog show(Activity activity) {
            MyDialog ret = new MyDialog();
            ret.show(activity.getFragmentManager(), "wait");
            return ret;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            setCancelable(false);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            ProgressDialog p = new ProgressDialog(getActivity());
            p.setTitle("正在解压安装GCC");
            p.setCancelable(false);
            p.setCanceledOnTouchOutside(false);
            p.setMessage("过程需要一些时间,请稍等");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                p.create();
            }
            return p;
        }

        public void onProgressUpdate(String[] values) {
            if (values.length == 1) {
                ((ProgressDialog) getDialog()).setMessage("过程需要一些时间,请稍等\n" + values[0]);
            }
        }
    }

    public static class Message extends DialogFragment {
        private Runnable dismissCallback;

        public static Message show(Activity activity, String title, String message) {
            Bundle arg = new Bundle();
            arg.putString("title", title);
            arg.putString("message", message);
            Message dlg = new Message();
            dlg.setArguments(arg);
            dlg.show(activity.getFragmentManager(), "message");
            return dlg;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle arg = getArguments();
            return new AlertDialog.Builder(getActivity()).setTitle(arg.getString("title")).setMessage(arg.getString("message")).create();
        }

        public void setMessage(String message) {
            if (getDialog() instanceof AlertDialog) {
                ((AlertDialog) getDialog()).setMessage(message);
            }
        }

        public void setDismissCallback(Runnable dismissCallback) {
            this.dismissCallback = dismissCallback;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            if (dismissCallback != null) dismissCallback.run();
            super.onDismiss(dialog);
        }
    }

    public static class InstallApp extends DialogFragment implements DialogInterface.OnClickListener {
        public static void show(Activity activity, String title, String message, String app) {
            InstallApp dlg = new InstallApp();
            Bundle arg = new Bundle();
            arg.putString("title", title);
            arg.putString("message", message);
            arg.putString("app", app);
            dlg.setArguments(arg);
            dlg.show(activity.getFragmentManager(), "install");
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    installApp(getActivity(), getArguments().getString("app"));
                    break;
                case Dialog.BUTTON_NEUTRAL:
                    //browseTieba(getActivity());
                    Main.installGccFromWeb(getActivity());
                    break;
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Bundle arg = getArguments();
            return new AlertDialog.Builder(getActivity()).
                    setTitle(arg.getString("title")).
                    setMessage(arg.getString("message")).
                    setPositiveButton("从商店搜索安装", this).
                    setNeutralButton("网络解压安装(24M)", this).create();
        }
    }
}
