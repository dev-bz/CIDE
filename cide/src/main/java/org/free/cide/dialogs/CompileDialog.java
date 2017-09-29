package org.free.cide.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;

import org.free.cide.ide.Compile;

/**
 * Created by Administrator on 2016/6/29.
 */
public class CompileDialog extends DialogFragment implements DialogInterface.OnClickListener {
    private Compile compile;

    public static CompileDialog show(Activity activity, Compile compile) {
        CompileDialog dlg = new CompileDialog();
        dlg.compile = compile;
        dlg.show(activity.getFragmentManager(), "compile");
        return dlg;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        compile.destroy();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dlg = new ProgressDialog(getActivity());
        dlg.setTitle("编译中");
        dlg.setMessage("编译时间取决于项目大小,请稍等...");
        dlg.setCancelable(false);
        dlg.setCanceledOnTouchOutside(false);
        dlg.setButton(Dialog.BUTTON_NEGATIVE, "终止", this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dlg.create();
        }
        return dlg;
    }
}
