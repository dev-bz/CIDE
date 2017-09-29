package org.free.cide.utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.TypedValue;

import com.myopicmobile.textwarrior.common.ColorScheme;

import org.free.cide.R;

public class SystemColorScheme extends ColorScheme {
    public SystemColorScheme(Context context) {
        setColor(Colorable.FOREGROUND, getAppColor(context, R.color.foreground));
        setColor(Colorable.BACKGROUND, getAppColor(context, R.color.background));
        setColor(Colorable.SELECTION_FOREGROUND, getAppColor(context, R.color.selection_foreground));
        setColor(Colorable.SELECTION_BACKGROUND, getAppColor(context, R.color.selection_background));
        setColor(Colorable.CARET_FOREGROUND, getAppColor(context, R.color.caret_foreground));
        setColor(Colorable.CARET_BACKGROUND, getAppColor(context, R.color.caret_background));
        setColor(Colorable.CARET_DISABLED, getAppColor(context, R.color.caret_disabled));
        setColor(Colorable.LINE_HIGHLIGHT, getAppColor(context, R.color.line_highlight));//unused
        setColor(Colorable.NON_PRINTING_GLYPH, getAppColor(context, R.color.non_printing_glyph));
        setColor(Colorable.COMMENT, (getAppColor(context, R.color.comment) & 0x00ffffff) | 0x3f000000);
        setColor(Colorable.KEYWORD, getAppColor(context, R.color.keyword));
        setColor(Colorable.LITERAL, getAppColor(context, R.color.literal));
        setColor(Colorable.SECONDARY, getAppColor(context, R.color.secondary));
        setColor(Colorable.LINECOLOR, getAppColor(context, R.color.linecolor));//unused
        setColor(Colorable.LINETEXT, getAppColor(context, R.color.linetext));//unused
        setColor(Colorable.LINE_COLOR, (getAppColor(context, R.color.line_color) & 0x00ffffff) | 0x1f000000);
        setColor(Colorable.CHAR_COLOR_R, getAppColor(context, R.color.char_color_r));
        setColor(Colorable.CHAR_COLOR_G, getAppColor(context, R.color.char_color_g));
        setColor(Colorable.CHAR_COLOR_B, getAppColor(context, R.color.char_color_b));
    }

    public static int getAppColor(Context context, int colorAttr) {
        TypedValue outValue = new TypedValue();
        context.getResources().getValue(colorAttr, outValue, true);
        if (outValue.type == TypedValue.TYPE_ATTRIBUTE) {
            TypedArray typedArray = context.getTheme().obtainStyledAttributes(new int[]{outValue.data});
            int count = typedArray.getIndexCount();
            int color = -1;
            for (int i = 0; i < count; ++i) {
                int index = typedArray.getIndex(i);
                color = typedArray.getColor(index, -1);
            }
            typedArray.recycle();
            return color;
        } else if (outValue.type >= TypedValue.TYPE_FIRST_COLOR_INT && outValue.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return getAppColor2(context, colorAttr);
        } else return 0xfffff000;
    }

    public static int getAppColor2(Context context, int colorId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getColor(colorId);
        } else {
            return context.getResources().getColor(colorId);
        }
    }

    @Override
    public boolean isDark() {
        return true;
    }
}
