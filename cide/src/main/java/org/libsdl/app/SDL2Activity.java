package org.libsdl.app;

import android.view.KeyEvent;

public class SDL2Activity extends SDLActivity {
    @Override
    public void onBackPressed() {
        onNativeKeyDown(KeyEvent.KEYCODE_BACK);
        onNativeKeyUp(KeyEvent.KEYCODE_BACK);
    }
}
