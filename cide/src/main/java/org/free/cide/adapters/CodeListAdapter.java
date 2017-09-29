package org.free.cide.adapters;

import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.free.cide.ide.Clang;
import org.free.cide.ide.CodeHelp;
import org.free.cide.views.MyListView;

/**
 * Created by Administrator on 2016/6/27.
 */
public class CodeListAdapter extends BaseAdapter {
    private final MyListView list;
    private ForegroundColorSpan color = new ForegroundColorSpan(Clang.mainColor);
    private int count;
    private String[] data;
    private String filterString;
    private int max;

    public CodeListAdapter(MyListView view1) {
        data = new String[]{"Test00", "Test01", "Test02", "Test03"};
        count = data.length;
        max = Math.max(count, 1);
        this.list = view1;
    }

    @Override
    public int getCount() {
        return max;
    }

    @Override
    public Object getItem(int position) {
        return position < count ? data[position] : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null)
            convertView = View.inflate(parent.getContext(), android.R.layout.simple_list_item_2, null);
        String text;
        if (count > position) {
            text = data[position];
        } else {
            text = "Nothing";
        }
        if (text == null) text = "Fail";
        String[] s = text.split("↔");
        if (s.length == 2) {
            if (TextUtils.isEmpty(filterString)) {
                ((TextView) convertView.findViewById(android.R.id.text1)).setText(s[1]);
            } else {
                SpannableString ss = new SpannableString(s[1]);
                int index = s[1].toLowerCase().indexOf(filterString);
                if (index != -1) {
                    ss.setSpan(color, index, index + filterString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    ((TextView) convertView.findViewById(android.R.id.text1)).setText(ss);
                } else {
                    ((TextView) convertView.findViewById(android.R.id.text1)).setText(s[1]);
                }
            }
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(s[0]);
        } else {
            ((TextView) convertView.findViewById(android.R.id.text1)).setText("?");
            ((TextView) convertView.findViewById(android.R.id.text2)).setText(text);
        }
        return convertView;
    }

    public void post(String[] _data, int _count, String text, String filter) {
        if (!TextUtils.isEmpty(text)) {
            list.setFooterText(text);
        } else {
            list.removeFooter();
        }
        data = _data;
        count = _count;
        this.filterString = filter;
        max = Math.max(count, 1);
        notifyDataSetChanged();
    }

    public void set(final String filter, final CodeHelp codeHelp) {
        new AsyncTask<String, Object, String>() {
            public int _count;
            public String[] _data;

            @Override
            protected String doInBackground(String... params) {
                _count = codeHelp.set(filter, false);
                String text = "";
                while (_count > 0) {
                    String s = codeHelp.get(_count - 1);
                    if (text.contains("<提示>")) {
                        text += "\n" + s;
                        --_count;
                    } else {
                        break;
                    }
                }
                if (_count > 100) _count = 100;
                _data = new String[_count];
                for (int i = 0; i < _count; ++i) _data[i] = codeHelp.get(i);
                return text.trim();
            }

            @Override
            protected void onPostExecute(String text) {
                post(_data, _count, text, filter);
            }
        }.execute();
    }

    public void setSelection() {
        list.setSelection(list.getHeaderViewsCount());
    }
}
