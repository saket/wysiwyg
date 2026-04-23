package me.saket.wysiwyg.extendedspans

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import me.saket.extendedspans.ExtendedSpanPainter
import me.saket.extendedspans.SpanDrawInstructions
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class BlockQuoteSpanPainter(private val markerColor: Color) : ExtendedSpanPainter() {
  override fun decorate(
    span: SpanStyle,
    start: Int,
    end: Int,
    text: AnnotatedString,
    builder: AnnotatedString.Builder
  ): SpanStyle = span

  override fun drawInstructionsFor(layoutResult: TextLayoutResult): SpanDrawInstructions {
    val text = layoutResult.layoutInput.text
    val blockQuotes = text.paragraphStyles.filter { it.item.textIndent != null }

    return SpanDrawInstructions {
      blockQuotes.fastForEach { annotation ->
        val box = layoutResult.getParagraphBox(annotation.start, annotation.end)
        drawRoundRect(
          color = markerColor,
          topLeft = box.topLeft,
          size = Size(4f.dp.toPx(), box.height),
          cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
      }
    }
  }
}

internal fun TextLayoutResult.getParagraphBox(startOffset: Int, endOffset: Int): Rect {
  val startLineNum = getLineForOffset(startOffset)
  val endLineNum = getLineForOffset(endOffset)
  return Rect(
    top = getLineTop(startLineNum),
    bottom = getLineBottom(endLineNum),
    left = 0f,
    right = size.width.toFloat()
  )
}
