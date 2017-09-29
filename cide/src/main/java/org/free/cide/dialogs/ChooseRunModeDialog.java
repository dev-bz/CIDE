package org.free.cide.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import org.free.cide.R;
import org.free.cide.callbacks.ProjectCallback;

import java.util.Set;

/**
 * Created by Administrator on 2016/6/11.
 */
public class ChooseRunModeDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public static void show(Activity activity, Set<String> modes) {
        ChooseRunModeDialog dialog = new ChooseRunModeDialog();
        Bundle args = new Bundle(1);
        args.putStringArray("list", modes.toArray(new String[modes.size()]));
        dialog.setArguments(args);
        dialog.show(activity.getFragmentManager(), "chooseRunMode");
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        ProjectCallback p = (ProjectCallback) getActivity().findViewById(R.id.projects);
        String[] lists = getArguments().getStringArray("list");
        if (lists != null) {
            p.setModes(lists[which]);
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setTitle("选择一种模式").setItems(getArguments().getStringArray("list"), this).create();
    }
}
