package org.free.cide.dialogs;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListView;

import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;

import org.free.cide.R;
import org.free.cide.adapters.FunctionListAdapter;
import org.free.cide.ide.Main;
import org.free.cide.ide.MainActivity;
import org.free.cide.ide.ViewFileActivity;

import java.io.File;

public class FunctionListDialog extends DialogFragment implements FunctionListAdapter.Callback, AdapterView.OnItemClickListener {
    public static void show(Activity context, String[] split) {
        FunctionListDialog dialog = new FunctionListDialog();
        Bundle bundle = new Bundle(1);
        bundle.putStringArray("list", split);
        dialog.setArguments(bundle);
        dialog.show(context.getFragmentManager(), "functionList");
    }

    @Override
    public String[] get(String str) {
        String[] split = str.split(" cursor: ");
        if (split.length > 1) {
            split[1] = split[1].split("↔")[0];
        }
        return split;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setGravity(Gravity.FILL);
        dialog.setContentView(createView());
        return dialog;
    }

    private View createView() {
        String[] list = getArguments().getStringArray("list");
        ListView view = new ListView(getActivity());
        view.setAdapter(new FunctionListAdapter(list, this, getActivity()));
        view.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Object text = parent.getItemAtPosition(position);
        if (text instanceof String) {
            FreeScrollingTextField textField = (FreeScrollingTextField) getActivity().findViewById(R.id.textField);
            if (textField != null) {
                String[] nn = ((String) text).split(":");
                String line_string = nn[1];
                String column_string = null;
                if (nn.length > 2) column_string = nn[2];
                if ((textField.isCurrent(nn[0]))) {
                    Main.gotoCursor(textField, line_string, column_string);
                } else {
                    String start = (view.getContext().getFilesDir().toString() + File.separator);
                    if (nn[0].startsWith(start)) {
                        Intent i = new Intent(view.getContext(), ViewFileActivity.class).setData(Uri.parse("file://" + nn[0]));
                        i.putExtra("back", textField.currentFile());
                        i.putExtra("line", line_string);
                        if (nn.length > 2) {
                            i.putExtra("column", column_string);
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                            Bundle b = ActivityOptions.makeSceneTransitionAnimation(getActivity(), view, "include").toBundle();
                            getActivity().startActivity(i, b);
                        } else {
                            getActivity().startActivity(i);
                        }
                    } else {
                        Intent i = new Intent(getActivity(), MainActivity.class).setData(Uri.parse("file://" + nn[0]));
                        i.setAction(Intent.ACTION_VIEW);
                        i.putExtra("back", textField.currentFile());
                        i.putExtra("line", line_string);
                        if (nn.length > 2) {
                            i.putExtra("column", column_string);
                        }
                        getActivity().startActivity(i);
                    }
                }
                dismiss();
            }
        }
    }
}
