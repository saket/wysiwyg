package me.saket.markdownrenderer.spans

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.spans.pool.Recycler

/**
 * Copied from https://github.com/noties/Markwon.
 */
class HeadingSpan(
  val theme: WysiwygTheme,
  val recycler: Recycler
) : MetricAffectingSpan(), WysiwygSpan {

  lateinit var level: HeadingLevel

  override fun updateMeasureState(textPaint: TextPaint) = apply(textPaint)

  override fun updateDrawState(textPaint: TextPaint) = apply(textPaint)

  private fun apply(paint: TextPaint) {
    paint.isFakeBoldText = true
    paint.textSize *= level.textSizeRatio
  }

  override fun recycle() {
    recycler(this)
  }
}
