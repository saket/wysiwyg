package me.saket.wysiwyg.parser

import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.TextRange
import kotlinx.coroutines.flow.Flow
import me.saket.wysiwyg.MarkdownSpan

interface MarkdownParser {
  /**
   * Produces markdown spans for [text]. The returned flow may emit multiple times:
   * an initial synchronous approximate result (for flicker-free rendering while a full parse
   * runs), followed by the final correct result. Parsers that can't produce an interim
   * result emit once.
   *
   * @param changes describes what changed since the previous call. Incremental parsers use
   * this to skip re-diffing the text; non-incremental parsers ignore it. Pass
   * [ChangeListSnapshot.Empty] for the initial parse or when edit info isn't available.
   */
  fun parse(text: String, changes: ChangeListSnapshot): Flow<ParseResult>

  @JvmInline
  value class ParseResult(
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
