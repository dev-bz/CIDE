package org.free.cide.ide;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListView;
import android.widget.TextView;

import com.myopicmobile.textwarrior.androidm.SelectionModeListener;
import com.myopicmobile.textwarrior.common.DocumentProvider;

import org.free.API;
import org.free.cide.R;
import org.free.cide.adapters.FunctionListAdapter;
import org.free.cide.app.FileManager;
import org.free.cide.views.EditField;
import org.free.clangide.ClangAPI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class ViewFileActivity extends AppCompatActivity implements FunctionListAdapter.Callback, SelectionModeListener, ActionMode.Callback {
    private ActionMode actionMode;
    private EditField edit;

    @Override
    public String[] get(String str) {
        return str.split("↔");
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer != null) {
            if (drawer.isDrawerOpen(GravityCompat.START) || drawer.isDrawerOpen(GravityCompat.END)) {
                drawer.closeDrawers();
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        if ("1".equals(PreferenceManager.getDefaultSharedPreferences(this).getString("theme", "0"))) {
            setTheme(R.style.AppTheme_NoActionBar);
        }
        setContentView(R.layout.activity_view_file);
        String file = getIntent().getData().getPath();
        View view = findViewById(R.id.textField);
        if (view instanceof EditField) {
            edit = (EditField) view;
            edit.setActivity(this);
            try {
                DocumentProvider doc = FileManager.getDocument(file);
                //edit.setCallback(this);
                doc.setMetrics(edit);
                edit.setDocumentProvider(doc);
                edit.stopFlingScrolling();
                edit.setSelModeListener(this);
                edit.setFileName(file);
                //edit.scrollTo(0,0);
                String line = getIntent().getStringExtra("line");
                if (!TextUtils.isEmpty(line)) {
                    String column = getIntent().getStringExtra("column");
                    Main.gotoCursor(edit, line, column);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (ClangAPI.bHasFile(file, true)) {
            new AsyncTask<String, Object, ArrayList<Clang.Diagnostic>>() {
                @Override
                protected ArrayList<Clang.Diagnostic> doInBackground(String... params) {
                    String file = params[0];
                    String[] strs = ClangAPI.updatePosition(API._Diagnostic, 0).split("~~");
                    ArrayList<Clang.Diagnostic> errLines = new ArrayList<>();
                    for (String one : strs) {
                        boolean errFile = false;
                        if (one.startsWith(file)) {
                            errFile = true;
                            one = one.substring(file.length());
                        }
                        String[] str = one.split(": ", 2);
                        if (str.length < 2) {
                            continue;
                        }
                        if (errFile) {
                            String[] ll = str[0].split(":");
                            int line, column;
                            try {
                                line = Integer.parseInt(ll[1]);
                                //if (ll.length == 3) column = Integer.parseInt(ll[2]);
                            } catch (NumberFormatException e) {
                                continue;
                            }
                            one = str[1];
                            column = one.indexOf('↔');
                            int i;
                            for (i = 0; i < errLines.size(); ++i) {
                                if (errLines.get(i).line > line) break;
                            }
                            String fix = null;
                            if (one.endsWith("</r>") && column >= 0) fix = one.substring(column + 1);
                            errLines.add(i, new Clang.Diagnostic(line, column < 0 ? one : one.substring(0, column), fix));
                        }
                    }
                    return errLines;
                }

                @Override
                protected void onPostExecute(ArrayList<Clang.Diagnostic> errLines) {
                    if (errLines.size() > 0) {
                        EditField edit = (EditField) findViewById(R.id.textField);
                        if (edit != null) {
                            edit.setErrLines(errLines);
                        }
                    }
                    ListView list = (ListView) findViewById(R.id.functionList);
                    if (list != null) {
                        String[] function = ClangAPI.updatePosition(API._FunctionList, 0).split("\n\n");
                        FunctionListAdapter adapter = new FunctionListAdapter(function, ViewFileActivity.this, ViewFileActivity.this);
                        list.setAdapter(adapter);
                        list.setOnItemClickListener(adapter);
                    }
                }
            }.execute(file);
        }
        TextView textView = (TextView) findViewById(android.R.id.text2);
        if (textView != null) {
            file = file.replace(getFilesDir().getPath() + File.separator, "");
            textView.setSingleLine();
            textView.setEllipsize(TextUtils.TruncateAt.START);
            textView.setText(file);
            textView = (TextView) findViewById(android.R.id.text1);
            if (textView != null) {
                int ix = file.lastIndexOf(File.separatorChar);
                if (ix != -1) {
                    file = file.substring(ix + 1);
                }
                textView.setText(file);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        View view = findViewById(R.id.textField);
        if (view instanceof EditField) {
            outState.putParcelable("STATE_TEXT_UI", ((EditField) view).getUiState());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        getMenuInflater().inflate(R.menu.view, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_copy:
                edit.onKeyShortcut(KeyEvent.KEYCODE_C, null);
                break;
            case R.id.edit_selectAll:
                edit.selectAll();
                break;
            case R.id.edit_moveLeft:
                edit.moveCaretLeft();
                break;
            case R.id.edit_moveRight:
                edit.moveCaretRight();
                break;
            case R.id.edit_cursor:
                edit.onKeyShortcut(KeyEvent.KEYCODE_O, null);
                break;
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        actionMode = null;
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        View view = findViewById(R.id.textField);
        if (view instanceof EditField) {
            Parcelable textUi = savedInstanceState.getParcelable("STATE_TEXT_UI");
            if (textUi != null) ((EditField) view).restoreUiState(textUi);
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void finish() {
        String file = getIntent().getStringExtra("back");
        if (!TextUtils.isEmpty(file)) ClangAPI.bHasFile(file, true);
        super.finish();
    }

    @Override
    public void onSelectionModeChanged(boolean active) {
        if (active) actionMode = startActionMode(this);
        else {
            actionMode.finish();
        }
    }
}
