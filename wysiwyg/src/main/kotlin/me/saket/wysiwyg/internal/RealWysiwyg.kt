package me.saket.wysiwyg.internal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.util.fastForEach
import androidx.tracing.trace
import kotlinx.coroutines.flow.collectLatest
import me.saket.wysiwyg.BuildConfig
import me.saket.wysiwyg.Wysiwyg
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.format.OnEnterMarkdownFormatters
import me.saket.wysiwyg.highlight.IncrementalMarkdownParser
import me.saket.wysiwyg.highlight.MarkdownDocument
import me.saket.wysiwyg.highlight.MarkdownParser
import me.saket.wysiwyg.highlight.TextChangeListSnapshot
import me.saket.wysiwyg.highlight.snapshot

@Stable
internal class RealWysiwyg internal constructor(
  override val textState: TextFieldState,
  override val theme: WysiwygTheme,
  parser: MarkdownParser,
  onEnterFormatters: OnEnterMarkdownFormatters,
) : Wysiwyg {
  private val parser = IncrementalMarkdownParser(parser)

  // Holds the ChangeList from the most recent InputTransformation invocation, awaiting consumption.
  // FWIW, this value isn't updated for non-user edits made directly using TextFieldState#edit().
  // In those cases, the highlighter will do a full re-scan even if it supported incremental highlighting.
  private var pendingChangeList: TextChangeListSnapshot = TextChangeListSnapshot.Empty

  @OptIn(ExperimentalFoundationApi::class)
  private val captureChangeList = InputTransformation {
    pendingChangeList = changes.snapshot()
  }

  override var outputTransformation by mutableStateOf(OutputTransformation {})

  override val inputTransformation: InputTransformation = onEnterFormatters
    .asInputTransformation()
    .then(captureChangeList)

  suspend fun syncOutputTransformationWithText() {
    snapshotFlow { textState.text }.collectLatest { text ->
      try {
        val changes = this.pendingChangeList
          .also { this.pendingChangeList = TextChangeListSnapshot.Empty }

        parser.parse(text.toString(), changes).collect { document ->
          outputTransformation = StyledOutputTransformation(
            document = document,
            renderer = MarkdownRenderer(theme),
          )
        }
      } catch (e: Throwable) {
        if (BuildConfig.DEBUG) {
          throw e
        } else {
          // todo: expose errors to consumers
          outputTransformation = OutputTransformation {}
        }
      }
    }
  }
}

private data class StyledOutputTransformation(
  private val document: MarkdownDocument,
  private val renderer: MarkdownRenderer,
) : OutputTransformation {

  override fun TextFieldBuffer.transformOutput() {
    trace("Wysiwyg:transformOutput") {
      applyMarkdownStyles()
    }
  }

  private fun TextFieldBuffer.applyMarkdownStyles() {
    if (document.children.isEmpty()) {
      return
    }

    val styled = trace("Wysiwyg:buildAnnotatedString") {
      renderer.buildAnnotatedString(
        text = AnnotatedString(toString()),
        document = document,
      )
    }
    trace("Wysiwyg:applySpans") {
      styled.spanStyles.fastForEach { range ->
        addStyle(range.item, range.start, range.end)
      }
      styled.paragraphStyles.fastForEach { range ->
        addStyle(range.item, range.start, range.end)
      }
    }
  }
}
