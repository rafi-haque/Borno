//package com.haque.settings;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.preference.EditTextPreference;
//import android.preference.PreferenceActivity;
//import android.content.Intent;
//import android.os.Bundle;
//import android.preference.PreferenceActivity;
//import com.android.inputmethodcommon.InputMethodSettingsFragment;
//
//
//import com.gilbertl.s9.R;
//
//public class S9IMESettings extends PreferenceActivity {
//    //implements SharedPreferences.OnSharedPreferenceChangeListener {
//
//    public static final String SWIPE_SENSITIVITY_KEY = "swipe_sensitivity";
//
//    private EditTextPreference mSwipeSensitivity;
//
//    @Override
//    public Intent getIntent() {
//        final Intent modIntent = new Intent(super.getIntent());
//        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, Settings.class.getName());
//        modIntent.putExtra(EXTRA_NO_HEADERS, true);
//        return modIntent;
//    }
//
//
//    @Override
//    protected void onCreate(Bundle icicle) {
//        super.onCreate(icicle);
//        addPreferencesFromResource(R.xml.prefs);
//        mSwipeSensitivity = (EditTextPreference)
//        	findPreference(SWIPE_SENSITIVITY_KEY);
//        super.onCreate(icicle);
//
//        // We overwrite the title of the activity, as the default one is "Voice Search".
//        setTitle("Title");
//        //getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(
//        //        this);
//    }
//
//    @Override
//    protected boolean isValidFragment(final String fragmentName) {
//        return Settings.class.getName().equals(fragmentName);
//    }
//
//    public static class Settings extends InputMethodSettingsFragment {
//        @Override
//        public void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
//            setInputMethodSettingsCategoryTitle(R.string.language_selection_title);
//            setSubtypeEnablerTitle(R.string.select_language);
//
//            // Load the preferences from an XML resource
//            addPreferencesFromResource(R.xml.ime_preferences);
//        }
//    }
//
//    /*
//    @Override
//    protected void onResume() {
//        super.onResume();
//        int autoTextSize = AutoText.getSize(getListView());
//        if (autoTextSize < 1) {
//            ((PreferenceGroup) findPreference(PREDICTION_SETTINGS_KEY))
//                .removePreference(mQuickFixes);
//        } else {
//            mShowSuggestions.setDependency(QUICK_FIXES_KEY);
//        }
//    }
//    */
//
//    /*
//    @Override
//    protected void onDestroy() {
//        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(
//                this);
//        super.onDestroy();
//    }
//    */
//
//    /*
//    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
//            String key) {
//        (new BackupManager(this)).dataChanged();
//    }
//    */
//}

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gilbertl.s9;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import com.android.inputmethodcommon.InputMethodSettingsFragment;
import com.gilbertl.s9.R;

/**
 * Displays the IME preferences inside the input method setting.
 */
public class S9IMESettings extends PreferenceActivity {

    public static final String SWIPE_SENSITIVITY_KEY = "swipe_sensitivity";

    @Override
    public Intent getIntent() {
        final Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, Settings.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We overwrite the title of the activity, as the default one is "Voice Search".
        setTitle(R.string.settings_name);
    }

    @Override
    protected boolean isValidFragment(final String fragmentName) {
        return Settings.class.getName().equals(fragmentName);
    }

    public static class Settings extends InputMethodSettingsFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setInputMethodSettingsCategoryTitle(R.string.language_selection_title);
            setSubtypeEnablerTitle(R.string.select_language);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.ime_preferences);
        }
    }
}
