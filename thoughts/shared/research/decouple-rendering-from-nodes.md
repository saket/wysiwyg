# Decouple Rendering from Markdown Nodes

## Summary

Move rendering off the `MarkdownNode` interface. Replace today's `fun MarkdownNodeRenderScope.render(buffer: MarkdownStyleBuffer)` (defined per node) with a separate `MarkdownRenderer` interface that consumers implement. Nodes become pure data. The textfield's renderer (`AnnotatedStringRenderer`) is one impl; the test HTML helper becomes another (`HtmlRenderer`).

Inspired by swift-markdown's split between data nodes (`Markup`) and behavior (`MarkupVisitor` / `MarkupWalker`), but adapted for Kotlin and the constraint that wysiwyg supports user-defined node types.

## Goals

- Decouple the AST from rendering. A node is data. Rendering policy is external.
- Treat every node uniformly. No "built-in vs custom" interface split. The library's `HeadingNode` and a downstream `EmojiNode` are rendered through the same dispatch path.
- Preserve perf characteristics. No per-child scope allocation; subtree-level viewport culling becomes possible.
- Keep extensibility. Consumers write their own `MarkdownRenderer` (or wrap the default) to handle custom node types.

## Final Design

### `MarkdownNode` (modified, public)

```kotlin
interface MarkdownNode {
  val range: LocalTextRange
  val children: List<MarkdownChildNode>
    get() = emptyList()
}
```

No `render` method. No `accept`. Pure data. Stays an open interface (not sealed) so consumers can add their own node types.

### `MarkdownRenderer` (new, public)

```kotlin
interface MarkdownRenderer {
  fun render(node: MarkdownNode, scope: MarkdownRenderScope)
}
```

Single method. Implementations choose their own dispatch (`when (node)` over the types they care about). Implementations are responsible for:

- Detecting `is MarkdownDocument` at the top to reset `scope.offsetInRoot` to 0 and pull `scope.changes` from `node.changes`.
- Calling `descendInto(node, scope)` after their per-node work to recurse into children.

A free helper handles descent without duplicating the offset push/pop loop:

```kotlin
fun MarkdownRenderer.descendInto(node: MarkdownNode, scope: MarkdownRenderScope) {
  for (child in node.children) {
    scope.pushOffset(child.offsetInParent)
    render(child.node, scope)
    scope.popOffset(child.offsetInParent)
  }
}
```

### `MarkdownRenderScope` (moved to public, restructured)

```kotlin
class MarkdownRenderScope {
  var viewport: TextFieldViewport? = null
  var offsetInRoot: Int = 0
    internal set
  var changes: List<TextChangeListSnapshot> = emptyList()
    internal set

  fun resolve(range: LocalTextRange, dropOnEdit: Boolean): TextRange? { ... }

  internal fun pushOffset(delta: Int) { offsetInRoot += delta }
  internal fun popOffset(delta: Int) { offsetInRoot -= delta }
}
```

Mutable internal state, public read-only accessors plus `resolve()`. No `childScope()` value-copy; `pushOffset` / `popOffset` mutate in place. No per-child allocation on the hot path.

The scope does not manage state lifecycle (no `reset()`, no root detection). Renderer implementations handle that.

### `AnnotatedStringRenderer` (new, internal)

```kotlin
internal class AnnotatedStringRenderer(
  var theme: WysiwygTheme,
) : MarkdownRenderer {
  var buffer: MarkdownStyleBuffer = MarkdownStyleBuffer.Empty

  override fun render(node: MarkdownNode, scope: MarkdownRenderScope) {
    if (node is MarkdownDocument) {
      scope.offsetInRoot = 0
      scope.changes = node.changes
    }
    when (node) {
      is HeadingNode -> { /* span/painter logic from HeadingNode.render */ }
      is BoldNode -> { /* ... */ }
      // ... 12 built-ins total
      else -> {} // unknown node types render nothing in the default impl
    }
    descendInto(node, scope)
  }
}
```

The 12 `when` arms are the bodies of today's per-node `render()` methods, moved verbatim. Marker-painting logic shared by `BoldNode`, `ItalicNode`, `InlineCodeNode`, `FencedCodeBlockNode` (today via `DelimitedMarkdownNode`'s default `render`) becomes a private helper inside the renderer.

### `HtmlRenderer` (new, test-only)

```kotlin
internal class HtmlRenderer : MarkdownRenderer {
  val result = StringBuilder()

  override fun render(node: MarkdownNode, scope: MarkdownRenderScope) {
    when (node) {
      is HeadingNode -> {
        result.append("<h${node.level}>")
        descendInto(node, scope)
        result.append("</h${node.level}>")
      }
      // ... etc
      else -> descendInto(node, scope)
    }
  }
}
```

Replaces the `markdownToHtml.kt` test helper that today fakes a `MarkdownStyleBuffer` to intercept `addTestTag` calls.

## Files

### New (public)

