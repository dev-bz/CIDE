package org.free.cide.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.free.cide.R;

public class BottomSettingDialog extends DialogFragment {
    private static BottomSettingDialog fragment;

    public static void show(Activity activity) {
        FragmentManager fragmentManager = activity.getFragmentManager();
        BottomSettingDialog dialog = (BottomSettingDialog) fragmentManager.findFragmentByTag("temp");
        if (dialog == null) dialog = newInstance();
        dialog.show(fragmentManager, "temp");
    }

    public static BottomSettingDialog newInstance() {
        if (fragment == null) fragment = new BottomSettingDialog();
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings, container);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //FragmentManager fm = getFragmentManager();
        //getChildFragmentManager();
        //fm.beginTransaction().replace(R.id.setting_dialog, MainActivity.SettingFragment.newInstance()).commit();
        return new BottomSheetDialog(getActivity());
    }
}
