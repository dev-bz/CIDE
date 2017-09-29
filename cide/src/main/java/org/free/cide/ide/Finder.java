package org.free.cide.ide;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;
import com.myopicmobile.textwarrior.common.FindThread;
import com.myopicmobile.textwarrior.common.ProgressObserver;
import com.myopicmobile.textwarrior.common.ProgressSource;

import org.free.cide.R;

class Finder implements ProgressObserver, TextWatcher, View.OnClickListener, CompoundButton.OnCheckedChangeListener, View.OnLongClickListener {
    private final Activity activity;
    private FindThread _taskFind;
    private Drawable backup;
    private boolean isCaseSensitive;
    private boolean isWholeWord;
    private FreeScrollingTextField textField;
    private String what;

    public Finder(Activity target) {
        this.activity = target;
        View v = activity.findViewById(R.id.find_text);
        if (v instanceof TextView) {
            ((TextView) v).addTextChangedListener(this);
        }
        v = activity.findViewById(R.id.find_next);
        v.setOnClickListener(this);
        v = activity.findViewById(R.id.find_precious);
        v.setOnClickListener(this);
        v = activity.findViewById(R.id.find_replace);
        v.setOnClickListener(this);
        v = activity.findViewById(R.id.find_replaceAll);
        v.setOnClickListener(this);
        CheckBox c = (CheckBox) activity.findViewById(R.id.isCaseSensitive);
        c.setOnCheckedChangeListener(this);
        c = (CheckBox) activity.findViewById(R.id.isWholeWord);
        c.setOnCheckedChangeListener(this);
        v = activity.findViewById(R.id.find_toggle);
        v.setOnLongClickListener(this);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        findFirstText(s);
    }

    private void findFirstText(CharSequence s) {
        if (TextUtils.isEmpty(s)) return;
        isCaseSensitive = ((CheckBox) activity.findViewById(R.id.isCaseSensitive)).isChecked();
        isWholeWord = ((CheckBox) activity.findViewById(R.id.isWholeWord)).isChecked();
        find(0, what = s.toString().replace("\\n", "\n").replace("\\t", "\t"), isCaseSensitive, isWholeWord);
    }

