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
import kotlin.time.ExperimentalTime

class RedditSuperscriptExtension : ParserExtension {
  override fun parserOptions(options: MutableDataHolder) = Unit

  override fun extend(parserBuilder: Parser.Builder) {
    parserBuilder.postProcessorFactory(
      object : NodePostProcessorFactory(false) {
        init {
          addNodes(Text::class.java)
        }

        override fun apply(document: Document): NodePostProcessor {
          return SuperscriptNodePostProcessor()
        }
      }
    )
  }
}

private class SuperscriptNodePostProcessor() : NodePostProcessor() {
  @OptIn(ExperimentalTime::class)
  override fun process(state: NodeTracker, node: Node) {
    println("node = $node")
    check(node is Text)

    // Rules:
    // - Search for "^"
    //   - if it's followed by "(" then end it when ")^" is detected.
    //   - otherwise, end when a space is detected
    //   - if another "^" is detected then increase nesting.

    node.chars.indexOfAll("^").forEach { index ->
      val markerStartsAt = index

      if (node.chars.getOrNull(markerStartsAt - 1) == ' ' && node.chars.getOrNull(markerStartsAt + 1) == '(') {
        val endIndex = node.chars.indexOf(")", /* fromIndex = */ markerStartsAt)
        if (endIndex != -1) {
          val endOffset = node.startOffset + endIndex
          val openingMarker = node.chars.subSequence(markerStartsAt, markerStartsAt + 2)
          val closingMarker = node.chars.subSequence(endOffset, endOffset + 1)
          val superscriptChars = node.baseSequence.subSequence(openingMarker.startOffset, closingMarker.endOffset)
          val superscriptText = node.baseSequence.subSequence(openingMarker.endOffset, closingMarker.startOffset)

          if (superscriptText.isNotEmpty) {
            val superscript = RedditSuperscript(
              chars = superscriptChars,
              openingMarker = openingMarker,
              closingMarker = closingMarker
            )
            node.appendChild(superscript)
            state.nodeAdded(superscript)
          }
        }

      } else {
        val endIndex = node.chars.indexOf(" ", /* fromIndex = */ markerStartsAt).let {
          if (it == -1) node.chars.lastIndex else it
        }

        val endOffset = node.startOffset + endIndex
        val openingMarker = node.chars.subSequence(markerStartsAt, markerStartsAt + 1)
        val superscriptChars = node.baseSequence.subSequence(openingMarker.startOffset, endOffset + 1)
        val superscriptText = node.baseSequence.subSequence(openingMarker.endOffset, endOffset + 1)

        if (superscriptText.isNotEmpty) {
          val superscript = RedditSuperscript(
            chars = superscriptChars,
            openingMarker = openingMarker,
            closingMarker = null,
          )
          node.appendChild(superscript)
          state.nodeAdded(superscript)
        }
      }
    }
  }
}

class RedditSuperscript(
  chars: BasedSequence,
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
