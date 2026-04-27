package me.saket.wysiwyg.highlight

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange

/**
 * An immutable version of Compose's [TextFieldBuffer.ChangeList].
 */
@JvmInline
value class TextChangeListSnapshot(val changes: List<Change>) {
  /**
   * A single edit region. [range] is the affected region in the new text; [originalRange] is
   * the region in the previous text that was replaced.
   */
  data class Change(
    val range: TextRange,
    val originalRange: TextRange,
  )

  companion object {
    val Empty = TextChangeListSnapshot(emptyList())
  }
}

fun TextFieldBuffer.ChangeList.snapshot(): TextChangeListSnapshot {
  if (changeCount == 0) {
    return TextChangeListSnapshot.Empty
  }
  val copy = ArrayList<TextChangeListSnapshot.Change>(changeCount)
  for (i in 0 until changeCount) {
    copy += TextChangeListSnapshot.Change(
      range = getRange(i),
      originalRange = getOriginalRange(i),
    )
  }
  return TextChangeListSnapshot(changes = copy)
}

fun TextRange.rebased(changes: TextChangeListSnapshot): TextRange? {
  var startShift = 0
  var endShift = 0
  for (change in changes.changes) {
    val editStart = change.originalRange.start
    val editEnd = change.originalRange.end
    val delta = change.range.length - change.originalRange.length
    when {
      editEnd <= start -> {
        startShift += delta
        endShift += delta
      }
      editStart >= end -> break
      editStart >= start && editEnd <= end -> endShift += delta
      else -> return null
    }
  }
  return TextRange(start + startShift, end + endShift)
}
