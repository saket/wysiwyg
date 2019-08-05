package me.saket.markdownrenderer.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.TextPaint
import android.text.style.LeadingMarginSpan
import android.text.style.MetricAffectingSpan
import me.saket.markdownrenderer.spans.pool.Recycler
import io.noties.markwon.core.MarkwonTheme

/**
 * Copied from Markwon.
 * https://github.com/noties/Markwon/blob/822f16510e91d38f2a139e325aa3744b654805e1/markwon-core/src/main/java/io/noties/markwon/core/spans/CodeBlockSpan.java
 */
class IndentedCodeBlockSpan(
  val theme: MarkwonTheme,
  val recycler: Recycler
) : MetricAffectingSpan(), LeadingMarginSpan, WysiwygSpan {

  private val rect = COMMON_RECT
  private val paint = COMMON_PAINT

  override fun updateMeasureState(textPaint: TextPaint) = apply(textPaint)

  override fun updateDrawState(textPaint: TextPaint) = apply(textPaint)

  override fun getLeadingMargin(first: Boolean) = theme.codeBlockMargin

  private fun apply(p: TextPaint) = theme.applyCodeBlockTextStyle(p)

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
    paint.style = Paint.Style.FILL
    paint.color = theme.getCodeBlockBackgroundColor(p)

    val left = if (dir > 0) x else (x - c.width)
    val right = if (dir > 0) c.width else x

    rect.set(left, top, right, bottom)
    c.drawRect(rect, paint)
  }

  override fun recycle() {
    recycler(this)
  }

  companion object {
    val COMMON_RECT = Rect()
    val COMMON_PAINT = Paint()
  }
}
