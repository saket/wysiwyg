package me.saket.wysiwyg.format

interface OnEnterMarkdownFormatter {
  /** Returns a [TextReplacement2] to apply, or `null` to let the next formatter try. */
  fun onEnterPressed(
    text: CharSequence,
    paragraph: TextParagraph,
    cursorPositionBeforeEnter: Int,
  ): TextReplacement2?
}
