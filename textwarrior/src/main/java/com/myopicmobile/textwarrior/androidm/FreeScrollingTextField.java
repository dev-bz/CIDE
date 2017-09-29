package com.myopicmobile.textwarrior.androidm;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.SpannableStringBuilder;
import android.text.method.CharacterPickerDialog;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Scroller;

import com.myopicmobile.textwarrior.R;
import com.myopicmobile.textwarrior.TextWarriorApplication;
import com.myopicmobile.textwarrior.common.ColorScheme;
import com.myopicmobile.textwarrior.common.ColorScheme.Colorable;
import com.myopicmobile.textwarrior.common.ColorSchemeLight;
import com.myopicmobile.textwarrior.common.Document;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Language;
import com.myopicmobile.textwarrior.common.Lexer;
import com.myopicmobile.textwarrior.common.Pair;
import com.myopicmobile.textwarrior.common.RowListener;
import com.myopicmobile.textwarrior.common.TextWarriorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A custom text view that uses a solid shaded caret (aka cursor) instead of a
 * blinking caret and allows a variety of navigation methods to be easily
 * integrated.
 * <p/>
 * It also has a built-in syntax highlighting feature. The global programming
 * language syntax to use is specified with Lexer.setLanguage(Language).
 * To disable syntax highlighting, simply pass LanguageNonProg to that function.
 * <p/>
 * Responsibilities
 * 1. Display text
 * 2. Display padding
 * 3. Scrolling
 * 4. Store and display caret position and selection range
 * 5. Store font type, font size, and tab length
 * 6. Interpret non-touch input events and shortcut keystrokes, triggering
 * the appropriate inner class controller actions
 * 7. Reset view, set cursor position and selection range
 * <p/>
 * Inner class controller responsibilities
 * 1. Caret movement
 * 2. Activate/deactivate selection mode
 * 3. Cut, copy, paste, delete, insert
 * 4. Schedule areas to repaint and analyze for spans in response to edits
 * 5. Directs scrolling if caret movements or edits causes the caret to be off-screen
 * 6. Notify rowListeners when caret row changes
 * 7. Provide helper methods for InputConnection to setComposingText from the IME
 * <p/>
 * This class is aware that the underlying text buffer uses an extra char (EOF)
 * to mark the end of the text. The text size reported by the text buffer includes
 * this extra char. Some bounds manipulation is done so that this implementation
 * detail is hidden from client classes.
 */
