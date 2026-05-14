package me.saket.wysiwyg.internal

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * The layout-time state needed to compute the visible-text band of the underlying text field.
 *
 * Name inspired by Compose UI's `LazyListLayoutInfo`.
 */
@Stable
internal class TextFieldLayoutInfo {
  internal var lastLayoutResult: TextLayoutResult? by mutableStateOf(null)
  internal var scrollState: ScrollState? by mutableStateOf(null)
  internal var viewportSize: Size by mutableStateOf(Size.Zero)
  private var contentPadding: Rect by mutableStateOf(Rect.Zero)

  internal val contentBounds: Rect by derivedStateOf(structuralEqualityPolicy()) {
    Rect(
      left = contentPadding.left,
      top = contentPadding.top,
      right = viewportSize.width - contentPadding.right,
      bottom = viewportSize.height - contentPadding.bottom,
    )
  }

  fun updateContentPadding(
    padding: PaddingValues,
    density: Density,
    layoutDirection: LayoutDirection,
  ) = with(density) {
    contentPadding = Rect(
      left = padding.calculateLeftPadding(layoutDirection).toPx(),
      top = padding.calculateTopPadding().toPx(),
      right = padding.calculateRightPadding(layoutDirection).toPx(),
      bottom = padding.calculateBottomPadding().toPx(),
    )
  }

  /**
   * Whether the rendered document fits within the visible viewport.
   *
   * FYI [derivedStateOf] is the subscription barrier here: callers that only read through
   * [currentViewport] track this Boolean, not the per-keystroke churn of [lastLayoutResult]
   * that the block reads internally. Inlining would attribute those reads to the caller.
   *
   * [structuralEqualityPolicy] is essential. Without it, [derivedStateOf] would
   * invalidate downstream readers on every input change regardless of value
   * equivalence, defeating the dedupe a Boolean naturally affords.
   */
  private val documentFitsInViewport by derivedStateOf(structuralEqualityPolicy()) {
    val layout = lastLayoutResult ?: return@derivedStateOf true
    val height = viewportSize.height
    height == 0f || layout.size.height <= height
  }

  fun currentViewport(): TextFieldViewport {
    if (documentFitsInViewport) {
      return TextFieldViewport(
        topPx = 0f,
        bottomPx = 0f,
        beyondViewportPx = viewportSize.height,
        translationX = contentBounds.left,
        translationY = contentBounds.top,
        layout = null,
      )
    }

    val scrollPosition = scrollState?.value ?: 0
    val visibleTop = scrollPosition.toFloat() - contentBounds.top

    return TextFieldViewport(
      topPx = visibleTop,
      bottomPx = visibleTop + viewportSize.height,
      beyondViewportPx = viewportSize.height,
      translationX = contentBounds.left,
      translationY = contentBounds.top - scrollPosition.toFloat(),
      layout = Snapshot.withoutReadObservation {
        // The layout result is read outside snapshot tracking. It changes per keystroke, but the
        // visible pixel band depends only on scroll and padding. Without [withoutReadObservation],
        // transformOutput would re-emit styles per char and an unlucky band could fail to converge.
        lastLayoutResult
      },
    )
  }
}

/**
 * Vertical slice of a [TextLayoutResult] currently on screen, expressed in the
 * same coordinate space spans are drawn in. Used to skip span painters whose
 * ranges are scrolled out of view, and to short-circuit the render walk over
 * offscreen markdown nodes.
 *
 * The band is in pixel coordinates ([topPx], [bottomPx]) so it doesn't shift
 * when text is typed; only scroll, padding, and viewport size move it. Range
 * lookups go through [layout], which is captured outside snapshot tracking so
 * its per-char churn doesn't dirty downstream readers.
 *
 * [translationX] and [translationY] are the canvas translation needed to map
 * text-content coordinates onto the visible viewport, baking together content
 * padding and scroll offset for callers that draw under the text field.
 */
internal class TextFieldViewport(
  private val topPx: Float,
  private val bottomPx: Float,
  private val beyondViewportPx: Float,
  private val layout: TextLayoutResult?,
  val translationX: Float,
  val translationY: Float,
) {
  /**
   * Returns whether [range] intersects the visible band. When [includeBeyondViewport] is true,
   * the band is extended by [beyondViewportPx] on each side to absorb per-frame layout drift.
   */
  fun intersects(
    range: TextRange,
    includeBeyondViewport: Boolean = false,
  ): Boolean {
    val layout = layout ?: return true
    val firstLine = layout.getLineForOffset(range.start)
    val lastLine = layout.getLineForOffset(range.end)
    val slack = if (includeBeyondViewport) beyondViewportPx else 0f
    return layout.getLineTop(firstLine) <= bottomPx + slack &&
        layout.getLineBottom(lastLine) >= topPx - slack
  }
}
