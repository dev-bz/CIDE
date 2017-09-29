package org.free.cide.tasks;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import org.free.API;
import org.free.cide.callbacks.ProjectCallback;
import org.free.clangide.ClangAPI;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

public class ScanProjectModeTask extends AsyncTask<Object, Object, Set<String>> {
    private final ProjectCallback project;
    private String c_std;
    private boolean cppmode;
    private String current;
    private String cxx_std;
    private boolean hasBox2D;
    private String[] mainFiles;
    private String[] opt;

    public ScanProjectModeTask(ProjectCallback project) {
        this.project = project;
    }

    @Override
    protected Set<String> doInBackground(Object[] params) {
        Set<String> out = new LinkedHashSet<>();
        Set<String> set = new HashSet<>();
        for (String i : mainFiles) {
            set(i);
            String position = ClangAPI.updatePosition(API._IncludeList, 0);
            if (TextUtils.isEmpty(position)) continue;
            String[] inc = position.split("\n");
            for (String n : inc) {
                if (set.add(n)) {
                    String mode = getMode(n);
                    if (!TextUtils.isEmpty(mode)) {
                        out.add(mode);
                    }
                }
            }
        }
        set(current);
        return out;
    }

    @Override
    protected void onPreExecute() {
        mainFiles = project.listMainFile();
        current = project.getEditing();
        if (current.equals("New")) current = "New.c";
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(project.getContext());
        c_std = "-std=" + p.getString("c_std", "c99");
        cxx_std = "-std=" + p.getString("cxx_std", "c++0x");
        opt = project.getOptions();
        if (mainFiles.length == 0) cancel(false);
        hasBox2D = false;
    }

    @Override
    protected void onPostExecute(Set<String> result) {
        project.setModes(result, cppmode, hasBox2D);
    }

    private void set(String mainFile) {
        String old = opt[0];
        if (mainFile.endsWith(".c")) opt[0] = c_std;
        else {
            opt[0] = cxx_std;
            cppmode = true;
        }
        if (!old.equals(opt[0]))
            ClangAPI.updateOption(opt);
        ClangAPI.updateFileCode(mainFile, null, true);
    }

    private String getMode(String string) {
        if (!hasBox2D && string.endsWith("/Box2D/Dynamics/b2World.h")) {
            hasBox2D = true;
        }
        if (string.endsWith("/SDL2/SDL.h")) {
            return "SDL2";
        }
        if (string.endsWith("/SDL.h") || string.endsWith("/SDL/SDL.h")) {
            return "SDL";
        }
        if (string.endsWith("/android_native_app_glue.h")) {
            return "Native";
        }
        return null;
    }
}
