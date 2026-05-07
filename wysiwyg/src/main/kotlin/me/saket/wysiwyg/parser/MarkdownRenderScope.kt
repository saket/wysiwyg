package me.saket.wysiwyg.parser

import androidx.compose.ui.text.TextRange
import me.saket.wysiwyg.internal.TextFieldViewport

// todo: kdoc
interface MarkdownRenderScope {
  // todo: kdoc
  var changes: List<TextChangeListSnapshot>

  // todo: kdoc
  var offsetInRoot: Int

  /**
   * Resolves this local range to its absolute position in the rendered text.
   *
   * When [dropOnEdit] is true, returns `null` if any edit overlaps this range. The caller's
   * `?: return` then drops the node's styling for one frame until the reparse arrives. Use
   * it for fixed-shape markers (emphasis's `**`, a link's `]`, a list item's `-`, a
   * blockquote's `>`) where any edit invalidates the syntax. Skip it for repeatable markers
   * like a heading's `#`s.
   */
  fun LocalTextRange.resolve(dropOnEdit: Boolean = false): TextRange?
}

internal class RealMarkdownRenderScope : MarkdownRenderScope {
  // todo: make this a constructor param.
  internal var viewport: TextFieldViewport? = null

  override var changes: List<TextChangeListSnapshot> = emptyList()
  override var offsetInRoot: Int = 0

  override fun LocalTextRange.resolve(dropOnEdit: Boolean): TextRange? {
    val rangeInRoot = TextRange(
      start = textRange.start + offsetInRoot,
      end = textRange.end + offsetInRoot,
    )
    val rebased = if (dropOnEdit && changes.editsOverlap(rangeInRoot)) {
      null
    } else {
      rangeInRoot.rebased(changes)
    }
    val viewport = viewport

    // todo: this line wasn't this long in previous version of MarkdownRenderScope.
    return if (rebased == null || viewport == null || viewport.intersects(rebased, includeBeyondViewport = true)) {
      rebased
    } else {
      null
    }
  }
}
