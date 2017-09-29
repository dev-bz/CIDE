package org.free.cide.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.widget.TabHost;

import org.free.cide.R;

public class StartTabHost extends TabHost {
    public StartTabHost(Context context) {
        super(context);
    }

    public StartTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StartTabHost(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StartTabHost(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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
        addTab(newTabSpec("listFiles").setIndicator("浏览").setContent(R.id.unused));
        addTab(newTabSpec("opened").setIndicator("历史").setContent(R.id.openedList));
        addTab(newTabSpec("projects").setIndicator("项目").setContent(R.id.projects));
        super.onFinishInflate();
    }

    @Override
    public void onTouchModeChanged(boolean isInTouchMode) {
        //super.onTouchModeChanged(isInTouchMode);
    }

}

