package me.saket.wysiwyg.format

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange

internal fun interface MarkdownSyntaxInserter {
  fun insertInto(text: CharSequence, selection: TextRange): TextReplacement
}

internal fun TextFieldState.insertSyntax(inserter: MarkdownSyntaxInserter) {
  val replacement = inserter.insertInto(text = text, selection = selection)
  edit {
    replace(0, length, replacement.text)
    selection = replacement.newSelection
  }
}

/** Insert "*" around any text under selection or at the current cursor position. */
fun TextFieldState.insertItalic() {
  insertSyntax(SymmetricMarkdownSyntaxInserter(syntax = "*", placeholder = "Italic"))
}

/** Insert "**" around any text under selection or at the current cursor position. */
fun TextFieldState.insertBold() {
  insertSyntax(SymmetricMarkdownSyntaxInserter(syntax = "**", placeholder = "Bold"))
}

/** Insert "~~" around any text under selection or at the current cursor position. */
fun TextFieldState.insertStrikethrough() {
  insertSyntax(SymmetricMarkdownSyntaxInserter(syntax = "~~", placeholder = "Strikethrough"))
}

/** Insert "`" around any text under selection or at the current cursor position. */
fun TextFieldState.insertInlineCode() {
  insertSyntax(SymmetricMarkdownSyntaxInserter(syntax = "`", placeholder = "Code"))
}

/** Insert "```" around the paragraph currently being edited. */
fun TextFieldState.insertCodeBlock() {
  insertSyntax(FencedCodeBlockSyntaxInserter)
}

/**
 * Insert ">" at the beginning of the paragraph currently being edited.
 * Can be used multiple times on the same paragraph to insert nested blockquotes.
 */
fun TextFieldState.insertBlockQuote() {
  insertSyntax(CompoundableParagraphSyntaxInserter.BlockQuote)
}

/**
 * Insert "#" at the beginning of the paragraph currently being edited.
 * Can be used multiple times on the same paragraph to insert nested headings.
 */
fun TextFieldState.insertHeading() {
  insertSyntax(CompoundableParagraphSyntaxInserter.Heading)
}

/**
 * For markdown syntaxes that use the same characters on both sides of text.
 * For example: **strong emphasis**, ~~strikethrough~~.
 */
internal class SymmetricMarkdownSyntaxInserter(
  private val syntax: String,
  private val placeholder: String,
) : MarkdownSyntaxInserter {
  override fun insertInto(text: CharSequence, selection: TextRange): TextReplacement {
    val textUnderSelection = if (selection.collapsed) null else text.substring(selection.min, selection.max)

    val newSelection = if (textUnderSelection == null) {
      TextRange(
        start = selection.min + syntax.length,
        end = selection.min + syntax.length + placeholder.length,
      )
    } else {
      TextRange(
        index = selection.min + (syntax.length * 2) + textUnderSelection.length,
      )
    }

    return TextReplacement(
      text = text.replaceRange(
        startIndex = selection.min,
        endIndex = selection.max,
        replacement = "$syntax${textUnderSelection ?: placeholder}$syntax",
      ),
      newSelection = newSelection,
    )
  }
}

internal object FencedCodeBlockSyntaxInserter : MarkdownSyntaxInserter {
  private const val leftSyntax = "```\n"
  private const val rightSyntax = "\n```"

  override fun insertInto(text: CharSequence, selection: TextRange): TextReplacement {
    val currentParagraph = TextParagraph.findUnderCursor(text, selection)
    return TextReplacement(
      text = text.replaceRange(
        startIndex = currentParagraph.startIndex,
        endIndex = currentParagraph.endIndexExclusive,
        replacement = "$leftSyntax${currentParagraph.text}$rightSyntax",
      ),
      newSelection = selection.offsetBy(leftSyntax.length),
    )
  }
}

internal class CompoundableParagraphSyntaxInserter(
  private val leftSyntax: Char,
  private val addSurroundingLineBreaks: Boolean,
) : MarkdownSyntaxInserter {

  override fun insertInto(text: CharSequence, selection: TextRange): TextReplacement {
    val currentParagraph = TextParagraph.findUnderCursor(text, selection)

    val willCompound = currentParagraph.text.getOrNull(0) == leftSyntax
    val hasLeadingSpace = currentParagraph.text.getOrNull(0)?.isWhitespace() ?: false
    val leftSyntaxWithSpace = when {
      willCompound || hasLeadingSpace -> "$leftSyntax"
      else -> "$leftSyntax "
    }

    val needsLeadingNewLine = addSurroundingLineBreaks
      && currentParagraph.startIndex >= 2
      && text[currentParagraph.startIndex - 2] != '\n'

    val hasFollowingNewLine = text.getOrNull(currentParagraph.endIndexExclusive + 1) == '\n'
    val needsFollowingNewLine = !hasFollowingNewLine
      && addSurroundingLineBreaks
      && currentParagraph.endIndexExclusive != text.length

    val leadingNewLine = if (needsLeadingNewLine) "\n" else ""
    return TextReplacement(
      text = text.replaceRange(
        startIndex = currentParagraph.startIndex,
        endIndex = currentParagraph.endIndexExclusive,
        replacement = buildString {
          append(leadingNewLine)
          append(leftSyntaxWithSpace)
          append(currentParagraph.text)
          if (needsFollowingNewLine) {
            append("\n")
          }
        },
      ),
      newSelection = selection.offsetBy(
        leadingNewLine.length + leftSyntaxWithSpace.length,
      ),
    )
  }

  companion object {
    val BlockQuote = CompoundableParagraphSyntaxInserter(
      leftSyntax = '>',
      addSurroundingLineBreaks = true,
    )

    val Heading = CompoundableParagraphSyntaxInserter(
      leftSyntax = '#',
      addSurroundingLineBreaks = false,
    )
  }
}

private fun TextRange.offsetBy(by: Int): TextRange {
  return TextRange(start = start + by, end = end + by)
}
