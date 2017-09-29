package com.myopicmobile.textwarrior;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

public class EditCtrl {
    static Drawable j = null;
    static Drawable k = null;
    static Drawable l = null;
    //static int kw;

    private static float ss = 1;

    private static Paint p;

    static public void readyEditCtrl(Context context) {
        //kw = 3*textField.getAdvance('M');
        //if(k==null||j==null||l==null)
        init(context);
        //ss = kw*2.0f/(k.getIntrinsicWidth()+l.getIntrinsicWidth());
        if (p == null) {
            p = new Paint();
            p.setStyle(Paint.Style.STROKE);
        }
    }

    public static void init(Context a) {
        int[] n = {android.R.attr.textSelectHandleLeft, android.R.attr.textSelectHandle, android.R.attr.textSelectHandleRight};
        TypedArray t = a.obtainStyledAttributes(n);
        k = t.getDrawable(t.getIndex(0));
        l = t.getDrawable(t.getIndex(2));
        j = t.getDrawable(t.getIndex(1));
        t.recycle();//PorterDuffColorFilter pp=new PorterDuffColorFilter(0xFF1FAFFF,PorterDuff.Mode.SRC_ATOP);

        if (k == null) k = method(a, R.drawable.caret_left);//else k.setColorFilter(0xFF1FAFFF,PorterDuff.Mode.SRC_ATOP);
        if (l == null) l = method(a, R.drawable.caret_right);//else l.setColorFilter(0xFF1FAFFF,PorterDuff.Mode.SRC_ATOP);
        if (j == null) j = method(a, R.drawable.caret_mid);//else j.setColorFilter(0xFF1FAFFF,PorterDuff.Mode.SRC_ATOP);
    }

    private static Drawable method(Context a, int resId) {
        Bitmap b = BitmapFactory.decodeResource(a.getResources(), resId);
        //int kh=kw*b.getHeight()/b.getWidth();
        //b = Bitmap.createScaledBitmap(b,kw,kh,true);
        return new BitmapDrawable(a.getResources(), b);
    }

    public static void drawLeft(Canvas pCanvas, int x, int y) {
        int intrinsicWidth = (int) (k.getIntrinsicWidth() * ss);
        x -= (intrinsicWidth * 3 / 4);
        k.setBounds(x, y, x + intrinsicWidth, (int) (y + k.getIntrinsicHeight() * ss));
        //pCanvas.drawRect(k.getBounds(),p);
        k.draw(pCanvas);
    }

    public static void drawRight(Canvas pCanvas, int x, int y) {
        int intrinsicWidth = (int) (l.getIntrinsicWidth() * ss);
        x -= (intrinsicWidth >> 2);
        l.setBounds(x, y, x + intrinsicWidth, y + (int) (l.getIntrinsicHeight() * ss));
        //pCanvas.drawRect(l.getBounds(),p);
        l.draw(pCanvas);
    }

    public static void drawMid(Canvas pCanvas, int x, int y) {
        int intrinsicWidth = (int) (j.getIntrinsicWidth() * ss);
        x -= (intrinsicWidth >> 1);
        j.setBounds(x, y, x + intrinsicWidth, (int) (y + j.getIntrinsicHeight() * ss));
        //pCanvas.drawRect(j.getBounds(),p);
        j.draw(pCanvas);
    }

    public static int getHeight() {
        if (j == null) return 0;
        return (int) (j.getIntrinsicHeight() * ss);
    }
}
