package me.saket.wysiwyg.format

import androidx.compose.foundation.text.input.TextFieldBuffer

fun interface TextReplacement {
  fun TextFieldBuffer.replace()
}
