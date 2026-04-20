package me.saket.wysiwyg.internal

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.AnnotatedString
import kotlinx.coroutines.flow.collectLatest
import me.saket.wysiwyg.BuildConfig
import me.saket.wysiwyg.Wysiwyg
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.format.OnEnterMarkdownFormatters
import me.saket.wysiwyg.parser.ChangeListSnapshot
import me.saket.wysiwyg.parser.MarkdownParser

@Stable
internal class RealWysiwyg internal constructor(
  override val textState: TextFieldState,
  override val theme: WysiwygTheme,
  val parser: MarkdownParser,
  onEnterFormatters: OnEnterMarkdownFormatters,
) : Wysiwyg {
  override val inputTransformation = onEnterFormatters.asInputTransformation()
  override var outputTransformation by mutableStateOf(OutputTransformation {})

  suspend fun syncOutputTransformationWithText() {
    snapshotFlow { textState.text }.collectLatest { text ->
      try {
        parser.parse(text.toString(), ChangeListSnapshot.Empty).collect { parsed ->
          outputTransformation = StyledOutputTransformation(
            parseResult = parsed,
            renderer = MarkdownRenderer(theme),
          )
        }
      } catch (e: Throwable) {
        if (BuildConfig.DEBUG) {
          throw e
        } else {
          outputTransformation = OutputTransformation {}
        }
      }
    }
  }
}

private data class StyledOutputTransformation(
  val parseResult: MarkdownParser.ParseResult,
  private val renderer: MarkdownRenderer,
) : OutputTransformation {

  override fun TextFieldBuffer.transformOutput() {
    applyMarkdownStyles()
  }

  private fun TextFieldBuffer.applyMarkdownStyles() {
    if (parseResult.spans.isEmpty()) {
      return
    }

    val styled = renderer.buildAnnotatedString(
      text = AnnotatedString(toString()),
      spans = parseResult.spans,
    )
    styled.spanStyles.fastForEach { range ->
      addStyle(range.item, range.start, range.end)
    }
    styled.paragraphStyles.fastForEach { range ->
      addStyle(range.item, range.start, range.end)
    }
  }
}
