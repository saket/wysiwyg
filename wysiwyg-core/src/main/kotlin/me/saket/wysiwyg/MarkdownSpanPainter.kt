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

  fun DrawScope.drawSlop(): DrawSlop = DrawSlop.Zero

  class DrawSlop(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
  ) {
    companion object {
      val Zero = DrawSlop(left = 0f, top = 0f, right = 0f, bottom = 0f)
    }
  }
}
