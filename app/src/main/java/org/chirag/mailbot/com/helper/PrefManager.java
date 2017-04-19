package org.chirag.mailbot.com.helper;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.chirag.mailbot.com.MainApplication;

public class PrefManager {

    private static SharedPreferences getPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(MainApplication.getInstance()
                .getApplicationContext());
    }

    public static boolean getBoolean(String preferenceKey, boolean preferenceDefaultValue) {
        return getPreferences().getBoolean(preferenceKey, preferenceDefaultValue);
    }
}


