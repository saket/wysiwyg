package me.saket.wysiwyg.parser

import com.google.common.truth.Truth.assertThat
import me.saket.wysiwyg.SpanTextRange
import me.saket.wysiwyg.decodeTextSelection
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
          style = SyntaxColorSpanStyle,
          range = SpanTextRange(startIndex = 0, endIndexExclusive = 2),
        ),
        MarkdownSpan(
          style = ListBlockSpanStyle,
          range = SpanTextRange(startIndex = 0, endIndexExclusive = 7)
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
          style = SyntaxColorSpanStyle,
          range = SpanTextRange(startIndex = 0, endIndexExclusive = 2)
        ),
        MarkdownSpan(
          style = ListBlockSpanStyle,
          range = SpanTextRange(startIndex = 0, endIndexExclusive = 11)
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
          style = SyntaxColorSpanStyle,
          range = SpanTextRange(startIndex = 0, endIndexExclusive = 1)
        ),
        MarkdownSpan(
          style = HeadingSpanStyle(level = 1),
          range = SpanTextRange(startIndex = 0, endIndexExclusive = 8)
        ),
      )
    )

    assertSpansFor(
      input = """
        |## Heading
        """.trimMargin(),
      expect = listOf(
        MarkdownSpan(
          style = SyntaxColorSpanStyle,
          range = SpanTextRange(startIndex = 0, endIndexExclusive = 2)
        ),
        MarkdownSpan(
          style = HeadingSpanStyle(level = 2),
          range = SpanTextRange(startIndex = 0, endIndexExclusive = 10)
        ),
      )
    )
  }

  @Test fun `adjust span offsets when characters are changed after existing spans`() {
    // Scenario: one character is added.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall you be taking the *Batpod* s▮",
        after = "Alfred: Shall you be taking the *Batpod* si▮"
      )
    ).isEqualTo(
      "Alfred: Shall you be taking the {i}*Batpod*{/i} si"
    )

    // Scenario: one character is removed.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall you be taking the *Batpod* sir▮",
        after = "Alfred: Shall you be taking the *Batpod* si▮"
      )
    ).isEqualTo(
      "Alfred: Shall you be taking the {i}*Batpod*{/i} si"
    )

    // Scenario: multiple characters are added.
    assertThat(
      editOffsetsAndHighlight(
        before = "**Alfred**: Shall you be taking the *Batpod* ▮",
        after = "**Alfred**: Shall you be taking the *Batpod* sir▮?"
      )
    ).isEqualTo(
      "{b}**Alfred**{/b}: Shall you be taking the {i}*Batpod*{/i} sir?"
    )

    // Scenario: multiple characters are selected and replaced.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall you be taking the *Batpod* ▮boy▮?",
        after = "Alfred: Shall you be taking the *Batpod* ▮sir▮?"
      )
    ).isEqualTo(
      "Alfred: Shall you be taking the {i}*Batpod*{/i} sir?"
    )

    // Scenario: multiple characters are selected and replaced. The selection is then removed.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall you be taking the *Batpod* ▮boy▮?",
        after = "Alfred: Shall you be taking the *Batpod* sir▮?"
      )
    ).isEqualTo(
      "Alfred: Shall you be taking the {i}*Batpod*{/i} sir?"
    )
  }

  @Test fun `adjust span offsets when characters are changed before existing spans`() {
    // Scenario: one character is added.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall yo▮ be taking the *Batpod* sir?",
        after = "Alfred: Shall you▮ be taking the *Batpod* sir?"
      )
    ).isEqualTo(
      "Alfred: Shall you be taking the {i}*Batpod*{/i} sir?"
    )

    // Scenario: one character is removed.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall you▮ be taking the *Batpod* sir?",
        after = "Alfred: Shall yo▮ be taking the *Batpod* sir?"
      )
    ).isEqualTo(
      "Alfred: Shall yo be taking the {i}*Batpod*{/i} sir?"
    )

    // Scenario: multiple characters are added.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall ▮ be taking the *Batpod* sir?",
        after = "Alfred: Shall you▮ be taking the *Batpod* sir?"
      )
    ).isEqualTo(
      "Alfred: Shall you be taking the {i}*Batpod*{/i} sir?"
    )

    // Scenario: multiple characters are selected and replaced.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall you be ▮driving▮ the *Batpod* sir?",
        after = "Alfred: Shall you be ▮taking▮ the *Batpod* sir?"
      )
    ).isEqualTo(
      "Alfred: Shall you be taking the {i}*Batpod*{/i} sir?"
    )

    // Scenario: multiple characters are selected and replaced. The selection is then removed.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall you be ▮driving▮ the *Batpod* sir?",
        after = "Alfred: Shall you be taking▮ the *Batpod* sir?"
      )
    ).isEqualTo(
      "Alfred: Shall you be taking the {i}*Batpod*{/i} sir?"
    )
  }

  @Test fun `adjust span offsets when characters are changed inside existing spans`() {
    // Scenario: one character is added.
    assertThat(
      editOffsetsAndHighlight(
        before = """
          |Alfred: Shall you be taking the *Bat▮od* sir?
          |
          |> Bruce Wayne: In the middle of the day Alfred?
          """.trimMargin(),
        after = """
          |Alfred: Shall you be taking the *Batp▮od* sir?
          |
          |> Bruce Wayne: In the middle of the day Alfred?
          """.trimMargin()
      )
    ).isEqualTo(
      """
        |Alfred: Shall you be taking the {i}*Batpod*{/i} sir?
        |
        |{quote}> Bruce Wayne: In the middle of the day Alfred?{/quote}
        """.trimMargin()
    )

    // Scenario: one character is removed.
    assertThat(
      editOffsetsAndHighlight(
        before = """
          |Alfred: Shall you be taking the *Batp▮od* sir?
          |
          |> Bruce Wayne: In the middle of the day Alfred?
          """.trimMargin(),
        after = """
          |Alfred: Shall you be taking the *Bat▮od* sir?
          |
          |> Bruce Wayne: In the middle of the day Alfred?
          """.trimMargin()
      )
    ).isEqualTo(
      """
        |Alfred: Shall you be taking the {i}*Batod*{/i} sir?
        |
        |{quote}> Bruce Wayne: In the middle of the day Alfred?{/quote}
        """.trimMargin()
    )

    // Scenario: multiple characters are added.
    assertThat(
      editOffsetsAndHighlight(
        before = """
          |Alfred: Shall you be taking the *Batpod* sir?
          |
          |> Bruce Wayne: In the ▮ of the day Alfred?
          """.trimMargin(),
        after = """
          |Alfred: Shall you be taking the *Batpod* sir?
          |
          |> Bruce Wayne: In the middle▮ of the day Alfred?
          """.trimMargin()
      )
    ).isEqualTo(
      """
        |Alfred: Shall you be taking the {i}*Batpod*{/i} sir?
        |
        |{quote}> Bruce Wayne: In the middle of the day Alfred?{/quote}
        """.trimMargin()
    )

    // Scenario: multiple characters are selected and replaced.
    assertThat(
      editOffsetsAndHighlight(
        before = """
          |Alfred: Shall you be taking the *Batpod* sir?
          |
          |> Bruce Wayne: ▮In the middle of the day▮ Alfred?
          """.trimMargin(),
        after = """
          |Alfred: Shall you be taking the *Batpod* sir?
          |
          |> Bruce Wayne: why not▮ Alfred?
          """.trimMargin()
      )
    ).isEqualTo(
      """
        |Alfred: Shall you be taking the {i}*Batpod*{/i} sir?
        |
        |{quote}> Bruce Wayne: why not Alfred?{/quote}
        """.trimMargin()
    )
  }

  @Test fun `adjust span offsets when characters are removed at the edges of spans`() {
    // Scenario: one character is removed at the starting edge.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall you be taking the *▮Batpod* sir?",
        after = "Alfred: Shall you be taking the ▮Batpod* sir?"
      )
    ).isEqualTo("Alfred: Shall you be taking the Batpod* sir?")

    // Scenario: one character is removed at the closing edge.
    assertThat(
      editOffsetsAndHighlight(
        before = "Alfred: Shall you be taking the *Batpod*▮ sir?",
        after = "Alfred: Shall you be taking the *Batpod▮ sir?"
      )
    ).isEqualTo("Alfred: Shall you be taking the *Batpod sir?")

    // Scenario: one character is removed at the closing edge of both the span and its paragraph.
    assertThat(
      editOffsetsAndHighlight(
        before = """
          |Alfred: Shall you be taking the *Batpod* sir?
          |
          |>k▮
          """.trimMargin(),
        after = """
          |Alfred: Shall you be taking the *Batpod* sir?
          |
          |>▮
          """.trimMargin()
      )
    ).isEqualTo(
      """
        |Alfred: Shall you be taking the {i}*Batpod*{/i} sir?
        |
        |{quote}>{/quote}
        """.trimMargin()
    )
  }

  @Test fun `adjust span offsets when characters are changed across edges of spans`() {
    // Scenario: the affected span has both opening and closing markers.
    assertThat(
      editOffsetsAndHighlight(
        before = """
          |Alfred: Shall you be taking ▮the *Bat▮pod* sir?
          |
          |> Bruce Wayne: In the middle of the day Alfred?
          """.trimMargin(),
        after = """
          |Alfred: Shall you be taking ▮pod* sir?
          |
          |> Bruce Wayne: In the middle of the day Alfred?
          """.trimMargin()
      )
    ).isEqualTo(
      """
        |Alfred: Shall you be taking pod* sir?
        |
        |{quote}> Bruce Wayne: In the middle of the day Alfred?{/quote}
        """.trimMargin()
    )

    // Scenario: the affected span only has an opening marker.
    assertThat(
      editOffsetsAndHighlight(
        before = """
          |Alfred: Shall you be taking ▮the *Batpod* sir?
          |
          |> Bruce Wayne: In the middle of the day▮ Alfred?
          """.trimMargin(),
        after = """
          |Alfred: Shall you be taking ▮ Alfred?
          """.trimMargin()
      )
    ).isEqualTo(
      """
        |Alfred: Shall you be taking  Alfred?
        """.trimMargin()
    )

    // Scenario: multiple characters are replaced across the starting edge.
    assertThat(
      editOffsetsAndHighlight(
        before = """
          |Alfred: Shall you be taking ▮the *Batpod* sir?
          |
          |> Bruce Wayne: In the middle of the day Alfred▮?
          """.trimMargin(),
        after = """
          |Alfred: Shall you be taking ▮ Alfred?
          """.trimMargin()
      )
    ).isEqualTo(
      """
        |Alfred: Shall you be taking  Alfred?
        """.trimMargin()
    )

    // Scenario: multiple characters are replaced across the closing edge.
    assertThat(
      editOffsetsAndHighlight(
        before = """
          |>Alfred: Shall you be taking▮ the Batpod sir?
          |
          |Bruce Wayne: In the middle of the day▮ Alfred?
          """.trimMargin(),
        after = """
          |>Alfred: Shall you be taking▮ Alfred?
          """.trimMargin()
      )
    ).isEqualTo(
      """
        |{quote}>Alfred: Shall you be taking Alfred?{/quote}
        """.trimMargin()
    )
  }

  private fun assertSpansFor(
    input: String,
    expect: List<MarkdownSpan>
  ) {
    val result = parser.parse(input)
    assertThat(result.spans).containsExactlyElementsIn(expect)
  }

  private fun editOffsetsAndHighlight(
    before: String,
    after: String,
  ): String {
    val previousValue = decodeTextSelection(before)
    val newValue = decodeTextSelection(after)
    val newSpans = parser.offsetSpansOnTextChange(
      newValue = newValue,
      previousValue = previousValue,
      previousSpans = parser.parse(previousValue.text).spans
    )
    return renderHtml(newValue.text, newSpans)
  }

  private fun renderHtml(text: String, spans: List<MarkdownSpan>): String {
    val html = StringBuilder(text)

    var offset = 0
    spans.forEach {
      val tag = when (it.style) {
        SyntaxColorSpanStyle -> null
        BoldSpanStyle -> "b"
        ItalicSpanStyle -> "i"
        BlockQuoteBodySpanStyle -> "quote"
        else -> error("${it.style} is unsupported")
      }
      if (tag != null) {
        val substring = text.substring(it.range.startIndex, it.range.endIndexExclusive.coerceAtMost(text.length))
        val replacement = "{$tag}$substring{/$tag}"
        html.replace(offset + it.range.startIndex, offset + it.range.endIndexExclusive, replacement)
        offset += replacement.length - substring.length
      }
    }
    return html.toString()
  }
}
