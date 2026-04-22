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
import me.saket.wysiwyg.internal.MarkdownRendererScope
import me.saket.wysiwyg.highlight.flexmark.FlexmarkMarkdownHighlighterExtension
import me.saket.wysiwyg.MarkdownSpan
import me.saket.wysiwyg.MarkdownSpanStyle
import me.saket.wysiwyg.MarkerColorSpanStyle

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

  override fun Node.addSpansInto(buffer: MutableList<MarkdownSpan>) {
    if (this is RedditSpoilersNode) {
      buffer.add(
        MarkdownSpan(
          style = MarkerColorSpanStyle,
          range = TextRange(openingMarker.startOffset, openingMarker.endOffset)
        )
      )
      buffer.add(
        MarkdownSpan(
          style = MarkerColorSpanStyle,
          range = TextRange(closingMarker.startOffset, closingMarker.endOffset)
        )
      )
      buffer.add(
        MarkdownSpan(
          style = SpoilersSpanStyle,
          range = TextRange(body.startOffset, body.endOffset)
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
          val spoilerBody = node.baseSequence.subSequence(openingMarker.endOffset, closingMarker.startOffset)

          if (spoilerBody.isNotEmpty) {
            val spoilers = RedditSpoilersNode(
              chars = node.baseSequence.subSequence(openingMarker.startOffset, closingMarker.endOffset),
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

class RedditSpoilersNode(
  chars: BasedSequence,
  val body: BasedSequence,
  val openingMarker: BasedSequence,
  val closingMarker: BasedSequence,
) : Node(chars) {
  override fun getSegments(): Array<BasedSequence> {
    return arrayOf(chars, openingMarker, closingMarker)
  }
}

object SpoilersSpanStyle : MarkdownSpanStyle() {
  override fun MarkdownRendererScope.render(text: AnnotatedString.Builder, range: TextRange) {
    text.addStyle(
      style = SpanStyle(
        color = theme.spoilersTextColor,
        background = theme.spoilersBackground,
      ),
      range = range,
    )
  }
}
