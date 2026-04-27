package me.saket.wysiwyg.highlight

import androidx.compose.ui.text.TextRange
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.test.runTest
import me.saket.wysiwyg.highlight.flexmark.FlexmarkMarkdownParser
import org.junit.Test

class IncrementalMarkdownParserTest {

  private fun parser(): IncrementalMarkdownParser =
    IncrementalMarkdownParser(FlexmarkMarkdownParser())

  @Test fun `edit inside a span extends the span's end`() = runTest {
    parser().test {
      sendInput("**Beginnings** are such **delicate** times")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

      sendInput("**Beginnings** are such **de!licate** times")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are such <b>**de!licate**</b> times")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `edit before all spans shifts both endpoints`() = runTest {
    parser().test {
      sendInput("**Beginnings** are such **delicate** times")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

      sendInput("!**Beginnings** are such **delicate** times")
      assertThat(awaitItem()).isEqualTo("!<b>**Beginnings**</b> are such <b>**delicate**</b> times")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `edit after all spans leaves them alone`() = runTest {
    parser().test {
      sendInput("**Beginnings** are such **delicate** times")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

      sendInput("**Beginnings** are such **delicate** times!")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times!")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `edit crossing a span boundary drops that span only`() = runTest {
    parser().test {
      sendInput("**Beginnings** are such **delicate** times")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

      // Deleting "h **" straddles the start of the second bold's opening marker.
      sendInput("**Beginnings** are sucdelicate** times")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are sucdelicate** times")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `deletion inside a span shrinks its end`() = runTest {
    parser().test {
      sendInput("**Beginnings** are such **delicate** times")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

      sendInput("**Beginnings** are such **deicate** times")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are such <b>**deicate**</b> times")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `edit between two spans shifts only the later span`() = runTest {
    parser().test {
      sendInput("**Beginnings** are such **delicate** times")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are such <b>**delicate**</b> times")

      // Insert "!" between "are" and "such" — after the first span, before the second.
      sendInput("**Beginnings** are! such **delicate** times")
      assertThat(awaitItem()).isEqualTo("<b>**Beginnings**</b> are! such <b>**delicate**</b> times")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `edit inside a heading extends the heading span`() = runTest {
    parser().test {
      sendInput("# First heading\n\n# Second heading")
      assertThat(awaitItem()).isEqualTo("<h1># First heading</h1>\n\n<h1># Second heading</h1>")

      // Insert "!" inside "heading" in the first line.
      sendInput("# First h!eading\n\n# Second heading")
      assertThat(awaitItem()).isEqualTo("<h1># First h!eading</h1>\n\n<h1># Second heading</h1>")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `empty changes skip the shifted emission`() = runTest {
    val text = "**Beginnings** are such **delicate** times"
    val expected = "<b>**Beginnings**</b> are such <b>**delicate**</b> times"

    parser().test {
      sendInput(text)
      assertThat(awaitItem()).isEqualTo(expected)

      // Re-highlight the same text: zero changes → the flow emits only the delegate's fresh
      // result, not a shifted approximation.
      sendInput(text, ChangeListSnapshot.Empty)
      assertThat(awaitItem()).isEqualTo(expected)
      expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `stale cache skips the shifted emission when length does not match`() = runTest {
    val seedText = "**Beginnings** are such **delicate** times"

    // Pretend only "!" was inserted at the end, but actually hand the highlighter a text
    // that's 5 chars longer. The length invariant (cachedLength + Σdelta == newLength)
    // fails, so the shifted emission is skipped.
    val newText = "$seedText!!!!!"
    val staleChanges = changeListSnapshot(seedText, "$seedText!")

    val expected = "<b>**Beginnings**</b> are such <b>**delicate**</b> times!!!!!"
    parser().test {
      sendInput(seedText) // seed a 42-char text
      assertThat(awaitItem()).isEqualTo(
        "<b>**Beginnings**</b> are such <b>**delicate**</b> times"
      )

      sendInput(newText, staleChanges)
      assertThat(awaitItem()).isEqualTo(expected)
      expectNoEvents()
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `deleting one list item marker keeps the list block for surviving items`() = runTest {
    parser().test {
      sendInput(
        """
        |- first
        |- second
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<list>- first
        |- second
        |</list>""".trimMargin()
      )

      sendInput(
        """
        | first
        |- second
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<list> first
        |- second
        |</list>""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `editing a nested list keeps unaffected siblings aligned`() = runTest {
    parser().test {
      sendInput(
        """
        |- parent
        |  - child
        |- sibling
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
          |<list>- parent
          |  - child
          |- sibling
          |</list>""".trimMargin()
      )

      sendInput(
        """
        |- parent
        |  - ch!ild
        |- sibling
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
          |<list>- parent
          |  - ch!ild
          |- sibling
          |</list>""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `editing inside a link preserves link rendering`() = runTest {
    val editedExpected = "<link>[label](https://exa!mple.com)</link>"

    parser().test {
      sendInput("[label](https://example.com)")
      assertThat(awaitItem()).isEqualTo("<link>[label](https://example.com)</link>")

      sendInput("[label](https://exa!mple.com)")
      assertThat(awaitItem()).isEqualTo(editedExpected)
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `block quotes headings and inline spans survive overlay shifts together`() = runTest {
    parser().test {
      sendInput(
        """
        |> quote
        |
        |# heading
        |
        |**bold**
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
          |<blockquote>> quote</blockquote>
          |
          |<h1># heading</h1>
          |
          |<b>**bold**</b>
          |""".trimMargin()
      )

      sendInput(
        """
        |> qu!ote
        |
        |# heading
        |
        |**bold**
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
          |<blockquote>> qu!ote</blockquote>
          |
          |<h1># heading</h1>
          |
          |<b>**bold**</b>
          |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `trailing plain text after a composite node stays aligned after edits`() = runTest {
    parser().test {
      sendInput(
        """
        |- first
        |- second
        |
        |tail
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
          |<list>- first
          |- second
          |</list>
          |tail
          |""".trimMargin()
      )

      sendInput(
        """
        |- fir!st
        |- second
        |
        |tail
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
          |<list>- fir!st
          |- second
          |</list>
          |tail
          |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }
}

private suspend fun IncrementalMarkdownParser.test(test: suspend ParserTester.() -> Unit) {
  val inputs = MutableSharedFlow<ParserTester.Input>(replay = 1, extraBufferCapacity = 1)
  val highlights = inputs.transform { input ->
    parse(input.text, input.changes).collect { document ->
      emit(document.renderHtml(input.text))
    }
  }
  highlights.test {
    test(ParserTester(turbine = this, inputs = inputs))
  }
}

private class ParserTester(
  turbine: ReceiveTurbine<String>,
  private val inputs: MutableSharedFlow<Input>,
) : ReceiveTurbine<String> by turbine {

  private var lastInput: String? = null

  fun sendInput(text: String, changes: ChangeListSnapshot? = null) {
    val changes = changes
      ?: lastInput?.let { changeListSnapshot(it, text) }
      ?: ChangeListSnapshot.Empty
    lastInput = text

    inputs.tryEmit(
      Input(text, changes)
    )
  }

  class Input(
    val text: String,
    val changes: ChangeListSnapshot,
  )
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
