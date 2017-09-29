/*
 * Copyright (c) 2013 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package com.myopicmobile.textwarrior.androidm;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.myopicmobile.textwarrior.EditCtrl;
import com.myopicmobile.textwarrior.common.ColorScheme;
//TODO minimise unnecessary invalidate calls

/**
 * TouchNavigationMethod classes implementing their own carets have to override
 * getCaretBloat() to return the size of the drawing area it needs, in excess of
 * the bounding box of the character the caret is on, and use
 * onTextDrawComplete(Canvas) to draw the caret. Currently, only a fixed size
 * caret is allowed, but scalable carets may be implemented in future.
 */
public class TouchNavigationMethod extends GestureDetector.SimpleOnGestureListener {
    private final static Rect _caretBloat = new Rect(0, 0, 0, 0);
    // When the caret is dragged to the edges of the text field, the field will
    // scroll automatically. SCROLL_EDGE_SLOP is the width of these edges in pixels
    // and extends inside the content area, not outside to the padding area
    protected static int SCROLL_EDGE_SLOP = 10;
    /**
     * The radius, in density-independent pixels, around a point of interest
     * where any touch event within that radius is considered to have touched
     * the point of interest itself
     */
    protected static int TOUCH_SLOP = 12;
    protected FreeScrollingTextField _textField;
    protected boolean _isCaretTouched = false;
    private GestureDetector _gestureDetector;
    private int _touchOffsetX;
    private int _touchOffsetY;

    public TouchNavigationMethod(FreeScrollingTextField textField) {
        _textField = textField;
        _gestureDetector = new GestureDetector(textField.getContext(), this);
        _gestureDetector.setIsLongpressEnabled(true);
        _gestureDetector.setOnDoubleTapListener(null);
    }

    @SuppressWarnings("unused")
    private TouchNavigationMethod() {
        // do not invoke; always needs a valid _textField
    }

