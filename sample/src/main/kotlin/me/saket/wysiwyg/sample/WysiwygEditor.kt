@file:OptIn(ExperimentalMaterial3Api::class)

package me.saket.wysiwyg.sample

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.saket.wysiwyg.WsyiwygTextField
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.highlight.flexmark.FlexmarkMarkdownParser
import me.saket.wysiwyg.rememberWysiwyg

@Composable
fun WysiwygEditor(
  modifier: Modifier = Modifier,
) {
  val textState = rememberTextFieldState(
    initialText =
      """
        |# Wysiwyg
        |
        |Markdown is a **lightweight** and easy-to-use `syntax` for styling all forms of ~~web~~ writing.
        |> The overriding design goal for Markdown's formatting syntax is to make it as readable as possible.
        |---
        |Markdown was originally developed by [John Gruber](daringfireball.net/markdown).
        """.trimMargin(),
  )

  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(focusRequester) {
    delay(50) // Workaround for https://issuetracker.google.com/issues/199631318.
    focusRequester.requestFocus()
  }

  val wysiwyg = rememberWysiwyg(
    textState = textState,
    theme = wysiwygTheme(),
    parser = remember { FlexmarkMarkdownParser() },
  )

  Column(modifier) {
    Box(
      Modifier
        .fillMaxWidth()
        .weight(1f)
        .fitInside(WindowInsetsRulers.SystemBars.current)
        .fitInside(WindowInsetsRulers.Ime.current)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
    ) {
      WsyiwygTextField(
        modifier = Modifier.focusRequester(focusRequester),
        wysiwyg = wysiwyg,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
      )
    }

    MarkdownFormattingBar(
      modifier = Modifier.fillMaxWidth(),
      state = textState,
    )
  }
}

@Composable
private fun wysiwygTheme(): WysiwygTheme {
  return WysiwygTheme(
    markerColor = MaterialTheme.colorScheme.tertiary,
    headingColor = MaterialTheme.colorScheme.primary,
    linkTextColor = MaterialTheme.colorScheme.primary,
    linkUrlColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
    struckThroughTextColor = LocalContentColor.current.copy(alpha = 0.5f),
    codeBackground = LocalContentColor.current.copy(alpha = 0.1f),
    codeBlockLeadingPadding = 16.sp,
    blockQuoteText = LocalContentColor.current.copy(alpha = 0.9f),
    blockQuoteLeadingPadding = 16.sp,
    listBlockLeadingPadding = 16.sp,
  )
}
