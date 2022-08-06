package me.saket.wysiwyg.format

data class TextParagraph(
  val text: CharSequence,
  val startIndex: Int,
  val endIndexExclusive: Int,
) {
  companion object
}
