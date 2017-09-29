package org.free.cide.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import org.free.cide.R;
import org.free.cide.ide.MainActivity;
import org.free.cide.views.FileListView;

import java.io.File;

public class PathDialog extends DialogFragment implements DialogInterface.OnClickListener, FileListView.Callback, DialogInterface.OnShowListener {
    public static final String REQUEST_CODE = "requestCode";
    public static final String SELECT_FILE = "SELECT_FILE";
    public static final String SELECT_PATH = "SELECT_PATH";
    private static PathDialog dialog;
    private File curDir;

    public static void show(MainActivity ctx) {
        if (PathDialog.dialog == null) PathDialog.dialog = new PathDialog();
        dialog.show(ctx.getFragmentManager(), "select_path");
    }

    @Override
    public void addToProjects(File path) {
    }

    @Override
    public void onFileDeleted(File file) {
    }

    @Override
    public void select(File file) {
        if (file == null || file.isDirectory()) {
            curDir = file;
            return;
        }
        Intent i = new Intent(getActivity(), getActivity().getClass());
        i.setAction(SELECT_PATH);
        i.putExtra("path", file.getPath());
        getActivity().startActivity(i);
        dismiss();
    }

    @Override
    public boolean isProjectClosed() {
        return false;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                if (curDir != null) {
                    Intent i = new Intent(getActivity(), getActivity().getClass());
                    i.setAction(SELECT_FILE);
                    i.putExtra("path", curDir.getPath() + File.separator);
                    getActivity().startActivity(i);
                    dismiss();
                }
                break;
            case Dialog.BUTTON_NEUTRAL:
                break;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity())
                .setTitle("Get Path").setPositiveButton("当前目录", this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dialog.setView(R.layout.path);
        } else {
            dialog.setView(View.inflate(getActivity(), R.layout.path, null));
        }
        AlertDialog alertDialog = dialog.create();
        alertDialog.setOnShowListener(this);
        return alertDialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        View view = getDialog().findViewById(R.id.select_file);
        if (view instanceof FileListView) {
            ((FileListView) view).setCallback(this);
        }
    }
}
