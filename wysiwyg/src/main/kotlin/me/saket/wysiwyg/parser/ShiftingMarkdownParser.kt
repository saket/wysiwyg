package me.saket.wysiwyg.parser

import androidx.compose.ui.util.fastMapNotNull
import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import me.saket.wysiwyg.MarkdownSpan
import me.saket.wysiwyg.MarkdownSpanTextRange
import me.saket.wysiwyg.parser.MarkdownParser.ParseResult

/**
 * Wraps a non-incremental [delegate] to emit an approximate, flicker-free
 * first result before the delegate's full re-parse completes.
 */
internal class ShiftingMarkdownParser(
  private val delegate: MarkdownParser,
) {
  private var previousParseResult: ParseSnapshot? = null

  fun parse(text: String, changes: ChangeListSnapshot): Flow<ParseResult> {
    return flow {
      val shifted = previousParseResult?.rebasedOn(text, changes)
      if (shifted != null) {
        emit(shifted)
      }

      val result = delegate.parse(text, changes)
      previousParseResult = ParseSnapshot(textLength = text.length, result = result)
      emit(result)
    }
  }
}

private data class ParseSnapshot(
  val textLength: Int,
  val result: ParseResult,
) {
  fun rebasedOn(text: String, changes: ChangeListSnapshot): ParseResult? {
    val canShift = if (changes.changes.isEmpty()) {
      false
    } else {
      // If [changes] genuinely describes the path from the cached text to [text], then the
      // cached length plus the changes' net delta must equal the new text's length. A mismatch
      // means some edits aren't represented — e.g. a prior parse was cancelled before it could
      // update the cache, or a programmatic textState.edit {} slipped in between user edits
      // (which bypasses InputTransformation). Shifting by a partial [changes] would produce
      // wrong offsets; fall back to waiting for the full parse.
      val totalDelta = changes.changes.fastSumBy {
        it.range.length - it.originalRange.length
      }
      textLength + totalDelta == text.length
    }
    return if (canShift) ParseResult(result.spans.shiftedBy(changes)) else null
  }
}

/**
 * Returns a copy of these spans with their ranges shifted to reflect [changes].
 * Meant as an approximate, flicker-free result to render while a full re-parse runs.
 */
private fun List<MarkdownSpan>.shiftedBy(changes: ChangeListSnapshot): List<MarkdownSpan> {
  return fastMapNotNull { span ->
    val spanStart = span.range.startIndex
    val spanEnd = span.range.endIndexExclusive
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
      range = MarkdownSpanTextRange(spanStart + startShift, spanEnd + endShift),
    )
  }
}