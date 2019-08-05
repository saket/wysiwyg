package me.saket.markdownrenderer.spans

import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.pool.Recycler

class InlineCodeSpan(
  val theme: WysiwygTheme,
  val recycler: Recycler
) : MetricAffectingSpan(), WysiwygSpan {

  private val codeBackgroundColor = theme.codeBackgroundColor

  override fun updateMeasureState(textPaint: TextPaint) {
    apply(textPaint)
  }

  override fun updateDrawState(textPaint: TextPaint) {
    apply(textPaint)
    textPaint.bgColor = codeBackgroundColor
  }

  private fun apply(paint: TextPaint) {
    paint.typeface = Typeface.MONOSPACE
    paint.textSize = paint.textSize * CODE_DEFINITION_TEXT_SIZE_RATIO
  }

  override fun recycle() {
    recycler(this)
  }

  companion object {
    const val CODE_DEFINITION_TEXT_SIZE_RATIO = .87f
  }
}
