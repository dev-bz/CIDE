package org.free.cide.views;

import android.content.Context;
import android.view.Gravity;
import android.widget.ListView;
import android.widget.TextView;

public class MyListView extends ListView {
    private TextView foot;
    private boolean removed;

    public MyListView(Context context) {
        super(context);
        if (foot == null) {
            foot = new TextView(getContext());
            foot.setGravity(Gravity.CENTER);
            addHeaderView(foot);
            foot.setVisibility(GONE);
            removed = true;
        }
        // to get the available height for the whole window
    }

    @Override
    public boolean isFocused() {
        return true;
    }

    @Override
    public boolean hasFocus() {
        return true;
    }

    @Override
    public boolean hasWindowFocus() {
        return true;
    }

    public void removeFooter() {
        removed = true;
        foot.setVisibility(GONE);
    }

    public void setFooterText(String text) {
        foot.setText(text.split("↔")[0]);
        if (removed) {
            foot.setVisibility(VISIBLE);
            removed = false;
        }
    }
}
