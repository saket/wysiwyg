package me.saket.markdownrenderer

import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.vladsch.flexmark.ast.BlockQuote
import com.vladsch.flexmark.ast.Code
import com.vladsch.flexmark.ast.DelimitedNode
import com.vladsch.flexmark.ast.Document
import com.vladsch.flexmark.ast.Emphasis
import com.vladsch.flexmark.ast.FencedCodeBlock
import com.vladsch.flexmark.ast.Heading
import com.vladsch.flexmark.ast.IndentedCodeBlock
import com.vladsch.flexmark.ast.Link
import com.vladsch.flexmark.ast.ListBlock
import com.vladsch.flexmark.ast.ListItem
import com.vladsch.flexmark.ast.Node
import com.vladsch.flexmark.ast.Paragraph
import com.vladsch.flexmark.ast.SoftLineBreak
import com.vladsch.flexmark.ast.StrongEmphasis
import com.vladsch.flexmark.ast.Text
import com.vladsch.flexmark.ast.ThematicBreak
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough
import com.vladsch.flexmark.util.sequence.SubSequence
import me.saket.markdownrenderer.spans.HorizontalRuleSpan
import timber.log.Timber

/**
 * To support:
 * - Superscript
 */
class MarkdownNodeTreeVisitor(private val spanPool: MarkdownSpanPool, private val options: MarkdownHintOptions) {

  @ColorInt
  private val syntaxColor: Int = options.syntaxColor

  @ColorInt
  private val blockQuoteTextColor: Int = options.blockQuoteTextColor

  @ColorInt
  private val linkUrlColor: Int = options.linkUrlColor

  @ColorInt
  private val horizontalRuleColor: Int = options.horizontalRuleColor

  @Px
  private val horizontalRuleStrokeWidth: Int = options.horizontalRuleStrokeWidth

  @Px
  private val listBlockIndentationMargin: Int = options.listBlockIndentationMargin

  private var writer: MarkdownHintsSpanWriter? = null

  fun visit(markdownRootNode: Node, hintsWriter: MarkdownHintsSpanWriter) {
    this.writer = hintsWriter
    visitChildren(markdownRootNode)
  }

  /**
   * Visit the child nodes.
   *
   * @param parent the parent node whose children should be visited
   */
  private fun visitChildren(parent: Node) {
    var node: Node? = parent.firstChild
    while (node != null) {
      // A subclass of this visitor might modify the node, resulting in getNext returning a different node or no
      // node after visiting it. So get the next node before visiting.
      val next = node.next

      when (node) {
        is Emphasis -> highlightItalics(node)
        is StrongEmphasis -> highlightBold(node)
        is Strikethrough -> highlightStrikeThrough(node)
        is Heading ->
          // Setext styles aren't supported. Setext-style headers are "underlined" using equal signs
          // (for first-level headers) and dashes (for second-level headers). For example:
          // This is an H1
          // =============
          //
          // This is an H2
          // -------------
          if (node.isAtxHeading) {
            highlightHeading(node)

          } else {
            // Reddit allows thematic breaks without a leading new line. So we'll manually support this.
            highlightThematicBreakWithoutLeadingNewLine(node)
          }
        is Link -> highlightLink(node)
        is Code -> highlightInlineCode(node)
        is IndentedCodeBlock -> highlightIndentedCodeBlock(node)
        is FencedCodeBlock -> highlightFencedCodeBlock(node)
        is BlockQuote -> highlightBlockQuote(node)
        is ListBlock -> highlightListBlock(node)
        is ListItem -> highlightListItem(node)
        // a.k.a. horizontal rule.
        is ThematicBreak -> highlightThematicBreak(node)
        is Document, is Text, is Paragraph, is SoftLineBreak -> {
          // Ignored.
        }
        else -> Timber.w("Unknown node: $node")
      }
      visitChildren(node)

      node = next
    }
  }

  private fun <T> highlightMarkdownSyntax(delimitedNode: T) where T : Node, T : DelimitedNode {
    if (delimitedNode.openingMarker.isNotEmpty()) {
      writer!!.pushSpan(
          spanPool.foregroundColor(syntaxColor),
          delimitedNode.startOffset,
          delimitedNode.startOffset + delimitedNode.openingMarker.length
      )
    }

    if (delimitedNode.closingMarker.isNotEmpty()) {
      writer!!.pushSpan(
          spanPool.foregroundColor(syntaxColor),
          delimitedNode.endOffset - delimitedNode.closingMarker.length,
          delimitedNode.endOffset
      )
    }
  }

