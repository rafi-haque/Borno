/*
 * Copyright (C) 2008-2009 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gilbertl.s9;


import java.util.List;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.PointF;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.Keyboard.Key;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;

public class S9IME extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener, View.OnTouchListener {
	
	static final String TAG = "S9InputMethodService";
    static final boolean DEBUG = false;

    private S9KeyboardView mInputView;
    private InputMethodManager mInputMethodManager;

    private float mSwipeSensitivity;
    private long mLastShiftTime;
    private boolean mCapsLock;
    
    private StringBuilder mComposing = new StringBuilder();
    //private WordComposer mWord = new WordComposer();

    private int mLastDisplayWidth;
    private long mMetaState;

    private RidmikParser toBangla = new RidmikParser();
    private int motion = 0;
    private int prevMotion = 0;

    private S9Keyboard mQwertytKeyboard;
    private S9Keyboard mShiftedKeyboard;
    
    private S9Keyboard mCurKeyboard;
    
    private String mWordSeparators;
    
    private S9KeyMotion[] mS9KeyMotions;
    
    /**
     * Main initialization of the input method component.  Be sure to call
     * to super class.
     */
    @Override public void onCreate() {
        super.onCreate();
        //initSuggest();
        mInputMethodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    }
    
    /**
     * This is the point where you can do all of your UI initialization.  It
     * is called after creation and any configuration change.
     */
    @Override public void onInitializeInterface() {
        if (mQwertytKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertytKeyboard = new S9Keyboard(this, R.xml.s9);
        mShiftedKeyboard = new S9Keyboard(this, R.xml.s9_shifted);
        mShiftedKeyboard.setShifted(false);
    }
    
    /**
     * Called by the framework when your view for creating input needs to
     * be generated.  This will be called the first time your input method
     * is displayed, and every time it needs to be re-created such as due to
     * a configuration change.
     */
    @Override public View onCreateInputView() {
    	final int maxMultiTouchEvents = 256;
    	mS9KeyMotions = new S9KeyMotion[maxMultiTouchEvents];
    	
        mInputView = (S9KeyboardView) getLayoutInflater().inflate(
                R.layout.input, null);
        //mInputView.setPreviewEnabled(false);
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setOnTouchListener(this);
        mInputView.setKeyboard(mQwertytKeyboard);
        return mInputView;
    }

    /**
     * Called by the framework when your view for showing candidates needs to
     * be generated, like {@link #onCreateInputView}.
     */
    @Override public View onCreateCandidatesView() {
//        mCandidateView = new CandidateView(this);
//        mCandidateView.setService(this);
//        return mCandidateView;
        return null;
    }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        mComposing.setLength(0);
//        mWord.reset();
//        updateCandidates();
        
        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }


        // We are now going to initialize our state based on the type of
        // text being edited.
        switch (attribute.inputType&EditorInfo.TYPE_MASK_CLASS) {
            case EditorInfo.TYPE_CLASS_NUMBER:
            case EditorInfo.TYPE_CLASS_DATETIME:      
            case EditorInfo.TYPE_CLASS_PHONE:      
            case EditorInfo.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mQwertytKeyboard;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType &  EditorInfo.TYPE_MASK_VARIATION;
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                }
                
                if (variation == EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS 
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_URI
                        || variation == EditorInfo.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    //mPredictionOn = false;
                	// it's nice to be able to see what's being typed
                }
                
                if ((attribute.inputType&EditorInfo.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {

                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertytKeyboard;
                updateShiftKeyState(attribute);
        }
        
        loadSettings();
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        mQwertytKeyboard.setImeOptions(getResources(), attribute.imeOptions);
        mShiftedKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }
        
    private void loadSettings() {
    	SharedPreferences sp =
    		PreferenceManager.getDefaultSharedPreferences(this);
    	mSwipeSensitivity = Float.parseFloat(
    		sp.getString(S9IMESettings.SWIPE_SENSITIVITY_KEY, "0.1"));
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        
        // Clear current composing text and candidates.
        mComposing.setLength(0);
//        mWord.reset();

        setCandidatesViewShown(false);
        
        mCurKeyboard = mQwertytKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }
    
    @Override
    public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();



        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        mInputView.setSubtypeOnSpaceKey(subtype);
    }



    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
