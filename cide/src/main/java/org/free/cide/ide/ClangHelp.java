package org.free.cide.ide;

import org.free.cide.views.EndTabHost;
import org.free.clangide.ClangAPI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Created by Administrator on 2016/6/16.
 */
public class ClangHelp implements CodeHelp {
    private final ArrayList<String> array;
    private ArrayList<String> filterArray;

    public ClangHelp(Set<String> load) {
        array = new ArrayList<>();
        if (load != null) {
            for (String i : load) {
                array.add(i);
            }
            Collections.sort(array, EndTabHost.sort);
        }
        filterArray = array;
    }

    @Override
    public String get(int index) {
        if (index < filterArray.size()) {
            return filterArray.get(index);
        } else index -= filterArray.size();
        return ClangAPI.get(index);
    }

    @Override
    public String getText(int index) {
        return "";
    }

    @Override
    public int set(String filterString, boolean update) {

        if (this.array.size() > 0) {
            filterArray = new ArrayList<>();
            filterString = filterString.toLowerCase();
            for (String i : this.array) {
                int indexOf = i.indexOf('↔') + 1;
                if (indexOf > 0) {
                    if (i.substring(indexOf).toLowerCase().contains(filterString))
                        filterArray.add(i);
                }
            }
        }
        return ClangAPI.set(filterString, update) + filterArray.size();
    }

    @Override
    public void updatePosition(int line, int column) {
        ClangAPI.updatePosition(line, column);
    }
}
