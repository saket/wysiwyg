package me.saket.wysiwyg

import me.saket.markdownrenderer.WysiwygTheme
import me.saket.markdownrenderer.flexmark.FlexmarkMarkdownParser

/**
 * Things needed:
 * 1. Update "supported spans"
 * 2. Update FlexmarkNodeTreeVisitor.
 * 3. Update parser extensions available.
 *
 * FYI: Add WYSIWYG as api.
 */
class ExtendedFlexmarkParser(
  theme: WysiwygTheme
) : FlexmarkMarkdownParser(theme) {

//  private val stylers = emptyList<FlexmarkSyntaxStyler<Any>>()
//
//  override fun buildParser(): Parser {
//    val superP = super.buildParser()
//
//    superP.addExtension()
//
//    val dataOptions = MutableDataSet().set(Parser.UNDERSCORE_DELIMITER_PROCESSOR, false)
//
//    return Parser.builder(dataOptions)
//        .extensions(supportedParserExtensions())
//        .build()
//  }

}

//class UnderlineStyler : FlexmarkSyntaxStyler<CustomSpan>(CustomSpan::class.java) {
//
//  override fun pool(): Pool<CustomSpan> {
//    return object : Pool<CustomSpan> {
//      override fun get(): CustomSpan {
//        TODO()
//      }
//
//      override fun recycle(span: CustomSpan) {
//        TODO()
//      }
//    }
//  }
//
//  override fun nodeVisitor(
//    pool: RealSpanPool
//  ): NodeVisitor? {
//    return object : NodeVisitor {
//      override fun visit(
//        node: Node,
//        proceedToChildren: () -> Unit
//      ) {
//
//      }
//    }
//  }
//}
//
//class UnderlineExtension : Parser.ParserExtension {
//  override fun extend(parserBuilder: Builder?) {
//    TODO()
//  }
//
//  override fun parserOptions(options: MutableDataHolder?) {
//    TODO()
//  }
//}