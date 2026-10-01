# ImageToPdfMerger

A desktop Java application for converting image collections into PDF documents with control over **ordering, filtering, image quality, output strategy, and file naming**.

The application is built with **Java Swing** for the desktop interface and **Apache PDFBox 3.0.7** for PDF generation. It can create a single PDF, split a large image set into multiple PDFs, or generate one PDF per folder.

---

## Overview

ImageToPdfMerger is designed for batch image-to-PDF workflows where the order and organization of source files matter.

The application lets you:

- Select a source image folder
- Detect supported image formats
- Filter images by filename keywords
- Choose from several ordering strategies
- Control JPEG compression quality
- Merge everything into one PDF
- Split a large collection into multiple PDFs
- Create one PDF per folder, including the root folder
- Choose automatic or custom PDF names
- Track progress while processing
- Open the resulting output directly from the application

The interface is organized as a guided six-step workflow so the conversion settings remain visible and predictable.

---

## Key Features

### 1. Folder-based image selection

The application uses a directory picker rather than requiring individual files to be selected.

After selecting a folder, it counts supported images in:

- The selected root directory
- Its immediate subfolders

The application reports the discovered image count before processing begins.

### 2. Supported image formats

The current implementation recognizes:

- JPG
- JPEG
- PNG
- BMP
- GIF
- TIFF
- TIF

Image discovery is centralized in `ImageUtils.java`.

### 3. Filename keyword filtering

Images can be excluded before PDF generation by entering one or more comma-separated keywords.

Examples:

```text
thumb
thumb, draft
draft, low, preview
```

Filtering is:

- Case-insensitive
- Applied against the filename
- Optional
- Supported in both normal and per-folder processing modes

An image is skipped when its filename contains any configured keyword.

---

## Image Ordering

Ordering is one of the main parts of the application. The converter supports multiple sorting strategies rather than relying on filesystem order.

### Name-based sorting

- Name A → Z
- Name Z → A

### File timestamp sorting

- File date: oldest first
- File date: newest first

These use the operating system's last-modified timestamp.

### Date embedded in filename

The application can extract date components from common filename patterns such as:

```text
2026-05-07
2026-05-07T14:32:51
20260507
```

It supports ascending or descending date order and can use a trailing number as a tie-breaker.

### Numeric filename sorting

The application can also sort using numbers embedded in filenames:

- Trailing number ascending
- Trailing number descending
- Leading number ascending
- Leading number descending

For example:

```text
001_photo.jpg
002_photo.jpg
010_photo.jpg
```

can be ordered numerically rather than lexicographically.

---

## Output Modes

The converter provides three distinct output strategies.

### Single PDF

All selected images are placed into one PDF.

Each source image becomes a PDF page, with the page sized to the image's pixel dimensions.

```text
image_001.jpg
image_002.jpg
image_003.jpg
        ↓
merged_output.pdf
```

### Sliced PDFs

Large collections can be divided into several PDF files.

The user chooses the number of images per PDF, with the current UI allowing values from **10 to 100,000**.

The application generates names in the form:

```text
<base>_part_001.pdf
<base>_part_002.pdf
<base>_part_003.pdf
```

This is useful when a single PDF becomes too large or when batches need to be distributed separately.

### Per-folder PDFs

The selected root folder and each immediate subfolder are treated as separate image sets.

For example:

```text
Photos/
├── image1.jpg
├── image2.jpg
├── EventA/
│   ├── 001.jpg
│   └── 002.jpg
└── EventB/
    ├── 003.jpg
    └── 004.jpg
```

can produce:

```text
Photos.pdf
EventA.pdf
EventB.pdf
```

Empty folders or folders containing no images after filtering are skipped.

---

## Image Quality

The UI provides five output quality options:

- Original (no compression)
- High quality (90%)
- Medium quality (70%)
- Low quality (50%)
- Very low quality (30%)

The quality setting affects JPEG images.

For non-JPEG formats, the current implementation writes the source image directly into the PDF rather than converting it through the JPEG compression path.

This gives the application a simple tradeoff between output size and JPEG detail while preserving direct embedding for other supported formats.

---

## PDF Generation

PDF creation is implemented with **Apache PDFBox 3.0.7**.

For each accepted image, the application:

1. Loads the image through PDFBox or Java ImageIO, depending on the selected quality path.
2. Creates a PDF page sized to the image dimensions.
3. Adds the image to the page.
4. Appends the page to the output document.
5. Saves the completed PDF to the chosen destination.

The converter continues past individual image failures where possible and records skipped files instead of aborting the complete batch.

---

## Progress and Batch Processing

Large conversions are executed through Swing's `SwingWorker` so the UI can remain responsive while processing.

During a merge, the application reports:

- Current operation
- Current image
- Current PDF part
- Images processed
- Overall percentage
- Number of skipped images
- Number of generated PDFs

A progress bar is updated throughout the operation.

For very large root-level jobs, the UI also shows a warning before processing begins.

---

## File Naming

Output names can be selected in two ways.

### Automatic naming

The application uses the selected folder name.

Example:

```text
MyPhotos
   ↓
MyPhotos.pdf
```

### Custom naming

A user-defined base name can be supplied.

The application sanitizes characters that are invalid or unsafe for a Windows filename.

In per-folder mode, the folder name remains part of the generated filename when automatic naming is enabled; with custom naming, the supplied name acts as a prefix.

---

## Desktop UI

The interface is implemented entirely with **Java Swing**.

The main window is structured into:

```text
1. Select Image Folder
2. Sort Order & Filters
3. Image Quality
4. Output Mode
5. PDF File Naming
6. Output & Merge
7. Progress
```

