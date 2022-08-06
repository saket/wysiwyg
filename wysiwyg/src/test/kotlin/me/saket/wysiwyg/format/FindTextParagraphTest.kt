package me.saket.wysiwyg.format

import com.google.common.truth.Truth.assertThat
import me.saket.wysiwyg.decodeTextSelection
import org.junit.Test

class FindTextParagraphTest {
  @Test fun `empty paragraph`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
         |▮
         """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "",
        startIndex = 0,
        endIndexExclusive = 0
      )
    )
  }

  @Test fun `cursor after a whitespace`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        | ▮
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = " ",
        startIndex = 0,
        endIndexExclusive = 1
      )
    )
  }

  @Test fun `cursor surrounded by whitespaces`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        | ▮${Typography.nbsp}
        """.trimMargin()
      )
    )

    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = " ${Typography.nbsp}",
        startIndex = 0,
        endIndexExclusive = 2
      )
    )
  }

  @Test fun `empty paragraph with a leading empty line`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |
        |▮
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "",
        startIndex = 1,
        endIndexExclusive = 1
      )
    )
  }

  @Test fun `empty paragraph with a multiple leading empty lines`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |
        |
        |
        |
        |▮
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "",
        startIndex = 4,
        endIndexExclusive = 4
      )
    )
  }

  @Test fun `empty paragraph with a leading paragraph`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |Alfred: Shall you be taking the Batpod sir?
        |▮
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "",
        startIndex = 44,
        endIndexExclusive = 44
      )
    )
  }

  @Test fun `empty paragraph with a following empty line`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |▮
        |
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "",
        startIndex = 0,
        endIndexExclusive = 0
      )
    )
  }

  @Test fun `blank paragraph after a blank line`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |  
        | ▮
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = " ",
        startIndex = 3,
        endIndexExclusive = 4
      )
    )
  }

  @Test fun `blank paragraph surrounded by blank lines`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |  
        | ▮
        | 
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = " ",
        startIndex = 3,
        endIndexExclusive = 4
      )
    )
  }

  @Test fun `cursor at the starting of a single paragraph`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |▮Alfred: Shall you be taking the Batpod sir?
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "Alfred: Shall you be taking the Batpod sir?",
        startIndex = 0,
        endIndexExclusive = 43
      )
    )
  }

  @Test fun `cursor in the middle of a single paragraph`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |Alfred: Shall you be ▮taking the Batpod sir?
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "Alfred: Shall you be taking the Batpod sir?",
        startIndex = 0,
        endIndexExclusive = 43
      )
    )
  }

  @Test fun `cursor at the end of a single paragraph`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |Alfred: Shall you be taking the Batpod sir?▮
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "Alfred: Shall you be taking the Batpod sir?",
        startIndex = 0,
        endIndexExclusive = 43
      )
    )
  }

  @Test fun `cursor at the end of a paragraph surrounded by paragraphs`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |Alfred: Shall you be taking the Batpod sir?
        |
        |Batman/Bruce Wayne: In the middle of the day Alfred?▮
        |
        |Alfred: The Lamborghini then? Much more subtle.
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "Batman/Bruce Wayne: In the middle of the day Alfred?",
        startIndex = 45,
        endIndexExclusive = 97
      )
    )
  }

  @Test fun `cursor at the starting of a paragraph surrounded by paragraphs`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |Alfred: Shall you be taking the Batpod sir?
        |
        |▮Batman/Bruce Wayne: In the middle of the day Alfred?
        |
        |Alfred: The Lamborghini then? Much more subtle.
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "Batman/Bruce Wayne: In the middle of the day Alfred?",
        startIndex = 45,
        endIndexExclusive = 97
      )
    )
  }

  @Test fun `cursor in the middle of a paragraph surrounded by paragraphs`() {
    val paragraph = TextParagraph.findUnderCursor(
      decodeTextSelection(
        """
        |Alfred: Shall you be taking the Batpod sir?
        |
        |Batman/Bruce Wayne: In ▮the middle of the day Alfred?
        |
        |Alfred: The Lamborghini then? Much more subtle.
        """.trimMargin()
      )
    )
    assertThat(paragraph).isEqualTo(
      TextParagraph(
        text = "Batman/Bruce Wayne: In the middle of the day Alfred?",
        startIndex = 45,
        endIndexExclusive = 97
      )
    )
  }
}
