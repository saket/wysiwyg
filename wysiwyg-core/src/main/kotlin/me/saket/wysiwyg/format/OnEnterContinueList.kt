package me.saket.wysiwyg.format

import androidx.compose.ui.util.fastAny
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
  ): TextReplacement? {
    val paragraphText = paragraph.text
    val paragraphLength = paragraphText.length

    val contentStart = paragraphText.indexOfFirst { !it.isWhitespace() }
    if (contentStart == -1) {
      return null
    }

    if (paragraphText[contentStart] in itemMarkers
      && contentStart + 1 < paragraphLength
      && paragraphText[contentStart + 1].isWhitespace()
    ) {
      val body = paragraphText.substring(contentStart + 2)
      val isTaskItem = taskMarkers.fastAny { body.startsWith(it) }
      val syntax = buildString {
        append(paragraphText[contentStart])
        append(' ')
        if (isTaskItem) {
          append("[ ] ")
        }
      }
      val isItemEmpty = paragraphLength == contentStart + syntax.length
      return if (isItemEmpty) {
        endListSyntax(
          lastItem = paragraph,
          cursorPositionBeforeEnter = cursorPositionBeforeEnter,
        )
      } else {
        continueListSyntax(
          cursorPositionBeforeEnter = cursorPositionBeforeEnter,
          paragraphLeadingMargin = paragraphText.substring(0, contentStart),
          syntax = syntax,
        )
      }
    }

    if (paragraphText[contentStart].isDigit()) {
      val matchResult = orderedItemRegex.find(paragraphText, startIndex = contentStart)
      if (matchResult != null) {
        val (syntax, number) = matchResult.groupValues
        val isItemEmpty = paragraphLength - contentStart == syntax.length

        return if (isItemEmpty) {
          endListSyntax(
            cursorPositionBeforeEnter = cursorPositionBeforeEnter,
            lastItem = paragraph,
          )
        } else {
          val nextNumber = number.toInt() + 1
          continueListSyntax(
            cursorPositionBeforeEnter = cursorPositionBeforeEnter,
            paragraphLeadingMargin = paragraphText.substring(0, contentStart),
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
  ): TextReplacement {
    // Eat the empty list marker and the user's just-typed newline; leave a
    // single newline so the cursor lands on a blank line below the list.
    return TextReplacement {
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
  ): TextReplacement {
    // Insert the next list item's prefix right after the user's typed newline.
    val insertAt = cursorPositionBeforeEnter + 1
    return TextReplacement {
      replace(insertAt, insertAt, "$paragraphLeadingMargin$syntax")
    }
  }

  companion object {
    private const val itemMarkers = "*+-"
    private val taskMarkers = listOf("[ ] ", "[x] ", "[X] ")
    private val orderedItemRegex by lazy(NONE) { Regex("(\\d+)\\.\\s") }
  }
}
