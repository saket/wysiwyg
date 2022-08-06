@file:Suppress("NAME_SHADOWING")

package me.saket.wysiwyg

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.saket.wysiwyg.format.MarkdownSyntaxInserter
import me.saket.wysiwyg.format.OnEnterContinueList
import me.saket.wysiwyg.format.OnEnterFormatter
import me.saket.wysiwyg.format.OnEnterFormatters
import me.saket.wysiwyg.format.OnEnterStartCodeBlock
import me.saket.wysiwyg.format.insertBoldSyntax
import me.saket.wysiwyg.internal.MarkdownRenderer
import me.saket.wysiwyg.parser.FlexmarkMarkdownParser
import me.saket.wysiwyg.parser.MarkdownParser
import me.saket.wysiwyg.parser.MarkdownSpan
import kotlin.coroutines.CoroutineContext

@Composable fun rememberWysiwyg(
  theme: WysiwygTheme,
  onEnterFormatters: List<OnEnterFormatter> = listOf(OnEnterStartCodeBlock, OnEnterContinueList()),
  text: () -> TextFieldValue = { TextFieldValue() }
): Wysiwyg {
  return remember {
    Wysiwyg(
      text = text(),
      theme = theme,
      onEnterFormatters = onEnterFormatters,
      parser = FlexmarkMarkdownParser(),
      backgroundDispatcher = Dispatchers.IO
    )
  }
}

@Stable
class Wysiwyg internal constructor(
  text: TextFieldValue,
  theme: WysiwygTheme,
  onEnterFormatters: List<OnEnterFormatter>,
  private val parser: MarkdownParser,
  private val backgroundDispatcher: CoroutineContext,
) {
  private var text: TextFieldValue by mutableStateOf(text)
  private val renderer = MarkdownRenderer(theme)
  private val onEnterFormatters = OnEnterFormatters(onEnterFormatters)

  /** Text with syntax highlighting of markdown syntaxes. */
  @Composable
  fun text(): TextFieldValue {
    var previousValue: TextFieldValue? by remember { mutableStateOf(null) }
    var previousSpans: List<MarkdownSpan> by remember { mutableStateOf(emptyList()) }

    var styledText: AnnotatedString by remember(text.text) {
      // Retain any previously applied spans immediately so that there's no delay in
      // highlighting markdown. The downside of doing this is that it's happening on
      // the main thread so it's not great for large amounts of text.
      previousValue?.let { previousValue ->
        previousSpans = parser.offsetSpansOnTextChange(
          newValue = text,
          previousValue = previousValue,
          previousSpans = previousSpans,
        )
      }
      mutableStateOf(renderer.buildAnnotatedString(text.annotatedString, previousSpans))
    }

    LaunchedEffect(text.text) {
      withContext(backgroundDispatcher) {
        try {
          val result = parser.parse(text.text)
          styledText = renderer.buildAnnotatedString(text.annotatedString, result.spans)
          previousSpans = result.spans

        } catch (e: Throwable) {
          if (BuildConfig.DEBUG) {
            throw e
          } else {
            previousSpans = emptyList()
            e.printStackTrace()
          }
        }
      }
    }

    previousValue = text
    return text.copy(annotatedString = styledText)
  }

  fun onTextChange(newValue: TextFieldValue) {
    text = onEnterFormatters.formatIfEnterWasPressed(
      previousText = text,
      newText = newValue
    )
  }

  /**
   * See [Wysiwyg.insert*Syntax()] functions.
   */
  fun insertSyntax(inserter: MarkdownSyntaxInserter) {
    val replacement = inserter.insertInto(text)
    text = text.copy(
      text = replacement.text.toString(),
      selection = replacement.newSelection
    )
  }
}
