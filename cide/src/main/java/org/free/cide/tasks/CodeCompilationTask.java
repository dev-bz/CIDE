package org.free.cide.tasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.myopicmobile.textwarrior.TextWarriorApplication;
import com.myopicmobile.textwarrior.androidm.FreeScrollingTextField;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Lexer;
import com.myopicmobile.textwarrior.common.Pair;

import org.free.cide.R;
import org.free.cide.callbacks.CodeCompilationDialogCallback;
import org.free.cide.ide.ClangHelp;
import org.free.cide.ide.CodeHelp;
import org.free.cide.ide.IncludeHelp;
import org.free.cide.views.EditField;

import java.util.List;
import java.util.Set;

/**
 * Created by Administrator on 2016/6/21.
 */
public class CodeCompilationTask extends AsyncTask<String, Object, Integer> {
    private final CodeCompilationDialogCallback callback;
    private final CodeHelp clangAPI;
    private final int line;
    private final int column;
    private final int start;
    private String[] data;
    private EditField edit;
    private String filterString;
    private String text;

    public CodeCompilationTask(int line, int column, int start, String filterString, CodeHelp clangAPI, CodeCompilationDialogCallback callback) {
        this.line = line;
        this.column = column;
        this.callback = callback;
        this.clangAPI = clangAPI;
        this.filterString = filterString;
        this.start = start;
        Log.e(TextWarriorApplication.TAG, String.format("CodeCompilationTask:%d,%d,(%s),(%s),%d", line, column, clangAPI.getClass().toString(), filterString, start));
    }

    public static CodeCompilationTask show(Activity activity, CodeCompilationDialogCallback callback) {
        View view = activity.findViewById(R.id.textField);
        EditField editField = (EditField) view;
        if (editField != null) {
            int[] range = new int[2];
            String filterString = takeIncludeString(editField, range);
            CodeHelp clangAPI;
            if (filterString != null) {
                clangAPI = new IncludeHelp(activity);
            } else {
                filterString = takeWordRange(editField, range);
                if (filterString.length() > 0 && !Character.isJavaIdentifierStart(filterString.charAt(0))) return null;
                Set<String> load = PreferenceManager.getDefaultSharedPreferences(activity).getStringSet("sortString", null);
                clangAPI = new ClangHelp(load);
            }
            DocumentProvider _hDoc = editField.createDocumentProvider();
            int line = _hDoc.findLineNumber(range[0]);
            int offset = _hDoc.getLineOffset(line);
            int column = range[0] - offset;
            if (column > 0) {
                column = new String(_hDoc.subSequence(offset, column)).getBytes().length;
            }
            CodeCompilationTask task = new CodeCompilationTask(line + 1, column + 1, range[0], filterString, clangAPI, callback);
            task.edit = editField;
            task.execute();
            return task;
        }
        return null;
    }

    public static String takeIncludeString(FreeScrollingTextField editField, int[] range) {
        int st = editField.getCaretPosition();
        int ed = st;
        DocumentProvider doc = editField.createDocumentProvider();
        List<Pair> spans = doc.getSpans();
        int spanIndex = 0;
        Pair nextSpan = spans.get(spanIndex++);
        Pair currSpan = null, preSpan = null;
        do {
            char at;
            if (currSpan != null) {
                at = doc.charAt(currSpan.getFirst());
                if (at == '#' || (Character.isJavaIdentifierStart(at))) {
                    preSpan = currSpan;
                }
            }
            currSpan = nextSpan;
            if (spanIndex < spans.size()) {
                nextSpan = spans.get(spanIndex++);
            } else {
                nextSpan = null;
            }
        } while (nextSpan != null && nextSpan.getFirst() < st);
        if (currSpan.getSecond() == Lexer.SINGLE_SYMBOL_DELIMITED_A) {
            if (preSpan != null && preSpan.getSecond() == Lexer.SINGLE_SYMBOL_LINE_A) {
                char[] str = doc.subSequence(preSpan.getFirst(), currSpan.getFirst() - preSpan.getFirst());
                int index = 0;
                for (char i : str) {
                    if (Character.isJavaIdentifierPart(i)) str[index++] = i;
                }
                if (new String(str).startsWith("include")) {
                    st = currSpan.getFirst() + 1;
                    for (int i = st; i < ed; ++i) if (doc.charAt(i) == '>' || doc.charAt(i) == '"') return null;
                    range[0] = st;
                    range[1] = ed;
                    return new String(doc.subSequence(st, ed - st));
                }
            }
        }
        return null;
    }

    public static String takeWordRange(FreeScrollingTextField editField, int[] range) {
        int st = editField.getCaretPosition();
        int ed = st;
        DocumentProvider doc = editField.createDocumentProvider();
        while (st > 0) {
            char c = doc.charAt(st - 1);
            if (Character.isJavaIdentifierPart(c))
                --st;
            else
                break;
        }
    /*int length = doc.docLength() - 1;
    while (ed < length) {
      char c = doc.charAt(ed);
      if (Character.isJavaIdentifierPart(c))
        ++ed;
      else
        break;
    }*/
        range[0] = st;
        range[1] = ed;
        return new String(doc.subSequence(st, ed - st));
    }

    @Override
    protected Integer doInBackground(String... params) {
        clangAPI.updatePosition(line, column);
        int count = clangAPI.set(filterString, true);

        text = "";
        while (count > 0) {
            String s = clangAPI.get(count - 1);
            if (s.contains("<提示>")) {
                text += "\n" + s;
                --count;
            } else {
                break;
            }
        }
        int myCount = 0;
        if (params != null) {
            myCount += params.length;
        }
        count += myCount;
        if (count > 100) count = 100;
        this.data = new String[count];
        myCount = 0;
        if (params != null) {
            for (String b : params) {
                data[myCount] = b;
                ++myCount;
            }
        }
        for (int i = myCount; i < count; ++i) data[i] = clangAPI.get(i - myCount);
        return count;
    }

    @Override
    protected void onPostExecute(Integer result) {
        int[] _range = new int[2];
        if (clangAPI instanceof IncludeHelp) {
            takeIncludeString(edit, _range);
        } else {
            takeWordRange(edit, _range);
        }
        Log.e(TextWarriorApplication.TAG, String.format("start=%d,range=%d", start, _range[0]));
        if (_range[0] != start) return;
        callback.codeCompilationNotify(result, data, start, text.trim(), filterString, clangAPI);
    }
}
