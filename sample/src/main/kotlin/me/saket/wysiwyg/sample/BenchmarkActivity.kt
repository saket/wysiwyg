@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)

package me.saket.wysiwyg.sample

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import me.saket.wysiwyg.WsyiwygTextField
import me.saket.wysiwyg.parser.flexmark.FlexmarkMarkdownParser
import me.saket.wysiwyg.rememberWysiwyg

/** Driven by `:benchmarks` via UiAutomator. */
class BenchmarkActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)

    val markdown = assets.open(this.intent.getStringExtra(MarkdownAssetPath)!!)
      .bufferedReader()
      .use { it.readText().take(50_000) }

    setContent {
      AppTheme {
        Surface {
          val modifier = Modifier
            .semantics { testTagsAsResourceId = true }
            .fillMaxSize()
            .systemBarsPadding()
            .testTag("editor")
          val contentPadding = PaddingValues(16.dp)

          if (this.intent.getBooleanExtra(UseWysiwygTextEditor, false)) {
            WysiwygEditor(
              modifier = modifier,
              markdown = markdown,
              contentPadding = contentPadding,
            )
          } else {
            BasicTextField(
              state = rememberTextFieldState(initialText = markdown),
              modifier = modifier.padding(contentPadding),
            )
          }
        }
      }
    }
  }

  @Composable
  private fun WysiwygEditor(
    markdown: String,
    modifier: Modifier,
    contentPadding: PaddingValues,
  ) {
    val wysiwyg = rememberWysiwyg(
      textState = rememberTextFieldState(initialText = markdown),
      theme = wysiwygTheme(),
      parser = remember { FlexmarkMarkdownParser() },
    )
    WsyiwygTextField(
      modifier = modifier,
      wysiwyg = wysiwyg,
      cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
      textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
      contentPadding = contentPadding,
    )
  }

  companion object {
    const val UseWysiwygTextEditor = "use_wysiwyg"
    const val MarkdownAssetPath = "asset_path"
  }
}
