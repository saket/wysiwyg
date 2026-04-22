package me.saket.wysiwyg.highlight

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange
import me.saket.wysiwyg.MarkdownSpan

interface MarkdownHighlighter {
  /**
   * Produces markdown spans for [text]. Called on every edit.
   *
   * Implementations don't produce interim results. Flicker-free rendering is handled automatically
   * by Wysiwyg, which wraps every highlighter in [ShiftingMarkdownHighlighter] to emit a
   * shifted-spans approximation between calls.
   *
   * @param changes Describes what changed since the previous call. This can be used by
   * highlighters that support incremental invalidation of their markdown ASTs.
   */
  suspend fun highlight(text: String, changes: ChangeListSnapshot): HighlightResult

  @JvmInline
  value class HighlightResult(
    val spans: List<MarkdownSpan>,
  )
}

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
