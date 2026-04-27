package me.saket.wysiwyg.highlight

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastFold

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

/**
 * Rebases this range through multiple edits in sequence.
 *
 * Note to self: each [TextChangeListSnapshot] is expressed relative to the text produced by all
 * prior entries, so these snapshots cannot be flattened by simply concatenating their change lists.
 */
fun TextRange.rebased(changes: List<TextChangeListSnapshot>): TextRange? {
  return changes.fastFold(this) { acc, changes ->
    acc.rebased(changes) ?: return null
  }
}

private fun TextRange.rebased(changes: TextChangeListSnapshot): TextRange? {
  var startShift = 0
  var endShift = 0
  for (change in changes.changes) {
    val editStart = change.originalRange.start
    val editEnd = change.originalRange.end
    val delta = change.range.length - change.originalRange.length
    when {
      editEnd <= start -> {
        // The edit happened fully before this range,
        // so both endpoints shift by the same delta.
        startShift += delta
        endShift += delta
      }
      editStart >= end -> {
        // The remaining edits are sorted and begin after
        //  this range, so no later edit can affect it.
        break
      }
      editStart >= start && editEnd <= end -> {
        // The edit is fully contained within this range.
        // Its inserted/deleted length only changes the
        // range's end; the start stays anchored where it was.
        endShift += delta
      }
      else -> {
        // The edit overlaps one of the range's boundaries.
        // Interim rendering treats that as invalid rather than
        // guessing how the syntax survives it.
        return null
      }
    }
  }
  return TextRange(start + startShift, end + endShift)
}
