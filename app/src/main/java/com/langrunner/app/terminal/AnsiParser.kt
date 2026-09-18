package com.langrunner.app.terminal

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan

/**
 * Minimal ANSI SGR (Select Graphic Rendition) parser. Converts ANSI color/style
 * escape codes into a colored/styled Spannable for display in a plain TextView.
 *
 * This does NOT implement cursor-addressable rendering (no \x1b[H cursor
 * positioning, no alternate screen buffer, no character grid) — that requires
 * a full custom terminal view and is tracked as future work. What this covers
 * is the most common real-world use of ANSI codes in a build/run terminal:
 * colored compiler output, `ls --color`, `grep --color`, etc.
 */
object AnsiParser {

    private val sgrRegex = Regex("\u001B\\[([0-9;]*)m")
    private val unsupportedEscapeRegex = Regex("\u001B\\[[0-9;]*[A-HJKST]|\u001B\\[\\?[0-9]+[hl]")

    private val palette = mapOf(
        30 to Color.rgb(0, 0, 0),
        31 to Color.rgb(205, 49, 49),
        32 to Color.rgb(13, 188, 121),
        33 to Color.rgb(229, 229, 16),
        34 to Color.rgb(36, 114, 200),
        35 to Color.rgb(188, 63, 188),
        36 to Color.rgb(17, 168, 205),
        37 to Color.rgb(229, 229, 229),
        90 to Color.rgb(102, 102, 102),
        91 to Color.rgb(241, 76, 76),
        92 to Color.rgb(35, 209, 139),
        93 to Color.rgb(245, 245, 67),
        94 to Color.rgb(59, 142, 234),
        95 to Color.rgb(214, 112, 214),
        96 to Color.rgb(41, 184, 219),
        97 to Color.rgb(229, 229, 229)
    )

    fun render(raw: String, defaultColor: Int): SpannableStringBuilder {
        val cleaned = unsupportedEscapeRegex.replace(raw, "")

        val builder = SpannableStringBuilder()
        var currentColor = defaultColor
        var bold = false
        var lastIndex = 0

        for (match in sgrRegex.findAll(cleaned)) {
            val textChunk = cleaned.substring(lastIndex, match.range.first)
            if (textChunk.isNotEmpty()) {
                appendStyled(builder, textChunk, currentColor, bold)
            }

            val codes = match.groupValues[1].split(";").mapNotNull { it.toIntOrNull() }
            if (codes.isEmpty()) {
                currentColor = defaultColor
                bold = false
            } else {
                for (code in codes) {
                    when {
                        code == 0 -> { currentColor = defaultColor; bold = false }
                        code == 1 -> bold = true
                        code == 22 -> bold = false
                        code == 39 -> currentColor = defaultColor
                        palette.containsKey(code) -> currentColor = palette.getValue(code)
                    }
                }
            }
            lastIndex = match.range.last + 1
        }

        if (lastIndex < cleaned.length) {
            appendStyled(builder, cleaned.substring(lastIndex), currentColor, bold)
        }

        return builder
    }

    private fun appendStyled(builder: SpannableStringBuilder, text: String, color: Int, bold: Boolean) {
        val start = builder.length
        builder.append(text)
        builder.setSpan(ForegroundColorSpan(color), start, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        if (bold) {
            builder.setSpan(StyleSpan(Typeface.BOLD), start, builder.length, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
