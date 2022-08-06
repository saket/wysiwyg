package me.saket.wysiwyg.parser

import androidx.compose.ui.text.input.TextFieldValue

interface MarkdownParser {
  fun parse(text: String): ParseResult

  /**
   * Before the parser gets a chance to reparse the tree, manually reapply the spans by
   * calculating their new positions to prevent a flicker on key stroke. The implementation
   * does not need to be perfect because the spans will be corrected in a few milliseconds.
   * A better implementation would use something like tree-sitter.
   */
  fun offsetSpansOnTextChange(
    newValue: TextFieldValue,
    previousValue: TextFieldValue,
    previousSpans: List<MarkdownSpan>
  ): List<MarkdownSpan>

  @JvmInline
  value class ParseResult(
    val spans: List<MarkdownSpan>
  )
}

data class MarkdownSpan(
  val token: MarkdownSpanToken,
  val startIndex: Int,
  val endIndexExclusive: Int
) {
  val endIndexInclusive: Int get() = endIndexExclusive - 1

  init {
    check(endIndexExclusive > startIndex) { "Invalid offsets for $this" }
  }
}

sealed interface MarkdownSpanToken {
  object SyntaxColor : MarkdownSpanToken
  object Bold : MarkdownSpanToken
  object Italic : MarkdownSpanToken
  object StrikeThrough : MarkdownSpanToken
  object LinkText : MarkdownSpanToken
  object LinkUrl : MarkdownSpanToken
  object InlineCode : MarkdownSpanToken
  object FencedCodeBlock : MarkdownSpanToken
  object BlockQuote : MarkdownSpanToken
  object ListBlock : MarkdownSpanToken

  data class Heading(val level: Int) : MarkdownSpanToken {
    init {
      check(level in 0..6)
    }
  }
}