The UI includes custom styled controls, progress reporting, status messages, and completion/error dialogs.

The default window size is approximately **780 × 880**, with a smaller supported minimum size for constrained desktop windows.

---

## Architecture

The project is intentionally small and desktop-focused.

```text
Main
  ↓
ImageToPdfMerger
  ├── Image discovery
  ├── Filtering
  ├── Sorting
  ├── PDF generation
  ├── Batch slicing
  ├── Per-folder processing
  └── Progress / UI state

ImageUtils
  └── Supported image detection and folder scanning

Apache PDFBox
  └── PDF document and image/page generation
```

### Main classes

#### `Main.java`

Application entry point.

Starts the Swing UI on the Event Dispatch Thread and opens `ImageToPdfMerger`.

#### `ImageToPdfMerger.java`

Contains the main application window and the core conversion workflow, including:

- UI construction
- Folder selection
- Filtering
- Sorting
- PDF naming
- Single-PDF generation
- Sliced-PDF generation
- Per-folder generation
- Image-to-page conversion
- Progress handling
- Completion/error handling

#### `ImageUtils.java`

Provides image-file detection and directory image enumeration.

---

## Project Structure

```text
.
├── src/
│   ├── META-INF/
│   └── com/
│       └── example/
│           ├── ImageToPdfMerger.java
│           ├── ImageUtils.java
│           └── Main.java
├── commons-io-2.16.1.jar
├── commons-logging-1.3.4.jar
├── fontbox-3.0.7.jar
├── pdfbox-3.0.7.jar
├── pdfbox-io-3.0.7.jar
├── ImageToPdfMerger.iml
├── ImageToPdfMerger.xml
├── .gitignore
└── README.md
```

The repository currently uses IntelliJ IDEA module metadata and keeps the required PDFBox-related JAR dependencies alongside the project.

---

## Dependencies

The project currently bundles:

| Dependency | Version | Purpose |
|---|---:|---|
| Apache PDFBox | 3.0.7 | PDF creation and image embedding |
| Apache FontBox | 3.0.7 | PDFBox dependency |
| PDFBox IO | 3.0.7 | PDFBox I/O support |
| Apache Commons IO | 2.16.1 | Supporting I/O functionality |
| Apache Commons Logging | 1.3.4 | Logging dependency |

The application also relies on Java's standard desktop and image APIs, including:

- Swing
- AWT
- Java ImageIO
- NIO file APIs

---

## Requirements

The launcher configuration specifies:

- **Java 17 or newer**
- Desktop environment capable of running Swing applications

The current Launch4j configuration does not require a 64-bit JRE specifically.

---

## Running from IntelliJ IDEA

The project includes an IntelliJ module file:

```text
ImageToPdfMerger.iml
```

Open the project in IntelliJ IDEA and use `Main.java` as the run target.

The application entry point is:

```java
com.example.Main
```

---

## Building a Runnable JAR

The repository is structured as an IntelliJ Java module with the PDFBox dependency JARs already referenced by the project.

A typical IntelliJ workflow is:

1. Open the project.
2. Ensure a Java 17+ JDK is configured.
3. Build the project/artifact.
4. Run the generated JAR with the required PDFBox dependency classes available on the runtime classpath, or package dependencies according to the chosen artifact configuration.

---

## Windows EXE Packaging

The repository includes:

```text
ImageToPdfMerger.xml
```

for **Launch4j** packaging.

The configuration is designed to wrap the Java application as a Windows GUI executable and specifies a minimum Java version of 17.

This makes it possible to distribute the application as a Windows `.exe` rather than requiring users to launch the Java class directly.

The checked-in Launch4j file currently contains machine-specific output paths, so those paths may need to be updated when building on another machine.

---

## Error Handling and Reliability

The application is designed to continue processing when an individual image cannot be added.

During a merge:

- Failed images can be skipped
- The filename and error are logged
- Successful pages continue to be written
- A final status reports how many files were skipped

If no pages can be added, the operation is reported as a failure instead of saving an empty PDF.

The UI also disables relevant controls during processing and restores them when the background task finishes.

---

## Platform Notes

Some completion behavior is Windows-specific.

After successful folder-based output, the application attempts to open the output directory through Windows Explorer.

For a single generated PDF, it can also attempt to open the resulting PDF through the Windows shell.

The core PDF generation itself is implemented with Java and PDFBox, but these convenience actions depend on Windows commands available to the runtime environment.

---

## Current Limitations

The current implementation is intentionally focused on local batch conversion.

Notable boundaries include:

- Only images in the selected root and immediate subfolders are discovered; recursive traversal is not implemented.
- Quality compression settings affect JPEG processing specifically.
- Per-folder output is based on immediate subfolders.
- The Launch4j configuration contains local machine paths that are not portable as-is.
- The project is currently organized around an IntelliJ module rather than a Maven or Gradle build.

These limitations are implementation characteristics of the current repository rather than requirements of PDFBox itself.

---

## Typical Workflow

```text
Choose source folder
       ↓
Inspect detected images
       ↓
Optionally exclude filename keywords
       ↓
Choose sorting strategy
       ↓
Choose image quality
       ↓
Choose output mode
       ↓
Choose PDF naming
       ↓
Select destination
       ↓
Merge in background
       ↓
Review progress / skipped files
       ↓
Open generated PDF(s)
```

---

## License

No explicit open-source license file is currently included in the repository. Unless a license is added, the source remains subject to the applicable copyright restrictions.

The bundled third-party libraries remain subject to their respective licenses.
