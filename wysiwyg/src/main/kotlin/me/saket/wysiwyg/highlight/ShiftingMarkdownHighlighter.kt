package me.saket.wysiwyg.highlight

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastMapNotNull
import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.saket.wysiwyg.MarkdownSpan
import me.saket.wysiwyg.highlight.MarkdownHighlighter.HighlightResult

/**
 * Wraps a non-incremental [delegate] to emit an approximate, flicker-free
 * first result before the delegate's full re-highlight completes.
 */
internal class ShiftingMarkdownHighlighter(
  private val delegate: MarkdownHighlighter,
) {
  private var previousHighlightResult: HighlightSnapshot? = null

  fun highlight(text: String, changes: ChangeListSnapshot): Flow<HighlightResult> {
    return flow {
      val shifted = previousHighlightResult?.rebasedOn(text, changes)
      if (shifted != null) {
        emit(shifted)
      }

      val result = delegate.highlight(text, changes)
      previousHighlightResult = HighlightSnapshot(textLength = text.length, result = result)
      emit(result)
    }
  }
}

private data class HighlightSnapshot(
  val textLength: Int,
  val result: HighlightResult,
) {
  fun rebasedOn(text: String, changes: ChangeListSnapshot): HighlightResult? {
    val canShift = if (changes.changes.isEmpty()) {
      false
    } else {
      // If [changes] genuinely describes the path from the cached text to [text], then the
      // cached length plus the changes' net delta must equal the new text's length. A mismatch
      // means some edits aren't represented — e.g. a prior highlight was cancelled before it
      // could update the cache, or a programmatic textState.edit {} slipped in between user
      // edits (which bypasses InputTransformation). Shifting by a partial [changes] would
      // produce wrong offsets; fall back to waiting for the full re-highlight.
      val totalDelta = changes.changes.fastSumBy {
        it.range.length - it.originalRange.length
      }
      textLength + totalDelta == text.length
    }
    return if (canShift) HighlightResult(result.spans.shiftedBy(changes)) else null
  }
}

/**
 * Returns a copy of these spans with their ranges shifted to reflect [changes].
 * Meant as an approximate, flicker-free result to render while a full re-parse runs.
 */
private fun List<MarkdownSpan>.shiftedBy(changes: ChangeListSnapshot): List<MarkdownSpan> {
  return fastMapNotNull { span ->
    val spanStart = span.range.start
    val spanEnd = span.range.end
    var startShift = 0
    var endShift = 0

    for (change in changes.changes) {
      // [editStart, editEnd) is the replaced region in the old (cached) text — same coordinate
      //  space as [spanStart, spanEnd). `delta` is how much the new text grew at this edit site.
      val editStart = change.originalRange.start
      val editEnd = change.originalRange.end
      val delta = change.range.length - change.originalRange.length

      when {
        editEnd <= spanStart -> {
          // Edit lies entirely before the span: the whole span slides by [delta].
          startShift += delta
          endShift += delta
        }
        editStart >= spanEnd -> {
          // Edit (and every later one, since they're ordered) lies
          // entirely after the span: leave both endpoints alone.
          break
        }
        editStart >= spanStart && editEnd <= spanEnd -> {
          // Edit is entirely inside the span: extend; keep the start, push the end by [delta]
          // (covers typing inside well-formed inline syntax like **bold** or `code`).
          endShift += delta
        }
        else -> {
          // Edit crosses a span boundary or engulfs the span: drop; the syntax may
          // be invalid now. The next full parse will restate the correct structure.
          return@fastMapNotNull null
        }
      }
    }

    return@fastMapNotNull MarkdownSpan(
      style = span.style,
      range = TextRange(spanStart + startShift, spanEnd + endShift),
    )
  }
}