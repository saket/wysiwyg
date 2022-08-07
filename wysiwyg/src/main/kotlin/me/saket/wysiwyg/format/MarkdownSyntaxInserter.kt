package me.saket.wysiwyg.format

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import me.saket.wysiwyg.Wysiwyg

fun interface MarkdownSyntaxInserter {
  fun insertInto(text: TextFieldValue): TextReplacement
}

/** Insert "*" around any text under selection or at the current cursor position. */
fun Wysiwyg.insertItalicSyntax() {
  insertSyntax(
    SymmetricMarkdownSyntaxInserter(
      syntax = "*",
      placeholder = "Italic"
    )
  )
}

/** Insert "**" around any text under selection or at the current cursor position. */
fun Wysiwyg.insertBoldSyntax() {
  insertSyntax(
    SymmetricMarkdownSyntaxInserter(
      syntax = "**",
      placeholder = "Bold"
    )
  )
}

/** Insert "~~" around any text under selection or at the current cursor position. */
fun Wysiwyg.insertStrikethroughSyntax() {
  insertSyntax(
    SymmetricMarkdownSyntaxInserter(
      syntax = "~~",
      placeholder = "Strikethrough"
    )
  )
}

/** Insert "[`]" around any text under selection or at the current cursor position. */
fun Wysiwyg.insertInlineCodeSyntax() {
  insertSyntax(
    SymmetricMarkdownSyntaxInserter(
      syntax = "`",
      placeholder = "Code"
    )
  )
}

/** Insert "[```]" around the paragraph currently being edited. */
fun Wysiwyg.insertCodeBlockSyntax() {
  insertSyntax(FencedCodeBlockSyntaxInserter)
}

/**
 * Insert ">" at the beginning of the paragraph currently being edited.
 * Can be used multiple times on the same paragraph to insert nested blockquotes.
 */
fun Wysiwyg.insertBlockQuoteSyntax() {
  insertSyntax(
    CompoundableParagraphSyntaxInserter.BlockQuote
  )
}

/**
 * Insert "#" at the beginning of the paragraph currently being edited.
 * Can be used multiple times on the same paragraph to insert nested blockquotes.
 */
fun Wysiwyg.insertHeadingSyntax() {
  insertSyntax(
    CompoundableParagraphSyntaxInserter.Heading
  )
}

/**
 * For markdown syntaxes that use the same characters on both sides of text.
 * For example: **strong emphasis**, ~~strikethrough~~.
 */
internal class SymmetricMarkdownSyntaxInserter(
  private val syntax: String,
  private val placeholder: String,
) : MarkdownSyntaxInserter {
  override fun insertInto(text: TextFieldValue): TextReplacement {
    val textUnderSelection = if (text.selection.collapsed) null else text.getSelectedText()

    val newSelection = if (textUnderSelection == null) {
      TextRange(
        start = text.selection.min + syntax.length,
        end = text.selection.min + syntax.length + placeholder.length,
      )
    } else {
      TextRange(
        index = text.selection.min + (syntax.length * 2) + textUnderSelection.length
      )
    }

    return TextReplacement(
      text = text.text.replaceRange(
        startIndex = text.selection.min,
        endIndex = text.selection.max,
        replacement = "$syntax${textUnderSelection ?: placeholder}$syntax"
      ),
      newSelection = newSelection
    )
  }
}

internal object FencedCodeBlockSyntaxInserter : MarkdownSyntaxInserter {
  private const val leftSyntax = "```\n"
  private const val rightSyntax = "\n```"

  override fun insertInto(text: TextFieldValue): TextReplacement {
    val currentParagraph = TextParagraph.findUnderCursor(text)
    return TextReplacement(
      text = text.text.replaceRange(
        startIndex = currentParagraph.startIndex,
        endIndex = currentParagraph.endIndexExclusive,
        replacement = "$leftSyntax${currentParagraph.text}$rightSyntax"
      ),
      newSelection = TextRange(
        start = text.selection.min + leftSyntax.length,
        end = text.selection.max + leftSyntax.length,
      )
    )
  }
}

internal class CompoundableParagraphSyntaxInserter(
  private val leftSyntax: Char,
  private val addSurroundingLineBreaks: Boolean
) : MarkdownSyntaxInserter {

  override fun insertInto(text: TextFieldValue): TextReplacement {
    val currentParagraph = TextParagraph.findUnderCursor(text)

    val willCompound = currentParagraph.text.getOrNull(0) == leftSyntax
    val hasLeadingSpace = currentParagraph.text.getOrNull(0)?.isWhitespace() ?: false
    val leftSyntaxWithSpace = when {
      willCompound || hasLeadingSpace -> "$leftSyntax"
      else -> "$leftSyntax "
    }

    val needsLeadingNewLine = addSurroundingLineBreaks
      && currentParagraph.startIndex >= 2
      && text.text[currentParagraph.startIndex - 2] != '\n'

    val hasFollowingNewLine = text.text.getOrNull(currentParagraph.endIndexExclusive + 1) == '\n'
    val needsFollowingNewLine = !hasFollowingNewLine
      && addSurroundingLineBreaks
      && currentParagraph.endIndexExclusive != text.text.length

    val leadingNewLine = if (needsLeadingNewLine) "\n" else ""
    return TextReplacement(
      text = text.text.replaceRange(
        startIndex = currentParagraph.startIndex,
        endIndex = currentParagraph.endIndexExclusive,
        replacement = buildString {
          append(leadingNewLine)
          append(leftSyntaxWithSpace)
          append(currentParagraph.text)
          if (needsFollowingNewLine) {
            append("\n")
          }
        }
      ),
      newSelection = text.selection.offsetBy(
        leadingNewLine.length + leftSyntaxWithSpace.length
      )
    )
  }

  companion object {
    val BlockQuote = CompoundableParagraphSyntaxInserter(
      leftSyntax = '>',
      addSurroundingLineBreaks = true
    )

    val Heading = CompoundableParagraphSyntaxInserter(
      leftSyntax = '#',
      addSurroundingLineBreaks = false
    )
  }
}

private fun TextRange.offsetBy(by: Int): TextRange {
  return TextRange(start = start + by, end = end + by)
}
