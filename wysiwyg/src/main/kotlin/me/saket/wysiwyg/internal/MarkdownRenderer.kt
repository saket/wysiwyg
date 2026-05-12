package me.saket.wysiwyg.internal

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.parser.LocalTextRange
import me.saket.wysiwyg.parser.MarkdownChildNode
import me.saket.wysiwyg.parser.MarkdownDocument
import me.saket.wysiwyg.parser.TextChangeListSnapshot
import me.saket.wysiwyg.parser.editsOverlap
import me.saket.wysiwyg.parser.rebased

internal class MarkdownRenderer(
  private val theme: WysiwygTheme,
  private val layoutInfo: TextFieldLayoutInfo,
) {
  fun render(
    document: MarkdownDocument,
    buffer: MarkdownStyleBuffer,
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    density: Density,
  ) {
    val scope = RealMarkdownNodeRenderScope(
      theme = theme,
      textMeasurer = textMeasurer,
      textStyle = textStyle,
      densityContext = density,
      changes = document.changes,
      offsetInRoot = 0,
      viewport = layoutInfo.currentViewport(),
    )
    with(document) {
      scope.render(buffer)
    }
  }
}

// todo: kdoc
// todo: move to its own file
interface MarkdownNodeRenderScope : Density {
  val theme: WysiwygTheme
  val textMeasurer: TextMeasurer
  val textStyle: TextStyle

  // todo: kdoc
  val changes: List<TextChangeListSnapshot>

  // todo: kdoc
  val offsetInRoot: Int

  // todo: kdoc
  fun childScope(child: MarkdownChildNode): MarkdownNodeRenderScope

  /**
   * Resolves this local range to its absolute position in the rendered text.
   *
   * When [dropOnEdit] is true, returns `null` if any edit overlaps this range. The caller's
   * `?: return` then drops the node's styling for one frame until the reparse arrives. Use
   * it for fixed-shape markers (emphasis's `**`, a link's `]`, a list item's `-`, a
   * blockquote's `>`) where any edit invalidates the syntax. Skip it for repeatable markers
   * like a heading's `#`s.
   */
  fun LocalTextRange.resolve(dropOnEdit: Boolean = false): TextRange?

  // todo: kdoc
  fun MarkdownChildNode.render(buffer: MarkdownStyleBuffer) {
    val childScope = childScope(this)
    with(node) {
      childScope.render(buffer)
    }
  }
}

// todo: this doesn't have to be a data class.
private data class RealMarkdownNodeRenderScope(
  override val theme: WysiwygTheme,
  override val textMeasurer: TextMeasurer,
  override val textStyle: TextStyle,
  private val densityContext: Density,
  override val changes: List<TextChangeListSnapshot>,
  override val offsetInRoot: Int,
  private val viewport: TextFieldViewport,
) : MarkdownNodeRenderScope, Density by densityContext {

  override fun childScope(child: MarkdownChildNode): MarkdownNodeRenderScope {
    return copy(
      offsetInRoot = offsetInRoot + child.offsetInParent,
    )
  }

  override fun LocalTextRange.resolve(dropOnEdit: Boolean): TextRange? {
    val rangeInRoot = TextRange(
      start = localStart + offsetInRoot,
      end = localEnd + offsetInRoot,
    )
    val rebased = if (dropOnEdit && changes.editsOverlap(rangeInRoot)) {
      null
    } else {
      rangeInRoot.rebased(changes)
    }
    // Skip nodes whose absolute range falls outside the visible viewport.
    return if (rebased == null || viewport.intersects(rebased, includeBeyondViewport = true)) {
      rebased
    } else {
      null
    }
  }
}
