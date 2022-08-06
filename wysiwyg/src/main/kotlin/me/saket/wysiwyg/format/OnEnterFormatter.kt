package me.saket.wysiwyg.format

interface OnEnterFormatter {
  fun onEnterPressed(
    text: CharSequence,
    paragraph: TextParagraph,
    cursorPositionBeforeEnter: Int,
  ): TextReplacement?
}
