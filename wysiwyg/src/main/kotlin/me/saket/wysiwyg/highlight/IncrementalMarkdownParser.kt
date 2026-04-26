package me.saket.wysiwyg.highlight

import androidx.compose.ui.util.fastSumBy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Wraps a non-incremental [delegate] to emit an approximate, flicker-free
 * first result before the delegate's full re-parse completes.
 */
internal class IncrementalMarkdownParser(
  private val delegate: MarkdownParser,
) {
  private var previousDocument: MarkdownDocumentSnapshot? = null

  fun parse(text: String, changes: ChangeListSnapshot): Flow<MarkdownDocument> {
    return flow {
      val shifted = previousDocument?.rebasedOn(text, changes)
      if (shifted != null) {
        emit(shifted)
      }

      val document = delegate.parse(text, changes)
      previousDocument = MarkdownDocumentSnapshot(oldTextLength = text.length, document = document)
      emit(document)
    }
  }
}

private data class MarkdownDocumentSnapshot(
  val oldTextLength: Int,
  val document: MarkdownDocument,
) {
  fun rebasedOn(newText: String, changes: ChangeListSnapshot): MarkdownDocument? {
    if (changes.changes.isEmpty()) {
      return null
    }
    val totalDelta = changes.changes.fastSumBy {
      it.range.length - it.originalRange.length
    }
    if (oldTextLength + totalDelta != newText.length) {
      // [changes] does not fully describe the path from the cached text to [text]. This can
      // happen if a prior parse was cancelled before updating the cache, or if a programmatic
      // textState.edit {} slipped in between user edits (which bypasses InputTransformation).
      // Rebasing by this partial change list would produce wrong offsets.
      return null
    }

    return document.rebased(changes)
  }
}
