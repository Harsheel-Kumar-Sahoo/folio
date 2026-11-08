# Folio

A minimal Android viewer for **PDF, Word (.docx), Excel (.xlsx) and PowerPoint (.pptx)** files.
Folio registers itself for these file types, so it shows up in the **Open with** menu when you tap a document in WhatsApp (or Files, Gmail, Drive, etc.).

## Build and install

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer).
2. **File → Open** and choose this `Folio` folder. Let Gradle sync finish (needs internet the first time).
3. Plug in your phone (USB debugging on) or start an emulator, then press **Run ▶**.
   To get a shareable APK instead: **Build → Build Bundle(s) / APK(s) → Build APK(s)**.

Requires Android 8.0 (API 26) or newer.

## Using it with WhatsApp

Tap a document in a WhatsApp chat → choose **Folio** in the Open with menu (pick *Always* to make it the default).
Back returns you to WhatsApp.

## What each viewer does

| Type | How it's shown |
| --- | --- |
| PDF | Real page rendering, scroll, double-tap to zoom, page counter |
| Word | Headings, bold/italic, bullets, tables; selectable text |
| Excel | All sheets as tabs, scrollable grid with sticky column headers (first 2,000 rows × 60 columns) |
| PowerPoint | Swipeable slide cards with titles and text |

## Known limits (v1)

- Only modern formats are registered: `.docx`, `.xlsx`, `.pptx`. Old `.doc/.xls/.ppt` are not offered in Open with.
- Word and PowerPoint show text and tables only. Images, charts and shapes are not drawn.
- Excel shows raw stored values: dates appear as serial numbers, and number formats (%, currency) are not applied.
- Password-protected files can't be opened.

## Project layout

- `app/src/main/AndroidManifest.xml`: Open with / Share intent filters
- `app/src/main/java/com/folio/viewer/Parsers.kt`: file-type detection plus DOCX/XLSX/PPTX parsers (no third-party libraries)
- `app/src/main/java/com/folio/viewer/PdfSource.kt`: PDF rendering via Android's built-in `PdfRenderer`
- `app/src/main/java/com/folio/viewer/ui/`: Jetpack Compose UI (theme, home, one view per file type)
