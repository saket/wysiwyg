package me.saket.markdownrenderer.spans

import android.annotation.SuppressLint
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import me.saket.markdownrenderer.spans.pool.Recycler

/**
 * Copy of [android.text.style.StyleSpan].
 */
class StyleSpan(val recycler: Recycler) : MetricAffectingSpan(), WysiwygSpan {

  @get:JvmName("style")
  var style: Int = Typeface.NORMAL

  override fun updateMeasureState(textPaint: TextPaint) {
    apply(textPaint, style)
  }

  override fun updateDrawState(textPaint: TextPaint) {
    apply(textPaint, style)
  }

  @SuppressLint("WrongConstant")
  private fun apply(
    paint: Paint,
    style: Int
  ) {
    val old = paint.typeface
    val oldStyle: Int = old?.style ?: 0

    val want = oldStyle or style

    val tf: Typeface = when (old) {
      null -> Typeface.defaultFromStyle(want)
      else -> Typeface.create(old, want)
    }

    val fake = want and tf.style.inv()

    if (fake and Typeface.BOLD != 0) {
      paint.isFakeBoldText = true
    }

    if (fake and Typeface.ITALIC != 0) {
      paint.textSkewX = -0.25f
    }

    paint.typeface = tf
  }

  override fun recycle() {
    recycler(this)
  }
}