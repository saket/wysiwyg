package me.saket.markdownrenderer.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.TextPaint
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import me.saket.markdownrenderer.spans.pool.Recycler
import ru.noties.markwon.core.MarkwonTheme
import ru.noties.markwon.utils.LeadingMarginUtils

/**
 * Copied from Markwon.
 * https://github.com/noties/Markwon/blob/822f16510e91d38f2a139e325aa3744b654805e1/markwon-core/src/main/java/io/noties/markwon/core/spans/HeadingSpan.java
 */
class HeadingSpanWithLevel(val recycler: Recycler) : MetricAffectingSpan(),
    LeadingMarginSpan,
    WysiwygSpan {

  lateinit var theme: MarkwonTheme
  var level: Int = 0

  private val rect = COMMON_RECT
  private val paint = COMMON_PAINT

  override fun updateMeasureState(textPaint: TextPaint) = apply(textPaint)

  override fun updateDrawState(textPaint: TextPaint) = apply(textPaint)

  private fun apply(paint: TextPaint) = theme.applyHeadingTextStyle(paint, level)

  override fun getLeadingMargin(first: Boolean): Int {
    // no margin actually, but we need to access Canvas to draw break
    return 0
  }

  override fun drawLeadingMargin(
    c: Canvas,
    p: Paint,
    x: Int,
    dir: Int,
    top: Int,
    baseline: Int,
    bottom: Int,
    text: CharSequence,
    start: Int,
    end: Int,
    first: Boolean,
    layout: Layout
  ) {
    if ((level == 1 || level == 2) && LeadingMarginUtils.selfEnd(end, text, this)) {
      paint.set(p)
      theme.applyHeadingBreakStyle(paint)

      val height = paint.strokeWidth
      if (height > .0f) {
        val b = (bottom - height + .5f).toInt()

        val left = if (dir > 0) x else (x - c.width)
        val right = if (dir > 0) c.width else x

        rect.set(left, b, right, bottom)
        c.drawRect(rect, paint)
      }
    }
  }

  override fun recycle() {
    recycler(this)
  }

  companion object {
    val COMMON_RECT = Rect()
    val COMMON_PAINT = Paint()
  }
}
