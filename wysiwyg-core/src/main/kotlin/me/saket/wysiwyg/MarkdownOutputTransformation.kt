package me.saket.wysiwyg

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer

// todo: remove this from the public API if its empty?
interface MarkdownOutputTransformation : OutputTransformation {
  // todo: kdoc.
  //val styleBuffer: MarkdownStyleBuffer
}
