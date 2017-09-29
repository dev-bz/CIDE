package org.free.cide.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import org.free.cide.R;
import org.free.cide.callbacks.ProjectCallback;
import org.free.cide.ide.Project;

/**
 * Created by Administrator on 2016/6/11.
 */
public class ProjectSettingDialog extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnShowListener, RadioGroup.OnCheckedChangeListener {
    public static void show(Activity context) {
        ProjectSettingDialog dialog = new ProjectSettingDialog();
        dialog.setStyle(STYLE_NORMAL, 0);
        dialog.show(context.getFragmentManager(), "project_setting");
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.mode_auto:
                getDialog().findViewById(R.id.layout_auto).setVisibility(View.VISIBLE);
                getDialog().findViewById(R.id.layout_user).setVisibility(View.GONE);
                break;
            case R.id.mode_user:
                getDialog().findViewById(R.id.layout_user).setVisibility(View.VISIBLE);
                getDialog().findViewById(R.id.layout_auto).setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        ProjectCallback p = (ProjectCallback) getActivity().findViewById(R.id.projects);
        Project pt = p.getProject();
        RadioGroup button = (RadioGroup) getDialog().findViewById(R.id.mode);
        pt.build_mode = button.getCheckedRadioButtonId() == R.id.mode_auto ? "auto" : "user";
        pt.cflags = ((TextView) getDialog().findViewById(R.id.flags)).getText().toString();
        pt.ldflags = ((TextView) getDialog().findViewById(R.id.ldflags)).getText().toString();
        pt.shell_command = ((TextView) getDialog().findViewById(R.id.shell_command)).getText().toString();
        pt.shell_cleanup = ((TextView) getDialog().findViewById(R.id.shell_cleanup)).getText().toString();
        pt.output_bin = ((TextView) getDialog().findViewById(R.id.output_bin)).getText().toString();
        button = (RadioGroup) getDialog().findViewById(R.id.mode_list);
        switch (button.getCheckedRadioButtonId()) {
            case R.id.mode_term:
                pt.run_mode = 0;
                break;
            case R.id.mode_sdl:
                pt.run_mode = 1;
                break;
            case R.id.mode_sdl2:
                pt.run_mode = 2;
                break;
            case R.id.mode_native:
                pt.run_mode = 3;
                break;
        }
        pt.appendChanged = true;
        pt.save(p.getProjectFile());
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle("项目设置").setPositiveButton(android.R.string.ok, this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(R.layout.project_settings_dialog);
        } else {
            builder.setView(View.inflate(getActivity(), R.layout.project_settings_dialog, null));
        }
        AlertDialog dialog = builder.create();
      /*dialog.setTitle("项目设置");
      dialog.setContentView(R.layout.project_settings_dialog);*/
        dialog.setOnShowListener(this);
        dialog.getWindow().setLayout(-1, -2);
        dialog.getWindow().setGravity(Gravity.FILL);
        return dialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ProjectCallback p = (ProjectCallback) getActivity().findViewById(R.id.projects);
        Project pt = p.getProject();
        Dialog dialog1 = getDialog();
        RadioGroup button = (RadioGroup) dialog1.findViewById(R.id.mode);
        button.setOnCheckedChangeListener(this);
        if (pt.build_mode.equals("auto")) {
            button.check(R.id.mode_auto);
        } else
            button.check(R.id.mode_user);
        ((TextView) dialog1.findViewById(R.id.flags)).setText(pt.cflags);
        ((TextView) dialog1.findViewById(R.id.ldflags)).setText(pt.ldflags);
        ((TextView) dialog1.findViewById(R.id.shell_command)).setText(pt.shell_command);
        ((TextView) dialog1.findViewById(R.id.shell_cleanup)).setText(pt.shell_cleanup);
        ((TextView) dialog1.findViewById(R.id.output_bin)).setText(pt.output_bin);
        button = (RadioGroup) dialog1.findViewById(R.id.mode_list);
        switch (pt.run_mode) {
            case 0:
                button.check(R.id.mode_term);
                break;
            case 1:
                button.check(R.id.mode_sdl);
                break;
            case 2:
                button.check(R.id.mode_sdl2);
                break;
            case 3:
                button.check(R.id.mode_native);
                break;
        }
    }
}
