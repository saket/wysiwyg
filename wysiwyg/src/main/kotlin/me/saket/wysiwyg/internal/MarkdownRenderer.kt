package me.saket.wysiwyg.internal

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.parser.MarkdownSpan
import me.saket.wysiwyg.parser.MarkdownSpanToken.BlockQuote
import me.saket.wysiwyg.parser.MarkdownSpanToken.Bold
import me.saket.wysiwyg.parser.MarkdownSpanToken.FencedCodeBlock
import me.saket.wysiwyg.parser.MarkdownSpanToken.Heading
import me.saket.wysiwyg.parser.MarkdownSpanToken.InlineCode
import me.saket.wysiwyg.parser.MarkdownSpanToken.Italic
import me.saket.wysiwyg.parser.MarkdownSpanToken.LinkText
import me.saket.wysiwyg.parser.MarkdownSpanToken.LinkUrl
import me.saket.wysiwyg.parser.MarkdownSpanToken.ListBlock
import me.saket.wysiwyg.parser.MarkdownSpanToken.StrikeThrough
import me.saket.wysiwyg.parser.MarkdownSpanToken.SyntaxColor

@JvmInline
internal value class MarkdownRenderer(
  private val theme: WysiwygTheme
) {
  fun buildAnnotatedString(text: AnnotatedString, spans: List<MarkdownSpan>): AnnotatedString {
    return buildAnnotatedString {
      append(text)
      spans.fastForEach { span ->
        addSyntaxStyle(span, text)
      }
    }
  }

  private fun AnnotatedString.Builder.addSyntaxStyle(span: MarkdownSpan, text: AnnotatedString) {
    fun addSpanStyle(style: SpanStyle) {
      addStyle(
        style = style,
        start = span.startIndex.coerceAtMost(length - 1),
        end = span.endIndexExclusive.coerceAtMost(length)
      )
    }

    fun addParagraphStyle(style: ParagraphStyle, reduceVerticalPadding: Boolean) {
      addStyle(
        style = style,
        start = span.startIndex.coerceAtMost(length - 1),
        end = span.endIndexExclusive.coerceAtMost(length)
      )
      // Compose UI adds a lot of vertical paddings around paragraphs.
      // Reduce the font size of line breaks to make them smaller.
      // https://issuetracker.google.com/u/1/issues/241426911
      if (reduceVerticalPadding) {
        if (text.getOrNull(span.startIndex - 1) == '\n') {
          addStyle(SpanStyle(fontSize = 1.sp), start = span.startIndex - 1, end = span.startIndex)
        }
        if (text.getOrNull(span.endIndexExclusive) == '\n') {
          addStyle(SpanStyle(fontSize = 1.sp), start = span.endIndexExclusive - 1, end = span.endIndexExclusive + 1)
        }
      }
    }

    when (span.token) {
      Bold -> addSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
      Italic -> addSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
      SyntaxColor -> addSpanStyle(SpanStyle(color = theme.syntaxColor))
      StrikeThrough -> addSpanStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
      LinkText -> addSpanStyle(SpanStyle(color = theme.linkTextColor))
      LinkUrl -> addSpanStyle(SpanStyle(color = theme.linkUrlColor))
      InlineCode -> {
        addSpanStyle(
          SpanStyle(
            background = theme.codeBackground,
            fontFamily = FontFamily.Monospace,
          )
        )
      }
      FencedCodeBlock -> {
        addSpanStyle(
          SpanStyle(
            background = theme.codeBackground,
            fontFamily = FontFamily.Monospace,
          )
        )
        addParagraphStyle(
          style = ParagraphStyle(
            textIndent = TextIndent(
              firstLine = theme.codeBlockLeadingPadding,
              restLine = theme.codeBlockLeadingPadding
            )
          ),
          reduceVerticalPadding = false,
        )
      }
      BlockQuote -> {
        addSpanStyle(SpanStyle(color = theme.blockQuoteText))
        addParagraphStyle(
          style = ParagraphStyle(
            textIndent = TextIndent(
              firstLine = theme.blockQuoteLeadingPadding,
              restLine = theme.blockQuoteLeadingPadding
            )
          ),
          reduceVerticalPadding = true,
        )
      }
      ListBlock -> {
        addParagraphStyle(
          style = ParagraphStyle(
            textIndent = TextIndent(
              firstLine = theme.listBlockLeadingPadding,
              restLine = theme.listBlockLeadingPadding
            )
          ),
          reduceVerticalPadding = true,
        )
      }
      is Heading -> {
        addSpanStyle(
          SpanStyle(
            fontSize = 1.em * theme.headingFontSizes.forLevel(span.token.level),
            fontWeight = FontWeight.Bold
          )
        )
      }
    }
  }
}

private fun WysiwygTheme.HeadingFontSizeMultipliers.forLevel(level: Int): Float {
  return when (level) {
    1 -> h1
    2 -> h2
    3 -> h3
    4 -> h4
    5 -> h5
    6 -> h6
    else -> error("nope")
  }
}
