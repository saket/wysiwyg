package me.saket.wysiwyg.format

import kotlin.LazyThreadSafetyMode.NONE

/**
 * Inserts a blank list item on the next line when enter key is pressed at the
 * end of an ordered/unordered list. Preserves leading margin for nested lists.
 * Ends list blocks when enter is pressed again on a blank list item.
 */
class OnEnterContinueList : OnEnterMarkdownFormatter {

  override fun onEnterPressed(
    text: CharSequence,
    paragraph: TextParagraph,
    cursorPositionBeforeEnter: Int,
  ): TextReplacement2? {
    val paragraphString = paragraph.text.toString()
    val paragraphText = paragraphString.trimStart()
    fun paragraphMargin() = paragraphString.takeWhile { it.isWhitespace() }

    if (paragraphText.length >= 2 && paragraphText[0] in itemMarkers && paragraphText[1].isWhitespace()) {
      val isItemEmpty = paragraphText.length == 2
      return if (isItemEmpty) {
        endListSyntax(
          lastItem = paragraph,
          cursorPositionBeforeEnter = cursorPositionBeforeEnter,
        )
      } else {
        continueListSyntax(
          cursorPositionBeforeEnter = cursorPositionBeforeEnter,
          paragraphLeadingMargin = paragraphMargin(),
          syntax = "${paragraphText[0]} ",
        )
      }
    }

    if (paragraphText[0].isDigit()) {
      val matchResult = orderedItemRegex.find(paragraphText)
      if (matchResult != null) {
        val (syntax, number) = matchResult.groupValues
        val isItemEmpty = paragraphText.length == syntax.length

        return if (isItemEmpty) {
          endListSyntax(
            cursorPositionBeforeEnter = cursorPositionBeforeEnter,
            lastItem = paragraph,
          )
        } else {
          val nextNumber = number.toInt() + 1
          continueListSyntax(
            cursorPositionBeforeEnter = cursorPositionBeforeEnter,
            paragraphLeadingMargin = paragraphMargin(),
            syntax = "$nextNumber. ",
          )
        }
      }
    }

    return null
  }

  private fun endListSyntax(
    cursorPositionBeforeEnter: Int,
    lastItem: TextParagraph,
  ): TextReplacement2 {
    // Eat the empty list marker and the user's just-typed newline; leave a
    // single newline so the cursor lands on a blank line below the list.
    return TextReplacement2 {
      replace(
        start = lastItem.startIndex,
        end = cursorPositionBeforeEnter + 1,  // +1 for new line.
        text = "\n",
    )
  }
  }

  private fun continueListSyntax(
    cursorPositionBeforeEnter: Int,
    paragraphLeadingMargin: String,
    syntax: String,
  ): TextReplacement2 {
    // Insert the next list item's prefix right after the user's typed newline.
    val insertAt = cursorPositionBeforeEnter + 1
    return TextReplacement2 {
      replace(insertAt, insertAt, "$paragraphLeadingMargin$syntax")
    }
  }

  companion object {
    private const val itemMarkers = "*+-"
    private val orderedItemRegex by lazy(NONE) { Regex("(\\d+)\\.\\s") }
  }
}
