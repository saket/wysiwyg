package me.saket.markdownrenderer.flexmark.stylers

import com.vladsch.flexmark.ast.ThematicBreak
import com.vladsch.flexmark.util.sequence.SubSequence
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.HorizontalRuleSpan.Mode.ASTERISKS
import me.saket.markdownrenderer.spans.HorizontalRuleSpan.Mode.HYPHENS
import me.saket.markdownrenderer.spans.HorizontalRuleSpan.Mode.UNDERSCORES
import me.saket.markdownrenderer.spans.pool.SpanPool

class ThematicBreakVisitor : NodeVisitor<ThematicBreak> {

  override fun visit(
    node: ThematicBreak,
    pool: SpanPool,
    writer: SpanWriter,
    theme: WysiwygTheme
  ) {
    writer.add(pool.foregroundColor(theme.syntaxColor), node.startOffset, node.endOffset)

    val thematicBreakSyntax = node.chars
    val clashesWithBoldSyntax = FOUR_ASTERISKS_HORIZONTAL_RULE == thematicBreakSyntax
    if (clashesWithBoldSyntax) {
      return
    }

    val ruleMode = when (thematicBreakSyntax[0]) {
      '*' -> ASTERISKS
      '-' -> HYPHENS
      '_' -> UNDERSCORES
      else -> throw UnsupportedOperationException(
          "Unknown thematic break mode: $thematicBreakSyntax"
      )
    }

    // Caching mutable BasedSequence isn't a good idea.
    val immutableThematicBreakChars = thematicBreakSyntax.toString()

    val hrSpan = pool.horizontalRule(
        text = immutableThematicBreakChars,
        ruleColor = theme.horizontalRuleColor,
        ruleStrokeWidth = theme.horizontalRuleStrokeWidth,
        mode = ruleMode
    )
    writer.add(hrSpan, node.startOffset, node.endOffset)
  }

  companion object {
    private val FOUR_ASTERISKS_HORIZONTAL_RULE = SubSequence.of("****")
  }
}