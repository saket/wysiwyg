package me.saket.wysiwyg.internal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import me.saket.wysiwyg.SpanTextRange
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.parser.MarkdownSpan

@JvmInline
internal value class MarkdownRenderer(
  private val theme: WysiwygTheme
) {
  fun buildAnnotatedString(text: AnnotatedString, spans: List<MarkdownSpan>): AnnotatedString {
    // TODO: remove once this is available in a stable release:
    //  https://android-review.googlesource.com/c/platform/frameworks/support/+/2171623/
    val spans = spans.sortedBy { it.range.startIndex }

    val scope = object : MarkdownRendererScope {
      override val theme: WysiwygTheme get() = this@MarkdownRenderer.theme
      override val unstyledText: AnnotatedString get() = text
    }

    return buildAnnotatedString {
      // Discard any previous styles that may have gotten restored after a config change.
      // This is slightly unfortunate because any spans added by user will also be discarded.
      append(text.text)

      val textBuilder: AnnotatedString.Builder = this
      spans.fastForEach { span ->
        with(span.style) {
          scope.render(text = textBuilder, span.range)
        }
      }
    }
  }
}

interface MarkdownRendererScope {
  val theme: WysiwygTheme
  val unstyledText: AnnotatedString

  fun AnnotatedString.Builder.addStyle(style: SpanStyle, range: SpanTextRange) {
    addStyle(
      style = style,
      start = range.startIndex.coerceAtMost(unstyledText.lastIndex),
      end = range.endIndexExclusive.coerceAtMost(length)  // todo: remove these coerces. coerce in offsetSpansOnTextChange() instead.
    )
  }

  fun AnnotatedString.Builder.addStyle(style: ParagraphStyle, range: SpanTextRange) {
    addStyle(
      style = style,
      start = range.startIndex.coerceAtMost(unstyledText.lastIndex),
      end = range.endIndexExclusive.coerceAtMost(length)
    )
    // Compose UI adds a lot of vertical paddings around paragraphs.
    // Reduce the font size of line breaks to make them smaller.
    // https://issuetracker.google.com/u/1/issues/241426911
    if (unstyledText.getOrNull(range.startIndex - 1) == '\n') {
      addStyle(SpanStyle(fontSize = 1.sp), start = range.startIndex - 1, end = range.startIndex)
    }
    if (unstyledText.getOrNull(range.endIndexExclusive) == '\n') {
      addStyle(SpanStyle(fontSize = 1.sp), start = range.endIndexExclusive - 1, end = range.endIndexExclusive + 1)
    }
  }
}
