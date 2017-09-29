package org.free.cide.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.myopicmobile.textwarrior.common.ColorScheme;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Lexer;
import com.myopicmobile.textwarrior.common.Pair;

import org.free.cide.utils.SystemColorScheme;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2016/6/17.
 */
public class CodeView extends View {
    private ColorScheme _colorScheme;
    private int _tabLength;
    private int docLength;
    private Paint paint = new Paint();
    private String replacement;
    private List<Pair> spans;
    private String[] text;
    private float[] widths = {};

    public CodeView(Context context) {
        super(context);
        init();
    }

    public CodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public CodeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private int getTabAdvance() {
        return _tabLength * (int) paint.measureText(" ");
    }

    private void myDraw(Canvas canvas) {
        Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
        if (0 == docLength || null == text) {
            paint.setColor(0xff000000);
            canvas.drawText("Error Context", 0, 0 - metrics.ascent, paint);
        } else {
            int spanIndex = 0;
            Pair nextSpan = spans.get(spanIndex++), currSpan;
            int currIndex = 0;
            float paintX = 0, paintY = -metrics.ascent;
            for (String row : text) {
                int length = row.length();
                if (length == 0) {
                    break;
                }
                if (this.widths.length < row.length()) this.widths = new float[row.length()];
                paint.getTextWidths(row, this.widths);
                int tab = 0;
                boolean hasTab = false;
                while ((tab = row.indexOf('\t', tab)) != -1) {
                    this.widths[tab] = getTabAdvance();
                    ++tab;
                    hasTab = true;
                }
                int right = currIndex + length;
                int wordEnd = Math.min(nextSpan != null ? nextSpan.getFirst() : docLength, right);
                {
                    for (int i = 0; i < length; ) {
                        if (reachedNextSpan(currIndex, nextSpan)) {
                            currSpan = nextSpan;
                            int spanColor = _colorScheme.getTokenColor(currSpan.getSecond());
                            paint.setColor(spanColor);
                            if (spanIndex < spans.size()) {
                                nextSpan = spans.get(spanIndex++);
                                wordEnd = Math.min(nextSpan.getFirst(), right);
                            } else {
                                nextSpan = null;
                                wordEnd = Math.min(docLength, right);
                            }
                        }
                        int ni = i + wordEnd - currIndex;
                        String string = row.substring(i, ni);
                        if (hasTab) string = string.replace("\t", replacement);
                        currIndex = wordEnd;
                        canvas.drawText(string, paintX, paintY, paint);
                        for (; i < ni; ++i) paintX += this.widths[i];
                    }
                }
                paintY += (metrics.descent - metrics.ascent);
                paintX = 0;
            }
        }
    }

    private boolean reachedNextSpan(int charIndex, Pair span) {
        return (span != null) && (charIndex == span.getFirst());
    }

    public void init() {
        spans = new ArrayList<>();
        spans.add(new Pair(0, Lexer.NORMAL));
        paint.setTextSize(getResources().getDisplayMetrics().density * 16);
        setColorScheme(new SystemColorScheme(getContext()));
        setTabSpaces(4);
        setText("Hello world", "Test");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        //translate clipping region to create padding around edges
        canvas.clipRect(getScrollX() + getPaddingLeft(),
                getScrollY() + getPaddingTop(),
                getScrollX() + getWidth() - getPaddingRight(),
                getScrollY() + getHeight() - getPaddingBottom());
        canvas.translate(getPaddingLeft(), getPaddingTop());
        myDraw(canvas);
        canvas.restore();
        super.onDraw(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (widthMode != MeasureSpec.EXACTLY) {
            if (null != text) {
                int m = 0;
                for (String i : text) {
                    m = Math.max((int) paint.measureText(i), m);
                }
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(m + getPaddingLeft() + getPaddingRight(), MeasureSpec.AT_MOST);
            }
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            if (null != text) {
                Paint.FontMetricsInt metrics = paint.getFontMetricsInt();
                heightMeasureSpec = MeasureSpec.makeMeasureSpec(text.length * (metrics.descent - metrics.ascent) + getPaddingTop() + getPaddingBottom(), MeasureSpec.AT_MOST);
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setColorScheme(ColorScheme colorScheme) {
        this._colorScheme = colorScheme;
        setBackgroundColor(colorScheme.getColor(ColorScheme.Colorable.BACKGROUND));
    }

    public void setTabSpaces(int spaceCount) {
        if (spaceCount <= 0 || spaceCount == _tabLength) {
            return;
        }
        replacement = "";
        for (int i = 0; i < spaceCount; ++i) replacement += " ";
        _tabLength = spaceCount;
        postInvalidate();
    }

    public void setText(String... text) {
        this.text = text;
        docLength = 0;
        if (null != text) for (String i : text) {
            docLength += i.length();
        }
        while (spans.size() > 1) spans.remove(1);
        spans.get(0).setSecond(Lexer.NORMAL);
        postInvalidate();
    }

    public void setText(DocumentProvider doc, int from, int to) {
        int currIndex = doc.getRowOffset(from);
        if (currIndex < 0) {
            return;
        }
        if (to == from) ++to;
        this.text = new String[to - from];
        docLength = 0;
        for (int i = from; i < to; ++i) {
            String row = doc.getRow(i);
            text[i - from] = row;
            docLength += row.length();
        }
        this.spans.clear();
        List<Pair> s = doc.getSpans();
        Pair currSpan = s.get(0);
        int right = docLength + currIndex;
        for (Pair i : s) {
            if (i.getFirst() > currIndex) {
                int first = currSpan.getFirst();
                if (first < right) {
                    spans.add(new Pair(Math.max(0, first - currIndex), currSpan.getSecond()));
                } else break;
            }
            currSpan = i;
        }
        if (this.spans.size() == 0) {
            this.spans.add(new Pair(0, Lexer.NORMAL));
        }
        postInvalidate();
    }

    public void setTextSize(float size) {
        paint.setTextSize(size);
        postInvalidate();
    }

    public void setTypeface(Typeface typeface) {
        paint.setTypeface(typeface);
        postInvalidate();
    }
}
