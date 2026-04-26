package me.saket.wysiwyg.highlight

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastAny

/**
 * An immutable version of Compose's [TextFieldBuffer.ChangeList] that outlives the transformation
 * callback in which it was produced, safe to read from any thread.
 */
@JvmInline
value class ChangeListSnapshot(val changes: List<Change>) {
  /**
   * A single edit region. [range] is the affected region in the new text; [originalRange] is
   * the region in the previous text that was replaced.
   */
  data class Change(
    val range: TextRange,
    val originalRange: TextRange,
  )

  companion object {
    val Empty = ChangeListSnapshot(emptyList())

    fun from(changes: TextFieldBuffer.ChangeList): ChangeListSnapshot {
      if (changes.changeCount == 0) return Empty
      val copy = ArrayList<Change>(changes.changeCount)
      for (i in 0 until changes.changeCount) {
        copy += Change(
          range = changes.getRange(i),
          originalRange = changes.getOriginalRange(i),
        )
      }
      return ChangeListSnapshot(changes = copy)
    }
  }
}

fun ChangeListSnapshot.touches(range: TextRange): Boolean {
  return changes.fastAny { change ->
    change.touches(range)
  }
}

fun ChangeListSnapshot.touchesAny(
  first: TextRange,
  second: TextRange,
): Boolean {
  return changes.fastAny { change ->
    change.touches(first) || change.touches(second)
  }
}

fun ChangeListSnapshot.touchesAny(
  first: TextRange,
  second: TextRange,
  third: TextRange,
): Boolean {
  return changes.fastAny { change ->
    change.touches(first)
        || change.touches(second)
        || change.touches(third)
  }
}

fun ChangeListSnapshot.touchesAny(
  first: TextRange,
  second: TextRange,
  third: TextRange,
  fourth: TextRange,
): Boolean {
  return changes.fastAny { change ->
    change.touches(first)
        || change.touches(second)
        || change.touches(third)
        || change.touches(fourth)
  }
}

fun ChangeListSnapshot.Change.touches(range: TextRange): Boolean {
  if (originalRange.collapsed) {
    val insertionPoint = originalRange.start
    return insertionPoint > range.start && insertionPoint < range.end
  }
  return originalRange.intersects(range)
}

fun TextRange.rebased(changes: ChangeListSnapshot): TextRange? {
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
