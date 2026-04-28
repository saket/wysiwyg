package me.saket.wysiwyg

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap
import me.saket.extendedspans.ExtendedSpanPainter
import me.saket.extendedspans.RoundedCornerSpanPainter
import me.saket.extendedspans.SpanDrawInstructions
import me.saket.wysiwyg.extendedspans.BlockQuoteSpanPainter
import me.saket.wysiwyg.extendedspans.ThematicBreakSpanPainter

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
) {
  val extendedSpans = remember {
    ExtendedSpansForTextField(
      listOf(
        RoundedCornerSpanPainter(
          cornerRadius = 4.sp,
          padding = RoundedCornerSpanPainter.TextPaddingValues(horizontal = 2.sp),
          topMargin = 2.sp,
          bottomMargin = 2.sp,
          stroke = null,
        ),
        BlockQuoteSpanPainter(wysiwyg.theme.markerColor),
        ThematicBreakSpanPainter(wysiwyg.theme.markerColor),
      )
    )
  }

  BasicTextField(
    state = wysiwyg.textState,
    modifier = modifier.drawBehind { extendedSpans.drawBehind(this) },
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
        extendedSpans.onTextLayout(result)
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
private class ExtendedSpansForTextField(private val painters: List<ExtendedSpanPainter>) {
  private var instructions = mutableStateOf(emptyList<SpanDrawInstructions>())

  fun drawBehind(scope: DrawScope) {
    // todo: account for the text field's scroll state.
    //instructions.value.fastForEach { instruction ->
    //  with(instruction) {
    //    scope.draw()
    //  }
    //}
  }

  fun onTextLayout(result: TextLayoutResult) {
    instructions.value = painters.fastMap { it.drawInstructionsFor(result) }
  }
}