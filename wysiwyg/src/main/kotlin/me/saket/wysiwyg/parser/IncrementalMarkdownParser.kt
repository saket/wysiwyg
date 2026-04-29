package me.saket.wysiwyg.parser

import androidx.compose.ui.util.fastSumBy
import androidx.tracing.trace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * Wraps a non-incremental [delegate] to emit an approximate, flicker-free
 * first result before the delegate's full re-parse completes.
 */
internal class IncrementalMarkdownParser(
  private val delegate: MarkdownParser,
) {
  private var previousDocument: MarkdownDocumentSnapshot? = null

  fun parse(text: String, changes: TextChangeListSnapshot): Flow<MarkdownDocument> {
    return flow {
      val overlayed = trace("Wysiwyg:edited") {
        previousDocument?.overlayedOn(text, changes)
      }
      if (overlayed != null) {
        previousDocument = MarkdownDocumentSnapshot(text.length, overlayed)
        emit(overlayed)
      }

      val document = trace("Wysiwyg:parse") {
        delegate.parse(text, changes)
      }
      previousDocument = MarkdownDocumentSnapshot(text.length, document)
      emit(document)
    }.distinctUntilChanged()
  }
}

private data class MarkdownDocumentSnapshot(
  val oldTextLength: Int,
  val document: MarkdownDocument,
) {
  fun overlayedOn(newText: String, changes: TextChangeListSnapshot): MarkdownDocument? {
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
      // Overlaying by this partial change list would produce wrong offsets.
      return null
    }
    return document.copy(changes = document.changes + changes)
  }
}
