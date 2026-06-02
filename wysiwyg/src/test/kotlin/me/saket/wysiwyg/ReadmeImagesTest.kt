package me.saket.wysiwyg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import me.saket.touchrobot.onNode
import me.saket.touchrobot.rememberTouchRobot
import me.saket.wysiwyg.parser.flexmark.FlexmarkMarkdownParser
import org.junit.Rule
import org.junit.Test

/** Generates the animations and screenshots embedded in the project's README. */
class ReadmeImagesTest {
  @get:Rule val paparazzi = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5,
    renderingMode = SessionParams.RenderingMode.SHRINK,
  )

  @Test fun checkboxes() {
    paparazzi.gif(end = 2600, fps = 20) {
      val wysiwyg = rememberWysiwyg(
        textState = TextFieldState(
          """
            |Task lists are interactive. Tapping a checkbox toggles its marker between `[ ]` and `[x]`.
            |
            |- [ ] Milk
            |- [ ] Eggs
            |- [x] Coffee
          """.trimMargin()
        ),
        parser = remember {
          FlexmarkMarkdownParser(dispatcher = Dispatchers.Unconfined)
        },
      )
      Scaffold {
        WysiwygTextField(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("editor"),
          wysiwyg = wysiwyg,
          contentPadding = PaddingValues(16.dp),
          theme = ReadmeTheme,
          textStyle = ReadmeTextStyle,
        )
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        touchRobot.onNode(hasTestTag("editor")).performGesture {
          delay(400)
          repeat(times = 3) { lineIndex ->
            click(wysiwyg.findTaskCheckboxCoordinates(lineIndex))
            delay(500)
          }
        }
      }
    }
  }

  @Test fun `auto complete list items on enter`() {
    // The editor is seeded with a caption paragraph and an opening "1. ". Each '\n'
    // below triggers the on-enter formatter, which continues the list with the next number.
    val typed = "First\nSecond\nThird"
    val delayPerChar = 150L
    val textState = TextFieldState(
      """
        |Pressing enter continues the list, numbers and all.
        |
        |1.${" "}
      """.trimMargin()
    )

    // The typing coroutine lags the gif's frame clock, so give the window enough
    // headroom to finish typing and hold on the completed list for a beat.
    paparazzi.gif(end = AutoFormatScreenshotDuration, fps = 20) {
      val wysiwyg = rememberWysiwyg(
        textState = textState,
        parser = remember {
          FlexmarkMarkdownParser(dispatcher = Dispatchers.Unconfined)
        },
      )
      val focusRequester = remember { FocusRequester() }

      Scaffold {
        WysiwygTextField(
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("editor"),
          wysiwyg = wysiwyg,
          contentPadding = PaddingValues(16.dp),
          theme = ReadmeTheme,
          textStyle = ReadmeTextStyle,
        )
      }

      LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        textState.type(typed, wysiwyg.inputTransformation, delayPerChar)
      }
    }
  }

  @Test fun `auto close code blocks on enter`() {
    // After the caption paragraph, pressing enter right after an opening code fence
    // inserts the closing fence below and parks the cursor between them, ready for code.
    val typed = "```kotlin\nfun helloWorld()"
    val delayPerChar = 120L
    val textState = TextFieldState(
      """
        |Pressing enter closes the code block for you.
        |
        |
      """.trimMargin()
    )

    paparazzi.gif(end = AutoFormatScreenshotDuration, fps = 20) {
      val wysiwyg = rememberWysiwyg(
        textState = textState,
        parser = remember {
          FlexmarkMarkdownParser(dispatcher = Dispatchers.Unconfined)
        },
      )
      val focusRequester = remember { FocusRequester() }

      Scaffold {
        WysiwygTextField(
          modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .testTag("editor"),
          wysiwyg = wysiwyg,
          contentPadding = PaddingValues(16.dp),
          theme = ReadmeTheme,
          textStyle = ReadmeTextStyle,
        )
      }

      LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        textState.type(typed, wysiwyg.inputTransformation, delayPerChar)
      }
    }
  }

  /** Types [text] one character at a time at the cursor, running [inputTransformation] per keystroke. */
  private suspend fun TextFieldState.type(
    text: String,
    inputTransformation: InputTransformation,
    delayPerChar: Long,
  ) {
    for (nextChar in text) {
      edit {
        val cursor = selection.start
        replace(cursor, cursor, nextChar.toString())
        selection = TextRange(cursor + 1)
        with(inputTransformation) {
          transformInput()
        }
      }
      delay(delayPerChar)
    }
  }

  @Composable
  private fun Scaffold(
    content: @Composable () -> Unit,
  ) {
    Box(
      Modifier
        .fillMaxWidth()
        .heightIn(min = 300.dp)
        .background(Color(0xFFFAFAF0))
    ) {
      content()
    }
  }

  companion object {
    // Used for screenshots shown together so that they loop at the same time.
    val AutoFormatScreenshotDuration = 5_000L

    val ReadmeTextStyle = TextStyle(
      fontSize = 18.sp,
      lineHeight = 1.7.em,
      letterSpacing = 0.5.sp,
    )

    val ReadmeTheme = WysiwygTheme.GithubLight.copy(
      markerColor = Color(0xFF6B5F27),
      codeBackground = Color(0xFFDCE7C7),
    )
  }
}
