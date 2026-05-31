package me.saket.wysiwyg.render.spans

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import me.saket.wysiwyg.MarkdownSpanPainter

internal class ThematicBreakSpanPainter(
  override val range: TextRange,
  private val markerColor: Color,
) : MarkdownSpanPainter {

  override fun DrawScope.draw(layoutResult: TextLayoutResult) {
    val box = layoutResult.getParagraphBox(range.start, range.end)
    val centerY = layoutResult.getInkCenterY(range.start) ?: box.center.y
    val strokeWidth = 2.dp.toPx()
    val capRadius = strokeWidth / 2f
    drawLine(
      color = markerColor.copy(alpha = 0.4f),
      start = Offset(box.left + capRadius, centerY),
      end = Offset(box.right - capRadius, centerY),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Round,
    )
  }
}
