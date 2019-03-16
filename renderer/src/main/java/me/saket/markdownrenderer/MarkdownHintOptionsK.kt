package me.saket.markdownrenderer

import androidx.annotation.ColorInt
import androidx.annotation.Px

data class MarkdownHintOptionsK(

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
    val spoilerSyntaxHintColor: Int,

    @ColorInt
    val spoilerHiddenContentOverlayColor: Int,

    @ColorInt
    val horizontalRuleColor: Int,

    @Px
    val horizontalRuleStrokeWidth: Int,

    @ColorInt
    val inlineCodeBackgroundColor: Int,

    @ColorInt
    val tableBorderColor: Int
)
