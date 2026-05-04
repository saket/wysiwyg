![Screenshot](/screenshots/wysiwyg_github.png)

A markdown editor that highlights syntax in real-time.

```
TODO: upload to maven
```

### Usage

```kotlin
val parser = FlexmarkMarkdownParser(MarkdownHintStyles(context), MarkdownSpanPool())
val markdownHints = MarkdownHints(editText, parser)
editText.addTextChangedListener(markdownHints.textWatcher())
```

**Supported syntax**

* Bold
* Italic
* Strikethrough
* Headings
* Quote
* Horizontal rule
* Lists
* Links

### How do I...

**Customize colors and dimensions?**

`MarkdownHintStyles` contains default values for colors and dimensions that I felt were sensible. You can override them to change the feel of your editor. It's a Kotlin data class so overriding default values with Java may not be pretty.

**Use a different markdown parser?**

TODO

**Extend support for more markdown syntax?**

TODO

**Use custom spans?**

TODO

### Performance
Last measured: long document on 2026-05-03; short note on 2026-05-04.

Numbers below were taken on a Pixel 10 Pro (Android 16), Compose UI 1.11.0-rc01.
The scenario is: launch the editor, jump the cursor to end-of-doc, type 26 characters with no inter-char delay.
Two fixtures: the first 50,000 characters of the CommonMark spec (long document) and the ~400-char sample fixture (short note).

#### Long document — first 50,000 characters of the CommonMark spec

**Frame timing during the typing burst** (lower is better; 8.3 ms is the 120 Hz frame budget):

|                   | Plain `BasicTextField` | `WsyiwygTextField` | Ratio |
|-------------------|------------------------|--------------------|-------|
| P50 frame CPU     | 4.1 ms                 | 4.3 ms             | 1.0×  |
| P90 frame CPU     | 13.5 ms                | 32.9 ms            | 2.4×  |
| P95 frame CPU     | 30.9 ms                | 40.8 ms            | 1.3×  |
| P99 frame CPU     | 53.0 ms                | 53.9 ms            | 1.0×  |
| P99 frame overrun | 61.1 ms                | 72.8 ms            | 1.2×  |

**Per-iteration trace section sums** (wysiwyg only):

| Section                             | Median   | Calls / iteration |
|-------------------------------------|----------|-------------------|
| `Wysiwyg:parse`                     | 116.3 ms | 3                 |
| ↳ `Wysiwyg:flexmarkParse`           | 92.4 ms  | 3                 |
| ↳ `Wysiwyg:convertFlexmarkAst`      | 2.2 ms   | 3                 |
| `Wysiwyg:drawBehind`                | 3.8 ms   | 8                 |
| `Wysiwyg:transformOutput`           | 14.9 ms  | 22                |
| ↳ `Wysiwyg:render`                  | 14.5 ms  | 22                |
| ↳↳ `Wysiwyg:render:addStyle`        | 2.9 ms   | 1,402             |
| `Wysiwyg:inputTransformation`       | 0.2 ms   | 4                 |
| `Wysiwyg:edited` (overlay)          | 0.06 ms  | 3                 |

`Wysiwyg:parse` runs on `Dispatchers.Default`, so it doesn't block frames directly — but the main-thread work it triggers (`transformOutput` → `render`) explains most of the gap above plain `BasicTextField`.

Two signals stand out under the granular traces. **Render scales with the visible viewport, not the document:** every `OutputTransformation` query still re-walks the AST, but the resolve()-side viewport filter drops offscreen ranges so emissions stay proportional to what's on screen. With Compose querying `OutputTransformation` 22× per typing burst, that's 1,402 `addStyle` calls into the `TextFieldBuffer` span list (~64 per walk). **Draw is paint-bound:** of the 3.8 ms `drawBehind` median, 3.1 ms is actual painter draws across 65 visible spans; the painter-loop overhead is ~0.2 ms (`paintVisible` − `paint`).

#### Short note — ~400-char sample fixture

**Frame timing during the typing burst:**

|                   | Plain `BasicTextField` | `WsyiwygTextField` | Ratio |
|-------------------|-----------------------:|-------------------:|------:|
| P50 frame CPU     |                 3.4 ms |             3.5 ms |  1.0× |
| P90 frame CPU     |                 5.2 ms |             5.6 ms |  1.1× |
| P95 frame CPU     |                 7.2 ms |             7.5 ms |  1.0× |
| P99 frame CPU     |                26.0 ms |            22.6 ms |  0.9× |
| P99 frame overrun |                35.5 ms |            28.2 ms |  0.8× |

**Per-iteration trace section sums** (wysiwyg only):

| Section                             |  Median | Calls / iteration |
|-------------------------------------|--------:|------------------:|
| `Wysiwyg:parse`                     | 76.0 ms |                22 |
| ↳ `Wysiwyg:flexmarkParse`           | 44.1 ms |                22 |
| ↳ `Wysiwyg:convertFlexmarkAst`      |  1.4 ms |                22 |
| `Wysiwyg:drawBehind`                |  4.9 ms |                21 |
| `Wysiwyg:transformOutput`           | 14.2 ms |               110 |
| ↳ `Wysiwyg:render`                  | 13.7 ms |               110 |
| ↳↳ `Wysiwyg:render:addStyle`        |  3.0 ms |             2,090 |
| `Wysiwyg:inputTransformation`       |  0.7 ms |                26 |
| `Wysiwyg:edited` (overlay)          |  0.4 ms |                22 |

P50 is effectively tied with plain `BasicTextField` (3.5 ms vs 3.4 ms), well under the 8.3 ms 120 Hz budget. The P90 gap is 0.4 ms (5.6 ms vs 5.2 ms), and P95 is 0.3 ms over the plain field (7.5 ms vs 7.2 ms). In this run, the WYSIWYG field had a better tail: P99 frame CPU was 22.6 ms vs 26.0 ms, and P99 frame overrun was 28.2 ms vs 35.5 ms. `transformOutput` uses a stable `OutputTransformation` and re-renders markdown from the latest parsed AST on every Compose query; the 14.2 ms trace value is the median per-iteration sum across 110 calls, not a single-call duration.

### License

```
Copyright 2019 Saket Narayan.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
