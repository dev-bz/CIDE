package org.free.cide.ide;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.myopicmobile.textwarrior.TextWarriorApplication;
import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;
import com.myopicmobile.textwarrior.androidm.SelectionModeListener;
import com.myopicmobile.textwarrior.common.ColorScheme;
import com.termux.app.TermuxActivity;

import org.free.cide.BuildConfig;
import org.free.cide.R;
import org.free.cide.dialogs.ColorDialog;
import org.free.cide.dialogs.FixableListDialog;
import org.free.cide.dialogs.GotoDialog;
import org.free.cide.dialogs.PathDialog;
import org.free.cide.views.EditField;
import org.free.cide.views.FileListView;
import org.free.cide.views.ProjectView;
import org.free.tools.DocumentState;
import org.free.tools.Utils;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Iterator;

public class MainActivity extends AppCompatActivity implements SelectionModeListener, ActionMode.Callback {
    public static final int INSTALL_DONE = 124;
    private static final int REQUESTCODE_PERMISSION_STORAGE = 246;
    public Clang clang;
    public Main main;
    public boolean autoSave;
    private ActionMode actionMode;
    private ValueAnimator currAnim;
    private Finder finder;
    private Handler handler;

    public static void toastMakeText(View view, String s) {
        Snackbar.make(view, s, Snackbar.LENGTH_SHORT).show();
    }

    public static void toastMakeTextAndSave(View view, String message) {
        if (view instanceof EditField) {
            if (((EditField) view).isFileCanSave()) {
                ((EditField) view).askSave(message);
                return;
            }
        }
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    public static void installApk(Activity context, File file) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//            data = FileProvider.getUriForFile(context, "org.free.cide.fileprovider", file);
//            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//        } else
        {
            data = Uri.fromFile(file);
        }
        intent.setDataAndType(data, "application/vnd.android.package-archive");
        try {
            context.startActivity(intent/*, MainActivity.INSTALL_DONE*/);
        } catch (Exception e) {
            Toast.makeText(context, R.string.do_it_your_self, Toast.LENGTH_SHORT).show();
        }
    }

    private void format() {
        handler.sendEmptyMessage(1);
    }

    public void clickFindSetting(View view) {
        ScrollView scrollView = (ScrollView) findViewById(R.id.find_scrollView);
        if (scrollView != null) {
            int scrollY = scrollView.getScrollY();
            int height = scrollView.getMeasuredHeight();
            if (scrollY < (height >> 1)) {
                scrollView.fullScroll(View.FOCUS_DOWN);
            } else {
                scrollView.fullScroll(View.FOCUS_UP);
            }
        }
    }

    public void fileOpened(String filename) {
        if (filename.indexOf('.') == -1) {
            if (DocumentState.getLanguageCpp(this))
                filename += ".cpp";
            else filename += ".c";
        }
        clang.clangOnFileOpened(filename);
    }

