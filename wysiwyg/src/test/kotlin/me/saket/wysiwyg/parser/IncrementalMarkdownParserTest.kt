package me.saket.wysiwyg.parser

import androidx.compose.ui.text.TextRange
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.test.runTest
import me.saket.wysiwyg.parser.flexmark.FlexmarkMarkdownParser
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
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

  @Test fun `adding or removing hashes in a heading marker keeps it styled`() = runTest {
    // Adding a `#` (h1 to h2). The marker shape stays `#+\s`, so the cached heading
    // stays styled in the overlay. Level stays h1 until the reparse upgrades it.
    parser().test {
      sendInput("# heading")
      assertThat(awaitItem()).isEqualTo("<h1># heading</h1>")
      sendInput("## heading")
      assertThat(awaitItem()).isEqualTo("<h1>## heading</h1>")
      cancelAndIgnoreRemainingEvents()
    }
    // Removing a `#` (h2 to h1). Same idea in the other direction.
    parser().test {
      sendInput("## heading")
      assertThat(awaitItem()).isEqualTo("<h2>## heading</h2>")
      sendInput("# heading")
      assertThat(awaitItem()).isEqualTo("<h2># heading</h2>")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `replacing a hash with a non-hash drops the heading`() = runTest {
    parser().test {
      sendInput("## heading")
      assertThat(awaitItem()).isEqualTo("<h2>## heading</h2>")

      // Replace the first `#` with `!`. The marker is no longer all `#`s, so the cached
      // heading should drop until the reparse arrives.
      sendInput("!# heading")
      assertThat(awaitItem()).isEqualTo("!# heading")
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `deleting one closing tilde of a strikethrough drops the styling`() = runTest {
    parser().test {
      sendInput("~~foo~~")
      assertThat(awaitItem()).isEqualTo("<s>~~foo~~</s>")

      // Delete one trailing `~`. The closing marker is no longer `~~`, so the cached
      // strikethrough should drop in the overlay until the reparse arrives.
      sendInput("~~foo~")
      assertThat(awaitItem()).isEqualTo("~~foo~")
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
      sendInput(text, TextChangeListSnapshot.Empty)
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
        |- second</list>
        |""".trimMargin()
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
        |- second</list>
        |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `editing a later list marker drops only that item while tail content stays aligned`() = runTest {
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
        |- second</list>
        |
        |tail
        |""".trimMargin()
      )

      sendInput(
        """
        |- first
        | second
        |
        |tail
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<list>- first
        | second</list>
        |
        |tail
        |""".trimMargin()
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
          |- sibling</list>
          |""".trimMargin()
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
          |- sibling</list>
          |""".trimMargin()
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

  @Test fun `editing a link marker drops only the link while preserving following content`() = runTest {
    parser().test {
      sendInput("[label](url) tail **bold**")
      assertThat(awaitItem()).isEqualTo("<link>[label](url)</link> tail <b>**bold**</b>")

      sendInput("[label]url) tail **bold**")
      assertThat(awaitItem()).isEqualTo("[label]url) tail <b>**bold**</b>")
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

  @Test fun `touching a heading marker drops only that heading while later nodes still render`() = runTest {
    parser().test {
      sendInput(
        """
        |# heading
        |
        |**bold**
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<h1># heading</h1>
        |
        |<b>**bold**</b>
        |""".trimMargin()
      )

      sendInput(
        """
        |#heading
        |
        |**bold**
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |#heading
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
          |- second</list>
          |
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
          |- second</list>
          |
          |tail
          |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `lazy continuation under task list item is excluded from list block`() = runTest {
    parser().test {
      sendInput(
        """
        |- [ ] task item
        |lazy continuation
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<list><monospace>- [ ] </monospace>task item</list>
        |lazy continuation
        |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }

    // A single-character body counts as a lazy continuation too. Guards against
    // re-introducing a length-based carve-out that keeps short lines inside the list.
    parser().test {
      sendInput(
        """
        |- [ ] One
        |- [ ] Two
        |T""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<list><monospace>- [ ] </monospace>One
        |<monospace>- [ ] </monospace>Two</list>
        |T""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `lazy continuation under list item is excluded from list block`() = runTest {
    parser().test {
      sendInput(
        """
        |- item
        |lazy continuation
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<list>- item</list>
        |lazy continuation
        |""".trimMargin()
      )

      sendInput(
        """
        |1. ordered item
        |lazy continuation
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<list>1. ordered item</list>
        |lazy continuation
        |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `properly indented continuation stays inside the list block`() = runTest {
    // Six spaces match the content column of "- [ ] " so this is a non-lazy
    // continuation per CommonMark and should remain part of the task item.
    parser().test {
      sendInput(
        """
        |- [ ] task item
        |      properly indented
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<list><monospace>- [ ] </monospace>task item
        |      properly indented</list>
        |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

@Test fun `lazy continuation under blockquote is excluded from blockquote range`() = runTest {
    parser().test {
      sendInput(
        """
        |> quote
        |lazy continuation
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<blockquote>> quote</blockquote>
        |lazy continuation
        |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `prefixed continuation stays inside the blockquote range`() = runTest {
    parser().test {
      sendInput(
        """
        |> quote line 1
        |> quote line 2
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<blockquote>> quote line 1
        |> quote line 2</blockquote>
        |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `trailing space after empty task list marker stays monospace`() = runTest {
    // For a non-empty task item the marker range extends one character past "]"
    // so the gap before the body is monospaced and lines up with the marker
    // column. The same gap on an empty task item should also be monospace,
    // otherwise the cursor sits in a narrower proportional space and the
    // indentation shifts by a sub-character width as soon as the user types.
    parser().test {
      sendInput(
        """
        |- [ ]${" "}
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<list><monospace>- [ ]${" "}</monospace></list>
        |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `empty list item directly under a paragraph still renders as a list`() = runTest {
    // No blank line separates "Foo" from the empty "- " marker. The empty bullet
    // should still interrupt the paragraph so the user sees list styling as soon
    // as they hit enter and type "- ", before adding any content.
    parser().test {
      sendInput(
        """
        |Foo
        |-${" "}
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |Foo
        |<list>-${" "}</list>
        |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `blank line still ends the list block`() = runTest {
    // Regression check: the trim must not over-eagerly chop off the legitimate
    // CommonMark list-terminating blank line.
    parser().test {
      sendInput(
        """
        |- item
        |
        |paragraph
        |""".trimMargin()
      )
      assertThat(awaitItem()).isEqualTo(
        """
        |<list>- item</list>
        |
        |paragraph
        |""".trimMargin()
      )
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test fun `overlay extends across cancelled reparses while user is typing fast`() = runTest {
    val parser = IncrementalMarkdownParser(
      OneShotMarkdownParser(
        FlexmarkMarkdownParser()
      )
    )
    parser.test {
      // Seed parse: the delegate's first call goes through and populates the cache.
      sendInput("**bold** tail")
      assertThat(awaitItem()).isEqualTo("<b>**bold**</b> tail")

      // Edit 1: the parser emits an overlay synchronously, then suspends inside
      // delegate.parse (subsequent calls block forever).
      sendInput("**bold** tail!")
      assertThat(awaitItem()).isEqualTo("<b>**bold**</b> tail!")

      // Edit 2 arrives before edit 1's reparse completes; transformLatest cancels
      // edit 1's flow while delegate.parse was still suspended. The overlay must
      // extend across edit 1 + edit 2 — without that, the cache's recorded text
      // length is stale and the length-invariant guard would suppress this emission.
      sendInput("**bold** tail!?")
      assertThat(awaitItem()).isEqualTo("<b>**bold**</b> tail!?")

      cancelAndIgnoreRemainingEvents()
    }
  }
}

@OptIn(ExperimentalCoroutinesApi::class)
private suspend fun IncrementalMarkdownParser.test(test: suspend ParserTester.() -> Unit) {
  val inputs = MutableSharedFlow<ParserTester.Input>(replay = 1, extraBufferCapacity = 1)
  val highlights = inputs.transformLatest { input ->
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

  fun sendInput(text: String, changes: TextChangeListSnapshot? = null) {
    val changes = changes
      ?: lastInput?.let { changeListSnapshot(it, text) }
      ?: TextChangeListSnapshot.Empty
    lastInput = text

    inputs.tryEmit(
      Input(text, changes)
    )
  }

  class Input(
    val text: String,
    val changes: TextChangeListSnapshot,
  )
}

/** Lets the first [parse] call go through to [delegate]; every subsequent call suspends forever. */
private class OneShotMarkdownParser(
  private val delegate: MarkdownParser,
) : MarkdownParser {
  private var seeded = false

  override suspend fun parse(text: String, changes: TextChangeListSnapshot): MarkdownDocument {
    if (!seeded) {
      seeded = true
      return delegate.parse(text, changes)
    }
    awaitCancellation()
  }
}

/** Derives a single-edit [TextChangeListSnapshot] from the common prefix/suffix of the two texts. */
private fun changeListSnapshot(before: String, after: String): TextChangeListSnapshot {
  if (before == after) return TextChangeListSnapshot.Empty

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

  return TextChangeListSnapshot(
    changes = listOf(
      TextChangeListSnapshot.Change(
        range = TextRange(prefix, after.length - suffix),
        originalRange = TextRange(prefix, before.length - suffix),
      ),
    ),
  )
}
