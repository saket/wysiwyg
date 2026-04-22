package me.saket.wysiwyg.highlight

import androidx.compose.ui.text.TextRange
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import me.saket.wysiwyg.HeadingSpanStyle
import me.saket.wysiwyg.ListBlockSpanStyle
import me.saket.wysiwyg.MarkdownSpan
import me.saket.wysiwyg.MarkerColorSpanStyle
import me.saket.wysiwyg.highlight.flexmark.FlexmarkMarkdownHighlighter
import org.junit.Test

class FlexmarkMarkdownHighlighterTest {
  private val highlighter = FlexmarkMarkdownHighlighter()

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
          range = TextRange(0, 2),
        ),
        MarkdownSpan(
          style = ListBlockSpanStyle,
          range = TextRange(0, 7),
        ),
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
          range = TextRange(0, 2),
        ),
        MarkdownSpan(
          style = ListBlockSpanStyle,
          range = TextRange(0, 11),
        ),
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
          range = TextRange(0, 1),
        ),
        MarkdownSpan(
          style = HeadingSpanStyle(level = 1),
          range = TextRange(0, 8),
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
          range = TextRange(0, 2),
        ),
        MarkdownSpan(
          style = HeadingSpanStyle(level = 2),
          range = TextRange(0, 10),
        ),
      )
    )
  }

  private fun assertSpansFor(
    input: String,
    expect: List<MarkdownSpan>
  ) = runTest {
    val result = highlighter.highlight(input, ChangeListSnapshot.Empty)
    assertThat(result.spans).containsExactlyElementsIn(expect)
  }
}
