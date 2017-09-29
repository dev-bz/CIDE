package com.termux.terminal;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A client which receives callbacks from events triggered by feeding input to a {@link TerminalEmulator}.
 */
public abstract class TerminalOutput {
    /**
     * Notify the terminal client that the terminal title has changed.
     */
    public abstract void clipboardText(String text);

    /**
     * Notify the terminal client that a bell character (ASCII 7, bell, BEL, \a, ^G)) has been received.
     */
    public abstract void onBell();

    /**
     * Notify the terminal client that the terminal title has changed.
     */
    public abstract void titleChanged(String oldTitle, String newTitle);

    /**
     * Write a string using the UTF-8 encoding to the terminal client.
     */
    public final void write(String data) {
        byte[] bytes;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            bytes = data.getBytes(StandardCharsets.UTF_8);
        } else {
            bytes = data.getBytes(Charset.forName("UTF-8"));
        }
        write(bytes, 0, bytes.length);
    }

    /**
     * Write bytes to the terminal client.
     */
    public abstract void write(byte[] data, int offset, int count);
}
