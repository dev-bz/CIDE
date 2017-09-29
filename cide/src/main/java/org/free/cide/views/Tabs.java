package org.free.cide.views;

import android.app.ActionBar;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import org.free.cide.R;
import org.free.cide.callbacks.TabCtrl;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class Tabs extends LinearLayout implements TabCtrl, TabHost.OnTabChangeListener, View.OnClickListener {
    private final ArrayList<TabSpec> mTabSpecs = new ArrayList<>();
    private Tabs.Callback cb;
    private View editView;
    private final TabHost.TabContentFactory tabContentFactory = new TabHost.TabContentFactory() {
        @Override
        public View createTabContent(String p1) {
            return editView;
        }
    };
    //private PopupMenu.OnMenuItemClickListener menuClickListener;
    private TabHost.OnTabChangeListener tabChangeListener;
    private TabHost tabHost;
    private final View.OnLongClickListener longPress = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View tab) {
            delByLongPress(tab);
            return true;
        }
    };
    private String tabsLoad;
    private TextView title;

    public Tabs(Context context) {
        super(context);
    }

    public Tabs(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public Tabs(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void delByLongPress(View item) {
        if (tabHost == null) return;
        int tc = tabHost.getTabWidget().getTabCount();
        String d = tabHost.getCurrentTabTag();
        for (int i = 0; i < tc; ++i) {
            View v = tabHost.getTabWidget().getChildTabViewAt(i);
            if (v == item) {
                d = mTabSpecs.get(i).getTag();
            }
        }
        if (!d.equals("New")) {
            if (cb != null) cb.onRemoveTab(d);
            removeTab(d);
        }
    }

    private void addTab(TabHost.TabSpec ttt) {
        int lst = tabHost.getTabWidget().getTabCount();
        tabHost.addTab(ttt);
        View v = tabHost.getTabWidget().getChildTabViewAt(lst);
        //v.setFocusable(false);
        ViewGroup.LayoutParams layoutParams = v.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        if (v instanceof LinearLayout) {
            LinearLayout l = (LinearLayout) v;
            l.setMinimumWidth(0);
            for (int i = 0; i < l.getChildCount(); ++i) {
                View lAt = l.getChildAt(i);
                if (lAt instanceof TextView) {
                    //((TextView)lAt).setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    ((TextView) lAt).setMaxWidth(getResources().getDimensionPixelSize(R.dimen.tabwidth));
                    ((TextView) lAt).setSingleLine();
                    //} else if (lAt instanceof ImageView) {
                    //Log.e("Tabs",lAt.toString());
                }
            }
        }
        //if(layoutParams instanceof android.widget.LinearLayout.LayoutParams)
        //  ((android.widget.LinearLayout.LayoutParams)layoutParams).weight = 0.0f;
        v.setOnLongClickListener(longPress);
    /*if (!ttt.getTag().equals("New"))*/
        v.setOnClickListener(this);
        //v.setFocusable(false);
    }

    private void addTabWithOutNew(String i) {
        for (TabSpec anA : mTabSpecs) {
            if (anA.getTag().equals(i)) return;
        }
        CharSequence n = "New";
        if (!i.equals(n)) {
            n = new File(i).getName();
        }
        TabHost.TabSpec newTabSpec = tabHost.newTabSpec(i).setIndicator(n).setContent(tabContentFactory);
        mTabSpecs.add(newTabSpec);
        addTab(newTabSpec);
    }

    public boolean hasTag(String tag) {
        for (TabSpec i : mTabSpecs) {
            if (i.getTag().equals(tag)) return true;
        }
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getLayoutParams().height = ViewGroup.LayoutParams.MATCH_PARENT;
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());
        tabsLoad = p.getString("tabs", "New");
        String[] t = tabsLoad.split(File.pathSeparator);
        addTabs(Arrays.asList(t), p.getString("curr_tabs", null));
    }

    @Override
    protected void onDetachedFromWindow() {
        saveTabStatus();
        super.onDetachedFromWindow();
    }

    private void saveTabStatus() {
        if (tabsLoad == null) return;
        SharedPreferences.Editor p = PreferenceManager.getDefaultSharedPreferences(getContext()).edit();
        if (mTabSpecs.size() > 0) {
            String t = "";
            for (TabSpec i : mTabSpecs) {
                if (TextUtils.isEmpty(t)) t = i.getTag();
                else t += File.pathSeparator + i.getTag();
            }
            if (!t.equals(tabsLoad)) {
                tabsLoad = t;
                p.putString("tabs", t);
            }
            p.putString("curr_tabs", getCurrentTag()).apply();
        } else p.remove("tabs").apply();
    }

    private void addTabs(List<String> str, String tag) {
        tabHost.clearAllTabs();//tabHost.setFocusable(false);
        mTabSpecs.clear();
        if (str != null) {
            for (String i : str) {
                addTabWithOutNew(i);
            }
        }
        if (tag != null)
            tabHost.setCurrentTabByTag(tag);
        else
            tabHost.setCurrentTab(0);
    }

    @Override
    public void onClick(View tab) {
        if (tabHost.getCurrentTabView() != tab) {
            select(tab);
            return;
        }
        if (cb != null) cb.openMenu(tab, "New".equals(getCurrentTag()));
    }

    private void select(View tab) {
        if (tabHost == null) return;
        int tc = tabHost.getTabWidget().getTabCount();
        //String d=tabHost.getCurrentTabTag();
        for (int i = 0; i < tc; ++i) {
            View v = tabHost.getTabWidget().getChildTabViewAt(i);
            if (v == tab) {
                String d = mTabSpecs.get(i).getTag();
                tabHost.setCurrentTabByTag(d);
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable parcelable = super.onSaveInstanceState();
        String ct = tabHost.getCurrentTabTag();
        for (TabHost.TabSpec i : mTabSpecs) {
            ct += (":" + i.getTag());
        }
        return new SavedState(parcelable, ct);
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState s = (SavedState) state;
        super.onRestoreInstanceState(s.getSuperState());
        String[] str = s.ct.split(":", 2);
        String tag = str[0];
        str = str[1].split(":");
        addTabs(Arrays.asList(str), tag);
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public boolean hasFocus() {
        return false;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        View view = findViewById(R.id.barTitle);
        tabHost = (TabHost) findViewById(R.id.topTabHost);
        setTabs(view);
    }

    private void setTabs(View _sub) {
        editView = _sub;
        {
            ActionBar.LayoutParams pp = new ActionBar.LayoutParams(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.MATCH_PARENT);
            setLayoutParams(pp);
            tabHost.setup();
            tabHost.getTabWidget().setMeasureWithLargestChildEnabled(false);
            //tabHost.getTabWidget().setStripEnabled(true);
            //tabHost.getTabWidget().setDividerDrawable(null);
            title = (TextView) findViewById(R.id.barTitle);
        }
        if (tabHost != null) {
            tabHost.setOnTabChangedListener(this);
            //setOnTabChangedListener(tc);
            //for(iTag=0;iTag<4;++iTag)
            //addTabWithOutNew("New");
            HorizontalScrollView s = (HorizontalScrollView) tabHost.findViewById(R.id.mainHorizontalScrollView1);
            s.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    showTab();
                }
            });
        }
    }

    private void showTab() {
        onTabChanged(null);
    }

    @Override
    public void onTabChanged(String tabId) {
        Runnable n = new Runnable() {
            @Override
            public void run() {
                {
                    HorizontalScrollView s = (HorizontalScrollView) tabHost.findViewById(R.id.mainHorizontalScrollView1);
                    View currentTabView = tabHost.getCurrentTabView();
                    if (currentTabView != null) {
                        int m = currentTabView.getMeasuredWidth() / 2;
                        int x = currentTabView.getLeft() - (s.getMeasuredWidth() / 2 - m);
                        s.smoothScrollTo(x, 0);
                    }
                }
            }
        };
        tabHost.post(n);
        if (tabId != null) {
            saveTabStatus();
            if (tabChangeListener != null) tabChangeListener.onTabChanged(tabId);
        }
    }

    public void setTitle(String t) {
        title.setText(t);
    }

    public void left() {
        if (mTabSpecs.isEmpty()) return;
        String tag = tabHost.getCurrentTabTag();
        String left = null;
        for (TabSpec s : mTabSpecs) {
            if (s.getTag().equals(tag)) break;
            left = s.getTag();
        }
        if (left != null) {
            tabHost.setCurrentTabByTag(left);
        }
    }

    public void right() {
        if (mTabSpecs.isEmpty()) return;
        String tag = tabHost.getCurrentTabTag();
        String left = null;
        boolean find = false;
        for (TabSpec s : mTabSpecs) {
            if (find) {
                left = s.getTag();
                break;
            }
            if (s.getTag().equals(tag)) find = true;
        }
        if (left != null) {
            tabHost.setCurrentTabByTag(left);
        }
    }

    public String getCurrentTag() {
        return tabHost.getCurrentTabTag();
    }

    @Override
    public void setCurrentTag(String tag) {
        tabHost.setCurrentTabByTag(tag);
    }

    public void removeTab(String d) {
        TabHost.TabSpec forDel = null;
        //tabHost.setCurrentTab(0);
        tabHost.clearAllTabs();//tabHost.setFocusable(false);
        for (TabSpec ttt : mTabSpecs) {
            if (ttt.getTag().equals(d)) {
                forDel = ttt;
            } else
                addTab(ttt);
        }
        if (forDel != null) {
            int index = mTabSpecs.indexOf(forDel);
            mTabSpecs.remove(forDel);
            if (index == mTabSpecs.size()) --index;
            String sTag = "New";
            if (index < 0) {
                addTabWithOutNew(sTag);
            } else {
                sTag = mTabSpecs.get(index).getTag();
                tabHost.setCurrentTabByTag(sTag);
            }
        }
    }

    public void removeOthers(String d) {
        //tabHost.setCurrentTab(0);
        tabHost.clearAllTabs();//tabHost.setFocusable(false);
        mTabSpecs.clear();
        addTab(d);
    }

    public void addTab(String d) {
        Iterator<TabHost.TabSpec> i = mTabSpecs.iterator();
        TabHost.TabSpec forDel = null;
        while (i.hasNext()) {
            TabHost.TabSpec ttt = i.next();
            if (ttt.getTag().equals("New")) {
                forDel = ttt;
            }
        }
        if (forDel != null) {
            mTabSpecs.remove(forDel);
            //tabHost.setCurrentTab(0);
            tabHost.clearAllTabs();//tabHost.setFocusable(false);
            i = mTabSpecs.iterator();
            while (i.hasNext()) {
                TabHost.TabSpec ttt = i.next();
                addTab(ttt);
            }
        }
        addTabWithOutNew(d);
        tabHost.setCurrentTabByTag(d);
    }

    public void setOnTabChangedListener(TabHost.OnTabChangeListener tc) {
        tabChangeListener = tc;
    }

    public void setCallback(Callback tc) {
        cb = tc;
    }

    public interface Callback {
        void onRemoveTab(String d);

        void openMenu(View tab, boolean isNew);
    }

    /**
     * Created by Administrator on 2016/4/30.
     */
    static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR
                = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        public final String ct;

        public SavedState(Parcelable parcelable, String ct) {
            super(parcelable);
            this.ct = ct;
        }

        public SavedState(Parcel source) {
            super(source);
            ct = source.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeString(ct);
        }
    }
}
