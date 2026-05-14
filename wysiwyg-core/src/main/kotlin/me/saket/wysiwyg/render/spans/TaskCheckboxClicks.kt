package me.saket.wysiwyg.render.spans

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.internal.RealWysiwyg

@Composable
internal fun Modifier.toggleTaskCheckboxesOnClick(
  theme: WysiwygTheme,
  wysiwyg: RealWysiwyg,
): Modifier {
  val clicks = remember(theme, wysiwyg) {
    TaskCheckboxClicks(theme, wysiwyg)
  }
  LaunchedEffect(clicks) {
    clicks.observe()
  }

  return with(clicks) {
    this@toggleTaskCheckboxesOnClick
      .drawBehind { drawIndicators() }
      .pointerInput(this) { detectClicks() }
  }
}

/**
 * Detects taps on task list checkbox markers and animates a fading touch
 * indicator on the tapped marker for the duration of the press.
 */
@Stable
internal class TaskCheckboxClicks(
  private val theme: WysiwygTheme,
  private val wysiwyg: RealWysiwyg,
) {
  private val presses = MutableSharedFlow<CheckboxPressEvent>(
    // extraBufferCapacity = 1 covers the startup race window where the gesture
    // handler may emit before observe() first suspends in collect.
    extraBufferCapacity = 1,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
  )

  private val activePresses = mutableStateListOf<CheckboxPressEvent>()

  suspend fun observe() {
    coroutineScope {
      presses.collect { press ->
        activePresses.add(press)
        launch {
          press.alpha.animateTo(0.3f, animationSpec = tween(150))
          press.terminal.await()
          press.alpha.animateTo(0f, animationSpec = tween(200))
          activePresses.remove(press)
        }
      }
    }
  }

  fun DrawScope.drawIndicators() {
    val viewport = wysiwyg.layoutInfo.currentViewport()
    val color = theme.markerColor // todo: can this use the text color instead?

    translate(viewport.translationX, viewport.translationY) {
      activePresses.fastForEach { press ->
        drawCircle(
          color = color.copy(alpha = press.alpha.value),
          radius = press.bounds.height,
          center = press.bounds.center,
        )
      }
    }
  }

  suspend fun PointerInputScope.detectClicks() {
    awaitEachGesture {
      val down = awaitFirstDown(requireUnconsumed = true, pass = PointerEventPass.Initial)
      val checkbox = findCheckboxAt(down.position) ?: return@awaitEachGesture
      val layoutResult = wysiwyg.layoutInfo.lastLayoutResult ?: return@awaitEachGesture
      val bounds = checkbox.boundsIn(layoutResult) ?: return@awaitEachGesture

      val press = CheckboxPressEvent(bounds)
      presses.tryEmit(press)

      try {
        // Don't consume the down: a swipe starting on a checkbox should still scroll the
        // document. If the gesture turns into a drag, the scroll modifier consumes a move
        // and waitForUpOrCancellation returns null, so we naturally bail.
        val up = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
          waitForUpOrCancellation(PointerEventPass.Initial)
        }

        if (up != null) {
          val movedDistance = (up.position - down.position).getDistance()
          val stillOnCheckbox = findCheckboxAt(up.position) === checkbox
          if (movedDistance <= viewConfiguration.touchSlop && stillOnCheckbox) {
            // Click confirmed. Consume the up so that the text field's
            // click detector skips cursor placement.
            up.consume()
            press.terminal.complete(Unit)
            wysiwyg.textState.edit {
              replace(
                start = checkbox.range.start,
                end = checkbox.range.end,
                text = if (checkbox.isChecked) "[ ]" else "[x]",
              )
            }
            return@awaitEachGesture
          }
        }
      } finally {
        press.terminal.complete(Unit)
      }
    }
  }

  private fun findCheckboxAt(position: Offset): TaskCheckboxSpanPainter? {
    val layoutResult = wysiwyg.layoutInfo.lastLayoutResult ?: return null
    val viewport = wysiwyg.layoutInfo.currentViewport()
    val docPosition = Offset(
      x = position.x - viewport.translationX,
      y = position.y - viewport.translationY,
    )

    val offsetUnderTouch = layoutResult.getOffsetForPosition(docPosition)
    wysiwyg.currentRenderResult.spanPainters.fastForEach { painter ->
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
}

/** One in-flight finger press on a checkbox marker. */
private class CheckboxPressEvent(val bounds: Rect) {
  val terminal = CompletableDeferred<Unit>()
  val alpha = Animatable(0f)
}
