package me.saket.wysiwyg.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.tracing.trace
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import me.saket.wysiwyg.BuildConfig
import me.saket.wysiwyg.MarkdownOutputTransformation
import me.saket.wysiwyg.Wysiwyg
import me.saket.wysiwyg.format.OnEnterMarkdownFormatters
import me.saket.wysiwyg.parser.IncrementalMarkdownParser
import me.saket.wysiwyg.parser.MarkdownDocument
import me.saket.wysiwyg.parser.MarkdownParser
import me.saket.wysiwyg.render.MarkdownRenderer
import me.saket.wysiwyg.render.RealMarkdownRenderScope
import me.saket.wysiwyg.render.RenderResult
import me.saket.wysiwyg.parser.TextChangeListSnapshot
import me.saket.wysiwyg.parser.snapshot

@Stable
internal class RealWysiwyg internal constructor(
  override val textState: TextFieldState,
  parser: MarkdownParser,
  onEnterFormatters: OnEnterMarkdownFormatters,
) : Wysiwyg {
  private val parser = IncrementalMarkdownParser(parser)
  internal val layoutInfo = TextFieldLayoutInfo()

  @OptIn(ExperimentalCoroutinesApi::class)
  override var document by mutableStateOf(MarkdownDocument.Empty)

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

  suspend fun startParsingMarkdown() {
    snapshotFlow { textState.text }.collectLatest { text ->
      try {
        val changes = this.pendingChangeList
          .also { this.pendingChangeList = TextChangeListSnapshot.Empty }

        parser.parse(text.toString(), changes).collect { document ->
          this.document = document
        }
      } catch (e: Throwable) {
        if (BuildConfig.DEBUG) {
          throw e
        } else {
          // todo: expose errors to consumers
        }
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
      val document = wysiwyg.document
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
