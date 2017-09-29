package org.free.cide.callbacks;

import org.free.cide.ide.CodeHelp;

/**
 * Created by Administrator on 2016/6/21.
 */
public interface CodeCompilationDialogCallback {
    void codeCompilationNotify(int count, String[] data, int lineStart, String text, String filterString, CodeHelp codeHelp);
}
