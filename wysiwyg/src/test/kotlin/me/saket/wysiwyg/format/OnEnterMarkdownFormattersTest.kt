package me.saket.wysiwyg.format

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.text.TextRange
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class OnEnterMarkdownFormattersTest {
  @Test fun `detect enter key via input transformation`() {
    val formatter = object : OnEnterMarkdownFormatter {
      override fun onEnterPressed(
        text: CharSequence,
        paragraph: TextParagraph,
        cursorPositionBeforeEnter: Int,
      ) = TextReplacement("enter detected", newCursorPosition = 0)
    }
    val formatters = OnEnterMarkdownFormatters(listOf(formatter))
    val transformation = formatters.asInputTransformation()

    // Typing a non-newline character should leave the text untouched.
    val beforeDState = TextFieldState(
      initialText = "Alfred: Shall you be taking the Batpo",
      initialSelection = TextRange(37),
    )
    beforeDState.edit {
      replace(37, 37, "d")
      with(transformation) { transformInput() }
    }
    assertThat(beforeDState.text.toString()).isEqualTo("Alfred: Shall you be taking the Batpod")

    // Typing a newline at the cursor should trigger the formatter.
    val beforeEnterState = TextFieldState(
      initialText = "Alfred: Shall you be taking the Batpod",
      initialSelection = TextRange(38),
    )
    beforeEnterState.edit {
      replace(38, 38, "\n")
      with(transformation) { transformInput() }
    }
    assertThat(beforeEnterState.text.toString()).isEqualTo("enter detected")
  }

  @Test fun `enter key on an empty paragraph shouldn't do anything`() {
    val formatters = OnEnterMarkdownFormatters(
      listOf(OnEnterStartCodeBlock, OnEnterContinueList()),
    )

    formatters.assertOnEnter(
      input = """
              |▮
              """.trimMargin(),
      expect = null,
    )
    formatters.assertOnEnter(
      input = """
              |
              |▮
              """.trimMargin(),
      expect = null,
    )
  }
}
