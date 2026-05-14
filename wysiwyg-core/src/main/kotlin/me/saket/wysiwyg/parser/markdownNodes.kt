package me.saket.wysiwyg.parser

import dev.drewhamilton.poko.Poko

@Poko
class MarkdownDocument(
  override val range: LocalTextRange,
  override val children: List<MarkdownChildNode>,
  // todo: this is renderer-overlay plumbing, not part of the syntax tree. move it somewhere else.
  val changes: List<TextChangeListSnapshot> = emptyList(),
) : MarkdownNode {

  fun copy(changes: List<TextChangeListSnapshot>): MarkdownDocument {
    return MarkdownDocument(
      range = this.range,
      children = this.children,
      changes = changes,
    )
  }

  companion object {
    val Empty = MarkdownDocument(
      range = LocalTextRange.span(0, 0),
      children = emptyList(),
    )
  }
}

interface DelimitedMarkdownNode : MarkdownNode {
  val openingMarkerRange: LocalTextRange
  val closingMarkerRange: LocalTextRange
}

@Poko
class BoldNode(
  override val range: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode

@Poko
class ItalicNode(
  override val range: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode

@Poko
class InlineCodeNode(
  override val range: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode

@Poko
class FencedCodeBlockNode(
  override val range: LocalTextRange,
  override val openingMarkerRange: LocalTextRange,
  override val closingMarkerRange: LocalTextRange,
) : DelimitedMarkdownNode

@Poko
class StrikeThroughNode(
  override val range: LocalTextRange,
  val openingMarkerRange: LocalTextRange,
  val closingMarkerRange: LocalTextRange,
) : MarkdownNode

@Poko
class LinkNode(
  override val range: LocalTextRange,
  val textRange: LocalTextRange,
  val textOpeningMarkerRange: LocalTextRange,
  val textClosingMarkerRange: LocalTextRange,
  val urlRange: LocalTextRange,
  val urlOpeningMarkerRange: LocalTextRange,
  val urlClosingMarkerRange: LocalTextRange,
) : MarkdownNode

@Poko
class BlockQuoteNode(
  override val range: LocalTextRange,
  val markerRange: LocalTextRange,
) : MarkdownNode

@Poko
class ListBlockNode(
  override val range: LocalTextRange,
  override val children: List<MarkdownChildNode>,
) : MarkdownNode

@Poko
class ListItemNode(
  override val range: LocalTextRange,
  override val children: List<MarkdownChildNode>,
  val markerRange: LocalTextRange,
) : MarkdownNode

@Poko
class TaskListItemNode(
  override val range: LocalTextRange,
  override val children: List<MarkdownChildNode>,
  val markerRange: LocalTextRange,
  val checkboxRange: LocalTextRange,
  val contentRange: LocalTextRange,
  val isChecked: Boolean,
) : MarkdownNode

@Poko
class HeadingNode(
  override val range: LocalTextRange,
  val openingMarkerRange: LocalTextRange,
  val level: Int,
) : MarkdownNode

@Poko
class ThematicBreakNode(
  override val range: LocalTextRange,
) : MarkdownNode