- `parser/MarkdownRenderer.kt` — interface + `descendInto` helper.
- `parser/MarkdownRenderScope.kt` — moved from `internal/MarkdownRenderer.kt`. Becomes a public class with mutable internal state.

### New (internal)

- `internal/AnnotatedStringRenderer.kt` — concrete renderer. Holds `var buffer` and `var theme`. Body is the `when (node)` over 12 built-ins with logic moved from each node's `render()`.

### Modified

- `parser/MarkdownNode.kt` — drop `render` method. Just `range` and `children`.
- `parser/markdownNodes.kt` — drop `render` from every node class. Drop `DelimitedMarkdownNode`'s default `render` / abstract `renderText`. Nodes become pure `@Poko` data.
- `internal/RealWysiwyg.kt` — `RealMarkdownOutputTransformation` holds an `AnnotatedStringRenderer` and a `MarkdownRenderScope`. Per pass: assign `renderer.buffer`, call `renderer.render(document, scope)`.

### Deleted

- `internal/MarkdownRenderer.kt` — replaced by the public interface plus `AnnotatedStringRenderer`.

### Tests

- `test/.../parser/markdownToHtml.kt` — rewrite as `HtmlRenderer`. `MarkdownDocument.renderHtml()` becomes a few lines wiring up an `HtmlRenderer` and a `MarkdownRenderScope`.
- `IncrementalMarkdownParserTest.kt` — no changes (uses `renderHtml()` only).
- `WysiwygTest.kt` — no changes (Paparazzi snapshots through the composable).

## Phases

1. **New types alone.** Add `MarkdownRenderer` interface, `descendInto` helper, public `MarkdownRenderScope`. No callers yet. Build green.
2. **Move rendering off nodes (single non-bisectable commit).** Create `AnnotatedStringRenderer`, drop `render` from `MarkdownNode` and each node class, switch `RealWysiwyg` to use it, delete old `internal/MarkdownRenderer.kt`. Build is broken in the middle; this phase lands as one commit.
3. **Test helper rewrite.** `markdownToHtml.kt` becomes `HtmlRenderer`. `IncrementalMarkdownParserTest` should pass unchanged.
4. **Benchmarks.** Run `TypingBenchmark` against trunk vs. branch. Confirm `Wysiwyg:render` time is flat-or-better and frame timing on long-doc scrolling improves. Trace section names stay the same.

## Perf Notes

- **Per-child scope allocation eliminated.** Today `RealMarkdownNodeRenderScope.childScope(child)` returns a new data-class copy per child. New design mutates `scope.offsetInRoot` in place via `pushOffset` / `popOffset`. Zero allocations per child during traversal.
- **Renderer + scope are reused across passes.** Both held by `RealMarkdownOutputTransformation`. Per pass, only `buffer` and `viewport` are reassigned; offset is auto-zeroed when the renderer encounters `MarkdownDocument` at the top.
- **Subtree-level viewport culling.** `descendInto` can skip an entire subtree whose root range is offscreen, saving work on long documents. Today's code culls per-leaf inside `addStyle`.
- **Snapshot pattern preserved.** `HeadingNode`'s render reads `buffer.unstyledText` to validate the heading marker (commit 1a46f7c). That logic moves into `AnnotatedStringRenderer.renderHeading` with the same snapshot guarantee from `TextFieldMarkdownStyleBuffer`.

## Out of Scope

- **No `MarkdownVisitor<R>` generic interface.** Considered briefly. We have only one current use case beyond the textfield renderer (HTML test export), and it accumulates into a `StringBuilder` field — `Unit` return is sufficient. Add later if a query use case (link extraction, plain-text export) lands.
- **No `MarkupRewriter` equivalent.** Tree mutation is not a current use case; the parser is the source of truth and rebuilds on every parse.
- **No `BlockNode` / `InlineNode` marker interfaces.** Flexmark enforces structural validity at parse time and we don't expose tree-construction APIs to consumers, so this would be cosmetic.
- **No `RawMarkup`-style CoW backing store.** swift-markdown's two-layer design supports in-place mutation with structural sharing. We don't mutate trees, so plain `@Poko` data classes are simpler and faster.
- **No parent pointers on nodes.** The renderer maintains `offsetInRoot` itself. Saves a field per node.

## Extensibility

A consumer with a custom `EmojiNode` writes their own `MarkdownRenderer`:

```kotlin
class MyRenderer(private val delegate: AnnotatedStringRenderer) : MarkdownRenderer {
  override fun render(node: MarkdownNode, scope: MarkdownRenderScope) {
    when (node) {
      is EmojiNode -> {
        val resolvedRange = scope.resolve(node.range, dropOnEdit = true) ?: return
        delegate.buffer.addStyle(SpanStyle(...), resolvedRange)
        descendInto(node, scope)
      }
      else -> delegate.render(node, scope)
    }
  }
}
```

Or the consumer can subclass `AnnotatedStringRenderer` if we expose it (TBD when extensibility lands as a real requirement).
