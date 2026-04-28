package me.saket.wysiwyg.format

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange

internal fun interface MarkdownMarkerInserter {
  fun insertInto(text: CharSequence, selection: TextRange): TextReplacement
}

internal fun TextFieldState.insertMarker(inserter: MarkdownMarkerInserter) {
  val replacement = inserter.insertInto(text = text, selection = selection)
  edit {
    with(replacement) { replace() }
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
    val start = selection.min
    val end = selection.max
    val textUnderSelection = if (selection.collapsed) null else text.substring(start, end)
    val newSelection = if (textUnderSelection == null) {
      TextRange(
        start = start + marker.length,
        end = start + marker.length + placeholder.length,
      )
    } else {
      TextRange(start + (marker.length * 2) + textUnderSelection.length)
    }
    return TextReplacement {
      replace(start, end, "$marker${textUnderSelection ?: placeholder}$marker")
      this.selection = newSelection
    }
  }
}

internal object FencedCodeBlockMarkerInserter : MarkdownMarkerInserter {
  private const val leftMarker = "```\n"
  private const val rightMarker = "\n```"

  override fun insertInto(text: CharSequence, selection: TextRange): TextReplacement {
    val currentParagraph = TextParagraph.findUnderCursor(text, selection)
    val newSelection = TextRange(
      start = selection.start + leftMarker.length,
      end = selection.end + leftMarker.length,
    )
    return TextReplacement {
      replace(
        start = currentParagraph.startIndex,
        end = currentParagraph.endIndexExclusive,
        text = "$leftMarker${currentParagraph.text}$rightMarker",
      )
      this.selection = newSelection
    }
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
    val replacement = buildString {
      append(leadingNewLine)
      append(leftMarkerWithSpace)
      append(currentParagraph.text)
      if (needsFollowingNewLine) {
        append("\n")
      }
    }
    val cursorOffset = leadingNewLine.length + leftMarkerWithSpace.length
    val newSelection = TextRange(
      start = selection.start + cursorOffset,
      end = selection.end + cursorOffset,
    )
    return TextReplacement {
      replace(
        start = currentParagraph.startIndex,
        end = currentParagraph.endIndexExclusive,
        text = replacement,
      )
      this.selection = newSelection
    }
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
