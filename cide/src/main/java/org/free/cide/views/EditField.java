package org.free.cide.views;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethod;
import android.widget.AdapterView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.myopicmobile.textwarrior.EditCallback;
import com.myopicmobile.textwarrior.EditCtrl;
import com.myopicmobile.textwarrior.TextWarriorApplication;
import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;
import com.myopicmobile.textwarrior.androidm.KeysInterpreter;
import com.myopicmobile.textwarrior.common.ColorScheme;
import com.myopicmobile.textwarrior.common.ColorSchemeDark;
import com.myopicmobile.textwarrior.common.ColorSchemeLight;
import com.myopicmobile.textwarrior.common.ColorSchemeObsidian;
import com.myopicmobile.textwarrior.common.ColorSchemeSolarizedDark;
import com.myopicmobile.textwarrior.common.ColorSchemeSolarizedLight;
import com.myopicmobile.textwarrior.common.Document;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Language;
import com.myopicmobile.textwarrior.common.LanguageC;
import com.myopicmobile.textwarrior.common.Lexer;

import org.free.API;
import org.free.cide.R;
import org.free.cide.adapters.CodeListAdapter;
import org.free.cide.app.FileManager;
import org.free.cide.app.FileState;
import org.free.cide.callbacks.PopupList;
import org.free.cide.dialogs.ColorDialog;
import org.free.cide.dialogs.FixableListDialog;
import org.free.cide.dialogs.FunctionListDialog;
import org.free.cide.ide.Clang;
import org.free.cide.ide.CodeHelp;
import org.free.cide.ide.IncludeHelp;
import org.free.cide.ide.MainActivity;
import org.free.cide.tasks.CodeCompilationTask;
import org.free.cide.utils.SystemColorScheme;
import org.free.clangide.ClangAPI;

import java.util.ArrayList;

public class EditField extends FreeScrollingTextField implements ActionMode.Callback, PopupList, AdapterView.OnItemClickListener, PopupWindow.OnDismissListener {
    private static boolean hasParams;
    private final Paint _paint = new Paint();
    private final Paint _paintNormal = new Paint();
    private boolean ErrorOn;
    private Runnable _action;
    private int _ascent;
    private Activity activity;
    private CodeListAdapter adapter;
    private float ascent;
    private boolean bUserTaped;
    private Rect bound;
    private EditCallback callback = new EditCallback() {
        @Override
        public void codeChange() {
        }

        @Override
        public void edited(FreeScrollingTextField editField) {
        }

        @Override
        public void formatLine(int caretRow) {
        }

        @Override
        public boolean onLayout() {
            return false;
        }

        @Override
        public void popCodeCompletion() {
        }
    };
    private CodeHelp codeHelp;
    private FileState current;
    private float descent;
    private int diagonalDisplay = 3;
    private ActionMode editAction;
    private ArrayList<Clang.Diagnostic> errLines = new ArrayList<>();
    private int ex;
    private MyListView mListView;
    private PopupWindow mPopup;
    private int oldLineNumber;
    private boolean oldWordWrap;
    private boolean showLineNumber;
    private int startOffset;

    public EditField(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPopup = new PopupWindow(context, attrs, android.R.attr.autoCompleteTextViewStyle);
        setupPopup();
        init(context);
    }

    public EditField(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mPopup = new PopupWindow(context, attrs, defStyle);
        setupPopup();
        init(context);
    }

    public static PopupList show(MainActivity activity, int count, String[] data, int startOffset, String footerText, String filterString, CodeHelp codeHelp) {
        if (count > 0) {
            PopupList dialog = (PopupList) activity.findViewById(R.id.textField);
            if (dialog != null) {
                Log.e(TextWarriorApplication.TAG, String.format("PopupList show:count=%d", count));
                dialog.post(data, count, startOffset, footerText, filterString, codeHelp);
                dialog.show();
            }
            return dialog;
        }
        return null;
    }

    public void setHasParams(boolean value) {
        hasParams = value;
    }

    private int backToNoneWhitespace() {
        int offset = -1;
        while (Character.isWhitespace(_hDoc.charAt(_caretPosition + offset))) --offset;
        return offset;
    }

    private int findLineEndOffset() {
        if (isSelectText()) _hDoc.seekChar(_selectionEdge);
        else _hDoc.seekChar(_caretPosition);
        char lastChar;
        int seekCount = 0;
        while (_hDoc.hasNext()) {
            ++seekCount;
            if (!Character.isWhitespace(lastChar = _hDoc.next())) {
                if (lastChar != ';') seekCount = 0;
                break;
            }
        }
        return seekCount;
    }

    private int getMarkLeft(int selectionStart) {
        int rowNumber = _hDoc.findRowNumber(selectionStart);
        String row = _hDoc.getRow(rowNumber);
        int rowOffset = _hDoc.getRowOffset(rowNumber);
        int column = selectionStart - rowOffset;
        int index = row.indexOf("-*/", Math.max(4, column - 3));
        if (index != -1) {
            int left = row.lastIndexOf("/*-", index - 2);
            if (left != -1) {
                if (row.indexOf("-*/", left + 4) == index) {
                    if (left <= column) {
                        return Math.min(selectionStart, rowOffset + left);
                    }
                }
            }
        }
        return selectionStart;
    }

    private int getMarkRight(int selectionEnd) {
        int rowNumber = _hDoc.findRowNumber(selectionEnd);
        String row = _hDoc.getRow(rowNumber);
        int rowOffset = _hDoc.getRowOffset(rowNumber);
        int column = selectionEnd - rowOffset;
        int index = row.indexOf("-*/", Math.max(4, column - 3));
        if (index != -1) {
            int left = row.lastIndexOf("/*-", index - 2);
            if (left != -1) {
                if (row.indexOf("-*/", left + 4) == index) {
                    if (left <= column) {
                        return Math.max(selectionEnd, rowOffset + index + 3);
                    }
                }
            }
        }
        return selectionEnd;
    }

