package org.free.cide.callbacks;

import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;

import org.free.cide.ide.CodeHelp;

/**
 * Created by Administrator on 2016/6/27.
 */
public interface PopupList {
    void onLayout();

    void post(String[] data, int count, int startOffset, String footerText, String filterString, CodeHelp codeHelp);

    void show();

    void yes(FreeScrollingTextField editField);
}
