package me.saket.wysiwyg.highlight

import androidx.compose.ui.text.TextRange
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import me.saket.wysiwyg.BoldSpanStyle
import me.saket.wysiwyg.HeadingSpanStyle
import me.saket.wysiwyg.MarkdownSpan
import me.saket.wysiwyg.MarkdownSpanStyle
import me.saket.wysiwyg.highlight.flexmark.FlexmarkMarkdownHighlighter
import org.junit.Test

class ShiftingMarkdownHighlighterTest {
  @Test fun `edit inside a span extends the span's end`() = runTest {
    val highlighter = TestHighlighter()

    assertThat(highlighter.highlight("**Beginnings** are such **delicate** times"))
      .isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

    assertThat(highlighter.highlight("**Beginnings** are such **de!licate** times"))
      .isEqualTo("<b>**Beginnings**</b> are such <b>**de!licate**</b> times")
  }

  @Test fun `edit before all spans shifts both endpoints`() = runTest {
    val highlighter = TestHighlighter()

    assertThat(highlighter.highlight("**Beginnings** are such **delicate** times"))
      .isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

    assertThat(highlighter.highlight("!**Beginnings** are such **delicate** times"))
      .isEqualTo("!<b>**Beginnings**</b> are such <b>**delicate**</b> times")
  }

  @Test fun `edit after all spans leaves them alone`() = runTest {
    val highlighter = TestHighlighter()

    assertThat(highlighter.highlight("**Beginnings** are such **delicate** times"))
      .isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

    assertThat(highlighter.highlight("**Beginnings** are such **delicate** times!"))
      .isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times!")
  }

  @Test fun `edit crossing a span boundary drops that span only`() = runTest {
    val highlighter = TestHighlighter()

    assertThat(highlighter.highlight("**Beginnings** are such **delicate** times"))
      .isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

    // Deleting "h **" straddles the start of the second bold's opening marker.
    assertThat(highlighter.highlight("**Beginnings** are sucdelicate** times"))
      .isEqualTo("<b>**Beginnings**</b> are sucdelicate** times")
  }

  @Test fun `deletion inside a span shrinks its end`() = runTest {
    val highlighter = TestHighlighter()

    assertThat(highlighter.highlight("**Beginnings** are such **delicate** times"))
      .isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

    assertThat(highlighter.highlight("**Beginnings** are such **deicate** times"))
      .isEqualTo("<b>**Beginnings**</b> are such <b>**deicate**</b> times")
  }

  @Test fun `edit between two spans shifts only the later span`() = runTest {
    val highlighter = TestHighlighter()

    assertThat(highlighter.highlight("**Beginnings** are such **delicate** times"))
      .isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

    // Insert "!" between "are" and "such" — after the first span, before the second.
    assertThat(highlighter.highlight("**Beginnings** are! such **delicate** times"))
      .isEqualTo("<b>**Beginnings**</b> are! such <b>**delicate**</b> times")
  }

  @Test fun `edit inside a heading extends the heading span`() = runTest {
    val highlighter = TestHighlighter()

    assertThat(highlighter.highlight("# First heading\n\n# Second heading"))
      .isEqualTo("<h1># First heading</h1>\n\n<h1># Second heading</h1>")

    // Insert "!" inside "heading" in the first line.
    assertThat(highlighter.highlight("# First h!eading\n\n# Second heading"))
      .isEqualTo("<h1># First h!eading</h1>\n\n<h1># Second heading</h1>")
  }

  @Test fun `empty changes skip the shifted emission`() = runTest {
    val highlighter = TestHighlighter()
    val text = "**Beginnings** are such **delicate** times"
    val expected = "<b>**Beginnings**</b> are such <b>**delicate**</b> times"

    highlighter.highlight(text) // Seed.

    // Re-highlight the same text: zero changes → the flow emits only the delegate's fresh
    // result, not a shifted approximation. awaitComplete() enforces "no further emissions".
    highlighter.highlights(text).test {
      assertThat(awaitItem()).isEqualTo(expected)
      awaitComplete()
    }
  }

  @Test fun `stale cache skips the shifted emission when length does not match`() = runTest {
    val seedText = "**Beginnings** are such **delicate** times"
    val highlighter = TestHighlighter()
    highlighter.highlight(seedText) // seed a 42-char text

    // Pretend only "!" was inserted at the end, but actually hand the highlighter a text
    // that's 5 chars longer. The length invariant (cachedLength + Σdelta == newLength)
    // fails, so the shifted emission is skipped.
    val newText = "$seedText!!!!!"
    val staleChanges = changeListSnapshot(seedText, "$seedText!")

    val expected = "<b>**Beginnings**</b> are such <b>**delicate**</b> times!!!!!"
    highlighter.highlights(newText, staleChanges).test {
      assertThat(awaitItem()).isEqualTo(expected)
      awaitComplete()
    }
  }
}

private class TestHighlighter {
  private val highlighter = ShiftingMarkdownHighlighter(FlexmarkMarkdownHighlighter())
  private var lastText: String? = null

  suspend fun highlight(text: String): String {
    return highlights(text).first()
  }

  fun highlights(text: String, changes: ChangeListSnapshot? = null): Flow<String> {
    return flow {
      val changes = changes
        ?: lastText?.let { changeListSnapshot(it, text) }
        ?: ChangeListSnapshot.Empty
      lastText = text
      highlighter.highlight(text, changes).collect { result ->
        emit(encodeTagged(text, result.spans))
      }
    }
  }

  /** Re-encodes recognized spans inline as tags (`<b>`, `<h>`) around their ranges in [text]. */
  private fun encodeTagged(text: String, spans: List<MarkdownSpan>): String {
    // Sort events by position; close tags come before open tags at the same
    // position so adjacent spans produce "</b><b>" rather than "<b></b>".
    val events = buildList {
      for (span in spans) {
        val tag = tagFor(span.style) ?: continue
        add(span.range.start to "<$tag>")
        add(span.range.end to "</$tag>")
      }
    }.sortedWith(compareBy({ it.first }, { if (it.second.startsWith("</")) 0 else 1 }))

    val output = StringBuilder()
    var cursor = 0
    for ((position, tag) in events) {
      output.append(text, cursor, position)
      output.append(tag)
      cursor = position
    }
    output.append(text, cursor, text.length)
    return output.toString()
  }

  private fun tagFor(style: MarkdownSpanStyle): String? = when (style) {
    is BoldSpanStyle -> "b"
    is HeadingSpanStyle -> "h1"
    else -> null
  }
}

/** Derives a single-edit [ChangeListSnapshot] from the common prefix/suffix of the two texts. */
private fun changeListSnapshot(before: String, after: String): ChangeListSnapshot {
  if (before == after) return ChangeListSnapshot.Empty

  var prefix = 0
  while (prefix < before.length && prefix < after.length && before[prefix] == after[prefix]) {
    prefix++
  }

  var suffix = 0
  while (
    suffix < before.length - prefix
    && suffix < after.length - prefix
    && before[before.length - 1 - suffix] == after[after.length - 1 - suffix]
  ) {
    suffix++
  }

  return ChangeListSnapshot(
    changes = listOf(
      ChangeListSnapshot.Change(
        range = TextRange(prefix, after.length - suffix),
        originalRange = TextRange(prefix, before.length - suffix),
      ),
    ),
  )
}
