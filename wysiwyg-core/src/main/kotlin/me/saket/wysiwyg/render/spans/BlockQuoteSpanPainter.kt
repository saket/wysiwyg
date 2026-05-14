package me.saket.wysiwyg.render.spans

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import me.saket.wysiwyg.MarkdownSpanPainter

internal class BlockQuoteSpanPainter(
  override val range: TextRange,
  private val markerColor: Color,
) : MarkdownSpanPainter {

  override fun DrawScope.draw(layoutResult: TextLayoutResult) {
    val box = layoutResult.getParagraphBox(range.start, range.end)
    drawRoundRect(
      color = markerColor,
      topLeft = box.topLeft,
      size = Size(4f.dp.toPx(), box.height),
      cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
    )
  }
}

internal fun TextLayoutResult.getParagraphBox(startOffset: Int, endOffset: Int): Rect {
  val startLineNum = getLineForOffset(startOffset)
  val endLineNum = getLineForOffset(endOffset)
  return Rect(
    top = getLineTop(startLineNum),
    bottom = getLineBottom(endLineNum),
    left = 0f,
    right = size.width.toFloat(),
  )
}
