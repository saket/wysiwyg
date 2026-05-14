package me.saket.wysiwyg.render

import androidx.compose.foundation.text.input.TextFieldBuffer
import dev.drewhamilton.poko.Poko
import me.saket.wysiwyg.MarkdownSpanPainter
import me.saket.wysiwyg.parser.MarkdownNode
import me.saket.wysiwyg.parser.TextChangeListSnapshot

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