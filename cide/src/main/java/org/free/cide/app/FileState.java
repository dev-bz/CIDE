package org.free.cide.app;

import android.support.annotation.NonNull;

import com.myopicmobile.textwarrior.common.Document;
import com.myopicmobile.textwarrior.common.DocumentProvider;
import com.myopicmobile.textwarrior.common.Lexer;

import org.free.cide.views.EditField;
import org.free.tools.DocumentState;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileState {
    private final String _fileName;
    long _timestamp = 0;
    private int _caretPosition = 0;
    private DocumentProvider _doc;
    private int _hash;
    private int _scrollX = 0;
    private int _scrollY = 0;

    public FileState(DocumentProvider documentProvider, @NonNull String fileName) {
        _doc = documentProvider;
        _fileName = fileName;
        if (_doc == null) {
            reload();
        } else {
            _hash = getHashCode();
        }
        _timestamp = new File(_fileName).lastModified();
    }

    public void reload() {
        if (isChanged()) {
            try {
                _doc = FileManager.getDocument(_fileName);
                _hash = getHashCode();
            } catch (IOException e) {
                _doc = new DocumentProvider(new Document(null));
                _doc.setWordWrap(false);
            }
        }
    }

    private int getHashCode() {
        return new String(_doc.subSequence(0, _doc.docLength() - 1)).hashCode();
    }

    public boolean isChanged() {
        return _timestamp != new File(_fileName).lastModified();
    }

    public String getFileName() {
        return _fileName;
    }

    public boolean is(@NonNull String filename) {
        return filename.equals(_fileName);
    }

    public boolean isExists() {
        return new File(_fileName).canWrite();
    }

    public boolean checkNeedSave() {
        if (!_fileName.startsWith("/")) return false;
        File file = new File(_fileName);
        String toSave = new String(_doc.subSequence(0, _doc.docLength() - 1));
        int _newHash = toSave.hashCode();
        return !(_newHash == _hash && !isChanged());
    }

    public void save(boolean byAuto) {
        if (!_fileName.startsWith("/")) return;
        File file = new File(_fileName);
        if (byAuto && !file.exists()) return;
        String toSave = new String(_doc.subSequence(0, _doc.docLength() - 1));
        int _newHash = toSave.hashCode();
        if (_newHash == _hash && !isChanged()) return;
        try {
            FileOutputStream fos = new FileOutputStream(_fileName);
            fos.write(toSave.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        _hash = _newHash;
        _timestamp = file.lastModified();
        FileManager.postSaved(file.getName());
    }

    public void saveState(@NonNull EditField edit) {
        _scrollX = edit.getScrollX();
        _scrollY = edit.getScrollY();
        _caretPosition = edit.getCaretPosition();
        _timestamp = new File(_fileName).lastModified();
    }

    public void update(@NonNull EditField edit) {
        DocumentProvider tmp = edit.createDocumentProvider();
        tmp.setMetrics(null);
        _doc.setMetrics(edit);
        _doc.setWordWrap(tmp.isWordWrap());
        Lexer.setLanguage(DocumentState.getLanguage(new File(_fileName), edit.getContext()));
        edit.setDocumentProvider(_doc);
        edit.scrollTo(_scrollX, _scrollY);
        edit.moveCaret(_caretPosition);
    }
}