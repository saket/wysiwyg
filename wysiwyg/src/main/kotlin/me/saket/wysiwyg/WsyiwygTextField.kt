package me.saket.wysiwyg

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.tracing.trace

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
  val spanPainters = remember(wysiwyg) {
    MarkdownSpanPainters(wysiwyg)
  }

  BasicTextField(
    state = wysiwyg.textState,
    modifier = modifier
      .drawBehind { spanPainters.drawBehind(scrollState, contentPadding) }
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
        spanPainters.onTextLayout(result)
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
private class MarkdownSpanPainters(val wysiwyg: Wysiwyg) {
  private var lastLayoutResult: TextLayoutResult? by mutableStateOf(null)

  // todo (future improvement): skip painters whose ranges are outside the visible viewport.
  context(scope: DrawScope)
  fun drawBehind(scrollState: ScrollState, contentPadding: PaddingValues) {
    trace("Wysiwyg:drawBehind") {
      val layoutResult = lastLayoutResult ?: return
      val painters = wysiwyg.outputTransformation.lastRenderResult?.extraSpanPainters.orEmpty()

      val startPadding = with(scope) { contentPadding.calculateStartPadding(layoutDirection).toPx() }
      val topPadding = with(scope) { contentPadding.calculateTopPadding().toPx() }

      scope.clipRect {
        translate(left = startPadding, top = topPadding - scrollState.value.toFloat()) {
          val translatedScope = this
          painters.fastForEach { painter ->
            with(painter) {
              translatedScope.draw(layoutResult)
            }
          }
        }
      }
    }
  }

  fun onTextLayout(result: TextLayoutResult) {
    lastLayoutResult = result
  }
}
