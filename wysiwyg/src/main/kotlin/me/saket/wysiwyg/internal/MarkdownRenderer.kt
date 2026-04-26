package me.saket.wysiwyg.internal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.sp
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.highlight.MarkdownDocument

@JvmInline
internal value class MarkdownRenderer(
  private val theme: WysiwygTheme,
) {
  fun buildAnnotatedString(text: AnnotatedString, document: MarkdownDocument): AnnotatedString {
    val scope = object : MarkdownRendererScope {
      override val theme: WysiwygTheme get() = this@MarkdownRenderer.theme
      override val unstyledText: AnnotatedString get() = text
    }
    return buildAnnotatedString {
      // Discard any previous styles that may have gotten restored after a config change.
      // This is slightly unfortunate because any spans added by user will also be discarded.
      append(text.text)
      with(document) { scope.render(text = this@buildAnnotatedString) }
    }
  }
}

// todo: rename to MarkdownRenderScope.
interface MarkdownRendererScope {
  val theme: WysiwygTheme
  val unstyledText: AnnotatedString

  fun AnnotatedString.Builder.addStyle(style: SpanStyle, range: TextRange) {
    addStyle(
      style = style,
      start = range.start.coerceAtMost(unstyledText.lastIndex),
      end = range.end.coerceAtMost(length),
    )
  }

  fun AnnotatedString.Builder.addStyle(style: ParagraphStyle, range: TextRange) {
    addStyle(
      style = style,
      start = range.start.coerceAtMost(unstyledText.lastIndex),
      end = range.end.coerceAtMost(length),
    )
    // Compose UI adds a lot of vertical paddings around paragraphs.
    // Reduce the font size of line breaks to make them smaller.
    // https://issuetracker.google.com/u/1/issues/241426911
    if (unstyledText.getOrNull(range.start - 1) == '\n') {
      addStyle(SpanStyle(fontSize = 1.sp), start = range.start - 1, end = range.start)
    }
    if (unstyledText.getOrNull(range.end) == '\n') {
      addStyle(
        SpanStyle(fontSize = 1.sp),
        start = range.end - 1,
        end = range.end + 1,
      )
    }
  }
}
