package me.saket.wysiwyg.format

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange

internal fun interface MarkdownMarkerInserter {
  fun insertInto(text: CharSequence, selection: TextRange): TextReplacement
}

internal fun TextFieldState.insertMarker(inserter: MarkdownMarkerInserter) {
  val replacement = inserter.insertInto(text = text, selection = selection)
  edit {
    replace(0, length, replacement.text)
    selection = replacement.newSelection
  }
}

/** Insert "*" around any text under selection or at the current cursor position. */
fun TextFieldState.insertItalicMarker() {
  insertMarker(SymmetricMarkdownMarkerInserter(marker = "*", placeholder = "Italic"))
}

/** Insert "**" around any text under selection or at the current cursor position. */
fun TextFieldState.insertBoldMarker() {
  insertMarker(SymmetricMarkdownMarkerInserter(marker = "**", placeholder = "Bold"))
}

/** Insert "~~" around any text under selection or at the current cursor position. */
fun TextFieldState.insertStrikethroughMarker() {
  insertMarker(SymmetricMarkdownMarkerInserter(marker = "~~", placeholder = "Strikethrough"))
}

/** Insert "`" around any text under selection or at the current cursor position. */
fun TextFieldState.insertInlineCodeMarker() {
  insertMarker(SymmetricMarkdownMarkerInserter(marker = "`", placeholder = "Code"))
}

/** Insert "```" around the paragraph currently being edited. */
fun TextFieldState.insertCodeBlockMarker() {
  insertMarker(FencedCodeBlockMarkerInserter)
}

/**
 * Insert ">" at the beginning of the paragraph currently being edited.
 * Can be used multiple times on the same paragraph to insert nested blockquotes.
 */
fun TextFieldState.insertBlockQuoteMarker() {
  insertMarker(CompoundableParagraphMarkerInserter.BlockQuote)
}

/**
 * Insert "#" at the beginning of the paragraph currently being edited.
 * Can be used multiple times on the same paragraph to insert nested headings.
 */
fun TextFieldState.insertHeadingMarker() {
  insertMarker(CompoundableParagraphMarkerInserter.Heading)
}

/**
 * For markdown markers that wrap text symmetrically.
 * For example: **strong emphasis**, ~~strikethrough~~.
 */
internal class SymmetricMarkdownMarkerInserter(
  private val marker: String,
  private val placeholder: String,
) : MarkdownMarkerInserter {
  override fun insertInto(text: CharSequence, selection: TextRange): TextReplacement {
    val textUnderSelection = if (selection.collapsed) null else text.substring(selection.min, selection.max)

    val newSelection = if (textUnderSelection == null) {
      TextRange(
        start = selection.min + marker.length,
        end = selection.min + marker.length + placeholder.length,
      )
    } else {
      TextRange(
        index = selection.min + (marker.length * 2) + textUnderSelection.length,
      )
    }

    return TextReplacement(
      text = text.replaceRange(
        startIndex = selection.min,
        endIndex = selection.max,
        replacement = "$marker${textUnderSelection ?: placeholder}$marker",
      ),
      newSelection = newSelection,
    )
  }
}

internal object FencedCodeBlockMarkerInserter : MarkdownMarkerInserter {
  private const val leftMarker = "```\n"
  private const val rightMarker = "\n```"

  override fun insertInto(text: CharSequence, selection: TextRange): TextReplacement {
    val currentParagraph = TextParagraph.findUnderCursor(text, selection)
    return TextReplacement(
      text = text.replaceRange(
        startIndex = currentParagraph.startIndex,
        endIndex = currentParagraph.endIndexExclusive,
        replacement = "$leftMarker${currentParagraph.text}$rightMarker",
      ),
      newSelection = selection.offsetBy(leftMarker.length),
    )
  }
}

internal class CompoundableParagraphMarkerInserter(
  private val leftMarker: Char,
  private val addSurroundingLineBreaks: Boolean,
) : MarkdownMarkerInserter {

  override fun insertInto(text: CharSequence, selection: TextRange): TextReplacement {
    val currentParagraph = TextParagraph.findUnderCursor(text, selection)

    val willCompound = currentParagraph.text.getOrNull(0) == leftMarker
    val hasLeadingSpace = currentParagraph.text.getOrNull(0)?.isWhitespace() ?: false
    val leftMarkerWithSpace = when {
      willCompound || hasLeadingSpace -> "$leftMarker"
      else -> "$leftMarker "
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
          append(leftMarkerWithSpace)
          append(currentParagraph.text)
          if (needsFollowingNewLine) {
            append("\n")
          }
        },
      ),
      newSelection = selection.offsetBy(
        leadingNewLine.length + leftMarkerWithSpace.length,
      ),
    )
  }

  companion object {
    val BlockQuote = CompoundableParagraphMarkerInserter(
      leftMarker = '>',
      addSurroundingLineBreaks = true,
    )

    val Heading = CompoundableParagraphMarkerInserter(
      leftMarker = '#',
      addSurroundingLineBreaks = false,
    )
  }
}

private fun TextRange.offsetBy(by: Int): TextRange {
  return TextRange(start = start + by, end = end + by)
}
