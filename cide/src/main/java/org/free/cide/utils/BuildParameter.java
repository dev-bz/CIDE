package org.free.cide.utils;

import android.content.SharedPreferences;
import android.os.Build;

import org.free.cide.R;
import org.free.cide.ide.Main;

import java.io.File;

/**
 * Created by Administrator on 2016/6/11.
 */
public class BuildParameter {
    private final String std;
    private String command;
    private String cursrcdir;
    private String file;
    private Main main;
    private int mode;
    private SharedPreferences preferences;

    public BuildParameter(Main main, SharedPreferences preferences, String command, String file, String std, String cursrcdir, int mode) {
        this.main = main;
        this.preferences = preferences;
        this.command = command;
        this.file = file;
        this.std = std;
        this.cursrcdir = cursrcdir;
        this.mode = mode;
    }

    public String get() {
        return command;
    }

    public BuildParameter invoke() {
        if (main != null) {
            String mode_arg = preferences.getString("mode_arg", main.activity.getString(R.string.mode_arg)).trim();
            command = command.replace("(c4droid:MODEARGS)", mode_arg);
            String path = main.activity.getFilesDir().getPath() + File.separator;
            command = command.replace("(c4droid:DATADIR)", path);
            command = command.replace("(c4droid:BIN)", path + "temp");
            command = command.replace("(c4droid:GCCROOT)", main.gccDir.getPath() + File.separator);
        }
        command = command.replace("(c4droid:PIE)", (mode == 0 && Build.VERSION.SDK_INT >= 19) ? "-pie" : "");
        if (file != null) command = command.replace("(c4droid:SRC)", file);
        if (cursrcdir != null) command = command.replace("(c4droid:CURSRCDIR)", cursrcdir);
        if (std != null) command = command.replace("(c4droid:STD)", std);
        command = command.replace("(c4droid:PREFIX)", Main.prefix);
        return this;
    }
}
