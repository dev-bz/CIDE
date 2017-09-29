package org.free.cide.app;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.widget.Toast;

public class App extends Application {
    private FileManager fileManager;
    private Handler handler;

    public FileManager getFileManager() {
        String ver = "unknown";
        try {
            ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (fileManager == null) fileManager = new FileManager(handler, ver);
        return fileManager;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //CrashHandler.getInstance().init(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
            StrictMode.setVmPolicy(builder.build());
        }


        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                Toast.makeText(App.this, String.valueOf(msg.obj), Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }
}
