package me.saket.wysiwyg.extendedspans

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import me.saket.wysiwyg.MarkdownSpanPainter

internal class TaskCheckboxSpanPainter(
  override val range: TextRange,
  val isChecked: Boolean,
) : MarkdownSpanPainter {

  override fun DrawScope.draw(layoutResult: TextLayoutResult) {
    // Phase 3 will draw a real checkbox using boundsIn(layoutResult).
  }
}
