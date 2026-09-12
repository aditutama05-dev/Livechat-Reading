package com.example.bacachat;

import android.content.Context;
import android.content.SharedPreferences;

public class LanguageSettings {

    private static final String PREF_NAME =
            "language_settings";

    private static final String KEY_TTS_LANGUAGE =
            "tts_language";

    private static final String KEY_OCR_LANGUAGE =
            "ocr_language";

    private final SharedPreferences preferences;

    public LanguageSettings(Context context) {

        preferences = context.getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
        );
    }

    public String getTtsLanguage() {

        return preferences.getString(
                KEY_TTS_LANGUAGE,
                "en-US"
        );
    }

    public void setTtsLanguage(String languageCode) {

        preferences.edit()
                .putString(
                        KEY_TTS_LANGUAGE,
                        languageCode
                )
                .apply();
    }

    public String getOcrLanguage() {

        return preferences.getString(
                KEY_OCR_LANGUAGE,
                "en"
        );
    }

    public void setOcrLanguage(String languageCode) {

        preferences.edit()
                .putString(
                        KEY_OCR_LANGUAGE,
                        languageCode
                )
                .apply();
    }
}
