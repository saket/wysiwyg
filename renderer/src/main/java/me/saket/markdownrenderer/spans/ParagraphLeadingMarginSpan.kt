package me.saket.markdownrenderer.spans

import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.style.LeadingMarginSpan
import me.saket.markdownrenderer.spans.pool.Recycler

class ParagraphLeadingMarginSpan(val recycler: Recycler) : LeadingMarginSpan, WysiwygSpan {

  var margin: Int = 0

  override fun getLeadingMargin(first: Boolean): Int {
    return margin
  }

  override fun drawLeadingMargin(
    c: Canvas?,
    p: Paint?,
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
  ) = Unit

  override fun recycle() {
    recycler(this)
  }
}