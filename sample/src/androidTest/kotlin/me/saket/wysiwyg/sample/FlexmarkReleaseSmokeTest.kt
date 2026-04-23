package me.saket.wysiwyg.sample

import androidx.test.ext.junit.runners.AndroidJUnit4
import assertk.assertThat
import assertk.assertions.isNotEmpty
import kotlinx.coroutines.runBlocking
import me.saket.wysiwyg.highlight.ChangeListSnapshot
import me.saket.wysiwyg.highlight.flexmark.FlexmarkMarkdownHighlighter
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.EmptyCoroutineContext

@RunWith(AndroidJUnit4::class)
class FlexmarkReleaseSmokeTest {

  @Test fun can_parse_markdown_in_release() = runBlocking(EmptyCoroutineContext) {
    val result = FlexmarkMarkdownHighlighter().highlight(
      text = """
        |# Wysiwyg
        |
        |Markdown is a **lightweight** and easy-to-use `syntax` for styling all forms of ~~web~~ writing.
      """.trimMargin(),
      changes = ChangeListSnapshot.Empty,
    )
    assertThat(result.spans).isNotEmpty()
  }
}
