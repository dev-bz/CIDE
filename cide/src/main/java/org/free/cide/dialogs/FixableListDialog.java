package org.free.cide.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;
import com.myopicmobile.textwarrior.common.DocumentProvider;

import org.free.cide.R;
import org.free.cide.ide.Clang;
import org.free.cide.views.EditField;
import org.free.tools.Utils;

import java.util.ArrayList;

public class FixableListDialog extends DialogFragment implements AdapterView.OnItemClickListener {
    public static boolean dotNotFixit;
    private ArrayList<String> fixList;

    public static boolean show(Activity activity) {
        if (dotNotFixit) return false;
        FixableListDialog fixableListDialog = new FixableListDialog();
        EditField edit = (EditField) activity.findViewById(R.id.textField);
        if (edit != null) {
            DocumentProvider doc = edit.createDocumentProvider();
            int line = doc.findLineNumber(edit.getCaretPosition()) + 1;
            ArrayList<Clang.Diagnostic> errLines = edit.getErrLines();
            ArrayList<String> fixList = new ArrayList<>();
            for (Clang.Diagnostic i : errLines) {
                if (i.note == line) {
                    if (i.fix != null) fixList.add(i.fix);
                }
            }
            if (fixList.size() > 0) {
                Bundle bundle = new Bundle();
                bundle.putStringArrayList("fix", fixList);
                fixableListDialog.setArguments(bundle);
                fixableListDialog.show(activity.getFragmentManager(), "fixableList");
                return true;
            }
        }
        return false;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(createView());
        return dialog;
    }

    public View createView() {
        fixList = getArguments().getStringArrayList("fix");
        ListView view = new ListView(getActivity());
        view.setAdapter(new FixListAdapter());
        view.setOnItemClickListener(this);
        return view;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String text = (String) parent.getItemAtPosition(position);
        FreeScrollingTextField edit = (FreeScrollingTextField) getActivity().findViewById(R.id.textField);
        Utils.fixIt(text, edit);
        dismiss();
    }

    private class FixListAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return fixList.size();
        }

        @Override
        public Object getItem(int position) {
            return fixList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = View.inflate(parent.getContext(), android.R.layout.simple_list_item_2, null);
            String text = fixList.get(position);
            String[] s = text.split("\n");
            if (s.length == 2) {
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(s[1]);
                ((TextView) convertView.findViewById(android.R.id.text2)).setText(s[0]);
            } else {
                ((TextView) convertView.findViewById(android.R.id.text1)).setText("?");
                ((TextView) convertView.findViewById(android.R.id.text2)).setText(text);
            }
            return convertView;
        }
    }
}
