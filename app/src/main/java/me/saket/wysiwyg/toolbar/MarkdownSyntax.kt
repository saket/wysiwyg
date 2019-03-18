package me.saket.wysiwyg.toolbar

import android.widget.EditText

sealed class MarkdownSyntax {
  abstract fun insert(editText: EditText)
}

sealed class SymmetricMarkdownSyntax(val prefix: String, val suffix: String = prefix) : MarkdownSyntax() {
  override fun insert(editText: EditText) {
    val isSomeTextSelected = editText.selectionStart != editText.selectionEnd
    val text = editText.text

    if (isSomeTextSelected) {
      val selectionStart = editText.selectionStart
      val selectionEnd = editText.selectionEnd

      text.insert(selectionStart, prefix)
      text.insert(selectionEnd + prefix.length, suffix)
      editText.setSelection(selectionStart + prefix.length, selectionEnd + prefix.length)

    } else {
      text.insert(editText.selectionStart, prefix + suffix)
      editText.setSelection(editText.selectionStart - suffix.length)
    }
  }
}

sealed class CompoundableMarkdownSyntax(prefix: String) : SymmetricMarkdownSyntax(prefix, suffix = "") {
  /**
   * Insert '>' or '#' at the starting of the line and delete extra space when nesting.
   */
  override fun insert(editText: EditText) {
    val syntax = prefix[0]   // '>' or '#'.

    val layout = editText.layout
    val text = editText.text
    val currentLineIndex = layout.getLineForOffset(editText.selectionStart)
    val textOffsetOfCurrentLine = layout.getLineStart(currentLineIndex)

    val currentLine = text.subSequence(textOffsetOfCurrentLine, text.length)
    val isCurrentLineNonEmpty = currentLine.isNotEmpty()
    val isNestingSyntax = isCurrentLineNonEmpty && currentLine[0] == syntax

    var textOffsetOfCompoundedSyntax = textOffsetOfCurrentLine

    // Find the end of this compounded syntax so that another '>' or '#' can be inserted.
    if (isNestingSyntax) {
      while (text[textOffsetOfCompoundedSyntax] == syntax) {
        ++textOffsetOfCompoundedSyntax
      }
    }

    val selectionStartCopy = editText.selectionStart
    val selectionEndCopy = editText.selectionEnd

    editText.setSelection(textOffsetOfCompoundedSyntax)
    super.insert(editText)
    val quoteSyntaxLength = prefix.length
    editText.setSelection(selectionStartCopy + quoteSyntaxLength, selectionEndCopy + quoteSyntaxLength)

    // Next, delete extra spaces between nested quotes/heading.
    if (isNestingSyntax) {
      text.delete(textOffsetOfCompoundedSyntax + 1, textOffsetOfCompoundedSyntax + 2)
    }
  }
}

data class Link(val title: String, val url: String) : MarkdownSyntax() {
  override fun insert(editText: EditText) {
    val selectionStart = Math.min(editText.selectionStart, editText.selectionEnd)
    val selectionEnd = Math.max(editText.selectionStart, editText.selectionEnd)

    val linkMarkdown = when {
      title.isEmpty() -> url
      else -> String.format("[%s](%s)", title, url)
    }
    editText.text.replace(selectionStart, selectionEnd, linkMarkdown)

    val linkEnd = selectionStart + linkMarkdown.length
    editText.setSelection(linkEnd, linkEnd)
  }
}

object ThematicBreak : MarkdownSyntax() {
  override fun insert(editText: EditText) {
    val layout = editText.layout
    val text = editText.text
    val currentLineIndex = layout.getLineForOffset(editText.selectionStart)
    val textOffsetOfCurrentLine = layout.getLineStart(currentLineIndex)

    val currentLine = text.subSequence(textOffsetOfCurrentLine, text.length)
    val isAtStartOfCurrentLine = currentLine.isEmpty()

    val lineSyntax = when {
      isAtStartOfCurrentLine.not() -> "\n---\n"
      else -> "---\n"
    }
    editText.text.insert(editText.selectionEnd, lineSyntax)
  }
}

object Bold : SymmetricMarkdownSyntax("**")
object Italic : SymmetricMarkdownSyntax("*")
object StrikeThrough : SymmetricMarkdownSyntax("~~")
object InlineCode : SymmetricMarkdownSyntax("`")
object Quote : CompoundableMarkdownSyntax("> ")
object Heading : CompoundableMarkdownSyntax("# ")
