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
import ru.noties.markwon.spans.SpannableTheme
import timber.log.Timber

/**
 * To support:
 * - Superscript
 */
class MarkdownNodeTreeVisitor(
    private val spanPool: MarkdownSpanPool,
    private val styles: MarkdownHintStyles
) {

  @ColorInt
  private val syntaxColor: Int = styles.syntaxColor

  @ColorInt
  private val blockQuoteTextColor: Int = styles.blockQuoteTextColor

  @ColorInt
  private val linkUrlColor: Int = styles.linkUrlColor

  @ColorInt
  private val horizontalRuleColor: Int = styles.horizontalRuleColor

  @Px
  private val horizontalRuleStrokeWidth: Int = styles.horizontalRuleStrokeWidth

  @Px
  private val listBlockIndentationMargin: Int = styles.listBlockIndentationMargin

  private val spannableTheme = SpannableTheme.builderWithDefaults(styles.context)
      .headingBreakHeight(0)
      .blockQuoteColor(styles.blockQuoteIndentationRuleColor)
      .blockQuoteWidth(styles.blockQuoteVerticalRuleStrokeWidth)
      .blockMargin(styles.listBlockIndentationMargin)
      .codeBackgroundColor(styles.inlineCodeBackgroundColor)
      .build()

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
        is FencedCodeBlock -> {
          if (node.openingMarker.contains('~')) {
            // Ignore. Messes with strikethrough.
          } else {
            highlightFencedCodeBlock(node)
          }
        }
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
      writer!!.add(
          spanPool.foregroundColor(syntaxColor),
          delimitedNode.startOffset,
          delimitedNode.startOffset + delimitedNode.openingMarker.length
      )
    }

    if (delimitedNode.closingMarker.isNotEmpty()) {
      writer!!.add(
          spanPool.foregroundColor(syntaxColor),
          delimitedNode.endOffset - delimitedNode.closingMarker.length,
          delimitedNode.endOffset
      )
    }
  }

  private fun highlightItalics(emphasis: Emphasis) {
    writer!!.add(spanPool.italics(), emphasis.startOffset, emphasis.endOffset)
    highlightMarkdownSyntax(emphasis)
  }

  private fun highlightBold(strongEmphasis: StrongEmphasis) {
    writer!!.add(spanPool.bold(), strongEmphasis.startOffset, strongEmphasis.endOffset)
    highlightMarkdownSyntax(strongEmphasis)
  }

  private fun highlightInlineCode(code: Code) {
    writer!!.add(spanPool.inlineCode(spannableTheme), code.startOffset, code.endOffset)
    writer!!.add(spanPool.monospaceTypeface(), code.startOffset, code.endOffset)
    highlightMarkdownSyntax(code)
  }

  private fun highlightIndentedCodeBlock(indentedCodeBlock: IndentedCodeBlock) {
    // A LineBackgroundSpan needs to start at the starting of the line.
    val lineStartOffset = indentedCodeBlock.startOffset - 4

    writer!!.add(spanPool.indentedCodeBlock(spannableTheme), lineStartOffset, indentedCodeBlock.endOffset)
    writer!!.add(spanPool.monospaceTypeface(), indentedCodeBlock.startOffset, indentedCodeBlock.endOffset)
  }

  private fun highlightFencedCodeBlock(indentedCodeBlock: FencedCodeBlock) {
    writer!!.add(spanPool.indentedCodeBlock(spannableTheme), indentedCodeBlock.startOffset, indentedCodeBlock.endOffset)
    writer!!.add(spanPool.monospaceTypeface(), indentedCodeBlock.startOffset, indentedCodeBlock.endOffset)
  }

  private fun highlightStrikeThrough(strikethrough: Strikethrough) {
    writer!!.add(spanPool.strikethrough(), strikethrough.startOffset, strikethrough.endOffset)
    highlightMarkdownSyntax(strikethrough)
  }

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
    val quoteSpan = spanPool.quote(spannableTheme)
    writer!!.add(quoteSpan, blockQuote.startOffset - nestedParents, blockQuote.endOffset)

    // Quote markers ('>').
    val markerStartOffset = blockQuote.startOffset - nestedParents
    writer!!.add(spanPool.foregroundColor(syntaxColor), markerStartOffset, blockQuote.startOffset + 1)

    // Text color.
    writer!!.add(spanPool.foregroundColor(blockQuoteTextColor), blockQuote.startOffset - nestedParents, blockQuote.endOffset)
  }

  private fun highlightHeading(heading: Heading) {
    writer!!.add(spanPool.heading(heading.level, spannableTheme), heading.startOffset, heading.endOffset)
    writer!!.add(
        spanPool.foregroundColor(syntaxColor),
        heading.startOffset,
        heading.startOffset + heading.openingMarker.length
    )
  }

  private fun highlightListBlock(listBlock: ListBlock) {
    writer!!.add(spanPool.leadingMargin(listBlockIndentationMargin), listBlock.startOffset, listBlock.endOffset)
  }

  private fun highlightListItem(listItem: ListItem) {
    writer!!.add(spanPool.foregroundColor(syntaxColor), listItem.startOffset, listItem.startOffset + 1)
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
      writer!!.add(hrSpan, thematicBreak.startOffset, thematicBreak.endOffset)
    }

    writer!!.add(spanPool.foregroundColor(syntaxColor), thematicBreak.startOffset, thematicBreak.endOffset)
  }

  private fun highlightThematicBreakWithoutLeadingNewLine(node: Heading) {
    if (!node.isSetextHeading) {
      throw AssertionError()
    }

    val thematicBreakChars = node.closingMarker
    val ruleStartOffset = node.startOffset + node.text.length + 1

    if (thematicBreakChars.length < 3) {
      // Not sure why "-" gets treated as a heading.
      return

    } else if (thematicBreakChars[0] == '=') {
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
      writer!!.add(horizontalRuleSpan, ruleStartOffset, node.endOffset)
    }
    writer!!.add(spanPool.foregroundColor(syntaxColor), ruleStartOffset, node.endOffset)
  }

  private fun highlightLink(link: Link) {
    // Text.
    writer!!.add(spanPool.foregroundColor(styles.linkTextColor), link.startOffset, link.endOffset)

    // Url.
    val textClosingPosition = link.startOffset + link.text.length + 1
    val urlOpeningPosition = textClosingPosition + 1
    writer!!.add(spanPool.foregroundColor(linkUrlColor), urlOpeningPosition, link.endOffset)
  }

  companion object {
    private val FOUR_ASTERISKS_HORIZONTAL_RULE = SubSequence.of("****")
  }
}
