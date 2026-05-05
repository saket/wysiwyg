package me.saket.wysiwyg.extendedspans

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.util.fastForEach
import me.saket.wysiwyg.MarkdownSpanPainter
import me.saket.wysiwyg.internal.RealWysiwyg

internal class TaskCheckboxSpanPainter(
  override val range: TextRange,
  val isChecked: Boolean,
) : MarkdownSpanPainter {

  override fun DrawScope.draw(layoutResult: TextLayoutResult) {
    // Phase 3 will draw a real checkbox using boundsIn(layoutResult).
  }
}

internal suspend fun PointerInputScope.handleCheckboxClicks(wysiwyg: RealWysiwyg) {
  awaitEachGesture {
    val down = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Initial)
    val checkbox = wysiwyg.findCheckboxAt(down.position) ?: return@awaitEachGesture

    // Don't consume the down: a swipe starting on a checkbox should still scroll the
    // document. If the gesture turns into a drag, the scroll modifier consumes a move
    // and waitForUpOrCancellation returns null, so we naturally bail.
    val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
      waitForUpOrCancellation(PointerEventPass.Initial)
    } ?: return@awaitEachGesture

    val movedDistance = (up.position - down.position).getDistance()
    if (movedDistance > viewConfiguration.touchSlop) return@awaitEachGesture
    if (wysiwyg.findCheckboxAt(up.position) !== checkbox) return@awaitEachGesture

    // Click confirmed. Consume the up so BasicTextField's click detector skips cursor placement.
    up.consume()

    wysiwyg.textState.edit {
      replace(
        start = checkbox.range.start,
        end = checkbox.range.end,
        text = if (checkbox.isChecked) "[ ]" else "[x]",
      )
    }
  }
}

private fun RealWysiwyg.findCheckboxAt(position: Offset): TaskCheckboxSpanPainter? {
  val layoutResult = layoutInfo.lastLayoutResult ?: return null
  val viewport = layoutInfo.currentViewport()
  val docPosition = Offset(
    x = position.x - viewport.translationX,
    y = position.y - viewport.translationY,
  )

  val offsetUnderTouch = layoutResult.getOffsetForPosition(docPosition)
  outputTransformation.styleBuffer.spanPainters.fastForEach { painter ->
    if (painter is TaskCheckboxSpanPainter) {
      if (painter.range.start > offsetUnderTouch) {
        // This painter starts past the touch. Every later one starts even later, so bail.
        // This assumes that span painters are sorted by their range, which is okay for now.
        return null
      }
      if (offsetUnderTouch in painter.range) return painter
    }
  }
  return null
}

