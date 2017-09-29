package org.free.cide.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TextInputLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.myopicmobile.textwarrior.common.DocumentProvider;

import org.free.cide.R;
import org.free.cide.views.EditField;

public class GotoDialog extends DialogFragment implements DialogInterface.OnClickListener, DialogInterface.OnShowListener, TextWatcher, TextView.OnEditorActionListener {
    private int mLine;

    public static void show(Activity activity) {
        new GotoDialog().show(activity.getFragmentManager(), "goto");
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        String trim = s.toString().trim();
        TextInputLayout inputLayout = (TextInputLayout) getDialog().findViewById(R.id.inputLayout);
        if (trim.isEmpty()) {
            inputLayout.setError(getActivity().getString(R.string.errNumber));
        } else {
            try {
                mLine = Integer.parseInt(trim);
                inputLayout.setError(null);
            } catch (NumberFormatException ignored) {
                inputLayout.setError(getActivity().getString(R.string.errNumber));
            }
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.gotoLine).setPositiveButton(R.string.go_to, this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(R.layout.goto_line);
        } else {
            builder.setView(LayoutInflater.from(getActivity()).inflate(R.layout.goto_line, null));
        }
        AlertDialog dialog = builder.create();
        dialog.getWindow().setGravity(Gravity.TOP);
        dialog.setOnShowListener(this);
        return dialog;
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        onClick(null, 0);
        dismiss();
        return true;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        int line = mLine - 1;
        final EditField text = (EditField) getActivity().findViewById(R.id.textField);
        DocumentProvider documentProvider = text.createDocumentProvider();
        int lineNumber = documentProvider.findLineNumber(documentProvider.docLength() - 1);
        if (line > lineNumber) line = lineNumber;
        else if (line < 0) line = 0;
        line = documentProvider.getLineOffset(line);
        text.selectText(false);
        text.moveCaret(line);
        InputMethodManager ims = (InputMethodManager) text.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        EditText textLine = (EditText) getDialog().findViewById(R.id.line);
        boolean input = ims.showSoftInput(textLine, InputMethodManager.SHOW_IMPLICIT);
        if (input && !text.isFocused())
            ims.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Override
    public void onShow(DialogInterface dialog) {
        EditText text = (EditText) getDialog().findViewById(R.id.line);
        text.setOnEditorActionListener(this);
        EditField textField = (EditField) getActivity().findViewById(R.id.textField);
        mLine = textField.createDocumentProvider().findLineNumber(textField.getCaretPosition()) + 1;
        TextInputLayout inputLayout = (TextInputLayout) getDialog().findViewById(R.id.inputLayout);
        text.setFocusable(false);
        //text.clearFocus();
        text.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getDialog() == null) return;
                EditText text = (EditText) getDialog().findViewById(R.id.line);
                String valueOf = String.valueOf(mLine);
                text.setFocusableInTouchMode(true);
                text.setText(valueOf);
                text.requestFocus();
                text.addTextChangedListener(GotoDialog.this);
                text.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (getDialog() == null) return;
                        EditText text = (EditText) getDialog().findViewById(R.id.line);
                        InputMethodManager ims = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                        ims.showSoftInput(text, InputMethodManager.SHOW_FORCED);
                        text.selectAll();
                    }
                }, 500);
            }
        }, 300);
    }
}
