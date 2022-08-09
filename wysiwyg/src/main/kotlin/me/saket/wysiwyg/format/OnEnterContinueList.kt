package me.saket.wysiwyg.format

import kotlin.LazyThreadSafetyMode.NONE

/**
 * Inserts a blank list item on the next line when enter key is pressed at the
 * end of an ordered/unordered list. Preserves leading margin for nested lists.
 * Ends list blocks when enter is pressed again on a blank list item.
 *
 * Before:
 * ```
 * 1. Mango
 * 2. Apple▮
 * ```
 *
 * On enter:
 * ```
 * 1. Mango
 * 2. Apple
 * 3. ▮
 * ```
 */
class OnEnterContinueList(
  private val itemMarkers: String = "*+-"
) : OnEnterMarkdownFormatter {
  private val orderedItemRegex by lazy(NONE) { Regex("(\\d+)\\.\\s") }

  override fun onEnterPressed(
    text: CharSequence,
    paragraph: TextParagraph,
    cursorPositionBeforeEnter: Int
  ): TextReplacement? {
    val paragraphMargin = paragraph.text.toString().takeWhile { it.isWhitespace() }
    val paragraphText = paragraph.text.trimStart()

    // Unordered list item.
    if (paragraphText.length >= 2 && paragraphText[0] in itemMarkers && paragraphText[1].isWhitespace()) {
      val isItemEmpty = paragraphText.length == 2
      return if (isItemEmpty) {
        endListSyntax(
          text = text,
          lastItem = paragraph,
          cursorPositionBeforeEnter = cursorPositionBeforeEnter
        )
      } else {
        continueListSyntax(
          text = text,
          cursorPositionBeforeEnter = cursorPositionBeforeEnter,
          paragraphLeadingMargin = paragraphMargin,
          syntax = "${paragraphText[0]} "
        )
      }
    }

    // Ordered list item.
    if (paragraphText[0].isDigit()) {
      val matchResult = orderedItemRegex.find(paragraphText)
      if (matchResult != null) {
        val (syntax, number) = matchResult.groupValues
        val isItemEmpty = paragraphText.length == syntax.length

        return if (isItemEmpty) {
          endListSyntax(
            text = text,
            cursorPositionBeforeEnter = cursorPositionBeforeEnter,
            lastItem = paragraph
          )
        } else {
          val nextNumber = number.toInt() + 1
          continueListSyntax(
            text = text,
            cursorPositionBeforeEnter = cursorPositionBeforeEnter,
            paragraphLeadingMargin = paragraphMargin, syntax = "$nextNumber. ")
        }
      }
    }

    return null
  }

  private fun endListSyntax(
    text: CharSequence,
    cursorPositionBeforeEnter: Int,
    lastItem: TextParagraph
  ): TextReplacement {
    return TextReplacement(
      text = text.replaceRange(
        startIndex = lastItem.startIndex,
        endIndex = lastItem.endIndexExclusive,
        replacement = "\n"
      ),
      newCursorPosition = cursorPositionBeforeEnter - lastItem.text.length + 1  // +1 for new line.
    )
  }

  private fun continueListSyntax(
    text: CharSequence,
    cursorPositionBeforeEnter: Int,
    paragraphLeadingMargin: String,
    syntax: String
  ): TextReplacement {
    val syntaxWithLineBreak = "\n$paragraphLeadingMargin$syntax"
    return TextReplacement(
      text = text.replaceRange(
        startIndex = cursorPositionBeforeEnter,
        endIndex = cursorPositionBeforeEnter,
        replacement = syntaxWithLineBreak
      ),
      newCursorPosition = cursorPositionBeforeEnter + syntaxWithLineBreak.length
    )
  }
}
