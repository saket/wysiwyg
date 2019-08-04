package me.saket.markdownrenderer.spans

import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import ru.noties.markwon.core.MarkwonTheme

class InlineCodeSpan : MetricAffectingSpan(), WysiwygSpan {

  lateinit var theme: MarkwonTheme

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
}