    @Override
    public boolean onDown(MotionEvent e) {
        float x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        _isCaretTouched = isNearChar(x, y, _textField.getCaretPosition());
        if (_textField.isFlingScrolling()) {
            _textField.stopFlingScrolling();
        } else if (_textField.isSelectText()) {
            if (isNearChar(x, y, _textField.getSelectionStart())) {
                _textField.focusSelectionStart();
                _textField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                _isCaretTouched = true;
            } else if (isNearChar(x, y, _textField.getSelectionEnd())) {
                _textField.focusSelectionEnd();
                _textField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                _isCaretTouched = true;
            }
        }
        if (_isCaretTouched) {
            _textField.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
        return true;
    }

    /**
     * Note that up events from a fling are NOT captured here.
     * Subclasses have to call super.onUp(MotionEvent) in their implementations
     * of onFling().
     * <p/>
     * Also, up events from non-primary pointers in a multi-touch situation are
     * not captured here.
     *
     * @param e
     * @return
     */
    public boolean onUp(MotionEvent e) {
        _textField.stopAutoScrollCaret();
        _isCaretTouched = false;
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) {
        if (_isCaretTouched) {
            dragCaret(e2);
        } else {
            scrollView(distanceX, distanceY);
        }
        //TODO find out if ACTION_UP events are actually passed to onScroll
        if ((e2.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            onUp(e2);
        }
        return true;
    }

    private void dragCaret(MotionEvent e) {
        if (!_textField.isSelectText() && isDragSelect()) {
            _textField.selectText(true);
        }
        int x = (int) e.getX() - _textField.getPaddingLeft();
        int y = (int) e.getY() - _textField.getPaddingTop();
        boolean scrolled = false;
        // If the edges of the textField content area are touched, scroll in the
        // corresponding direction.
        if (x < SCROLL_EDGE_SLOP) {
            scrolled = _textField.autoScrollCaret(FreeScrollingTextField.SCROLL_LEFT);
        } else if (x >= (_textField.getContentWidth() - SCROLL_EDGE_SLOP)) {
            scrolled = _textField.autoScrollCaret(FreeScrollingTextField.SCROLL_RIGHT);
        } else if (y < SCROLL_EDGE_SLOP) {
            scrolled = _textField.autoScrollCaret(FreeScrollingTextField.SCROLL_UP);
        } else if (y >= (_textField.getContentHeight() - SCROLL_EDGE_SLOP)) {
            scrolled = _textField.autoScrollCaret(FreeScrollingTextField.SCROLL_DOWN);
        }
        if (!scrolled) {
            _textField.stopAutoScrollCaret();
            int newCaretIndex = _textField.coordToCharIndex(
                    screenToViewX((int) e.getX() + _touchOffsetX),
                    screenToViewY((int) e.getY() + _touchOffsetY)
            );
            if (newCaretIndex >= 0) {
                _textField.moveCaret(newCaretIndex);
                _textField.onUserTap();
            }
        }
    }

    private void scrollView(float distanceX, float distanceY) {
        int newX = (int) distanceX + _textField.getScrollX();
        int newY = (int) distanceY + _textField.getScrollY();
        // If scrollX and scrollY are somehow more than the recommended
        // max scroll values, use them as the new maximum
        // Also take into account the size of the caret,
        // which may extend beyond the text boundaries
        int maxWidth = Math.max(_textField.getMaxScrollX(),
                _textField.getScrollX());
        if (newX > maxWidth) {
            newX = maxWidth;
        } else if (newX < 0) {
            newX = Math.max(newX, _textField.getScrollX());
        }
        int maxHeight = Math.max(_textField.getMaxScrollY(),
                _textField.getScrollY());
        if (newY > maxHeight) {
            newY = maxHeight;
        } else if (newY < 0) {
            newY = Math.max(newY, _textField.getScrollY());
        }
        _textField.scrollTo(newX, newY);
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        float x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        int charOffset = _textField.coordToCharIndex(x, y);
        if (_textField.isSelectText()) {
            int strictCharOffset = _textField.coordToCharIndexStrict(x, y);
            if (_textField.inSelectionRange(strictCharOffset) ||
                    isNearChar(x, y, _textField.getSelectionStart()) ||
                    isNearChar(x, y, _textField.getSelectionEnd())) {
                // do nothing
                if (isNearChar(x, y, _textField.getSelectionStart())) {
                    _textField.focusSelectionStart();
                } else if (isNearChar(x, y, _textField.getSelectionEnd())) {
                    _textField.focusSelectionEnd();
                } else if (_textField.isFocused()) {
                    _textField.selectText(false);
                }
            } else _textField.selectText(false);
            if (!_textField.isFocused()) {
                _textField.requestFocus();//return true;
            }
        }
        {
            if (charOffset >= 0) {
                _textField.moveCaret(charOffset);
                _textField.onUserTap();
            }
            if (!_textField.isFocused()) {
                _textField.requestFocus();
            }
//      _textField.showIME(true);
        }
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        _isCaretTouched = true;
        float x = screenToViewX((int) e.getX());
        int y = screenToViewY((int) e.getY());
        int charOffset = _textField.coordToCharIndex(x, y);
        if (_textField.isSelectText()) {
            if (_textField.inSelectionRange(charOffset)) {
                /**_textField.moveCaret(charOffset);
                 _textField.selectText(false);
                 _textField.onUserTap();*/
                _textField.selectLine(charOffset);
            } else if (charOffset >= 0) {
                _textField.selectWord(charOffset);
            } else {
                _textField.selectText(false);
                _isCaretTouched = false;
            }
        } else {
            if (charOffset >= 0) {
                _textField.selectWord(charOffset);
            }
        }
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        /**_isCaretTouched = true;
         float x = screenToViewX((int) e.getX());
         int y = screenToViewY((int) e.getY());
         int charOffset = _textField.coordToCharIndex(x, y);
         if (_textField.isSelectText()) {
         if (_textField.inSelectionRange(charOffset)) {
         _textField.moveCaret(charOffset);
         _textField.selectText(false);
         } else if (charOffset >= 0) {
         _textField.setSelectionRange(charOffset, 0);
         } else {
         _textField.selectText(false);
         _isCaretTouched = false;
         }
         } else {
         if (charOffset >= 0) {
         _textField.moveCaret(charOffset);
         _textField.selectText(true);
         }
         }
         return true;*/
        return super.onDoubleTap(e);
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        /**if (_isCaretTouched && e.getAction() == MotionEvent.ACTION_MOVE) {
         dragCaret(e);
         return true;
         }*/
        return super.onDoubleTapEvent(e);
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,
                           float velocityY) {
        if (!_isCaretTouched) {
            _textField.flingScroll((int) -velocityX, (int) -velocityY);
        }
        onUp(e2);
        return true;
    }

    /**
     * Subclasses overriding this method have to call the superclass method
     */
    public boolean onTouchEvent(MotionEvent event) {
        boolean handled = _gestureDetector.onTouchEvent(event);
        if (!handled
                && (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            // propagate up events since GestureDetector does not do so
            handled = onUp(event);
        }
        return handled;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    /**
     * Android lifecyle event. See {@link android.app.Activity#onPause()}.
     */
    void onPause() {
        //do nothing
    }

    /**
     * Android lifecyle event. See {@link android.app.Activity#onResume()}.
     */
    void onResume() {
        //do nothing
    }

    /**
     * Called by FreeScrollingTextField when it has finished drawing text.
     * Classes extending TouchNavigationMethod can use this to draw, for
     * example, a custom caret.
     * <p/>
     * The canvas includes padding in it.
     *
     * @param canvas
     */
    public void onTextDrawComplete(Canvas canvas) {
        // Do nothing. Basic caret drawing is handled by FreeScrollingTextField.
    }

    public void onColorSchemeChanged(ColorScheme colorScheme) {
        // Do nothing. Derived classes can use this to change their graphic assets accordingly.
    }

    public void onChiralityChanged(boolean isRightHanded) {
        // Do nothing. Derived classes can use this to change their input
        // handling and graphic assets accordingly.
    }

    /**
     * For any printed character, this method returns the amount of space
     * required in excess of the bounding box of the character to draw the
     * caret.
     * Subclasses should override this method if they are drawing their
     * own carets.
     */
    public Rect getCaretBloat() {
        return _caretBloat;
    }

    //*********************************************************************
    //**************************** Utilities ******************************
    //*********************************************************************
    final protected int getPointerId(MotionEvent e) {
        return (e.getAction() & MotionEvent.ACTION_POINTER_ID_MASK)
                >> MotionEvent.ACTION_POINTER_ID_SHIFT;
    }

    /**
     * Converts a x-coordinate from screen coordinates to local coordinates,
     * excluding padding
     */
    final protected float screenToViewX(int x) {
        return x - _textField.getPaddingLeft() + _textField.getScrollX();
    }

    /**
     * Converts a y-coordinate from screen coordinates to local coordinates,
     * excluding padding
     */
    final protected int screenToViewY(int y) {
        return y - _textField.getPaddingTop() + _textField.getScrollY();
    }

    final public boolean isRightHanded() {
        return true;
    }

    final private boolean isDragSelect() {
        return true;
    }

    /**
     * Determine if a point(x,y) on screen is near a character of interest,
     * specified by its index charOffset. The radius of proximity is defined
     * by TOUCH_SLOP.
     *
     * @param x          X-coordinate excluding padding
     * @param y          Y-coordinate excluding padding
     * @param charOffset the character of interest
     * @return Whether (x,y) lies close to the character with index charOffset
     */
    public boolean isNearChar(float x, int y, int charOffset) {
        Rect bounds = _textField.getBoundingBox(charOffset);
        _touchOffsetX = 0;
        _touchOffsetY = ((bounds.top + bounds.bottom) >> 1) - y;
        if (_textField.handOn) {
            int m3 = EditCtrl.getHeight() * 3 / 5;
            if (_textField.isSelectText()) {
                int selectionStart = _textField.getSelectionStart();
                int selectionEnd = _textField.getSelectionEnd();
                if (selectionStart != selectionEnd) {
                    if (charOffset == selectionStart) {
                        bounds.right = bounds.left;
                        bounds.left -= m3;
                        bounds.bottom += m3;
                    } else if (charOffset == selectionEnd) {
                        bounds.right = bounds.left + m3;
                        bounds.bottom += m3;
                    }
                } else {
                    bounds.left -= (m3 >> 1);
                    bounds.right = bounds.left + (m3 >> 1);
                    bounds.bottom += (m3 + (m3 >> 1));
                }
            } else {
                bounds.left -= (m3 >> 1);
                bounds.right = bounds.left + (m3 >> 1);
                bounds.bottom += (m3 + (m3 >> 1));
            }
        }
        return (y >= (bounds.top - TOUCH_SLOP)
                && y < (bounds.bottom + TOUCH_SLOP)
                && x >= (bounds.left - TOUCH_SLOP)
                && x < (bounds.right + TOUCH_SLOP)
        );
    }
}