    private String getMarkText(int offset) {
        int rowNumber = _hDoc.findRowNumber(offset);
        String row = _hDoc.getRow(rowNumber);
        int rowOffset = _hDoc.getRowOffset(rowNumber);
        int column = offset - rowOffset;
        int index = row.indexOf("-*/", Math.max(4, column - 3));
        if (index != -1) {
            int left = row.lastIndexOf("/*-", index - 2);
            if (left != -1) {
                if (row.indexOf("-*/", left + 4) == index) {
                    if (left <= column) {
                        //setSelectionRange(rowOffset + left, index + 3 - left);
                        return row.substring(left + 3, index);
                    }
                }
            }
        }
        return null;
    }

    private void init(Context context) {
        EditCtrl.readyEditCtrl(context);
        _navMethod.getCaretBloat().bottom = EditCtrl.getHeight();
        //setScrollX(s);
        if (isInEditMode()) {
            _hDoc.setWordWrap(false);
            this.setColorScheme(new ColorSchemeObsidian());
            Document _doc = new Document(this);
            if (!_hDoc.isWordWrap())
                _doc.setWordWrap(false);
            _doc.insert(("#include <stdio.h>\n//Test Color\nint printf(const char*,...);\nint main(){\n  printf(\"Hello World %s\\n\",\"" + Build.VERSION.RELEASE + "\");\n  return 0;\n}").toCharArray(), 0, 0, false);
            DocumentProvider doc = new DocumentProvider(_doc);
            this.setDocumentProvider(doc);
            if (attr_select_start != -1 && attr_select_end != -1) {
                if (attr_select_end > attr_select_start) {
                    this.setSelectionRange(attr_select_start, attr_select_end - attr_select_start);
                } else if (attr_select_start > attr_select_end) {
                    this.setSelectionRange(attr_select_end, attr_select_start - attr_select_end);
                    this.focusSelectionStart();
                } else {
                    this.moveCaret(attr_select_start);
                }
            }
            errLines.add(new Clang.Diagnostic(3, "Error color test", null));
            scrollTo(0, 0);
        } else {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            diagonalDisplay = Integer.valueOf(preferences.getString("diagonalDisplay", "3"));
            String value = preferences.getString("codeTheme", "5");
            setColorScheme(value);
            if (!isWordWrap())
                _hDoc.setWordWrap(false);
            boolean goodFont = preferences.getBoolean("AntiAlias", false);
            setAntiAlias(goodFont);
            value = preferences.getString("font", "0");
            setFont(Integer.valueOf(value));
            value = preferences.getString("font_size", "16.0");
            setZoom(Float.valueOf(value));
            current = new FileState(_hDoc, "Unused");
        }
    }

    private void moveCaretByKeyEvent(KeyEvent event, int newRow) {
        selectText(event.isShiftPressed());
        moveCaret(newRow);
        bUserTaped = true;
    }

    private void myDrawTextBackground(Canvas canvas, int paintX, int paintY, int advance) {
        canvas.drawRect(paintX,
                paintY + ascent,
                paintX + advance,
                paintY + descent,
                _paintNormal);
    }

    @NonNull
    private String removeBlock(String string, String stringLeft, String stringRight) {
        int index, offset = 0;
        String row = string;
        int _length = row.length();
        string = "";
        while (offset < _length) {
            index = row.indexOf(stringRight, offset + 3);
            if (index != -1) {
                int left = row.lastIndexOf(stringLeft, index - 2);
                if (left >= offset) {
                    if (left > offset) string += row.substring(offset, left);
                    ///rows.add(row.substring(left, index + 2));
                    offset = index + 2;
                } else {
                    string += row.substring(offset, index + 1);
                    offset = index + 1;
                }
            } else if (offset > 0) {
                string += row.substring(offset);
                break;
            } else {
                string = row;
                break;
            }
        }
        return string;
    }

    private void selectModeChanged(boolean mode) {
        if (mode) {
            if (_action != null) removeCallbacks(_action);
        } else if (!isSelectText()) {
            if (_action == null) _action = new Runnable() {
                @Override
                public void run() {
                    handOn = false;
                    postInvalidate();
                }
            };
            else removeCallbacks(_action);
            postDelayed(_action, 5000);
        }
        if (!handOn) {
            handOn = true;
            postInvalidate();
        }
    }

    private void setupPopup() {
        mListView = new MyListView(getContext());
        adapter = new CodeListAdapter(mListView);

        mListView.setAdapter(adapter);
        mListView.setOnItemClickListener(this);
    /*{
      Drawable bg = mPopup.getBackground();
      mPopup.setBackgroundDrawable(null);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
        mListView.setBackground(bg);
      } else
        mListView.setBackgroundDrawable(bg);
    }*/
        mPopup.setContentView(mListView);
        mPopup.setInputMethodMode(InputMethod.SHOW_EXPLICIT);
        mPopup.setTouchable(true);
        mPopup.setOutsideTouchable(false);
        mPopup.setOnDismissListener(this);
    }

    public void _onDestroy() {
        onDestroy();
    }

    public void _onResume() {
        onResume();
    }

