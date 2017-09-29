package org.free.crash;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class CrashHandler implements UncaughtExceptionHandler {
    public static final String TAG = "CrashHandler";
    private static CrashHandler INSTANCE = new CrashHandler();
    private DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.CHINESE);
    private Map<String, String> infos = new TreeMap<String, String>();
    private Context mContext;
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    private String s;

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return INSTANCE;
    }

    public void init(Context context) {
        mContext = context;
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            {
                Intent intent = new Intent(mContext, M.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("log", s);
                mContext.startActivity(intent);
            }
            android.os.Process.killProcess(android.os.Process.myPid());
            //System.exit(1);
        }
    }

    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return true;
        }
        collectDeviceInfo(mContext);
        s = saveCrashInfo2File(ex, false);

		/*new Thread() {  
     @Override
		 public void run(){  
		 Looper.prepare();
		 ++e;
		 Toast.makeText(mContext,"很抱歉,程序出现异常,即将退出.("+e+")\n"+s,Toast.LENGTH_LONG).show();  
		 cc=new AlertDialog.Builder(mContext).setMessage(s).setTitle("FC").create();
		 cc.show();
		 //Intent i=new Intent(mContext,MsgActivity.class);
		 //i.putExtra("log",s);
		 //mContext.startActivity(i);
		 Looper.loop();  
		 }  
		 }.start();*/
        return true;
    }

    public void collectDeviceInfo(Context ctx) {
        try {
            PackageManager pm = ctx.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(ctx.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                String versionCode = pi.versionCode + "";
                infos.put("versionName", versionName);
                infos.put("versionCode", versionCode);
            }
        } catch (NameNotFoundException e) {
            Log.e(TAG, "an error occured when collect package info", e);
        }
        Field[] fields = Build.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                String toString = field.get(null).toString();
                if (!toString.equals("unknown"))
                    infos.put(field.getName(), toString);
                //Log.d(TAG,field.getName()+" : "+field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
        fields = Build.VERSION.class.getDeclaredFields();
        for (Field field : fields) {
            try {
                field.setAccessible(true);
                infos.put(field.getName(), field.get(null).toString());
                //Log.d(TAG,field.getName()+" : "+field.get(null));
            } catch (Exception e) {
                Log.e(TAG, "an error occured when collect crash info", e);
            }
        }
    }

    private String saveCrashInfo2File(Throwable ex, boolean save) {
        StringBuffer sb = new StringBuffer();
        for (Map.Entry<String, String> entry : infos.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append("\t = ").append(value).append("\n");
        }
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while (cause != null) {
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        sb.append(result);
        if (save) {
            try {
                long timestamp = System.currentTimeMillis();
                String time = formatter.format(new Date());
                String fileName = "crash-" + time + "-" + timestamp + ".log";
                if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                    String path = Environment.getExternalStorageDirectory().getPath() + "crash/";
                    File dir = new File(path);
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    FileOutputStream fos = new FileOutputStream(path + fileName);
                    fos.write(sb.toString().getBytes());
                    fos.close();
                }
                //return fileName;
            } catch (Exception e) {
                Log.e(TAG, "an error occured while writing file...", e);
            }
        }
        return sb.toString();
    }
}
