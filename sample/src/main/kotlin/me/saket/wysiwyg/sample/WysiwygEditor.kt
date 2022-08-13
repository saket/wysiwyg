package me.saket.wysiwyg.sample

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.saket.extendedspans.ExtendedSpans
import me.saket.extendedspans.RoundedCornerSpanPainter
import me.saket.extendedspans.drawBehind
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.parser.FlexmarkMarkdownParser
import me.saket.wysiwyg.rememberWysiwyg
import me.saket.wysiwyg.sample.extensions.RedditSpoilersExtension
import me.saket.wysiwyg.sample.extensions.RedditSuperscriptExtension

@Composable
fun WysiwygEditor() {
  val wysiwyg = rememberWysiwyg(
    theme = wysiwygTheme(),
    markdownParser = remember {
      FlexmarkMarkdownParser(
        RedditSuperscriptExtension(),
        RedditSpoilersExtension()
      )
    },
    initialText = {
      val text = """
          |The greatest thing you'll ever learn is just to >!reddit and be reddited in return!<.
          |
          |**Shopping list**
          |1. Milk
          """.trimMargin()

      // |This is a ^superscript^. This is also a ^(superscript but with multiple words). This^looks^interesting^on^old^reddit.
      /*
        |
        |Markdown is a **lightweight** and easy-to-use `syntax` for styling all forms of ~~web~~ writing.
        |> The overriding design goal for Markdown's formatting syntax is to make it as readable as possible.
        |
        |Markdown was originally developed by [John Gruber](daringfireball.net/markdown).
        * */
      TextFieldValue(text, selection = TextRange(text.length))
    },
  )
  val extendedSpans = remember {
    ExtendedSpans(
      RoundedCornerSpanPainter(
        cornerRadius = 4.sp,
        padding = RoundedCornerSpanPainter.TextPaddingValues(horizontal = 2.sp),
        topMargin = 2.sp,
        bottomMargin = 2.sp,
        stroke = null,
      )
    )
  }

  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(focusRequester) {
    delay(50) // Workaround for https://issuetracker.google.com/issues/199631318.
    focusRequester.requestFocus()
  }

  Column {
    BasicTextField(
      modifier = Modifier
        .focusRequester(focusRequester)
        .fillMaxWidth()
        .weight(1f)
        .padding(24.dp)
        .drawBehind(extendedSpans),
      value = wysiwyg.text().let {
        it.copy(annotatedString = extendedSpans.extend(it.annotatedString))
      },
      onValueChange = wysiwyg::onTextChange,
      onTextLayout = { result ->
        extendedSpans.onTextLayout(result)
      },
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
    )

    MarkdownFormattingBar(
      modifier = Modifier.fillMaxWidth(),
      wysiwyg = wysiwyg
    )
  }
}

@Composable
private fun wysiwygTheme(): WysiwygTheme {
  return WysiwygTheme(
    syntaxColor = MaterialTheme.colorScheme.primary,
    linkTextColor = MaterialTheme.colorScheme.primary,
    linkUrlColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
    struckThroughTextColor = LocalContentColor.current.copy(alpha = 0.5f),
    spoilersBackground = Color.Transparent,
    spoilersTextColor = MaterialTheme.colorScheme.error,
    codeBackground = Color.Black.copy(alpha = 0.3f),
    codeBlockLeadingPadding = 16.sp,
    blockQuoteText = LocalContentColor.current.copy(alpha = 0.9f),
    blockQuoteLeadingPadding = 16.sp,
    listBlockLeadingPadding = 16.sp,
  )
}