  private fun highlightItalics(emphasis: Emphasis) {
    writer!!.pushSpan(spanPool.italics(), emphasis.startOffset, emphasis.endOffset)
    highlightMarkdownSyntax(emphasis)
  }

  private fun highlightBold(strongEmphasis: StrongEmphasis) {
    writer!!.pushSpan(spanPool.bold(), strongEmphasis.startOffset, strongEmphasis.endOffset)
    highlightMarkdownSyntax(strongEmphasis)
  }

  private fun highlightInlineCode(code: Code) {
    writer!!.pushSpan(spanPool.inlineCode(), code.startOffset, code.endOffset)
    writer!!.pushSpan(spanPool.monospaceTypeface(), code.startOffset, code.endOffset)
    highlightMarkdownSyntax(code)
  }

  private fun highlightIndentedCodeBlock(indentedCodeBlock: IndentedCodeBlock) {
    // A LineBackgroundSpan needs to start at the starting of the line.
    val lineStartOffset = indentedCodeBlock.startOffset - 4

    writer!!.pushSpan(spanPool.indentedCodeBlock(), lineStartOffset, indentedCodeBlock.endOffset)
    writer!!.pushSpan(spanPool.monospaceTypeface(), indentedCodeBlock.startOffset, indentedCodeBlock.endOffset)
  }

  private fun highlightFencedCodeBlock(indentedCodeBlock: FencedCodeBlock) {
    writer!!.pushSpan(spanPool.indentedCodeBlock(), indentedCodeBlock.startOffset, indentedCodeBlock.endOffset)
    writer!!.pushSpan(spanPool.monospaceTypeface(), indentedCodeBlock.startOffset, indentedCodeBlock.endOffset)
  }

  private fun highlightStrikeThrough(strikethrough: Strikethrough) {
    writer!!.pushSpan(spanPool.strikethrough(), strikethrough.startOffset, strikethrough.endOffset)
    highlightMarkdownSyntax(strikethrough)
  }

  //  public void visit(Superscript superscript) {
  //    //writer.pushSpan(spanPool.foregroundColor(syntaxColor), superscript.getStartOffset(), superscript.getEndOffset());
  //    //writer.pushSpan(new SuperscriptSpan(), superscript.getStartOffset(), superscript.getEndOffset());
  //
  //    BasedSequence superscriptText = superscript.getText();
  //    int relativeStartOffset = superscript.getStartOffset();
  //    int relativeEndOffset = superscript.getEndOffset() - 1;
  //
  //    Timber.i("-----------------------");
  //    Timber.i("Superscript: [%s..%s]", relativeStartOffset, relativeEndOffset);
  //
  //    for (int i = superscriptText.length() - 1, o = relativeEndOffset; i >= 0 && o >= relativeStartOffset; o--, i--) {
  //      char c = superscriptText.charAt(i);
  //      if (c == '^') {
  //        //Timber.i("Superscript: [%s..%s]", i + relativeStartOffset, relativeEndOffset);
  //        //Timber.i("Superscript: %s", superscriptText.substring(i, superscriptText.length()));
  //        Timber.i("[%s..%s]", o - 1, relativeEndOffset);
  //        writer.pushSpan(new SuperscriptSpan(), o - 1, relativeEndOffset);
  //        writer.pushSpan(spanPool.foregroundColor(syntaxColor), o - 1, relativeEndOffset);
  //      }
  //    }
  //
  //    writer.pushSpan(new SuperscriptSpan(), relativeStartOffset, relativeEndOffset);
  //    writer.pushSpan(spanPool.foregroundColor(syntaxColor), relativeStartOffset, relativeEndOffset);
  //
  //    //    applyHighlightForegroundSpan(superscript);
  //
  //  }

  private fun highlightBlockQuote(blockQuote: BlockQuote) {
    // Android seems to require quote spans to be inserted at the starting of the line.
    // Otherwise, nested quote spans aren't rendered correctly. Calculate the offset for
    // this quote's starting index instead and include all text from there under the spans.
    var nestedParents = 0
    var parent: Node = blockQuote.parent
    while (parent is BlockQuote) {
      ++nestedParents
      parent = parent.parent
    }

    // Quote's vertical rule.
    val quoteSpan = spanPool.quote()
    writer!!.pushSpan(quoteSpan, blockQuote.startOffset - nestedParents, blockQuote.endOffset)

    // Quote markers ('>').
    val markerStartOffset = blockQuote.startOffset - nestedParents
    writer!!.pushSpan(spanPool.foregroundColor(syntaxColor), markerStartOffset, blockQuote.startOffset + 1)

    // Text color.
    writer!!.pushSpan(spanPool.foregroundColor(blockQuoteTextColor), blockQuote.startOffset - nestedParents, blockQuote.endOffset)
  }

