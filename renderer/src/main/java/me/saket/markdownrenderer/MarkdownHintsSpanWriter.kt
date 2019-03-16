package me.saket.markdownrenderer

import android.text.Editable
import android.text.Spanned

/**
 * TODO: Doc.
 */
class MarkdownHintsSpanWriter {

  private var editable: Editable? = null

  fun setText(editable: Editable) {
    this.editable = editable
  }

  fun pushSpan(span: Any, start: Int, end: Int): MarkdownHintsSpanWriter {
    if (!MarkdownHints.SUPPORTED_MARKDOWN_SPANS.contains(span.javaClass)) {
      throw IllegalArgumentException("Span not supported: " + span.javaClass)
    }
    editable!!.setSpan(span, start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE)
    return this
  }
}
