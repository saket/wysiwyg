package me.saket.markdownrenderer

import android.text.Spannable
import android.text.Spanned

/**
 * Inserts spans to [Spannable].
 */
class MarkdownHintsSpanWriter {

  private val spans = mutableListOf<Triple<Any, Int, Int>>()

  fun add(span: Any, start: Int, end: Int): MarkdownHintsSpanWriter {
    spans.add(Triple(span, start, end))
    return this
  }

  fun writeTo(editable: Spannable) {
    for ((span, start, end) in spans) {
      editable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    spans.clear()
  }
}
