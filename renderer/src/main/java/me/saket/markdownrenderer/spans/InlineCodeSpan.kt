package me.saket.markdownrenderer.spans

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import me.saket.markdownrenderer.spans.pool.Recycler
import ru.noties.markwon.core.MarkwonTheme

class InlineCodeSpan(
  val theme: MarkwonTheme,
  val recycler: Recycler
) : MetricAffectingSpan(), WysiwygSpan {

  override fun updateMeasureState(textPaint: TextPaint) {
    apply(textPaint)
  }

  override fun updateDrawState(textPaint: TextPaint) {
    apply(textPaint)
    textPaint.bgColor = theme.getCodeBackgroundColor(textPaint)
  }

  private fun apply(p: TextPaint) {
    theme.applyCodeTextStyle(p)
  }

  override fun recycle() {
    recycler(this)
  }
}
