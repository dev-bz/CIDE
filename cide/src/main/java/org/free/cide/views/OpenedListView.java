package org.free.cide.views;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import org.free.cide.R;
import org.free.cide.ide.MainActivity;
import org.free.cide.utils.Util;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class OpenedListView extends ListView implements AdapterView.OnItemClickListener, AdapterView.OnItemLongClickListener {
    private Callback callback;
    private ArrayList<MyFile> files = new ArrayList<>();
    private BaseAdapter mAdapter;

    public OpenedListView(Context context) {
        super(context);
    }

    public OpenedListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public OpenedListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OpenedListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDetachedFromWindow() {
        SharedPreferences.Editor e = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        LinkedHashSet<String> tmp = new LinkedHashSet<>();
        for (MyFile i : files) tmp.add(i.path);
        e.putStringSet("opened", tmp).apply();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onFinishInflate() {
        init();
        super.onFinishInflate();
    }

    private void init() {
        Set<String> opened = PreferenceManager.getDefaultSharedPreferences(getContext()).getStringSet("opened", null);
        if (null != opened) {
            for (String i : opened) {
                if (new File(i).exists())
                    files.add(new MyFile(i));
            }
            if (files.size() > 1) Collections.sort(files, new Comp());
        }
        setAdapter(mAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return files.size();
            }

            @Override
            public Object getItem(int position) {
                return files.get(position);
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = ViewGroup.inflate(getContext(), android.R.layout.simple_list_item_2, null);
                    ((TextView) convertView.findViewById(android.R.id.text2)).setGravity(Gravity.END);
                }
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(files.get(position).name);
                String modifiedDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(files.get(position).lastModified);
                ((TextView) convertView.findViewById(android.R.id.text2)).setText(modifiedDate);
                return convertView;
            }
        });
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
        //shouldDelete = new ShouldDelete();
        //new FileAsyncTask().execute();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        select(files.get(position));
    }

    private void select(MyFile file) {
        if (Util.isOpenable(file.name)) {
            Intent i = new Intent(getContext(), MainActivity.class);
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse(file.path));
            getContext().startActivity(i);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        new FilePopupMenu(view, files.get(position));
        return true;
    }

    public void opened(String filename) {
        if (!new File(filename).exists()) return;
        ArrayList<MyFile> tmp = new ArrayList<>(files);
        boolean bNeed = true;
        for (MyFile f : tmp) {
            if (f.path.equals(filename)) {
                bNeed = false;
                break;
            }
        }
        if (bNeed) tmp.add(new MyFile(filename));
        Collections.sort(tmp, new Comp());
        files = tmp;
        mAdapter.notifyDataSetChanged();
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void browse(String path);
    }

    private class MyFile {
        public final long lastModified;
        public final String name;
        public final String path;

        public MyFile(String filename) {
            File file = new File(filename);
            name = file.getName();
            path = file.getPath();
            lastModified = file.lastModified();
        }
    }

    private class Comp implements java.util.Comparator<MyFile> {
        @Override
        public int compare(MyFile lhs, MyFile rhs) {
            if (lhs.lastModified == rhs.lastModified) return 0;
            return lhs.lastModified < rhs.lastModified ? 1 : -1;
        }
    }

    class FilePopupMenu extends PopupMenu implements PopupMenu.OnMenuItemClickListener {
        private final MyFile file;

        public FilePopupMenu(View anchor, MyFile file) {
            super(getContext(), anchor);
            inflate(R.menu.opened);
            setOnMenuItemClickListener(this);
            this.file = file;
            show();
        }

        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.nav_goto:
                    callback.browse(file.path);
                    break;
                case R.id.nav_copyName: {
                    ClipboardManager m = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    m.setPrimaryClip(ClipData.newPlainText("path", file.name));
                }
                break;
                case R.id.nav_copy: {
                    ClipboardManager m = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                    m.setPrimaryClip(ClipData.newPlainText("path", file.path));
                }
                break;
                case R.id.nav_send: {
                    FileListView.send(new File(file.path), getContext());
                }
                break;
                case R.id.nav_delete:
                    //shouldDelete.setFile(file);
                    if (files.remove(file))
                        mAdapter.notifyDataSetChanged();
                    break;
            }
            return false;
        }
    }
}
