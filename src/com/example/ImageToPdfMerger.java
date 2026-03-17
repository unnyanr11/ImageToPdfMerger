package com.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ImageToPdfMerger extends JFrame {

    private final JButton browseButton;
    private final JButton mergeButton;
    private final JComboBox<String> sortCombo;
    private final JComboBox<String> qualityCombo;
    private final JCheckBox sliceCheckbox;
    private final JSpinner imagesPerSpinner;
    private final JLabel inputDirLabel;
    private final JLabel outputPdfLabel;
    private final JLabel imageCountLabel;
    private final JLabel statusLabel;
    private final JProgressBar progressBar;
    private final JLabel progressDetail;
    private Path imageDir;
    private long imageCount = 0;

    public ImageToPdfMerger() {
        super("Image → PDF Merger");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("🖼  Image → PDF Merger", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 10, 0));
        add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        // ── Step 1 — Folder ──
        JPanel folderPanel = new JPanel(new BorderLayout(8, 4));
        folderPanel.setBorder(makeTitled("Step 1 — Select Image Folder"));
        inputDirLabel = new JLabel("No folder selected");
        inputDirLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        imageCountLabel = new JLabel("Images found: —");
        imageCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        browseButton = new JButton("Browse…");
        browseButton.setPreferredSize(new Dimension(100, 32));
        JPanel folderInfo = new JPanel(new GridLayout(2, 1, 2, 2));
        folderInfo.add(inputDirLabel);
        folderInfo.add(imageCountLabel);
        folderPanel.add(folderInfo, BorderLayout.CENTER);
        folderPanel.add(browseButton, BorderLayout.EAST);

        // ── Step 2 — Sort ──
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        sortPanel.setBorder(makeTitled("Step 2 — Sort Order"));
        sortCombo = new JComboBox<>(new String[]{
                "Name  (A → Z)", "Name  (Z → A)",
                "Date  (Oldest first)", "Date  (Newest first)"
        });
        sortCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sortCombo.setPreferredSize(new Dimension(220, 30));
        sortPanel.add(new JLabel("Sort images by:"));
        sortPanel.add(sortCombo);

        // ── Step 3 — Quality ──
        JPanel qualityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        qualityPanel.setBorder(makeTitled("Step 3 — Image Quality"));
        qualityCombo = new JComboBox<>(new String[]{
                "Original (no compression)",
                "High quality   (90%)",
                "Medium quality (70%)",
                "Low quality    (50%)",
                "Very low       (30%)"
        });
        qualityCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        qualityCombo.setPreferredSize(new Dimension(220, 30));
        JLabel qualityNote = new JLabel("⚠ Lower = smaller file, less detail");
        qualityNote.setFont(new Font("SansSerif", Font.ITALIC, 11));
        qualityNote.setForeground(Color.GRAY);
        qualityPanel.add(new JLabel("Quality:"));
        qualityPanel.add(qualityCombo);
        qualityPanel.add(qualityNote);

        // ── Step 4 — Slicing ──
        JPanel slicingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        slicingPanel.setBorder(makeTitled("Step 4 — Slicing (Optional)"));
        sliceCheckbox = new JCheckBox("Split into multiple PDFs");
        sliceCheckbox.setFont(new Font("SansSerif", Font.PLAIN, 13));
        JLabel imagesPerLabel = new JLabel("Images per PDF:");
        imagesPerLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        imagesPerSpinner = new JSpinner(new SpinnerNumberModel(2000, 10, 100000, 100));
        ((JSpinner.DefaultEditor) imagesPerSpinner.getEditor()).getTextField().setColumns(7);
        imagesPerSpinner.setEnabled(false);
        imagesPerLabel.setEnabled(false);
        JLabel sliceNote = new JLabel("e.g. 2000 → part_001.pdf, part_002.pdf…");
        sliceNote.setFont(new Font("SansSerif", Font.ITALIC, 11));
        sliceNote.setForeground(Color.GRAY);
        sliceCheckbox.addItemListener(ev -> {
            boolean on = sliceCheckbox.isSelected();
            imagesPerSpinner.setEnabled(on);
            imagesPerLabel.setEnabled(on);
        });
        slicingPanel.add(sliceCheckbox);
        slicingPanel.add(imagesPerLabel);
        slicingPanel.add(imagesPerSpinner);
        slicingPanel.add(sliceNote);

        // ── Step 5 — Merge ──
        JPanel mergePanel = new JPanel(new BorderLayout(8, 4));
        mergePanel.setBorder(makeTitled("Step 5 — Output & Merge"));
        outputPdfLabel = new JLabel("Output PDF: —");
        outputPdfLabel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        mergeButton = new JButton("Merge Images to PDF");
        mergeButton.setEnabled(false);
        mergeButton.setFont(new Font("SansSerif", Font.BOLD, 13));
        mergeButton.setPreferredSize(new Dimension(200, 35));
        mergePanel.add(outputPdfLabel, BorderLayout.CENTER);
        mergePanel.add(mergeButton, BorderLayout.EAST);

        // ── Step 6 — Progress ──
        JPanel progressPanel = new JPanel(new GridLayout(3, 1, 4, 4));
        progressPanel.setBorder(makeTitled("Progress"));
        statusLabel = new JLabel("Waiting…");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("SansSerif", Font.BOLD, 12));
        progressDetail = new JLabel("No operation running.");
        progressDetail.setFont(new Font("SansSerif", Font.ITALIC, 11));
        progressDetail.setForeground(Color.GRAY);
        progressPanel.add(statusLabel);
        progressPanel.add(progressBar);
        progressPanel.add(progressDetail);

        centerPanel.add(folderPanel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(sortPanel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(qualityPanel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(slicingPanel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(mergePanel);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(progressPanel);
        add(centerPanel, BorderLayout.CENTER);

        JLabel footer = new JLabel(
                "Supports: JPG · JPEG · PNG · BMP · GIF · TIFF  |  Apache PDFBox 3.0.7",
                SwingConstants.CENTER);
        footer.setFont(new Font("SansSerif", Font.PLAIN, 11));
        footer.setForeground(Color.GRAY);
        add(footer, BorderLayout.SOUTH);

        setSize(700, 660);
        setMinimumSize(new Dimension(600, 600));
        setLocationRelativeTo(null);

        browseButton.addActionListener(this::onBrowse);
        mergeButton.addActionListener(this::onMerge);
    }

    private TitledBorder makeTitled(String title) {
        TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1, true), title);
        b.setTitleFont(new Font("SansSerif", Font.BOLD, 12));
        return b;
    }

    private void onBrowse(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showDialog(this, "Select Folder") == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            imageDir = f.toPath();
            inputDirLabel.setText(f.getAbsolutePath());
            imageCount = ImageUtils.imageFilesIn(imageDir).count();
            if (imageCount == 0) {
                imageCountLabel.setText("⚠  No supported images found.");
                imageCountLabel.setForeground(Color.RED);
                mergeButton.setEnabled(false);
            } else {
                imageCountLabel.setText("✔  " + imageCount + " image(s) ready to merge.");
                imageCountLabel.setForeground(new Color(0, 130, 0));
                mergeButton.setEnabled(true);
            }
        }
    }

    private void onMerge(ActionEvent e) {
        if (imageDir == null) return;

        if (imageCount > 1000) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "You are about to merge " + imageCount + " images.\n"
                            + "This may take several minutes.\n\nContinue?",
                    "Large Batch Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
        }

        final boolean enableSlicing = sliceCheckbox.isSelected();
        final int imagesPerPdf = (int) imagesPerSpinner.getValue();

        JFileChooser fc = new JFileChooser();
        fc.setDialogType(JFileChooser.SAVE_DIALOG);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(new FileNameExtensionFilter("PDF files (*.pdf)", "pdf"));
        fc.setSelectedFile(new File(
                System.getProperty("user.home") + "\\Desktop\\merged_output.pdf"));

        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        File out = fc.getSelectedFile();
        if (!out.getName().toLowerCase().endsWith(".pdf")) {
            out = new File(out.getAbsolutePath() + ".pdf");
        }
        final File finalOut = out;

        if (!finalOut.getParentFile().exists()) {
            finalOut.getParentFile().mkdirs();
        }

        mergeButton.setEnabled(false);
        browseButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("0%");
        statusLabel.setText("Preparing…");
        progressDetail.setText("Collecting images…");
        outputPdfLabel.setText("Output: " + finalOut.getParent()
                + (enableSlicing ? " (multiple PDFs)" : " → " + finalOut.getName()));

        final String sortKey    = (String) sortCombo.getSelectedItem();
        final String qualityKey = (String) qualityCombo.getSelectedItem();

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            private Exception error = null;
            // For sliced mode: track how many PDFs were actually created
            private int slicedPdfCount = 0;

            @Override
            protected Void doInBackground() {
                try {
                    // 1. Collect images
                    List<Path> images = new ArrayList<>(
                            ImageUtils.imageFilesIn(imageDir).toList());

                    System.out.println("=== Images found: " + images.size() + " ===");
                    if (!images.isEmpty()) {
                        System.out.println("First: " + images.get(0).getFileName());
                        System.out.println("Last:  " + images.get(images.size() - 1).getFileName());
                    }

                    if (images.isEmpty()) {
                        publish("STATUS:No images found.");
                        return null;
                    }

                    // 2. Sort
                    if (sortKey != null) {
                        switch (sortKey) {
                            case "Name  (A → Z)" ->
                                    images.sort(Comparator.comparing(
                                            p -> p.getFileName().toString().toLowerCase()));
                            case "Name  (Z → A)" ->
                                    images.sort(Comparator.comparing(
                                            (Path p) -> p.getFileName().toString().toLowerCase()).reversed());
                            case "Date  (Oldest first)" ->
                                    images.sort((p1, p2) -> {
                                        try { return Long.compare(
                                                Files.getLastModifiedTime(p1).toMillis(),
                                                Files.getLastModifiedTime(p2).toMillis());
                                        } catch (Exception ex) { return 0; }
                                    });
                            case "Date  (Newest first)" ->
                                    images.sort((p1, p2) -> {
                                        try { return -Long.compare(
                                                Files.getLastModifiedTime(p1).toMillis(),
                                                Files.getLastModifiedTime(p2).toMillis());
                                        } catch (Exception ex) { return 0; }
                                    });
                        }
                    }

                    // 3. Build PDF(s)
                    if (enableSlicing) {
                        doSlicedMerge(images, imagesPerPdf, qualityKey, finalOut);
                    } else {
                        doSingleMerge(images, qualityKey, finalOut);
                    }

                } catch (Throwable ex) {
                    ex.printStackTrace();
                    error = new Exception(ex.getMessage(), ex);
                    publish("STATUS:❌ " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
                return null;
            }

            // ── Single PDF ──
            private void doSingleMerge(List<Path> images, String qualityKey, File outFile)
                    throws Exception {
                long total = images.size();
                int skipped = 0;
                publish("STATUS:Merging " + total + " images…");

                try (PDDocument doc = new PDDocument()) {
                    for (int i = 0; i < images.size(); i++) {
                        if (isCancelled()) break;
                        Path path = images.get(i);
                        try {
                            addImageToPdf(doc, path, qualityKey);
                        } catch (Throwable imgEx) {
                            skipped++;
                            System.err.println("⚠ Skipped [" + (i + 1) + "]: "
                                    + path.getFileName() + " → " + imgEx.getMessage());
                            continue;
                        }
                        int pct = (int) (((i + 1) * 100L) / total);
                        publish("STATUS:Added " + (i + 1) + " / " + total + ":  " + path.getFileName());
                        publish("PERCENT:" + pct);
                    }

                    int added = doc.getNumberOfPages();
                    System.out.println("=== Pages added:  " + added + " ===");
                    System.out.println("=== Skipped:      " + skipped + " ===");

                    if (added == 0) {
                        error = new Exception("0 pages added. All " + skipped + " images failed.");
                        publish("STATUS:❌ 0 pages added — check console.");
                        return;
                    }

                    publish("STATUS:Saving PDF (" + added + " pages)…");
                    doc.save(outFile);
                    System.out.println("✔ Saved:  " + outFile.getAbsolutePath());
                    System.out.println("✔ Size:   " + outFile.length() + " bytes");
                    publish("STATUS:✔ Saved " + added + " pages → " + outFile.getName()
                            + "  (" + skipped + " skipped)");
                    publish("PERCENT:100");
                }
            }

            // ── Sliced PDFs ──
            private void doSlicedMerge(List<Path> images, int perPdf, String qualityKey, File baseFile)
                    throws Exception {
                int total      = images.size();
                int totalPdfs  = (int) Math.ceil((double) total / perPdf);
                int totalDone  = 0;
                int totalSkip  = 0;

                // Base name without extension: e.g. "merged_output"
                String base = baseFile.getName().replaceAll("(?i)\\.pdf$", "");
                File dir = baseFile.getParentFile();

                System.out.println("=== Slicing " + total + " images → " +
                        totalPdfs + " PDFs (" + perPdf + " each) ===");

                for (int part = 0; part < totalPdfs; part++) {
                    if (isCancelled()) break;

                    int startIdx = part * perPdf;
                    int endIdx   = Math.min(startIdx + perPdf, total);
                    int count    = endIdx - startIdx;

                    String partName = String.format("%s_part_%03d.pdf", base, part + 1);
                    File partFile   = new File(dir, partName);

                    publish("STATUS:PDF " + (part + 1) + "/" + totalPdfs +
                            " — adding " + count + " images…");

                    try (PDDocument doc = new PDDocument()) {
                        for (int i = startIdx; i < endIdx; i++) {
                            if (isCancelled()) break;
                            Path path = images.get(i);
                            try {
                                addImageToPdf(doc, path, qualityKey);
                                totalDone++;
                            } catch (Throwable imgEx) {
                                totalSkip++;
                                System.err.println("⚠ Skipped [" + (i + 1) + "]: "
                                        + path.getFileName() + " → " + imgEx.getMessage());
                                continue;
                            }
                            // Progress = overall images done / total
                            int pct = (int) ((totalDone * 100L) / total);
                            publish("STATUS:PDF " + (part + 1) + "/" + totalPdfs
                                    + "  [" + (i - startIdx + 1) + "/" + count + "]  "
                                    + path.getFileName());
                            publish("PERCENT:" + pct);
                        }

                        doc.save(partFile);
                        slicedPdfCount++;
                        System.out.println("✔ Saved: " + partFile.getName() +
                                "  (" + doc.getNumberOfPages() + " pages, " +
                                partFile.length() / 1024 + " KB)");
                    }
                }

                System.out.println("=== Slicing complete ===");
                System.out.println("=== PDFs created: " + slicedPdfCount + " ===");
                System.out.println("=== Pages total:  " + totalDone + " ===");
                System.out.println("=== Skipped:      " + totalSkip + " ===");

                publish("STATUS:✔ Done! " + slicedPdfCount + " PDFs created  ("
                        + totalSkip + " skipped)");
                publish("PERCENT:100");
            }

            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    if (chunk.startsWith("STATUS:")) {
                        String msg = chunk.substring(7);
                        statusLabel.setText(msg);
                        progressDetail.setText(msg);
                    } else if (chunk.startsWith("PERCENT:")) {
                        int pct = Integer.parseInt(chunk.substring(8));
                        progressBar.setValue(pct);
                        progressBar.setString(pct + "%");
                    }
                }
            }

            @Override
            protected void done() {
                mergeButton.setEnabled(true);
                browseButton.setEnabled(true);

                if (error != null) {
                    JOptionPane.showMessageDialog(
                            ImageToPdfMerger.this,
                            "Operation failed:\n" + error.getMessage()
                                    + "\n\nCheck IntelliJ Run console for details.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (isCancelled()) {
                    statusLabel.setText("Cancelled.");
                    return;
                }

                // ── Sliced success dialog ──
                if (enableSlicing) {
                    JOptionPane.showMessageDialog(
                            ImageToPdfMerger.this,
                            "✅ Slicing complete!\n\n"
                                    + slicedPdfCount + " PDFs created in:\n"
                                    + finalOut.getParent()
                                    + "\n\nNaming: "
                                    + finalOut.getName().replaceAll("(?i)\\.pdf$", "")
                                    + "_part_001.pdf … _part_"
                                    + String.format("%03d", slicedPdfCount) + ".pdf",
                            "Done", JOptionPane.INFORMATION_MESSAGE);

                    // Open the output folder
                    try {
                        new ProcessBuilder("explorer", finalOut.getParent()).start();
                    } catch (Exception ignored) {}
                    return;
                }

                // ── Single PDF success dialog ──
                if (!finalOut.exists() || finalOut.length() == 0) {
                    JOptionPane.showMessageDialog(
                            ImageToPdfMerger.this,
                            "PDF not found at:\n" + finalOut.getAbsolutePath()
                                    + "\n\nCheck IntelliJ Run console for the exact error.",
                            "File Not Found", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int open = JOptionPane.showConfirmDialog(
                        ImageToPdfMerger.this,
                        "✅ PDF created!\n\n"
                                + finalOut.getAbsolutePath()
                                + "\nSize: " + (finalOut.length() / 1024) + " KB"
                                + "\n\nOpen it now?",
                        "Success", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

                if (open == JOptionPane.YES_OPTION) {
                    try {
                        new ProcessBuilder("cmd", "/c", "start", "",
                                finalOut.getAbsolutePath()).start();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(
                                ImageToPdfMerger.this,
                                "Saved but could not open automatically.\nPath:\n"
                                        + finalOut.getAbsolutePath(),
                                "Saved", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        };

        worker.execute();
    }

    // ── Add single image to any PDDocument ──
    private void addImageToPdf(PDDocument doc, Path imagePath, String qualityKey) throws Exception {
        String name = imagePath.getFileName().toString().toLowerCase();
        boolean isJpeg = name.endsWith(".jpg") || name.endsWith(".jpeg");

        if (qualityKey == null || qualityKey.startsWith("Original") || !isJpeg) {
            PDImageXObject img = PDImageXObject.createFromFile(imagePath.toString(), doc);
            int w = img.getWidth(), h = img.getHeight();
            PDPage page = new PDPage(new PDRectangle(w, h));
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.drawImage(img, 0, 0, w, h);
            }
            return;
        }

        float quality = switch (qualityKey) {
            case "High quality   (90%)" -> 0.90f;
            case "Medium quality (70%)" -> 0.70f;
            case "Low quality    (50%)" -> 0.50f;
            case "Very low       (30%)" -> 0.30f;
            default -> 1.0f;
        };

        java.awt.image.BufferedImage bImg = javax.imageio.ImageIO.read(imagePath.toFile());
        if (bImg == null) throw new Exception("Cannot read: " + imagePath.getFileName());

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageWriter jpegWriter =
                javax.imageio.ImageIO.getImageWritersByFormatName("jpeg").next();
        javax.imageio.ImageWriteParam jpegParam = jpegWriter.getDefaultWriteParam();
        jpegParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        jpegParam.setCompressionQuality(quality);
        jpegWriter.setOutput(javax.imageio.ImageIO.createImageOutputStream(baos));
        jpegWriter.write(null, new javax.imageio.IIOImage(bImg, null, null), jpegParam);
        jpegWriter.dispose();

        PDImageXObject img = PDImageXObject.createFromByteArray(
                doc, baos.toByteArray(), imagePath.getFileName().toString());
        int w = img.getWidth(), h = img.getHeight();
        PDPage page = new PDPage(new PDRectangle(w, h));
        doc.addPage(page);
        try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
            cs.drawImage(img, 0, 0, w, h);
        }
    }

    private record ProgressHint(String message, int percent) {}
}
