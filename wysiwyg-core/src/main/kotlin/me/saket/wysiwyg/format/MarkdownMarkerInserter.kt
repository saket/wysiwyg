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
 * Applying it again to a block-quote removes the marker.
 */
fun TextFieldState.insertBlockQuoteMarker() {
  insertMarker(CompoundableParagraphMarkerInserter.BlockQuote)
}

/**
 * Insert "#" at the beginning of the paragraph currently being edited.
 * Applying it again raises the heading one level, cycling back to h1 after h6.
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

/**
 * Inserts a paragraph level marker such as ">" (block quote) or "#" (heading) at the
 * start of the paragraph currently being edited.
 *
 * Each insertion raises the paragraph one level, up to [maxLevel]. Once at [maxLevel],
 * the next insertion wraps back to [minLevel]: headings cycle h1 through h6 and back to
 * h1, while block quotes (whose [minLevel] is 0) toggle on and off.
 */
internal class CompoundableParagraphMarkerInserter(
  private val leftMarker: Char,
  private val minLevel: Int,
  private val maxLevel: Int,
) : MarkdownMarkerInserter {

  override fun insertInto(text: CharSequence, selection: TextRange): TextReplacement {
    val currentParagraph = TextParagraph.findUnderCursor(text, selection)

    val currentLevel = currentParagraph.text.takeWhile { it == leftMarker }.length
    val newLevel = if (currentLevel >= maxLevel) minLevel else currentLevel + 1

    // The paragraph text with its leading markers removed. The single space that
    // separates the markers from the content is dropped only when falling back to
    // level 0, so that "> text" round-trips back to "text".
    val textAfterMarkers = currentParagraph.text.substring(currentLevel)
    val markers = "$leftMarker".repeat(newLevel)
    val replacement = when {
      newLevel == 0 -> textAfterMarkers.removePrefix(" ")
      textAfterMarkers.startsWith(" ") -> "$markers$textAfterMarkers"
      else -> "$markers $textAfterMarkers"
    }

    val cursorOffset = replacement.length - currentParagraph.text.length
    val newSelection = TextRange(
      start = (selection.start + cursorOffset).coerceAtLeast(currentParagraph.startIndex),
      end = (selection.end + cursorOffset).coerceAtLeast(currentParagraph.startIndex),
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
      minLevel = 0,
      maxLevel = 1,
    )

    val Heading = CompoundableParagraphMarkerInserter(
      leftMarker = '#',
      minLevel = 1,
      maxLevel = 6,
    )
  }
}
