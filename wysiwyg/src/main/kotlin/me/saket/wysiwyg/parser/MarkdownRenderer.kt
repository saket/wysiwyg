package me.saket.wysiwyg.parser

/**
 * Renders a [MarkdownDocument] into some output (styled text, HTML, plain text, etc.). Implementations
 * dispatch on node type via `when (node)` for the cases they care about and call [descendInto]
 * to recurse into children.
 *
 * Implementations are responsible for detecting [MarkdownDocument] at the top of a pass and resetting any
 * scope state (offset, changes) themselves; [MarkdownRenderScope] does not manage lifecycle.
 *
 * Nodes are pure data. Behaviour lives here. Consumers that introduce their own node types can
 * delegate to a built-in renderer (typically [me.saket.wysiwyg.internal.AnnotatedStringRenderer])
 * for the standard cases and handle their own types in their own arm.
 */
interface MarkdownRenderer {
  fun render(node: MarkdownNode, scope: MarkdownRenderScope)
}

/**
 * Recurses into [node]'s children, pushing each child's offset onto [scope] for the duration of
 * its render and popping it on the way back up. Called by implementations inside their `when` arms after
 * any per-node work.
 */
fun MarkdownRenderer.descendInto(node: MarkdownNode, scope: MarkdownRenderScope) {
  for (child in node.children) {
    scope.offsetInRoot += child.offsetInParent
    render(child.node, scope)
    scope.offsetInRoot -= child.offsetInParent
  }
}
