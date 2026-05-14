package me.saket.wysiwyg.parser

import dev.drewhamilton.poko.Poko

interface MarkdownNode {
  val range: LocalTextRange
  val children: List<MarkdownChildNode> get() = emptyList()
}

@Poko
class MarkdownChildNode(
  val offsetInParent: Int,
  val node: MarkdownNode,
)
