package me.saket.wysiwyg.format

interface OnEnterMarkdownFormatter {
  fun onEnterPressed(
    text: CharSequence,
    paragraph: TextParagraph,
    cursorPositionBeforeEnter: Int,
  ): TextReplacement?
}
