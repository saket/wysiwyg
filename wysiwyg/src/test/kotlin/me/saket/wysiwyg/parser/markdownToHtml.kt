package me.saket.wysiwyg.parser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.sp
import me.saket.wysiwyg.MarkdownSpanPainter
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.internal.MarkdownRenderer
import me.saket.wysiwyg.internal.MarkdownStyleBuffer

internal fun MarkdownDocument.renderHtml(source: String): String {
  val markdownRenderer = MarkdownRenderer(FakeWysiwygTheme)
  val buffer = TestTagRecordingBuffer(source)
  markdownRenderer.render(document = this, buffer)

  val tags = buffer.testTags
    .flatMap { listOf(it.range.start to "<${it.tag}>", it.range.end to "</${it.tag}>") }
    .sortedBy { it.first }

  return buildString {
    var cursor = 0
    for ((offset, tag) in tags) {
      append(source, cursor, offset)
      append(tag)
      cursor = offset
    }
    append(source, cursor, source.length)
  }
}

private class TestTagRecordingBuffer(
  override val unstyledText: CharSequence,
) : MarkdownStyleBuffer {
  data class TestTag(val tag: String, val range: TextRange)

  val testTags: MutableList<TestTag> = mutableListOf()

  override fun addTestTag(tag: String, range: TextRange) {
    testTags += TestTag(tag, range)
  }

  override fun addSpanPainter(painter: MarkdownSpanPainter) = Unit
  override fun addStyle(style: SpanStyle, range: TextRange) = Unit
  override fun addStyle(
    style: ParagraphStyle,
    range: TextRange,
    trimVerticalPadding: Boolean
  ) = Unit
}

private val FakeWysiwygTheme = WysiwygTheme(
  markerColor = Color.Unspecified,
  linkTextColor = Color.Unspecified,
  linkUrlColor = Color.Unspecified,
  struckThroughTextColor = Color.Unspecified,
  codeBackground = Color.Unspecified,
  codeBlockLeadingPadding = 0.sp,
  blockQuoteText = Color.Unspecified,
  blockQuoteLeadingPadding = 0.sp,
  listBlockLeadingPadding = 0.sp,
  headingColor = Color.Unspecified,
)
