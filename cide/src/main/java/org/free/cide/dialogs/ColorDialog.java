package org.free.cide.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import org.free.cide.R;
import org.free.cide.views.ColorPickerAlphaView;
import org.free.cide.views.ColorPickerBoxView;
import org.free.cide.views.ColorPickerView;

public class ColorDialog extends DialogFragment implements DialogInterface.OnShowListener, View.OnClickListener {
    public static final String SELECT_COLOR = "SELECT_COLOR";
    private static ColorDialog dialog;

    public static void show(Activity activity) {
        if (ColorDialog.dialog == null) ColorDialog.dialog = new ColorDialog();
        Bundle args = new Bundle(2);
        args.putInt("color", 0xAf7f7f7f);
        args.putString("action", SELECT_COLOR);
        dialog.setArguments(args);
        dialog.show(activity.getFragmentManager(), "color_dialog");
    }

    public static void show(Activity activity, int color) {
        if (ColorDialog.dialog == null) ColorDialog.dialog = new ColorDialog();
        Bundle args = new Bundle(2);
        args.putInt("color", color);
        args.putString("action", SELECT_COLOR);
        dialog.setArguments(args);
        dialog.show(activity.getFragmentManager(), "color_dialog");
    }

    public static void show(Activity activity, int color, String action) {
        if (ColorDialog.dialog == null) ColorDialog.dialog = new ColorDialog();
        Bundle args = new Bundle(2);
        args.putInt("color", color);
        args.putString("action", action);
        dialog.setArguments(args);
        dialog.show(activity.getFragmentManager(), "color_dialog");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_cancel:
                dismiss();
                break;
            case R.id.button_ok:
                Drawable d = getDialog().findViewById(R.id.color_right).getBackground();
                if (d instanceof ColorDrawable) {
                    int color = ((ColorDrawable) d).getColor();
                    Intent i = new Intent(getActivity(), getActivity().getClass());
                    i.setAction(getArguments().getString("action"));
                    String value = Integer.toHexString(color);
                    while (value.length() < 8) value = "0" + value;
                    i.putExtra("color", "0x" + value);
                    getActivity().startActivity(i);
                    dismiss();
                }
                break;
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle("选择颜色");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(R.layout.color_picker);
        } else {
            builder.setView(View.inflate(getActivity(), R.layout.color_picker, null));
        }
        //builder.setPositiveButton("OK",  this);
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(this);
        return alertDialog;
    }

    @Override
    public void onShow(DialogInterface dialog) {
        ColorPickerBoxView v1 = (ColorPickerBoxView) getDialog().findViewById(R.id.color_view1);
        ColorPickerView v2 = (ColorPickerView) getDialog().findViewById(R.id.color_view2);
        ColorPickerAlphaView v3 = (ColorPickerAlphaView) getDialog().findViewById(R.id.color_view3);
        getDialog().findViewById(R.id.button_cancel).setOnClickListener(this);
        getDialog().findViewById(R.id.button_ok).setOnClickListener(this);
        if (v3 != null) {
            v3.notifyTo(getDialog().findViewById(R.id.color_right));
        }
        if (v2 != null) {
            v2.notifyTo(v3);
        }
        if (v1 != null) {
            v1.notifyTo(v2);
        }
        if (v1 != null) {
            v1.startColor(getArguments().getInt("color"));
        }
    }
}
