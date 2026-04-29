package me.saket.wysiwyg.parser

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.sp
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.internal.MarkdownRenderer

internal fun MarkdownDocument.renderHtml(source: String): String {
  val markdownRenderer = MarkdownRenderer(FakeWysiwygTheme, useTestTags = true)
  val annotated = markdownRenderer.buildAnnotatedString(
    text = AnnotatedString(source),
    document = this,
  ).text
  val tags = annotated.getStringAnnotations("test-tag", 0, annotated.length)
    .flatMap { listOf(it.start to "<${it.item}>", it.end to "</${it.item}>") }
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
