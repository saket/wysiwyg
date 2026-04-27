package me.saket.wysiwyg

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import kotlinx.coroutines.Dispatchers
import me.saket.wysiwyg.highlight.flexmark.FlexmarkMarkdownParser
import org.junit.Rule
import org.junit.Test

class WysiwygTest {
  @get:Rule val paparazzi = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5,
    renderingMode = SessionParams.RenderingMode.SHRINK,
  )

  @Test fun canary() {
    paparazzi.snapshot {
      WysiwygEditor(
        markdown = """
          |# Wysiwyg
          |
          |Markdown is a **lightweight** and easy-to-use `syntax` for styling all forms of ~~web~~ writing.
          |> The overriding design goal for Markdown's formatting syntax is to make it as readable as possible.
          |---
          |Markdown was originally developed by [John Gruber](daringfireball.net/markdown).
          """.trimMargin(),
      )
    }
  }

  @Test fun `unicode text`() {
    paparazzi.snapshot {
      WysiwygEditor(
        markdown = """
          |# Héllo café
          |
          |Markdown with **bold café** and `résumé` and ~~émoji~~.
          |> Déjà vu — **context** matters.
          |---
          |By [André](example.com).
          """.trimMargin(),
      )
    }
  }

  @Test fun `list items without leading space`() {
    paparazzi.snapshot {
      WysiwygEditor(
        markdown = """
          |1.
          |*
          |+
          |-
          |
          |None of the above should render as list items since each marker is missing the space that separates it from its content.
          """.trimMargin(),
      )
    }
  }

  @Test fun `blank list item`() {
    paparazzi.snapshot {
      WysiwygEditor(
        markdown = """
          |1.${"    "}
          |
          |Although the list item above is blank, its indentation should not extend into this paragraph.
          """.trimMargin(),
      )
    }
  }

  @Test fun `list items`() {
    paparazzi.snapshot {
      WysiwygEditor(
        markdown = """
          |1.    Milk
          |2. Mangoes
          |3. Notebooks
          |
          |Unrelated text.
          """.trimMargin(),
      )
    }
  }

  @Test fun `invalid headings`() {
    paparazzi.snapshot {
      WysiwygEditor(
        """
          |#Heading
          |
          |^ Headings should always have a space after their opening marker (`#`).
          |
          |#
          |
          |^ A bare `#` isn't followed by content, so it shouldn't render as a heading.
          |""".trimMargin()
      )
    }
  }

  @Test fun `invalid block quotes`() {
    paparazzi.snapshot {
      WysiwygEditor(
        """
          |Empty marker:
          |
          |>${""}
          |
          |Marker with whitespaces:
          |
          |>${" "}
          |
          |Quote without leading whitespaces:
          |
          |Marker without a leading newline:
          |> Quote
          |
          |^ A bare `#` isn't followed by content, so it shouldn't render as a heading.
          |""".trimMargin()
      )
    }
  }

  @Test fun `valid block quotes`() {
    paparazzi.snapshot {
      WysiwygEditor(
        """
          |>Quote without leading whitespaces.
          |
          |> Quote with a leading whitespace.
          |""".trimMargin()
      )
    }
  }

  @Test fun `valid headings`() {
    paparazzi.snapshot {
      WysiwygEditor(
        """
        |# H1
        |
        |## H2
        |
        |### H3
        |
        |#### H4
        |
        |##### H5
        |
        |###### H6
        |""".trimMargin()
      )
    }
  }

  @Test fun urls() {
    paparazzi.snapshot {
      WysiwygEditor(
        """
        |[Project docs](https://example.com/docs)
        |
        |[Quarterly report](<docs/quarterly report.pdf>)
        |
        |[Team homepage](https://example.com "Open the team website")
        |
        |[Release notes](<docs/release notes.md> "Read offline")
        |""".trimMargin()
      )
    }
  }
}

@Composable
private fun WysiwygEditor(markdown: String) {
  MaterialTheme {
    Surface(
      Modifier
        .fillMaxWidth()
        .heightIn(min = 300.dp)
    ) {
      WsyiwygTextField(
        modifier = Modifier.padding(16.dp),
        wysiwyg = rememberWysiwyg(
          textState = rememberTextFieldState(markdown),
          theme = wysiwygTheme(),
          parser = remember {
            FlexmarkMarkdownParser(dispatcher = Dispatchers.Unconfined)
          },
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
      )
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
