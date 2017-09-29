package org.free.cide.ide;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

public class Project {
    public ArrayList<String> append = new ArrayList<>();
    public boolean appendChanged;
    public String build_mode = "auto";
    public String cflags = "";
    public String ldflags = "";
    public String mode = "Console";
    public String output_bin = "";
    public int run_mode;
    public String shell_cleanup = "make clean";
    public String shell_command = "make CC=\"$CC\" CXX=\"$CXX\" SHELL=\"$SHELL\" OUTPUT=\"(c4droid:BIN)\"";

    public boolean add(String path) {
        if (!append.contains(path)) {
            append.add(path);
            appendChanged = true;
            return true;
        }
        return false;
    }

    public boolean contains(String path) {
        return append.contains(path);
    }

    public String get(int i) {
        return append.get(i);
    }

    public boolean load(File project) {
        if (project == null || !project.exists()) return false;
        Gson g = new Gson();
        try {
            Reader r = new FileReader(project);
            Project p = g.fromJson(r, Project.class);
            r.close();
            if (p != null) {
                append = p.append;
                appendChanged = p.appendChanged;
                cflags = p.cflags;
                ldflags = p.ldflags;
                shell_command = p.shell_command;
                shell_cleanup = p.shell_cleanup;
                run_mode = p.run_mode;
                build_mode = p.build_mode;
                output_bin = p.output_bin;
                mode = p.mode;
                return true;
            }
        } catch (IOException | JsonSyntaxException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void remove(String path) {
        append.remove(path);
        appendChanged = true;
    }

    public boolean save(File project) {
        if (project != null && project.exists() && appendChanged) {
            try {
                FileOutputStream os = new FileOutputStream(project);
                appendChanged = false;
                Gson g = new Gson();
                os.write(g.toJson(this).getBytes());
                os.close();
                return true;
            } catch (IOException e) {
                appendChanged = true;
            }
        }
        return false;
    }

    public int size() {
        return append.size();
    }
}