package me.saket.wysiwyg.format

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange

data class TextReplacement(
  val text: CharSequence,
  val newSelection: TextRange,
) {
  constructor(
    text: CharSequence,
    newCursorPosition: Int,
  ) : this(
    text = text,
    newSelection = TextRange(newCursorPosition),
  )
}

fun interface TextReplacement2 {
  fun TextFieldBuffer.replace()
}
