package me.saket.wysiwyg

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import me.saket.wysiwyg.format.OnEnterMarkdownFormatters
import me.saket.wysiwyg.parser.flexmark.FlexmarkMarkdownParser

@Composable
fun rememberWysiwyg(
  textState: TextFieldState,
  onEnterFormatters: OnEnterMarkdownFormatters = OnEnterMarkdownFormatters.Default,
): Wysiwyg {
  return rememberWysiwyg(
    textState = textState,
    parser = remember { FlexmarkMarkdownParser() },
    onEnterFormatters = onEnterFormatters,
  )
}
