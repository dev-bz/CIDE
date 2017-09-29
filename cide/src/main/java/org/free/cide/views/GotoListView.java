package org.free.cide.views;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;
import com.myopicmobile.textwarrior.common.DocumentProvider;

import org.free.cide.R;
import org.free.cide.ide.Clang;
import org.free.cide.utils.SystemColorScheme;
import org.free.tools.Utils;

import java.util.ArrayList;

public class GotoListView extends ListView implements AdapterView.OnItemClickListener {
    private BaseAdapter adapter;
    private ArrayList<String> data = new ArrayList<>();
    private boolean doNotFixIt;
    private ArrayList<String> newData;
    private int normalColor;

    public GotoListView(Context context) {
        super(context);
    }

    public GotoListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GotoListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public GotoListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onFinishInflate() {
        normalColor = SystemColorScheme.getAppColor(getContext(), R.color.foreground);
        adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return data.size();
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = ViewGroup.inflate(getContext(), android.R.layout.simple_list_item_2, null);
                }
                String text = data.get(position);
                String[] split = text.split("↔", 2);
                ((TextView) convertView.findViewById(android.R.id.text2)).setText(split[0]);
                TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
                String string1 = text;
                if (split.length == 2) {
                    text = split[1];
                    if (string1.endsWith("</r>")) {
                        int fixitIndex = text.lastIndexOf("\n");
                        String source = text.replace(text.substring(fixitIndex), "修复");
                        SpannableString string = new SpannableString(source);
                        string.setSpan(new FixitOnClickText(string1), fixitIndex, source.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        textView.setTextColor(normalColor);
                        textView.setText(string);
                        textView.setMovementMethod(LinkMovementMethod.getInstance());
                    } else {
                        textView.setText(text);
                        textView.setTextColor(Clang.mainColor);
                        textView.setMovementMethod(null);
                    }
                } else {
                    textView.setTextColor(normalColor);
                    textView.setText("跳转");
                    textView.setMovementMethod(null);
                }
                return convertView;
            }
        };
        setAdapter(adapter);
        setOnItemClickListener(this);
        super.onFinishInflate();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (doNotFixIt) return;
        FreeScrollingTextField textField = null;
        if (getContext() instanceof Activity) {
            Activity context = (Activity) getContext();
            textField = (FreeScrollingTextField) context.findViewById(R.id.textField);
            ((DrawerLayout) context.findViewById(R.id.drawer_layout)).closeDrawers();
        }
        if (textField == null) return;
        String ss = data.get(position);
        if (ss.charAt(0) == ':') {
            String[] nn = ss.split(":");
            int parseInt = Integer.parseInt(nn[1]);
            DocumentProvider ddd = textField.createDocumentProvider();
            int lineOffset = ddd.getLineOffset(parseInt - 1);
            if (nn.length > 2) {
                int parseLne = Integer.parseInt(nn[2]);
                int l2 = ddd.getLineOffset(parseInt);
                if (l2 <= parseLne) l2 = ddd.docLength() - 1;
                byte[] bt = new String(ddd.subSequence(lineOffset, l2 - lineOffset)).getBytes();
                lineOffset += new String(bt, 0, parseLne - 1).length();
            }
            ddd.seekChar(lineOffset);
            if (ddd.hasNext())
                if (Character.isJavaIdentifierStart(ddd.next())) {
                    ++lineOffset;
                    while (ddd.hasNext())
                        if (Character.isJavaIdentifierPart(ddd.next())) ++lineOffset;
                        else break;
                }
            textField.selectText(false);
            textField.moveCaret(lineOffset);
            //if (ss.endsWith("</r>")) Utils.fixIt(ss, textField);
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        if (visibility == VISIBLE && isDirty() && isShown()) {
            if (newData != null) {
                data = newData;
                adapter.notifyDataSetChanged();
                newData = null;
            }
        }
        super.onVisibilityChanged(changedView, visibility);
    }

    public void setData(ArrayList<String> out) {
        if (isShown()) {
            data = out;
            adapter.notifyDataSetChanged();
        } else {
            newData = out;
        }
        doNotFixIt = false;
    }

    public void setDoNotFixIt() {
        this.doNotFixIt = true;
    }

    private class FixitOnClickText extends ClickableSpan {
        private final String text;

        public FixitOnClickText(String text) {
            this.text = text;
        }

        @Override
        public void onClick(View widget) {
            if (doNotFixIt) return;
            if (getContext() instanceof Activity) {
                Activity context = (Activity) getContext();
                FreeScrollingTextField textField = (FreeScrollingTextField) context.findViewById(R.id.textField);
                //((DrawerLayout) context.findViewById(R.id.drawer_layout)).closeDrawers();
                Utils.fixIt(text, textField);
            }
        }
    }
}
