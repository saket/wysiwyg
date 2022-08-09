package me.saket.wysiwyg.sample

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

class RedditSuperscriptExtension : ParserExtension {
  override fun parserOptions(options: MutableDataHolder) = Unit

  override fun extend(parserBuilder: Parser.Builder) {
    parserBuilder.postProcessorFactory(
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
}

private class SuperscriptNodePostProcessor : NodePostProcessor() {
  override fun process(state: NodeTracker, node: Node) {
    runCatchingOnRelease {
      node.chars.indexOfAll("^").forEach { startIndex ->
        val isMultiWord = node.chars.getOrNull(startIndex - 1) == ' ' && node.chars.getOrNull(startIndex + 1) == '('
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

class RedditSuperscriptNode(
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

internal inline fun runCatchingOnRelease(block: () -> Unit) {
  try {
    block()
  } catch (e: Throwable) {
    if (BuildConfig.DEBUG) {
      throw e
    } else {
      e.printStackTrace()
    }
  }
}
