package org.free.tools;

import android.content.Context;
import android.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class IncludeTool {
    private final String append;
    private final String base;
    private final ArrayList<File> data = new ArrayList<>();
    private final tmp ttt = new tmp();
    public String left = "";
    private String[] include;
    private ArrayList<File> listData = new ArrayList<>();
    private String oldLeft = "###";
    private String txt;

    //private ArrayList<File> listData;
    public IncludeTool(Context ctx) {
        append = PreferenceManager.getDefaultSharedPreferences(ctx).getString("inc", "");
        updateIncludeDirs();
        base = ctx.getFilesDir().getPath();
    }

    private void updateIncludeDirs() {
        String includes = System.getenv("C_INCLUDE_PATH") + File.pathSeparator + System.getenv("CPLUS_INCLUDE_PATH") + File.pathSeparator + append;
        includes = includes.replace("/", File.separator).replace("\\", File.separator);
        include = includes.split(File.pathSeparator);
    }

    public String getElementPath(int pI) {
        return listData.get(pI).getParent().replace(base, "<Base>") + File.separatorChar;
    }

    public String getText(int id) {
        String sel = getElementName(id);
        if (oldLeft.isEmpty()) return sel;
        else return oldLeft + sel;
    }

    public String getElementName(int pI) {
        File tmp = listData.get(pI);
        String fn = tmp.getName();
        if (tmp.isDirectory()) fn += File.separatorChar;
        return fn;
    }

    public void inc() {
        inc(ttt);
    }

    public void inc(iFrame incFrame) {
        {
            txt = txt.replace("/", File.separator);
            int ix = txt.lastIndexOf(File.separator);
            String right;
            if (ix > -1) {
                left = txt.substring(0, ix + 1);
                right = txt.substring(ix + 1).toLowerCase();
            } else {
                left = "";
                right = txt.toLowerCase();
            }
            if (!left.equals(oldLeft)) {
                data.clear();
                ArrayList<File> file = new ArrayList<>();
                for (String i : include) {
                    File f = new File(i + File.separator + left);
                    if (f.isDirectory()) {
                        File[] l = f.listFiles();
                        for (File e : l)
                            if (e.isDirectory()) data.add(e);
                            else if (e.isFile()) file.add(e);
                    }
                }
                Collections.sort(data);
                Collections.sort(file);
                for (File e : file) data.add(e);
                oldLeft = left;
            }
            listData.clear();
            int ixx = 0;
            float sv = 1000;
            for (File i : data)
                if (i.getName().toLowerCase().contains(right)) {
                    listData.add(i);
                    {
                        float mx = i.length() / (right.length() + 1.0F);
                        if (mx < sv) {
                            sv = mx;
                            incFrame.setSelectedIndex(ixx);
                        }
                    }
                    ++ixx;
                }
            //if(data.isEmpty())listData=null;
        }
        incFrame.updateUI();
    }

    public boolean isNotID(int id) {
        return id < 0 || id >= getSize();
    }

    public int getSize() {
        return listData.size();
    }

    public void setText(String pText) {
        txt = pText;
    }

    public void updateIncludeDirs(ArrayList<String> arrayList) {
        include = arrayList.toArray(include);
    }

    public void updateIncludeDirs(String[] inc) {
        include = inc;
    }

    public interface iFrame {
        void setSelectedIndex(int pIxx);

        void updateUI();
    }

    class tmp implements iFrame {
        @Override
        public void setSelectedIndex(int pIxx) {
            // TODO: Implement this method
        }

        @Override
        public void updateUI() {
            // TODO: Implement this method
        }
    }
}
