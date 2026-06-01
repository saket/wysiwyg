package me.saket.wysiwyg

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
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
            |# Shopping list
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
      Box(
        Modifier
          .fillMaxWidth()
          .heightIn(min = 300.dp)
          .background(Color(0xFFFAFAF0))
      ) {
        WysiwygTextField(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("editor"),
          wysiwyg = wysiwyg,
          contentPadding = PaddingValues(16.dp),
          theme = WysiwygTheme.GithubLight.copy(
            markerColor = Color(0xFF6B5F27),
          ),
          textStyle = TextStyle(
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.5.sp,
          ),
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
}