public abstract class FreeScrollingTextField extends View
        implements Document.TextFieldMetrics {
    //---------------------------------------------------------------------
    //--------------------------  Caret Scroll  ---------------------------
    public final static int SCROLL_UP = 0;
    public final static int SCROLL_DOWN = 1;
    public final static int SCROLL_LEFT = 2;
    public final static int SCROLL_RIGHT = 3;
    /**
     * Scale factor for the width of a caret when on a NEWLINE or EOF char.
     * A factor of 1.0 is equals to the width of a space character
     */
    protected static float EMPTY_CARET_WIDTH_SCALE = 0.75f;
    /**
     * When in selection mode, the caret height is scaled by this factor
     */
    protected static float SEL_CARET_HEIGHT_SCALE = 0.5f;
    protected static int DEFAULT_TAB_LENGTH_SPACES = 4;
    protected static long SCROLL_PERIOD = 250; //in milliseconds
    /*
   * Hash map for determining which characters to let the user choose from when
   * a hardware key is long-pressed. For example, long-pressing "e" displays
   * choices of "é, è, ê, ë" and so on.
   * This is biased towards European locales, but is standard Android behavior
   * for TextView.
   *
   * Copied from android.text.method.QwertyKeyListener, dated 2006
   */
    private static SparseArray<String> PICKER_SETS =
            new SparseArray<>();

    static {
        PICKER_SETS.put('A', "\u00C0\u00C1\u00C2\u00C4\u00C6\u00C3\u00C5\u0104\u0100");
        PICKER_SETS.put('C', "\u00C7\u0106\u010C");
        PICKER_SETS.put('D', "\u010E");
        PICKER_SETS.put('E', "\u00C8\u00C9\u00CA\u00CB\u0118\u011A\u0112");
        PICKER_SETS.put('G', "\u011E");
        PICKER_SETS.put('L', "\u0141");
        PICKER_SETS.put('I', "\u00CC\u00CD\u00CE\u00CF\u012A\u0130");
        PICKER_SETS.put('N', "\u00D1\u0143\u0147");
        PICKER_SETS.put('O', "\u00D8\u0152\u00D5\u00D2\u00D3\u00D4\u00D6\u014C");
        PICKER_SETS.put('R', "\u0158");
        PICKER_SETS.put('S', "\u015A\u0160\u015E");
        PICKER_SETS.put('T', "\u0164");
        PICKER_SETS.put('U', "\u00D9\u00DA\u00DB\u00DC\u016E\u016A");
        PICKER_SETS.put('Y', "\u00DD\u0178");
        PICKER_SETS.put('Z', "\u0179\u017B\u017D");
        PICKER_SETS.put('a', "\u00E0\u00E1\u00E2\u00E4\u00E6\u00E3\u00E5\u0105\u0101");
        PICKER_SETS.put('c', "\u00E7\u0107\u010D");
        PICKER_SETS.put('d', "\u010F");
        PICKER_SETS.put('e', "\u00E8\u00E9\u00EA\u00EB\u0119\u011B\u0113");
        PICKER_SETS.put('g', "\u011F");
        PICKER_SETS.put('i', "\u00EC\u00ED\u00EE\u00EF\u012B\u0131");
        PICKER_SETS.put('l', "\u0142");
        PICKER_SETS.put('n', "\u00F1\u0144\u0148");
        PICKER_SETS.put('o', "\u00F8\u0153\u00F5\u00F2\u00F3\u00F4\u00F6\u014D");
        PICKER_SETS.put('r', "\u0159");
        PICKER_SETS.put('s', "\u00A7\u00DF\u015B\u0161\u015F");
        PICKER_SETS.put('t', "\u0165");
        PICKER_SETS.put('u', "\u00F9\u00FA\u00FB\u00FC\u016F\u016B");
        PICKER_SETS.put('y', "\u00FD\u00FF");
        PICKER_SETS.put('z', "\u017A\u017C\u017E");
        PICKER_SETS.put(KeyCharacterMap.PICKER_DIALOG_INPUT,
                "\u2026\u00A5\u2022\u00AE\u00A9\u00B1[]{}\\|");
        PICKER_SETS.put('/', "\\");
        // From packages/inputmethods/LatinIME/res/xml/kbd_symbols.xml
        PICKER_SETS.put('1', "\u00b9\u00bd\u2153\u00bc\u215b");
        PICKER_SETS.put('2', "\u00b2\u2154");
        PICKER_SETS.put('3', "\u00b3\u00be\u215c");
        PICKER_SETS.put('4', "\u2074");
        PICKER_SETS.put('5', "\u215d");
        PICKER_SETS.put('7', "\u215e");
        PICKER_SETS.put('0', "\u207f\u2205");
        PICKER_SETS.put('$', "\u00a2\u00a3\u20ac\u00a5\u20a3\u20a4\u20b1");
        PICKER_SETS.put('%', "\u2030");
        PICKER_SETS.put('*', "\u2020\u2021");
        PICKER_SETS.put('-', "\u2013\u2014");
        PICKER_SETS.put('+', "\u00b1");
        PICKER_SETS.put('(', "[{<");
        PICKER_SETS.put(')', "]}>");
        PICKER_SETS.put('!', "\u00a1");
        PICKER_SETS.put('"', "\u201c\u201d\u00ab\u00bb\u02dd");
        PICKER_SETS.put('?', "\u00bf");
        PICKER_SETS.put(',', "\u201a\u201e");
        // From packages/inputmethods/LatinIME/res/xml/kbd_symbols_shift.xml
        PICKER_SETS.put('=', "\u2260\u2248\u221e");
        PICKER_SETS.put('<', "\u2264\u00ab\u2039");
        PICKER_SETS.put('>', "\u2265\u00bb\u203a");
    }

    private final Scroller _scroller;
    public int s;
    protected boolean handOn = false;
    protected boolean _isEdited = false; // whether the text field is dirtied
    protected TouchNavigationMethod _navMethod;
    protected DocumentProvider _hDoc; // the model in MVC
    protected int _caretPosition = 0;
    protected int _caretRow = 0; // can be calculated, but stored for efficiency purposes
    protected int _selectionAnchor = -1; // inclusive
    protected int _selectionEdge = -1; // exclusive
    protected int _tabLength = DEFAULT_TAB_LENGTH_SPACES;
    protected ColorScheme _colorScheme = new ColorSchemeLight();
    protected boolean _isHighlightRow = false;
    protected boolean _showNonPrinting = false;
    protected boolean _isAutoIndent = true;
    protected boolean _isLongPressCaps = false;
    protected float _density = 16;
    protected int attr_select_end = -1;
    protected int attr_select_start = -1;
    protected int zeroSize;
    protected boolean readOnly;
    float[] widths = new float[0];
    boolean doNotUsePost = true;
    private TextFieldController _fieldController; // the controller in MVC
    private TextFieldInputConnection _inputConnection;
    private RowListener _rowLis;
    private SelectionModeListener _selModeLis;
    private Paint _brush;
    private final Runnable _scrollCaretDownTask = new Runnable() {
        @Override
        public void run() {
            _fieldController.moveCaretDown();
            if (!caretOnLastRowOfFile()) {
                postDelayed(_scrollCaretDownTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable _scrollCaretUpTask = new Runnable() {
        @Override
        public void run() {
            _fieldController.moveCaretUp();
            if (!caretOnFirstRowOfFile()) {
                postDelayed(_scrollCaretUpTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable _scrollCaretLeftTask = new Runnable() {
        @Override
        public void run() {
            _fieldController.moveCaretLeft(false);
            if (_caretPosition > 0 &&
                    _caretRow == _hDoc.findRowNumber(_caretPosition - 1)) {
                postDelayed(_scrollCaretLeftTask, SCROLL_PERIOD);
            }
        }
    };
    private final Runnable _scrollCaretRightTask = new Runnable() {
        @Override
        public void run() {
            _fieldController.moveCaretRight(false);
            if (!caretOnEOF() &&
                    _caretRow == _hDoc.findRowNumber(_caretPosition + 1)) {
                postDelayed(_scrollCaretRightTask, SCROLL_PERIOD);
            }
        }
    };
    /**
     * Max amount that can be scrolled horizontally based on the longest line
     * displayed on screen so far
     */
    private int _xExtent = 0;
    private boolean fast = false;
    private String replacement = "    ";

    public FreeScrollingTextField(Context context, AttributeSet attrs) {
        super(context, attrs);
        _hDoc = new DocumentProvider(this);
        _navMethod = new TouchNavigationMethod(this);
        _scroller = new Scroller(context);
        takeAttrs(context, attrs);
        initView();
    }

    public FreeScrollingTextField(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        _hDoc = new DocumentProvider(this);
        _navMethod = new TouchNavigationMethod(this);
        _scroller = new Scroller(context);
        takeAttrs(context, attrs);
        initView();
    }

    public abstract String currentFile();

    public abstract boolean isCurrent(String fileName);

    public abstract boolean isWordWrap();

    public void setWordWrap(boolean enable) {
        _hDoc.setWordWrap(enable);
        if (enable) {
            _xExtent = 0;
            scrollTo(0, 0);
        }
        _fieldController.updateCaretRow();
        if (!makeCharVisible(_caretPosition)) {
            invalidate();
        }
    }

    public abstract void onUserTap();

    public abstract void selectLine(int charOffset);

    public abstract void selectWord(int charOffset);

    private void takeAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FreeScrollingTextField);
        int N = a.getIndexCount();
        for (int i = 0; i < N; ++i) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.FreeScrollingTextField_fastDraw) {
                fast = a.getBoolean(attr, false);
            } else if (attr == R.styleable.FreeScrollingTextField_selectStart) {
                attr_select_start = a.getInteger(attr, -1);
            } else if (attr == R.styleable.FreeScrollingTextField_selectEnd) {
                attr_select_end = a.getInteger(attr, -1);
            } else if (attr == R.styleable.FreeScrollingTextField_readOnly) {
                readOnly = a.getBoolean(attr, false);
            }
        }
        a.recycle();
    }

    private void initView() {
        _density = getContext().getResources().getDisplayMetrics().density;
        _fieldController = this.new TextFieldController();
        _brush = new Paint();
        _brush.setStrokeWidth(_density);
        _brush.setAntiAlias(false);
        onBrushChanged(_brush);
        //_brush.setTextSize(_density);
        zeroSize = getAdvance('0');
        s = zeroSize * 4;
        setBackgroundColor(_colorScheme.getColor(Colorable.BACKGROUND));
        setLongClickable(false);
        setFocusableInTouchMode(true);
        setHapticFeedbackEnabled(true);
        _rowLis = new RowListener() {
            @Override
            public void onRowChange(int newRowIndex) {
                // Do nothing
            }
        };
        _selModeLis = new SelectionModeListener() {
            @Override
            public void onSelectionModeChanged(boolean active) {
                // Do nothing
            }
        };
        resetView();
        //TODO find out if this function works
        //setScrollContainer(true);
    }

    protected abstract void onBrushChanged(Paint brush);

    private void resetView() {
        _caretPosition = 0;
        _caretRow = 0;
        _xExtent = 0;
        _fieldController.setSelectText(false);
        _fieldController.stopTextComposing();
        _hDoc.clearSpans();
        if (getContentWidth() > 0) {
            _hDoc.analyzeWordWrap();
        }
        _rowLis.onRowChange(0);
        scrollTo(0, 0);
    }

    /**
     * Sets the text displayed to the document referenced by hDoc. The view
     * state is reset and the view is invalidated as a side-effect.
     */
    public void setDocumentProvider(DocumentProvider hDoc) {
        _hDoc = hDoc;
        resetView();
        _fieldController.cancelSpanning(); //stop existing lex threads
        _fieldController.determineSpans();
        invalidate();
    }

    /**
     * Returns a DocumentProvider that references the same Document used by the
     * FreeScrollingTextField.
     */
    public DocumentProvider createDocumentProvider() {
        return new DocumentProvider(_hDoc);
    }

    public void setRowListener(RowListener rLis) {
        _rowLis = rLis;
    }

    public void setSelModeListener(SelectionModeListener sLis) {
        _selModeLis = sLis;
    }

    /**
     * Sets the caret navigation method used by this text field
     */
    public void setNavigationMethod(TouchNavigationMethod navMethod) {
        _navMethod = navMethod;
    }
    //---------------------------------------------------------------------
    //-------------------------- Paint methods ----------------------------

    public void setChirality(boolean isRightHanded) {
        _navMethod.onChiralityChanged(isRightHanded);
    }

    // this used to be isDirty(), but was renamed to avoid conflicts with Android API 11
    public boolean isEdited() {
        return _isEdited;
    }

    public void setEdited(boolean set) {
        _isEdited = set;
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = /*0;//InputType.TYPE_CLASS_TEXT |*/ InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;// | InputType.TYPE_TEXT_VARIATION_FILTER;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION
                | EditorInfo.IME_ACTION_NONE
                | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        if (_inputConnection == null) {
            _inputConnection = this.new TextFieldInputConnection(this);
        } else {
            _inputConnection.resetComposingState();
        }
        outAttrs.initialSelStart = getSelectionStart();
        outAttrs.initialSelEnd = getSelectionEnd();
        outAttrs.initialCapsMode = _inputConnection.getCursorCapsMode(outAttrs.inputType);
        return _inputConnection;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return !readOnly;
    }

    @Override
    public boolean isSaveEnabled() {
        return true;
    }

    //---------------------------------------------------------------------
    //------------------------- Layout methods ----------------------------
//TODO test with height less than 1 complete row
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(useAllDimensions(widthMeasureSpec),
                useAllDimensions(heightMeasureSpec));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldh == 0 && oldh == 0) {
            makeCharVisible(_caretPosition);
        }
        if (w != oldw) {
            _hDoc.analyzeWordWrap();
            if (h == oldh) _fieldController.updateCaretRow();
        } else if (h < oldh && isFocused()) {
            if (!makeCharVisible(_caretPosition)) {
                invalidate();
            }
        }
        /**if(h<oldh) {
         if (!makeCharVisible(_caretPosition)) {
         invalidate();
         }
         }*/
    }

    private int useAllDimensions(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int result = MeasureSpec.getSize(measureSpec);
        if (specMode != MeasureSpec.EXACTLY && specMode != MeasureSpec.AT_MOST) {
            result = Integer.MAX_VALUE;
            TextWarriorException.fail("MeasureSpec cannot be UNSPECIFIED. Setting dimensions to max.");
        }
        return result;
    }

    protected int getNumVisibleRows() {
        return (int) Math.ceil((double) getContentHeight() / rowHeight());
    }

    protected int rowHeight() {
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        return (metrics.descent - metrics.ascent);
    }

    /*
   The only methods that have to worry about padding are invalidate, draw
   and computeVerticalScrollRange() methods. Other methods can assume that
   the text completely fills a rectangular viewport given by getContentWidth()
   and getContentHeight()
   */
    protected int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    protected int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    /**
     * Determines if the View has been layout or is still being constructed
     */
    public boolean hasLayout() {
        return (getWidth() == 0); // simplistic implementation, but should work for most cases
    }

    /**
     * The first row of text to paint, which may be partially visible.
     * Deduced from the clipping rectangle given to onDraw()
     */
    private int getBeginPaintRow(Canvas canvas) {
        Rect bounds = canvas.getClipBounds();
        return getRealPosition(bounds.top) / rowHeight();
    }

    /**
     * The last row of text to paint, which may be partially visible.
     * Deduced from the clipping rectangle given to onDraw()
     */
    private int getEndPaintRow(Canvas canvas) {
        //clip top and left are inclusive; bottom and right are exclusive
        Rect bounds = canvas.getClipBounds();
        return (getRealPosition(bounds.bottom) - 1) / rowHeight();
    }

    protected abstract int getRealPosition(int position);

    /**
     * @return The x-value of the baseline for drawing text on the given row
     */
    protected int getPaintBaseline(int row) {
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        return getPaintPosition((row + 1) * rowHeight() - metrics.descent);
    }

    protected abstract int getPaintPosition(int position);

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        //translate clipping region to create padding around edges
        canvas.clipRect(getScrollX() + getPaddingLeft(),
                getScrollY() + getPaddingTop(),
                getScrollX() + getWidth() - getPaddingRight(),
                getScrollY() + getHeight() - getPaddingBottom());
        canvas.translate(getPaddingLeft(), getPaddingTop());
        realDraw(canvas);
        drawAtLast(canvas);
        canvas.restore();
        _navMethod.onTextDrawComplete(canvas);
    }

    protected abstract void drawAtLast(Canvas canvas);

    private void realDraw(Canvas canvas) {
        //----------------------------------------------
        // initialize and set up boundaries
        //----------------------------------------------
        int currRowNum = getBeginPaintRow(canvas);
        int currIndex = _hDoc.getRowOffset(currRowNum);
        if (currIndex < 0) {
            //return;
            currIndex = 0;
            currRowNum = 0;
        }
        int endRowNum = getEndPaintRow(canvas);
        int paintX;
        int paintY = getPaintBaseline(currRowNum);
        drawBeforeLoop(canvas, currRowNum, endRowNum);
        //----------------------------------------------
        // set up initial span color
        //----------------------------------------------
        int spanIndex = 0;
        List<Pair> spans = _hDoc.getSpans();
        // There must be at least one span to paint, even for an empty file,
        // where the span contains only the EOF character
        TextWarriorException.assertVerbose(!spans.isEmpty(),
                "No spans to paint in TextWarrior.paint()");
        //TODO use binary search
        Pair nextSpan = spans.get(spanIndex++);
        Pair currSpan;
        do {
            currSpan = nextSpan;
            if (spanIndex < spans.size()) {
                nextSpan = spans.get(spanIndex++);
            } else {
                nextSpan = null;
            }
        } while (nextSpan != null && nextSpan.getFirst() <= currIndex);
        int spanColor = _colorScheme.getTokenColor(currSpan.getSecond());
        _brush.setColor(spanColor);
        //----------------------------------------------
        // start painting!
        //----------------------------------------------
        while (currRowNum <= endRowNum) {
            String row = _hDoc.getRow(currRowNum).replace(Language.EOF, '\0');
            int _length = row.length();
            if (_length == 0) {
                break;
            }
            drawAtEachLineLeft(canvas, currIndex, s, paintY);
            paintX = s;
            int length = _length;
            if (fast) {
                int index, offset = 0;
                ArrayList<String> rows = new ArrayList<>();
                while (offset < _length) {
                    index = row.indexOf("-*/", offset + 4);
                    if (index != -1) {
                        int left = row.lastIndexOf("/*-", index - 2);
                        if (left >= offset) {
                            if (left > offset) rows.add(row.substring(offset, left));
                            rows.add(row.substring(left, index + 3));
                        } else rows.add(row.substring(offset, index + 3));
                        offset = index + 3;
                    } else if (offset > 0) {
                        rows.add(row.substring(offset));
                        break;
                    } else {
                        rows.add(row);
                        break;
                    }
                }
                //canvas.drawText(rows.size()+"|",paintX,paintY,_brush);paintX+=64;
                if (this.widths.length < row.length()) this.widths = new float[row.length()];
                _brush.getTextWidths(row, this.widths);
                int selectionStart;
                int selectionEnd;
                if (isSelectText()) {
                    selectionStart = getSelectionStart();
                    selectionEnd = getSelectionEnd();
                } else {
                    selectionStart = _caretPosition;
                    selectionEnd = _caretPosition;
                }
                ArrayList<Pair> block = new ArrayList<>();
                int position = _caretPosition;
                int _offset = 0;
                length = 0;
                for (String _row : rows) {
                    _offset += length;
                    length = _row.length();
                    int right = currIndex + length;
                    int _x = 0;
                    boolean marked = false;
                    if (isMarked(_row)) {
                        _x = paintX;
                        marked = true;
                        if (position < right && right - position < 3) {
                            position = right;
                        } else if (position > currIndex && position - currIndex < 3) {
                            position = currIndex;
                        }
                        if (selectionStart < right && right - selectionStart < 3) {
                            selectionStart = right;
                        } else if (selectionStart > currIndex && selectionStart - currIndex < 3) {
                            selectionStart = currIndex;
                        }
                        if (selectionEnd < right && right - selectionEnd < 3) {
                            selectionEnd = right;
                        } else if (selectionEnd > currIndex && selectionEnd - currIndex < 3) {
                            selectionEnd = currIndex;
                        }
                    }
                    int tab = 0;
                    boolean hasTab = false;
                    while ((tab = _row.indexOf('\t', tab)) != -1) {
                        this.widths[tab + _offset] = getTabAdvance();
                        ++tab;
                        hasTab = true;
                    }
                    if (selectionStart < currIndex && selectionEnd >= right) {
                        int start = 0, tmp = length;
                        if (marked) {
                            start = 3;
                            tmp -= 3;
                            _row = _row.substring(3, tmp);
                        }
                        drawSelectedText(canvas, hasTab ? _row.replace("\t", replacement) : _row, paintX, paintY);
                        for (int i = start; i < tmp; ++i) {
                            paintX += this.widths[i + _offset];
                        }
                        currIndex = right;
                    } else {
                        int wordEnd = Math.min(nextSpan != null ? nextSpan.getFirst() : _hDoc.docLength(), right);
                        if (selectionEnd < currIndex || selectionStart > right) {
                            for (int i = 0; i < length; ) {
                                if (reachedNextSpan(currIndex, nextSpan)) {
                                    currSpan = nextSpan;
                                    spanColor = _colorScheme.getTokenColor(currSpan.getSecond());
                                    _brush.setColor(spanColor);
                                    if (spanIndex < spans.size()) {
                                        nextSpan = spans.get(spanIndex++);
                                        wordEnd = Math.min(nextSpan.getFirst(), right);
                                    } else {
                                        nextSpan = null;
                                        wordEnd = Math.min(_hDoc.docLength(), right);
                                    }
                                }
                                int ni = i + wordEnd - currIndex;
                                currIndex = wordEnd;
                                int tmp = ni;
                                if (marked) {
                                    if (ni < 4 || i > length - 4) {
                                        i = ni;
                                        continue;
                                    } else {
                                        if (i < 3) i = 3;
                                        if (length - 3 < ni)
                                            tmp = length - 3;
                                    }
                                }
                                String string = _row.substring(i, tmp);
                                if (hasTab) string = string.replace("\t", replacement);
                                drawString(canvas, string, paintX, paintY);
                                for (; i < tmp; ++i) paintX += this.widths[i + _offset];
                                i = ni;
                            }
                        } else {
                            int st = Math.max(currIndex, selectionStart);
                            int ed = Math.min(right, selectionEnd);
                            for (int i = 0; i < length; ) {
                                if (reachedNextSpan(currIndex, nextSpan)) {
                                    currSpan = nextSpan;
                                    spanColor = _colorScheme.getTokenColor(currSpan.getSecond());
                                    _brush.setColor(spanColor);
                                    if (spanIndex < spans.size()) {
                                        nextSpan = spans.get(spanIndex++);
                                        wordEnd = Math.min(nextSpan.getFirst(), right);
                                    } else {
                                        nextSpan = null;
                                        wordEnd = Math.min(_hDoc.docLength(), right);
                                    }
                                }
                                if (currIndex < st) wordEnd = Math.min(st, wordEnd);
                                if (st == currIndex && currIndex < ed) wordEnd = ed;
                                if (currIndex == selectionEnd) {
                                    boolean ch = false;
                                    int nextOffset = nextSpan == null ? -1 : nextSpan.getFirst();
                                    while (currIndex >= nextOffset && nextOffset != -1) {
                                        currSpan = nextSpan;
                                        ch = true;
                                        if (spanIndex < spans.size()) {
                                            nextSpan = spans.get(spanIndex++);
                                            nextOffset = nextSpan.getFirst();
                                        } else {
                                            nextSpan = null;
                                            nextOffset = -1;
                                        }
                                    }
                                    if (ch) {
                                        spanColor = _colorScheme.getTokenColor(currSpan.getSecond());
                                        _brush.setColor(spanColor);
                                    }
                                    wordEnd = Math.min(nextSpan != null ? nextSpan.getFirst() : _hDoc.docLength(), right);
                                }
                                if (wordEnd > currIndex) {
                                    int ni = i + wordEnd - currIndex;
                                    int tmp = ni;
                                    if (marked) {
                                        if (ni < 4 || i > length - 4) {
                                            currIndex = wordEnd;
                                            i = ni;
                                            continue;
                                        } else {
                                            if (i < 3) i = 3;
                                            if (length - 3 < ni)
                                                tmp = length - 3;
                                        }
                                    }
                                    if (i > 0 && widths[i + _offset] == 0) {
                                        int _advance = getAdvance(_row.charAt(i));
                                        paintX -= (this.widths[i + _offset] = _advance);
                                    }
                                    String string = _row.substring(i, tmp);
                                    if (hasTab) string = string.replace("\t", replacement);
                                    int advance = 0;
                                    for (; i < tmp; ++i) advance += this.widths[i + _offset];
                                    i = ni;
                                    if (st == currIndex && ed == wordEnd) {
                                        _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_BACKGROUND));
                                        drawTextBackground(canvas, paintX, paintY, (float) advance);
                                        if (currIndex == position) {
                                            _brush.setColor(_colorScheme.getColor(isFocused() ? Colorable.CARET_BACKGROUND : Colorable.CARET_DISABLED));
                                            drawTextBackground(canvas, (int) (paintX - _density), paintY, _density * 2);
                                        } else if (wordEnd != right && wordEnd == position) {
                                            _brush.setColor(_colorScheme.getColor(isFocused() ? Colorable.CARET_BACKGROUND : Colorable.CARET_DISABLED));
                                            drawTextBackground(canvas, (int) (paintX + (float) advance - _density), paintY, _density * 2);
                                        }
                                        _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_FOREGROUND));
                                        drawString(canvas, string, paintX, paintY);
                                        _brush.setColor(spanColor);
                                    } else {
                                        if (currIndex == position) {
                                            _brush.setColor(_colorScheme.getColor(isFocused() ? Colorable.CARET_BACKGROUND : Colorable.CARET_DISABLED));
                                            drawTextBackground(canvas, (int) (paintX - _density), paintY, 2 * _density);
                                            _brush.setColor(spanColor);
                                        } else if (wordEnd != right && wordEnd == position) {
                                            _brush.setColor(_colorScheme.getColor(isFocused() ? Colorable.CARET_BACKGROUND : Colorable.CARET_DISABLED));
                                            drawTextBackground(canvas, (int) (paintX + advance - _density), paintY, 2 * _density);
                                            _brush.setColor(spanColor);
                                        }
                                        drawString(canvas, string, paintX, paintY);
                                    }
                                    paintX += advance;
                                    currIndex = wordEnd;
                                } else {
                                    currIndex = right;
                                    break;
                                }
                            }
                        }
                    }
                    if (marked) {
                        block.add(new Pair(_x, paintX));
                    }
                }
                for (Pair i : block) {
                    _brush.setColor(_colorScheme.getTokenColor(Lexer.DOUBLE_SYMBOL_DELIMITED_MULTILINE));
                    _brush.setStyle(Paint.Style.STROKE);
                    drawTextBackground2(canvas, i.getFirst(), paintY, i.getSecond() - i.getFirst());
                    _brush.setStyle(Paint.Style.FILL);
                    _brush.setColor(spanColor);
                }
            } else
                for (int i = 0; i < length; ++i) {
                    // check if formatting changes are needed
                    if (reachedNextSpan(currIndex, nextSpan)) {
                        currSpan = nextSpan;
                        spanColor = _colorScheme.getTokenColor(currSpan.getSecond());
                        _brush.setColor(spanColor);
                        if (spanIndex < spans.size()) {
                            nextSpan = spans.get(spanIndex++);
                        } else {
                            nextSpan = null;
                        }
                    }
                    char c = row.charAt(i);
                    if (currIndex == _caretPosition) {
                        paintX += drawCaret(canvas, c, paintX, paintY);
                    } else if (_fieldController.inSelectionRange(currIndex)) {
                        paintX += drawSelectedText(canvas, c, paintX, paintY);
                    } else {
                        paintX += drawChar(canvas, c, paintX, paintY);
                    }
                    ++currIndex;
                }
            paintX += drawAtEachLineRight(canvas, currRowNum, paintX, paintY);
            paintY += rowHeight();
            paintY += drawAtEachLineBottom(canvas, currRowNum, paintX, paintY);
            if (paintX > _xExtent) {
                // record widest line seen so far
                _xExtent = paintX;
            }
            ++currRowNum;
        } // end while
        doOptionHighlightRow(canvas);
    }

    private boolean isMarked(String string) {
        int right = string.length() - 3;
        if (right < 4) return false;
        return string.charAt(0) == '/' && string.charAt(1) == '*' && string.charAt(2) == '-' && string.charAt(right) == '-' && string.charAt(right + 1) == '*' && string.charAt(right + 2) == '/';
    }

    protected abstract float drawAtEachLineBottom(Canvas canvas, int currRowNum, int paintX, int paintY);

    protected abstract int drawAtEachLineRight(Canvas canvas, int currRowNum, int paintX, int paintY);

    protected abstract void drawAtEachLineLeft(Canvas canvas, int currIndex, int width, int paintY);

    protected abstract void drawBeforeLoop(Canvas canvas, int currRowNum, int endRowNum);

    private void drawString(Canvas canvas, String c, int paintX, int paintY) {
        canvas.drawText(c, paintX, paintY, _brush);
    }

    private float drawSelectedText(Canvas canvas, String c, int paintX, int paintY) {
        int oldColor = _brush.getColor();
        float advance = _brush.measureText(c);
        _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_BACKGROUND));
        drawTextBackground(canvas, paintX, paintY, advance);
        _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_FOREGROUND));
        drawString(canvas, c, paintX, paintY);
        _brush.setColor(oldColor);
        return advance;
    }

    private void drawSelectedText_(Canvas canvas, String c, int paintX, int paintY, float advance, boolean cartAtLeft, boolean cartAtRight) {
        int oldColor = _brush.getColor();
        _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_BACKGROUND));
        drawTextBackground(canvas, paintX, paintY, advance);
        if (cartAtLeft || cartAtRight) {
            _brush.setColor(_colorScheme.getColor(isFocused() ? Colorable.CARET_BACKGROUND : Colorable.CARET_DISABLED));
            if (cartAtLeft)
                drawTextBackground(canvas, (int) (paintX - _density), paintY, _density * 2);
            else if (cartAtRight)
                drawTextBackground(canvas, (int) (paintX + advance - _density), paintY, _density * 2);
        }
        _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_FOREGROUND));
        drawString(canvas, c, paintX, paintY);
        _brush.setColor(oldColor);
    }

    /**
     * Underline the caret row if the option for highlighting it is set
     */
    private void doOptionHighlightRow(Canvas canvas) {
        if (_isHighlightRow) {
            int y = getPaintBaseline(_caretRow);
            int originalColor = _brush.getColor();
            _brush.setColor(_colorScheme.getColor(Colorable.LINE_HIGHLIGHT));
            float lineLength = Math.max(_xExtent, getContentWidth());
            canvas.drawRect(0, y + 1, lineLength, y + 2, _brush);
            _brush.setColor(originalColor);
        }
    }

    private int drawChar(Canvas canvas, char c, int paintX, int paintY) {
        int originalColor = _brush.getColor();
        switch (c) {
            case ' ':
                if (_showNonPrinting) {
                    _brush.setColor(_colorScheme.getColor(Colorable.NON_PRINTING_GLYPH));
                    canvas.drawText(Language.GLYPH_SPACE, 0, 1, paintX, paintY, _brush);
                    _brush.setColor(originalColor);
                } else {
                    canvas.drawText(" ", 0, 1, paintX, paintY, _brush);
                }
                break;
            case Language.EOF: //fall-through
            case Language.NEWLINE:
                if (_showNonPrinting) {
                    _brush.setColor(_colorScheme.getColor(Colorable.NON_PRINTING_GLYPH));
                    canvas.drawText(Language.GLYPH_NEWLINE, 0, 1, paintX, paintY, _brush);
                    _brush.setColor(originalColor);
                }
                break;
            case Language.TAB:
                if (_showNonPrinting) {
                    _brush.setColor(_colorScheme.getColor(Colorable.NON_PRINTING_GLYPH));
                    canvas.drawText(Language.GLYPH_TAB, 0, 1, paintX, paintY, _brush);
                    _brush.setColor(originalColor);
                }
                break;
            default:
                char[] ca = {c};
                canvas.drawText(ca, 0, 1, paintX, paintY, _brush);
                break;
        }
        return getAdvance(c);
    }

    // paintY is the baseline for text, NOT the top extent
    protected void drawTextBackground(Canvas canvas, int paintX, int paintY,
                                      float advance) {
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        canvas.drawRect(paintX,
                paintY + metrics.ascent,
                paintX + advance,
                paintY + metrics.descent,
                _brush);
    }

    protected void drawTextBackground2(Canvas canvas, int paintX, int paintY,
                                       float advance) {
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        canvas.drawRoundRect(new RectF(paintX,
                        paintY + metrics.ascent + 1,
                        paintX + advance,
                        paintY + metrics.descent - 1), metrics.descent, metrics.descent,
                _brush);
    }

    private int drawSelectedText(Canvas canvas, char c, int paintX, int paintY) {
        int oldColor = _brush.getColor();
        int advance = getAdvance(c);
        _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_BACKGROUND));
        drawTextBackground(canvas, paintX, paintY, advance);
        _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_FOREGROUND));
        drawChar(canvas, c, paintX, paintY);
        _brush.setColor(oldColor);
        return advance;
    }

    private int drawCaret(Canvas canvas, char c, int paintX, int paintY) {
        int originalColor = _brush.getColor();
        int textColor = originalColor;
        int advance = getAdvance(c);
        if (_caretPosition == _selectionAnchor &&
                _caretPosition != _selectionEdge) {
            // draw selection background
            _brush.setColor(_colorScheme.getColor(Colorable.SELECTION_BACKGROUND));
            drawTextBackground(canvas, paintX, paintY, advance);
            textColor = _colorScheme.getColor(Colorable.CARET_FOREGROUND);
        }
        int caretColor = _colorScheme.getColor(isFocused()
                ? Colorable.CARET_BACKGROUND
                : Colorable.CARET_DISABLED);
        _brush.setColor(caretColor);
        if (_caretPosition == _selectionEdge ||
                _caretPosition == _selectionAnchor) {
            // draw half caret
            Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
            canvas.drawRect(paintX,
                    paintY + (metrics.ascent * SEL_CARET_HEIGHT_SCALE),
                    paintX + advance,
                    paintY + metrics.descent,
                    _brush);
        } else {
            // draw full caret
            drawTextBackground(canvas, paintX, paintY, advance);
            textColor = _colorScheme.getColor(Colorable.CARET_FOREGROUND);
        }
        _brush.setColor(textColor);
        // draw text
        drawChar(canvas, c, paintX, paintY);
        _brush.setColor(originalColor);
        return advance;
    }

    @Override
    final public int getRowWidth() {
        return getContentWidth() - s;
    }

    /**
     * Returns printed width of c.
     * <p/>
     * Takes into account user-specified tab width and also handles
     * application-defined widths for NEWLINE and EOF
     *
     * @param c Character to measure
     * @return Advance of character, in pixels
     */
    @Override
    public int getAdvance(char c) {
        int advance;
        switch (c) {
            case ' ':
                advance = getSpaceAdvance();
                break;
            case Language.NEWLINE: // fall-through
            case Language.EOF:
                advance = getEOLAdvance();
                break;
            case Language.TAB:
                advance = getTabAdvance();
                break;
            default:
                char[] ca = {c};
                advance = (int) _brush.measureText(ca, 0, 1);
                break;
        }
        return advance;
    }

    protected int getSpaceAdvance() {
        if (_showNonPrinting) {
            return (int) _brush.measureText(Language.GLYPH_SPACE,
                    0, Language.GLYPH_SPACE.length());
        } else {
            return (int) _brush.measureText(" ", 0, 1);
        }
    }

    protected int getEOLAdvance() {
        if (_showNonPrinting) {
            return (int) _brush.measureText(Language.GLYPH_NEWLINE,
                    0, Language.GLYPH_NEWLINE.length());
        } else {
            return (int) (EMPTY_CARET_WIDTH_SCALE * _brush.measureText(" ", 0, 1));
        }
    }
    //---------------------------------------------------------------------
    //------------------- Scrolling and touch -----------------------------

    protected int getTabAdvance() {
        if (_showNonPrinting) {
            return _tabLength * (int) _brush.measureText(Language.GLYPH_SPACE,
                    0, Language.GLYPH_SPACE.length());
        } else {
            return _tabLength * (int) _brush.measureText(" ", 0, 1);
        }
    }
    /**int coordToCharIndex(float x, int y) {Log.e(TextWarriorApplication.LOG_TAG,new Throwable().getStackTrace()[0].getMethodName());
     int row = y / rowHeight();
     if(row>=_hDoc.getRowCount())row=_hDoc.getRowCount()-1;
     int charIndex = _hDoc.getRowOffset(row);
     if (charIndex < 0) {
     //non-existent row
     return -1;
     }
     if (x < 0) {
     return charIndex; // coordinate is outside, to the left of view
     }
     String rowText = _hDoc.getRow(row);
     float extent = 0;
     int i = 0;
     while (i < rowText.length()) {
     char c = rowText.charAt(i);
     if (c == Language.NEWLINE || c == Language.EOF) {
     extent += getEOLAdvance();
     } else if (c == ' ') {
     extent += getSpaceAdvance();
     } else if (c == Language.TAB) {
     extent += getTabAdvance();
     } else {
     char[] ca = {c};
     extent += _brush.measureText(ca, 0, 1);
     }
     if (_caretPosition == i + charIndex) extent += 2*_density;
     if (extent >= x) {
     break;
     }
     ++i;
     }
     if (i < rowText.length()) {
     return charIndex + i;
     }
     //nearest char is last char of line
     return charIndex + i - 1;
     }*/

    /**
     * Invalidate rows from startRow (inclusive) to endRow (exclusive)
     */
    private void invalidateRows(int startRow, int endRow) {
        TextWarriorException.assertVerbose(startRow <= endRow && startRow >= 0,
                "Invalid startRow and/or endRow");
        Rect caretSpill = _navMethod.getCaretBloat();
        //TODO The ascent of (startRow+1) may jut inside startRow, so part of
        // that rows have to be invalidated as well.
        // This is a problem for Thai, Vietnamese and Indic scripts
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        int top = startRow * rowHeight() + getPaddingTop();
        top -= Math.max(caretSpill.top, metrics.descent);
        top = Math.max(0, top);
        super.invalidate(0,
                getPaintPosition(top),
                getScrollX() + getWidth(),
                getPaintPosition(endRow * rowHeight() + getPaddingTop() + caretSpill.bottom));
    }

    /**
     * Invalidate rows from startRow (inclusive) to the end of the field
     */
    private void invalidateFromRow(int startRow) {
        TextWarriorException.assertVerbose(startRow >= 0,
                "Invalid startRow");
        Rect caretSpill = _navMethod.getCaretBloat();
        //TODO The ascent of (startRow+1) may jut inside startRow, so part of
        // that rows have to be invalidated as well.
        // This is a problem for Thai, Vietnamese and Indic scripts
        Paint.FontMetricsInt metrics = _brush.getFontMetricsInt();
        int top = startRow * rowHeight() + getPaddingTop();
        top -= Math.max(caretSpill.top, metrics.descent);
        top = Math.max(0, top);
        super.invalidate(0,
                getPaintPosition(top),
                getScrollX() + getWidth(),
                getScrollY() + getHeight());
    }

    protected void invalidateCaretRow() {
        invalidateRows(_caretRow, _caretRow + 1);
    }

    private void invalidateSelectionRows() {
        int startRow = _hDoc.findRowNumber(_selectionAnchor);
        int endRow = _hDoc.findRowNumber(_selectionEdge);
        invalidateRows(startRow, endRow + 1);
    }

    /**
     * Scrolls the text horizontally and/or vertically if the character
     * specified by charOffset is not in the visible text region.
     * The view is invalidated if it is scrolled.
     *
     * @param charOffset The index of the character to make visible
     * @return True if the drawing area was scrolled horizontally
     * and/or vertically
     */
    private boolean makeCharVisible(int charOffset) {
        TextWarriorException.assertVerbose(
                charOffset >= 0 && charOffset < _hDoc.docLength(),
                "Invalid charOffset given");
        int scrollVerticalBy = makeCharRowVisible(charOffset);
        if (doNotUsePost) {
            scrollBy(0, scrollVerticalBy);
            return true;
        }
        int scrollHorizontalBy = makeCharColumnVisible(charOffset);
        if (scrollVerticalBy == 0 && scrollHorizontalBy == 0) {
            return false;
        } else {
            _scroller.startScroll(getScrollX(), getScrollY(), scrollHorizontalBy, scrollVerticalBy);
            postInvalidate();
            //postInvalidateDelayed(5);
            //scrollBy(scrollHorizontalBy, scrollVerticalBy);
            return true;
        }
    }

    /**
     * Calculates the amount to scroll vertically if the char is not
     * in the visible region.
     *
     * @param charOffset The index of the character to make visible
     * @return The amount to scroll vertically
     */
    private int makeCharRowVisible(int charOffset) {
        int scrollBy = 0;
        int charTop = getPaintPosition(_hDoc.findRowNumber(charOffset) * rowHeight());
        int charBottom = charTop + rowHeight();
        if (charTop < getScrollY()) {
            scrollBy = charTop - getScrollY();
        } else if (charBottom > (getScrollY() + getContentHeight() - getToolHeight())) {
            scrollBy = charBottom - getScrollY() - getContentHeight() + getToolHeight();
        }
        return scrollBy;
    }

    protected abstract int getToolHeight();

    /**
     * Calculates the amount to scroll horizontally if the char is not
     * in the visible region.
     *
     * @param charOffset The index of the character to make visible
     * @return The amount to scroll horizontally
     */
    private int makeCharColumnVisible(int charOffset) {
        int scrollBy = 0;
        Pair visibleRange = getCharExtent(charOffset);
        int charLeft = visibleRange.getFirst();
        int charRight = visibleRange.getSecond();
        if (charRight > (getScrollX() + getContentWidth())) {
            scrollBy = charRight - getScrollX() - getContentWidth();
        }
        if (charLeft < getScrollX()) {
            scrollBy = charLeft - getScrollX();
        }
        return scrollBy;
    }

    /**
     * Calculates the x-coordinate extent of charOffset.
     *
     * @return The x-values of left and right edges of charOffset. Pair.first
     * contains the left edge and Pair.second contains the right edge
     */
    protected Pair getCharExtent(int charOffset) {
        int row = _hDoc.findRowNumber(charOffset);
        int rowOffset = _hDoc.getRowOffset(row);
        int len = charOffset - rowOffset;
        char[] rowString = _hDoc.subSequence(rowOffset, _hDoc.getRowSize(row));
        float[] widths = new float[rowString.length];
        _brush.getTextWidths(rowString, 0, rowString.length, widths);
        if (len > 0 && widths[len] == 0.0f)
            widths[len - 1] -= (widths[len] = getAdvance(rowString[len]));
        int index, offset = 0;
        String _row = new String(rowString);
        int _length = _row.length();
        while (offset < _length) {
            index = _row.indexOf("-*/", offset + 4);
            if (index != -1) {
                int left = _row.lastIndexOf("/*-", index - 2);
                if (left >= offset) {
                    for (int i = 0; i < 3; ++i) {
                        widths[left + i] = 0;
                        widths[index + i] = 0;
                    }
                }
                offset = index + 3;
            } else
                break;
        }
        int currOffset = 0;
        float left = 0;
        float right = s;
        while (currOffset <= len) {
            left = right;
            char c = rowString[currOffset];
            switch (c) {
                case ' ':
                    right += getSpaceAdvance();
                    break;
                case Language.NEWLINE:
                case Language.EOF:
                    right += getEOLAdvance();
                    break;
                case Language.TAB:
                    right += getTabAdvance();
                    break;
                default:
                    right += widths[currOffset];
                    break;
            }
            //if(currOffset+rowOffset==_caretPosition)right+=2*_density;
            ++currOffset;
        }
        return new Pair((int) left, (int) right);
    }

    protected void makeRowVisible(int rowNumber, int addHeight) {
    /*int scrollBy = 0;
    int charTop = getPaintPosition(rowNumber * rowHeight());
    int charBottom = charTop + rowHeight();
    if (charTop < getScrollY()) {
      scrollBy = charTop - getScrollY();
    } else if (charBottom > (getScrollY() + addHeight)) {
      scrollBy = charBottom - getScrollY() - addHeight;
    }*/
        if (addHeight != 0) {
            _scroller.startScroll(getScrollX(), getScrollY(), 0, addHeight);
            postInvalidate();
        }
    }

    /**
     * Returns the bounding box of a character in the text field.
     * The coordinate system used is one where (0, 0) is the top left corner
     * of the text, before padding is added.
     *
     * @param charOffset The character offset of the character of interest
     * @return Rect(left, top, right, bottom) of the character bounds,
     * or Rect(-1, -1, -1, -1) if there is no character at that coordinate.
     */
    protected Rect getBoundingBox(int charOffset) {
        if (charOffset < 0 || charOffset >= _hDoc.docLength()) {
            return new Rect(-1, -1, -1, -1);
        }
        int row = _hDoc.findRowNumber(charOffset);
        int top = getPaintPosition(row * rowHeight());
        int bottom = top + rowHeight();
        Pair xExtent = getCharExtent(charOffset);
        int left = xExtent.getFirst();
        int right = xExtent.getSecond();
        return new Rect(left, top, right, bottom);
    }

    public ColorScheme getColorScheme() {
        return _colorScheme;
    }

    public void setColorScheme(ColorScheme colorScheme) {
        _colorScheme = colorScheme;
        _navMethod.onColorSchemeChanged(colorScheme);
        setBackgroundColor(colorScheme.getColor(Colorable.BACKGROUND));
    }

    /**
     * Maps a coordinate to the character that it is on. If the coordinate is
     * on empty space, the nearest character on the corresponding row is returned.
     * If there is no character on the row, -1 is returned.
     * <p/>
     * The coordinates passed in should not have padding applied to them.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return The index of the closest character, or -1 if there is
     * no character or nearest character at that coordinate
     */
    int coordToCharIndex(float x, int y) {
        int row = getRealPosition(y) / rowHeight();
        if (row >= _hDoc.getRowCount()) row = _hDoc.getRowCount() - 1;
        int charIndex = _hDoc.getRowOffset(row);
        if (charIndex < 0) {
            //non-existent row
            return -1;
        }
        x -= s;
        if (x < 0) {
            return charIndex; // coordinate is outside, to the left of view
        }
        char[] rowText = _hDoc.subSequence(charIndex, _hDoc.getRowSize(row));
        float[] widths = new float[rowText.length];
        _brush.getTextWidths(rowText, 0, rowText.length, widths);
        int index, offset = 0;
        String _row = new String(rowText);
        int _length = _row.length();
        while (offset < _length) {
            index = _row.indexOf("-*/", offset + 4);
            if (index != -1) {
                int left = _row.lastIndexOf("/*-", index - 2);
                if (left >= offset) {
                    for (int i = 0; i < 3; ++i) {
                        widths[left + i] = 0;
                        widths[index + i] = 0;
                    }
                }
                offset = index + 3;
            } else
                break;
        }
        float extent = 0;
        int i = 0;
        float half;
        while (i < rowText.length) {
            char c = rowText[i];
            if (c == Language.NEWLINE || c == Language.EOF) {
                extent += (half = getEOLAdvance());
            } else if (c == ' ') {
                extent += (half = getSpaceAdvance());
            } else if (c == Language.TAB) {
                extent += (half = getTabAdvance());
            } else {
                extent += (half = widths[i]);
            }
            if (extent - (half * 0.5f) >= x) {
                break;
            }
            ++i;
        }
        if (i < rowText.length) {
            return charIndex + i;
        }
        //nearest char is last char of line
        return charIndex + i - 1;
    }

    /**
     * Maps a coordinate to the character that it is on.
     * Returns -1 if there is no character on the coordinate.
     * <p/>
     * The coordinates passed in should not have padding applied to them.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return The index of the character that is on the coordinate,
     * or -1 if there is no character at that coordinate.
     */
    int coordToCharIndexStrict(float x, int y) {
        int row = getRealPosition(y) / rowHeight();
        if (row >= _hDoc.getRowCount()) row = _hDoc.getRowCount() - 1;
        int charIndex = _hDoc.getRowOffset(row);
        x -= s;
        if (charIndex < 0 || x < 0) {
            //non-existent row
            return -1;
        }
        char[] rowText = _hDoc.subSequence(charIndex, _hDoc.getRowSize(row));
        float[] widths = new float[rowText.length];
        _brush.getTextWidths(rowText, 0, rowText.length, widths);
        int index, offset = 0;
        String _row = new String(rowText);
        int _length = _row.length();
        while (offset < _length) {
            index = _row.indexOf("-*/", offset + 4);
            if (index != -1) {
                int left = _row.lastIndexOf("/*-", index - 2);
                if (left >= offset) {
                    for (int i = 0; i < 3; ++i) {
                        widths[left + i] = 0;
                        widths[index + i] = 0;
                    }
                }
                offset = index + 3;
            } else
                break;
        }
        float extent = 0;
        int i = 0;
        float half;
        while (i < rowText.length) {
            char c = rowText[i];
            if (c == Language.NEWLINE || c == Language.EOF) {
                extent += (half = getEOLAdvance());
            } else if (c == ' ') {
                extent += (half = getSpaceAdvance());
            } else if (c == Language.TAB) {
                extent += (half = getTabAdvance());
            } else {
                extent += (half = widths[i]);
            }
            if (extent - (half * 0.5f) >= x) {
                break;
            }
            ++i;
        }
        if (i < rowText.length) {
            return charIndex + i;
        }
        //no char enclosing x
        return -1;
    }

    /**
     * Not private to allow access by TouchNavigationMethod
     *
     * @return The maximum x-value that can be scrolled to for the current rows
     * of text in the viewport.
     */
    int getMaxScrollX() {
        return Math.max(0,
                _xExtent - getContentWidth() + _navMethod.getCaretBloat().right);
    }

    /**
     * Not private to allow access by TouchNavigationMethod
     *
     * @return The maximum y-value that can be scrolled to.
     */
    int getMaxScrollY() {
        return Math.max(0,
                _hDoc.getRowCount() * rowHeight() + getPlusHeight() - getContentHeight() + _navMethod.getCaretBloat().bottom);
    }

    protected abstract int getPlusHeight();

    @Override
    protected int computeVerticalScrollOffset() {
        return getScrollY();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return _hDoc.getRowCount() * rowHeight() + getPaddingTop() + getPaddingBottom();
    }

    @Override
    public void computeScroll() {
        if (_scroller.computeScrollOffset()) {
            scrollTo(_scroller.getCurrX(), _scroller.getCurrY());
        }
    }

    /**
     * Start fling scrolling
     */
    void flingScroll(int velocityX, int velocityY) {
        _scroller.fling(getScrollX(), getScrollY(), velocityX, velocityY,
                Math.min(0, getScrollX()), getMaxScrollX(), Math.min(0, getScrollY()), getMaxScrollY());
        // Keep on drawing until the animation has finished.
        postInvalidate();
    }

    public boolean isFlingScrolling() {
        return !_scroller.isFinished();
    }

    public void stopFlingScrolling() {
        _scroller.forceFinished(true);
    }

    /**
     * Starting scrolling continuously in scrollDir.
     * Not private to allow access by TouchNavigationMethod.
     *
     * @return True if auto-scrolling started
     */
    boolean autoScrollCaret(int scrollDir) {
        boolean scrolled = false;
        switch (scrollDir) {
            case SCROLL_UP:
                removeCallbacks(_scrollCaretUpTask);
                if ((!caretOnFirstRowOfFile())) {
                    post(_scrollCaretUpTask);
                    scrolled = true;
                }
                break;
            case SCROLL_DOWN:
                removeCallbacks(_scrollCaretDownTask);
                if (!caretOnLastRowOfFile()) {
                    post(_scrollCaretDownTask);
                    scrolled = true;
                }
                break;
            case SCROLL_LEFT:
                removeCallbacks(_scrollCaretLeftTask);
                if (_caretPosition > 0 &&
                        _caretRow == _hDoc.findRowNumber(_caretPosition - 1)) {
                    post(_scrollCaretLeftTask);
                    scrolled = true;
                }
                break;
            case SCROLL_RIGHT:
                removeCallbacks(_scrollCaretRightTask);
                if (!caretOnEOF() &&
                        _caretRow == _hDoc.findRowNumber(_caretPosition + 1)) {
                    post(_scrollCaretRightTask);
                    scrolled = true;
                }
                break;
            default:
                TextWarriorException.fail("Invalid scroll direction");
                break;
        }
        return scrolled;
    }

    /**
     * Stops automatic scrolling initiated by autoScrollCaret(int).
     * Not private to allow access by TouchNavigationMethod
     */
    void stopAutoScrollCaret() {
        removeCallbacks(_scrollCaretDownTask);
        removeCallbacks(_scrollCaretUpTask);
        removeCallbacks(_scrollCaretLeftTask);
        removeCallbacks(_scrollCaretRightTask);
    }

    /**
     * Stops automatic scrolling in scrollDir direction.
     * Not private to allow access by TouchNavigationMethod
     */
    void stopAutoScrollCaret(int scrollDir) {
        switch (scrollDir) {
            case SCROLL_UP:
                removeCallbacks(_scrollCaretUpTask);
                break;
            case SCROLL_DOWN:
                removeCallbacks(_scrollCaretDownTask);
                break;
            case SCROLL_LEFT:
                removeCallbacks(_scrollCaretLeftTask);
                break;
            case SCROLL_RIGHT:
                removeCallbacks(_scrollCaretRightTask);
                break;
            default:
                TextWarriorException.fail("Invalid scroll direction");
                break;
        }
    }

    //---------------------------------------------------------------------
    //------------------------- Caret methods -----------------------------
    public int getCaretRow() {
        return _caretRow;
    }

    public int getCaretPosition() {
        return _caretPosition;
    }

    /**
     * Sets the caret to position i, scrolls it to view and invalidates
     * the necessary areas for redrawing
     *
     * @param i The character index that the caret should be set to
     */
    public void moveCaret(int i) {
        _fieldController.moveCaret(i);
    }

    /**
     * Sets the caret one position back, scrolls it on screen, and invalidates
     * the necessary areas for redrawing.
     * <p/>
     * If the caret is already on the first character, nothing will happen.
     */
    public void moveCaretLeft() {
        _fieldController.moveCaretLeft(false);
    }

    /**
     * Sets the caret one position forward, scrolls it on screen, and
     * invalidates the necessary areas for redrawing.
     * <p/>
     * If the caret is already on the last character, nothing will happen.
     */
    public void moveCaretRight() {
        _fieldController.moveCaretRight(false);
    }

    /**
     * Sets the caret one row down, scrolls it on screen, and invalidates the
     * necessary areas for redrawing.
     * <p/>
     * If the caret is already on the last row, nothing will happen.
     */
    public void moveCaretDown() {
        _fieldController.moveCaretDown();
    }

    /**
     * Sets the caret one row up, scrolls it on screen, and invalidates the
     * necessary areas for redrawing.
     * <p/>
     * If the caret is already on the first row, nothing will happen.
     */
    public void moveCaretUp() {
        _fieldController.moveCaretUp();
    }

    /**
     * Scrolls the caret into view if it is not on screen
     */
    public void focusCaret() {
        makeCharVisible(_caretPosition);
    }

    /**
     * @return The column number where charOffset appears on
     */
    protected int getColumn(int charOffset) {
        int row = _hDoc.findRowNumber(charOffset);
        TextWarriorException.assertVerbose(row >= 0,
                "Invalid char offset given to getColumn");
        int firstCharOfRow = _hDoc.getRowOffset(row);
        return charOffset - firstCharOfRow;
    }

    protected boolean caretOnFirstRowOfFile() {
        return (_caretRow == 0);
    }

    protected boolean caretOnLastRowOfFile() {
        return (_caretRow == (_hDoc.getRowCount() - 1));
    }

    protected boolean caretOnEOF() {
        return (_caretPosition == (_hDoc.docLength() - 1));
    }

    //---------------------------------------------------------------------
    //------------------------- Text Selection ----------------------------
    public final boolean isSelectText() {
        return _fieldController.isSelectText();
    }

    /**
     * Enter or exit select mode.
     * Invalidates necessary areas for repainting.
     *
     * @param mode If true, enter select mode; else exit select mode
     */
    public void selectText(boolean mode) {
        if (_fieldController.isSelectText() && !mode) {
            invalidateSelectionRows();
            _fieldController.setSelectText(false);
        } else if (!_fieldController.isSelectText() && mode) {
            invalidateCaretRow();
            _fieldController.setSelectText(true);
        }
    }

    public void selectAll() {
        _fieldController.setSelectionRange(0, _hDoc.docLength() - 1, false);
    }

    public void setSelectionRange(int beginPosition, int numChars) {
        _fieldController.setSelectionRange(beginPosition, numChars, true);
    }

    public boolean inSelectionRange(int charOffset) {
        return _fieldController.inSelectionRange(charOffset);
    }

    public int getSelectionStart() {
        return _selectionAnchor;
    }

    public int getSelectionEnd() {
        return _selectionEdge;
    }

    public void focusSelectionStart() {
        _fieldController.focusSelection(true);
    }

    public void focusSelectionEnd() {
        _fieldController.focusSelection(false);
    }

    public void cut(ClipboardManager cb) {
        selectMark();
        _fieldController.cut(cb);
    }

    public void copy(ClipboardManager cb) {
        selectMark();
        _fieldController.copy(cb);
    }

    public void paste(String text) {
        selectMark();
        _fieldController.paste(text);
    }

    //---------------------------------------------------------------------
    //------------------------- Formatting methods ------------------------
    private boolean reachedNextSpan(int charIndex, Pair span) {
        return (span != null) && (charIndex == span.getFirst());
    }

    public void respan() {
        _fieldController.determineSpans();
    }

    public void cancelSpanning() {
        _fieldController.cancelSpanning();
    }

    /**
     * Sets the text to use the new typeface, scrolls the view to display the
     * caret if needed, and invalidates the entire view
     */
    public void setAntiAlias(boolean value) {
        if (_brush != null) {
            _brush.setAntiAlias(value);
            onBrushChanged(_brush);
            postInvalidate();
        }
    }

    public void setTypeface(Typeface typeface) {
        Rect from = getBoundingBox(_caretPosition);
        _brush.setTypeface(typeface);
        zeroSize = getAdvance('0');
        s = zeroSize * 4;
        _hDoc.analyzeWordWrap();
        onBrushChanged(_brush);
        Rect to = getBoundingBox(_caretPosition);
        _scroller.forceFinished(true);
        scrollTo(Math.max(0, getScrollX() + to.left - from.left), Math.max(0, getScrollY() + to.centerY() - from.centerY()));
        _fieldController.updateCaretRow();
        if (!makeCharVisible(_caretPosition)) {
            invalidate();
        }
    }

    /**
     * Sets the text size to be factor of the base text size, scrolls the view
     * to display the caret if needed, and invalidates the entire view
     */
    public void setZoom(float factor) {
        if (factor <= 0) {
            return;
        }
        int newSize = (int) (factor * _density);
        if (newSize == (int) _brush.getTextSize()) return;
        Rect from = getBoundingBox(_caretPosition);
        _brush.setTextSize(newSize);
        zeroSize = getAdvance('0');
        s = zeroSize * 4;
        _hDoc.analyzeWordWrap();
        _fieldController.updateCaretRow();
        onBrushChanged(_brush);
        Rect to = getBoundingBox(_caretPosition);
        _scroller.forceFinished(true);
        scrollTo(Math.max(0, getScrollX() + to.left - from.left), Math.max(0, getScrollY() + to.centerY() - from.centerY()));
        if (!makeCharVisible(_caretPosition)) {
            postInvalidate();
        } else postInvalidate();
    }

    /**
     * Sets the length of a tab character, scrolls the view to display the
     * caret if needed, and invalidates the entire view
     *
     * @param spaceCount The number of spaces a tab represents
     */
    public void setTabSpaces(int spaceCount) {
        if (spaceCount <= 0 || spaceCount == _tabLength) {
            return;
        }
        replacement = "";
        for (int i = 0; i < spaceCount; ++i) replacement += " ";
        _tabLength = spaceCount;
        _hDoc.analyzeWordWrap();
        _fieldController.updateCaretRow();
        if (!makeCharVisible(_caretPosition)) {
            invalidate();
        }
    }

    /**
     * Enable/disable auto-indent
     */
    public void setAutoIndent(boolean enable) {
        _isAutoIndent = enable;
    }

    /**
     * Enable/disable long-pressing capitalization.
     * When enabled, a long-press on a hardware key capitalizes that letter.
     * When disabled, a long-press on a hardware key bring up the
     * CharacterPickerDialog, if there are alternative characters to choose from.
     */
    public void setLongPressCaps(boolean enable) {
        _isLongPressCaps = enable;
    }

    /**
     * Enable/disable highlighting of the current row. The current row is also
     * invalidated
     */
    public void setHighlightCurrentRow(boolean enable) {
        _isHighlightRow = enable;
        invalidateCaretRow();
    }

    /**
     * Enable/disable display of visible representations of non-printing
     * characters like spaces, tabs and end of lines
     * Invalidates the view if the enable state changes
     */
    public void setNonPrintingCharVisibility(boolean enable) {
        if (enable ^ _showNonPrinting) {
            _showNonPrinting = enable;
            _hDoc.analyzeWordWrap();
            _fieldController.updateCaretRow();
            if (!makeCharVisible(_caretPosition)) {
                invalidate();
            }
        }
    }

    //---------------------------------------------------------------------
    //------------------------- Event handlers ----------------------------
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        //Intercept multiple key presses of printing characters to implement
        //long-press caps, because the IME may consume them and not pass the
        //event to onKeyDown() for long-press caps logic to work.
        //TODO Technically, long-press caps should be implemented in the IME,
        //but is put here for end-user's convenience. Unfortunately this may
        //cause some IMEs to break. Remove this feature in future.
        if (_isLongPressCaps
                && event.getRepeatCount() == 1
                && event.getAction() == KeyEvent.ACTION_DOWN) {
            char c = KeysInterpreter.keyEventToPrintableChar(event);
            if (Character.isLowerCase(c)
                    && c == Character.toLowerCase(_hDoc.charAt(_caretPosition - 1))) {
                _fieldController.onPrintableChar(Language.BACKSPACE);
                _fieldController.onPrintableChar(Character.toUpperCase(c));
                return true;
            }
        }
        return super.onKeyPreIme(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Let touch navigation method intercept key event first
        if (_navMethod.onKeyDown(keyCode, event)) {
            return true;
        }
        //check if direction or symbol key
        if (KeysInterpreter.isNavigationKey(event)) {
            handleNavigationKey(keyCode, event);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_SYM ||
                keyCode == KeyCharacterMap.PICKER_DIALOG_INPUT) {
            showCharacterPicker(
                    PICKER_SETS.get(KeyCharacterMap.PICKER_DIALOG_INPUT), false);
            return true;
        }
        if (readOnly) return super.onKeyDown(keyCode, event);
        //check if character is printable
        char c = KeysInterpreter.keyEventToPrintableChar(event);
        if (c == Language.NULL_CHAR) {
            return super.onKeyDown(keyCode, event);
        }
        int repeatCount = event.getRepeatCount();
        //handle multiple (held) key presses
        if (repeatCount == 1) {
            if (_isLongPressCaps) {
                handleLongPressCaps(c);
            } else {
                handleLongPressDialogDisplay(c);
            }
        } else if (repeatCount == 0
                || _isLongPressCaps && !Character.isLowerCase(c)
                || !_isLongPressCaps && PICKER_SETS.get(c) == null) {
            _fieldController.onPrintableChar(c);
        }
        return true;
    }

    private void handleNavigationKey(int keyCode, KeyEvent event) {
        if (event.isShiftPressed() && !isSelectText()) {
            invalidateCaretRow();
            _fieldController.setSelectText(true);
        } else if (!event.isShiftPressed() && isSelectText()) {
            invalidateSelectionRows();
            _fieldController.setSelectText(false);
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                _fieldController.moveCaretRight(false);
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                _fieldController.moveCaretLeft(false);
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                _fieldController.moveCaretDown();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                _fieldController.moveCaretUp();
                break;
            default:
                break;
        }
    }

    private void handleLongPressCaps(char c) {
        if (Character.isLowerCase(c)
                && c == _hDoc.charAt(_caretPosition - 1)) {
            _fieldController.onPrintableChar(Language.BACKSPACE);
            _fieldController.onPrintableChar(Character.toUpperCase(c));
        } else {
            _fieldController.onPrintableChar(c);
        }
    }

    //Precondition: If c is alphabetical, the character before the caret is
    //also c, which can be lower- or upper-case
    private void handleLongPressDialogDisplay(char c) {
        //workaround to get the appropriate caps mode to use
        boolean isCaps = Character.isUpperCase(_hDoc.charAt(_caretPosition - 1));
        char base = (isCaps) ? Character.toUpperCase(c) : c;
        String candidates = PICKER_SETS.get(base);
        if (candidates != null) {
            _fieldController.stopTextComposing();
            showCharacterPicker(candidates, true);
        } else {
            _fieldController.onPrintableChar(c);
        }
    }

    /**
     * @param candidates A string of characters to for the user to choose from
     * @param replace    If true, the character before the caret will be replaced
     *                   with the user-selected char. If false, the user-selected char will
     *                   be inserted at the caret position.
     */
    private void showCharacterPicker(String candidates, boolean replace) {
        final boolean shouldReplace = replace;
        final SpannableStringBuilder dummyString = new SpannableStringBuilder();
        Selection.setSelection(dummyString, 0);
        CharacterPickerDialog dialog = new CharacterPickerDialog(getContext(),
                this, dummyString, candidates, true);
        dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (dummyString.length() > 0) {
                    if (shouldReplace) {
                        _fieldController.onPrintableChar(Language.BACKSPACE);
                    }
                    _fieldController.onPrintableChar(dummyString.charAt(0));
                }
            }
        });
        dialog.show();
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (_navMethod.onKeyUp(keyCode, event)) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        // TODO Test on real device
        int deltaX = Math.round(event.getX());
        int deltaY = Math.round(event.getY());
        while (deltaX > 0) {
            _fieldController.moveCaretRight(false);
            --deltaX;
        }
        while (deltaX < 0) {
            _fieldController.moveCaretLeft(false);
            ++deltaX;
        }
        while (deltaY > 0) {
            _fieldController.moveCaretDown();
            --deltaY;
        }
        while (deltaY < 0) {
            _fieldController.moveCaretUp();
            ++deltaY;
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        _navMethod.onTouchEvent(event);
        if (isFocused()) {
        } else {
            if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP
                    && isPointInView((int) event.getX(), (int) event.getY())) {
                // somehow, the framework does not automatically change the focus
                // to this view when it is touched
                //requestFocus();
            }
        }
        return true;
    }

    final private boolean isPointInView(int x, int y) {
        return (x >= 0 && x < getWidth() &&
                y >= 0 && y < getHeight());
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        invalidateCaretRow();
        if (isInEditMode()) return;
        showIME(isFocused());
    }

    /**
     * Not public to allow access by {@link TouchNavigationMethod}
     */
    void showIME(boolean show) {
        if (readOnly) show = false;
        //if (TextWarriorApplication.KeyboardHided != show) return;
        InputMethodManager im = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        //im.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        if (show) {
            im.showSoftInput(this, 0);
        } else {
            im.hideSoftInputFromWindow(this.getWindowToken(), 0);
        }
    }

    public void onPause() {
        _navMethod.onPause();
    }

    protected void onResume() {
        _navMethod.onResume();
    }

    protected void onDestroy() {
        _fieldController.cancelSpanning();
    }

    public Parcelable getUiState() {
        return new TextFieldUiState(this);
    }

    public void restoreUiState(Parcelable state) {
        final TextFieldUiState uiState = (TextFieldUiState) state;
        final int caretPosition = uiState._caretPosition;
        // If the text field is in the process of being created, it may not
        // have its width and height set yet.
        // Therefore, post UI restoration tasks to run later.
        if (uiState._selectMode) {
            final int selStart = uiState._selectBegin;
            final int selEnd = uiState._selectEnd;
            post(new Runnable() {
                @Override
                public void run() {
                    setSelectionRange(selStart, selEnd - selStart);
                    if (caretPosition < selEnd) {
                        focusSelectionStart(); //caret at the end by default
                    }
                    scrollTo(uiState._scrollX, uiState._scrollY);
                }
            });
        } else {
            post(new Runnable() {
                @Override
                public void run() {
                    moveCaret(caretPosition);
                    int _newHeight = getContentHeight();
                    if (_newHeight != uiState._height) {
                        int _top = getBoundingBox(caretPosition).centerY();
                        int _scrollY = (_top - (uiState._top - uiState._scrollY) * _newHeight / uiState._height);
                        _scroller.forceFinished(true);
                        scrollTo(uiState._scrollX, _scrollY);
                    } else scrollTo(uiState._scrollX, uiState._scrollY);
                }
            });
        }
    }

    @Override
    protected void onAttachedToWindow() {
        if (!isInEditMode()) doNotUsePost = false;
        super.onAttachedToWindow();
    }

    protected abstract void userInputed(char text);

    protected abstract void selectMark();

    protected abstract String checkPrintableChar(String text);

    protected abstract char checkPrintableChar(char c);

    protected abstract String onPaste(String text);

    protected abstract void onCaretPositionChanged();

    //*********************************************************************
    //**************** UI State for saving and restoring ******************
    //*********************************************************************
