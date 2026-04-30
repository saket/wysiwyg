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
 *
 * Re-applying the same marker to a selection that already wraps or sits inside
 * the markers removes them (toggle off). Toggling requires an active selection;
 * a bare cursor always inserts a placeholder.
 */
internal class SymmetricMarkdownMarkerInserter(
  private val marker: String,
  private val placeholder: String,
) : MarkdownMarkerInserter {
  override fun insertInto(text: CharSequence, selection: TextRange): TextReplacement {
    val markerLength = marker.length
    val start = selection.min
    val end = selection.max

    // Toggle off when the selection sits inside the markers: `**▮text▮**`.
    // Wrap-on below always produces this shape, so re-clicking round-trips here.
    if (!selection.collapsed
      && start >= markerLength
      && end + markerLength <= text.length
      && text.regionMatches(start - markerLength, marker, 0, markerLength)
      && text.regionMatches(end, marker, 0, markerLength)
    ) {
      val innerText = text.substring(start, end)
      return TextReplacement {
        replace(start - markerLength, end + markerLength, innerText)
        this.selection = TextRange(start - markerLength, end - markerLength)
      }
    }

    // Wrap the selection (or insert a placeholder for a bare cursor) and leave
    // the selection on the inner text only. Typing replaces the selected text
    // without disturbing the markers.
    val selectedText = if (selection.collapsed) null else text.substring(start, end)
    val innerText = selectedText ?: placeholder
    return TextReplacement {
      replace(start, end, "$marker$innerText$marker")
      this.selection = TextRange(start + markerLength, start + markerLength + innerText.length)
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
