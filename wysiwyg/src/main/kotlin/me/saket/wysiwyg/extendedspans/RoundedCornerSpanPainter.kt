package me.saket.wysiwyg.extendedspans

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import me.saket.extendedspans.ExtendedSpanPainter
import me.saket.extendedspans.SpanDrawInstructions
import me.saket.wysiwyg.MarkdownSpanPainter

/**
 * Draws a rounded background behind [range]. When the range covers entire
 * paragraphs (e.g. fenced code blocks), a single full-width box is drawn.
 * Otherwise the shape follows the text on each line and rounds only the
 * outermost corners.
 *
 * Modeled after [extended-spans](https://github.com/saket/extended-spans).
 */
class RoundedCornerSpanPainter(
  private val range: TextRange,
  private val backgroundColor: Color,
  private val cornerRadius: TextUnit = 8.sp,
  private val padding: TextPaddingValues,
  private val topMargin: TextUnit,
  private val bottomMargin: TextUnit,
) : MarkdownSpanPainter {

  override fun DrawScope.draw(layoutResult: TextLayoutResult) {
    val cornerRadius = CornerRadius(cornerRadius.toPx())
    val boxes = layoutResult.getBoundingBoxes(
      startOffset = range.start,
      endOffset = range.end,
      flattenForFullParagraphs = true,
    )
    boxes.fastForEachIndexed { index, box ->
      path.rewind()
      path.addRoundRect(
        RoundRect(
          rect = box.copy(
            left = box.left - padding.horizontal.toPx(),
            right = box.right + padding.horizontal.toPx(),
            top = box.top - padding.vertical.toPx() + topMargin.toPx(),
            bottom = box.bottom + padding.vertical.toPx() - bottomMargin.toPx(),
          ),
          topLeft = if (index == 0) cornerRadius else CornerRadius.Zero,
          bottomLeft = if (index == 0) cornerRadius else CornerRadius.Zero,
          topRight = if (index == boxes.lastIndex) cornerRadius else CornerRadius.Zero,
          bottomRight = if (index == boxes.lastIndex) cornerRadius else CornerRadius.Zero,
        ),
      )
      drawPath(
        path = path,
        color = backgroundColor,
        style = Fill,
      )
    }
  }

  data class TextPaddingValues(
    val horizontal: TextUnit = 0.sp,
    val vertical: TextUnit = 0.sp,
  ) {
    constructor(all: TextUnit) : this(horizontal = all, vertical = all)
  }

  companion object {
    // Shared across painter instances. Safe because Compose draws happen sequentially
    // on the UI thread, and each draw consumes the path before returning.
    private val path = Path()
  }
}

private fun TextLayoutResult.getBoundingBoxes(
  startOffset: Int,
  endOffset: Int,
  flattenForFullParagraphs: Boolean,
): List<Rect> {
  return ExtendedSpanPainterBridge.boundingBoxesFor(
    layoutResult = this,
    startOffset = startOffset,
    endOffset = endOffset,
    flattenForFullParagraphs = flattenForFullParagraphs,
  )
}

// Subclass to expose ExtendedSpanPainter#getBoundingBoxes(), which is `protected`.
// Reusing it (rather than re-implementing) keeps RTL handling and full-paragraph
// detection in lockstep with upstream.
private object ExtendedSpanPainterBridge : ExtendedSpanPainter() {
  override fun decorate(
    span: SpanStyle,
    start: Int,
    end: Int,
    text: AnnotatedString,
    builder: AnnotatedString.Builder,
  ): SpanStyle = error("unused")

  override fun drawInstructionsFor(layoutResult: TextLayoutResult): SpanDrawInstructions =
    error("unused")

  fun boundingBoxesFor(
    layoutResult: TextLayoutResult,
    startOffset: Int,
    endOffset: Int,
    flattenForFullParagraphs: Boolean,
  ): List<Rect> = layoutResult.getBoundingBoxes(startOffset, endOffset, flattenForFullParagraphs)
}
