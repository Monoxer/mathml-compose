# mathml-compose

A Kotlin Multiplatform / Compose Multiplatform library for rendering **MathML** and the small
**MxHTML** rich-text tag set (`<mxhtml>`, `<ruby>`, `<u>`, `<b>`, `<p>`, `<br>`, ...) used by
Monoxer's question/answer content, with no dependency on a WebView or MathJax. Everything is
parsed and drawn natively on Compose's `Canvas`, so it works identically on every target Compose
Multiplatform supports.

Supported targets: **Android**, **iOS** (`iosArm64`, `iosSimulatorArm64`), **JVM (Desktop)**, and
**Wasm (Browser)**.

## What's in this library

The library covers two distinct scenarios, each with its own public entry point:

| Scenario | Entry point | State |
| --- | --- | --- |
| Read-only display of question/answer text (may contain plain text and/or `<math>`) | [`MxhtmlView`](src/commonMain/kotlin/com/monoxer/mathmlcompose/render/compose/MxhtmlView.kt) | stateless |
| Editable formula input (e.g. handwritten-style math answers) | [`FormulaInputState`](src/commonMain/kotlin/com/monoxer/mathmlcompose/input/formula/FormulaInputState.kt) + [`MathCanvas`](src/commonMain/kotlin/com/monoxer/mathmlcompose/render/compose/MathCanvas.kt) | controlled component |

Internally both are backed by the same MathML parser (`MathMLParser`) and element model
(`MathElement`), so read-only rendering and editable input always agree on how a given formula
looks.

## Installation

This library is not yet published to Maven Central. Until then, consume it from source via a
Gradle [composite build](https://docs.gradle.org/current/userguide/composite_builds.html):

```kotlin
// settings.gradle.kts of the consuming project
includeBuild("../mathml-compose")
```

```kotlin
// build.gradle.kts of the consuming module
dependencies {
    implementation("com.monoxer.mathmlcompose:mathml-compose:0.1.0")
}
```

## Usage

### Read-only display

`MxhtmlView` accepts plain text, a bare `<math>...</math>` string, or mixed text with embedded
`<math>` (optionally wrapped in `<mxhtml>...</mxhtml>`), and renders whichever is appropriate:

```kotlin
MxhtmlView(
    text = "<mxhtml>解の公式は <math><mi>x</mi><mo>=</mo>" +
        "<mfrac><mrow><mo>-</mo><mi>b</mi></mrow><mrow><mn>2</mn><mi>a</mi></mrow></mfrac>" +
        "</math> です。</mxhtml>",
    colors = MxhtmlColors(
        accentColor = MaterialTheme.colorScheme.primary,
        focusedBackground = Color.Transparent,
        focusedBorder = Color.Transparent,
    ),
    fontSize = 18.sp,
)
```

`colors` is required on every call because the library carries no dependency on the host app's
design system — the host always supplies its own accent/highlight colors.

If you already have a parsed `MathElement` tree (or only ever render pure MathML), you can call
`MathCanvas` directly instead of `MxhtmlView`:

```kotlin
val element = remember(mathml) { MathMLParser.parse(mathml) }
MathCanvas(element = element, colors = colors)
```

### Editable formula input

Editable input follows a controlled-component pattern: the caller owns `FormulaInputState`
between recompositions, and the library only renders it and reports taps back as a path into the
tree — it never touches Compose layout coordinates or your view models directly.

```kotlin
var state by remember { mutableStateOf(FormulaInputState()) }

MathCanvas(
    element = state.root,
    colors = colors,
    editable = true,
    onTap = { hit -> state = state.moveCursorTo(hit.path, hit.onRight) },
)

// Toolbar actions:
Button(onClick = { state = state.insertLeaf(MathMn("1")) }) { Text("1") }
Button(onClick = { state = state.insertFrac() }) { Text("a/b") }
Button(onClick = { state = state.insertSup() }) { Text("x²") }
Button(onClick = { state = state.backspace() }) { Text("⌫") }

// Reading the result once the user is done:
val resultMathML = state.toResultElement().toMathML()
```

Available mutators on `FormulaInputState`: `insertLeaf`, `insertFrac`, `insertSqrt`, `insertSup`,
`insertSub`, `backspace`, `moveLeft`, `moveRight`, `moveCursorTo`, and `toResultElement`.

### Hint-based fill-in-the-blank input

For formulas with multiple pre-defined blanks filled in sequence (rather than free-form cursor
movement), use `FormulaHintInputState` together with `hintInputSlot`:

```kotlin
var hintState by remember { mutableStateOf(FormulaHintInputState(filledBlanks = List(blankCount) { null })) }

val displayElement = MathMrow(
    fixedFormulaParts.mapIndexed { i, part -> part ?: hintState.toSlotElement(blankIndexFor(i)) },
)
MathCanvas(element = displayElement, colors = colors, editable = true)

// After the user picks an answer for the active blank:
hintState = hintState.fillSlot(MathMi("x"))
```

### Serialization

`MathElement` round-trips to and from MathML strings:

```kotlin
val element = MathMLParser.parse("<math><mi>x</mi><mo>=</mo><mn>1</mn></math>")
val mathml = element.toMathML() // "<math><mi>x</mi><mo>=</mo><mn>1</mn></math>"
```

`stripHiddenAttrs` and `stripPlaceholders` are provided for cleaning up MathML strings that were
authored with Monoxer's hidden-slot (`mx-data-is-hidden`) or placeholder
(`mx-is-placeholder`) extensions before displaying them read-only.

## Public API surface

The canonical source is grouped by domain:

- `com.monoxer.mathmlcompose.math`: Math AST, MathML attributes.
- `com.monoxer.mathmlcompose.mathml`: MathML parsing and serialization.
- `com.monoxer.mathmlcompose.mxhtml`: MxHTML document/segment parsing.
- `com.monoxer.mathmlcompose.input`: editable formula and hint-input state machines.
- `com.monoxer.mathmlcompose.render.compose`: Compose rendering entry points.

Anything under `*.internal.*`, especially `render.compose.internal.mathlayout`, is an
implementation detail and may change without notice between versions.

## Project layout

```
src/commonMain/kotlin/com/monoxer/mathmlcompose/
├── math/                         # Math AST, attributes
├── mathml/                       # MathML parser and serializer
├── mxhtml/                       # MxHTML segment/parser layer
├── input/
│   ├── formula/                  # free-form formula input state
│   └── hint/                     # fill-in-the-blank formula input state
└── render/
    └── compose/                  # Compose UI entry points and math layout internals
```

## Development

```bash
./gradlew jvmTest             # run unit tests on the JVM target
./gradlew ktlintCheck          # check Kotlin formatting and style
./gradlew ktlintFormat         # format Kotlin sources
./gradlew detekt               # run static analysis and auto-correct supported findings
./gradlew build                # build and test all targets; also runs ktlintCheck and detekt
```
