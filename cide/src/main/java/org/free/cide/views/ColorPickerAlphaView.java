package org.free.cide.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerAlphaView extends View {
    private final Paint pp = new Paint();
    private int bottom;
    private int[] colors;
    private GradientDrawable g;
    private ShapeDrawable gg;
    private boolean landscape;
    private int length;
    private View next;
    private float pickerX;
    private int right;
    private int size;
    private float tx;
    private float width;

    public ColorPickerAlphaView(Context context) {
        super(context);
        init();
    }

    public ColorPickerAlphaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPickerAlphaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ColorPickerAlphaView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        landscape = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        colors = new int[]{0x00ffffff, 0xffffffff};
        if (landscape) g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        else g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        g.setShape(GradientDrawable.RECTANGLE);
        g.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        gg = new ShapeDrawable(new RectShape());
        int[] pixels = {0xffefefef, 0xff3f3f3f, 0xff3f3f3f, 0xffefefef};
        Bitmap bm = Bitmap.createBitmap(pixels, 2, 2, Bitmap.Config.ARGB_8888);
        gg.getPaint().setColor(0xff00ffff);
        BitmapShader shader = new BitmapShader(bm, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        Matrix lm = new Matrix();
        float density = getResources().getDisplayMetrics().density;
        lm.setScale(8 * density, 8 * density);
        shader.setLocalMatrix(lm);
        gg.setFilterBitmap(false);
        gg.getPaint().setShader(shader);
        gg.getPaint().setAntiAlias(false);
        pp.setStyle(Paint.Style.STROKE);
        pp.setStrokeWidth(2.0f * density);
        pickerX = 100;
    }

    public void notifyTo(View view) {
        this.next = view;
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
        gg.draw(canvas);
        g.draw(canvas);
        pickerX = size + length * tx;
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
        gg.setBounds(g.getBounds());
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
        if (landscape) pickerX = event.getY();
        else pickerX = event.getX();
        if (pickerX < size) pickerX = size;
        if (pickerX > length + size) pickerX = length + size;
        tx = (pickerX - size) / length;
        outputColor();
        invalidate();
    }

    private void outputColor() {
        int alpha = (int) (Color.alpha(colors[1]) * tx);
        int output = (alpha << 24) | colors[0];
        next.setBackgroundColor(output);
    }

    public void startColor(int color) {
        colors = new int[]{color & 0x00ffffff, color | 0xff000000};
        if (landscape) g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        else g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        g.setShape(GradientDrawable.RECTANGLE);
        g.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        this.tx = Color.alpha(color) / 255.0f;
        next.setBackgroundColor(color);
    }

    public void updateColor(int color) {
        Rect b = g.getBounds();
        colors = new int[]{color & 0x00ffffff, color};
        if (landscape) g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
        else g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors);
        g.setShape(GradientDrawable.RECTANGLE);
        g.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        g.setBounds(b);
        outputColor();
        invalidate();
    }
}
