package org.free.cide.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TabHost;
import android.widget.TextView;

import org.free.cide.R;
import org.free.cide.dialogs.NewShortCutStringDialog;
import org.free.cide.ide.MainActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

public class EndTabHost extends TabHost {
    public static Comparator<? super String> sort = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            String[] split = o1.split("↔");
            if (split.length == 2) o1 = split[1];
            else o1 = "";
            split = o2.split("↔");
            if (split.length == 2) o2 = split[1];
            else o2 = "";
            return o1.compareToIgnoreCase(o2);
        }
    };

    public EndTabHost(Context context) {
        super(context);
    }

    public EndTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EndTabHost(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public EndTabHost(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onDetachedFromWindow() {
        saveSortString();
        super.onDetachedFromWindow();
    }

    private void saveSortString() {
        ListView listView = (ListView) findViewById(R.id.help);
        ListAdapter adapter = listView.getAdapter();
        if (adapter instanceof SortAdapter) {
            Set<String> sortString = ((SortAdapter) adapter).getItems();
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit().putStringSet("sortString", sortString).apply();
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        String ct = getCurrentTabTag();
        return new Tabs.SavedState(parcelable, ct);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        Tabs.SavedState s = (Tabs.SavedState) state;
        super.onRestoreInstanceState(s.getSuperState());
        setCurrentTabByTag(s.ct);
    }

    @Override
    protected void onFinishInflate() {
        setup();
        addTab(newTabSpec("Functions").setIndicator("诊断/导航").setContent(R.id.listfunc));
        addTab(newTabSpec("Includes").setIndicator("关联文件").setContent(R.id.include));
        addTab(newTabSpec("Help").setIndicator(getContext().getString(R.string.snippet)).setContent(R.id.help));
        super.onFinishInflate();
        ListView listView = (ListView) findViewById(R.id.help);
        if (listView != null) {
            Set<String> load = PreferenceManager.getDefaultSharedPreferences(getContext()).getStringSet("sortString", null);
            if (load == null) load = new LinkedHashSet<>();
            int realSize = load.size();
            if (realSize == 0) {
                load.add(getContext().getString(R.string.click_to_add_snippet));
            }
            ArrayList<String> array = new ArrayList<>();
            for (String i : load) {
                array.add(i);
            }
            Collections.sort(array, sort);
            SortAdapter adapter = new SortAdapter(array.toArray(new String[array.size()]), realSize);
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(adapter);
            //listView.setOnItemLongClickListener(adapter);
        }
    }

    @Override
    public void onTouchModeChanged(boolean isInTouchMode) {
        //super.onTouchModeChanged(isInTouchMode);
    }

    public class SortAdapter extends BaseAdapter implements /*AdapterView.OnItemLongClickListener, */AdapterView.OnItemClickListener, PopupMenu.OnMenuItemClickListener {
        private boolean canotPaste;
        private String[] finalSort;
        private int popMenuPosition;

        public SortAdapter(String[] finalSort, int realSize) {
            this.finalSort = finalSort;
            this.canotPaste = realSize == 0;
        }

        @Override
        public int getCount() {
            return finalSort.length;
        }

        @Override
        public Object getItem(int position) {
            return finalSort[position];
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getContext(), android.R.layout.simple_list_item_2, null);
                convertView.findViewById(android.R.id.text2).setEnabled(false);
            }
            String[] split = finalSort[position].split("↔");
            TextView text = (TextView) convertView.findViewById(android.R.id.text2);
            text.setText(split[0]);
            text = (TextView) convertView.findViewById(android.R.id.text1);
            if (split.length == 2) {
                text.setText(split[1]);
            } else text.setText(R.string.no_name);
            return convertView;
        }

        public Set<String> getItems() {
            LinkedHashSet<String> set = new LinkedHashSet<>();
            if (!canotPaste) Collections.addAll(set, finalSort);
            return set;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
      /*if (canotPaste) {
        onItemLongClick(parent, view, position, id);
        return;
      }
      Context context = getContext();
      if (context instanceof Activity) {
        EditField edit = (EditField) ((Activity) context).findViewById(R.id.textField);
        if (edit != null) {
          String[] split = finalSort[position].split("↔");
          edit.paste(split[0]);
          DrawerLayout drawerLayout = (DrawerLayout) ((Activity) context).findViewById(R.id.drawer_layout);
          if (drawerLayout != null) drawerLayout.closeDrawers();
        }
      }*/
            PopupMenu pop = new PopupMenu(getContext(), view);
            pop.inflate(R.menu.sort);
            pop.setOnMenuItemClickListener(this);
            popMenuPosition = position;
            pop.show();
        }

        /*    @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
              PopupMenu pop = new PopupMenu(getContext(), view);
              pop.inflate(R.menu.sort);
              pop.setOnMenuItemClickListener(this);
              pop.show();
              popMenuPosition = position;
              return true;
            }*/
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Context context = getContext();
            switch (item.getItemId()) {
                case R.id.shortcut_create:
                    NewShortCutStringDialog.show(-1, this, (Activity) getContext());
                    break;
                case R.id.shortcut_delete:
                    delete(popMenuPosition);
                    break;
                case R.id.shortcut_modify:
                    NewShortCutStringDialog.show(canotPaste ? -1 : popMenuPosition, this, (Activity) getContext());
                    break;
                case R.id.shortcut_copy:
                    if (!canotPaste) {
                        ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                        cm.setPrimaryClip(ClipData.newPlainText("shortString", finalSort[popMenuPosition].split("↔")[0]));
                        MainActivity.toastMakeText(EndTabHost.this, "已复制");
                    }
                    break;
//        case R.id.shortcut_paste:
//          ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
//          ClipData clip = cm.getPrimaryClip();
//          if (clip.getItemCount() > 0) {
//            add(-1, clip.getItemAt(0).getText().toString());
//          }
//          break;
            }
            return true;
        }

        private void delete(int position) {
            if (finalSort.length == 1) {
                finalSort[0] = getContext().getString(R.string.click_to_add_snippet);
                canotPaste = true;
            } else {
                String[] tmp = Arrays.copyOf(finalSort, finalSort.length - 1);
                System.arraycopy(finalSort, position + 1, tmp, position, tmp.length - position);
                finalSort = tmp;
            }
            saveSortString();
            notifyDataSetChanged();
        }

        public void add(int position, String text) {
            if (position >= 0 && position < finalSort.length) {
                finalSort[position] = text;
            } else {
                if (canotPaste) {
                    finalSort[0] = text;
                } else {
                    for (String e : finalSort) {
                        if (e.equals(text)) return;
                    }
                    String[] strings = Arrays.copyOf(finalSort, finalSort.length + 1);
                    strings[finalSort.length] = text;
                    finalSort = strings;
                }
            }
            ArrayList<String> array = new ArrayList<>();
            Collections.addAll(array, finalSort);
            Collections.sort(array, sort);
            finalSort = array.toArray(finalSort);
            notifyDataSetChanged();
            saveSortString();
            canotPaste = false;
        }
    }
}
