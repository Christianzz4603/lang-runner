package com.langrunner.app.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("lang_runner_settings", Context.MODE_PRIVATE)

    var backgroundColor: Int
        get() = prefs.getInt(KEY_BG_COLOR, DEFAULT_BG)
        set(value) = prefs.edit().putInt(KEY_BG_COLOR, value).apply()

    var textColor: Int
        get() = prefs.getInt(KEY_TEXT_COLOR, DEFAULT_TEXT)
        set(value) = prefs.edit().putInt(KEY_TEXT_COLOR, value).apply()

    var fontSizeSp: Float
        get() = prefs.getFloat(KEY_FONT_SIZE, DEFAULT_FONT_SIZE)
        set(value) = prefs.edit().putFloat(KEY_FONT_SIZE, value).apply()

    var fontFamily: String
        get() = prefs.getString(KEY_FONT_FAMILY, DEFAULT_FONT_FAMILY) ?: DEFAULT_FONT_FAMILY
        set(value) = prefs.edit().putString(KEY_FONT_FAMILY, value).apply()

    companion object {
        private const val KEY_BG_COLOR = "bg_color"
        private const val KEY_TEXT_COLOR = "text_color"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_FONT_FAMILY = "font_family"

        const val DEFAULT_BG = 0xFF000000.toInt()
        const val DEFAULT_TEXT = 0xFF33FF33.toInt()
        const val DEFAULT_FONT_SIZE = 14f
        const val DEFAULT_FONT_FAMILY = "monospace"
    }
}
