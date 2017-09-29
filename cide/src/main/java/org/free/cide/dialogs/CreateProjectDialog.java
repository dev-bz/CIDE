package org.free.cide.dialogs;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.free.cide.R;
import org.free.cide.ide.MainActivity;
import org.free.cide.views.ProjectView;

import java.io.File;
import java.io.IOException;

public class CreateProjectDialog extends DialogFragment implements TextView.OnEditorActionListener {
    private TextInputLayout inputLayout;

    public static void show(MainActivity activity, File current) {
        CreateProjectDialog dialog = new CreateProjectDialog();
        Bundle bundle = new Bundle(1);
        bundle.putString("dir", current.getPath());
        dialog.setArguments(bundle);
        dialog.show(activity.getFragmentManager(), "createProjectDialog");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("创建项目");
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View view1 = getView();
        if (view1 != null) {
            View view = view1.findViewById(R.id.project_name);
            if (view instanceof TextView)
                ((TextView) view).setOnEditorActionListener(this);
            inputLayout = (TextInputLayout) view1.findViewById(R.id.inputLayout);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.project_name_dialog, container);
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        String name = v.getText().toString().trim();
        File file = new File(getArguments().getString("dir"), name);
        String[] list = file.list();
        if (file.exists() && !TextUtils.isEmpty(name) && (null != list && list.length > 0)) {
            inputLayout.setError("项目名已被占用");
            return false;
        }
        if (file.exists() || file.mkdir()) {
            File project = new File(file, ".cide");
            try {
                if (project.createNewFile()) {
                    dismiss();
                    ProjectView p = (ProjectView) getActivity().findViewById(R.id.projects);
                    p.open(project, true);
                    return true;
                }
            } catch (IOException e) {
                inputLayout.setError("创建项目失败");
                e.printStackTrace();
            }
        } else {
            inputLayout.setError("创建项目目录失败");
        }
        return false;
    }
}
