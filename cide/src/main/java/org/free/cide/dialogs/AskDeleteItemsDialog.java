package org.free.cide.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import org.free.cide.views.ProjectView;

/**
 * Created by Administrator on 2016/6/9.
 */
public class AskDeleteItemsDialog extends DialogFragment implements DialogInterface.OnClickListener {
    public static void show(Activity activity, int projectID) {
        AskDeleteItemsDialog dialog = new AskDeleteItemsDialog();
        Bundle args = new Bundle(1);
        args.putInt("projectID", projectID);
        dialog.setArguments(args);
        dialog.show(activity.getFragmentManager(), "ask_delete");
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        ProjectView proj = (ProjectView) getActivity().findViewById(getArguments().getInt("projectID"));
        proj.delete();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity()).setTitle("删除").setMessage("是否确定删除这些文件?").setPositiveButton(android.R.string.ok, this).create();
    }
}
