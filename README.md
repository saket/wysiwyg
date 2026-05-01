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
Last measured: 2026-05-01.

Numbers below were taken on a Pixel 10 Pro (Android 16), Compose UI 1.11.0-rc01.
The scenario is: launch the editor, jump the cursor to end-of-doc, type 26 characters with no inter-char delay.
Two fixtures: the first 50,000 characters of the CommonMark spec (long document) and the ~400-char sample fixture (short note).

#### Long document — first 50,000 characters of the CommonMark spec

**Frame timing during the typing burst** (lower is better; 8.3 ms is the 120 Hz frame budget):

|                   | Plain `BasicTextField` | `WsyiwygTextField` | Ratio |
|-------------------|------------------------|--------------------|-------|
| P50 frame CPU     | 4.3 ms                 | 4.5 ms             | 1.0×  |
| P90 frame CPU     | 14.4 ms                | 56.8 ms            | 3.9×  |
| P95 frame CPU     | 26.3 ms                | 61.2 ms            | 2.3×  |
| P99 frame CPU     | 49.6 ms                | 66.5 ms            | 1.3×  |
| P99 frame overrun | 57.6 ms                | 112.9 ms           | 2.0×  |

**Per-iteration trace section sums** (wysiwyg only):

| Section                                | Median   | Calls / iteration |
|----------------------------------------|----------|-------------------|
| `Wysiwyg:parse`                        | 174.1 ms | 3                 |
| ↳ `Wysiwyg:flexmarkParse`              | 93.7 ms  | 3                 |
| ↳ `Wysiwyg:convertFlexmarkAst`         | 2.1 ms   | 3                 |
| `Wysiwyg:drawBehind`                   | 4.3 ms   | 8                 |
| ↳ `Wysiwyg:drawBehind:computeViewport` | 0.3 ms   | 8                 |
| ↳ `Wysiwyg:drawBehind:paintVisible`    | 3.7 ms   | 8                 |
| ↳↳ `Wysiwyg:drawBehind:paint`          | 3.3 ms   | 65                |
| `Wysiwyg:transformOutput`              | 48.5 ms  | 17                |
| ↳ `Wysiwyg:render`                     | 48.1 ms  | 17                |
| ↳↳ `Wysiwyg:render:addStyle`           | 18.6 ms  | 22,355            |
| `Wysiwyg:inputTransformation`          | 0.2 ms   | 5                 |
| `Wysiwyg:edited` (overlay)             | < 0.1 ms | 3                 |

`Wysiwyg:parse` runs on `Dispatchers.Default`, so it doesn't block frames directly — but the main-thread work it triggers (`transformOutput` → `render`) explains most of the gap above plain `BasicTextField`.

Two signals stand out under the granular traces. **Render is volume-bound:** every `OutputTransformation` query re-walks the full AST and re-emits every style, even when only one character changed. With Compose querying `OutputTransformation` 17× per typing burst, that's 22,355 `addStyle` calls into the `TextFieldBuffer` span list (~1,315 per walk). The trace events themselves add ~36 ms of overhead at this volume (1.6 µs × 22 K), so the underlying mutation cost is closer to ~12 ms — the volume, not per-call cost, is what to attack. **Draw is paint-bound:** of the 4.3 ms `drawBehind` median, 3.3 ms is actual painter draws across 65 visible spans; `computeViewport` is 0.3 ms and the painter-loop overhead is ~0.4 ms (`paintVisible` − `paint`).

#### Short note — ~400-char sample fixture

**Frame timing during the typing burst:**

| Metric            | `WsyiwygTextField` |
|-------------------|--------------------|
| P50 frame CPU     | 3.5 ms             |
| P90 frame CPU     | 10.2 ms            |
| P95 frame CPU     | 16.5 ms            |
| P99 frame CPU     | 33.2 ms            |
| P99 frame overrun | 32.0 ms            |

**Per-iteration trace section sums:**

| Section                                | Median  | Calls / iteration |
|----------------------------------------|---------|-------------------|
| `Wysiwyg:parse`                        | 32.2 ms | 3                 |
| ↳ `Wysiwyg:flexmarkParse`              | 13.0 ms | 3                 |
| ↳ `Wysiwyg:convertFlexmarkAst`         | 0.3 ms  | 3                 |
| `Wysiwyg:drawBehind`                   | 2.5 ms  | 6                 |
| ↳ `Wysiwyg:drawBehind:computeViewport` | 0.2 ms  | 6                 |
| ↳ `Wysiwyg:drawBehind:paintVisible`    | 1.9 ms  | 6                 |
| ↳↳ `Wysiwyg:drawBehind:paint`          | 1.8 ms  | 18                |
| `Wysiwyg:transformOutput`              | 4.5 ms  | 14                |
| ↳ `Wysiwyg:render`                     | 4.2 ms  | 14                |
| ↳↳ `Wysiwyg:render:addStyle`           | 0.9 ms  | 266               |
| `Wysiwyg:inputTransformation`          | 0.2 ms  | 3                 |
| `Wysiwyg:edited` (overlay)             | 0.1 ms  | 3                 |

Render is no longer dominant: `addStyle` drops 84× (22,355 → 266) and `transformOutput` shrinks ~10×, clearing the 8.3 ms budget at P50. The P95+ tail is driven by the off-thread `Wysiwyg:parse` triggering the next `transformOutput`, not render itself.

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