//            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }


    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                // The InputMethodService already takes care of the back
                // key for us, to dismiss the input method if it is shown.
                // However, our keyboard could be showing a pop-up window
                // that back should dismiss, so we first allow it to do that.
                if (event.getRepeatCount() == 0 && mInputView != null) {
                    if (mInputView.handleBack()) {
                        return true;
                    }
                }
                break;
                
            case KeyEvent.KEYCODE_DEL:
                // Special handling of the delete key: if we currently are
                // composing text for the user, we want to modify that instead
                // of let the application to the delete itself.
                if (mComposing.length() > 0) {
                    onKey(Keyboard.KEYCODE_DELETE, null);
                    return true;
                }
                break;
                
            case KeyEvent.KEYCODE_ENTER:
                // Let the underlying text editor always handle these.
                return false;
                
            default:
            	break;
        }
        
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Use this to monitor key events being delivered to the application.
     * We get first crack at them, and can either resume them or let them
     * continue to the app.
     */
    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }


    
    /**
     * Commits a certain char sequence to editor.
     */
//    private void commitText(
//    		InputConnection inputConn, CharSequence text, int textLength) {
//        if (mComposing.length() > 0) {
//            inputConn.commitText(text, textLength);
//            mComposing.setLength(0);
//            mWord.reset();
//        }
//    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null 
                && mInputView != null && mCurKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != EditorInfo.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            useShiftedKeyboard(caps != 0);
