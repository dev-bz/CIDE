package org.free.cide.ide;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.free.cide.R;

/**
 * Created by Administrator on 2016/6/29.
 */
public class CompileInfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if ("1".equals(defaultSharedPreferences.getString("theme", "0"))) {
            setTheme(R.style.AppTheme);
        }
        setContentView(R.layout.compile_info);
        TextView text1 = (TextView) findViewById(R.id.text1);
        TextView text2 = (TextView) findViewById(R.id.text2);
        String t1 = getIntent().getStringExtra("text1");
        String t2 = getIntent().getStringExtra("text2");
        if (text1 != null) {
            if (TextUtils.isEmpty(t1))
                text1.setVisibility(View.GONE);
            else text1.setText(t1);
        }
        if (text2 != null) {
            if (TextUtils.isEmpty(t2))
                text2.setVisibility(View.GONE);
            else text2.setText(t2);
        }
    }
}
