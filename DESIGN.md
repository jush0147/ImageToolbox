# Markit Design

<!-- Durable UI/UX direction for the Markit Android editor. -->

## Mode

Operate.

Markit is a task tool, not a showcase surface. The interface should disappear behind the job: mark an image, hide private data when needed, then save or share.

## Core interaction model

Canvas first.

The image is the primary workspace and keeps the majority of the viewport at all times. The app must never require the user to understand ImageToolbox editor modes, bottom-sheet states, or generic image-processing concepts.

Direct manipulation beats explicit adjustment modes. Existing markup objects are selected by tapping them directly on the canvas. There is no user-facing “Adjust” tool.

Controls are progressive. A tool reveals only the controls needed for that tool. Selecting an object reveals only object actions.

## Compact phone layout

### Top app bar

Persistent screen context and edit history:
- Back
- Title: 畫重點
- Undo
- Redo
- Smart redaction

Secondary image-level actions such as crop and replace image may live in overflow if needed.

### Canvas

The canvas owns all remaining flexible height.

Requirements:
- image remains visible and tappable while controls are present
- no modal editor mode for normal markup operations
- no bottom sheet may cover most of the canvas
- pan/zoom remain available without navigating elsewhere

### Tool dock

One compact row of primary markup tools:
- Arrow
- Box
- Text
- Highlighter
- Redact
- Number

No Adjust tool.

The dock uses Material 3 touch targets of at least 48 dp.

### Context controls

A single compact row appears directly above the tool dock only when needed.

Examples:
- Arrow / Box: color + width
- Text: text input trigger + color + size
- Highlighter: color + width
- Redact: no unnecessary color controls
- Number: color + size
- Selected object: delete / duplicate / object-specific actions

Context controls must not permanently consume canvas height.

### Primary outcomes

Save and Share are equally important.

Both are persistent, equally weighted actions in the main editor UI. Neither is hidden in overflow and neither is visually treated as secondary.

Recommended compact arrangement:
- two equal-width bottom actions: 儲存, 分享
- tool dock sits immediately above them
- context controls appear above the tool dock only when needed

## Empty state

When no image exists, do not render the editor shell.

Show one clear primary action:
- 選取圖片

Opening via Android share intent bypasses the empty state and enters the editor directly.

Use the Android system Photo Picker for manual image selection.

## Selection and object editing

Tapping an existing markup object selects it directly.

Selection should be visually obvious on the canvas.

Object editing should prefer direct manipulation:
- drag to move
- resize using handles or an equivalent direct gesture
- delete from contextual object controls

Do not require:
調整 → 選物件 → 展開控制面板 → 操作

## Image-level actions

Crop and replace image are image-level actions, not markup tools.

They must not appear alongside Arrow / Box / Text / Highlighter / Redact / Number as peers.

## Smart redaction

Smart redaction is an action, not a persistent editor mode.

The interaction should make processing and the resulting redactions understandable. It must not block normal editing after completion.

## Feedback

Use Material 3 feedback patterns:
- Snackbar for successful save and recoverable transient messages
- loading state tied to the action being processed
- dialogs only for destructive or genuinely interruptive decisions

Do not expose raw exception text in the primary UI.

## Android rules

- Material 3 structure and components
- predictive/system Back works normally
- edge-to-edge insets respected
- 48 dp minimum touch targets
- dark theme supported
- system font scaling must not collapse the canvas or clip labels

## Things that must not return

Do not reintroduce:
- AdaptiveBottomScaffoldLayoutScreen as the main Markit editor shell
- generic ImageToolbox bottom-sheet editor controls
- picker mode settings
- embedded MediaPickerActivity dependency
- Adjust as a user-facing tool
- permanent color/width panels
- crop mixed into the markup tool row
- Save hidden behind Share, or Share hidden behind Save
- generic ImageToolbox feature controls merely because the dependency still exists

## Refactor boundary

Preserve and reuse working domain / rendering capabilities where practical:
- draw engine
- markup path state
- undo / redo
- crop implementation
- smart redaction
- save
- share
- OCR and privacy detection
- path move / scale operations

Replace the Markit editor presentation layer and interaction shell.

## Acceptance test

A first-time user should be able to:
1. pick or share in an image
2. immediately see a large usable canvas
3. add an arrow or box without opening another panel
4. tap an existing markup object and edit/delete it without entering an Adjust mode
5. use smart redaction without leaving the editor
6. save or share with one obvious action from the editor

If the user must understand an ImageToolbox-specific concept to complete this flow, the redesign has failed.