    public boolean ensureStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                Log.e("CIDE", "checkSelfPermission true");
                return true;
            } else {
                Log.e("CIDE", "checkSelfPermission false");
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUESTCODE_PERMISSION_STORAGE);
                return false;
            }
        } else {
            // Always granted before Android 6.0.
            return true;
        }
    }

    public void hideReplacePanel(View view) {
        View view1 = findViewById(R.id.replace_panel);
        boolean landscape = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (view1 != null) {
            view.setSelected(!view1.isShown());
            if (landscape) {
                View viewById = findViewById(R.id.find_panel);
                if (viewById == null) {
                    return;
                }
                final int width = viewById.getMeasuredWidth();
                final View view2 = findViewById(R.id._find);
                if (view2 != null) {
                    final ViewGroup.LayoutParams ll = view2.getLayoutParams();
                    if (currAnim == null || !currAnim.isRunning()) {
                        if (view1.isShown()) {
                            currAnim = ValueAnimator.ofInt(ll.width, width);
                            long duration = currAnim.getDuration();
                            currAnim.setDuration(duration + (duration >> 1));
                            view1.animate().setDuration(duration).translationX(view1.getMeasuredWidth()).start();
                        } else {
                            currAnim = ValueAnimator.ofInt(ll.width, width / 2);
                            long duration = currAnim.getDuration();
                            view1.setVisibility(View.VISIBLE);
                            view1.animate().translationX(0).setDuration(duration + (duration >> 1)).start();
                        }
                        currAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                ll.width = (int) animation.getAnimatedValue();
                                view2.requestLayout();
                            }
                        });
                        currAnim.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                View view1 = findViewById(R.id.replace_panel);
                                if (view1 != null && view1.getTranslationX() > (width >> 2)) {
                                    view1.setVisibility(View.GONE);
                                }
                                super.onAnimationEnd(animation);
                            }
                        });
                        currAnim.start();
                    }
                }
            } else {
                view1.setVisibility(view1.isShown() ? View.GONE : View.VISIBLE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (INSTALL_DONE == requestCode) {
            Log.v("INSTALL_DONE", "data: " + data + " : " + resultCode);
            if (data != null) {
                Bundle extras = data.getExtras();
                for (Iterator<String> i = extras.keySet().iterator(); i.hasNext(); ) {
                    String next = i.next();
                    Log.v("INSTALL_DONE", "  data: " + next + " : " + extras.get(next));
                }
            }
        } else {
            clang.makeCurrent();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        boolean _return = true;
        if (drawer != null) {
            if (drawer.isDrawerOpen(GravityCompat.START) || drawer.isDrawerOpen(GravityCompat.END)) {
                drawer.closeDrawers();
                _return = false;
            }
        }
        View view = findViewById(R.id.find_panel);
        if (view != null && view.isShown()) {
            finder.onLongClick(null);
            _return = false;
        }
        View tmp = findViewById(R.id.settings);
        if (tmp != null && tmp.isShown()) {
            tmp.setVisibility(View.GONE);
            _return = false;
        }
        if (_return && !autoSave) {
            if (main.countNeedSave() > 0)
                if (!handler.hasMessages(-1)) {
                    handler.sendEmptyMessageDelayed(-1, 1600);
                    Toast.makeText(this, "有文件未保存，再按一次将直接退出", Toast.LENGTH_SHORT).show();
                    _return = false;
                }
        }
        if (_return) super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        String action = intent.getAction();
        if (TextUtils.isEmpty(action)) return;
        switch (action) {
            case Intent.ACTION_SEND:
            case Intent.ACTION_VIEW:
                main.onFileLoad(intent.getData().getPath());
            {
                View view = findViewById(R.id.textField);
                if (view instanceof FreeScrollingTextField) {
                    String line = intent.getStringExtra("line");
                    if (!TextUtils.isEmpty(line)) {
                        String column = intent.getStringExtra("column");
                        Main.gotoCursor((FreeScrollingTextField) view, line, column);
                    }
                }
            }
            break;
            case PathDialog.SELECT_FILE:
                main.paste(intent.getStringExtra("path"));
                break;
            case PathDialog.SELECT_PATH:
                main.paste(intent.getStringExtra("path"));
                break;
            case ColorDialog.SELECT_COLOR:
                main.paste(intent.getStringExtra("color"));
                break;
            case "action.INSTALL":
                main.reinstallGCC();
                break;
            case "action.GET_BACKGROUND":
                try {
                    long color = Long.parseLong(intent.getStringExtra("color").substring(2), 16);
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putInt("userColor", (int) color).apply();
                    View view = findViewById(R.id.textField);
                    if (view != null) {
                        view.setBackgroundColor((int) color);
                    }
                } catch (NumberFormatException ignored) {
                }
                break;
            case "action.SET_BACKGROUND":
                View view = findViewById(R.id.textField);
                if (view != null) {
                    Drawable bg = view.getBackground();
                    if (bg instanceof ColorDrawable) {
                        ColorDialog.show(this, ((ColorDrawable) bg).getColor(), "action.GET_BACKGROUND");
                    }
                }
                break;
        }
        super.onNewIntent(intent);
    }

    @Override
    protected void onResume() {
        EditField v = (EditField) findViewById(R.id.textField);
        if (v != null) {
            v._onResume();
        }
        super.onResume();
    }

    @Override
    protected void onStart() {
        main.onStart();
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if ("1".equals(defaultSharedPreferences.getString("theme", "0"))) {
            setTheme(R.style.AppTheme_NoActionBar);
        }
        autoSave = defaultSharedPreferences.getBoolean("autoSave", false);
        clang = new Clang(this);
        setContentView(R.layout.activity_main);
        initToolbar();
        initQuickBar(null);
        /**FragmentManager fm = getFragmentManager();
         fm.beginTransaction().replace(R.id.settings, SettingFragment.newInstance()).commit();*/
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null) {
            //fab.setImageDrawable(new NumberDrawable(this));
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    fabClick();
                }
            });
            fab.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    View view = findViewById(R.id.textField);
                    if (view instanceof EditField) {
                        ((EditField) view).toggleActionMode();
                    }
                    return true;
                }
            });
        }
        View view = findViewById(R.id.textField);
        if (view instanceof EditField) {
            ((EditField) view).setActivity(this);
            view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        View tmp = findViewById(R.id.settings);
                        if (tmp != null && tmp.isShown()) {
                            tmp.setVisibility(View.GONE);
                        }
                    }
                }
            });
            ((EditField) view).setSelModeListener(this);
            ((EditField) view).setHasParams(defaultSharedPreferences.getBoolean("hasParams", true));
        }
        Clang.setSort(defaultSharedPreferences.getBoolean("sort", false));
        finder = new Finder(this);
        clang.setup();
        main = new Main(this);
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what) {
                    case 0:
                        onNewIntent(getIntent());
                        break;
                    case 1:
                        main.format(true);
                        break;
                    case 2:
                        computeLayout();
                        break;
                    case 3:
                        showWhatsNew(false);
                    case 4: {
                        TextView title = (TextView) findViewById(R.id.barTitle);
                        if (title != null) {
                            Runtime runtime = Runtime.getRuntime();
                            String mem = "CIDE - " + toMemoryText(runtime.totalMemory()) + "/" + toMemoryText(runtime.maxMemory());
                            title.setText(mem);
                            handler.sendEmptyMessageDelayed(4, 2400);
                        }
                    }
                    break;
                }
                return true;
            }

            private String toMemoryText(long memory) {
                DecimalFormat format = new DecimalFormat("#.##");
                if (memory >= (1024 * 1024)) {
                    return format.format(memory / 1024.0 / 1024) + "M";
                } else if (memory >= 1024) {
                    return format.format(memory / 1024) + "K";
                }
                return memory + "B";
            }
        });
        handler.sendEmptyMessageDelayed(3, 1000);
        //View list = findViewById(R.id.textField);if (list != null) {list.requestFocus();}
        //view = findViewById(android.R.id.list);if (view != null) {view.setFocusable(false);}
    /*if (fab != null) {
      fab.postDelayed(new Runnable() {
        @Override
        public void run() {
          Log.e("CIDE", "getCurrentFocus:" + getCurrentFocus());
        }
      }, 1000);
    }*/
        ensureStoragePermissionGranted();
