package me.saket.wysiwyg.sample.extensions

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.BaselineShift
import com.vladsch.flexmark.ast.Text
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker
import com.vladsch.flexmark.util.sequence.BasedSequence
import me.saket.wysiwyg.highlight.MarkdownNode
import me.saket.wysiwyg.highlight.flexmark.FlexmarkMarkdownHighlighterExtension
import me.saket.wysiwyg.internal.MarkdownNodeRenderScope

class RedditSuperscriptExtension : FlexmarkMarkdownHighlighterExtension {
  override fun buildParser(builder: Parser.Builder) {
    builder.postProcessorFactory(
      object : NodePostProcessorFactory(/* ignored = */ false) {
        init {
          addNodes(Text::class.java)
        }

        override fun apply(document: Document): NodePostProcessor {
          return SuperscriptNodePostProcessor()
        }
      }
    )
  }

  override fun Node.addNodesInto(buffer: MutableList<MarkdownNode>) {
    if (this is RedditSuperscriptNode) {
      buffer.add(
        SuperscriptNode(
          offsetInParent = openingMarker.startOffset,
          totalLength = endOffset - openingMarker.startOffset,
          bodyRange = TextRange(openingMarker.endOffset, endOffset),
          openingMarkerRange = TextRange(openingMarker.startOffset, openingMarker.endOffset),
          closingMarkerRange = closingMarker?.let {
            TextRange(it.startOffset, it.endOffset)
          },
          isMultiWord = closingMarker != null,
        )
      )
    }
  }
}

private class SuperscriptNodePostProcessor : NodePostProcessor() {
  override fun process(state: NodeTracker, node: Node) {
    runCatchingOnRelease {
      node.chars.indexOfAll("^").forEach { startIndex ->
        val isMultiWord =
          node.chars.getOrNull(startIndex - 1) == ' ' && node.chars.getOrNull(startIndex + 1) == '('
        val superscript = if (isMultiWord) {
          val endIndex = node.chars.indexOf(")", /* fromIndex = */ startIndex)
          if (endIndex != -1) {
            createSuperscriptNode(
              openingMarker = node.chars.subSequence(startIndex, startIndex + 2),
              closingMarker = node.chars.subSequence(endIndex, endIndex + 1)
            )
          } else null

        } else {
          val endIndex = node.chars.indexOf(" ", /* fromIndex = */ startIndex).let {
            if (it == -1) node.chars.lastIndex else it
          }
          createSuperscriptNode(
            openingMarker = node.chars.subSequence(startIndex, startIndex + 1),
            closingMarker = node.chars.subSequence(endIndex, endIndex)
          )
        }

        if (superscript != null) {
          node.appendChild(superscript)
          state.nodeAdded(superscript)
        }
      }
    }
  }

  private fun createSuperscriptNode(
    openingMarker: BasedSequence,
    closingMarker: BasedSequence
  ): RedditSuperscriptNode? {
    val baseSequence = openingMarker.baseSequence
    val isNotEmpty = closingMarker.startOffset > openingMarker.endOffset

    return if (isNotEmpty) {
      RedditSuperscriptNode(
        chars = baseSequence.subSequence(openingMarker.startOffset, closingMarker.endOffset),
        openingMarker = openingMarker,
        closingMarker = closingMarker
      )
    } else {
      null
    }
  }
}

private data class RedditSuperscriptNode(
  private val chars: BasedSequence,
  val openingMarker: BasedSequence,
  val closingMarker: BasedSequence?,
) : Node(chars) {
  override fun getSegments(): Array<BasedSequence> {
    return when (closingMarker) {
      null -> arrayOf(openingMarker)
      else -> arrayOf(openingMarker, closingMarker)
    }
  }
}

data class SuperscriptNode(
  override val offsetInParent: Int,
  override val totalLength: Int,
  val bodyRange: TextRange,
  val openingMarkerRange: TextRange,
  val closingMarkerRange: TextRange?,
  val isMultiWord: Boolean,
) : MarkdownNode {

  override fun MarkdownNodeRenderScope.render(text: AnnotatedString.Builder, startOffset: Int) {
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = openingMarkerRange,
    )
    if (closingMarkerRange != null) {
      text.addStyle(
        style = SpanStyle(color = theme.markerColor),
        range = closingMarkerRange,
      )
    }
    text.addStyle(
      style = SpanStyle(baselineShift = BaselineShift.Superscript),
      range = bodyRange,
    )
  }
}
