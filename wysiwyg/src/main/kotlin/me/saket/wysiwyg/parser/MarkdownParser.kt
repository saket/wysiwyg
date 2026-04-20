package me.saket.wysiwyg.parser

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import kotlinx.coroutines.flow.Flow
import me.saket.wysiwyg.SpanTextRange
import me.saket.wysiwyg.internal.MarkdownRendererScope

interface MarkdownParser {
  /**
   * Produces markdown spans for [text]. The returned flow may emit multiple times:
   * an initial synchronous approximate result (for flicker-free rendering while a full parse
   * runs), followed by the final correct result. Parsers that can't produce an interim
   * result emit once.
   *
   * @param changes describes what changed since the previous call. Incremental parsers use
   * this to skip re-diffing the text; non-incremental parsers ignore it. Pass
   * [ChangeListSnapshot.Empty] for the initial parse or when edit info isn't available.
   */
  fun parse(text: String, changes: ChangeListSnapshot): Flow<ParseResult>

  @JvmInline
  value class ParseResult(
    val spans: List<MarkdownSpan>,
  )
}

/**
 * An immutable version of Compose's [TextFieldBuffer.ChangeList] that outlives the transformation
 * callback in which it was produced, safe to read from any thread.
 */
@JvmInline
value class ChangeListSnapshot(val changes: List<Change>) {
  /**
   * A single edit region. [range] is the affected region in the new text; [originalRange] is
   * the region in the previous text that was replaced.
   */
  data class Change(
    val range: TextRange,
    val originalRange: TextRange,
  )

  companion object {
    val Empty = ChangeListSnapshot(emptyList())

    fun from(changes: TextFieldBuffer.ChangeList): ChangeListSnapshot {
      if (changes.changeCount == 0) return Empty
      val copy = ArrayList<Change>(changes.changeCount)
      for (i in 0 until changes.changeCount) {
        copy += Change(
          range = changes.getRange(i),
          originalRange = changes.getOriginalRange(i),
        )
      }
      return ChangeListSnapshot(changes = copy)
    }
  }
}

/**
 * Holds styling information for a portion of text that was formatted with markdown.
 *
 * Spans are designed to be as granular as possible. For example, when a link markdown is
 * detected, instead of generating one span for representing the entire link, two spans
 * are generated -- one for coloring the link text and one for coloring the link url.
 */
data class MarkdownSpan(
  val style: MarkdownSpanStyle,
  val range: SpanTextRange,
)

/** Represents a text appearance. */
abstract class MarkdownSpanStyle {
  abstract fun MarkdownRendererScope.render(
    text: AnnotatedString.Builder,
    range: SpanTextRange
  )
}

object MarkerColorSpanStyle : MarkdownSpanStyle() {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = range,
    )
  }
}

object BoldSpanStyle : MarkdownSpanStyle() {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(fontWeight = FontWeight.Bold),
      range = range,
    )
  }
}

object ItalicSpanStyle : MarkdownSpanStyle() {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(fontStyle = FontStyle.Italic),
      range = range,
    )
  }
}

object StrikeThroughSpanStyle : MarkdownSpanStyle() {
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

object LinkTextSpanStyle : MarkdownSpanStyle() {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(color = theme.linkTextColor),
      range = range,
    )
  }
}

object LinkUrlSpanStyle : MarkdownSpanStyle() {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStyle(
      style = SpanStyle(color = theme.linkUrlColor),
      range = range,
    )
  }
}

object InlineCodeSpanStyle : MarkdownSpanStyle() {
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

object FencedCodeBlockSpanStyle : MarkdownSpanStyle() {
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

object BlockQuoteBodySpanStyle : MarkdownSpanStyle() {
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

object BlockQuoteParagraphLineSpanStyle : MarkdownSpanStyle() {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStringAnnotation(
      tag = "blockquote",
      annotation = "ignored",
      start = range.startIndex,
      end = range.endIndexExclusive
    )
  }
}

object ListBlockSpanStyle : MarkdownSpanStyle() {
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

data class HeadingSpanStyle(private val level: Int) : MarkdownSpanStyle() {
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

object ThematicBreakSpanStyle : MarkdownSpanStyle() {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: SpanTextRange) {
    text.addStringAnnotation(
      tag = "thematic_break",
      annotation = "ignored",
      start = range.startIndex,
      end = range.endIndexExclusive
    )
  }
}
