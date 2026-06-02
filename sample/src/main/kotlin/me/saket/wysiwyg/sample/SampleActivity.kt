package me.saket.wysiwyg.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fitInside
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import me.saket.wysiwyg.WysiwygTextField
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.rememberWysiwyg

class SampleActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    setContent {
      AppTheme {
        Surface {
          WysiwygEditor(
            Modifier.fillMaxSize(),
          )
        }
      }
    }
  }
}

@Composable
private fun WysiwygEditor(
  modifier: Modifier = Modifier,
) {
  val textState = rememberTextFieldState(
    initialText =
      """
        |# Wysiwyg
        |
        |A markdown editor that styles text *as you type*, so you never have to guess what your **bold** and `code` will look like.
        |
        |> The single biggest source of inspiration for Markdown's syntax is the format of plain text email.
        |
        |That's how [John Gruber](daringfireball.net/markdown) described his goal when he created Markdown in ~~2003~~ 2004, with help from Aaron Swartz.
        """.trimMargin(),
  )

  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(focusRequester) {
    delay(50) // Workaround for https://issuetracker.google.com/issues/199631318.
    focusRequester.requestFocus()
  }

  val wysiwyg = rememberWysiwyg(
    textState = textState,
  )

  Column(modifier) {
    Box(
      Modifier
        .fillMaxWidth()
        .weight(1f)
        .fitInside(WindowInsetsRulers.SystemBars.current)
        .fitInside(WindowInsetsRulers.Ime.current),
    ) {
      WysiwygTextField(
        modifier = Modifier.focusRequester(focusRequester),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        wysiwyg = wysiwyg,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = TextStyle(
          fontSize = 16.sp,
          lineHeight = 1.7.em,
          letterSpacing = 0.5.sp,
          color = LocalContentColor.current,
        ),
        theme = WysiwygTheme.Github.copy(
          markerColor = MaterialTheme.colorScheme.tertiary,
          codeBackground = MaterialTheme.colorScheme.secondaryContainer,
        ),
      )
    }

    MarkdownFormattingBar(
      modifier = Modifier.fillMaxWidth(),
      state = textState,
    )
  }
}

@Composable
internal fun AppTheme(content: @Composable () -> Unit) {
  val colorScheme = if (isSystemInDarkTheme()) {
    dynamicDarkColorScheme(LocalContext.current)
  } else {
    dynamicLightColorScheme(LocalContext.current)
  }
  MaterialTheme(colorScheme) {
    content()
  }
}
