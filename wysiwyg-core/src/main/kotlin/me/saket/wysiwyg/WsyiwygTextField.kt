package me.saket.wysiwyg

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.util.fastForEach
import androidx.tracing.trace
import me.saket.wysiwyg.render.spans.toggleTaskCheckboxesOnClick
import me.saket.wysiwyg.internal.RealMarkdownOutputTransformation
import me.saket.wysiwyg.internal.RealWysiwyg
import me.saket.wysiwyg.render.AnnotatedStringMarkdownRendererFactory
import me.saket.wysiwyg.render.MarkdownRenderer
import me.saket.wysiwyg.render.rememberMarkdownRenderScope

// todo: doc.
//  @param contentPadding is used so that span painters can draw outside the bounds. in the future
//   it should also support https://issuetracker.google.com/issues/302599627
@Composable
fun WsyiwygTextField(
  wysiwyg: Wysiwyg,
  modifier: Modifier = Modifier,
  theme: WysiwygTheme = if (isSystemInDarkTheme()) WysiwygTheme.GithubDark else WysiwygTheme.GithubLight,
  markdownRenderer: MarkdownRenderer.Factory = AnnotatedStringMarkdownRendererFactory,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  inputTransformation: InputTransformation? = null,
  textStyle: TextStyle = TextStyle.Default,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  onKeyboardAction: KeyboardActionHandler? = null,
  lineLimits: TextFieldLineLimits = TextFieldLineLimits.Default,
  onTextLayout: (Density.(getResult: () -> TextLayoutResult?) -> Unit)? = null,
  interactionSource: MutableInteractionSource? = null,
  cursorBrush: Brush = WsyiwygDefaults.CursorBrush,
  outputTransformation: OutputTransformation? = null,
  decorator: TextFieldDecorator? = null,
  scrollState: ScrollState = rememberScrollState(),
  contentPadding: PaddingValues = PaddingValues(0.dp),
) {
  check(wysiwyg is RealWysiwyg)

  val textStyle = textStyle.copy(
    // Drawing of rounded corner backgrounds is incompatible with unspecified font sizes.
    fontSize = textStyle.fontSize.takeOrElse { WsyiwygDefaults.FontSize },
  )
  val renderScope = rememberMarkdownRenderScope(theme, textStyle, wysiwyg.layoutInfo)
  val markdownOutputTransformation = remember(wysiwyg, markdownRenderer, renderScope) {
    RealMarkdownOutputTransformation(wysiwyg, markdownRenderer, renderScope)
  }

  // todo: i don't love this block. can it be offloaded into a rememberTextLayoutInfo()?
  val layoutInfo = wysiwyg.layoutInfo
  val density = LocalDensity.current
  val layoutDirection = LocalLayoutDirection.current
  SideEffect {
    layoutInfo.scrollState = scrollState
    layoutInfo.updateContentPadding(contentPadding, density, layoutDirection)
  }

  BasicTextField(
    state = wysiwyg.textState,
    modifier = modifier
      .onSizeChanged {
        layoutInfo.viewportSize = it.toSize()
      }
      .drawSpanPainters(wysiwyg)
      .toggleTaskCheckboxesOnClick(theme, wysiwyg)
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
    outputTransformation = outputTransformation.maybeThen(markdownOutputTransformation),
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

private object WsyiwygDefaults {
  val CursorBrush = SolidColor(Color.Black)

  // Copied from:
  // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui-text/src/androidHostTest/kotlin/androidx/compose/ui/text/TextStyleResolveDefaultsTest.kt;l=46;drc=0cec77f52e06e6b548e76ca0ab161c42feebe371
  val FontSize = 14.sp
}

private fun Modifier.drawSpanPainters(wysiwyg: RealWysiwyg): Modifier {
  return this.drawBehind {
    trace("Wysiwyg:drawBehind") {
      val layoutResult = wysiwyg.layoutInfo.lastLayoutResult ?: return@trace
      val painters = wysiwyg.currentRenderResult.spanPainters
      val viewport = wysiwyg.layoutInfo.currentViewport()
      val contentBounds = wysiwyg.layoutInfo.contentBounds

      painters.fastForEach { painter ->
        if (viewport.intersects(painter.range)) {
          val drawSlop = with(painter) { drawSlop() }
          clipRect(
            left = contentBounds.left - drawSlop.left,
            top = contentBounds.top - drawSlop.top,
            right = contentBounds.right + drawSlop.right,
            bottom = contentBounds.bottom + drawSlop.bottom,
          ) {
            translate(viewport.translationX, viewport.translationY) {
              with(painter) {
                draw(layoutResult)
              }
            }
          }
        }
      }
    }
  }
}
