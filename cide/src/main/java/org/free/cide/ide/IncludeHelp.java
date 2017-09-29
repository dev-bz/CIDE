package org.free.cide.ide;

import android.content.Context;

import org.free.tools.IncludeTool;

public class IncludeHelp implements CodeHelp {
    private final IncludeTool includeTool;

    public IncludeHelp(Context context) {
        includeTool = new IncludeTool(context);
    }

    @Override
    public String get(int index) {
        return includeTool.getElementPath(index) + "↔" + includeTool.getElementName(index);
    }

    @Override
    public String getText(int index) {
        return includeTool.getText(index);
    }

    @Override
    public int set(String filterString, boolean update) {
        includeTool.setText(filterString);
        includeTool.inc();
        return includeTool.getSize();
    }

    @Override
    public void updatePosition(int line, int column) {
    }
}