//            mInputView.setShifted(mCapsLock || caps != 0 );
        }
    }
    
    private void useShiftedKeyboard(boolean useShifted) {
    	if (useShifted) {
	    	mCurKeyboard = mShiftedKeyboard;
	    	mInputView.setKeyboard(mShiftedKeyboard);
    	} else {
    		mCurKeyboard = mQwertytKeyboard;
    		mInputView.setKeyboard(mQwertytKeyboard);
    	}
    }
    
    private void toggleKeyboard() {
    	useShiftedKeyboard(!isShifted());
    }
    
    private boolean isShifted() {
    	return mCurKeyboard == mShiftedKeyboard;
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }
    
    /**
     * Helper to send a character to the editor as raw key events.
     */
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection()
                    .commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    // Implementation of KeyboardViewListener

    public void onKey(int primaryCode, int[] keyCodes) {
        int motion = 0;
    	handleKey(primaryCode, keyCodes, motion);
    }

    public void onText(CharSequence text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
        	commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }


    
    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            mComposing.delete(length - 1, length);
            if(motion != 0) {
                getCurrentInputConnection().setComposingText(toBangla.toBangla(mComposing.toString()), 1);
            }
            else if (motion == 0){
                getCurrentInputConnection().setComposingText(mComposing.toString(), 1);
            }
        } else if (length > 0) {
            mComposing.setLength(0);
            getCurrentInputConnection().commitText("", 0);
        } else {
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

//    private void handleShift() {
//        if (mInputView == null) {
//            return;
//        }
//        toggleKeyboard();
//    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }

        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertytKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
//            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
            mCurKeyboard = mShiftedKeyboard;
            mInputView.setKeyboard(mShiftedKeyboard);
        }
        else{
            mCurKeyboard = mQwertytKeyboard;
            mInputView.setKeyboard(mQwertytKeyboard);
        }

    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }

    private boolean sameMotion(){
        if(motion==prevMotion)
            return true;
        else
            return false;
    }

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConn) {
        if (mComposing.length() > 0) {
            final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
            if(subtype.getLocale().equals("bangla")) {
                if(prevMotion == 0){
                    getCurrentInputConnection().commitText(toBangla.toBangla(mComposing.toString()), mComposing.length());
                }
                else if(prevMotion != 0){
                    getCurrentInputConnection().commitText(mComposing, mComposing.length());
                }
//                getCurrentInputConnection().commitText(toBangla.toBangla(mComposing.toString()), mComposing.length());

            }
            else if(subtype.getLocale().equals("en_US")) {
                if(prevMotion != 0){
                    getCurrentInputConnection().commitText(toBangla.toBangla(mComposing.toString()), mComposing.length());
                }
                else if(prevMotion == 0){
                    getCurrentInputConnection().commitText(mComposing, mComposing.length());
                }
            }

            mComposing.setLength(0);
        }
    }


    private void handleCharacter(int primaryCode, int [] codes, int motion) {
        if (isInputViewShown()) {
            if (mInputView.isShifted()) {
                primaryCode = Character.toUpperCase(primaryCode);
            }
        }
        if (isAlphabet(primaryCode)) {
            mComposing.append((char) primaryCode);
            final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
            if(subtype.getLocale().equals("bangla")) {
                if(motion == 0){
                    getCurrentInputConnection().setComposingText(toBangla.toBangla(mComposing.toString()), 1);
                } else if(motion != 0){
                    getCurrentInputConnection().setComposingText(mComposing.toString(), 1);
                }
//                getCurrentInputConnection().commitText(toBangla.toBangla(mComposing.toString()), mComposing.length());

            }
            else if(subtype.getLocale().equals("en_US")) {
                if(motion != 0){
                    getCurrentInputConnection().setComposingText(toBangla.toBangla(mComposing.toString()), 1);
                } else if(motion == 0){
                    getCurrentInputConnection().setComposingText(mComposing.toString(), 1);
                }
            }

            updateShiftKeyState(getCurrentInputEditorInfo());

        } else {
            getCurrentInputConnection().commitText(
                    String.valueOf((char) primaryCode), 1);
        }
        prevMotion = motion;
    }

    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }
    


    private void handleClose() {
    	commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }
    
    private String getWordSeparators() {
        return mWordSeparators;
    }
    
    public boolean isWordSeparator(int code) {
        String separators = getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }


    
    public void swipeRight() {
    }
    
    public void swipeLeft() {
    }

    public void swipeDown() {
        handleClose();
    }

    public void swipeUp() {
    }
    
    public void onPress(int primaryCode) {
    }
    
    public void onRelease(int primaryCode) {
    }

	public boolean onTouch(View v, MotionEvent event) {
		int action = event.getAction();
		int actionCode= action & MotionEvent.ACTION_MASK;
		int pointerId = -1;
		int pointerIdx = -1;
		
		switch (actionCode) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_UP:
				assert event.getPointerCount() == 1;
				pointerIdx = 0;
				pointerId = event.getPointerId(pointerIdx);
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
			case MotionEvent.ACTION_POINTER_UP:
				pointerId = action >> MotionEvent.ACTION_POINTER_ID_SHIFT;
				pointerIdx = event.findPointerIndex(pointerId);
				break;
			default:
				return false;
		}
		
		if (pointerId == -1 && pointerIdx == -1) {
			// not going to handle any non-down/up touches
			return false;
		}
		
		S9KeyMotion s9km;
		switch (actionCode) {
			case MotionEvent.ACTION_DOWN:
			case MotionEvent.ACTION_POINTER_DOWN:
				assert mS9KeyMotions[pointerId] == null;
				PointF downPoint =
					new PointF(event.getX(pointerIdx), event.getY(pointerIdx));
				Key keyPressed = findKey(downPoint);
		    	s9km = new S9KeyMotion(
		    			downPoint, keyPressed, mSwipeSensitivity);
	    		mS9KeyMotions[pointerId] = s9km;
	    		if (keyPressed != null && keyPressed.repeatable) {
	    			// let KeyboardView handle repeatable keys
	    			return false;
	    		}
				return true;
			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_POINTER_UP:
				s9km = mS9KeyMotions[pointerId];
				assert s9km != null;
				mS9KeyMotions[pointerId] = null;
				if (s9km.getKey() != null) {
					if (s9km.getKey().repeatable) {
		    			// let KeyboardView handle repeatable keys
						return false;
					}
					PointF upPoint = new PointF(
							event.getX(pointerIdx), event.getY(pointerIdx));
					motion = s9km.calcMotion(upPoint);
					int code = s9km.getKey().codes[motion];
		    		handleKey(code, s9km.getKey().codes, motion);
				}
	    		return true;
			default:
				break;
		}
		
		return false;
	}
	
	private Key findKey(PointF pt) {
		int [] nearbyKeyIndices =
			mCurKeyboard.getNearestKeys((int) pt.x, (int) pt.y);
		int l = nearbyKeyIndices.length;
		List<Key> keys = mCurKeyboard.getKeys();
		for (int i = 0; i < l; i++) {
			Key key = keys.get(nearbyKeyIndices[i]);
			if (key.isInside((int) pt.x, (int) pt.y)) {
				return key;
			}
		}		
		return null;
	}

	private void handleKey(int primaryCode, int [] codes, int motion) {
        if (isWordSeparator(primaryCode)) {
            // Handle separator
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            }
            sendKey(primaryCode);
            updateShiftKeyState(getCurrentInputEditorInfo());
        }
        else if(primaryCode == S9KeyboardView.KEYCODE_SPACE){
            commitTyped(getCurrentInputConnection());
        }
        else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_SHIFT) {
            handleShift();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == S9KeyboardView.KEYCODE_LANGUAGE_SWITCH) {
            handleLanguageSwitch();
            return;
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
        	assert false;
        } else if (primaryCode == S9Keyboard.KEYCODE_NULL) {
        	// do nothing
        } else {
            handleCharacter(primaryCode, codes, motion);
        }
	}

    private void handleLanguageSwitch() {
        mInputMethodManager.switchToNextInputMethod(getToken(), true /* onlyCurrentIme */);
    }

    private IBinder getToken() {
        final Dialog dialog = getWindow();
        if (dialog == null) {
            return null;
        }
        final Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        return window.getAttributes().token;
    }

}
