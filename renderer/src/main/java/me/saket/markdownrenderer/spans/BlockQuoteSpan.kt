package me.saket.markdownrenderer.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.style.LeadingMarginSpan
import me.saket.markdownrenderer.spans.pool.Recycler
import ru.noties.markwon.core.MarkwonTheme
import kotlin.math.max
import kotlin.math.min

/**
 * Copied from Markwon.
 * https://github.com/noties/Markwon/blob/822f16510e91d38f2a139e325aa3744b654805e1/markwon-core/src/main/java/io/noties/markwon/core/spans/BlockQuoteSpan.java
 */
class BlockQuoteSpan(val recycler: Recycler) : LeadingMarginSpan, WysiwygSpan {

  lateinit var theme: MarkwonTheme
  private val rect = COMMON_RECT
  private val paint = COMMON_PAINT

  override fun getLeadingMargin(first: Boolean) = theme.blockMargin

  override fun drawLeadingMargin(
    c: Canvas,
    p: Paint,
    x: Int,
    dir: Int,
    top: Int,
    baseline: Int,
    bottom: Int,
    text: CharSequence?,
    start: Int,
    end: Int,
    first: Boolean,
    layout: Layout?
  ) {
    val width = theme.blockQuoteWidth
    paint.set(p)
    theme.applyBlockQuoteStyle(paint)

    val l = x + dir * width
    val r = l + dir * width
    val left = min(l, r)
    val right = max(l, r)

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