//        if(isWifiMode()){
        //postUpdate();
//        }
        Main.installSigleVersion(this, true);
    }

    private boolean isWifiMode() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Network[] networks = cm.getAllNetworks();

            for (Network j : networks) {
                NetworkInfo i = cm.getNetworkInfo(j);
                if (i.isConnected() && i.getType() == ConnectivityManager.TYPE_WIFI) {
                    return true;
                }
            }
        } else {
            NetworkInfo[] networks = cm.getAllNetworkInfo();
            for (NetworkInfo i : networks) {
                if (i.isConnected() && i.getType() == ConnectivityManager.TYPE_WIFI) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUESTCODE_PERMISSION_STORAGE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            FileListView listView = (FileListView) findViewById(R.id.listFiles);
            if (listView != null) {
                listView.gotoBaseDir();
            }
        }
    }

    private void initQuickBar(String s) {
        LinearLayout quickbar = (LinearLayout) findViewById(R.id.quickKeyBarList);
        if (quickbar != null) {
            if (s == null)
                s = PreferenceManager.getDefaultSharedPreferences(this).getString("quick", "\t{}();,.=\"|&![]<>+-/*");
            quickbar.removeAllViewsInLayout();
            for (int i = 0; i < s.length(); ++i) {
                LayoutInflater.from(this).inflate(R.layout.quick_bar_key, quickbar, true);
                TextView t = (TextView) quickbar.getChildAt(i);
                char charAt = s.charAt(i);
                if (charAt == '\t')
                    t.setText("\u21b9");
                else if (!Character.isWhitespace(charAt))
                    t.setText(String.valueOf(charAt));
            }
            if (quickbar.getChildCount() > 0) {
                quickbar.setEnabled(true);
            } else {
                quickbar.setVisibility(View.GONE);
                quickbar.setEnabled(false);
            }
        }
    }

    private void showWhatsNew(boolean anyway) {
        if (!anyway) {
            String ver = "2.35";
            try {
                ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            String old = "2.00";
            try {
                old = preferences.getString("version", "2.00");
            } catch (ClassCastException ignored) {
            }
            if (!ver.equals(old)) {
                preferences.edit().putString("version", ver).apply();
            } else return;
        }
        String newFeature = "v2.35\n" + " 1. 紧急修复补全闪退的问题\n";
        newFeature += "v2.34\n" + " 1. 改进代码片段管理\n 2. 修复输入法不弹出的问题\n 3. 其他次要调整及bug\n";
        newFeature += "v2.31\n" + " 1. 紧急修复'New'标签切换C/C++语言诊断模式不更新的问题\n 2. 修复切换主题闪退问题\n";
        newFeature += "v2.30\n" + " 1. 修复文件发送崩溃\n 2. 调整代码片段编辑UI\n 3. 'New'标签可切换C/C++语言\n";
        if (BuildConfig.FLAVOR.equals("out")) newFeature += " 4. 允许通过更新切换到单架构版本(13M~15M)\n";

        newFeature += "v2.27\n" + " 1. 修复在补全头文件闪退的问题\n 2. 修复追加文件到项目闪退的问题\n 3. 其他已知bug\n";
        newFeature += "v2.24\n" + " 1. 修复在android 4.3上自动补全闪退的问题\n 2. 修复在android 4.2上不能正常启动的问题\n 3. 修复android 7上无法执行编译的问题\n";
        newFeature += "v2.21\n" + " 1. 修复在android 7上不能正常浏览sdcard的问题\n 2. 在设置中增加是否自动保存选项\n";
        newFeature += "v2.18\n" + " 1. 修复实体键盘下兼容性问题\n 2. 修复6.0系统上创建项目对话框宽度不正常的问题\n 3. 调整4.x系统下的快捷字符栏界面配色\n";
        newFeature += "v2.15\n" + " 1. 修复补全列表在4.3以下系统无法点选的问题\n 2. 修复默认include目录查找顺序导致32位系统无法编译c++的问题\n 3. 修复编译c++时参数误用c的参数的问题\n 4. 修复接实体键盘情况下编辑框经常失去编辑状态的问题\n";
        newFeature += "v2.11\n" + " 1. 增加补全参数开关选项\n 2. 增加格式化单行字数限制选项\n 3. 改善实体键盘输入体验\n 4. 增加x86原生支持\n 5. 修复一些问题\n";
        newFeature += "v2.06\n" + " 1. 单文件运行时自动保存再编译运行\n 2. 修复项目打开后运行上次的编译结果无响应的问题\n 3. 改善输入法兼容性\n 4. 增加自定义快捷符号栏选项\n";
        newFeature += "v2.05\n" + " 1. 修复实体键盘无法多选文字的问题\n 2. 增加快捷符号输入栏\n 3. 修复百度输入法兼容性问题\n";
        newFeature += "v2.04\n" + " 1. 修复文件假删除的bug\n 2. 增加自定义背景选项\n 3. 增加格式化排序头文件选项\n";
        newFeature += "v2.03\n" + " 1. 修复SDL2执行失败的bug\n 2. 解决输入法光标移动控制问题\n";
        newFeature += "v2.02\n" + " 1. 调整自动补全弹出时机\n 2. 更新gcc6.1.0及sdl2.0.4\n 3. 小修一些bug\n";
        newFeature += "v2.01\n" + " 1. 遇到输入‘}’或‘;’时自动格式代码\n 2. 支持点击浮动按钮修改颜色字符串\n 3. 改进自动补全功能\n 4. 运行前自动编译\n 5. 修复光标触摸检测区域问题\n";
        newFeature += "v2.00\n" + " 1. 支持代码补全/诊断/导航\n 2. 支持简易项目管理\n 3. 支持SDL2/Native/Console类型编译及运行\n 4. 包含Box2D物理引擎库";
        main.showErrors("version", newFeature);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            if (toolbar != null) {
                toolbar.showOverflowMenu();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) return true;
        return super.onKeyDown(keyCode, event);
    }

    private void initToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setDisplayShowCustomEnabled(true);
            actionBar.setCustomView(R.layout.tabs);
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            drawer.setScrimColor(0x3f000000);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
        }
    }

    private void fabClick() {
        View tmp = findViewById(R.id.settings);
        if (tmp != null && tmp.isShown()) {
            tmp.setVisibility(View.GONE);
        } else {
            View view = findViewById(R.id.textField);
            if (view instanceof EditField) {
                if (!clang.notProgramLanguage) {
                    ((EditField) view).cursorInfo();
                } else {
                    ((EditField) view).selectMore(true);
                }
            }
        }
    }

    private void computeLayout() {
        View replace_panel = findViewById(R.id.replace_panel);
        if (replace_panel != null) {
            Configuration configuration = getResources().getConfiguration();
            boolean landscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE;
            if (landscape) {
                View viewById = findViewById(android.R.id.content);
                if (viewById != null) {
                    int width = viewById.getMeasuredWidth();
                    ViewGroup.LayoutParams layoutParams = replace_panel.getLayoutParams();
                    layoutParams.width = width >> 1;
                    if (replace_panel.getVisibility() == View.GONE) {
                        replace_panel.setTranslationX(width >> 1);
                    }
                    //replace_panel.setVisibility(View.INVISIBLE);
                    View _find = findViewById(R.id._find);
                    if (_find != null) {
                        //_find.setVisibility(View.VISIBLE);
                        layoutParams = _find.getLayoutParams();
                        if (replace_panel.getVisibility() == View.VISIBLE) {
                            layoutParams.width = width - (width >> 1);
                        } else {
                            layoutParams.width = width;
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        handler.sendEmptyMessage(0);
        handler.sendEmptyMessage(2);
        //View list = findViewById(R.id.textField);    if (list != null) {      list.requestFocus();    }
        //View view = findViewById(android.R.id.list);    if (view != null) {      view.setFocusable(false);    }
    }

    @Override
    protected void onStop() {
        if (autoSave) main.onStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        EditField v = (EditField) findViewById(R.id.textField);
        if (v != null) {
            v._onDestroy();
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        View view = findViewById(R.id.settings);
        if (view != null) {
            outState.putBoolean("settingHide", view.isShown());
        }
        view = findViewById(R.id.find_panel);
        if (view != null) {
            outState.putBoolean("find_panel", view.isShown());
        }
        view = findViewById(R.id.replace_panel);
        if (view != null) {
            outState.putBoolean("replace_panel", view.isShown());
        }
        view = findViewById(R.id.textField);
        if (view instanceof EditField) {
            //((EditField) view).setErrLines(new ArrayList<Clang.Diagnostic>());
            outState.putParcelable("STATE_TEXT_UI", ((EditField) view).getUiState());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        getMenuInflater().inflate(R.menu.edit, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        EditField edit = (EditField) findViewById(R.id.textField);
        if (edit != null) {
            switch (item.getItemId()) {
                case R.id.edit_moveLeft:
                    edit.moveCaretLeft();
                    break;
                case R.id.edit_moveRight:
                    edit.moveCaretRight();
                    break;
                case R.id.edit_selectAll:
                    edit.selectMore(false);
                    break;
                case R.id.edit_copy:
                    edit.onKeyShortcut(KeyEvent.KEYCODE_C, null);
                    break;
                case R.id.edit_cut:
                    edit.onKeyShortcut(KeyEvent.KEYCODE_X, null);
                    break;
                case R.id.edit_paste:
                    edit.onKeyShortcut(KeyEvent.KEYCODE_V, null);
                    break;
            }
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        View view = findViewById(R.id.settings);
        if (view != null) {
            view.setVisibility(savedInstanceState.getBoolean("settingHide") ? View.VISIBLE : View.GONE);
        }
        view = findViewById(R.id.find_panel);
        if (view != null) {
            view.setVisibility(savedInstanceState.getBoolean("find_panel") ? View.VISIBLE : View.GONE);
        }
        view = findViewById(R.id.replace_panel);
        if (view != null) {
            boolean replace_panel = savedInstanceState.getBoolean("replace_panel");
            view.setVisibility(replace_panel ? View.VISIBLE : View.GONE);
            View view2 = findViewById(R.id.find_toggle);
            if (view2 != null)
                view2.setSelected(replace_panel);
        }
        view = findViewById(R.id.textField);
        if (view instanceof EditField) {
            Parcelable textUi = savedInstanceState.getParcelable("STATE_TEXT_UI");
            if (textUi != null) ((EditField) view).restoreUiState(textUi);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_Q:
                DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer != null) {
                    if (drawer.isDrawerOpen(GravityCompat.END))
                        drawer.closeDrawer(GravityCompat.END);
                    if (drawer.isDrawerOpen(GravityCompat.START))
                        drawer.closeDrawer(GravityCompat.START);
                    else drawer.openDrawer(GravityCompat.START);
                }
                break;
            case KeyEvent.KEYCODE_P:
                drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
                if (drawer != null) {
                    if (drawer.isDrawerOpen(GravityCompat.START))
                        drawer.closeDrawer(GravityCompat.START);
                    if (drawer.isDrawerOpen(GravityCompat.END))
                        drawer.closeDrawer(GravityCompat.END);
                    else drawer.openDrawer(GravityCompat.END);
                }
                break;
            case KeyEvent.KEYCODE_U:
                noSupport();
                break;
            case KeyEvent.KEYCODE_H:
                FixableListDialog.show(this);
                break;
            case KeyEvent.KEYCODE_S:
                main.saveAll();
                break;
            case KeyEvent.KEYCODE_N:
                ProjectView pv = (ProjectView) findViewById(R.id.projects);
                if (pv != null && pv.isOpened()) {
                    pv.onClick(null);
                } else {
                    FileListView list = (FileListView) findViewById(R.id.listFiles);
                    if (list != null) {
                        list.showNewFile();
                    }
                }
            default:
                if (event != null) {
                    return super.onKeyShortcut(keyCode, event);
                }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!BuildConfig.FLAVOR.equals("out")) menu.findItem(R.id.action_up).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        View view1 = findViewById(R.id.textField);
        switch (id) {
            case R.id.action_settings: {
                if (view1 != null) {
                    View view = findViewById(R.id.settings);
                    if (view != null) {
                        if (!view1.isFocused()) {
                            view.setVisibility(view.isShown() ? View.GONE : View.VISIBLE);
                        } else if (TextWarriorApplication.KeyboardHided) {
                            view1.clearFocus();
                            view.setVisibility(view.isShown() ? View.GONE : View.VISIBLE);
                        } else if (!view.isShown()) {
                            view1.clearFocus();
                            main.showViewOnKeyboardHided(view);
                        }
                    }
                }
                break;
            }
            case R.id.action_up:
                postUpdate();
                //BDAutoUpdateSDK.silenceUpdateAction(this);
                break;
            case R.id.action_term:
                Intent i = new Intent(this, TermuxActivity.class);
                String executablePath = main.getShell();
                if (executablePath != null) i.setData(Uri.parse(executablePath));
                startActivity(i);
                break;
            case R.id.action_run:
                main.exec();
                break;
            case R.id.action_compile:
                main.compile(false);
                break;
            case R.id.cc:
                clang.popCodeCompletion();
                break;
            case R.id.format:
                main.format(false);
                break;
            case R.id.inset_path:
                PathDialog.show(this);
                break;
            case R.id.inset_color:
                ColorDialog.show(this);
                break;
            case R.id.action_find:
                //startActionMode(search);
                finder.onLongClick(null);
                break;
            case R.id.action_out:
                i = new Intent(this, CompileInfoActivity.class);
                i.putExtra("text1", main.lastResultString);
                i.putExtra("text2", main.lastErrorString);
                startActivity(i);
                break;
            /***case R.id.action_view:*/
        /*String inc = ClangAPI.updatePosition(API._IncludeList, 0);
        Log.e(TextWarriorApplication.LOG_TAG,inc);*/
            /**if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
             InputManager im= (InputManager) getSystemService(INPUT_SERVICE);
             int[] ids = im.getInputDeviceIds();
             for(int ii:ids) {
             InputDevice d = im.getInputDevice(ii);
             Log.e(TextWarriorApplication.LOG_TAG, d.toString());
             }
             }*/
            /***
             InputMethodManager ims = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
             boolean ret = ims.showSoftInput(view1, InputMethodManager.SHOW_IMPLICIT);
             Log.e(TextWarriorApplication.LOG_TAG, "showSoftInput:" + ret);
             break;*/
            case R.id.action_goto:
                GotoDialog.show(this);
                break;
            case R.id.action_help:
                //Snackbar.make(view1, ClangAPI.updatePosition(API._Version,0),Snackbar.LENGTH_SHORT).show();
                main.showHelp();
                //noSupport();
                break;
            case R.id.action_update:
                showWhatsNew(true);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void postUpdate() {
        //BDAutoUpdateSDK.uiUpdateAction(this, new MyUICheckUpdateCallback());
        Main.installSigleVersion(this, false);
    }

    private void noSupport() {
        View view = findViewById(R.id.textField);
        if (view != null) {
            Snackbar.make(view, "暂时未提供", Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSelectionModeChanged(boolean active) {
        if (active) actionMode = startActionMode(this);
        else if (actionMode != null) {
            actionMode.finish();
        }
    }

    public void quickKey(View view) {
        char cc = ((TextView) view).getText().charAt(0);
        if (cc == '\u21b9') {
            main.edit.paste("\t");
        } else {
            main.edit.paste(String.valueOf(cc));
        }
    }

    public void toggleQuickBar(View view) {
        View viewById = findViewById(R.id.quickKeyBarOpenButtonContainer);
        View viewById1 = findViewById(R.id.quickKeyBarKeysContainer);
        if (viewById == null || viewById1 == null) return;
        int id = view.getId();
        if (id == R.id.quickKeyBarOpenButton) {
            viewById.setVisibility(View.GONE);
            viewById1.setVisibility(View.VISIBLE);
        } else if (id == R.id.quickKeyBarCloseButton) {
            viewById.setVisibility(View.VISIBLE);
            viewById1.setVisibility(View.GONE);
        }

    }

    public static class SettingFragment extends PreferenceFragment {
        final Preference.OnPreferenceChangeListener check = new Check();

        private void reformat() {
            ((MainActivity) getActivity()).format();
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            setSummaryUpdate("theme", "0");
            setSummaryUpdate("codeTheme", "5");
            setSummaryUpdate("font", "0");
            setSummaryUpdate("font_size", "18");
            setSummaryUpdate("format", "llvm");
            setSummaryUpdate("ColumnLimit", "80");
            setSummaryUpdate("MaxEmptyLinesToKeep", "0");
            setSummaryUpdate("BreakBeforeBraces", "Attach");
            setSummaryUpdate("TabWidth", "8");
            setSummaryUpdate("IndentWidth", "2");
            setSummaryUpdate("diagonalDisplay", "全部");
            setSummaryUpdate("diagonalDelay", "1000ms");
            setCheckChange("autoSave");
            setCheckChange("UseTab");
            setCheckChange("wordwrap");
            setCheckChange("AntiAlias");
            setCheckChange("screenOn");
            setCheckChange("sort");
            setCheckChange("hasParams");
            setCheckChange("KeepEmptyLinesAtTheStartOfBlocks");
            setCheckChange("SortIncludes");
            setNoEmpty("shell", R.string.shell, true);
            setNoEmpty("c_std", R.string.c_std, false);
            setNoEmpty("cxx_std", R.string.cxx_std, false);
            setNoEmpty("c_arg", R.string.c_arg, false);
            setNoEmpty("cxx_arg", R.string.cxx_arg, false);
            setNoEmpty("sdl_arg", R.string.sdl_arg, false);
            setNoEmpty("sdl2_arg", R.string.sdl2_arg, false);
            setNoEmpty("mode_arg", R.string.mode_arg, false);
            setNoEmpty("native_arg", R.string.native_arg, false);
            setNoEmpty("box2d_arg", R.string.box2d_arg, false);
            findPreference("inc").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String text = ((EditTextPreference) preference).getText();
                    String value = String.valueOf(newValue);
                    if (!text.equals(value)) {
                        Activity tmp = getActivity();
                        if (tmp instanceof MainActivity) {
                            MainActivity activity = (MainActivity) tmp;
                            activity.clang.onCompileArgsChanged(preference.getKey(), value);
                        }
                    }
                    return true;
                }
            });
            findPreference("quick").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String text = ((EditTextPreference) preference).getText();
                    String value = String.valueOf(newValue);
                    boolean retValue = true;
                    if (!text.equals(value)) {

                        Activity tmp = getActivity();
                        if ("".equals(newValue)) {
                            value = tmp.getString(R.string.quick);
                            ((EditTextPreference) preference).setText(value);
                            retValue = false;
                        }
                        if (tmp instanceof MainActivity) {
                            MainActivity activity = (MainActivity) tmp;
                            value = value.replace("\\t", "\t");
                            activity.initQuickBar(value);
                        }
                    }
                    return retValue;
                }
            });
            SwitchPreference moreInfo = (SwitchPreference) findPreference("moreInfo");
            if (moreInfo.isChecked()) {
                findPreference("more").setEnabled(false);
            }
            moreInfo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    boolean text = ((SwitchPreference) preference).isChecked();
                    Boolean value = (boolean) newValue;
                    if (text != value) {
                        findPreference("more").setEnabled(text);
                    }
                    return true;
                }
            });
            try {
                ComponentName component = new ComponentName("com.baidu.tieba", "com.baidu.tieba.frs.FrsActivity");
                ActivityInfo info = getActivity().getPackageManager().getActivityInfo(component, 0);
                if (null != info) {
                    Intent tieba = findPreference("cide").getIntent();
                    tieba.setComponent(component);
                    tieba.putExtra("name", "cide");
                    tieba = findPreference("aide").getIntent();
                    tieba.setComponent(component);
                    tieba.putExtra("name", "aide");
                    tieba = findPreference("c4droid").getIntent();
                    tieba.setComponent(component);
                    tieba.putExtra("name", "c4droid");
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TextWarriorApplication.TAG, "NameNotFoundException:" + e.getMessage());
            }
            {
                Intent zfb = findPreference("zfb").getIntent();
                ComponentName componentName = zfb.resolveActivity(getActivity().getPackageManager());
                if (null == componentName) {
                    zfb.setData(Uri.parse("https://mobilecodec.alipay.com/client_download.htm?qrcode=aescsnt49njcwynkda"));
                    componentName = zfb.resolveActivity(getActivity().getPackageManager());
                    if (null == componentName) {
                        zfb.setData(Uri.parse("market://details?id=" + "com.eg.android.AlipayGphone"));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            zfb.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                        } else
                            zfb.setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    }
                } else Log.e(TextWarriorApplication.TAG, componentName.toString());
            }
        }

        private void setSummaryUpdate(String key, String value) {
            Preference fp;
            (fp = findPreference(key)).setOnPreferenceChangeListener(check);
            check.onPreferenceChange(fp, fp.getSharedPreferences().getString(key, value));
        }

        private void setCheckChange(String key) {
            findPreference(key).setOnPreferenceChangeListener(check);
        }

        private void setNoEmpty(String key, int valueId, boolean updateSummary) {
            String value = getString(valueId);
            Preference.OnPreferenceChangeListener empty = new NoEmpty(value, updateSummary);
            Preference fp;
            (fp = findPreference(key)).setOnPreferenceChangeListener(empty);
            empty.onPreferenceChange(fp, fp.getSharedPreferences().getString(key, value));
        }

        private class Check implements Preference.OnPreferenceChangeListener {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (preference instanceof ListPreference) {
                    int id = ((ListPreference) preference).findIndexOfValue(String.valueOf(newValue));
                    if (id < 0) {
                        preference.setSummary("值索引没有找到");
                    } else {
                        CharSequence summary = ((ListPreference) preference).getEntries()[id];
                        preference.setSummary(summary);
                    }
                    String value = String.valueOf(newValue);
                    String oldValue = ((ListPreference) preference).getValue();
                    if (!oldValue.equals(value)) {
                        switch (preference.getKey()) {
                            case "theme":
                                getActivity().recreate();
                                break;
                            case "codeTheme":
                                View view = getActivity().findViewById(R.id.textField);
                                if (view instanceof EditField) {
                                    EditField text = (EditField) view;
                                    if (value.equals("6")) {
                                        id = ((ListPreference) preference).findIndexOfValue(oldValue);
                                        if (id >= 0) {
                                            CharSequence summary = ((ListPreference) preference).getEntries()[id];
                                            preference.setSummary(summary);
                                        }
                                        preference.getEditor().remove("userColor").apply();
                                        id = text.getColorScheme().getColor(ColorScheme.Colorable.BACKGROUND);
                                        text.setBackgroundColor(id);
                                        return false;
                                    } else text.setColorScheme(value);
                                }
                                break;
                            case "font":
                                view = getActivity().findViewById(R.id.textField);
                                if (view instanceof EditField)
                                    ((EditField) view).setFont(Integer.valueOf(value));
                                break;
                            case "font_size":
                                view = getActivity().findViewById(R.id.textField);
                                if (view instanceof EditField)
                                    ((EditField) view).setZoom(Float.valueOf(value));
                                break;
                            case "diagonalDisplay":
                                view = getActivity().findViewById(R.id.textField);
                                if (view instanceof EditField) {
                                    ((EditField) view).setDiagonalDisplay(Integer.valueOf(value));
                                }
                                break;
                            case "diagonalDelay":
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).clang.setDiagonalDelay(Integer.valueOf(value));
                                }
                                break;
                            case "TabWidth":
                                Utils.newTabWidth(Integer.valueOf(value));
                            case "format":
                            case "IndentWidth":
                            case "ColumnLimit":
                            case "MaxEmptyLinesToKeep":
                            case "BreakBeforeBraces":
                                reformat();
                                break;
                        }
                    }
                } else if (preference instanceof CheckBoxPreference) {
                    boolean value = (Boolean) newValue;
                    if (((CheckBoxPreference) preference).isChecked() != value) {
                        switch (preference.getKey()) {
                            case "autoSave":
                                ((MainActivity) getActivity()).autoSave = value;
                                break;
                            case "wordwrap":
                                EditField text = (EditField) getActivity().findViewById(R.id.textField);
                                text.setWordWrap(value);
                                break;
                            case "AntiAlias":
                                text = (EditField) getActivity().findViewById(R.id.textField);
                                text.setAntiAlias(value);
                                break;
                            case "screenOn":
                                text = (EditField) getActivity().findViewById(R.id.textField);
                                text.setKeepScreenOn(value);
                                break;
                            case "UseTab":
                            case "KeepEmptyLinesAtTheStartOfBlocks":
                                reformat();
                                break;
                            case "SortIncludes":
                                if (value) reformat();
                                break;
                            case "sort":
                                Clang.setSort(value);
                                break;
                            case "hasParams":
                                text = (EditField) getActivity().findViewById(R.id.textField);
                                text.setHasParams(value);
                                break;
                        }
                    }
                } else
                    preference.setSummary(String.valueOf(newValue));
                return true;
            }
        }

        private class NoEmpty implements Preference.OnPreferenceChangeListener {
            private final String string;
            private final boolean updateSummary;

            NoEmpty(String string, boolean updateSummary) {
                this.string = string;
                this.updateSummary = updateSummary;
            }

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean returnValue = true;
                String text = ((EditTextPreference) preference).getText();
                if ("".equals(newValue)) {
                    ((EditTextPreference) preference).setText(string);
                    if (updateSummary) preference.setSummary(string);
                    returnValue = false;
                    newValue = string;
                } else if (updateSummary) {
                    preference.setSummary(String.valueOf(newValue));
                }
                String value = String.valueOf(newValue);
                if (!text.equals(value)) {
                    Activity tmp = getActivity();
                    if (tmp instanceof MainActivity) {
                        MainActivity activity = (MainActivity) tmp;
                        activity.clang.onCompileArgsChanged(preference.getKey(), value);
                    }
                }
                return returnValue;
            }
        }
    }
}
