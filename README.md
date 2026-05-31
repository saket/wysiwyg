# Wysiwyg

<img src="screenshots/sample.png" width="400" />

An **experimental** markdown editor for Compose UI that styles markdown syntax in real time. Edits are applied incrementally on every keystroke for instant feedback, while the full re-parse runs off the main thread. Uses [Flexmark](https://github.com/vsch/flexmark-java/) by default, but can be replaced with any other parser.

### Usage

```groovy
TODO: upload to maven
```

```kotlin
WsyiwygTextField(
  wysiwyg = rememberWysiwyg(
    textState = rememberTextFieldState(),
  ),
  theme = WysiwygTheme.Default,
  contentPadding = PaddingValues(16.dp),
)
```

TODO: document
- Marker inserters
- Customization?

### License

```
Copyright 2026 Saket Narayan.

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
