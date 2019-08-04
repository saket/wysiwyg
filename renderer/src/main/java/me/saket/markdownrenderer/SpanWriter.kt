package me.saket.markdownrenderer

import android.text.Spannable
import android.text.Spanned
import me.saket.markdownrenderer.spans.WysiwygSpan

/**
 * Inserts spans to [Spannable].
 */
class SpanWriter {

  private val spans = mutableListOf<Triple<Any, Int, Int>>()

  fun add(span: WysiwygSpan, start: Int, end: Int): SpanWriter {
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
