package me.saket.wysiwyg.extendedspans

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import me.saket.wysiwyg.MarkdownSpanPainter

class ThematicBreakSpanPainter(
  override val range: TextRange,
  private val markerColor: Color,
) : MarkdownSpanPainter {

  override fun DrawScope.draw(layoutResult: TextLayoutResult) {
    val box = layoutResult.getParagraphBox(range.start, range.end)
    drawLine(
      color = markerColor.copy(alpha = 0.4f),
      start = box.centerLeft,
      end = box.centerRight,
      strokeWidth = 4.dp.toPx(),
    )
  }
}
