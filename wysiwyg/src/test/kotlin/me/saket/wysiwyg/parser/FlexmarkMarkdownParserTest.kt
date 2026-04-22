package me.saket.wysiwyg.parser

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import me.saket.wysiwyg.HeadingSpanStyle
import me.saket.wysiwyg.ListBlockSpanStyle
import me.saket.wysiwyg.MarkdownSpan
import me.saket.wysiwyg.MarkerColorSpanStyle
import me.saket.wysiwyg.MarkdownSpanTextRange
import me.saket.wysiwyg.parser.flexmark.FlexmarkMarkdownParser
import org.junit.Test

class FlexmarkMarkdownParserTest {
  private val parser = FlexmarkMarkdownParser()

  @Test fun `list items without leading space shouldn't be highlighted`() {
    assertSpansFor(
      input = """
        |1.
        |*
        |+
        |-
        """.trimMargin(),
      expect = emptyList()
    )
  }

  // Can be removed once https://github.com/vsch/flexmark-java/issues/519 is fixed.
  @Test fun `list items with blank content should be included in span's offsets`() {
    val leadingSpaces = "    "
    assertSpansFor(
      input = """
        |1.${leadingSpaces}
        |
        |Unrelated text.
        """.trimMargin(),
      expect = listOf(
        MarkdownSpan(
          style = MarkerColorSpanStyle,
          range = MarkdownSpanTextRange(startIndex = 0, endIndexExclusive = 2),
        ),
        MarkdownSpan(
          style = ListBlockSpanStyle,
          range = MarkdownSpanTextRange(startIndex = 0, endIndexExclusive = 7)
        )
      )
    )

    assertSpansFor(
      input =
      """
        |1.${leadingSpaces}Milk
        |
        |Unrelated text.
        """.trimMargin(),
      expect = listOf(
        MarkdownSpan(
          style = MarkerColorSpanStyle,
          range = MarkdownSpanTextRange(startIndex = 0, endIndexExclusive = 2)
        ),
        MarkdownSpan(
          style = ListBlockSpanStyle,
          range = MarkdownSpanTextRange(startIndex = 0, endIndexExclusive = 11)
        )
      )
    )
  }

  @Test fun `heading should always be non-empty`() {
    assertSpansFor(
      input = """
        |#
        """.trimMargin(),
      expect = emptyList()
    )

    assertSpansFor(
      input = """
        |#Heading
        """.trimMargin(),
      expect = listOf(
        MarkdownSpan(
          style = MarkerColorSpanStyle,
          range = MarkdownSpanTextRange(startIndex = 0, endIndexExclusive = 1)
        ),
        MarkdownSpan(
          style = HeadingSpanStyle(level = 1),
          range = MarkdownSpanTextRange(startIndex = 0, endIndexExclusive = 8)
        ),
      )
    )

    assertSpansFor(
      input = """
        |## Heading
        """.trimMargin(),
      expect = listOf(
        MarkdownSpan(
          style = MarkerColorSpanStyle,
          range = MarkdownSpanTextRange(startIndex = 0, endIndexExclusive = 2)
        ),
        MarkdownSpan(
          style = HeadingSpanStyle(level = 2),
          range = MarkdownSpanTextRange(startIndex = 0, endIndexExclusive = 10)
        ),
      )
    )
  }

  private fun assertSpansFor(
    input: String,
    expect: List<MarkdownSpan>
  ) = runTest {
    val result = parser.parse(input, ChangeListSnapshot.Empty)
    assertThat(result.spans).containsExactlyElementsIn(expect)
  }
}
