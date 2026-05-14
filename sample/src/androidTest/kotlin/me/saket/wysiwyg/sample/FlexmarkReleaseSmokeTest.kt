package me.saket.wysiwyg.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isNotEmpty
import kotlinx.coroutines.runBlocking
import me.saket.wysiwyg.parser.TextChangeListSnapshot
import me.saket.wysiwyg.parser.flexmark.FlexmarkMarkdownParser
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(AndroidJUnit4::class)
class FlexmarkReleaseSmokeTest {

  @Test fun can_parse_markdown_in_release() = runBlocking(EmptyCoroutineContext) {
    val result = FlexmarkMarkdownParser().parse(
      text = """
        |# Wysiwyg
        |
        |Markdown is a **lightweight** and easy-to-use `syntax` for styling all forms of ~~web~~ writing.
      """.trimMargin(),
      changes = TextChangeListSnapshot.Empty,
    )
    assertThat(result.children).isNotEmpty()
  }
}
