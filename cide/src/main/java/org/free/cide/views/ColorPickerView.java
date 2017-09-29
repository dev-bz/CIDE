package org.free.cide.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {
    private final Paint pp = new Paint();
    private int bottom;
    private int[] colors;
    private GradientDrawable g1;
    private GradientDrawable g2;
    private boolean landscape;
    private ColorPickerAlphaView next;
    private float pickerX;
    private float pickerY;
    private int right;
    private int size;
    private float tx;
    private float ty;

    public ColorPickerView(Context context) {
        super(context);
        init();
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ColorPickerView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        landscape = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        colors = new int[]{0xffffffff, 0xffff0000};
        g1 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        g1.setShape(GradientDrawable.RECTANGLE);
        g1.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        g2 = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0x00000000, 0xff000000});
        g2.setShape(GradientDrawable.RECTANGLE);
        g2.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        pp.setStyle(Paint.Style.STROKE);
        pp.setStrokeWidth(2.0f * getResources().getDisplayMetrics().density);
        pickerX = 100;
        pickerY = 100;
    }

    public void notifyTo(ColorPickerAlphaView v3) {
        this.next = v3;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1)
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    pickerX = event.getX();
                    pickerY = event.getY();
                    if (pickerX < size) pickerX = size;
                    if (pickerY < size) pickerY = size;
                    if (pickerX > right) pickerX = right;
                    if (pickerY > bottom) pickerY = bottom;
                    tx = (pickerX - size) / (float) (right - size);
                    ty = (pickerY - size) / (float) (bottom - size);
                    updateColor(colors[1]);
                    invalidate();
                    break;
                case MotionEvent.ACTION_DOWN:
                    pickerX = event.getX();
                    pickerY = event.getY();
                    if (pickerX < size) pickerX = size;
                    if (pickerY < size) pickerY = size;
                    if (pickerX > right) pickerX = right;
                    if (pickerY > bottom) pickerY = bottom;
                    tx = (pickerX - size) / (float) (right - size);
                    ty = (pickerY - size) / (float) (bottom - size);
                    updateColor(colors[1]);
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                    return true;
            }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        g1.draw(canvas);
        g2.draw(canvas);
        pickerX = size + (right - size) * tx;
        pickerY = size + (bottom - size) * ty;
        canvas.drawCircle(pickerX, pickerY, size * 2, pp);
        super.onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        size = (int) (8 * getResources().getDisplayMetrics().density);
        g1.setBounds(size, size, this.right = right - left - size, this.bottom = bottom - top - size);
        g2.setBounds(g1.getBounds());
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (!landscape) {
            if (MeasureSpec.getSize(widthMeasureSpec) < MeasureSpec.getSize(heightMeasureSpec))
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getMode(heightMeasureSpec));
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void updateColor(int color) {
        Rect bd = g1.getBounds();
        if (color != colors[1]) {
            colors = new int[]{0xffffffff, color};
            g1 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
            g1.setShape(GradientDrawable.RECTANGLE);
            g1.setGradientType(GradientDrawable.LINEAR_GRADIENT);
            g1.setBounds(bd);
            invalidate();
        }
        int r = (int) ((0xff * (1 - tx) + Color.red(color) * tx) * (1 - ty) + ty * 0x0);
        int g = (int) ((0xff * (1 - tx) + Color.green(color) * tx) * (1 - ty) + ty * 0x0);
        int b = (int) ((0xff * (1 - tx) + Color.blue(color) * tx) * (1 - ty) + ty * 0x0);
        next.updateColor(Color.rgb(r, g, b));
    }

    public void startColor(float ntx, float nty, int color, int color2) {
        colors = new int[]{0xffffffff, color};
        g1 = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        g1.setShape(GradientDrawable.RECTANGLE);
        g1.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        this.tx = ntx;
        this.ty = nty;
        next.startColor(color2);
    }
}
