package me.saket.wysiwyg.render

import androidx.compose.ui.text.TextRange
import me.saket.wysiwyg.parser.LocalTextRange
import me.saket.wysiwyg.parser.MarkdownNode
import me.saket.wysiwyg.parser.TextChangeListSnapshot
import me.saket.wysiwyg.parser.editsOverlap
import me.saket.wysiwyg.parser.rebased

// todo: kdoc
interface MarkdownNodeWalkScope : MarkdownRenderScope {
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
}

// todo: kdoc
fun MarkdownRenderScope.walkMarkdownNodes(
  root: MarkdownNode,
  changes: List<TextChangeListSnapshot>,
  visit: MarkdownNodeWalkScope.(MarkdownNode) -> Unit,
) {
  WalkState(scope = this, changes).walk(root, visit)
}

private class WalkState(
  scope: MarkdownRenderScope,
  private val changes: List<TextChangeListSnapshot>,
) : MarkdownNodeWalkScope, MarkdownRenderScope by scope {
  private var offsetInRoot = 0

  fun walk(node: MarkdownNode, visit: MarkdownNodeWalkScope.(MarkdownNode) -> Unit) {
    if (node.range.resolve() == null) {
      // Skip the entire subtree if its range is outside the viewport.
      return
    }
    visit(node)
    for (child in node.children) {
      offsetInRoot += child.offsetInParent
      walk(child.node, visit)
      offsetInRoot -= child.offsetInParent
    }
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
    return if (rebased == null || isInsideViewport(rebased)) {
      rebased
    } else {
      null
    }
  }
}
