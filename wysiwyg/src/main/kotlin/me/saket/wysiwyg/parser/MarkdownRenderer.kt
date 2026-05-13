package me.saket.wysiwyg.parser

import androidx.compose.foundation.text.input.TextFieldBuffer
import dev.drewhamilton.poko.Poko
import me.saket.wysiwyg.MarkdownSpanPainter
import me.saket.wysiwyg.internal.AnnotatedStringMarkdownRenderer
import me.saket.wysiwyg.internal.AnnotatedStringMarkdownRenderer as Impl

// todo: kdoc
fun interface MarkdownRenderer {
  fun MarkdownRenderScope.render(node: MarkdownNode): RenderResult

  fun interface Factory {
    // todo: kdoc.
    fun create(
      buffer: TextFieldBuffer,
      unstyledText: String,
      changes: List<TextChangeListSnapshot>,
    ): MarkdownRenderer
  }
}

// todo: kdoc
@Poko class RenderResult(
  val spanPainters: List<MarkdownSpanPainter>,
) {
  companion object {
    val Empty: RenderResult = RenderResult(spanPainters = emptyList())
  }
}

data object AnnotatedStringMarkdownRendererFactory : MarkdownRenderer.Factory {
  override fun create(
    buffer: TextFieldBuffer,
    unstyledText: String,
    changes: List<TextChangeListSnapshot>
  ): MarkdownRenderer {
    return AnnotatedStringMarkdownRenderer(buffer, unstyledText, changes)
  }
}