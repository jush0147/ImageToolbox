# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

People who need to mark up a screenshot or image quickly before sending it, especially when the image contains details that should be called out or personal information that should be hidden.

## Product Purpose

Markit / 畫重點 exists to make one image clear, safe to share, and ready to send in as few steps as possible.

The v1 primary flow is:

選一張圖 → 箭頭 / 框 / 文字 / 螢光筆 / 遮蔽 / 編號 → 必要時智慧遮蔽 → 儲存副本或分享

Success means a user can complete that flow without understanding ImageToolbox concepts, editor modes, or advanced image-processing settings.

## Positioning

Markit is a focused quick-markup tool, not a general image editor. Its differentiator is combining fast visual annotation with privacy-oriented smart redaction for common Taiwanese personal-data patterns.

## Operating Context

The app is expected to be used on a phone immediately before sharing an image to another person or app. The image canvas is the primary workspace; controls should stay out of the way until they are needed.

## Capabilities and Constraints

v1 includes:
- open or receive a shared image
- crop
- arrow
- rectangle
- text
- highlighter
- manual solid redaction
- numbered callouts
- select, move, and resize markup objects
- undo / redo
- smart redaction
- save a copy
- share

Smart redaction includes:
- email
- Taiwan mobile numbers
- formatted landline numbers
- Taiwan national ID with checksum
- credit-card numbers with Luhn validation

v1 deliberately excludes general-purpose editor features that slow the primary flow, including filters, background removal, PDF tools, gallery management, cloud sync, accounts, and other ImageToolbox workflows.

Android system affordances should be preferred over custom ImageToolbox infrastructure when they are simpler and more reliable, such as the system Photo Picker.

## Brand Commitments

Product name: 畫重點 / Markit

Android application ID: `com.e04stuff.markit`

The product should feel like a focused Android utility. Existing upstream ImageToolbox branding and interaction patterns are not product commitments.

## Product Principles

1. Canvas first. The image must keep most of the screen and remain directly interactive.
2. Direct manipulation beats modes. Users should tap the thing they want to edit instead of entering a separate adjustment mode.
3. Progressive controls. Show only the controls required by the current tool or selection.
4. Save and share are equal primary outcomes. Both must stay obvious and directly reachable; neither may be demoted into overflow.
5. Delete inherited complexity. If an ImageToolbox interaction does not make the primary flow faster, it does not belong in Markit v1.
