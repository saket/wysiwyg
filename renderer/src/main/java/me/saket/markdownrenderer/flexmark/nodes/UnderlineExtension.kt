package me.saket.markdownrenderer.flexmark.nodes

import com.vladsch.flexmark.Extension
import com.vladsch.flexmark.ast.DelimitedNode
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.html.CustomNodeRenderer
import com.vladsch.flexmark.html.HtmlRenderer
import com.vladsch.flexmark.html.renderer.NodeRenderer
import com.vladsch.flexmark.html.renderer.NodeRendererFactory
import com.vladsch.flexmark.html.renderer.NodeRenderingHandler
import com.vladsch.flexmark.internal.Delimiter
import com.vladsch.flexmark.parser.InlineParser
import com.vladsch.flexmark.parser.Parser
import com.vladsch.flexmark.parser.delimiter.DelimiterProcessor
import com.vladsch.flexmark.parser.delimiter.DelimiterRun
import com.vladsch.flexmark.util.options.DataHolder
import com.vladsch.flexmark.util.options.MutableDataHolder
import com.vladsch.flexmark.util.sequence.BasedSequence

internal class Underline(
    private var openingMarker: BasedSequence? = BasedSequence.NULL,
    private var text: BasedSequence? = BasedSequence.NULL,
    private var closingMarker: BasedSequence? = BasedSequence.NULL
) : Node(
    openingMarker?.baseSubSequence(
        openingMarker.startOffset,
        closingMarker?.endOffset ?: 0
    )
), DelimitedNode {

  override fun getSegments(): Array<BasedSequence?> {
    return arrayOf(openingMarker, text, closingMarker)
  }

  override fun getAstExtra(out: StringBuilder?) {
    delimitedSegmentSpan(out, openingMarker, text, closingMarker, "text")
  }

  override fun getOpeningMarker(): BasedSequence? {
    return openingMarker
  }

  override fun setOpeningMarker(openingMarker: BasedSequence?) {
    this.openingMarker = openingMarker
  }

  override fun getText(): BasedSequence? {
    return text
  }

  override fun setText(text: BasedSequence?) {
    this.text = text
  }

  override fun getClosingMarker(): BasedSequence? {
    return closingMarker
  }

  override fun setClosingMarker(closingMarker: BasedSequence?) {
    this.closingMarker = closingMarker
  }
}

internal class UnderlineDelimiterProcessor : DelimiterProcessor {

  override fun getOpeningCharacter(): Char {
    return '_'
  }

  override fun getClosingCharacter(): Char {
    return '_'
  }

  override fun getMinLength(): Int {
    return 1
  }

  override fun getDelimiterUse(opener: DelimiterRun, closer: DelimiterRun): Int {
    return if (opener.length() >= 1 && closer.length() >= 1) {
      1
    } else {
      0
    }
  }

  override fun canBeOpener(
      leftFlanking: Boolean,
      rightFlanking: Boolean,
      beforeIsPunctuation: Boolean,
      afterIsPunctuation: Boolean,
      beforeIsWhitespace: Boolean,
      afterIsWhiteSpace: Boolean
  ): Boolean {
    return leftFlanking
  }

  override fun canBeCloser(
      leftFlanking: Boolean,
      rightFlanking: Boolean,
      beforeIsPunctuation: Boolean,
      afterIsPunctuation: Boolean,
      beforeIsWhitespace: Boolean,
      afterIsWhiteSpace: Boolean
  ): Boolean {
    return rightFlanking
  }

  override fun unmatchedDelimiterNode(
      inlineParser: InlineParser?,
      delimiter: DelimiterRun?
  ): Node? {
    return null
  }

  override fun process(opener: Delimiter?, closer: Delimiter?, delimitersUsed: Int) {
    val underline = Underline(
        opener?.getTailChars(delimitersUsed),
        BasedSequence.NULL,
        closer?.getLeadChars(delimitersUsed)
    )
    opener?.moveNodesBetweenDelimitersTo(underline, closer)
  }
}

internal class UnderlineNodeRenderer : NodeRenderer {

  override fun getNodeRenderingHandlers(): MutableSet<NodeRenderingHandler<*>> {
    val set = HashSet<NodeRenderingHandler<*>>()
    set.add(NodeRenderingHandler(Underline::class.java, CustomNodeRenderer<Underline> { node, context, html ->
      if (context.htmlOptions.sourcePositionParagraphLines) {
        html.withAttr().tag("ins")
      } else {
        html.srcPos(node.text).withAttr().tag("ins")
      }
      context.renderChildren(node)
      html.tag("/ins")
    }))

    return set
  }

  class Factory : NodeRendererFactory {

    override fun create(options: DataHolder): NodeRenderer {
      return UnderlineNodeRenderer()
    }
  }
}

internal class UnderlineExtension : Parser.ParserExtension, HtmlRenderer.HtmlRendererExtension {
  override fun extend(parserBuilder: Parser.Builder?) {
    parserBuilder?.customDelimiterProcessor(UnderlineDelimiterProcessor())
  }

  override fun extend(rendererBuilder: HtmlRenderer.Builder?, rendererType: String?) {
    rendererBuilder?.nodeRendererFactory(UnderlineNodeRenderer.Factory())
  }

  override fun parserOptions(options: MutableDataHolder?) {
  }

  override fun rendererOptions(options: MutableDataHolder?) {

  }

  companion object {
    fun create(): Extension {
      return UnderlineExtension()
    }
  }

}
