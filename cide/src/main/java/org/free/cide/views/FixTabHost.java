package org.free.cide.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.TabHost;

/**
 * Created by MainUser on 2016/12/4.
 */

public class FixTabHost extends TabHost {
    public FixTabHost(Context context) {
        super(context);
    }

    public FixTabHost(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FixTabHost(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FixTabHost(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onTouchModeChanged(boolean isInTouchMode) {
    }
}
