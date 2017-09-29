package com.termux.app;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

/**
 * Utility to make the touch keyboard and immersive mode work with full screen activities.
 * <p>
 * See https://code.google.com/p/android/issues/detail?id=5497
 */
final class FullScreenHelper implements ViewTreeObserver.OnGlobalLayoutListener {
    private final Activity mActivity;
    private final Rect mWindowRect = new Rect();
    private boolean mEnabled = false;

    public FullScreenHelper(Activity activity) {
        this.mActivity = activity;
    }

    @Override
    public void onGlobalLayout() {
        final View childViewOfContent = ((FrameLayout) mActivity.findViewById(android.R.id.content)).getChildAt(0);
        if (mEnabled) setImmersiveMode();
        childViewOfContent.getWindowVisibleDisplayFrame(mWindowRect);
        int usableHeightNow = Math.min(mWindowRect.height(), childViewOfContent.getRootView().getHeight());
        FrameLayout.LayoutParams layout = (LayoutParams) childViewOfContent.getLayoutParams();
        if (layout.height != usableHeightNow) {
            layout.height = usableHeightNow;
            childViewOfContent.requestLayout();
        }
    }

    public void setImmersive(boolean enabled) {
        Window win = mActivity.getWindow();
        if (enabled == mEnabled) {
            if (!enabled) win.setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            return;
        }
        mEnabled = enabled;
        final View childViewOfContent = ((FrameLayout) mActivity.findViewById(android.R.id.content)).getChildAt(0);
        if (enabled) {
            win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setImmersiveMode();
            childViewOfContent.getViewTreeObserver().addOnGlobalLayoutListener(this);
        } else {
            win.setFlags(0, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            win.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                childViewOfContent.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } else {
                childViewOfContent.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }
            ((LayoutParams) childViewOfContent.getLayoutParams()).height = ViewGroup.LayoutParams.MATCH_PARENT;
        }
    }

    private void setImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            mActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    }
}
