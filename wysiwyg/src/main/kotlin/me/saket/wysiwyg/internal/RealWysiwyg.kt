package me.saket.wysiwyg.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.tracing.trace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import me.saket.wysiwyg.BuildConfig
import me.saket.wysiwyg.MarkdownOutputTransformation
import me.saket.wysiwyg.Wysiwyg
import me.saket.wysiwyg.format.OnEnterMarkdownFormatters
import me.saket.wysiwyg.parser.IncrementalMarkdownParser
import me.saket.wysiwyg.parser.MarkdownDocument
import me.saket.wysiwyg.parser.MarkdownParser
import me.saket.wysiwyg.parser.MarkdownRenderer
import me.saket.wysiwyg.parser.RealMarkdownRenderScope
import me.saket.wysiwyg.parser.RenderResult
import me.saket.wysiwyg.parser.TextChangeListSnapshot
import me.saket.wysiwyg.parser.snapshot

@Stable
internal class RealWysiwyg internal constructor(
  override val textState: TextFieldState,
  parser: MarkdownParser,
  onEnterFormatters: OnEnterMarkdownFormatters,
  coroutineScope: CoroutineScope,
) : Wysiwyg {
  private val parser = IncrementalMarkdownParser(parser)
  internal val layoutInfo = TextFieldLayoutInfo()

  @OptIn(ExperimentalCoroutinesApi::class)
  override val documents: StateFlow<MarkdownDocument> = snapshotFlow { textState.text }
    .flatMapLatest { text ->
      val changes = this.pendingChangeList
        .also { this.pendingChangeList = TextChangeListSnapshot.Empty }

      this.parser.parse(text.toString(), changes)
        .catch { e ->
          if (BuildConfig.DEBUG) {
            throw e
          } else {
            // todo: swallow errors in non-debug builds, and expose them to consumers
            TODO()
          }
        }
    }
    .stateIn(coroutineScope, SharingStarted.Lazily, MarkdownDocument.Empty)

  // todo: i don't love it that the recent render result is exposed through Wysiwyg.
  // This is not backed by snapshot state because transformOutput() can run inside
  // a read-only snapshot, and Compose will throw an IllegalStateException if snapshot
  // state is written from there. As a result, writes to this field do not invalidate
  // readers directly. That is okay for now because the styles are rebuilt as part of
  // an already invalidated text/output/layout pass, and draw code reads the latest
  // result from there.
  internal var currentRenderResult: RenderResult = RenderResult.Empty

  // Holds the ChangeList from the most recent InputTransformation invocation, awaiting consumption.
  // FWIW, this value isn't updated for non-user edits made directly using TextFieldState#edit().
  // In those cases, the highlighter will do a full re-scan even if it supported incremental highlighting.
  private var pendingChangeList: TextChangeListSnapshot = TextChangeListSnapshot.Empty

  @OptIn(ExperimentalFoundationApi::class)
  override val inputTransformation: InputTransformation = run {
    val chain = onEnterFormatters.asInputTransformation().then {
      pendingChangeList = changes.snapshot()
    }
    InputTransformation {
      trace("Wysiwyg:inputTransformation") {
        with(chain) { transformInput() }
      }
    }
  }
}

/**
 * Held as a stable instance because replacing the output transformation while editing causes
 * Compose to restart parts of the text input/output pipeline. In practice that dropped typed
 * characters and interrupted long-press backspace deletion.
 */
internal class RealMarkdownOutputTransformation(
  private val wysiwyg: RealWysiwyg,
  private val renderer: MarkdownRenderer.Factory,
  private val scope: RealMarkdownRenderScope,
) : MarkdownOutputTransformation {

  override fun TextFieldBuffer.transformOutput() {
    trace("Wysiwyg:transformOutput") {
      val document = wysiwyg.documents.value
      val renderer = renderer.create(
        buffer = this,
        unstyledText = run {
          // Note to self: create a copy of the text so the renderer reads from a stable snapshot.
          // The live TextFieldBuffer view can mutate mid-walk and throw IndexOutOfBoundsException
          // when its length shrinks under an in-flight read.
          this.toString()
        },
        changes = document.changes,
      )
      val result = trace("Wysiwyg:render") {
        with(renderer) {
          scope.render(document)
        }
      }
      wysiwyg.currentRenderResult = result
    }
  }
}
