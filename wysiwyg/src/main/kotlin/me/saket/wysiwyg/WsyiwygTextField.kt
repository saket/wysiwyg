package me.saket.wysiwyg

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldDecorator
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.then
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.tracing.trace
import me.saket.wysiwyg.extendedspans.TaskCheckboxSpanPainter
import me.saket.wysiwyg.internal.RealWysiwyg

// todo: doc.
@Composable
fun WsyiwygTextField(
  wysiwyg: Wysiwyg,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  inputTransformation: InputTransformation? = null,
  textStyle: TextStyle = TextStyle.Default,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  onKeyboardAction: KeyboardActionHandler? = null,
  lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
  onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
  interactionSource: MutableInteractionSource? = null,
  cursorBrush: Brush = WsyiwygTextFieldDefaults.CursorBrush,
  outputTransformation: OutputTransformation? = null,
  decorator: TextFieldDecorator? = null,
  scrollState: ScrollState = rememberScrollState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
) {
  check(wysiwyg is RealWysiwyg)
  val spanPainters = remember(wysiwyg) {
    MarkdownSpanPainters(wysiwyg)
  }

  // todo: i don't love this block. can it be offloaded into a rememberTextLayoutInfo()?
  val layoutInfo = wysiwyg.layoutInfo
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  SideEffect {
    layoutInfo.scrollState = scrollState
    layoutInfo.contentPaddingTopPx = with(density) {
      contentPadding.calculateTopPadding().toPx()
    }
    layoutInfo.contentPaddingLeftPx = with(density) {
      contentPadding.calculateLeftPadding(layoutDirection).toPx()
    }
  }

  BasicTextField(
    state = wysiwyg.textState,
    modifier = modifier
      .onSizeChanged { layoutInfo.viewportHeightPx = it.height.toFloat() }
      .drawBehind { spanPainters.drawBehind() }
      .pointerInput(wysiwyg) { handleCheckboxTaps(wysiwyg) }
      .padding(contentPadding),
    enabled = enabled,
    readOnly = readOnly,
    textStyle = textStyle,
    keyboardOptions = keyboardOptions,
    onKeyboardAction = onKeyboardAction,
    lineLimits = lineLimits,
    interactionSource = interactionSource,
    cursorBrush = cursorBrush,
    inputTransformation = inputTransformation.maybeThen(wysiwyg.inputTransformation),
    outputTransformation = outputTransformation.maybeThen(wysiwyg.outputTransformation),
    decorator = decorator,
    scrollState = scrollState,
    onTextLayout = { result ->
      if (onTextLayout != null) {
        onTextLayout(result)
      }

      // todo: find out why result is a lambda. do i need to evaluate it on every call?
      val result = result()
      if (result != null) {
        layoutInfo.lastLayoutResult = result
      }
    },
  )
}

@Stable
private fun InputTransformation?.maybeThen(next: InputTransformation): InputTransformation {
  return this?.then(next) ?: next
}

@Stable
private fun OutputTransformation?.maybeThen(next: OutputTransformation): OutputTransformation {
  if (this == null) {
    return next
  } else {
    val left = this
    return OutputTransformation {
      val buffer = this
      with(left) {
        buffer.transformOutput()
      }
      with(next) {
        buffer.transformOutput()
      }
    }
  }
}

private object WsyiwygTextFieldDefaults {
  val CursorBrush = SolidColor(Color.Black)
}

@Stable
private class MarkdownSpanPainters(val wysiwyg: RealWysiwyg) {
  context(scope: DrawScope)
  fun drawBehind() {
    trace("Wysiwyg:drawBehind") {
      val layoutResult = wysiwyg.layoutInfo.lastLayoutResult ?: return
      val painters = wysiwyg.outputTransformation.styleBuffer.spanPainters
      val viewport = wysiwyg.layoutInfo.currentViewport()

      scope.translate(viewport.translationX, viewport.translationY) {
        val translatedScope = this
        painters.fastForEach { painter ->
          if (viewport.intersects(painter.range)) {
            with(painter) {
              translatedScope.draw(layoutResult)
            }
          }
        }
      }
    }
  }
}

private suspend fun PointerInputScope.handleCheckboxTaps(wysiwyg: RealWysiwyg) {
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

    // Tap confirmed. Consume the up so BasicTextField's tap detector skips cursor placement.
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