  private fun highlightHeading(heading: Heading) {
    writer!!.pushSpan(spanPool.heading(heading.level), heading.startOffset, heading.endOffset)
    writer!!.pushSpan(
        spanPool.foregroundColor(syntaxColor),
        heading.startOffset,
        heading.startOffset + heading.openingMarker.length
    )
  }

  private fun highlightListBlock(listBlock: ListBlock) {
    writer!!.pushSpan(spanPool.leadingMargin(listBlockIndentationMargin), listBlock.startOffset, listBlock.endOffset)
  }

  private fun highlightListItem(listItem: ListItem) {
    writer!!.pushSpan(spanPool.foregroundColor(syntaxColor), listItem.startOffset, listItem.startOffset + 1)
  }

  private fun highlightThematicBreak(thematicBreak: ThematicBreak) {
    val thematicBreakChars = thematicBreak.chars

    // '****' clashes with bold syntax, so avoid drawing a rule for it.
    val canDrawHorizontalRule = FOUR_ASTERISKS_HORIZONTAL_RULE != thematicBreakChars
    if (canDrawHorizontalRule) {
      val ruleMode: HorizontalRuleSpan.Mode
      val firstChar = thematicBreakChars[0]
      ruleMode = when (firstChar) {
        '*' -> HorizontalRuleSpan.Mode.ASTERISKS
        '-' -> HorizontalRuleSpan.Mode.HYPHENS
        '_' -> HorizontalRuleSpan.Mode.UNDERSCORES
        else -> throw UnsupportedOperationException("Unknown thematic break mode: $thematicBreakChars")
      }

      // Caching mutable BasedSequence isn't a good idea.
      val immutableThematicBreakChars = thematicBreakChars.toString()

      val hrSpan = spanPool.horizontalRule(immutableThematicBreakChars, horizontalRuleColor, horizontalRuleStrokeWidth, ruleMode)
      writer!!.pushSpan(hrSpan, thematicBreak.startOffset, thematicBreak.endOffset)
    }

    writer!!.pushSpan(spanPool.foregroundColor(syntaxColor), thematicBreak.startOffset, thematicBreak.endOffset)
  }

  private fun highlightThematicBreakWithoutLeadingNewLine(node: Heading) {
    if (!node.isSetextHeading) {
      throw AssertionError()
    }

    val ruleStartOffset = node.startOffset + node.text.length + 1
    val thematicBreakChars = node.chars.subSequence(ruleStartOffset, node.endOffset)

    if (thematicBreakChars[0] == '=') {
      // "===" line breaks aren't supported by Reddit.
      return
    }

    // '****' clashes with bold syntax, so avoid drawing a rule for it.
    val canDrawHorizontalRule = FOUR_ASTERISKS_HORIZONTAL_RULE != thematicBreakChars
    if (canDrawHorizontalRule) {
      val horizontalRuleSpan = spanPool.horizontalRule(
          thematicBreakChars,
          horizontalRuleColor,
          horizontalRuleStrokeWidth,
          HorizontalRuleSpan.Mode.HYPHENS
      )
      writer!!.pushSpan(horizontalRuleSpan, ruleStartOffset, node.endOffset)
    }
    writer!!.pushSpan(spanPool.foregroundColor(syntaxColor), ruleStartOffset, node.endOffset)
  }

  private fun highlightLink(link: Link) {
    // Text.
    writer!!.pushSpan(spanPool.foregroundColor(options.linkTextColor), link.startOffset, link.endOffset)

    // Url.
    val textClosingPosition = link.startOffset + link.text.length + 1
    val urlOpeningPosition = textClosingPosition + 1
    writer!!.pushSpan(spanPool.foregroundColor(linkUrlColor), urlOpeningPosition, link.endOffset)
  }

  companion object {
    private val FOUR_ASTERISKS_HORIZONTAL_RULE = SubSequence.of("****")
  }
}
