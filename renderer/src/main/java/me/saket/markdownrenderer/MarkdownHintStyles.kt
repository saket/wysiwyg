package me.saket.markdownrenderer

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.Px
import ru.noties.markwon.core.MarkwonTheme

/**
 * Colors and dimensions to use for highlighting markdown syntax.
 */
data class MarkdownHintStyles(

    /**
     * Used for resolving default colors and dimensions in [MarkwonTheme.builderWithDefaults].
     * TODO: Passing the context around isn't good. Get rid of this by supplying the defaults to Markwon library directly.
     */
    val context: Context,

    @ColorInt
    val syntaxColor: Int,

    @ColorInt
    val blockQuoteIndentationRuleColor: Int,

    @ColorInt
    val blockQuoteTextColor: Int,

    /** Gap before a block of ordered/unordered list. */
    @Px
    val listBlockIndentationMargin: Int,

    /** Width of a block-quote's vertical line/stripe/rule. */
    @Px
    val blockQuoteVerticalRuleStrokeWidth: Int,

    @ColorInt
    val linkUrlColor: Int,

    @ColorInt
    val linkTextColor: Int,

    @ColorInt
    val horizontalRuleColor: Int,

    @Px
    val horizontalRuleStrokeWidth: Int,

    @ColorInt
    val inlineCodeBackgroundColor: Int
)
