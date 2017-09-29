package org.free.cide.callbacks;

import android.content.Context;

import org.free.cide.ide.Project;

import java.io.File;
import java.util.Set;

/**
 * Created by Administrator on 2016/6/7.
 */
public interface ProjectCallback {
    Context getContext();

    String getEditing();

    String[] getOptions();

    Project getProject();

    File getProjectFile();

    String[] listMainFile();

    void setModes(Set<String> result, boolean cppmode, boolean hasBox2D);

    void setModes(String result);
}
