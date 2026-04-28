package me.saket.wysiwyg.format

import androidx.compose.ui.text.TextRange
import org.junit.Test

class OnEnterMarkdownFormattersTest {
  @Test fun `detect enter key via input transformation`() {
    val formatter = object : OnEnterMarkdownFormatter {
      override fun onEnterPressed(
        text: CharSequence,
        paragraph: TextParagraph,
        cursorPositionBeforeEnter: Int,
      ): TextReplacement2 {
        return TextReplacement2 {
          replace(0, length, "enter detected")
          selection = TextRange(0)
        }
      }
    }
    val transformation = OnEnterMarkdownFormatters(listOf(formatter)).asInputTransformation()

    // Typing a non-newline character should leave the text untouched.
    transformation.assertOnChange(
      before = "Alfred: Shall you be taking the Batpo▮",
      after = "Alfred: Shall you be taking the Batpod▮",
      expect = "Alfred: Shall you be taking the Batpod▮",
    )

    // Typing a newline at the cursor should trigger the formatter.
    transformation.assertOnChange(
      before = "Alfred: Shall you be taking the Batpod▮",
      after = "Alfred: Shall you be taking the Batpod\n▮",
      expect = "▮enter detected",
    )
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
