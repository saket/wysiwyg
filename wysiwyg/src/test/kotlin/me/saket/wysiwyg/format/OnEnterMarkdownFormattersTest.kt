package me.saket.wysiwyg.format

import com.google.common.truth.Truth.assertThat
import me.saket.wysiwyg.decodeTextSelection
import org.junit.Test

class OnEnterMarkdownFormattersTest {
  @Test fun `detect enter key`() {
    val formatter = object : OnEnterMarkdownFormatter {
      override fun onEnterPressed(
        text: CharSequence,
        paragraph: TextParagraph,
        cursorPositionBeforeEnter: Int
      ) = TextReplacement("enter detected", newCursorPosition = 0)
    }
    val formatters = OnEnterMarkdownFormatters(listOf(formatter))

    assertThat(
      formatters.formatIfEnterWasPressed(
        previousText = decodeTextSelection("Alfred: Shall you be taking the Batpo▮"),
        newText = decodeTextSelection("Alfred: Shall you be taking the Batpod▮")
      ).text
    ).isEqualTo("Alfred: Shall you be taking the Batpod")

    assertThat(
      formatters.formatIfEnterWasPressed(
        previousText = decodeTextSelection("Alfred: Shall you be taking the Batpod▮"),
        newText = decodeTextSelection("Alfred: Shall you be taking the Batpod\n▮")
      ).text
    ).isEqualTo("enter detected")
  }

  @Test fun `enter key on an empty paragraph shouldn't do anything`() {
    val formatters = OnEnterMarkdownFormatters(
      listOf(OnEnterStartCodeBlock, OnEnterContinueList())
    )

    formatters.assertOnEnter(
      input = """
              |▮
              """.trimMargin(),
      expect = null
    )
    formatters.assertOnEnter(
      input = """
              |
              |▮
              """.trimMargin(),
      expect = null
    )
  }
}
