package com.myopicmobile.textwarrior;

import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;

/**
 * Created by Administrator on 2016/3/5.
 */
public interface EditCallback {
    void codeChange();

    void edited(FreeScrollingTextField editField);

    void formatLine(int caretRow);

    boolean onLayout();

    void popCodeCompletion();
}
