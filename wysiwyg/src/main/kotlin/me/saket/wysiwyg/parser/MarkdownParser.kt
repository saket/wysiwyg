package me.saket.wysiwyg.parser

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import me.saket.wysiwyg.SpanTextRange
import me.saket.wysiwyg.internal.MarkdownRendererScope

interface MarkdownParser {
  fun parse(text: String): ParseResult

  @JvmInline
  value class ParseResult(
    val spans: List<MarkdownSpan>
  )

  /**
   * When text is changed, Compose emits a new text value and discards any previously generated span styles.
   * Parsing this new text value will take at least a few milliseconds, enough for the user to see markdown
   * styling flicker on every key stroke.
   *
   * To prevent this, Wysiwyg retains the span styles generated for the previous text value and immediately
   * re-applies them. Ths function manually adjusts their spans to account for text that may have changed
   * between their bounds. For example, if a letter was inserted within a bold span, this function will move
   * all spans after the inserted index by one position.
   *
   * The implementation does not need to be perfect because the new text will be re-parsed in a few milliseconds.
   * It just needs to good enough to give an illusion that the parsing is happening instantly on every key stroke.
   */
  fun offsetSpansOnTextChange(
    newValue: TextFieldValue,
    previousValue: TextFieldValue,
    previousSpans: List<MarkdownSpan>
  ): List<MarkdownSpan>
}

/**
 * Holds styling information for a portion of text that was formatted with markdown.
 *
 * Spans are designed to be as granular as possible. For example, when a link markdown is
 * detected, instead of generating one span for representing the entire link, two spans
 * are generated -- one for coloring the link text and one for coloring the link url.
 *
 * Granularity of spans was needed so that [MarkdownParser.offsetSpansOnTextChange] could
 * individually adjust their bounds as needed.
 */
data class MarkdownSpan(
  val style: MarkdownSpanStyle,
  val range: SpanTextRange,
)

/**
 * Represents a text appearance.
 *
 * @param hasClosingMarker used by [MarkdownParser.offsetSpansOnTextChange] for determining whether
 * this span should be discarded if text is deleted at its last index. It is weird and may not apply
 * to all span styles, but is unavoidable.
 */
abstract class MarkdownSpanStyle(val hasClosingMarker: Boolean) {
  abstract fun MarkdownRendererScope.render(
    text: AnnotatedString.Builder,
    range: SpanTextRange
  )
}

object SyntaxColorSpanStyle : MarkdownSpanStyle(hasClosingMarker = true) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(color = theme.syntaxColor),
      range = range,
    )
  }
}

object BoldSpanStyle : MarkdownSpanStyle(hasClosingMarker = true) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(fontWeight = FontWeight.Bold),
      range = range,
    )
  }
}

object ItalicSpanStyle : MarkdownSpanStyle(hasClosingMarker = true) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(fontStyle = FontStyle.Italic),
      range = range,
    )
  }
}

object StrikeThroughSpanStyle : MarkdownSpanStyle(hasClosingMarker = true) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(
        textDecoration = TextDecoration.LineThrough,
        color = theme.struckThroughTextColor,
      ),
      range = range,
    )
  }
}

object LinkTextSpanStyle : MarkdownSpanStyle(hasClosingMarker = true) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(color = theme.linkTextColor),
      range = range,
    )
  }
}

object LinkUrlSpanStyle : MarkdownSpanStyle(hasClosingMarker = true) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(color = theme.linkUrlColor),
      range = range,
    )
  }
}

object InlineCodeSpanStyle : MarkdownSpanStyle(hasClosingMarker = true) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(
        background = theme.codeBackground,
        fontFamily = FontFamily.Monospace,
      ),
      range = range
    )
  }
}

object FencedCodeBlockSpanStyle : MarkdownSpanStyle(hasClosingMarker = true) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(
        background = theme.codeBackground,
        fontFamily = FontFamily.Monospace,
      ),
      range = range
    )
    text.addStyle(
      style = ParagraphStyle(
        textIndent = TextIndent(
          firstLine = theme.codeBlockLeadingPadding,
          restLine = theme.codeBlockLeadingPadding
        )
      ),
      range = range,
    )
  }
}

object BlockQuoteSpanStyle : MarkdownSpanStyle(hasClosingMarker = false) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(color = theme.blockQuoteText),
      range = range,
    )
    text.addStyle(
      style = ParagraphStyle(
        textIndent = TextIndent(
          firstLine = theme.blockQuoteLeadingPadding,
          restLine = theme.blockQuoteLeadingPadding
        )
      ),
      range = range,
    )
  }
}

object ListBlockSpanStyle : MarkdownSpanStyle(hasClosingMarker = false) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = ParagraphStyle(
        textIndent = TextIndent(
          firstLine = theme.listBlockLeadingPadding,
          restLine = theme.listBlockLeadingPadding
        )
      ),
      range = range,
    )
  }
}

data class HeadingSpanStyle(private val level: Int) : MarkdownSpanStyle(hasClosingMarker = false) {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    val fontSizeMultiplier = with(theme.headingFontSizes) {
      when (level) {
        1 -> h1
        2 -> h2
        3 -> h3
        4 -> h4
        5 -> h5
        6 -> h6
        else -> error("invalid level: $level")
      }
    }
    text.addStyle(
      style = SpanStyle(
        fontSize = 1.em * fontSizeMultiplier,
        fontWeight = FontWeight.Bold,
        color = theme.headingColor,
      ),
      range = range,
    )
  }
}
