package me.saket.wysiwyg.parser.treesitter

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import androidx.activity.ComponentActivity
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.dropbox.dropshots.Dropshots
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.rememberWysiwyg
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TreeSitterMarkdownParserTest {
  // Workaround for https://github.com/dropbox/dropshots/issues/74.
  @get:Rule val permissionRule = GrantPermissionRule.grant(WRITE_EXTERNAL_STORAGE)!!
  @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()
  @get:Rule val dropshots = Dropshots()

  @Before fun setUp() {
    val metrics = InstrumentationRegistry.getInstrumentation()
      .targetContext
      .resources
      .displayMetrics
    check(metrics.widthPixels == 1080 && metrics.heightPixels == 2424) {
      "wysiyg's snapshots were generated on a 1080x2424 device, but this one is $metrics."
    }
  }

  @Test fun canary() {
    composeRule.setContent {
      val textState = rememberTextFieldState(
        initialText = """
          |# Wysiwyg
          |
          |Markdown is a **lightweight** and easy-to-use `syntax` for styling all forms of ~~web~~ writing.
          |> The overriding design goal for Markdown's formatting syntax is to make it as readable as possible.
          |---
          |Markdown was originally developed by [John Gruber](daringfireball.net/markdown).
          """.trimMargin(),
      )
      val wysiwyg = rememberWysiwyg(
        textState = textState,
        theme = wysiwygTheme(),
        markdownParser = remember { TreeSitterMarkdownParser() },
      )

      MaterialTheme {
        BasicTextField(
          modifier = Modifier,
          state = textState,
          inputTransformation = wysiwyg.inputTransformation,
          outputTransformation = wysiwyg.outputTransformation,
          cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
          textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
        )
      }
    }
    composeRule.runOnIdle {
      dropshots.assertSnapshot(composeRule.activity)
    }
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
    spoilersBackground = Color.Transparent,
    spoilersTextColor = MaterialTheme.colorScheme.error,
    codeBackground = LocalContentColor.current.copy(alpha = 0.1f),
    codeBlockLeadingPadding = 16.sp,
    blockQuoteText = LocalContentColor.current.copy(alpha = 0.9f),
    blockQuoteLeadingPadding = 16.sp,
    listBlockLeadingPadding = 16.sp,
  )
}
