package org.free.cide.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.free.cide.R;
import org.free.cide.views.EndTabHost;

public class NewShortCutStringDialog extends DialogFragment implements View.OnClickListener {
    public static void show(int position, EndTabHost.SortAdapter adapter, Activity activity) {
        NewShortCutStringDialog dialog = new NewShortCutStringDialog();
        Bundle arg = new Bundle(2);
        arg.putInt("position", position);
        if (position >= 0) arg.putString("string", adapter.getItem(position).toString());
        dialog.setArguments(arg);
        dialog.show(activity.getFragmentManager(), "newShortString");
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.setTitle("快捷代码");
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.short_string_dialog, container);
        TextView tView = (TextView) view.findViewById(R.id.short_string_name);
        String string = getArguments().getString("string");
        if (!TextUtils.isEmpty(string)) {

            String[] split = string.split("↔");
            tView.setText(split[0]);
            if (split.length == 2) {
                tView = (TextView) view.findViewById(R.id.short_string_code);
                tView.setText(split[1]);
            }
        }
//    tView.setOnEditorActionListener(this);
        View btn = view.findViewById(R.id.save);
        btn.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        TextView view = (TextView) getDialog().findViewById(R.id.short_string_code);
        String code = view.getText().toString().trim();
        view = (TextView) getDialog().findViewById(R.id.short_string_name);
        String text = view.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        if (!TextUtils.isEmpty(code)) {
            text += "↔" + code;
        }
        ListView lst = (ListView) getActivity().findViewById(R.id.help);
        ListAdapter a = lst.getAdapter();
        if (a instanceof EndTabHost.SortAdapter) {
            ((EndTabHost.SortAdapter) a).add(getArguments().getInt("position"), text);
        }
        dismiss();
    }
}
