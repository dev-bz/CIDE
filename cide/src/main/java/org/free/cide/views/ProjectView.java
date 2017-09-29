package org.free.cide.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.myopicmobile.textwarrior.TextWarriorApplication;

import org.free.cide.R;
import org.free.cide.callbacks.ProjectCallback;
import org.free.cide.dialogs.AskDeleteItemsDialog;
import org.free.cide.dialogs.ChooseRunModeDialog;
import org.free.cide.dialogs.ProjectSettingDialog;
import org.free.cide.ide.Clang;
import org.free.cide.ide.Main;
import org.free.cide.ide.MainActivity;
import org.free.cide.ide.Project;
import org.free.cide.tasks.MakeFileAsyncTask;
import org.free.cide.tasks.ScanProjectModeTask;
import org.free.cide.utils.BuildParameter;
import org.free.cide.utils.SystemColorScheme;
import org.free.cide.utils.Util;
import org.free.tools.FileUtils;
import org.free.tools.FindFileAsyncTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class ProjectView extends LinearLayout implements ProjectCallback, FindFileAsyncTask.Callback, AdapterView.OnItemClickListener, MakeFileAsyncTask.MakeCallback, Toolbar.OnMenuItemClickListener, View.OnClickListener, TextView.OnEditorActionListener, PopupMenu.OnMenuItemClickListener, AbsListView.MultiChoiceModeListener {
    private boolean cppmode;
    private boolean hasBox2D;
    private Project projectData = new Project();
    private BaseAdapter adapter;
    private String[] files = new String[0];
    private ListView list;
    private String mode;
    private int normalColor;
    private File project;
    private OnClickListener runAction = new RunAction();
    private boolean runIfDone;
    private String tempBin;
    private Toast toast;

    public ProjectView(Context context) {
        super(context);
        init();
    }

    public ProjectView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProjectView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ProjectView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public String getProjectDir() {
        return project.getParent();
    }

    public void init() {
        tempBin = new File(getContext().getFilesDir(), "temp").getPath();
    }

    public void addFile(File file) {
        if (!file.exists()) return;
        if (project == null) {
            Toast.makeText(getContext(), "项目没有打开", Toast.LENGTH_SHORT).show();
        } else if (!file.isDirectory()) {
            if (file.getParent().equals(project.getParent())) return;
            if (!projectData.add(file.getPath())) return;
            files = Arrays.copyOf(files, files.length + 1);
            files[files.length - 1] = file.getPath();
            Arrays.sort(files);
            adapter.notifyDataSetChanged();
            new ScanProjectModeTask(this).execute();
        }
    }

    public boolean isOpened() {
        return project != null && project.exists();
    }

    public boolean create() {
        if (project == null) return false;
        if (!project.exists()) {
            try {
                project.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return project.exists();
    }

    public void delete() {
        if (project != null && project.exists()) {
            if (list.getCheckedItemCount() == 0) return;
            boolean changed = false;
            Main activityMain = null;
            if (getContext() instanceof MainActivity) {
                activityMain = ((MainActivity) getContext()).main;
            }
            for (int i = 0; i < list.getCount(); ++i) {
                String string = list.getItemAtPosition(i).toString();
                if (list.isItemChecked(i)) {
                    if (projectData.contains(string)) {
                        if (activityMain != null) {
                            activityMain.closeFile(string);
                        }
                        projectData.remove(string);
                        changed = true;
                    } else if (!string.startsWith(File.separator)) {
                        File file = new File(project.getParentFile(), string);
                        if (file.exists()) {
                            if (activityMain != null) {
                                activityMain.closeFile(file.getPath());
                            }
                            Log.e(TextWarriorApplication.TAG, "delete:" + file.delete());
                        }
                    }
                    if (Clang.isMainCode(string)) changed = true;
                    list.setItemChecked(i, false);
                }
            }
            if (project.exists())
                onResult(project.getParentFile().list(), changed);
            else closeProject();
        }
    }

    @Override
    public File fromDirectory() {
        return project.getParentFile();
    }

    @Override
    public String[] getEnv() {
        return Main.readyCompileEnv(getContext());
    }

    public void onFileDeleted(File file) {
        if (project == null) return;
        if (file.getParentFile().equals(project.getParentFile()))
            onResult(project.getParentFile().list(), Util.isOpenable(file.getName()));
    }

    @Override
    public void onResult(String[] strings, boolean addfile) {
        SparseBooleanArray checked = list.getCheckedItemPositions();
        for (int i = 0; i < checked.size(); ++i) {
            list.setItemChecked(checked.keyAt(i), false);
        }
        if (projectData.size() > 0) {
            files = Arrays.copyOf(strings, strings.length + projectData.size());
            for (int i = 0; i < projectData.size(); ++i) {
                files[strings.length + i] = projectData.get(i);
            }
        } else files = strings;
        Arrays.sort(files);
        adapter.notifyDataSetChanged();
        for (String file : files) {
            if (Util.isOpenable(file)) {
                if (addfile) new ScanProjectModeTask(this).execute();
                return;
            }
        }
        Snackbar.make(this, "没发现代码文件", Snackbar.LENGTH_SHORT).setAction(R.string.create_new, this).show();
    }

    @Override
    public byte[] getCleanCommand() {
        String command = projectData.shell_cleanup;
        command = new BuildParameter(((MainActivity) getContext()).main, PreferenceManager.getDefaultSharedPreferences(getContext()), command, tempBin, "", project.getParent(), projectData.run_mode).invoke().get();
        return command.getBytes();
    }

    @Override
    public byte[] getMakeCommand() {
        String command = projectData.shell_command;
        String outputBin = projectData.output_bin;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        outputBin = new BuildParameter(((MainActivity) getContext()).main, preferences, outputBin, tempBin, "", project.getParent(), projectData.run_mode).invoke().get();
        if (!TextUtils.isEmpty(outputBin)) {
            if (!outputBin.startsWith("/data")) {
                command += "\nbusybox cp " + outputBin + " " + getOutputName();
                command += "\nchmod 700 " + getOutputName();
            }
        }
        command = new BuildParameter(((MainActivity) getContext()).main, preferences, command, tempBin, "", project.getParent(), projectData.run_mode).invoke().get();
        Log.e(TextWarriorApplication.TAG, command);
        return command.getBytes();
    }

    @Override
    public String getOutputName() {
        return tempBin;
    }

    public void onErrors(String s) {
        new AlertDialog.Builder(getContext()).setMessage(s).show();
    }

    @Override
    public void onProgressUpdate(String[] values) {
        if (toast == null) toast = Toast.makeText(getContext(), "", Toast.LENGTH_SHORT);
        for (String i : values) {
            toast.setText(i);
        }
        toast.show();
    }

    @Override
    public void onResult(String result, String errString, int code) {
        if (getContext() instanceof MainActivity) {
            Main tmpMain = ((MainActivity) getContext()).main;
            tmpMain.lastResultString = result;
            tmpMain.lastErrorString = errString;
        }
        if (!TextUtils.isEmpty(errString)) {
            onErrors(errString);
        } else if (code == 0 && (result.equals(getOutputName())) || result.startsWith("make: Nothing")) {
            onResult(project.getParentFile().list(), false);
            if (runIfDone) {
                run();
                runIfDone = false;
            } else {
                Snackbar.make(getRootView().findViewById(isShown() ? R.id.drawer_layout : R.id.textField), result, Snackbar.LENGTH_SHORT).setAction("运行", runAction).show();
            }
        } else {
            onResult(project.getParentFile().list(), false);
            MainActivity.toastMakeText(getRootView().findViewById(isShown() ? R.id.drawer_layout : R.id.textField), TextUtils.isEmpty(result) ? "执行结束" : result);
        }
    }

    @Override
    public String getEditing() {
        Tabs tabs = (Tabs) getRootView().findViewById(R.id.tabs);
        return tabs.getCurrentTag();
    }

    @Override
    public String[] getOptions() {
        if (getContext() instanceof MainActivity) {
            return ((MainActivity) getContext()).clang.getOptions();
        }
        return new String[]{"-std=c99"};
    }

    @Override
    public Project getProject() {
        return projectData;
    }

    @Override
    public File getProjectFile() {
        return project;
    }

    @Override
    public String[] listMainFile() {
        int cnt = adapter.getCount();
        ArrayList<String> lst = new ArrayList<>();
        for (int i = 0; i < cnt; ++i) {
            String file = adapter.getItem(i).toString();
            if (Clang.isMainCode(file)) {
                lst.add(file.startsWith("/") ? file : project.getParent() + File.separator + file);
            }
        }
        return lst.toArray(new String[lst.size()]);
    }

    @Override
    public void setModes(Set<String> modes, boolean cppmode, boolean hasBox2D) {
        mode = null;
        this.cppmode = cppmode;
        this.hasBox2D = hasBox2D;
        for (String m : modes) {
            if (TextUtils.isEmpty(mode)) mode = m;
            else mode += "/" + m;
        }
        if (TextUtils.isEmpty(mode)) mode = "Console";
        adapter.notifyDataSetChanged();
        if (!modes.contains(projectData.mode)) {
            if (modes.size() > 1)
                ChooseRunModeDialog.show((Activity) getContext(), modes);
            else setModes(mode);
        } else {
            setName(project.getParentFile().getName() + "(" + projectData.mode + ")");
            Toast.makeText(getContext(), "项目模式:" + mode, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void setModes(String result) {
        projectData.mode = result;
        projectData.appendChanged = true;
        setName(project.getParentFile().getName() + "(" + result + ")");
        Toast.makeText(getContext(), "执行模式:" + result, Toast.LENGTH_SHORT).show();
    }

    private void setName(String name) {
        Toolbar view = (Toolbar) findViewById(R.id.project_toolbar);
        view.setTitle(name);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (project == null || !project.exists()) {
            String projectPath = PreferenceManager.getDefaultSharedPreferences(getContext()).getString("project", null);
            if (!TextUtils.isEmpty(projectPath)) {
                File file = new File(projectPath);
                if (file.exists() && file.getName().equals(".cide")) {
                    open(file, false);
                }
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        save(true);
        super.onDetachedFromWindow();
    }

    private void onProjecChanged() {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        if (project != null) {
            editor.putString("project", project.getPath());
        } else {
            editor.remove("project");
        }
        editor.apply();
    }

    private void save(boolean closing) {
        if (projectData.save(project)) {
            if (closing) {
                Toast.makeText(getContext(), "项目已保存", Toast.LENGTH_SHORT).show();
            } else {
                MainActivity.toastMakeText(this, "项目已保存");
            }
        }
    }

    public void open(File file, boolean fromUser) {
        project = file;
        setName(file.getParentFile().getName());
        setDir(file.getParentFile().getPath());
        if (!projectData.load(project)) projectData = new Project();
        onResult(project.getParentFile().list(), true);
        File file1 = new File(tempBin);
        if (file1.exists()) {
            file1.setLastModified(0);
        }
        show();
        MainActivity.toastMakeText(this, "项目已打开");
        if (fromUser) onProjecChanged();
    }

    private void setDir(String path) {
        Toolbar view = (Toolbar) findViewById(R.id.project_toolbar);
        view.setSubtitle(path);
    }

    public void show() {
        if (!isShown()) {
            DrawerLayout drawer = (DrawerLayout) getRootView().findViewById(R.id.drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(GravityCompat.START);
                TabHost tabhost = (TabHost) getRootView().findViewById(R.id.tabhost);
                if (tabhost != null) {
                    tabhost.setCurrentTab(2);
                }
            }
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        ((Activity) getContext()).getMenuInflater().inflate(R.menu.project_item_edit, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        int cnt = list.getChildCount();
        switch (item.getItemId()) {
            case R.id.item_selectAll:
                for (int i = 0; i < cnt; ++i) {
                    if (!list.isItemChecked(i)) list.setItemChecked(i, true);
                }
                break;
            case R.id.item_invSelect:
                SparseBooleanArray checked = new SparseBooleanArray();
                for (int i = 0; i < cnt; ++i) {
                    if (!list.isItemChecked(i)) list.setItemChecked(i, true);
                    else checked.put(i, true);
                }
                for (int i = 0; i < checked.size(); ++i) {
                    if (checked.valueAt(i)) list.setItemChecked(checked.keyAt(i), false);
                }
                break;
            case R.id.item_delete:
                if (list.getCheckedItemCount() > 0) {
                    AskDeleteItemsDialog.show((Activity) getContext(), R.id.projects);
                }
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        String name = v.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Snackbar.make(this, "空的文件名" + name, Snackbar.LENGTH_SHORT).show();
            findViewById(R.id.project_create).setVisibility(GONE);
            return true;
        }
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        String ext = spinner.getSelectedItem().toString();
        if (ext.startsWith(".")) {
            if (!name.endsWith(ext)) name += ext;
        }
        File new_file = new File(project.getParentFile(), name);
        if (ext.equals("文件夹")) {
            if (new_file.mkdirs()) {
                findViewById(R.id.project_create).setVisibility(GONE);
                onResult(project.getParentFile().list(), false);
                return true;
            }
        } else {
            try {
                if (new_file.createNewFile()) {
                    findViewById(R.id.project_create).setVisibility(GONE);
                    Snackbar.make(this, "已创建:" + name, Snackbar.LENGTH_SHORT).show();
                    openFile(new_file.getPath());
                    onResult(project.getParentFile().list(), true);
                    return true;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        MainActivity.toastMakeText(this, "请重新输入文件名");
        return false;
    }

    public void openFile(String file) {
        Intent i = new Intent(getContext(), MainActivity.class);
        i.setAction(Intent.ACTION_VIEW);
        if (file.startsWith(File.separator)) {
            i.setData(Uri.fromFile(new File(file)));
        } else
            i.setData(Uri.fromFile(new File(project.getParentFile(), file)));
        getContext().startActivity(i);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        normalColor = SystemColorScheme.getAppColor(getContext(), R.color.foreground);
        Toolbar view = (Toolbar) findViewById(R.id.project_toolbar);
        view.inflateMenu(R.menu.project);
        view.setOnMenuItemClickListener(this);
        TextView text = (TextView) findViewById(R.id.file_name);
        text.setOnEditorActionListener(this);
        list = (ListView) findViewById(R.id.project_files);
        list.setMultiChoiceModeListener(this);
        list.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        list.setAdapter(adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return files.length;
            }

            @Override
            public Object getItem(int position) {
                return files[position];
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null)
                    convertView = View.inflate(parent.getContext(), android.R.layout.simple_list_item_multiple_choice, null);
                String file = files[position];
                TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
                if (file.equals(".cide")) file += "(" + mode + ")";
                textView.setText(file);
                File _file;
                if (file.startsWith(File.separator)) _file = new File(file);
                else _file = new File(project.getParentFile(), file);
                if (_file.isFile() && Util.isOpenable(file)) textView.setTextColor(Clang.mainColor);
                else textView.setTextColor(normalColor);
                return convertView;
            }
        });
        list.setOnItemClickListener(this);
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (Util.isOpenable(files[position])) {
            openFile(files[position]);
            DrawerLayout d = (DrawerLayout) getRootView().findViewById(R.id.drawer_layout);
            if (d != null) d.closeDrawers();
        } else if (files[position].equals(".cide")) {
            PopupMenu menu = new PopupMenu(getContext(), view);
            menu.inflate(R.menu.project);
            menu.setOnMenuItemClickListener(this);
            menu.show();
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.new_file:
                onClick(null);
                break;
            case R.id.build:
                build(false);
                break;
            case R.id.run:
                run();
                break;
            case R.id.cleanup:
                cleanup();
                break;
            case R.id.close:
                closeProject();
                break;
            case R.id.refresh:
                new ScanProjectModeTask(this).execute();
                Toast.makeText(getContext(), "正在检测项目类型...", Toast.LENGTH_SHORT).show();
                break;
            case R.id.save:
                save(false);
                break;
            case R.id.settings:
                if (project != null)
                    ProjectSettingDialog.show((Activity) getContext());
                break;
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        show();
        if (project == null) {
            Snackbar.make(this, "项目状态不可用", Snackbar.LENGTH_SHORT);
            return;
        }
        View view = findViewById(R.id.project_create);
        if (view.isShown()) view.setVisibility(GONE);
        else {
            view.setVisibility(VISIBLE);
            view.findViewById(R.id.file_name).requestFocus();
        }
    }

    public boolean build(boolean runIfDone) {
        this.runIfDone = runIfDone;
        if (project != null && project.exists()) {
            if (getContext() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getContext();
                activity.main.saveAll();
                if (projectData.build_mode.equals("auto")) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    String c_arg = preferences.getString("c_arg", activity.getString(R.string.c_arg)).trim();
                    String cxx_arg = preferences.getString("cxx_arg", activity.getString(R.string.cxx_arg)).trim();
                    String modeFlags = "";
                    int _mode = 0;
                    switch (projectData.mode) {
                        case "SDL":
                            modeFlags = " " + preferences.getString("sdl_arg", activity.getString(R.string.sdl_arg)).trim();
                            _mode = Main.MODE_SDL;
                            break;
                        case "SDL2":
                            modeFlags = " " + preferences.getString("sdl2_arg", activity.getString(R.string.sdl2_arg)).trim();
                            _mode = Main.MODE_SDL2;
                            break;
                        case "Native":
                            modeFlags = " " + preferences.getString("native_arg", activity.getString(R.string.native_arg)).trim();
                            _mode = Main.MODE_native;
                            break;
                    }
                    if (hasBox2D) {
                        modeFlags += " " + preferences.getString("box2d_arg", activity.getString(R.string.box2d_arg)).trim();
                    }
                    c_arg = c_arg.replace("(c4droid:SRC)", "").replace("(c4droid:BIN)", "").replace(" -o", "").split(" ", 2)[1];
                    cxx_arg = cxx_arg.replace("(c4droid:SRC)", "").replace("(c4droid:BIN)", "").replace(" -o", "").split(" ", 2)[1];
                    String ldFlags = (cppmode ? cxx_arg : c_arg) + modeFlags + " " + projectData.ldflags;
                    //ldFlags = ldFlags.split(" ", 2)[1];
                    String c_std = preferences.getString("c_std", activity.getString(R.string.c_std));
                    String cxx_std = preferences.getString("cxx_std", activity.getString(R.string.cxx_std));
                    ldFlags = new BuildParameter(((MainActivity) getContext()).main, preferences, ldFlags, tempBin, c_std, project.getParent(), _mode).invoke().get();
                    String cflags = c_arg + " " + projectData.cflags;
                    String cxxflags = cxx_arg + " " + projectData.cflags;
                    cflags = new BuildParameter(((MainActivity) getContext()).main, preferences, cflags, tempBin, c_std, project.getParent(), _mode).invoke().get();
                    cxxflags = new BuildParameter(((MainActivity) getContext()).main, preferences, cxxflags, tempBin, cxx_std, project.getParent(), _mode).invoke().get();
                    ldFlags = "\"LDFLAGS=" + ldFlags.trim() + "\" \"CFLAGS=" + cflags + "\" \"CXXFLAGS=" + cxxflags + "\"";
                    if (projectData.size() > 0) {
                        String addString = "\"MORE=";
                        for (String a : projectData.append) {
                            addString += a + " ";
                        }
                        addString = addString.trim() + "\"";
                        FileUtils.make(this, 0, "all", ldFlags, addString.trim());
                    } else
                        FileUtils.make(this, 0, "all", ldFlags);
                } else {
                    FileUtils.make(this, 2);
                }
                return true;
            }
        }
        return false;
    }

    public boolean run() {
        if (project == null || !project.exists()) return false;
        if (getContext() instanceof MainActivity) {
            ((MainActivity) getContext()).main.exec();
        }
        return true;
    }

    public boolean cleanup() {
        if (project != null) {
            if (projectData.build_mode.equals("auto")) {
                if (projectData.size() > 0) {
                    String addString = "\"MORE=";
                    for (String a : projectData.append) {
                        addString += a + " ";
                    }
                    addString = addString.trim() + "\"";
                    FileUtils.make(this, 1, "clean", addString.trim());
                } else
                    FileUtils.make(this, 1, "clean");
            } else {
                FileUtils.make(this, 3);
            }
        }
        return true;
    }

    private void closeProject() {
        if (files.length > 0) {
            SparseBooleanArray checked = list.getCheckedItemPositions();
            for (int i = 0; i < checked.size(); ++i) {
                list.setItemChecked(checked.keyAt(i), false);
            }
            files = new String[0];
            adapter.notifyDataSetChanged();
        }
        setName("未打开");
        setDir("请从浏览页打开或创建");
        project = null;
        onProjecChanged();
    }

    private class RunAction implements OnClickListener {
        @Override
        public void onClick(View v) {
            run();
        }
    }
}
