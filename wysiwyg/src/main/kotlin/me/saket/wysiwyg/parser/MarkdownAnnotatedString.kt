package me.saket.wysiwyg.parser

import androidx.compose.ui.text.AnnotatedString
import dev.drewhamilton.poko.Poko
import me.saket.wysiwyg.MarkdownSpanPainter

// todo: kdoc
@Poko
class MarkdownAnnotatedString(
  val text: AnnotatedString,
  val extraSpanPainters: List<MarkdownSpanPainter>,
)
