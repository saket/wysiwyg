package me.saket.markdownrenderer.spans.pool

import androidx.annotation.ColorInt
import me.saket.markdownrenderer.spans.ForegroundColorSpan
import me.saket.markdownrenderer.spans.IndentedCodeBlockSpan
import me.saket.markdownrenderer.spans.MonospaceTypefaceSpan

fun SpanPool.foregroundColor(@ColorInt color: Int) =
  get { ForegroundColorSpan(recycler) }.apply {
    this.color = color
  }

fun SpanPool.indentedCodeBlock() =
  get { IndentedCodeBlockSpan(theme, recycler) }

fun SpanPool.monospaceTypeface() =
  get { MonospaceTypefaceSpan(recycler) }