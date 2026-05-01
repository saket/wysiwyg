package me.saket.wysiwyg.internal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

/**
 * Vertical slice of a [TextLayoutResult] currently on screen, expressed in the
 * same coordinate space spans are drawn in. Used to skip span painters whose
 * ranges are scrolled out of view.
 */
internal class TextFieldViewport(
  private val firstVisibleTextOffset: Int,
  private val lastVisibleTextOffset: Int,
) {
  fun intersects(range: TextRange): Boolean {
    return range.start <= lastVisibleTextOffset && range.end >= firstVisibleTextOffset
  }
}

context(scope: DrawScope)
internal fun TextLayoutResult.computeViewport(
  scrollState: ScrollState,
  contentPadding: PaddingValues,
): TextFieldViewport {
  val topPadding = with(scope) { contentPadding.calculateTopPadding().toPx() }
  val top = scrollState.value.toFloat() - topPadding
  val bottom = top + scope.size.height

  val firstLine = getLineForVerticalPosition(top)
  val lastLine = getLineForVerticalPosition(bottom)

  // getLineForVerticalPosition clamps to the nearest line when y is outside the
  // text. Verify the resolved lines actually overlap the requested viewport so
  // we don't keep drawing line-0 spans when the viewport sits entirely above
  // (or below) the text.
  return if (getLineBottom(firstLine) < top || getLineTop(lastLine) > bottom) {
    TextFieldViewport(
      firstVisibleTextOffset = Int.MAX_VALUE,
      lastVisibleTextOffset = Int.MIN_VALUE,
    )
  } else {
    TextFieldViewport(
      firstVisibleTextOffset = getLineStart(firstLine),
      lastVisibleTextOffset = getLineEnd(lastLine),
    )
  }
}
