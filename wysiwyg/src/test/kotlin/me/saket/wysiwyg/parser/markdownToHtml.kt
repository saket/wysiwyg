package me.saket.wysiwyg.parser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.sp
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.internal.MarkdownRenderer
import me.saket.wysiwyg.internal.MarkdownStyleBuffer
import me.saket.wysiwyg.internal.TextFieldLayoutInfo

internal fun MarkdownDocument.renderHtml(source: String): String {
  val markdownRenderer = MarkdownRenderer(FakeWysiwygTheme, TextFieldLayoutInfo())
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
  override val unstyledText: String,
  val testTags: MutableList<TestTag> = mutableListOf(),
) : MarkdownStyleBuffer by MarkdownStyleBuffer.Empty {

  override fun addTestTag(tag: String, range: TextRange) {
    testTags += TestTag(tag, range)
  }
}

private data class TestTag(
  val tag: String,
  val range: TextRange,
)

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
