package me.saket.markdownrenderer

import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.Px
import ru.noties.markwon.spans.SpannableTheme

data class MarkdownHintStyles(
    /**
     * TODO: I find passing the context around uncomfortable. Get rid of this by supplying the defaults directly.
     * Used for resolving default colors and dimensions in [SpannableTheme.builderWithDefaults].
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
