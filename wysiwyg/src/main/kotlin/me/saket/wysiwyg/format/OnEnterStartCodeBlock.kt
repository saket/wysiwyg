package me.saket.wysiwyg.format

import androidx.compose.ui.text.TextRange
import kotlin.LazyThreadSafetyMode.NONE

/** Starts a code block when enter key is pressed after 3 backticks. */
object OnEnterStartCodeBlock : OnEnterMarkdownFormatter {
  private val fencedCodeRegex by lazy(NONE) { Regex("```[a-z]*[\\s\\S]*?```") }

  override fun onEnterPressed(
    text: CharSequence,
    paragraph: TextParagraph,
    cursorPositionBeforeEnter: Int,
  ): TextReplacement2? {
    if (!paragraph.text.startsWith("```")) {
      return null
    }

    val allCodeBlocks = fencedCodeRegex.findAll(text)
    for (block in allCodeBlocks) {
      if (block.range.contains(cursorPositionBeforeEnter)) {
        return null
      }

      val enterPressedOnClosingLine = paragraph.startIndex < block.range.last
        && cursorPositionBeforeEnter <= paragraph.endIndexExclusive

      if (enterPressedOnClosingLine) {
        return null
      }
    }

    // Insert the closing fence after the user's just-typed newline.
    val insertAt = cursorPositionBeforeEnter + 1
    return TextReplacement2 {
      replace(insertAt, insertAt, "\n```")
      selection = TextRange(insertAt)
    }
  }
}