    public void askSave(String message) {
        Snackbar.make(this, message, Snackbar.LENGTH_SHORT).setAction(R.string.save, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.onKeyShortcut(KeyEvent.KEYCODE_S, null);
            }
        }).show();
    }

    @Override
    public String currentFile() {
        return current.getFileName();
    }

    @Override
    public boolean isCurrent(String fileName) {
        return current.is(fileName);
    }

    @Override
    public boolean isWordWrap() {
        return !isInEditMode() && PreferenceManager.getDefaultSharedPreferences(getContext()).getBoolean("wordwrap", false);
//return _hDoc.isWordWrap();
    }

    @Override
    public void onUserTap() {
        bUserTaped = true;
        selectModeChanged(isSelectText());
    }

    @Override
    public void selectLine(int charOffset) {
        int _line = _hDoc.findLineNumber(charOffset);
        int _offset = _hDoc.getLineOffset(_line);
        if (!isSelectText()) selectText(true);
        moveCaret(_offset);
        if (getSelectionStart() == _offset) focusSelectionEnd();
        else focusSelectionStart();
        _offset = _hDoc.getLineOffset(_line + 1) - 1;
        if (_offset < 0) _offset = _hDoc.docLength() - 1;
        moveCaret(_offset);
        onUserTap();
    }

    @Override
    public void selectWord(int charOffset) {
        int st = charOffset, ss = charOffset, ed = charOffset;
        while (--st >= 0) {
            char c = _hDoc.charAt(st);
            if (Character.isJavaIdentifierPart(c)) ss = st;
            else break;
        }
        st = ss;
        ss = _hDoc.docLength();
        while (ed < ss) {
            char c = _hDoc.charAt(ed);
            if (Character.isJavaIdentifierPart(c)) ++ed;
            else break;
        }
        if (ed >= st) setSelectionRange(st, ed - st);
        else if (_caretPosition != charOffset) {
            selectText(false);
            moveCaret(charOffset);
        }
        onUserTap();
    }

    @Override
    protected void onBrushChanged(Paint brush) {
        descent = brush.getFontMetrics().descent;
        ascent = brush.getFontMetrics().ascent;
        if (_paintNormal != null) _paintNormal.set(brush);
        if (_paint != null) {
            _paint.setTextSize(brush.getTextSize() / 2);
            _paint.setAntiAlias(brush.isAntiAlias());
            _ascent = _paint.getFontMetricsInt().ascent;
        }
        if (errLines != null) {
            if (oldWordWrap) {
                for (Clang.Diagnostic err : errLines) {
                    int offset = _hDoc.getLineOffset(err.note);
                    err.line = _hDoc.findRowNumber(offset);
                }
            } else for (Clang.Diagnostic err : errLines) {
                err.line = err.note;
            }
        }
    }

    @Override
    public void setEdited(boolean set) {
        super.setEdited(set);
        if (set) {
            callback.codeChange();
            post(new Runnable() {
                @Override
                public void run() {
                    callback.edited(EditField.this);
                }
            });
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (callback.onLayout()) {
            if (bound == null) {
                bound = getBoundingBox(_caretPosition);
                bound.left = bound.bottom - getScrollY();
            } else {
                Rect tmp = getBoundingBox(_caretPosition);
                bound.left = tmp.bottom - getScrollY();
            }
            int height = bound.left - MeasureSpec.getSize(heightMeasureSpec) / 4;
            if (height + getScrollY() < 0) height = -getScrollY();
            makeRowVisible(0, height);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected int getRealPosition(int position) {
        if ((diagonalDisplay & 1) == 1) {
            int id = 0;
            for (Clang.Diagnostic i : errLines) {
                if (i.line * rowHeight() - id * _ascent > position) {
                    break;
                }
                ++id;
            }
            return position + id * _ascent;
        } else return position;
    }

    @Override
    protected int getPaintPosition(int position) {
        if ((diagonalDisplay & 1) == 1) {
            int id = 0;
            for (Clang.Diagnostic i : errLines) {
                if (i.line * rowHeight() <= position) {
                    ++id;
                }
            }
            return position - _ascent * id;
        } else return position;
    }

    @Override
    protected void drawAtLast(Canvas canvas) {
        if (handOn) {
            if (this.isSelectText()) {
                int selectionStart = this.getSelectionStart();
                Rect r = this.getBoundingBox(selectionStart);
                int selectionEnd = this.getSelectionEnd();
                if (selectionEnd != selectionStart) {
                    //r.offset(_textField.s-EditCtrl.getLeftWidth()*3/4,0);
                    EditCtrl.drawLeft(canvas, r.left, r.bottom);
                    r = this.getBoundingBox(selectionEnd);
                    //r.offset(_textField.s-(EditCtrl.getRightWidth()>>2),0);
                    EditCtrl.drawRight(canvas, r.left, r.bottom);
                } else {
                    //r.offset(_textField.s-(EditCtrl.getMidWidth()>>1),0);
                    EditCtrl.drawMid(canvas, r.left, r.bottom);
                }
            } else {
                Rect r = this.getBoundingBox(this._caretPosition);
                //r.offset(_textField.s-(EditCtrl.getMidWidth()>>1),0);
                EditCtrl.drawMid(canvas, r.left, r.bottom);
            }
        }
    }

    @Override
    protected float drawAtEachLineBottom(Canvas canvas, int currRowNum, int paintX, int paintY) {
        if (ErrorOn) {
            if (_hDoc.isWordWrap()) {
                int ofs = _hDoc.getRowOffset(currRowNum);
                if (ofs != -1) {
                    ofs += _hDoc.getRowSize(currRowNum) - 1;
                    if (_hDoc.charAt(ofs) != '\n') return 0;
                }
            }
            if ((diagonalDisplay & 2) == 2) {
                _paint.setStrokeWidth(_density);
                _paint.setColor(0xffff3f3f);
                canvas.drawLine(s, paintY + ascent - _density, paintX, paintY + ascent - _density, _paint);
            }
            if ((diagonalDisplay & 1) == 1) {
                int y = 0;
                do {
                    String msg = errLines.get(ex).text;
                    if (msg.charAt(0) == 'w')
                        _paint.setColor(0xffff7f00);
                    else
                        _paint.setColor(0xffff3f3f);
                    canvas.drawText(msg, s, paintY + ascent - _ascent, _paint);
                    paintY -= _ascent;
                    y -= _ascent;
                    ++ex;
                } while (ex < errLines.size() && errLines.get(ex).note == oldLineNumber);
                return y;
            }
        }
        return 0;
    }

    @Override
    protected int drawAtEachLineRight(Canvas canvas, int currRowNum, int paintX, int paintY) {
        /**if (ErrorOn) {
         if (_hDoc.isWordWrap()) {
         int ofs = _hDoc.getRowOffset(currRowNum);
         if (ofs != -1) {
         ofs += _hDoc.getRowSize(currRowNum) - 1;
         if (_hDoc.charAt(ofs) != '\n') return 0;
         }
         }
         if (msg.charAt(0) == 'w')
         _paintNormal.setColor(0xffffff00);
         else
         _paintNormal.setColor(0xffff3f3f);
         _paintNormal.setStrokeWidth(_density);
         canvas.drawLine(s, paintY + descent - _density, paintX, paintY + descent - _density, _paintNormal);
         float dx = _paintNormal.measureText(msg);
         canvas.drawText(msg, paintX , paintY+ ascent, _paintNormal);
         return dx;
         }*/
        return 0;
    }

    @Override
    protected void drawAtEachLineLeft(Canvas canvas, int currIndex, int width, int paintY) {
        int nl = _hDoc.findLineNumber(currIndex) + 1;
        if (nl > oldLineNumber) {
            oldLineNumber = nl;
            ErrorOn = false;
            while (ex < errLines.size()) {
                if ((nl = errLines.get(ex).note) < oldLineNumber) {
                    ++ex;
                    continue;
                }
                if (nl == oldLineNumber) {
                    //msg = errMessages.get(ex);
                    //canvas.drawLine(s-zeroSize,paintY+descent,s,paintY+descent,_brush);
                    ErrorOn = true;
                }
                break;
            }
            if (showLineNumber) {
                _paintNormal.setColor(ErrorOn ? 0xffff0000 : 0xff808080);
                String numberText = String.valueOf(oldLineNumber % 10000);
                float lineX = _paintNormal.measureText(numberText) + zeroSize;
                canvas.drawText(numberText, s - lineX/*(numberText.length()+1)*zeroSize*/, paintY, _paintNormal);
            }
        }
    }

    @Override
    protected void drawBeforeLoop(Canvas canvas, int currRowNum, int endRowNum) {
        int scrollX = getScrollX();
        showLineNumber = s > scrollX;
        oldLineNumber = 0;
        if (_hDoc.isWordWrap() != oldWordWrap) {
            oldWordWrap = _hDoc.isWordWrap();
            if (oldWordWrap) {
                for (Clang.Diagnostic err : errLines) {
                    int offset = _hDoc.getLineOffset(err.note);
                    err.line = _hDoc.findRowNumber(offset);
                }
            } else for (Clang.Diagnostic err : errLines) {
                err.line = err.note;
            }
        }
        ex = 0;
        if (showLineNumber) {
            if (endRowNum >= _hDoc.getRowCount()) endRowNum = _hDoc.getRowCount() - 1;
            float paintX = s - (zeroSize * 0.5f);
            _paintNormal.setColor(0xff808080);
            _paintNormal.setStrokeWidth(_density);
            canvas.drawLine(paintX, 0, paintX, (rowHeight() * (1 + endRowNum)) - (((diagonalDisplay & 1) == 1) ? errLines.size() * _ascent : 0), _paintNormal);
        }
        if (currRowNum <= _caretRow && endRowNum >= _caretRow) {
            if (_colorScheme.isDark())
                _paintNormal.setColor(_colorScheme.getColor(ColorScheme.Colorable.LINE_COLOR));
            else _paintNormal.setColor(0xffccddff);
            myDrawTextBackground(canvas, Math.max(s, scrollX), getPaintBaseline(_caretRow), getWidth() + scrollX - s);
        }
    }

    @Override
    protected int getToolHeight() {
        if (activity != null) {
            View view = activity.findViewById(R.id.quickKeyBarKeysContainer);
            if (null != view && view.isShown()) {
                return view.getMeasuredHeight();
            }
        }
        return 0;
    }

    /**
     * 处理视图转换 Start
     **/
    @Override
    protected int getPlusHeight() {
        if ((diagonalDisplay & 1) == 1)
            return (getContentHeight() >> 1) - _ascent * errLines.size();
        else return 0;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mPopup.isShowing()) {
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                    keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                    keyCode == KeyEvent.KEYCODE_MOVE_HOME ||
                    keyCode == KeyEvent.KEYCODE_MOVE_END) {
                mPopup.getContentView().onKeyDown(keyCode, event);
                return true;
            }
        }
        switch (keyCode) {
            case KeyEvent.KEYCODE_PAGE_UP:
                int newRow = Math.max(0, getCaretRow() - getNumVisibleRows() + 1);
                int column = Math.min(getColumn(_caretPosition), _hDoc.getRowSize(newRow) - 1);
                newRow = _hDoc.getRowOffset(newRow) + column;
                moveCaretByKeyEvent(event, newRow);
                break;
            case KeyEvent.KEYCODE_PAGE_DOWN:
                newRow = Math.min(_hDoc.getRowCount() - 1, getCaretRow() + getNumVisibleRows() - 1);
                column = Math.min(getColumn(_caretPosition), _hDoc.getRowSize(newRow) - 1);
                newRow = _hDoc.getRowOffset(newRow) + column;
                moveCaretByKeyEvent(event, newRow);
                break;
            case KeyEvent.KEYCODE_MOVE_HOME:
                int _newRow = _hDoc.getRowOffset(getCaretRow());
                newRow = _newRow;
                _hDoc.seekChar(newRow);
                while (_hDoc.hasNext()) {
                    char next = _hDoc.next();
                    if (Character.isWhitespace(next) && Language.NEWLINE != next) {
                        ++newRow;
                        continue;
                    }
                    if (newRow == _caretPosition) {
                        newRow = _newRow;
                    }
                    break;
                }
                moveCaretByKeyEvent(event, newRow);
                break;
            case KeyEvent.KEYCODE_MOVE_END:
                _newRow = _hDoc.getRowOffset(getCaretRow()) + _hDoc.getRowSize(getCaretRow()) - 1;
                newRow = _newRow;
                while (newRow > 0) {
                    char next = _hDoc.charAt(newRow - 1);
                    if (Character.isWhitespace(next) && Language.NEWLINE != next) {
                        --newRow;
                        continue;
                    }
                    if (newRow == _caretPosition) {
                        newRow = _newRow;
                    }
                    break;
                }
                moveCaretByKeyEvent(event, newRow);
                break;
            case KeyEvent.KEYCODE_FORWARD_DEL:
                if (!isSelectText() && !readOnly) {
                    selectText(true);
                    moveCaretRight();
                    paste("");
                    bUserTaped = true;
                }
                break;
            default:
                if (KeysInterpreter.isNavigationKey(event)) {
                    bUserTaped = true;
                }
                if (event != null)
                    return super.onKeyDown(keyCode, event);
        }
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (mPopup.isShowing()) {
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                    keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                    keyCode == KeyEvent.KEYCODE_PAGE_UP ||
                    keyCode == KeyEvent.KEYCODE_PAGE_DOWN ||
                    keyCode == KeyEvent.KEYCODE_MOVE_HOME ||
                    keyCode == KeyEvent.KEYCODE_MOVE_END) {
                mPopup.getContentView().onKeyUp(keyCode, event);
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (activity != null) {
            View view = activity.findViewById(R.id.quickKeyBar);
            if (null != view && view.isEnabled()) view.setVisibility(gainFocus ? VISIBLE : INVISIBLE);
        }
    }

    @Override
    public void onPause() {
        current.saveState(this);
        super.onPause();
    }

    @Override
    protected void userInputed(char c) {
        if ('}' == c || ';' == c) {
            callback.formatLine(_caretRow);
        }
    }

    @Override
    protected void selectMark() {
        if (isSelectText()) {
            int start = getSelectionStart();
            int st = getMarkLeft(start);
            int end = getSelectionEnd();
            int ed = getMarkRight(end);
            if (start != ed || end != ed) setSelectionRange(st, ed - st);
        } else {
            String row = _hDoc.getRow(_caretRow);
            int rowOffset = _hDoc.getRowOffset(_caretRow);
            int column = _caretPosition - rowOffset;
            int index = row.indexOf("-*/", Math.max(4, column - 3));
            if (index != -1) {
                int left = row.lastIndexOf("/*-", index - 2);
                if (left != -1) {
                    if (row.indexOf("-*/", left + 4) == index) {
                        if (left <= column) {
                            setSelectionRange(rowOffset + left, index + 3 - left);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected String checkPrintableChar(String text) {
        if (text.length() > 0 && Character.isJavaIdentifierPart(text.charAt(0)))
            callback.popCodeCompletion();
        return text;
    }

    @Override
    protected char checkPrintableChar(char c) {
        if (Character.isJavaIdentifierPart(c)) callback.popCodeCompletion();
        return c;
    }

    @Override
    protected String onPaste(String text) {
        return text;
    }

    @Override
    protected void onCaretPositionChanged() {
        if (mPopup.isShowing()) yes(this);
    }

    /*@Override
    public void setBackgroundColor(int color) {
      SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
      super.setBackgroundColor(preferences.getInt("userColor",color));
    }*/
    public void cursorInfo() {
        String mText;
        if (isSelectText()) {
            onKeyShortcut(KeyEvent.KEYCODE_O, null);
            return;
        } else if (!TextUtils.isEmpty(mText = getMarkText(_caretPosition))) {
            int ix = mText.lastIndexOf(' ');
            if (ix != -1) mText = mText.substring(ix + 1);
            if (mText.equals("...")) mText = "";
            if (mText.startsWith("*")) {
                mText = mText.replace("**", "*").replace('*', '&');
            }
            paste(mText);
            return;
        }
        if (FixableListDialog.show(activity)) return;
        int _offset = _caretPosition;
        while (_offset > 0 && Character.isJavaIdentifierPart(_hDoc.charAt(_offset - 1))) {
            --_offset;
        }
        if (Character.isJavaIdentifierStart(_hDoc.charAt(_offset))) {
            int line = _hDoc.findLineNumber(_offset);
            int offset = _hDoc.getLineOffset(line);
            int column = _offset - offset;
            if (column > 0) column = new String(_hDoc.subSequence(offset, column)).getBytes().length;
            ClangAPI.updatePosition(line + 1, column + 1);
            String cursor = ClangAPI.updatePosition(API._CursorInfo, 0);
            if (!TextUtils.isEmpty(cursor)) {
                final String[] split = cursor.split("\n\n");
                int length = split.length;
                Snackbar.make(this, "出现 (" + length + ") 处, 展开列表查看", Snackbar.LENGTH_SHORT).setAction("展开", new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (split.length > 0) {
                            FunctionListDialog.show(activity, split);
                        }
                    }
                }).show();
            }
        } else if (_hDoc.charAt(_offset) == '0' && Character.toLowerCase(_hDoc.charAt(_offset + 1)) == 'x') {
            int tmp = _caretPosition;
            while (Character.isJavaIdentifierPart(_hDoc.charAt(tmp))) {
                ++tmp;
            }
            String color = new String(_hDoc.subSequence(_offset, tmp - _offset));
            try {
                long _color = Long.parseLong(color.substring(2), 16);
                setSelectionRange(_offset, tmp - _offset);
                ColorDialog.show(activity, (int) _color);
            } catch (NumberFormatException ignored) {
                Log.e(TextWarriorApplication.TAG, "NumberFormatException", ignored);
            }
        }
        //activity.onKeyShortcut(KeyEvent.KEYCODE_H, null);
    }

    public ArrayList<Clang.Diagnostic> getErrLines() {
        return errLines;
    }

    public void setErrLines(@NonNull ArrayList<Clang.Diagnostic> errLines) {
        oldWordWrap = false;
        Rect from = getBoundingBox(_caretPosition);
        this.errLines = errLines;
        Rect to = getBoundingBox(_caretPosition);
        scrollBy(0, to.centerY() - from.centerY());
        if (getScrollY() < 0) makeRowVisible(0, -getScrollY());
        postInvalidate();
        //for(int i:errLines)
    }

    public boolean isFileCanSave() {
        return current.isExists();
    }

    protected boolean isInMark(int offset) {
        int rowNumber = _hDoc.findRowNumber(offset);
        String row = _hDoc.getRow(rowNumber);
        int rowOffset = _hDoc.getRowOffset(rowNumber);
        int column = offset - rowOffset;
        int index = row.indexOf("-*/", Math.max(4, column - 3));
        if (index != -1) {
            int left = row.lastIndexOf("/*-", index - 2);
            if (left != -1) {
                if (row.indexOf("-*/", left + 4) == index) {
                    if (left <= column) {
                        //setSelectionRange(rowOffset + left, index + 3 - left);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.edit2, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.undo:
                onKeyShortcut(KeyEvent.KEYCODE_Z, null);
                break;
            case R.id.redo:
                onKeyShortcut(KeyEvent.KEYCODE_Y, null);
                break;
            case R.id.font_larger:
                onKeyShortcut(KeyEvent.KEYCODE_EQUALS, null);
                break;
            case R.id.font_smaller:
                onKeyShortcut(KeyEvent.KEYCODE_MINUS, null);
                break;
            case R.id.rename:
                activity.onKeyShortcut(KeyEvent.KEYCODE_U, null);
                break;
            case R.id.repair:
                activity.onKeyShortcut(KeyEvent.KEYCODE_H, null);
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        editAction = null;
    }

    @Override
    public void onDismiss() {
        setOnInputCallback(false);
        if (activity instanceof MainActivity)
            ((MainActivity) activity).clang.onCCDismiss();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getItemAtPosition(position) == null) return;
        String string;
        position = (int) parent.getItemIdAtPosition(position);
        if (codeHelp instanceof IncludeHelp) {
            string = codeHelp.getText(position);
        } else {
            View t = view.findViewById(android.R.id.text1);
            if (t instanceof TextView) {
                string = ((TextView) t).getText().toString();
                if (string.equals("<提示>")) return;
                if (hasParams) {
                    t = view.findViewById(android.R.id.text2);
                    if (t instanceof TextView) {
                        string = ((TextView) t).getText().toString().replace("'[void]'", "").replace("'<void>'", "");
                        String row = _hDoc.getRow(_caretRow);
                        String sub = row.substring(0, startOffset - _hDoc.getRowOffset(_caretRow)).trim();
                        string = removeBlock(string, "'(", ")'");
                        if (!TextUtils.isEmpty(sub) || !string.endsWith(")")) {
                            string = removeBlock(string, "'[", "]'");
                        } else
                            string = string.replace("'[", "/*-").replace("]'", " v=-*/");
                        string = string.replace("'<", "/*-").replace(">'", "-*/");
                        sub = row.substring(_caretPosition - _hDoc.getRowOffset(_caretRow)).trim();
                        if (TextUtils.isEmpty(sub)) {
                            if (string.endsWith(")")) {
                                string += ";";
                            }
                        } else if (sub.charAt(0) == '(') {
                            int index = string.indexOf('(');
                            if (index > 0) string = string.substring(0, index);
                        }
                        Log.e(TextWarriorApplication.TAG, "string=" + string);
                    }
                } else {
                    t = view.findViewById(android.R.id.text1);
                    if (t instanceof TextView)
                        string = ((TextView) t).getText().toString();
                }
            } else return;
        }
        {
            EditField editField = this;
            int caretPosition = editField.getCaretPosition();
            if (caretPosition >= startOffset) {
                editField.setSelectionRange(startOffset, caretPosition - startOffset);
                editField.paste(string);
            }
            if (!string.endsWith("/")) mPopup.dismiss();
        }
    }

    /**
     * 有文字的菜单项目请自行理解，只说明隐藏的操作项
     * 文件面板和导航面板的显示：从屏幕左边或右边拖出
     * 长按编辑区：进入文本选择状态及顶部显示编辑动作
     * 长按右下角的圆形浮动按钮：在顶部显示编辑动作条
     * 单击圆形浮动按钮：列出标识符跳转列表或修正建议
     * 长按查找界面左边的搜索按钮：可以关闭搜索对话框
     * 长按文件浏览页中的文件：显示相关文件操作菜单
     * 点击当前文件标签：显示相关文件操作菜单
     * 长按当前文件标签：关闭当前文件
     * -------快捷键------
     * ctrl - A   全选
     * ctrl - S   手动保存
     * ctrl - O   交换光标
     * ctrl - W   选词
     * ctrl - K   加选
     * ctrl - N   新建
     * ctrl - X   剪切
     * ctrl - C   复制
     * ctrl - V   粘贴
     * ctrl - I   跳转标识符
     * ctrl - Z   撤销
     * ctrl - Y   重做
     * ctrl - -   字体-
     * ctrl - =   字体+
     * ctrl - U   重命名
     * ctrl - H   修正当前行
     * ctrl - Q   打开左面板
     * ctrl - P   打开右面板
     * ctrl - E   编辑
     * ctrl - D   补全
     * ctrl - F   格式化
     * ctrl - R   运行
     * ctrl - B   构建
     * ctrl - J   文本查找
     * ctrl - G   转到行
     * ctrl - M   终端模拟器
     * ctrl - T   设置
     */
    @Override
    public boolean onKeyShortcut(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_A:
                selectAll();
                bUserTaped = true;
                break;
            case KeyEvent.KEYCODE_O:
                if (getCaretPosition() == getSelectionStart()) {
                    focusSelectionEnd();
                } else focusSelectionStart();
                break;
            case KeyEvent.KEYCODE_W:
                selectWord(getCaretPosition());
                break;
            case KeyEvent.KEYCODE_K:
                selectMore(false);
                break;
            case KeyEvent.KEYCODE_Z:
                if (_hDoc.canUndo()) {
                    int newPosition = _hDoc.undo();
                    if (newPosition >= 0) {
                        selectText(false);
                        respan();
                        moveCaret(newPosition);
                        bUserTaped = true;
                        setEdited(true);
                    }
                } else {
                    Snackbar.make(this, "没有了", Snackbar.LENGTH_SHORT).show();
                }
                break;
            case KeyEvent.KEYCODE_Y:
                if (_hDoc.canRedo()) {
                    int newPosition = _hDoc.redo();
                    if (newPosition >= 0) {
                        selectText(false);
                        respan();
                        moveCaret(newPosition);
                        bUserTaped = true;
                        setEdited(true);
                    }
                } else {
                    Snackbar.make(this, "没有了", Snackbar.LENGTH_SHORT).show();
                }
                break;
            case KeyEvent.KEYCODE_X:
                cut((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE));
                break;
            case KeyEvent.KEYCODE_C:
                copy((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE));
                break;
            case KeyEvent.KEYCODE_V:
                ClipData clipData = ((ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE)).getPrimaryClip();
                if (clipData != null && clipData.getItemCount() > 0) {
                    ClipData.Item item = clipData.getItemAt(0);
                    CharSequence text = item.getText();
                    if (text != null && text.length() > 0) {
                        paste(text.toString());
                        bUserTaped = true;
                    }
                }
                break;
            case KeyEvent.KEYCODE_I:
                cursorInfo();
                break;
            case KeyEvent.KEYCODE_MINUS:
                float _base = _paintNormal.getTextSize() / _density;
                setZoom(Math.max(_base - 2, 6));
                break;
            case KeyEvent.KEYCODE_EQUALS:
                _base = _paintNormal.getTextSize() / _density;
                setZoom(Math.min(_base + 2, 36));
                break;
            default:
                if (event != null) {
                    return super.onKeyShortcut(keyCode, event);
                }
        }
        return true;
    }

    @Override
    protected void onFinishInflate() {
        if (isInEditMode()) {
            setColorScheme("5");
        }
        super.onFinishInflate();
    }

    @Override
    public void onLayout() {
        if (mListView != null) mListView.requestLayout();
    }

    @Override
    public void post(String[] data, int count, int startOffset, String footerText, String filterString, CodeHelp codeHelp) {
        adapter.post(data, count, footerText, filterString);
        adapter.setSelection();
        this.codeHelp = codeHelp;
        this.startOffset = startOffset;
    }

    @Override
    public void show() {
        int measuredWidth = getMeasuredWidth();
        int width = 15 * (measuredWidth >> 4);
        int left = (measuredWidth - width) >> 1;
        int[] locatin = new int[2];
        getLocationOnScreen(locatin);
        int t = (getMeasuredHeight() - getToolHeight()) >> 1;
        if (mPopup.isShowing()) {
            int w = mPopup.getWidth();
            int h = mPopup.getHeight();
            //mPopup.update(this,left,-(getMeasuredHeight() >> 1),w,h);
            mPopup.update(left, t + locatin[1], w, h);
        } else {
            mPopup.setWidth(width);
            mPopup.setHeight(t - left);
            //mPopup.showAsDropDown(this, left, -(getMeasuredHeight() >> 1));
            mPopup.showAtLocation(this, 0, left, t + locatin[1]);
        }
        setOnInputCallback(true);
    }

    @Override
    public void yes(FreeScrollingTextField editField) {
        int[] range = new int[2];
        String filterString;
        if (codeHelp instanceof IncludeHelp) {
            filterString = CodeCompilationTask.takeIncludeString(editField, range);
            if (filterString == null) mPopup.dismiss();
        } else
            filterString = CodeCompilationTask.takeWordRange(editField, range);
        if (startOffset == range[0]) {
            if (adapter != null) adapter.set(filterString, codeHelp);
        } else {
            mPopup.dismiss();
        }
    }

    public void selectMore(boolean notProgramLanguage) {
        if (notProgramLanguage || (!(Lexer.getLanguage() instanceof LanguageC || Lexer.getLanguage() instanceof LanguageC))) {
            if (isSelectText()) {
                if (getSelectionStart() == 0 && getSelectionEnd() == _hDoc.docLength() - 1) return;
                int line = _hDoc.findLineNumber(getSelectionStart());
                int from = _hDoc.getLineOffset(line);
                line = _hDoc.findLineNumber(getSelectionEnd());
                line = _hDoc.getLineOffset(line + 1) - 1;
                if (line < from) line = _hDoc.docLength() - 1;
                if (from == getSelectionStart() && line == getSelectionEnd()) {
                    selectAll();
                } else {
                    focusSelectionStart();
                    moveCaret(from);
                    focusSelectionEnd();
                    moveCaret(line);
                }
            } else selectWord(_caretPosition);
            return;
        }
        int line;
        String cursor = null;
        boolean moreSelect = false;
        if (!isSelectText() || bUserTaped) {
            bUserTaped = false;
            line = _hDoc.findLineNumber(_caretPosition);
            int offset = _hDoc.getLineOffset(line);
            int column = _caretPosition - offset;
            if (column > 0) {
                column = new String(_hDoc.subSequence(offset, column)).getBytes().length;
                if (findLineEndOffset() > 0) {
                    column += backToNoneWhitespace();
                    moreSelect = true;
                }
            }
            ClangAPI.updatePosition(line + 1, column + 1);
            selectText(true);
            cursor = ClangAPI.updatePosition(API._cmdGotoCursor, 0);
            if (cursor.contains("{0:0-0:0}")) cursor = ClangAPI.updatePosition(API._cmdGotoParent, 0);
        } else {
            int seekCount = findLineEndOffset();
            if (seekCount > 0) {
                focusSelectionEnd();
                moveCaret(_caretPosition + seekCount);
            } else {
                cursor = ClangAPI.updatePosition(API._cmdGotoParent, 0);
            }
        }
        if (!TextUtils.isEmpty(cursor)) {
            String s = cursor.replace('{', '<').replace('}', '>').replace('-', ':').split(":<")[1].split(">:")[0];
            String[] num = s.split(":");
            if (num.length == 4) {
                int parseInt = Integer.parseInt(num[2]);
                line = _hDoc.getLineOffset(parseInt - 1);
                int parseColumn = Integer.parseInt(num[3]) - 1;
                byte[] bt = null;
                int l2;
                if (parseColumn > 0) {
                    l2 = _hDoc.getLineOffset(parseInt) - 1;
                    if (l2 == -2) l2 = _hDoc.docLength() - 1;
                    if (l2 > line) {
                        bt = new String(_hDoc.subSequence(line, l2 - line)).getBytes();
                        if (parseColumn > bt.length) parseColumn = bt.length;
                        line += new String(bt, 0, parseColumn).length();
                        if (line >= _hDoc.docLength()) line = _hDoc.docLength() - 1;
                    }
                }
                if (moreSelect) {
                    int seekCount = findLineEndOffset();
                    if (seekCount > 0) {
                        line += seekCount;
                    }
                }
                int endOffset = line;
                int parseInt2 = Integer.parseInt(num[0]);
                line = _hDoc.getLineOffset(parseInt2 - 1);
                parseColumn = Integer.parseInt(num[1]) - 1;
                if (parseColumn > 0) {
                    l2 = _hDoc.getLineOffset(parseInt2) - 1;
                    if (l2 == -2) l2 = _hDoc.docLength() - 1;
                    if (l2 > line) {
                        if (parseInt2 != parseInt)
                            bt = new String(_hDoc.subSequence(line, l2 - line)).getBytes();
                        if (null != bt) line = line + new String(bt, 0, parseColumn).length();
                    }
                }
                setSelectionRange(line, endOffset - line);
                focusSelectionStart();
            }
        }
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public void setCallback(EditCallback callback) {
        this.callback = callback;
    }

    public void setColorScheme(String value) {
        ColorScheme scheme = null;
        switch (Integer.valueOf(value)) {
            case 0:
                scheme = new ColorSchemeLight();
                break;
            case 1:
                scheme = new ColorSchemeDark();
                break;
            case 2:
                scheme = new ColorSchemeSolarizedLight();
                break;
            case 3:
                scheme = new ColorSchemeSolarizedDark();
                break;
            case 4:
                scheme = new ColorSchemeObsidian();
                break;
            case 5:
                scheme = new SystemColorScheme(getContext());
                break;
        }
        if (scheme != null) {
            setColorScheme(scheme);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
            int color = scheme.getColor(ColorScheme.Colorable.BACKGROUND);
            setBackgroundColor(preferences.getInt("userColor", color));
        }
    }

    public boolean setCurrent(FileManager files, String filename) {
        if (current.is(filename))
            return false;
        FileState _new = files.loadFile(filename);
        if (_new != null) {
            current.save(true);
            current.saveState(this);
            current = _new;
            if (errLines.size() > 0) errLines = new ArrayList<>();
            current.update(this);
            if (filename.equals("New")) {
                _isEdited = true;
            }
            return true;
        } else return false;
    }

    public void setDiagonalDisplay(int diagonalDisplay) {
        this.diagonalDisplay = diagonalDisplay;
        postInvalidate();
    }

    public void setFileName(String file) {
        current = new FileState(_hDoc, file);
    }

    public void setFont(Integer integer) {
        switch (integer) {
            case 0:
                setTypeface(null);
                break;
            case 1:
                setTypeface(Typeface.SERIF);
                break;
            case 2:
                setTypeface(Typeface.SANS_SERIF);
                break;
            case 3:
                setTypeface(Typeface.MONOSPACE);
                break;
            case 4:
                setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Courier.ttf"));
                break;
            case 5:
                setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Menlo-Regular.ttf"));
                break;
        }
    }

    public void setOnInputCallback(boolean adapter) {
        //Log.e(TextWarriorApplication.LOG_TAG,"adapter:"+adapter);
        if (adapter) {
            bound = getBoundingBox(_caretPosition);
            bound.left = bound.bottom - getScrollY();
            int height = bound.left - getMeasuredHeight() / 4;
            if (height + getScrollY() < 0) height = -getScrollY();
            makeRowVisible(0, height);
        } else {
            if (bound != null) {
                //Rect bound2 = getBoundingBox(_caretPosition);
                //makeRowVisible(0, bound2.bottom - getScrollY() - bound.left);
                bound = null;
            }
        }
    }

    public void toggleActionMode() {
        if (editAction == null)
            editAction = startActionMode(this);
        else {
            editAction.finish();
        }
    }

    public ArrayList<String> v(String row) {
        int _length = row.length();
        int index, offset = 0;
        ArrayList<String> rows = new ArrayList<>();
        while (offset < _length) {
            index = row.indexOf("-*/", offset);
            if (index > 3) {
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
        return rows;
    }
}