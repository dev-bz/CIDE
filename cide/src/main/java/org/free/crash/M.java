package org.free.crash;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Service;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.free.cide.R;

public class M extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO: Implement this method
        super.onCreate(savedInstanceState);
        setContentView(R.layout.tmp);
        View.OnLongClickListener lc = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View p1) {
                ClipboardManager m = (ClipboardManager) getSystemService(Service.CLIPBOARD_SERVICE);
                m.setText(getIntent().getStringExtra("log"));
                //"com.baidu.tieba.frs.FrsActivity"
                boolean hasTieba = false;
                try {
                    PackageInfo p = getPackageManager().getPackageInfo("com.baidu.tieba", 0);
                    hasTieba = p != null;
                } catch (PackageManager.NameNotFoundException e) {
                }
                if (hasTieba) {
                    DialogInterface.OnClickListener cl = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface p1, int p2) {
                            Intent i = new Intent();
                            i.setClassName("com.baidu.tieba", "com.baidu.tieba.frs.FrsActivity");
                            i.putExtra("name", "cide");
                            startActivity(i);
                        }
                    };
                    new AlertDialog.Builder(M.this).setTitle("反馈").setMessage("内容已复制，到百度贴吧cide吧反馈").setPositiveButton("跳转", cl).create().show();
                } else {
                    Toast.makeText(M.this, "已复制,请到百度贴吧cide吧反馈", Toast.LENGTH_LONG).show();
                }
                return true;
            }
        };
        TextView tx;
        (tx = (TextView) findViewById(R.id.log)).setOnLongClickListener(lc);
        tx.setText(getIntent().getStringExtra("log"));
        Toast.makeText(this, "长按复制", Toast.LENGTH_SHORT).show();
    }/*
  public void t(View v){
		Intent i=new Intent(Intent.ACTION_MAIN);
		i.setClass(getApplicationContext(),MainActivity.class);
		startActivity(i);
	}*/
}
