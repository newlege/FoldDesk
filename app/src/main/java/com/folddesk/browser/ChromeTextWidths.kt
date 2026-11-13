package com.folddesk.browser

import android.content.Context
import android.graphics.Paint
import android.widget.TextView
import androidx.annotation.DimenRes

/**
 * Fixed CJK title widths measured with [Paint.measureText] at the chip's text size.
 */
object ChromeTextWidths {

    private const val CJK_SAMPLE = "测"
    const val TAB_TITLE_HANZI_COUNT = 9
    const val BOOKMARK_TITLE_HANZI_COUNT = 6

    fun tabTitleWidthPx(context: Context): Int =
        cjkLineWidthPx(context, R.dimen.chrome_tab_text_size, TAB_TITLE_HANZI_COUNT)

    fun bookmarkTitleWidthPx(context: Context): Int =
        cjkLineWidthPx(context, R.dimen.chrome_bookmark_text_size, BOOKMARK_TITLE_HANZI_COUNT)

    fun applyTabTitleWidth(textView: TextView) {
        val width = tabTitleWidthPx(textView.context)
        textView.layoutParams = textView.layoutParams.apply { this.width = width }
    }

    fun applyBookmarkTitleWidth(textView: TextView) {
        val width = bookmarkTitleWidthPx(textView.context)
        textView.layoutParams = textView.layoutParams.apply { this.width = width }
    }

    private fun cjkLineWidthPx(context: Context, @DimenRes textSizeDimen: Int, hanziCount: Int): Int {
        val textSizePx = context.resources.getDimension(textSizeDimen)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = textSizePx
        }
        return paint.measureText(CJK_SAMPLE.repeat(hanziCount)).toInt()
    }
}
