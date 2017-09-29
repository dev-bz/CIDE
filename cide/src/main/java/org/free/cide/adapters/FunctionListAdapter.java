package org.free.cide.adapters;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;

import org.free.cide.R;
import org.free.cide.ide.Clang;
import org.free.cide.ide.Main;
import org.free.cide.ide.MainActivity;
import org.free.cide.ide.ViewFileActivity;

import java.io.File;

public class FunctionListAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {
    private final Activity activity;
    private final Callback callback;
    private final String[] strings;

    public FunctionListAdapter(String[] strings, Callback callback, Activity activity) {
        this.strings = strings;
        this.callback = callback;
        this.activity = activity;
    }

    @Override
    public int getCount() {
        return strings.length;
    }

    @Override
    public Object getItem(int position) {
        return strings[position];
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(parent.getContext(), android.R.layout.simple_list_item_2, null);
            View view = convertView.findViewById(android.R.id.text1);
            if (view instanceof TextView)
                ((TextView) view).setTextColor(Clang.mainColor);
        }
        String text = strings[position];
        String[] s = callback.get(text);
        View view = convertView.findViewById(android.R.id.text1);
        if (s.length == 2) {
            if (view instanceof TextView)
                ((TextView) view).setText(s[1]);
            /**else if(view instanceof CodeView) {
             String[] i = s[0].split(":");
             if(i.length>1){
             int row = Integer.parseInt(i[1]);
             EditField e= (EditField) activity.findViewById(R.id.textField);
             DocumentProvider doc = e.createDocumentProvider();
             ((CodeView) view).setText(doc,row-1,row);
             }else ((CodeView) view).setText(s[1]);
             }*/
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(s[0]);
        } else {
            if (view instanceof TextView)
                ((TextView) view).setText("?");
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(text);
        }
        return convertView;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        FreeScrollingTextField textField = null;
        if (view.getContext() instanceof Activity) {
            Activity context = (Activity) view.getContext();
            textField = (FreeScrollingTextField) context.findViewById(R.id.textField);
            ((DrawerLayout) context.findViewById(R.id.drawer_layout)).closeDrawers();
        }
        if (textField == null) return;
        String ss = strings[position];
        {
            String[] nn = ss.split(":");
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
                        Bundle b = ActivityOptions.makeSceneTransitionAnimation(activity, view, "include").toBundle();
                        activity.startActivity(i, b);
                    } else {
                        activity.startActivity(i);
                    }
                } else {
                    Intent i = new Intent(activity, MainActivity.class).setData(Uri.parse("file://" + nn[0]));
                    i.setAction(Intent.ACTION_VIEW);
                    i.putExtra("back", textField.currentFile());
                    i.putExtra("line", line_string);
                    if (nn.length > 2) {
                        i.putExtra("column", column_string);
                    }
                    activity.startActivity(i);
                }
            }
        }
    }

    public interface Callback {
        String[] get(String str);
    }
}
