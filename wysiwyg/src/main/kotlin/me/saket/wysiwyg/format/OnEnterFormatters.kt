package me.saket.wysiwyg.format

import androidx.compose.ui.text.input.TextFieldValue

@JvmInline
internal value class OnEnterFormatters(
  private val formatters: List<OnEnterFormatter>
) {
  fun formatIfEnterWasPressed(previousText: TextFieldValue, newText: TextFieldValue): TextFieldValue {
    runCatching {
      val wasEnterPressed = previousText.selection.collapsed
        && newText.text.length - previousText.text.length == 1
        && newText.text.getOrNull(newText.selection.start - 1) == '\n'

      if (wasEnterPressed) {
        val replacement = onEnterPressed(textBeforeEnter = previousText)
        if (replacement != null) {
          return newText.copy(
            text = replacement.text.toString(),
            selection = replacement.newSelection
          )
        }
      }
    }

    return newText
  }

  fun onEnterPressed(textBeforeEnter: TextFieldValue): TextReplacement? {
    if (!textBeforeEnter.selection.collapsed) {
      // Some text was selected.
      return null
    }

    val editedParagraph = TextParagraph.findUnderCursor(textBeforeEnter)
    if (editedParagraph.text.isBlank()) {
      return null
    }

    return formatters.firstNotNullOfOrNull {
      it.onEnterPressed(
        text = textBeforeEnter.text,
        paragraph = editedParagraph,
        cursorPositionBeforeEnter = textBeforeEnter.selection.start
      )
    }
  }
}

/**
 * Note for self: unlike functions in [String], this does not return
 * -1 for empty paragraphs. See [FindTextParagraphTest].
 */
internal fun TextParagraph.Companion.findUnderCursor(value: TextFieldValue): TextParagraph {
  val text = value.text

  // Begin with the assumption that this is the first paragraph
  var startOffset = 0
  for (i in value.selection.min downTo 0) {
    if (i > 0 && text[i - 1] == '\n') {
      startOffset = i
      break
    }
  }

  // Begin with the assumption that this is the last paragraph.
  var endOffsetExclusive = text.length
  for (i in value.selection.max until text.length) {
    if (text[i] == '\n') {
      endOffsetExclusive = i
      break
    }
  }

  return TextParagraph(
    text = text.substring(startOffset, endOffsetExclusive),
    startIndex = startOffset,
    endIndexExclusive = endOffsetExclusive
  )
}