//TODO change private
    public static class TextFieldUiState implements Parcelable {
        public static final Parcelable.Creator<TextFieldUiState> CREATOR
                = new Parcelable.Creator<TextFieldUiState>() {
            @Override
            public TextFieldUiState createFromParcel(Parcel in) {
                return new TextFieldUiState(in);
            }

            @Override
            public TextFieldUiState[] newArray(int size) {
                return new TextFieldUiState[size];
            }
        };
        final int _caretPosition;
        final int _scrollX;
        final int _scrollY;
        final boolean _selectMode;
        final int _selectBegin;
        final int _selectEnd;
        final int _height;
        final int _top;

        public TextFieldUiState(FreeScrollingTextField textField) {
            _caretPosition = textField.getCaretPosition();
            _scrollX = textField.getScrollX();
            _scrollY = textField.getScrollY();
            Rect bound = textField.getBoundingBox(_caretPosition);
            _top = bound.centerY();
            _height = textField.getContentHeight();
            _selectMode = textField.isSelectText();
            _selectBegin = textField.getSelectionStart();
            _selectEnd = textField.getSelectionEnd();
        }

        protected TextFieldUiState(Parcel in) {
            _caretPosition = in.readInt();
            _scrollX = in.readInt();
            _scrollY = in.readInt();
            _top = in.readInt();
            _height = in.readInt();
            _selectMode = in.readInt() != 0;
            _selectBegin = in.readInt();
            _selectEnd = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeInt(_caretPosition);
            out.writeInt(_scrollX);
            out.writeInt(_scrollY);
            out.writeInt(_top);
            out.writeInt(_height);
            out.writeInt(_selectMode ? 1 : 0);
            out.writeInt(_selectBegin);
            out.writeInt(_selectEnd);
        }
    }

    //*********************************************************************
    //************************ Controller logic ***************************
    //*********************************************************************
    private class TextFieldController
            implements Lexer.LexCallback {
        private final Lexer _lexer = new Lexer(this);
        private boolean _isInSelectionMode = false;

        /**
         * Analyze the text for programming language keywords and redraws the
         * text view when done. The global programming language used is set with
         * the static method Lexer.setLanguage(Language)
         * <p/>
         * Does nothing if the Lexer language is not a programming language
         */
        public void determineSpans() {
            _lexer.tokenize(_hDoc);
        }

        public void cancelSpanning() {
            _lexer.cancelTokenize();
        }

        @Override
        //This is usually called from a non-UI thread
        public void lexDone(final List<Pair> results) {
            if (doNotUsePost) {
                _hDoc.setSpans(results);
            } else
                post(new Runnable() {
                    @Override
                    public void run() {
                        _hDoc.setSpans(results);
                        invalidate();
                    }
                });
        }

        //- TextFieldController -----------------------------------------------
        //---------------------------- Key presses ----------------------------
        //TODO minimise invalidate calls from moveCaret(), insertion/deletion and word wrap
        public void onPrintableChar(char c) {
            selectMark();
            boolean selectionDeleted = false;
            int _edge;
            if (_isInSelectionMode) {
                _edge = _selectionEdge;
                selectionDelete();
                selectionDeleted = true;
            } else _edge = _caretPosition;
            int originalRow = _caretRow;
            int originalOffset = _hDoc.getRowOffset(originalRow);
            switch (c) {
                case Language.BACKSPACE:
                    if (selectionDeleted) {
                        break;
                    }
                    if (_caretPosition > 0) {
                        _hDoc.deleteAt(_caretPosition - 1, System.nanoTime());
                        moveCaretLeft(true);
                        if (_caretRow < originalRow) {
                            // either a newline was deleted or the caret was on the
                            // first word and it became short enough to fit the prev
                            // row
                            invalidateFromRow(_caretRow);
                        } else if (_hDoc.isWordWrap()) {
                            if (originalOffset != _hDoc.getRowOffset(originalRow)) {
                                //invalidate previous row too if its wrapping changed
                                --originalRow;
                            }
                            //TODO invalidate damaged rows only
                            invalidateFromRow(originalRow);
                        }
                    }
                    break;
                case Language.NEWLINE:
                    if (_isAutoIndent) {
                        char[] indent = createAutoIndent();
                        _hDoc.insertBefore(indent, _caretPosition, System.nanoTime());
                        moveCaret(_caretPosition + indent.length);
                    } else {
                        _hDoc.insertBefore(c, _caretPosition, System.nanoTime());
                        moveCaretRight(true);
                    }
                    if (_hDoc.isWordWrap() && originalOffset != _hDoc.getRowOffset(originalRow)) {
                        //invalidate previous row too if its wrapping changed
                        --originalRow;
                    }
                    invalidateFromRow(originalRow);
                    break;
                default:
                    c = checkPrintableChar(c);
                    _hDoc.insertBefore(c, _caretPosition, System.nanoTime());
                    moveCaretRight(true);
                    if (_hDoc.isWordWrap()) {
                        if (originalOffset != _hDoc.getRowOffset(originalRow)) {
                            //invalidate previous row too if its wrapping changed
                            --originalRow;
                        }
                        //TODO invalidate damaged rows only
                        invalidateFromRow(originalRow);
                    }
                    userInputed(c);
                    break;
            }
            int _movCount = _caretPosition - _edge;
            if (_movCount == -1 || _movCount > 0) {
                List<Pair> _spans = _hDoc.getSpans();
                int endRow = (getRealPosition(getScrollY() + getContentHeight())) / rowHeight() + 1;
                endRow = _hDoc.getRowOffset(endRow);
                if (endRow == -1) endRow = _hDoc.docLength();
                int last = 0;
                for (Pair i : _spans) {
                    int first = i.getFirst();
                    if ((last - first < _movCount) && first >= _edge) {
                        if (first >= endRow) break;
                        i.setFirst(first + _movCount);
                    } else last = first;
                }
            }
            setEdited(true);
            determineSpans();
        }

        /**
         * Return a char[] with a newline as the 0th element followed by the
         * leading spaces and tabs of the line that the caret is on
         */
        private char[] createAutoIndent() {
            int lineNum = _hDoc.findLineNumber(_caretPosition);
            int startOfLine = _hDoc.getLineOffset(lineNum);
            int whitespaceCount = 0;
            _hDoc.seekChar(startOfLine);
            while (_hDoc.hasNext()) {
                char c = _hDoc.next();
                if (c != ' ' && c != Language.TAB) {
                    break;
                }
                ++whitespaceCount;
            }
            char[] indent = new char[1 + whitespaceCount];
            indent[0] = Language.NEWLINE;
            _hDoc.seekChar(startOfLine);
            for (int i = 0; i < whitespaceCount; ++i) {
                indent[1 + i] = _hDoc.next();
            }
            return indent;
        }

        public void moveCaretDown() {
            if (!caretOnLastRowOfFile()) {
                int currCaret = _caretPosition;
                int currRow = _caretRow;
                int newRow = currRow + 1;
                Rect boundingBox = getBoundingBox(currCaret);
                _caretPosition = coordToCharIndex(boundingBox.left, boundingBox.centerY() + boundingBox.height());
                ++_caretRow;
                updateSelectionRange(currCaret, _caretPosition);
                if (!makeCharVisible(_caretPosition)) {
                    invalidateRows(currRow, newRow + 1);
                }
                _rowLis.onRowChange(newRow);
                stopTextComposing();
            }
        }

        public void moveCaretUp() {
            if (!caretOnFirstRowOfFile()) {
                int currCaret = _caretPosition;
                int currRow = _caretRow;
                int newRow = currRow + 1;
                Rect boundingBox = getBoundingBox(currCaret);
                _caretPosition = coordToCharIndex(boundingBox.left, boundingBox.centerY() - boundingBox.height());
                --_caretRow;
                updateSelectionRange(currCaret, _caretPosition);
                if (!makeCharVisible(_caretPosition)) {
                    invalidateRows(newRow, currRow + 1);
                }
                _rowLis.onRowChange(newRow);
                stopTextComposing();
            }
        }

        public void moveCaretDown_() {
            if (!caretOnLastRowOfFile()) {
                int currCaret = _caretPosition;
                int currRow = _caretRow;
                int newRow = currRow + 1;
                int currColumn = getColumn(currCaret);
                int currRowLength = _hDoc.getRowSize(currRow);
                int newRowLength = _hDoc.getRowSize(newRow);
                if (currColumn < newRowLength) {
                    // Position at the same column as old row.
                    _caretPosition += currRowLength;
                } else {
                    // Column does not exist in the new row (new row is too short).
                    // Position at end of new row instead.
                    _caretPosition +=
                            currRowLength - currColumn + newRowLength - 1;
                }
                ++_caretRow;
                updateSelectionRange(currCaret, _caretPosition);
                if (!makeCharVisible(_caretPosition)) {
                    invalidateRows(currRow, newRow + 1);
                }
                _rowLis.onRowChange(newRow);
                stopTextComposing();
            }
        }

        public void moveCaretUp_() {
            if (!caretOnFirstRowOfFile()) {
                int currCaret = _caretPosition;
                int currRow = _caretRow;
                int newRow = currRow - 1;
                int currColumn = getColumn(currCaret);
                int newRowLength = _hDoc.getRowSize(newRow);
                if (currColumn < newRowLength) {
                    // Position at the same column as old row.
                    _caretPosition -= newRowLength;
                } else {
                    // Column does not exist in the new row (new row is too short).
                    // Position at end of new row instead.
                    _caretPosition -= (currColumn + 1);
                }
                --_caretRow;
                updateSelectionRange(currCaret, _caretPosition);
                if (!makeCharVisible(_caretPosition)) {
                    invalidateRows(newRow, currRow + 1);
                }
                _rowLis.onRowChange(newRow);
                stopTextComposing();
            }
        }

        /**
         * @param isTyping Whether caret is moved to a consecutive position as
         *                 a result of entering text
         */
        public void moveCaretRight(boolean isTyping) {
            if (!caretOnEOF()) {
                int originalRow = _caretRow;
                ++_caretPosition;
                updateCaretRow();
                updateSelectionRange(_caretPosition - 1, _caretPosition);
                if (!makeCharVisible(_caretPosition)) {
                    invalidateRows(originalRow, _caretRow + 1);
                }
                if (!isTyping) {
                    stopTextComposing();
                }
            }
        }

        /**
         * @param isTyping Whether caret is moved to a consecutive position as
         *                 a result of deleting text
         */
        public void moveCaretLeft(boolean isTyping) {
            if (_caretPosition > 0) {
                int originalRow = _caretRow;
                --_caretPosition;
                updateCaretRow();
                updateSelectionRange(_caretPosition + 1, _caretPosition);
                if (!makeCharVisible(_caretPosition)) {
                    invalidateRows(_caretRow, originalRow + 1);
                }
                if (!isTyping) {
                    stopTextComposing();
                }
            }
        }

        public void moveCaret(int i) {
            if (i < 0 || i >= _hDoc.docLength()) {
                TextWarriorException.fail("Invalid caret position");
                return;
            }
            updateSelectionRange(_caretPosition, i);
            _caretPosition = i;
            updateAfterCaretJump();
        }

        private void updateAfterCaretJump() {
            int oldRow = _caretRow;
            updateCaretRow();
            if (!makeCharVisible(_caretPosition)) {
                invalidateRows(oldRow, oldRow + 1); //old caret row
                invalidateCaretRow(); //new caret row
            }
            stopTextComposing();
        }

        /**
         * This helper method should only be used by internal methods after setting
         * _caretPosition, in order to to recalculate the new row the caret is on.
         */
        void updateCaretRow() {
            int newRow = _hDoc.findRowNumber(_caretPosition);
            if (_caretRow != newRow) {
                _caretRow = newRow;
                _rowLis.onRowChange(newRow);
            }
            onCaretPositionChanged();
        }

        public void stopTextComposing() {
            if (isInEditMode()) return;
            //InputMethodManager im = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            // This is an overkill way to inform the InputMethod that the caret
            // might have changed position and it should re-evaluate the
            // caps mode to use.
            //im.restartInput(FreeScrollingTextField.this);
            if (_inputConnection != null && _inputConnection.isComposingStarted()) {
                _inputConnection.resetComposingState();
            }
        }

        //- TextFieldController -----------------------------------------------
        //-------------------------- Selection mode ---------------------------
        public final boolean isSelectText() {
            return _isInSelectionMode;
        }

        /**
         * Enter or exit select mode.
         * Does not invalidate view.
         *
         * @param mode If true, enter select mode; else exit select mode
         */
        public void setSelectText(boolean mode) {
            if (mode == _isInSelectionMode) {
                return;
            }
            if (mode) {
                _selectionAnchor = _caretPosition;
                _selectionEdge = _caretPosition;
            } else {
                _selectionAnchor = -1;
                _selectionEdge = -1;
            }
            _isInSelectionMode = mode;
            _selModeLis.onSelectionModeChanged(mode);
        }

        public boolean inSelectionRange(int charOffset) {
            if (_selectionAnchor < 0) {
                return false;
            }
            return (_selectionAnchor <= charOffset &&
                    charOffset < _selectionEdge);
        }

        /**
         * Selects numChars count of characters starting from beginPosition.
         * Invalidates necessary areas.
         *
         * @param beginPosition
         * @param numChars
         * @param scrollToStart If true, the start of the selection will be scrolled
         *                      into view. Otherwise, the end of the selection will be scrolled.
         */
        public void setSelectionRange(int beginPosition, int numChars,
                                      boolean scrollToStart) {
            TextWarriorException.assertVerbose(
                    (beginPosition >= 0) && numChars <= (_hDoc.docLength() - 1) && numChars >= 0,
                    "Invalid range to select");
            if (_isInSelectionMode) {
                // unhighlight previous selection
                invalidateSelectionRows();
            } else {
                // unhighlight caret
                invalidateCaretRow();
                setSelectText(true);
            }
            _selectionAnchor = beginPosition;
            _selectionEdge = _selectionAnchor + numChars;
            _caretPosition = _selectionEdge;
            stopTextComposing();
            updateCaretRow();
            boolean scrolled;
            if (scrollToStart) {
                //TODO reduce unnecessary scrolling and write a method to scroll
                // the beginning of multi-line selections as far left as possible
                scrolled = makeCharVisible(_selectionAnchor);
            } else scrolled = makeCharVisible(_selectionEdge);
            if (!scrolled) {
                invalidateSelectionRows();
            }
        }

        /**
         * Moves the caret to an edge of selected text and scrolls it to view.
         *
         * @param start If true, moves the caret to the beginning of
         *              the selection. Otherwise, moves the caret to the end of the selection.
         *              In all cases, the caret is scrolled to view if it is not visible.
         */
        public void focusSelection(boolean start) {
            if (_isInSelectionMode) {
                if (start && _caretPosition != _selectionAnchor) {
                    _caretPosition = _selectionAnchor;
                    updateAfterCaretJump();
                } else if (!start && _caretPosition != _selectionEdge) {
                    _caretPosition = _selectionEdge;
                    updateAfterCaretJump();
                }
            }
        }

        /**
         * Used by internal methods to update selection boundaries when a new
         * caret position is set.
         * Does nothing if not in selection mode.
         */
        private void updateSelectionRange(int oldCaretPosition, int newCaretPosition) {
            if (!_isInSelectionMode) {
                return;
            }
            if (oldCaretPosition < _selectionEdge) {
                if (newCaretPosition > _selectionEdge) {
                    _selectionAnchor = _selectionEdge;
                    _selectionEdge = newCaretPosition;
                } else {
                    _selectionAnchor = newCaretPosition;
                }
            } else {
                if (newCaretPosition < _selectionAnchor) {
                    _selectionEdge = _selectionAnchor;
                    _selectionAnchor = newCaretPosition;
                } else {
                    _selectionEdge = newCaretPosition;
                }
            }
        }
        //- TextFieldController -----------------------------------------------
        //------------------------ Cut, copy, paste ---------------------------

        /**
         * Convenience method for consecutive copy and paste calls
         */
        public void cut(ClipboardManager cb) {
            copy(cb);
            if (readOnly) return;
            selectionDelete();
        }

        /**
         * Copies the selected text to the clipboard.
         * <p/>
         * Does nothing if not in select mode.
         */
        public void copy(ClipboardManager cb) {
            //TODO catch OutOfMemoryError
            if (_isInSelectionMode &&
                    _selectionAnchor < _selectionEdge) {
                char[] contents = _hDoc.subSequence(_selectionAnchor,
                        _selectionEdge - _selectionAnchor);
                //cb.setText(new String(contents));
                cb.setPrimaryClip(ClipData.newPlainText("text", new String(contents)));
            }
        }

        /**
         * Inserts text at the caret position.
         * Existing selected text will be deleted and select mode will end.
         * The deleted area will be invalidated.
         * <p/>
         * After insertion, the inserted area will be invalidated.
         */
        public void paste(String text) {
            if (readOnly) return;
            text = onPaste(text);
            if (text == null) {
                return;
            }
            int _edge = _fieldController._isInSelectionMode ? _selectionEdge : _caretPosition;
            _hDoc.beginBatchEdit();
            selectionDelete();
            int originalRow = _caretRow;
            int originalOffset = _hDoc.getRowOffset(originalRow);
            _hDoc.insertBefore(text.toCharArray(), _caretPosition, System.nanoTime());
            _hDoc.endBatchEdit();
            _caretPosition += text.length();
            int _movCount = _caretPosition - _edge;
            if (_movCount == -1 || _movCount > 0) {
                List<Pair> _spans = _hDoc.getSpans();
                int endRow = (getRealPosition(getScrollY() + getContentHeight())) / rowHeight() + 1;
                endRow = _hDoc.getRowOffset(endRow);
                if (endRow == -1) endRow = _hDoc.docLength();
                int last = 0;
                for (Pair i : _spans) {
                    int first = i.getFirst();
                    if ((last - first < _movCount) && first >= _edge) {
                        if (first >= endRow) break;
                        i.setFirst(first + _movCount);
                    } else last = first;
                }
            }
            updateCaretRow();
            setEdited(true);
            determineSpans();
            stopTextComposing();
            if (!makeCharVisible(_caretPosition)) {
                int invalidateStartRow = originalRow;
                //invalidate previous row too if its wrapping changed
                if (_hDoc.isWordWrap() &&
                        originalOffset != _hDoc.getRowOffset(originalRow)) {
                    --invalidateStartRow;
                }
                if (originalRow == _caretRow && !_hDoc.isWordWrap()) {
                    //pasted text only affects caret row
                    invalidateRows(invalidateStartRow, invalidateStartRow + 1);
                } else {
                    //TODO invalidate damaged rows only
                    invalidateFromRow(invalidateStartRow);
                }
            }
        }

        /**
         * Deletes selected text, exits select mode and invalidates deleted area.
         * If the selected range is empty, this method exits select mode and
         * invalidates the caret.
         * <p/>
         * Does nothing if not in select mode.
         */
        public void selectionDelete() {
            if (!_isInSelectionMode) {
                return;
            }
            int totalChars = _selectionEdge - _selectionAnchor;
            if (totalChars > 0) {
                int originalRow = _hDoc.findRowNumber(_selectionAnchor);
                int originalOffset = _hDoc.getRowOffset(originalRow);
                boolean isSingleRowSel = _hDoc.findRowNumber(_selectionEdge) == originalRow;
                _hDoc.deleteAt(_selectionAnchor, totalChars, System.nanoTime());
                _caretPosition = _selectionAnchor;
                updateCaretRow();
                setEdited(true);
                determineSpans();
                setSelectText(false);
                stopTextComposing();
                if (!makeCharVisible(_caretPosition)) {
                    int invalidateStartRow = originalRow;
                    //invalidate previous row too if its wrapping changed
                    if (_hDoc.isWordWrap() &&
                            originalOffset != _hDoc.getRowOffset(originalRow)) {
                        --invalidateStartRow;
                    }
                    if (isSingleRowSel && !_hDoc.isWordWrap()) {
                        //pasted text only affects current row
                        invalidateRows(invalidateStartRow, invalidateStartRow + 1);
                    } else {
                        //TODO invalidate damaged rows only
                        invalidateFromRow(invalidateStartRow);
                    }
                }
            } else {
                setSelectText(false);
                invalidateCaretRow();
            }
        }
        //- TextFieldController -----------------------------------------------
        //----------------- Helper methods for InputConnection ----------------

        /**
         * Deletes existing selected text, then deletes charCount number of
         * characters starting at from, and inserts text in its place.
         * <p/>
         * Unlike paste or selectionDelete, does not signal the end of
         * text composing to the IME.
         */
        void replaceComposingText(int from, int charCount, String text) {
            selectMark();
            text = checkPrintableChar(text);
            int invalidateStartRow, originalOffset;
            boolean isInvalidateSingleRow = true;
            boolean dirty = false;
            //delete selection
            if (_isInSelectionMode) {
                invalidateStartRow = _hDoc.findRowNumber(_selectionAnchor);
                originalOffset = _hDoc.getRowOffset(invalidateStartRow);
                int totalChars = _selectionEdge - _selectionAnchor;
                if (totalChars > 0) {
                    _caretPosition = _selectionAnchor;
                    _hDoc.deleteAt(_selectionAnchor, totalChars, System.nanoTime());
                    if (invalidateStartRow != _caretRow) {
                        isInvalidateSingleRow = false;
                    }
                    dirty = true;
                }
                setSelectText(false);
            } else {
                invalidateStartRow = _caretRow;
                originalOffset = _hDoc.getRowOffset(_caretRow);
            }
            //delete requested chars
            if (charCount > 0) {
                int delFromRow = _hDoc.findRowNumber(from);
                if (delFromRow < invalidateStartRow) {
                    invalidateStartRow = delFromRow;
                    originalOffset = _hDoc.getRowOffset(delFromRow);
                }
                if (invalidateStartRow != _caretRow) {
                    isInvalidateSingleRow = false;
                }
                _caretPosition = from;
                _hDoc.deleteAt(from, charCount, System.nanoTime());
                dirty = true;
            }
            //insert
            if (text != null && text.length() > 0) {
                int insFromRow = _hDoc.findRowNumber(from);
                if (insFromRow < invalidateStartRow) {
                    invalidateStartRow = insFromRow;
                    originalOffset = _hDoc.getRowOffset(insFromRow);
                }
                _hDoc.insertBefore(text.toCharArray(), _caretPosition, System.nanoTime());
                _caretPosition += text.length();
                dirty = true;
            }
            if (dirty) {
                setEdited(true);
                determineSpans();
            }
            int originalRow = _caretRow;
            updateCaretRow();
            if (originalRow != _caretRow) {
                isInvalidateSingleRow = false;
            }
            if (!makeCharVisible(_caretPosition)) {
                //invalidate previous row too if its wrapping changed
                if (_hDoc.isWordWrap() &&
                        originalOffset != _hDoc.getRowOffset(invalidateStartRow)) {
                    --invalidateStartRow;
                }
                if (isInvalidateSingleRow && !_hDoc.isWordWrap()) {
                    //replaced text only affects current row
                    invalidateRows(_caretRow, _caretRow + 1);
                } else {
                    //TODO invalidate damaged rows only
                    invalidateFromRow(invalidateStartRow);
                }
            }
            if (text != null && text.length() > 0) userInputed(text.charAt(text.length() - 1));
        }

        /**
         * Delete leftLength characters of text before the current caret
         * position, and delete rightLength characters of text after the current
         * cursor position.
         * <p/>
         * Unlike paste or selectionDelete, does not signal the end of
         * text composing to the IME.
         */
        void deleteAroundComposingText(int left, int right) {
            int start = _caretPosition - left;
            if (start < 0) {
                start = 0;
            }
            int end = _caretPosition + right;
            int docLength = _hDoc.docLength();
            if (end > (docLength - 1)) { //exclude the terminal EOF
                end = docLength - 1;
            }
            replaceComposingText(start, end - start, "");
        }

        String getTextAfterCursor(int maxLen) {
            int docLength = _hDoc.docLength();
            if ((_caretPosition + maxLen) > (docLength - 1)) {
                //exclude the terminal EOF
                return new String(
                        _hDoc.subSequence(_caretPosition, docLength - _caretPosition - 1));
            }
            return new String(_hDoc.subSequence(_caretPosition, maxLen));
        }

        String getTextBeforeCursor(int maxLen) {
            int start = _caretPosition - maxLen;
            if (start < 0) {
                start = 0;
            }
            return new String(_hDoc.subSequence(start, _caretPosition - start));
        }
    }//end inner controller class

    //*********************************************************************
    //************************** InputConnection **************************
    //*********************************************************************
  /*
   * Does not provide ExtractedText related methods
	 */
    private class TextFieldInputConnection extends BaseInputConnection {
        private boolean _isComposing = false;
        private int _composingCharCount = 0;
        private ExtractedText extractedText;
        private int metaState;

        public TextFieldInputConnection(FreeScrollingTextField v) {
            super(v, true);
        }

        @Override
        public boolean setComposingRegion(int start, int end) {
            return super.setComposingRegion(start, end);
        }

        public void resetComposingState() {
            _composingCharCount = 0;
            _isComposing = false;
            _hDoc.endBatchEdit();
        }

        /**
         * Only true when the InputConnection has not been used by the IME yet.
         * Can be programatically cleared by resetComposingState()
         */

        public boolean isComposingStarted() {
            return _isComposing;
        }

        @Override
        public boolean setComposingText(CharSequence text, int newCursorPosition) {
            _isComposing = true;
            if (!_hDoc.isBatchEdit()) {
                _hDoc.beginBatchEdit();
            }
            _fieldController.replaceComposingText(
                    getCaretPosition() - _composingCharCount,
                    _composingCharCount,
                    text.toString());
            _composingCharCount = text.length();
            //TODO reduce invalidate calls
            if (newCursorPosition > 1) {
                _fieldController.moveCaret(_caretPosition + newCursorPosition - 1);
            } else if (newCursorPosition <= 0) {
                _fieldController.moveCaret(_caretPosition - text.length() - newCursorPosition);
            }
            return true;
        }

        @Override
        public Editable getEditable() {
            return null;
        }

        @Override
        public boolean clearMetaKeyStates(int states) {
            Log.e(TextWarriorApplication.TAG, "clearMetaKeyStates:" + states);
            if ((states & KeyEvent.META_SHIFT_ON) != 0) metaState &= ~KeyEvent.META_SHIFT_ON;
            if ((states & KeyEvent.META_ALT_ON) != 0) metaState &= ~KeyEvent.META_ALT_ON;
            if ((states & KeyEvent.META_SYM_ON) != 0) metaState &= ~KeyEvent.META_SYM_ON;
            //if ((states&KeyEvent.META_SELECTING) != 0) content.removeSpan(SELECTING);
            //if(isSelectText())selectText(false);
            return super.clearMetaKeyStates(states);
        }

        @Override
        public boolean commitText(CharSequence text, int newCursorPosition) {
            _isComposing = true;
            int _edge = _fieldController._isInSelectionMode ? _selectionEdge : _caretPosition;
            _fieldController.replaceComposingText(
                    getCaretPosition() - _composingCharCount,
                    _composingCharCount,
                    text.toString());
            _composingCharCount = 0;
            _hDoc.endBatchEdit();
            //TODO reduce invalidate calls
            if (newCursorPosition > 1) {
                _fieldController.moveCaret(_caretPosition + newCursorPosition - 1);
            } else if (newCursorPosition <= 0) {
                _fieldController.moveCaret(_caretPosition - text.length() - newCursorPosition);
            }
            int _movCount = _caretPosition - _edge;
            if (_movCount == -1 || _movCount > 0) {
                List<Pair> _spans = _hDoc.getSpans();
                int endRow = (getRealPosition(getScrollY() + getContentHeight())) / rowHeight() + 1;
                endRow = _hDoc.getRowOffset(endRow);
                if (endRow == -1) endRow = _hDoc.docLength();
                int last = 0;
                for (Pair i : _spans) {
                    int first = i.getFirst();
                    if ((last - first < _movCount) && first >= _edge) {
                        if (first >= endRow) break;
                        i.setFirst(first + _movCount);
                    } else last = first;
                }
            }
            return true;
        }

        @Override
        public boolean deleteSurroundingText(int leftLength, int rightLength) {
            if (_composingCharCount != 0) {
                Log.d(TextWarriorApplication.TAG,
                        "Warning: Implmentation of InputConnection.deleteSurroundingText" +
                                " will not skip composing text");
            }
            Log.v("deleteSurroundingText", leftLength + " : " + rightLength);
            if (isSelectText()) {
                leftLength = 0;
                rightLength = 0;
            }
            _fieldController.deleteAroundComposingText(leftLength, rightLength);
            return true;
        }

        @Override
        public boolean finishComposingText() {
            resetComposingState();
            return true;
        }

        @Override
        public int getCursorCapsMode(int reqModes) {
            int capsMode = 0;
            // Ignore InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS; not used in TextWarrior
            if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                    == InputType.TYPE_TEXT_FLAG_CAP_WORDS) {
                int prevChar = _caretPosition - 1;
                if (prevChar < 0 || Lexer.getLanguage().isWhitespace(_hDoc.charAt(prevChar))) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_WORDS;
                    //set CAP_SENTENCES if client is interested in it
                    if ((reqModes & InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                            == InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) {
                        capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                    }
                }
            }
            // Strangely, Android soft keyboard does not set TYPE_TEXT_FLAG_CAP_SENTENCES
            // in reqModes even if it is interested in doing auto-capitalization.
            // Android bug? Therefore, we assume TYPE_TEXT_FLAG_CAP_SENTENCES
            // is always set to be on the safe side.
            else {
                Language lang = Lexer.getLanguage();
                int prevChar = _caretPosition - 1;
                int whitespaceCount = 0;
                boolean capsOn = true;
                // Turn on caps mode only for the first char of a sentence.
                // A fresh line is also considered to start a new sentence.
                // The position immediately after a period is considered lower-case.
                // Examples: "abc.com" but "abc. Com"
                while (prevChar >= 0) {
                    char c = _hDoc.charAt(prevChar);
                    if (c == Language.NEWLINE) {
                        break;
                    }
                    if (!lang.isWhitespace(c)) {
                        if (whitespaceCount == 0 || !lang.isSentenceTerminator(c)) {
                            capsOn = false;
                        }
                        break;
                    }
                    ++whitespaceCount;
                    --prevChar;
                }
                if (capsOn) {
                    capsMode |= InputType.TYPE_TEXT_FLAG_CAP_SENTENCES;
                }
            }
            return capsMode;
        }

        @Override
        public ExtractedText getExtractedText(ExtractedTextRequest request, int flags) {
            ExtractedText ret = super.getExtractedText(request, flags);
            if (ret == null) {
                if (extractedText == null) {
                    extractedText = new ExtractedText();
                    extractedText.text = new StringBuilder();
                }
                ret = extractedText;
                //ret.text = null;//"Text Test\nsecend\nlow";
                if (isSelectText()) {
                    ret.selectionStart = getSelectionStart();
                    ret.selectionEnd = getSelectionEnd();
                    ret.flags = ret.selectionStart == ret.selectionEnd ? 0 : ExtractedText.FLAG_SELECTING;
                } else {
                    ret.selectionStart = getCaretPosition();
                    ret.selectionEnd = ret.selectionStart;
                    ret.flags = 0;
                }
        /*ret.text = _hDoc.getRow(_caretRow);
        ret.startOffset=_hDoc.getRowOffset(_caretRow);
        ret.partialStartOffset=ret.startOffset;
        ret.partialEndOffset=ret.startOffset+ret.text.length();*/
                int length = ret.text.length();
                int newLen = _hDoc.docLength() - 1 - length;
                if (newLen > 0) {
                    char[] cs = new char[newLen];
                    Arrays.fill(cs, '-');
                    ((StringBuilder) ret.text).append(cs);
                } else if (newLen < 0) {
                    ((StringBuilder) ret.text).delete(length + newLen, length);
                }
                //ret.partialEndOffset=_hDoc.docLength()-1;
            }
            return ret;
        }

        @Override
        public CharSequence getTextAfterCursor(int maxLen, int flags) {
            return _fieldController.getTextAfterCursor(maxLen); //ignore flags
        }

        @Override
        public boolean performContextMenuAction(int id) {
            switch (id) {
                case android.R.id.selectAll:
                    onKeyShortcut(KeyEvent.KEYCODE_A, null);
                    break;
                case android.R.id.paste:
                    onKeyShortcut(KeyEvent.KEYCODE_V, null);
                    break;
                case android.R.id.copy:
                    onKeyShortcut(KeyEvent.KEYCODE_C, null);
                    break;
                case android.R.id.cut:
                    onKeyShortcut(KeyEvent.KEYCODE_X, null);
                    break;
                default:
                    return super.performContextMenuAction(id);
            }
            return true;
        }

        @Override
        public CharSequence getTextBeforeCursor(int maxLen, int flags) {
            return _fieldController.getTextBeforeCursor(maxLen); //ignore flags
        }

        @Override
        public boolean setSelection(int start, int end) {
            //Log.e(TextWarriorApplication.TAG,"setSelection:"+start+","+end);
            if (start == end) {
                if (_fieldController.isSelectText()) _fieldController.setSelectText(false);
                _fieldController.moveCaret(start);
            } else {
                _fieldController.setSelectionRange(start, end - start, false);
            }
            return true;
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            //Log.e(TextWarriorApplication.TAG,event.toString());
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int tempMetaState = metaState;
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_SHIFT_LEFT:
                    case KeyEvent.KEYCODE_SHIFT_RIGHT:
                        metaState |= KeyEvent.META_SHIFT_ON;
                        break;
                    case KeyEvent.KEYCODE_ALT_LEFT:
                    case KeyEvent.KEYCODE_ALT_RIGHT:
                        metaState |= KeyEvent.META_ALT_ON;
                        break;
                    case KeyEvent.KEYCODE_SYM:
                        metaState |= KeyEvent.META_SYM_ON;
                        break;
                    //default:event=new KeyEvent(event.getDownTime(),event.getEventTime(),event.getAction(),event.getKeyCode(),event.getRepeatCount(),metaState);
                }
                if (tempMetaState != 0 && event.getMetaState() == 0) {
                    if (KeysInterpreter.isNavigationKey(event)) {
                        event = new KeyEvent(event.getDownTime(), event.getEventTime(), event.getAction(), event.getKeyCode(), event.getRepeatCount(), tempMetaState);
                        handleNavigationKey(event.getKeyCode(), event);
                        return true;
                    }
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                switch (event.getKeyCode()) {
                    case KeyEvent.KEYCODE_SHIFT_LEFT:
                    case KeyEvent.KEYCODE_SHIFT_RIGHT:
                        metaState &= ~KeyEvent.META_SHIFT_ON;
                        break;
                    case KeyEvent.KEYCODE_ALT_LEFT:
                    case KeyEvent.KEYCODE_ALT_RIGHT:
                        metaState &= ~KeyEvent.META_ALT_ON;
                        break;
                    case KeyEvent.KEYCODE_SYM:
                        metaState &= ~KeyEvent.META_SYM_ON;
                        break;
                    //default:event=new KeyEvent(event.getDownTime(),event.getEventTime(),event.getAction(),event.getKeyCode(),event.getRepeatCount(),metaState);
                }
            }
            return super.sendKeyEvent(event);
        }
    }// end inner class
}
