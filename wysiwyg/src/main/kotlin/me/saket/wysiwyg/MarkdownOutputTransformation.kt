package me.saket.wysiwyg

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import me.saket.wysiwyg.internal.MarkdownStyleBuffer

interface MarkdownOutputTransformation : OutputTransformation {
  // todo: kdoc.
  val styleBuffer: MarkdownStyleBuffer

  companion object {
    val Empty: MarkdownOutputTransformation = object : MarkdownOutputTransformation {
      override val styleBuffer: MarkdownStyleBuffer = MarkdownStyleBuffer.Empty
      override fun TextFieldBuffer.transformOutput() = Unit
    }
  }
}
