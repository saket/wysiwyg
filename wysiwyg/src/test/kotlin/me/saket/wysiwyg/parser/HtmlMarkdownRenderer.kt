package me.saket.wysiwyg.parser

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Density
import me.saket.wysiwyg.WysiwygTheme
import me.saket.wysiwyg.internal.isAtxHeadingMarker

internal fun MarkdownDocument.renderHtml(source: String): String {
  val renderer = HtmlMarkdownRenderer(source, changes = this.changes)
  with(renderer) {
    FakeMarkdownRenderScope.render(this@renderHtml)
  }
  return renderer.toHtml()
}

internal class HtmlMarkdownRenderer(
  private val source: String,
  private val changes: List<TextChangeListSnapshot>,
) : MarkdownRenderer {
  private val tags = mutableListOf<TagInsertion>()

  override fun MarkdownRenderScope.render(node: MarkdownNode): RenderResult {
    walkMarkdownNodes(node, changes) { node ->
      when (node) {
        is BoldNode -> {
          emitTag("b", node.range, listOf(node.openingMarkerRange, node.closingMarkerRange))
        }
        is StrikeThroughNode -> {
          emitTag("s", node.range, listOf(node.openingMarkerRange, node.closingMarkerRange))
        }
        is LinkNode -> emitTag(
          tag = "link",
          range = node.range,
          markers = listOf(
            node.textOpeningMarkerRange, node.textClosingMarkerRange,
            node.urlOpeningMarkerRange, node.urlClosingMarkerRange,
          ),
        )
        is BlockQuoteNode -> emitTag("blockquote", node.range, listOf(node.markerRange))
        is ListBlockNode -> emitTag("list", node.range)
        is TaskListItemNode -> emitTaskListItem(node)
        is HeadingNode -> emitHeading(node)
        else -> {}
      }
    }
    return RenderResult.Empty
  }

  fun toHtml(): String {
    val sortedTags = tags.sortedBy { it.offset }
    return buildString {
      var cursor = 0
      for ((offset, tag) in sortedTags) {
        append(source, cursor, offset)
        append(tag)
        cursor = offset
      }
      append(source, cursor, source.length)
    }
  }

  private fun MarkdownNodeWalkScope.emitTag(
    tag: String,
    range: LocalTextRange,
    markers: List<LocalTextRange> = emptyList()
  ) {
    val resolved = range.resolve() ?: return
    for (marker in markers) {
      marker.resolve(dropOnEdit = true) ?: return
    }
    tags += TagInsertion(resolved.start, "<$tag>")
    tags += TagInsertion(resolved.end, "</$tag>")
  }

  private fun MarkdownNodeWalkScope.emitTaskListItem(node: TaskListItemNode) {
    val markerRange = node.markerRange.resolve(dropOnEdit = true) ?: return
    tags += TagInsertion(markerRange.start, "<monospace>")
    tags += TagInsertion(markerRange.end, "</monospace>")
  }

  private fun MarkdownNodeWalkScope.emitHeading(node: HeadingNode) {
    val range = node.range.resolve() ?: return
    val openingMarkerRange = node.openingMarkerRange.resolve() ?: return
    if (source.isAtxHeadingMarker(openingMarkerRange)) {
      tags += TagInsertion(range.start, "<h${node.level}>")
      tags += TagInsertion(range.end, "</h${node.level}>")
    }
  }
}

private data class TagInsertion(
  val offset: Int, val tag: String
)

private object FakeMarkdownRenderScope : MarkdownRenderScope, Density by Density(1f, 1f) {
  override val theme: WysiwygTheme get() = error("unused")
  override val textMeasurer: TextMeasurer get() = error("unused")
  override val textStyle: TextStyle get() = error("unused")
  override fun isInsideViewport(range: TextRange): Boolean = true
}
