package me.saket.wysiwyg.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.HorizontalScrollView
import androidx.appcompat.app.AppCompatActivity
import me.saket.wysiwyg.R

class TextFormatToolbarView(context: Context, attrs: AttributeSet) : HorizontalScrollView(context, attrs) {

  lateinit var editorEditText: EditText

  init {
    LayoutInflater.from(context).inflate(R.layout.custom_text_formatting_toolbar, this, true)

    val actions = mapOf(
        R.id.textformattoolbar_insert_link to this::onClickInsertLink,
        R.id.textformattoolbar_bold to this::onClickBold,
        R.id.textformattoolbar_italic to this::onClickItalic,
        R.id.textformattoolbar_strikethrough to this::onClickStrikeThrough,
        R.id.textformattoolbar_quote to this::onClickQuote,
        R.id.textformattoolbar_inline_code to this::onClickInlineCode,
        R.id.textformattoolbar_header to this::onClickHeader
    )

    actions.entries.forEach { (buttonId, clickListener) ->
      findViewById<View>(buttonId).setOnClickListener {
        clickListener()
      }
    }
  }

  private fun onClickInsertLink() {
    // preFilledTitle will be empty when there's no text selected.
    val selectionStart = Math.min(editorEditText.selectionStart, editorEditText.selectionEnd)
    val selectionEnd = Math.max(editorEditText.selectionStart, editorEditText.selectionEnd)
    val preFilledTitle = editorEditText.text.subSequence(selectionStart, selectionEnd)
    AddLinkDialog.showPreFilled((context as AppCompatActivity).supportFragmentManager, preFilledTitle.toString())
  }

  private fun onClickBold() {
    insertMarkdownSyntax(MarkdownBlock.BOLD)
  }

  private fun onClickItalic() {
    insertMarkdownSyntax(MarkdownBlock.ITALIC)
  }

  private fun onClickStrikeThrough() {
    insertMarkdownSyntax(MarkdownBlock.STRIKE_THROUGH)
  }

  private fun onClickQuote() {
    insertQuoteOrHeadingMarkdownSyntax(MarkdownBlock.QUOTE)
  }

  private fun onClickInlineCode() {
    insertMarkdownSyntax(MarkdownBlock.INLINE_CODE)
  }

  private fun onClickHeader() {
    insertQuoteOrHeadingMarkdownSyntax(MarkdownBlock.HEADING)
  }

  /**
   * Insert '>' or '#' at the starting of the line and delete extra space when nesting.
   */
  private fun insertQuoteOrHeadingMarkdownSyntax(markdownBlock: MarkdownBlock) {
    val syntax = markdownBlock.prefix[0]   // '>' or '#'.

    // To keep things simple, we'll always insert the quote at the beginning.
    val layout = editorEditText.layout
    val text = editorEditText.text
    val currentLineIndex = layout.getLineForOffset(editorEditText.selectionStart)
    val textOffsetOfCurrentLine = layout.getLineStart(currentLineIndex)

    val currentLine = text.subSequence(textOffsetOfCurrentLine, text.length)
    val isCurrentLineNonEmpty = currentLine.isNotEmpty()
    val isNestingSyntax = isCurrentLineNonEmpty && currentLine[0] == syntax

    val selectionStartCopy = editorEditText.selectionStart
    val selectionEndCopy = editorEditText.selectionEnd

    editorEditText.setSelection(textOffsetOfCurrentLine)
    insertMarkdownSyntax(markdownBlock)
    val quoteSyntaxLength = markdownBlock.prefix.length
    editorEditText.setSelection(selectionStartCopy + quoteSyntaxLength, selectionEndCopy + quoteSyntaxLength)

    // Next, delete extra spaces between nested quotes/heading.
    if (isNestingSyntax) {
      text.delete(textOffsetOfCurrentLine + 1, textOffsetOfCurrentLine + 2)
    }
  }

  private fun insertMarkdownSyntax(markdownBlock: MarkdownBlock) {
    val isSomeTextSelected = editorEditText.selectionStart != editorEditText.selectionEnd
    val text = editorEditText.text

    if (isSomeTextSelected) {
      val selectionStart = editorEditText.selectionStart
      val selectionEnd = editorEditText.selectionEnd

      text.insert(selectionStart, markdownBlock.prefix)
      text.insert(selectionEnd + markdownBlock.prefix.length, markdownBlock.suffix)
      editorEditText.setSelection(selectionStart + markdownBlock.prefix.length, selectionEnd + markdownBlock.prefix.length)

    } else {
      text.insert(editorEditText.selectionStart, markdownBlock.prefix + markdownBlock.suffix)
      editorEditText.setSelection(editorEditText.selectionStart - markdownBlock.suffix.length)
    }
  }
}
