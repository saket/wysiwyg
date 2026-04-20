package me.saket.wysiwyg

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import me.saket.wysiwyg.format.OnEnterMarkdownFormatters
import me.saket.wysiwyg.parser.FlexmarkMarkdownParser
import me.saket.wysiwyg.parser.MarkdownParser

@Composable
fun rememberWysiwyg(
  theme: WysiwygTheme,
  markdownParser: MarkdownParser = FlexmarkMarkdownParser(),
  onEnterFormatters: OnEnterMarkdownFormatters = OnEnterMarkdownFormatters.Default,
  initialText: () -> String = { "" },
): Wysiwyg {
  val state = rememberTextFieldState(initialText = initialText())
  return remember(state, theme, markdownParser, onEnterFormatters) {
    Wysiwyg(
      state = state,
      theme = theme,
      parser = markdownParser,
      onEnterFormatters = onEnterFormatters,
    )
  }
}

@Stable
class Wysiwyg internal constructor(
  val state: TextFieldState,
  val theme: WysiwygTheme,
  private val parser: MarkdownParser,
  onEnterFormatters: OnEnterMarkdownFormatters,
) {
  val inputTransformation: InputTransformation = onEnterFormatters.asInputTransformation()
}
