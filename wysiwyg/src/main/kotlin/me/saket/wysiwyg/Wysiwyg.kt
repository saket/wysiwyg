package me.saket.wysiwyg

import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.StateFlow
import me.saket.wysiwyg.format.OnEnterMarkdownFormatters
import me.saket.wysiwyg.internal.RealWysiwyg
import me.saket.wysiwyg.parser.MarkdownDocument
import me.saket.wysiwyg.parser.MarkdownParser

@Composable
fun rememberWysiwyg(
  textState: TextFieldState,
  parser: MarkdownParser,
  // todo: should this still be offered instead of providing OnEnterMarkdownFormatters as an InputTransformation?
  onEnterFormatters: OnEnterMarkdownFormatters = OnEnterMarkdownFormatters.Default,
): Wysiwyg {
  val coroutineScope = rememberCoroutineScope()
  return remember(textState, parser, onEnterFormatters) {
    RealWysiwyg(
      textState = textState,
      parser = parser,
      onEnterFormatters = onEnterFormatters,
      coroutineScope = coroutineScope,
    )
  }
}

interface Wysiwyg {
  // todo: should this be here considering wysiwyg simply exposes it back?
  val textState: TextFieldState

  // todo: kdoc + review name
  val documents: StateFlow<MarkdownDocument>

  // todo: onEnterFormatters should probably be mutable.
  val inputTransformation: InputTransformation
}
