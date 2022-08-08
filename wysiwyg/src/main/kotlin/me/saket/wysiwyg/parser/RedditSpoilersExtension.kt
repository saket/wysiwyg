package me.saket.wysiwyg.parser

import com.vladsch.flexmark.ast.Text
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.Parser.ParserExtension
import com.vladsch.flexmark.parser.block.NodePostProcessor
import com.vladsch.flexmark.parser.block.NodePostProcessorFactory
import com.vladsch.flexmark.util.ast.Document
import com.vladsch.flexmark.util.ast.Node
import com.vladsch.flexmark.util.ast.NodeTracker
import com.vladsch.flexmark.util.data.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence

class RedditSpoilersExtension : ParserExtension {
  override fun parserOptions(options: MutableDataHolder?) = Unit

  override fun extend(parserBuilder: Parser.Builder) {
    parserBuilder.postProcessorFactory(
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
