package me.saket.wysiwyg.format

import kotlin.LazyThreadSafetyMode.NONE

/** Starts a code block when enter key is pressed after 3 backticks. */
object OnEnterStartCodeBlock : OnEnterFormatter {
  val fencedCodeRegex by lazy(NONE) { Regex("(```)[a-z]*[\\s\\S]*?(```)") }

  override fun onEnterPressed(
    text: CharSequence,
    paragraph: TextParagraph,
    cursorPositionBeforeEnter: Int
  ): TextReplacement? {
    if (!paragraph.text.startsWith("```")) {
      return null
    }

    val allCodeBlocks = fencedCodeRegex.findAll(text)
    for (block in allCodeBlocks) {
      // Check if the cursor is already inside a code block.
      if (block.range.contains(cursorPositionBeforeEnter)) {
        return null
      }

      // Check if the cursor is placed after the closing syntax.
      val enterPressedOnClosingLine = paragraph.startIndex < block.range.last
        && cursorPositionBeforeEnter <= paragraph.endIndexExclusive

      if (enterPressedOnClosingLine) {
        // Cursor is on the same line as the closing
        // marker. This isn't a new code block.
        return null
      }
    }

    return TextReplacement(
      text = text.replaceRange(
        startIndex = cursorPositionBeforeEnter,
        endIndex = cursorPositionBeforeEnter,
        replacement = "\n\n```"
      ),
      newCursorPosition = cursorPositionBeforeEnter + 1
    )
  }
}
