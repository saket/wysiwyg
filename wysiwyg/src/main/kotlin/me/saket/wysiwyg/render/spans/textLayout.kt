package me.saket.wysiwyg.render.spans

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.style.ResolvedTextDirection

/** Copied from [extended-spans](https://github.com/saket/extended-spans). */
internal fun TextLayoutResult.getBoundingBoxes(
  startOffset: Int,
  endOffset: Int,
  flattenForFullParagraphs: Boolean,
): List<Rect> {
  if (startOffset == endOffset) {
    return emptyList()
  }

  val startLineNum = getLineForOffset(startOffset)
  val endLineNum = getLineForOffset(endOffset)

  if (flattenForFullParagraphs) {
    val isFullParagraph = (startLineNum != endLineNum)
        && getLineStart(startLineNum) == startOffset
        && multiParagraph.getLineEnd(endLineNum, visibleEnd = true) == endOffset

    if (isFullParagraph) {
      // Use line metrics (including leading) so the block's surrounding line
      // leading reads as visual padding inside the box. Tightening to font
      // metrics like the per-line branch below does would make multi-line
      // blocks (e.g. fenced code) look cramped at the top and bottom.
      return listOf(
        Rect(
          top = getLineTop(startLineNum),
          bottom = getLineBottom(endLineNum),
          left = 0f,
          right = size.width.toFloat(),
        ),
      )
    }
  }

  // Compose UI does not offer any API for reading paragraph direction for an entire line,
  // so this assumes all paragraphs in the text run in the same direction and that the
  // paragraph does not contain bi-directional text.
  val isLtr = multiParagraph.getParagraphDirection(
    offset = layoutInput.text.lastIndex
  ) == ResolvedTextDirection.Ltr

  // Use baseline ± font ascent/descent (not line top/bottom) so the rect hugs the
  // glyph. Line metrics carry leading above the cap-height that's not part of any
  // glyph; a rounded background drawn around them sits visibly high.
  val fontSizePx = with(layoutInput.density) { layoutInput.style.fontSize.toPx() }
  val fontAscent = fontSizePx * FontAscentRatio
  val fontDescent = fontSizePx * FontDescentRatio

  return (startLineNum..endLineNum).map { lineNum ->
    val baseline = getLineBaseline(lineNum)
    Rect(
      top = baseline - fontAscent,
      bottom = baseline + fontDescent,
      left = if (lineNum == startLineNum) {
        getHorizontalPosition(startOffset, usePrimaryDirection = isLtr)
      } else {
        getLineLeft(lineNum)
      },
      right = if (lineNum == endLineNum) {
        getHorizontalPosition(endOffset, usePrimaryDirection = isLtr)
      } else {
        getLineRight(lineNum)
      },
    )
  }
}

// Approximate font ascent/descent ratios relative to fontSize. Tuned for sans-serif
// and monospace fonts; varies a few percent across fonts but produces visually
// centered pills regardless.
private const val FontAscentRatio = 0.93f
private const val FontDescentRatio = 0.24f

