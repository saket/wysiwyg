package me.saket.wysiwyg

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import me.saket.wysiwyg.parser.MarkdownAnnotatedString

interface MarkdownOutputTransformation : OutputTransformation {
  // todo: kdoc.
  val lastRenderResult: MarkdownAnnotatedString?

  companion object {
    val Empty: MarkdownOutputTransformation = object : MarkdownOutputTransformation {
      override val lastRenderResult: MarkdownAnnotatedString? = null
      override fun TextFieldBuffer.transformOutput() = Unit
    }
  }
}
