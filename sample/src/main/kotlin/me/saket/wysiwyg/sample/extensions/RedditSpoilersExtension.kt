package me.saket.wysiwyg.sample.extensions

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import com.vladsch.flexmark.ast.Text
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker
import com.vladsch.flexmark.util.sequence.BasedSequence
import me.saket.wysiwyg.highlight.ChangeListSnapshot
import me.saket.wysiwyg.highlight.MarkdownNode
import me.saket.wysiwyg.highlight.flexmark.FlexmarkMarkdownHighlighterExtension
import me.saket.wysiwyg.highlight.rebased
import me.saket.wysiwyg.highlight.touchesAny
import me.saket.wysiwyg.internal.MarkdownRendererScope

class RedditSpoilersExtension : FlexmarkMarkdownHighlighterExtension {
  override fun buildParser(builder: Parser.Builder) {
    builder.postProcessorFactory(
      object : NodePostProcessorFactory(/* ignored = */ false) {
        init {
          addNodes(Text::class.java)
        }

        override fun apply(document: Document): NodePostProcessor {
          return SpoilersProcessorFactory()
        }
      }
    )
  }

  override fun Node.addNodesInto(buffer: MutableList<MarkdownNode>) {
    if (this is RedditSpoilersNode) {
      buffer.add(
        SpoilersNode(
          range = TextRange(startOffset, endOffset),
          bodyRange = TextRange(body.startOffset, body.endOffset),
          openingMarkerRange = TextRange(openingMarker.startOffset, openingMarker.endOffset),
          closingMarkerRange = TextRange(closingMarker.startOffset, closingMarker.endOffset),
        )
      )
    }
  }
}

class SpoilersProcessorFactory : NodePostProcessor() {
  override fun process(state: NodeTracker, node: Node) {
    runCatchingOnRelease {
      node.chars.indexOfAll(">!").forEach { startIndex ->
        val endIndex = node.chars.indexOf("!<", /* fromIndex = */ startIndex)
        if (endIndex != -1) {
          val openingMarker = node.chars.subSequence(startIndex, startIndex + 2)
          val closingMarker = node.chars.subSequence(endIndex, endIndex + 2)
          val spoilerBody =
            node.baseSequence.subSequence(openingMarker.endOffset, closingMarker.startOffset)

          if (spoilerBody.isNotEmpty) {
            val spoilers = RedditSpoilersNode(
              chars = node.baseSequence.subSequence(
                openingMarker.startOffset,
                closingMarker.endOffset
              ),
              body = spoilerBody,
              openingMarker = openingMarker,
              closingMarker = closingMarker,
            )
            node.appendChild(spoilers)
            state.nodeAdded(spoilers)
          }
        }
      }
    }
  }
}

// todo: get rid of intermediate nodes. emit MarkdownNode directly.
private data class RedditSpoilersNode(
  private val chars: BasedSequence,
  val body: BasedSequence,
  val openingMarker: BasedSequence,
  val closingMarker: BasedSequence,
) : Node(chars) {
  override fun getSegments(): Array<BasedSequence> {
    return arrayOf(chars, openingMarker, closingMarker)
  }
}

data class SpoilersNode(
  override val range: TextRange,
  val bodyRange: TextRange,
  val openingMarkerRange: TextRange,
  val closingMarkerRange: TextRange,
) : MarkdownNode {

  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder) {
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = openingMarkerRange,
    )
    text.addStyle(
      style = SpanStyle(color = theme.markerColor),
      range = closingMarkerRange,
    )
    text.addStyle(
      style = SpanStyle(
        color = theme.spoilersTextColor,
        background = theme.spoilersBackground,
      ),
      range = bodyRange,
    )
  }

  override fun rebased(changes: ChangeListSnapshot): MarkdownNode? {
    if (changes.touchesAny(openingMarkerRange, closingMarkerRange)) return null

    return SpoilersNode(
      range = range.rebased(changes) ?: return null,
      bodyRange = bodyRange.rebased(changes) ?: return null,
      openingMarkerRange = openingMarkerRange.rebased(changes) ?: return null,
      closingMarkerRange = closingMarkerRange.rebased(changes) ?: return null,
    )
  }
}
