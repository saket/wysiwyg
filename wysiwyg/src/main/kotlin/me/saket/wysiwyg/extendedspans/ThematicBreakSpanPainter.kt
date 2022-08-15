package me.saket.wysiwyg.extendedspans

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import me.saket.extendedspans.ExtendedSpanPainter
import me.saket.extendedspans.SpanDrawInstructions

class ThematicBreakSpanPainter(private val syntaxColor: Color) : ExtendedSpanPainter() {
  override fun decorate(
    span: SpanStyle,
    start: Int,
    end: Int,
    text: AnnotatedString,
    builder: AnnotatedString.Builder
  ): SpanStyle = span

  override fun drawInstructionsFor(layoutResult: TextLayoutResult): SpanDrawInstructions {
    val text = layoutResult.layoutInput.text
    val annotations = text.getStringAnnotations("thematic_break", start = 0, end = text.length)

    return SpanDrawInstructions {
      annotations.fastForEach { annotation ->
        val box = layoutResult.getParagraphBox(annotation.start, annotation.end)
        drawLine(
          color = syntaxColor.copy(alpha = 0.4f),
          start = box.centerLeft,
          end = box.centerRight,
          strokeWidth = 4.dp.toPx()
        )
      }
    }
  }
}
