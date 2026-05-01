package me.saket.wysiwyg

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

// todo: kdoc
interface MarkdownSpanPainter {
  /**
   * Text range this painter draws against.
   * Used to skip painters that aren't visible in the viewport.
   */
  val range: TextRange

  fun DrawScope.draw(layoutResult: TextLayoutResult)
}
