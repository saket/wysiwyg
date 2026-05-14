package me.saket.wysiwyg.format

import dev.drewhamilton.poko.Poko

@Poko
class TextParagraph(
  val text: CharSequence,
  val startIndex: Int,
  val endIndexExclusive: Int,
) {
  companion object
}
