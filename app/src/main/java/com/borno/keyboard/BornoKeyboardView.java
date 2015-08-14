package com.borno.keyboard;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodSubtype;

public class BornoKeyboardView extends KeyboardView {

	static final String TAG = "S9KeyboardView";
    static final int KEYCODE_OPTIONS = -100;
    static final int KEYCODE_SPACE = 32;
    static final int KEYCODE_LANGUAGE_SWITCH = -101;

    private View.OnTouchListener onTouchListener;

    public BornoKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public BornoKeyboardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

	@Override
    protected boolean onLongPress(Key key) {
        if (key.codes[0] == Keyboard.KEYCODE_CANCEL) {
            getOnKeyboardActionListener().onKey(KEYCODE_OPTIONS, null);
            return true;
        } else {
            return super.onLongPress(key);
        }
    }

    void setSubtypeOnSpaceKey(final InputMethodSubtype subtype) {
        final BornoKeyboard keyboard = (BornoKeyboard)getKeyboard();
        keyboard.setSpaceIcon(getResources().getDrawable(subtype.getIconResId()));
        invalidateAllKeys();
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent me) {
    	return getOnTouchListener().onTouch(this,me) || super.onTouchEvent(me);
    }
    
    public View.OnTouchListener getOnTouchListener() {
		return onTouchListener;
	}

    @Override
	public void setOnTouchListener(View.OnTouchListener onTouchListener) {
		this.onTouchListener = onTouchListener;
	}
}