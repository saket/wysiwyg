package me.saket.markdownrenderer.flexmark.stylers

import androidx.annotation.ColorInt
import androidx.annotation.Px
import com.vladsch.flexmark.ast.ThematicBreak
import com.vladsch.flexmark.util.sequence.SubSequence
import me.saket.markdownrenderer.SpanWriter
import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.NodeVisitor
import me.saket.markdownrenderer.spans.HorizontalRuleSpan
import me.saket.markdownrenderer.spans.HorizontalRuleSpan.Mode
import me.saket.markdownrenderer.spans.HorizontalRuleSpan.Mode.ASTERISKS
import me.saket.markdownrenderer.spans.HorizontalRuleSpan.Mode.HYPHENS
import me.saket.markdownrenderer.spans.HorizontalRuleSpan.Mode.UNDERSCORES
import me.saket.markdownrenderer.spans.pool.Recycler
import me.saket.markdownrenderer.spans.pool.SpanPool
import me.saket.markdownrenderer.spans.pool.foregroundColor

class ThematicBreakVisitor : NodeVisitor<ThematicBreak> {

  private val horizontalRuleSpansPool = ThematicSpanPool()

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

    val hrSpan = horizontalRuleSpansPool.get(
        ruleColor = theme.horizontalRuleColor,
        ruleThickness = theme.horizontalRuleStrokeWidth.toFloat(),
        syntax = immutableThematicBreakChars,
        mode = ruleMode
    )
    writer.add(hrSpan, node.startOffset, node.endOffset)
  }

  companion object {
    private val FOUR_ASTERISKS_HORIZONTAL_RULE = SubSequence.of("****")
  }
}

internal class ThematicSpanPool {
  private val pool = mutableMapOf<String, HorizontalRuleSpan>()

  private val recycler: Recycler = { span ->
    require(span is HorizontalRuleSpan)
    pool[recyclingKey(span)] = span
  }

  /**
   * @param syntax See [HorizontalRuleSpan.syntax].
   */
  internal fun get(
    @ColorInt ruleColor: Int,
    @Px ruleThickness: Float,
    syntax: CharSequence,
    mode: Mode
  ): HorizontalRuleSpan {
    val key = recyclingKey(syntax, ruleColor, ruleThickness, mode)
    return pool.remove(key) ?: HorizontalRuleSpan(ruleColor, ruleThickness, syntax, mode, recycler)
  }

  private fun recyclingKey(span: HorizontalRuleSpan) = recyclingKey(
      span.syntax,
      span.ruleColor,
      span.ruleThickness,
      span.mode
  )

  private fun recyclingKey(
    syntax: CharSequence,
    ruleColor: Int,
    ruleThickness: Float,
    mode: Mode
  ) = "${syntax}_${ruleColor}_${ruleThickness}_$mode"
}