package me.saket.wysiwyg

import android.view.ViewGroup.LayoutParams
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.startsWith
import com.android.ide.common.rendering.api.SessionParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import me.saket.touchrobot.onNode
import me.saket.touchrobot.rememberTouchRobot
import me.saket.wysiwyg.parser.flexmark.FlexmarkMarkdownParser
import org.junit.Rule
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

class WysiwygTest {
  @get:Rule val paparazzi = Paparazzi(
    deviceConfig = DeviceConfig.PIXEL_5,
    renderingMode = SessionParams.RenderingMode.SHRINK,
  )

  @Test fun canary() {
    paparazzi.snapshot {
      Scaffold {
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
  }

  @Test fun `unicode text`() {
    paparazzi.snapshot {
      Scaffold {
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
  }

  @Test fun `line and paragraph spacings`() {
    paparazzi.snapshot {
      Scaffold {
        WysiwygEditor(
          markdown = """
          |First line that's so long it wraps to multiple lines in the editor.
          |Second line that's on a new visual line, but still in the same paragraph.
          |
          |Third line that's separated by a blank line, so it's in its own paragraph.
          """.trimMargin(),
        )
      }
    }
  }

  @Test fun `paragraph spacings of list blocks`() {
    paparazzi.snapshot {
      Scaffold {
        Row(Modifier.height(IntrinsicSize.Max)) {
          val markdown = """
            |List block that isn't separated by a blank line:
            |1. Milk
            |2. Mangoes
            |3. Peaches
            |
            |List block separated by a new line:
            |
            |1. Milk
            |2. Mangoes
            |3. Peaches
            """.trimMargin()

          val contentPadding = PaddingValues(16.dp)

          WysiwygEditor(
            modifier = Modifier.weight(1f),
            markdown = markdown,
            contentPadding = contentPadding,
          )

          VerticalDivider()

          BasicTextField(
            modifier = Modifier
              .weight(1f)
              .padding(contentPadding),
            state = rememberTextFieldState(markdown),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
          )
        }
      }
    }
  }

  @Test fun `fenced code blocks`() {
    paparazzi.snapshot {
      Scaffold {
        WysiwygEditor(
          markdown = """
            |Fenced code block that isn't separated by a blank line:
            |```
            |fun greet(name: String) {
            |  println("Hello, ${'$'}name!")
            |}
            |```
            |Fenced code block separated by a new line:
            |
            |```
            |fun greet(name: String) {
            |  println("Hello, ${'$'}name!")
            |}
            |```
            """.trimMargin(),
        )
      }
    }
  }

  @Test fun `edge cases with paragraph spacings of blocks`() {
    paparazzi.snapshot {
      Scaffold {
        WysiwygEditor(
          markdown = """
            |1. List at the very start of the document
            |2. so there's no preceding paragraph
            |
            |# Heading
            |1. List immediately after a heading
            |2. without a blank line in between
            |
            |> Block quote
            |1. List immediately after a block quote
            |2. without a blank line in between
            """.trimMargin(),
        )
      }
    }
  }

  @Test fun `list items without leading space`() {
    paparazzi.snapshot {
      Scaffold {
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
  }

  @Test fun `blank list item`() {
    paparazzi.snapshot {
      Scaffold {
        WysiwygEditor(
          markdown = """
          |1.${"    "}
          |
          |Although the list item above is blank, its indentation should not extend into this paragraph.
          """.trimMargin(),
        )
      }
    }
  }

  @Test fun `list items`() {
    paparazzi.snapshot {
      Scaffold {
        WysiwygEditor(
          markdown = """
          |1.    Milk
          |2. Mangoes
          |3. Notebooks
          |4. A list item so long it overflows to the next line, then keeps going for a third line just to be sure the indent holds
          |
          |Unrelated text.
          """.trimMargin(),
        )
      }
    }
  }

  @Test fun `task list items`() {
    paparazzi.snapshot {
      Scaffold {
        WysiwygEditor(
          markdown = """
            |### Unordered list
            |- [ ] Buy milk
            |- [x] Write blog post
            |- [X] Ship release
            |- [ ] A task item so long it overflows to the next line, then keeps going for a third line just to be sure the indent holds
            |- [ ]${" "}
            |
            |### Ordered list
            |1. [ ] Alphonso
            |2. [x] Kesar
            |3. [ ] Malda
            """.trimMargin(),
        )
      }
    }
  }

  @Test fun `tap toggles task list checkbox`() {
    paparazzi.gif(end = 3000) {
      Scaffold {
        WysiwygEditor(
          modifier = Modifier.height(300.dp),
          markdown = """
          |- [ ] Buy milk
          |- [x] Write blog post
          |- [ ] Refactor parser
          |- [X] Ship release
          |- [ ] Long task with a description that wraps to multiple visual lines so the document grows past the visible viewport
          |- [ ] Another long task with a verbose description spanning multiple lines to keep adding vertical space
          |- [ ] Yet another long task whose text wraps to several lines so the document content extends past two viewport heights
          |- [ ] One more long task pushing the document content well below the visible area so the final item is far offscreen
          |- [ ] Final task at the bottom of the document
          """.trimMargin(),
        )
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        touchRobot.onNode(hasTestTag("editor")).performGesture {
          // Toggle the first four items at the top of the document.
          repeat(times = 4) { lineIndex ->
            click(IntOffset(x = 153, y = 77 + lineIndex * 66))
            delay(200)
          }
          // Scroll the final task into view, then tap it. The press indicator's
          // bounds are computed at press time using the current layout, so they
          // must reflect the post-scroll position, not the doc-space coords.
          swipe(start = bottomCenter, stop = topCenter, duration = 600.milliseconds)
          delay(300)
          click(IntOffset(x = 153, y = 700))
        }
      }
    }
  }

  // todo: this test is failing because of the blinking cursor. maybe the cursor can be made fixed using LocalCursorBlinkEnabled.
  @Test fun `tapping a checkbox does not move the cursor`() {
    val markdown = """
      |- [ ] Buy milk
      |- [ ] Write blog post
      |
      |Cursor sits here at the end.
    """.trimMargin()
    val textState = TextFieldState(
      initialText = markdown,
      initialSelection = TextRange(markdown.length),
    )

    paparazzi.gif(end = 1200) {
      val focusRequester = remember { FocusRequester() }
      Scaffold {
        WysiwygEditor(
          modifier = Modifier.focusRequester(focusRequester),
          markdown = markdown,
          textState = textState,
        )
      }

      LaunchedEffect(focusRequester) {
        focusRequester.requestFocus()
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        // Hold for a beat so the cursor blink at the document's end is captured
        // in the recording before the tap fires.
        delay(500)
        touchRobot.onNode(hasTestTag("editor")).performGesture {
          click(IntOffset(x = 153, y = 77))
        }
      }
    }

    // The tap should toggle the first checkbox without moving the cursor away from
    // the document's end. If our handler hadn't consumed the up event, BasicTextField
    // would have placed the cursor at the tapped offset.
    assertThat(textState.text.toString()).startsWith("- [x] Buy milk")
    assertThat(textState.selection).isEqualTo(TextRange(textState.text.length))
  }

  @Test fun `swipe starting on a checkbox scrolls the document`() {
    paparazzi.gif(end = 1200) {
      Scaffold {
        WysiwygEditor(
          modifier = Modifier.height(300.dp),
          markdown = """
          |- [ ] First task at the top of the document
          |- [ ] Second task
          |- [ ] Third task
          |- [ ] Fourth task
          |- [ ] Fifth task
          |- [ ] Sixth task
          |
          |Body paragraph one. Filler text to push the document past the visible viewport.
          |
          |Body paragraph two. More prose so the scroll has somewhere to go.
          |
          |Body paragraph three. Yet more text giving the user a meaningful scroll range.
          |
          |Body paragraph four. Filler. Filler. Filler. Filler. Filler. Filler. Filler.
          |
          |Body paragraph five. The bottom of the document, only visible after scrolling.
          """.trimMargin(),
        )
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        touchRobot.onNode(hasTestTag("editor")).performGesture {
          // Swipe upward from inside the last visible checkbox, far enough to scroll
          // past the task list. If the tap handler mistakenly consumed the down,
          // scroll wouldn't engage and the document would stay put.
          swipe(
            start = IntOffset(x = 153, y = 77 + 5 * 66),
            stop = IntOffset(x = 153, y = -600),
            duration = 600.milliseconds,
          )
        }
      }
    }
  }

  @Test fun `invalid headings`() {
    paparazzi.snapshot {
      Scaffold {
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
  }

  @Test fun `invalid block quotes`() {
    paparazzi.snapshot {
      Scaffold {
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
  }

  @Test fun `valid block quotes`() {
    paparazzi.snapshot {
      Scaffold {
        WysiwygEditor(
          """
          |>Quote without leading whitespaces.
          |
          |> Quote with a leading whitespace.
          |""".trimMargin()
        )
      }
    }
  }

  @Test fun `valid headings`() {
    paparazzi.snapshot {
      Scaffold {
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
  }

  @Test fun urls() {
    paparazzi.snapshot {
      Scaffold {
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

  @Test fun `extended spans are synced with scroll position`() {
    paparazzi.gif(end = 1500) {
      Scaffold {
        WysiwygEditor(
          modifier = Modifier.height(400.dp),
          markdown = """
          |# Scrollable document
          |
          |Intro paragraph with **bold** and `inline code` for testing.
          |
          |> A block quote whose vertical marker should follow the text as it scrolls.
          |> Continuing the same quote across multiple lines so the marker is tall enough to notice.
          |
          |---
          |
          |After the thematic break, more content with `more code` and **bold**.
          |
          |Filler paragraph to push the document past the visible area.
          |Another paragraph with `code spans` interleaved with prose.
          |
          |> Quote near the bottom of the document, also spanning two lines so it is easy to spot in the snapshot.
          |
          |---
          |
          |Final paragraph with [a link](example.com) and `final code`.
          |""".trimMargin(),
        )
      }

      val touchRobot = rememberTouchRobot()
      LaunchedEffect(Unit) {
        touchRobot.onNode(hasTestTag("editor")).performGesture {
          swipe(
            start = bottomCenter,
            stop = topCenter,
            duration = 600.milliseconds,
          )
          delay(300)
          swipe(
            start = topCenter,
            stop = bottomCenter,
            duration = 600.milliseconds,
          )
        }
      }
    }
  }

  @Composable
  private fun Scaffold(
    content: @Composable () -> Unit,
  ) {
    MaterialTheme {
      Surface(
        Modifier
          .fillMaxWidth()
          .heightIn(min = 300.dp)
      ) {
        content()
      }
    }
  }

  @Composable
  private fun WysiwygEditor(
    markdown: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    textState: TextFieldState = rememberTextFieldState(markdown),
  ) {
    WsyiwygTextField(
      modifier = modifier.testTag("editor"),
      contentPadding = contentPadding,
      wysiwyg = rememberWysiwyg(
        textState = textState,
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

@Composable
private fun wysiwygTheme(): WysiwygTheme {
  return WysiwygTheme(
    markerColor = MaterialTheme.colorScheme.tertiary,
    headingColor = MaterialTheme.colorScheme.primary,
    linkTextColor = MaterialTheme.colorScheme.primary,
    linkUrlColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
    struckThroughTextColor = LocalContentColor.current.copy(alpha = 0.5f),
    codeBackground = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp),
    codeBlockLeadingPadding = 16.sp,
    blockQuoteText = LocalContentColor.current.copy(alpha = 0.9f),
    blockQuoteLeadingPadding = 16.sp,
    listBlockLeadingPadding = 16.sp,
  )
}

// todo: upstream this to paparazzi.
private fun Paparazzi.gif(
  start: Long = 0L,
  end: Long = 500L,
  fps: Int = 30,
  composable: @Composable () -> Unit,
) {
  val hostView = ComposeView(context).apply {
    layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
  }
  hostView.setContent(composable)
  gif(view = hostView, start = start, end = end, fps = fps)
}