package me.saket.wysiwyg

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import me.saket.wysiwyg.format.OnEnterMarkdownFormatters
import me.saket.wysiwyg.internal.RealWysiwyg
import me.saket.wysiwyg.parser.MarkdownParser

@Composable
fun rememberWysiwyg(
  textState: TextFieldState,
  theme: WysiwygTheme,
  parser: MarkdownParser,
  // todo: should this still be offered instead of providing OnEnterMarkdownFormatters as an InputTransformation?
  onEnterFormatters: OnEnterMarkdownFormatters = OnEnterMarkdownFormatters.Default,
): Wysiwyg {
  val wysiwyg = remember(textState, theme, parser, onEnterFormatters) {
    RealWysiwyg(
      textState = textState,
      theme = theme,
      parser = parser,
      onEnterFormatters = onEnterFormatters,
    )
  }
  LaunchedEffect(wysiwyg) {
    wysiwyg.syncOutputTransformationWithText()
  }
  return wysiwyg
}

interface Wysiwyg {
  // todo: should this be here considering wysiwyg simply exposes it back?
  val textState: TextFieldState

  val theme: WysiwygTheme

  // todo: onEnterFormatters should probably be mutable.
  val inputTransformation: InputTransformation

  // todo: this is incredibly slow for large text. typing experience is bad.
  val outputTransformation: MarkdownOutputTransformation
}
