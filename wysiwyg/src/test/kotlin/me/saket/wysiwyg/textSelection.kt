package me.saket.wysiwyg

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

fun decodeTextSelection(text: String): TextFieldValue {
  val markerCount = text.count { it == '▮' }
  require(markerCount in 1..2) {
    when (markerCount) {
      0 -> "Text has no cursor markers"
      else -> "Text has >2 ($markerCount) selection markers"
    }
  }

  val selection = when (markerCount) {
    1 -> TextRange(
      text.indexOfFirst { it == '▮' }
    )
    else -> TextRange(
      start = text.indexOfFirst { it == '▮' },
      end = text.indexOfLast { it == '▮' } - 1
    )
  }
  return TextFieldValue(
    text = text.replace("▮", ""),
    selection = selection
  )
}

fun encodeTextSelection(text: CharSequence, selection: TextRange?): CharSequence {
  return when {
    selection == null -> text
    selection.collapsed -> {
      text.substring(0, selection.start) +
        "▮" +
        text.substring(selection.start, text.length)
    }
    else -> {
      text.substring(0, selection.start) +
        ("▮" + text.substring(selection.start, selection.end) + "▮") +
        text.substring(selection.end, text.length)
    }
  }
}
