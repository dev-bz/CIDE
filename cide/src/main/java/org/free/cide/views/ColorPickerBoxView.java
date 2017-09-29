package org.free.cide.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerBoxView extends View {
    private final Paint pp = new Paint();
    private int bottom;
    private int[] colors;
    private GradientDrawable g;
    private boolean landscape;
    private int length;
    private ColorPickerView next;
    private float pickerX;
    private int right;
    private int size;
    private int top;
    private float tx;
    private float width;

    public ColorPickerBoxView(Context context) {
        super(context);
        init();
    }

    public ColorPickerBoxView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPickerBoxView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ColorPickerBoxView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        landscape = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        colors = new int[]{0xffff0000, 0xffff00ff, 0xff0000ff, 0xff00ffff, 0xff00ff00, 0xffffff00, 0xffff0000};
        if (landscape) g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        else g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        g.setShape(GradientDrawable.RECTANGLE);
        g.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        pp.setStyle(Paint.Style.STROKE);
        pp.setStrokeWidth(2.0f * getResources().getDisplayMetrics().density);
        pickerX = 100;
    }

    public void notifyTo(ColorPickerView v2) {
        this.next = v2;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Log.e("onTouchEvent",event.toString());
        if (event.getPointerCount() == 1)
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    colorChanged(event);
                    break;
                case MotionEvent.ACTION_DOWN:
                    colorChanged(event);
                    return true;
                case MotionEvent.ACTION_UP:
                    return true;
            }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        g.draw(canvas);
        pickerX = size + (length) * tx / 6;
        if (landscape) {
            canvas.drawRect(0, pickerX - width, right, pickerX + width, pp);
        } else {
            canvas.drawRect(pickerX - width, 0, pickerX + width, bottom, pp);
        }
        super.onDraw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        size = (int) (8 * getResources().getDisplayMetrics().density);
        width = size * 0.5f;
        if (landscape) {
            g.setBounds(0, size, this.right = right - left, this.bottom = bottom - size);
            length = this.bottom - size;
        } else {
            g.setBounds(size, 0, this.right = right - left - size, this.bottom = bottom);
            length = this.right - size;
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (landscape)
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec) / 8, MeasureSpec.getMode(widthMeasureSpec));
        else
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec) / 8, MeasureSpec.getMode(heightMeasureSpec));
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private void colorChanged(MotionEvent event) {
        if (event != null) {
            if (landscape)
                pickerX = event.getY();
            else
                pickerX = event.getX();
        }
        if (pickerX < size) pickerX = size;
        if (pickerX > length + size) pickerX = length + size;
        tx = (pickerX - size) * 6 / length;
        int i = (int) tx;
        if (i > 5) i = 5;
        int a = colors[i];
        int b = colors[i + 1];
        float f = tx - i;
        int rr = (int) (Color.red(a) * (1 - f) + Color.red(b) * f);
        int gg = (int) (Color.green(a) * (1 - f) + Color.green(b) * f);
        int bb = (int) (Color.blue(a) * (1 - f) + Color.blue(b) * f);
        invalidate();
        //Log.e("color","tx="+tx+" f="+f+" i="+i+" a="+Integer.toHexString(a)+" b="+Integer.toHexString(b));
        next.updateColor(Color.rgb(rr, gg, bb));
    }

    public void startColor(int color) {
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        int max = Math.max(blue, Math.max(red, green));
        float nty = (255.0f - max) / 255.0f;
        if (max != 0) {
            red = red * 255 / max;
            green = green * 255 / max;
            blue = blue * 255 / max;
            max = 255 - Math.min(blue, Math.min(red, green));
            if (max != 0) {
                red = 255 - (255 - red) * 255 / max;
                green = 255 - (255 - green) * 255 / max;
                blue = 255 - (255 - blue) * 255 / max;
            } else {
                green = 0;
                blue = 0;
            }
        }
        float ntx = max / 255.0f;
        if (red == 255) {
            if (green == 0) {
                tx = blue / 255.0f;
            } else {
                tx = (255 - green) / 255.0f + 5;
            }
        } else if (blue == 255) {
            if (red == 0) {
                tx = green / 255.0f + 2;
            } else {
                tx = (255 - red) / 255.0f + 1;
            }
        } else if (green == 255) {
            if (blue == 0) {
                tx = red / 255.0f + 4;
            } else {
                tx = (255 - blue) / 255.0f + 3;
            }
        }
        next.startColor(ntx, nty, Color.rgb(red, green, blue), color);
    }
}
