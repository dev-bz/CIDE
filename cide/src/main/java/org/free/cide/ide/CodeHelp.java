package org.free.cide.ide;

/**
 * Created by Administrator on 2016/6/16.
 */
public interface CodeHelp {
    String get(int index);

    String getText(int index);

    int set(String filterString, boolean update);

    void updatePosition(int line, int column);
}
