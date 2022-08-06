package me.saket.wysiwyg.sample

import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.FormatBold
import androidx.compose.material.icons.twotone.FormatItalic
import androidx.compose.material.icons.twotone.FormatQuote
import androidx.compose.material.icons.twotone.Tag
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import kotlinx.coroutines.delay
import me.saket.extendedspans.ExtendedSpans
import me.saket.extendedspans.RoundedCornerSpanPainter
import me.saket.extendedspans.RoundedCornerSpanPainter.TextPaddingValues
import me.saket.extendedspans.drawBehind
import me.saket.wysiwyg.Wysiwyg
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.format.insertBlockQuoteSyntax
import me.saket.wysiwyg.format.insertBoldSyntax
import me.saket.wysiwyg.format.insertHeadingSyntax
import me.saket.wysiwyg.format.insertItalicSyntax
import me.saket.wysiwyg.rememberWysiwyg

class SampleActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // For animated IME insets.
    WindowCompat.setDecorFitsSystemWindows(window, false)

    setContent {
      PreviewScaffold {
        WysiwygPreview()
      }
    }
  }
}

@Composable
fun WysiwygPreview() {
  val wysiwyg = rememberWysiwyg(wysiwygTheme()) {
    val text = """
          |# Shopping list
          |
          |
          """.trimMargin()
    TextFieldValue(text, selection = TextRange(text.length))
  }
  val extendedSpans = remember {
    ExtendedSpans(
      RoundedCornerSpanPainter(
        cornerRadius = 4.sp,
        padding = TextPaddingValues(horizontal = 2.sp),
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
    linkUrlColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
    codeBackground = Color.Black.copy(alpha = 0.3f),
    codeBlockLeadingPadding = 16.sp,
    blockQuoteText = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
    blockQuoteLeadingPadding = 16.sp,
    listBlockLeadingPadding = 16.sp,
  )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PreviewScaffold(content: @Composable () -> Unit) {
  val darkTheme = isSystemInDarkTheme()
  val colorScheme = when {
    SDK_INT >= 31 -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> darkColorScheme()
    else -> lightColorScheme()
  }
  MaterialTheme(colorScheme) {
    Scaffold { insetPadding ->
      Box(
        Modifier
          .fillMaxSize()
          .padding(insetPadding)
          .systemBarsPadding()
          .imePadding()
      ) {
        content()
      }
    }
  }
}

@Composable
fun MarkdownFormattingBar(
  wysiwyg: Wysiwyg,
  modifier: Modifier = Modifier
) {
  Row(
    modifier
      .clipToBounds()
      .background(MaterialTheme.colorScheme.surfaceColorAtElevation(10.dp))
      .horizontalScroll(rememberScrollState())
  ) {
    FormatButton(
      icon = Icons.TwoTone.FormatBold,
      contentDescription = "Insert bold markdown",
      onClick = { wysiwyg.insertBoldSyntax() }
    )
    FormatButton(
      icon = Icons.TwoTone.FormatItalic,
      contentDescription = "Insert italic markdown",
      onClick = { wysiwyg.insertItalicSyntax() }
    )
    FormatButton(
      icon = Icons.TwoTone.FormatQuote,
      contentDescription = "Insert block quote",
      onClick = { wysiwyg.insertBlockQuoteSyntax() }
    )
    FormatButton(
      icon = Icons.TwoTone.Tag,
      contentDescription = "Insert heading markdown",
      onClick = { wysiwyg.insertHeadingSyntax() }
    )
  }
}

@Composable
fun FormatButton(
  onClick: () -> Unit,
  icon: ImageVector,
  contentDescription: String
) {
  Box(
    Modifier
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(bounded = false),
        onClick = onClick
      )
      .padding(12.dp)
      .size(24.dp)
  ) {
    Image(
      modifier = Modifier.matchParentSize(),
      painter = rememberVectorPainter(icon),
      contentDescription = contentDescription,
      colorFilter = ColorFilter.tint(LocalContentColor.current)
    )
  }
}
