// Hand-written counterpart to the plugin-generated binding.c. The ktreesitter-plugin's
// Gradle DSL only supports one grammar per module, so this file mirrors its output for the
// inline grammar. Kept in sync with TreeSitterMarkdownInline.kt — the JNI export name must
// match the Kotlin package + class + method name exactly (underscores encoded as _1).

#include <jni.h>
#include <tree-sitter-markdown-inline.h>

#ifndef __ANDROID__
#define NATIVE_FUNCTION(name) JNIEXPORT jlong JNICALL name(JNIEnv * _env, jclass _class)
#else
#define NATIVE_FUNCTION(name) JNIEXPORT jlong JNICALL name()
#endif

NATIVE_FUNCTION(Java_me_saket_wysiwyg_parser_treesitter_grammar_TreeSitterMarkdownInline_tree_1sitter_1markdown_1inline) {
    return (jlong)tree_sitter_markdown_inline();
}
