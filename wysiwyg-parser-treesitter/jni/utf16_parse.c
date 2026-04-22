// Spike: parse UTF-16 directly, bypassing ktreesitter's hardcoded-UTF-8 Parser.parse(String).
//
// This file links its own copy of tree-sitter core (from the tree-sitter-core submodule) into
// libktreesitter-markdown.so. The TSParser* we receive was created by ktreesitter's libktreesitter.so,
// but since both libraries embed the same tree-sitter version (0.24.7 / 0.24.1), struct layouts and
// allocators (global ts_calloc/realloc/free → real malloc/free by default) are compatible — calling
// our linked copy of ts_parser_parse_string_encoding on the foreign TSParser* is well-defined.
//
// Spike only. The long-term path is upstreaming an encoding setter to kotlin-tree-sitter so we
// don't ship duplicate tree-sitter core and don't reflect private fields.

#include <jni.h>
#include <stdint.h>
#include <tree_sitter/api.h>

// GetStringChars returns jchar* — 16-bit unsigned in native byte order. Android is LE-only
// across arm64-v8a, armeabi-v7a, x86, x86_64. Tree-sitter 0.24 has just a single
// TSInputEncodingUTF16 variant (host endianness is assumed), which matches jchar layout
// directly on Android. (0.25+ split it into UTF16LE/UTF16BE — mirror that then.)
JNIEXPORT jlong JNICALL
Java_me_saket_wysiwyg_parser_treesitter_NativeParser_parseUtf16(
    JNIEnv *env,
    jclass _class,
    jlong parserPtr,
    jstring source
) {
    TSParser *parser = (TSParser *)parserPtr;

    const jchar *chars = (*env)->GetStringChars(env, source, NULL);
    jsize char_count = (*env)->GetStringLength(env, source);

    // ts_parser_parse_string_encoding takes length in bytes, not in code units.
    TSTree *tree = ts_parser_parse_string_encoding(
        parser,
        NULL,                          // no old tree → full parse
        (const char *)chars,
        (uint32_t)(char_count * 2),    // UTF-16 = 2 bytes per code unit
        TSInputEncodingUTF16
    );

    (*env)->ReleaseStringChars(env, source, chars);
    return (jlong)tree;
}
