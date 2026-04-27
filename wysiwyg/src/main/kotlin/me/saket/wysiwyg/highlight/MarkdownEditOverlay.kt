package me.saket.wysiwyg.highlight

import androidx.compose.ui.text.TextRange
@JvmInline
value class MarkdownEditOverlay(
  val changes: ChangeListSnapshot,
) {
  fun renderedRange(originalRange: TextRange): TextRange? {
    return originalRange.rebased(changes)
  }

  fun touches(range: TextRange): Boolean {
    return changes.touches(range)
  }

  companion object {
    val Empty = MarkdownEditOverlay(ChangeListSnapshot.Empty)

    // todo: dont love this indirection.
    inline fun TextRange.overlayed(overlay: MarkdownEditOverlay): TextRange? {
      return overlay.renderedRange(this)
    }
  }
}
