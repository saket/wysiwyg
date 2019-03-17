package me.saket.markdownrenderer

import android.text.Spannable
import android.text.Spanned

/** TODO: Doc. */
interface MarkdownHintsSpanWriter {
  fun pushSpan(span: Any, start: Int, end: Int): MarkdownHintsSpanWriter
  fun writeTo(editable: Spannable)

  class Deferrable : MarkdownHintsSpanWriter {

    private val spans = mutableListOf<Triple<Any, Int, Int>>()

    override fun pushSpan(span: Any, start: Int, end: Int): MarkdownHintsSpanWriter {
      spans.add(Triple(span, start, end))
      return this
    }

    override fun writeTo(editable: Spannable) {
      for ((span, start, end) in spans) {
        editable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
      }
      spans.clear()
    }
  }
}
