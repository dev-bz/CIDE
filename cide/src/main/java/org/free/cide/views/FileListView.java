package org.free.cide.views;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;

import org.free.cide.R;
import org.free.cide.ide.MainActivity;
import org.free.cide.utils.SystemColorScheme;
import org.free.cide.utils.Util;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileListView extends ListView implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener, TextView.OnEditorActionListener {
    private static final String LOG_TAG = "FileListView";
    private Callback callback;
    private File curDir;
    private File[] files = new File[0];
    private BaseAdapter mAdapter;
    private int nameColor;
    private int normalColor;
    private ShouldDelete shouldDelete;

    public FileListView(Context context) {
        super(context);
        init();
    }

    public FileListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FileListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FileListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    static public void send(File file, Context context) {
        if (!file.isDirectory()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            Uri data;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
//                data = FileProvider.getUriForFile(context, "org.free.cide.fileprovider", file);
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            } else
            {
                data = Uri.fromFile(file);
            }
            intent.addCategory(Intent.CATEGORY_DEFAULT);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            String extensionFromUrl = MimeTypeMap.getFileExtensionFromUrl(data.toString());
            if (extensionFromUrl == null) {
                intent.setType("application/*");
                intent.putExtra(Intent.EXTRA_STREAM, data);
            } else {
                String mt = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extensionFromUrl);
                if (mt == null) {
                    intent.setType("application/*");
                    intent.putExtra(Intent.EXTRA_STREAM, data);
                } else {
                    intent.setDataAndType(data, mt);
                }
            }
            ComponentName componentName = intent.resolveActivity(context.getPackageManager());
            if (componentName != null) {
                Log.v(LOG_TAG, "componentName" + componentName + "\n" + intent.getType());
                context.startActivity(intent);
            }
        }
    }

    private void init() {
        nameColor = SystemColorScheme.getAppColor(getContext(), R.color.keyword);
        normalColor = SystemColorScheme.getAppColor(getContext(), R.color.foreground);
        setAdapter(mAdapter = new BaseAdapter() {

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
                if (convertView == null) {
                    convertView = ViewGroup.inflate(getContext(), R.layout.simple_list_item_2, null);
                }
                File file = files[position];
                if (file == null) return convertView;
                TextView t = (TextView) convertView.findViewById(android.R.id.text1);
                TextView t2 = (TextView) convertView.findViewById(android.R.id.text2);
                ImageView m1 = (ImageView) convertView.findViewById(R.id.file_icon);
                ImageView m2 = (ImageView) convertView.findViewById(R.id.folder_next);
                if (position == 0 && curDir != null) {
                    t.setText("..");
                    t2.setText(curDir.getPath());
                    t2.setGravity(Gravity.START);
                    m1.setImageLevel(2);
                    t.setTextColor(normalColor);
                } else {
                    m1.setImageLevel(file.isDirectory() ? 1 : 3);
                    String name = file.getName();
                    if (file.exists()) {
                        if (name.equals(".cide")) {
                            t.setText("打开项目...");
                        } else {
                            t.setText(name);
                        }
                        String modifiedDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(file.lastModified());
                        t2.setText(modifiedDate);
                        if (file.isFile() && Util.isOpenable(name)) {
                            t.setTextColor(nameColor);
                        } else {
                            t.setTextColor(normalColor);
                        }
                    } else {
                        if (name.equals(".cide")) {
                            t.setText("新建项目...");
                            t2.setText("长按可进行其他操作");
                        } else {
                            t.setText(name);
                            t2.setText("受到系统限制");
                        }
                        t.setTextColor(normalColor);
                    }
                    t2.setGravity(Gravity.END);
                }
                m2.setVisibility(file.isDirectory() ? VISIBLE : INVISIBLE);
                return convertView;
            }
        });
        setOnItemClickListener(this);
        if (getId() == R.id.listFiles) setOnItemLongClickListener(this);
        shouldDelete = new ShouldDelete();
        //new FileAsyncTask().execute();
    }

    public void into(File file) {
        if (curDir != null && file.equals(curDir)) return;
        new FileAsyncTask().execute(file);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (curDir == null) {
            return true;
        }
        String name = v.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Snackbar.make(this, "空的文件名" + name, Snackbar.LENGTH_SHORT).show();
            showNewFile();
            return true;
        }
        Spinner spinner = (Spinner) ((ViewGroup) getParent()).findViewById(R.id.spinner);
        String ext = spinner.getSelectedItem().toString();
        if (ext.startsWith(".")) {
            if (!name.endsWith(ext)) name += ext;
        }
        if (ext.equals("文件夹")) {
            File new_file = new File(curDir, name);
            if (new_file.mkdirs()) {
                v.setText("");
                into(new_file);
            } else {
                MainActivity.toastMakeText(this, "请重新输入文件夹名");
                return true;
            }
        } else {
            File new_file = new File(curDir, name);
            try {
                if (new_file.createNewFile()) {
                    new FileAsyncTask().execute(curDir);
                    Snackbar.make(this, "已创建:" + name, Snackbar.LENGTH_SHORT).show();
                    if (callback != null) callback.select(new_file);
                }
            } catch (IOException e) {
                MainActivity.toastMakeText(this, "请重新输入文件名");
                return true;
            }
        }
        showNewFile();
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        File f = files[position];
        if (f == null) return;
        if (f.isDirectory()) new FileAsyncTask().execute(f);
            //else if(f.getName().equals(".cide") && !f.exists()){}
        else if (callback != null) callback.select(f);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        File file = files[position];
        if (file == null) return true;
        new FilePopupMenu(view, file);
        //pop.show();
        return true;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        String ct = curDir == null ? "" : curDir.getPath();
        return new Tabs.SavedState(parcelable, files.length + ":" + ct);
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
        Tabs.SavedState s = (Tabs.SavedState) state;
        super.onRestoreInstanceState(s.getSuperState());
        String[] ss = s.ct.split(":");
        if (ss.length == 2) {
            int count = Integer.parseInt(ss[0]);
            if (count > 0) {
                files = new File[count];
                if (!ss[1].isEmpty()) {
                    curDir = new File(ss[1]);
                }
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String baseDir = preferences.getString("baseDir", "");
        if (!baseDir.isEmpty()) {
            curDir = new File(baseDir);
            if (!curDir.exists()) curDir = null;
        }
        if (curDir == null) gotoBaseDir();
        else new FileAsyncTask().execute(curDir);
        super.onAttachedToWindow();
    }

    public void gotoBaseDir() {
        new FileAsyncTask().execute();
    }

    @Override
    protected void onDetachedFromWindow() {
        String cur = curDir == null ? "" : curDir.getPath();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String baseDir = preferences.getString("baseDir", "");
        if (!baseDir.equals(cur)) {
            preferences.edit().putString("baseDir", cur).apply();
        }
        super.onDetachedFromWindow();
    }

    public void setCallback(FileListView.Callback callback) {
        this.callback = callback;
    }

    public void setColor(int color) {
        nameColor = color;
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    public void showNewFile() {
        if (curDir == null) return;
        View view = ((ViewGroup) getParent()).findViewById(R.id.file_create);
        if (view.isShown()) view.setVisibility(GONE);
        else {
            show();
            view.setVisibility(VISIBLE);
            TextView text = (TextView) view.findViewById(R.id.file_name);
            text.requestFocus();
            text.setOnEditorActionListener(this);
        }
    }

    private void show() {
        if (!isShown()) {
            DrawerLayout drawer = (DrawerLayout) getRootView().findViewById(R.id.drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(GravityCompat.START);
                TabHost tabhost = (TabHost) getRootView().findViewById(R.id.tabhost);
                if (tabhost != null) {
                    tabhost.setCurrentTab(0);
                }
            }
        }
    }

    public interface Callback {
        void addToProjects(File path);

        void onFileDeleted(File file);

        void select(File file);

        boolean isProjectClosed();
    }

    class FileAsyncTask extends AsyncTask<File, Object, File[]> {
        private boolean bNeed;
        private File curDir;
        private File dataDir;

        private void addFiles(List<File> result, List<File> result_files, File[] files) {
            if (files == null) return;
            for (File file : files) {
                if (!file.isHidden() && file.canRead()) {
                    if (file.isDirectory())
                        result.add(file);
                    else result_files.add(file);
                } else if (file.getName().equals(".cide")) {
                    result.add(file);
                    bNeed = false;
                }
            }
        }

        @Override
        protected File[] doInBackground(File... params) {
            List<File> result = new ArrayList<>();
            List<File> result_files = new ArrayList<>();
            bNeed = true;
            boolean addBase = false;
            if (params.length == 0) {
                File[] r = File.listRoots();
                for (File p : r) {
                    addFiles(result, result_files, p.listFiles());
                }

                addBase = true;
            } else for (File p : params) {
                File parentFile = p;
                parentFile = parentFile.getParentFile();
                ///while (parentFile != null && !parentFile.canRead());
                if (parentFile != null) {
                    result.add(parentFile);
                    curDir = p;
                } else addBase = true;
                if (p.canRead()) addFiles(result, result_files, p.listFiles());
            }
            if (result.size() > 0) Collections.sort(result);
            if (result_files.size() > 0) Collections.sort(result_files);
            result.addAll(result_files);
            if (curDir != null && curDir.canWrite()) {
                if (bNeed && result.size() > 0) {
                    result.add(1, new File(curDir, ".cide"));
                }
            } else if (addBase) {
                if (result.size() == 0) {
                    result.add(Environment.getDataDirectory());
                    result.add(Environment.getRootDirectory());
                    result.add(Environment.getExternalStorageDirectory());
                    result.add(new File("/sdcard"));
                }
                result.add(0, dataDir);
            }
            return result.toArray(new File[result.size()]);
        }

        @Override
        protected void onPreExecute() {
            setEnabled(false);
            dataDir = getContext().getFilesDir();
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(File[] files) {
            FileListView.this.files = files;
            FileListView.this.curDir = curDir;
            if (callback != null) callback.select(curDir);
            mAdapter.notifyDataSetChanged();
            setEnabled(true);
        }
    }

    private class FilePopupMenu extends PopupMenu implements PopupMenu.OnMenuItemClickListener {
        private final File file;

        FilePopupMenu(View anchor, File file) {
            super(getContext(), anchor);
            inflate(R.menu.activity_main_drawer);
            if (FileListView.this.getId() != R.id.listFiles) {
                this.getMenu().findItem(R.id.new_file).setVisible(false);
                this.getMenu().findItem(R.id.nav_delete).setVisible(false);
            }
            if (callback == null || callback.isProjectClosed()) {
                getMenu().findItem(R.id.add_to_project).setEnabled(false);
            }
            setOnMenuItemClickListener(this);
            this.file = file;
            show();
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.new_file:
                    showNewFile();
                    break;
                case R.id.nav_copyName: {
                    ClipboardManager m = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    m.setPrimaryClip(ClipData.newPlainText("path", file.getName()));
                }
                break;
                case R.id.add_to_project:
                    if (callback != null) callback.addToProjects(file);
                    break;
                case R.id.nav_copy: {
                    ClipboardManager m = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    m.setPrimaryClip(ClipData.newPlainText("path", file.getPath()));
                }
                break;
                case R.id.nav_goto: {
                    ClipboardManager m = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = m.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        String st = clip.getItemAt(0).getText().toString();
                        if (!st.isEmpty()) {
                            File f = new File(st);
                            while (f != null && f.exists() && f.isFile()) f = f.getParentFile();
                            if (f != null && f.isDirectory()) new FileAsyncTask().execute(f);
                        }
                    }
                }
                break;
                case R.id.nav_send:
                    send(file, getContext());
                    break;
                case R.id.nav_delete:
                    shouldDelete.setFile(file);
                    Snackbar.make(FileListView.this, "准备删除文件:" + file.getName(), Snackbar.LENGTH_SHORT)
                            .setAction("确定", shouldDelete).show();
                    break;
            }
            return false;
        }
    }

    private class ShouldDelete implements OnClickListener {
        private File file;

        @Override
        public void onClick(View v) {
            if (file != null && file.delete()) {
                callback.onFileDeleted(file);
                new FileAsyncTask().execute(curDir);
            }
            file = null;
        }

        public void setFile(File file) {
            this.file = file;
        }
    }
}