    private void find(int start, String what, boolean isCaseSensitive, boolean isWholeWord) {
        if (TextUtils.isEmpty(what)) return;
        textField = (FreeScrollingTextField) activity.findViewById(R.id.textField);
        int startingPosition;
        if (start < 0)
            startingPosition = textField.isSelectText() ? textField.getSelectionStart() + 1 : textField.getCaretPosition() + 1;
        else startingPosition = start;
        _taskFind = FindThread.createFindThread(textField.createDocumentProvider(), what, startingPosition, true, isCaseSensitive, isWholeWord);
        _taskFind.registerObserver(this);
        _taskFind.start();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        findFirstText(what);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.find_next:
                find(-1, what, isCaseSensitive, isWholeWord);
                break;
            case R.id.find_precious:
                findBackwards(what, isCaseSensitive, isWholeWord);
                break;
            case R.id.find_replace: {
                View t = activity.findViewById(R.id.replace_text);
                if (t instanceof TextView) {
                    replaceSelection(((TextView) t).getText().toString().replace("\\n", "\n").replace("\\t", "\t"));
                }
            }
            break;
            case R.id.find_replaceAll:
                if (!TextUtils.isEmpty(what)) {
                    View t = activity.findViewById(R.id.replace_text);
                    if (t instanceof TextView) {
                        String replace = ((TextView) t).getText().toString().replace("\\n", "\n").replace("\\t", "\t");
                        replaceAll(what, replace, isCaseSensitive, isWholeWord);
                    }
                }
                break;
        }
    }

    private void findBackwards(String what, boolean isCaseSensitive, boolean isWholeWord) {
        if (TextUtils.isEmpty(what)) return;
        textField = (FreeScrollingTextField) activity.findViewById(R.id.textField);
        int startingPosition = textField.isSelectText() ? textField.getSelectionStart() - 1 : textField.getCaretPosition() - 1;
        _taskFind = FindThread.createFindThread(textField.createDocumentProvider(), what, startingPosition, false, isCaseSensitive, isWholeWord);
        _taskFind.registerObserver(this);
        _taskFind.start();
    }

    private void replaceSelection(String replacementText) {
        textField = (FreeScrollingTextField) activity.findViewById(R.id.textField);
        if (textField.isSelectText())
            textField.paste(replacementText);
    }

    private void replaceAll(String what, String replacementText, boolean isCaseSensitive, boolean isWholeWord) {
        if (TextUtils.isEmpty(what)) return;
        textField = (FreeScrollingTextField) activity.findViewById(R.id.textField);
        int startingPosition = textField.getCaretPosition();
        _taskFind = FindThread.createReplaceAllThread(textField.createDocumentProvider(), what, replacementText, startingPosition, isCaseSensitive, isWholeWord);
        _taskFind.registerObserver(this);
        _taskFind.start();
        View //view = activity.findViewById(R.id.find_text);
                //view.setEnabled(false);
                view = activity.findViewById(R.id.replace_text);
        if (backup == null) backup = view.getBackground();
        Drawable background = new Drawable() {
            final Paint paint = new Paint();

            @Override
            public void draw(Canvas canvas) {
                backup.draw(canvas);
                if (_taskFind == null) return;
                int w = canvas.getWidth();
                int h = canvas.getHeight();
                paint.setColor(0x7f7fff7f);
                canvas.drawRect(0, 0, w * _taskFind.getCurrent() / 100, h, paint);
                invalidateSelf();
            }

            @Override
            public void setAlpha(int alpha) {
            }

            @Override
            public void setColorFilter(ColorFilter colorFilter) {
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(background);
        } else view.setBackgroundDrawable(background);
    }

    @Override
    public void onComplete(int requestCode, Object result) {
        if (requestCode == ProgressSource.FIND || requestCode == ProgressSource.FIND_BACKWARDS) {
            final int foundIndex = ((FindThread.FindResults) result).foundOffset;
            final int length = ((FindThread.FindResults) result).searchTextLength;
            if (foundIndex != -1) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textField.setSelectionRange(foundIndex, length);
                    }
                });
            }
            _taskFind = null;
        } else if (requestCode == ProgressSource.REPLACE_ALL) {
            int replacementCount = ((FindThread.FindResults) result).replacementCount;
            final int newStartPosition = ((FindThread.FindResults) result).newStartPosition;
            if (replacementCount > 0) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textField.setEdited(true);
                        textField.selectText(false);
                        textField.moveCaret(newStartPosition);
                        textField.respan();
                        textField.invalidate();
                    }
                });
            }
            _taskFind = null;
            backupView();
        }
    }

    @Override
    public void onError(int requestCode, int errorCode, String message) {
        if (requestCode == ProgressSource.REPLACE_ALL) {
            backupView();
        }
    }

    @Override
    public void onCancel(int requestCode) {
        if (requestCode == ProgressSource.REPLACE_ALL) {
            backupView();
        }
    }

    private void backupView() {
        //View view = activity.findViewById(R.id.find_text);
        //view.setEnabled(true);
        if (backup != null) activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = activity.findViewById(R.id.replace_text);
                view.setBackgroundDrawable(backup);
                backup = null;
            }
        });
    }

    @Override
    public boolean onLongClick(View v) {
        View view = activity.findViewById(R.id.find_panel);
        if (view != null) {
            boolean shown = view.isShown();
            view.setVisibility(shown ? View.GONE : View.VISIBLE);
            if (!shown) {
                view = activity.findViewById(R.id.find_text);
                view.requestFocus();
            }
            boolean landscape = activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            if (!landscape) {
                view = activity.findViewById(R.id.replace_panel);
                if (view != null && view.isShown()) {
                    view.setVisibility(View.GONE);
                }
            }
        }
        return true;
    }

    public void onShow() {
        View v = activity.findViewById(R.id.find_text);
        if (v instanceof TextView) {
            textField = (FreeScrollingTextField) activity.findViewById(R.id.textField);
            if (textField.isSelectText() && textField.getSelectionStart() != textField.getSelectionEnd()) {
                what = new String(textField.createDocumentProvider().subSequence(textField.getSelectionStart(), textField.getSelectionEnd() - textField.getSelectionStart()));
                ((TextView) v).setText(what.replace("\n", "\\n").replace("\t", "\\t"));
            }
        }
    }
}
