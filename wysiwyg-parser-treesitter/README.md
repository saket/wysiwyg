# wysiwyg-parser-treesitter

Incremental markdown parser backed by [tree-sitter](https://tree-sitter.github.io) via
[kotlin-tree-sitter](https://github.com/tree-sitter/kotlin-tree-sitter).

## Performance checklist

Tree-sitter is fast in C, noticeably slower through JNI. These rules exist to keep the gap
closed. See [tree-sitter/kotlin-tree-sitter#34](https://github.com/tree-sitter/kotlin-tree-sitter/issues/34)
for the benchmark that motivated most of them.

### 1. Use `parse(text: String)`, not `parse(callback)`

The callback overload invokes the Kotlin lambda **once per byte** across the JNI boundary —
catastrophic on anything larger than a tweet. The `String` overload hands the whole source
across in one trip. Always use the String variant.

### 2. Minimize JNI crossings during tree walking

Every `node.type`, `node.startByte`, `node.endByte` from Kotlin is a JNI call. A tree with
thousands of nodes means thousands of boundary crossings — typically the dominant cost in
span emission.

**Mitigation:** use tree-sitter [queries](https://tree-sitter.github.io/tree-sitter/using-parsers/queries/1-syntax.html)
(S-expression patterns) to pre-filter nodes natively before handing them to Kotlin. Queries
are *exactly* what tree-sitter was designed for highlighting-wise.

```kotlin
val query = Query(language, """
  (atx_heading) @heading
  (fenced_code_block) @code
  (block_quote) @blockquote
""")
val captures = query.captures(tree.rootNode)  // one JNI call, native filtering
```

Then iterate `captures` in Kotlin rather than manually walking every node.

### 3. Cache `Language` and `Parser` instances

They're expensive to construct and JNI-resident. Hold them as class fields on the parser —
never per-call.

### 4. Initial parse is the danger zone

Incremental re-parses are cheap (work is bounded by edit size). The one bad path is the
first parse of a large document. Strategies:

- Initial parse runs off the main thread (already enforced by the `Flow` shape).
- Emit an empty `ParseResult` immediately so rendering doesn't block; replace with the real
  result when parse completes. The two-phase `Flow<ParseResult>` contract naturally handles
  this.

### 5. Batch UTF-8 ↔ UTF-16 conversions

Tree-sitter byte offsets are UTF-8; Kotlin `String` is UTF-16. Every span we emit needs
byte→char conversion. Naive per-span conversion is O(length) each time — quadratic in total
spans for the whole document.

**Do once per parse:** walk the source string computing a cumulative UTF-8 byte → UTF-16
char index table, reuse it for every span conversion. For ASCII-only input (the common
case) this is a no-op.

### 6. Measure on-device, not from upstream claims

The ktreesitter issue above shows the maintainer dismissing a 20× perf gap vs regex as
"JVM overhead." Don't trust library-side framing — benchmark on actual target hardware
(mid-range Android, ARM) with realistic content (markdown at a few hundred to few thousand
lines). Desktop JVM numbers don't translate.

### 7. Extensions regex text in Kotlin, not via grammar forks

For custom markers (Reddit spoilers, hashtags, mentions), scan already-parsed text nodes
with Kotlin regex rather than extending the tree-sitter grammar. Grammar forks require C
source, rebuilds, and tree-sitter CLI familiarity. Kotlin regex over an `inline` node's
text content is ~50 lines of contributor code and runs faster than you'd expect — the JSON
benchmark above showed regex beating tree-sitter 20× on the same input.

See `TreeSitterMarkdownParserExtension` for the hook shape.

## Build notes

See [`BUILDING.md`](../BUILDING.md) at the repo root for submodule setup and NDK/CMake
details. tl;dr: `./gradlew :wysiwyg-parser-treesitter:assembleDebug` should work on a fresh
clone — submodules auto-initialize.

### Grammar ABI pinning

The `tree-sitter-markdown` submodule is pinned to **v0.4.1**. Do not bump past v0.4.x without
verifying that kotlin-tree-sitter supports the newer language ABI.

Tree-sitter grammars bake a language-ABI version into their generated `parser.c`
(`#define LANGUAGE_VERSION N`). kotlin-tree-sitter's runtime rejects grammars whose ABI
falls outside a supported range — loading an incompatible one throws
`IllegalArgumentException: Incompatible language version N. Must be between X and Y.` at
`Language(...)` construction time.

Current state:

| component            | version            | grammar ABI   |
|----------------------|--------------------|---------------|
| kotlin-tree-sitter   | 0.24.1             | accepts 13–14 |
| tree-sitter-markdown | v0.4.1 (ABI 14) ✅  | —             |
| tree-sitter-markdown | v0.5.0+ (ABI 15) ❌ | —             |

When bumping either dependency:

1. Check the grammar's `parser.c` for `LANGUAGE_VERSION` after checkout.
2. Check kotlin-tree-sitter's supported ABI range (it's in
   `ktreesitter/src/commonMain/kotlin/io/github/treesitter/ktreesitter/Language.kt` or the
   runtime error message).
3. If they disagree, either find a compatible grammar tag or wait for a kotlin-tree-sitter
   release that supports the newer ABI.
