package org.free.cide.callbacks;

import android.widget.TabHost;

import org.free.cide.views.Tabs;

public interface TabCtrl {
    void addTab(String tabTag);

    String getCurrentTag();

    void setCurrentTag(String tag);

    void left();

    void removeOthers(String tabTag);

    void removeTab(String tabTag);

    void right();

    void setCallback(Tabs.Callback onMenuItemClick);

    void setOnTabChangedListener(TabHost.OnTabChangeListener tabChangedListener);
}
