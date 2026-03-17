package com.example;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageToPdfMerger extends JFrame {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color C_BG        = new Color(0xF7F8FA);
    private static final Color C_CARD      = Color.WHITE;
    private static final Color C_ACCENT    = new Color(0x4F6BED);
    private static final Color C_ACCENT_HO = new Color(0x3A54D4);
    private static final Color C_BORDER    = new Color(0xDDE1EA);
    private static final Color C_TEXT      = new Color(0x1A1D23);
    private static final Color C_SUBTEXT   = new Color(0x6B7280);
    private static final Color C_SUCCESS   = new Color(0x16A34A);
    private static final Color C_ERROR     = new Color(0xDC2626);
    private static final Color C_HINT      = new Color(0x4F6BED);
    private static final Color C_STEP_NUM  = new Color(0x4F6BED);
    private static final Font  F_TITLE     = new Font("SansSerif", Font.BOLD,   22);
    private static final Font  F_STEP      = new Font("SansSerif", Font.BOLD,   12);
    private static final Font  F_LABEL     = new Font("SansSerif", Font.PLAIN,  13);
    private static final Font  F_MONO      = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font  F_HINT      = new Font("SansSerif", Font.ITALIC, 11);
    private static final Font  F_BTN       = new Font("SansSerif", Font.BOLD,   13);
    private static final Font  F_MERGE_BTN = new Font("SansSerif", Font.BOLD,   14);

    // ── Widgets ───────────────────────────────────────────────────────────────
    private final JButton           browseButton;
    private final JButton           mergeButton;
    private final JComboBox<String> sortCombo;
    private final JComboBox<String> qualityCombo;
    private final JSpinner          imagesPerSpinner;
    private final JCheckBox         sliceCheckbox;
    private final JCheckBox         subfolderCheckbox;
    private final JLabel            inputDirLabel;
    private final JLabel            outputPdfLabel;
    private final JLabel            imageCountLabel;
    private final JLabel            statusLabel;
    private final JProgressBar      progressBar;
    private final JLabel            progressDetail;
    private final JRadioButton      nameAutoRadio;
    private final JRadioButton      nameCustomRadio;
    private final JTextField        customNameField;

    private Path imageDir;
    private long imageCount = 0;

    // ── Sort constants ────────────────────────────────────────────────────────
    private static final String S_NAME_AZ         = "Name  (A → Z)";
    private static final String S_NAME_ZA         = "Name  (Z → A)";
    private static final String S_FDATE_OLD       = "File Date  (Oldest first)";
    private static final String S_FDATE_NEW       = "File Date  (Newest first)";
    private static final String S_DT_DESC_TR_ASC  = "Date↓ + №↑  (Newest first, № 1→last)";
    private static final String S_DT_DESC_TR_DESC = "Date↓ + №↓  (Newest first, № last→1)";
    private static final String S_DT_ASC_TR_ASC   = "Date↑ + №↑  (Oldest first, № 1→last)";
    private static final String S_DT_ASC_TR_DESC  = "Date↑ + №↓  (Oldest first, № last→1)";
    private static final String S_TRAIL_ASC       = "ID / Trailing №  (Ascending)";
    private static final String S_TRAIL_DESC      = "ID / Trailing №  (Descending)";
    private static final String S_LEAD_ASC        = "ID / Leading №   (Ascending)";
    private static final String S_LEAD_DESC       = "ID / Leading №   (Descending)";

    // ─────────────────────────────────────────────────────────────────────────
    public ImageToPdfMerger() {
        super("Image → PDF Merger");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { System.exit(0); }
        });

        applyGlobalUI();

        setLayout(new BorderLayout());
        getContentPane().setBackground(C_BG);

        // ── Header ────────────────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(C_CARD);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, C_BORDER),
                new EmptyBorder(18, 24, 18, 24)));

        JLabel icon  = new JLabel("🖼");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 28));
        JPanel iconWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        iconWrap.setOpaque(false);
        iconWrap.add(icon);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);
        JLabel titleLabel = new JLabel("Image → PDF Merger");
        titleLabel.setFont(F_TITLE);
        titleLabel.setForeground(C_TEXT);
        JLabel subtitleLabel = new JLabel("Combine images into PDFs with full control");
        subtitleLabel.setFont(F_HINT);
        subtitleLabel.setForeground(C_SUBTEXT);
        titleBlock.add(titleLabel);
        titleBlock.add(Box.createVerticalStrut(2));
        titleBlock.add(subtitleLabel);

        JPanel titleLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titleLeft.setOpaque(false);
        titleLeft.add(iconWrap);
        titleLeft.add(titleBlock);

        JLabel badge = new JLabel("PDFBox 3.0.7");
        badge.setFont(new Font("SansSerif", Font.BOLD, 10));
        badge.setForeground(C_ACCENT);
        badge.setBorder(new CompoundBorder(
                new LineBorder(C_ACCENT, 1, true),
                new EmptyBorder(3, 8, 3, 8)));
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);

        header.add(titleLeft,  BorderLayout.WEST);
        header.add(badgeWrap,  BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ── Scrollable center ─────────────────────────────────────────────────
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(C_BG);
        centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // ── Step 1 — Folder ──────────────────────────────────────────────────
        inputDirLabel   = styledMonoLabel("No folder selected", C_SUBTEXT);
        imageCountLabel = styledLabel("Images found: —", C_SUBTEXT);
        browseButton    = styledSecondaryButton("Browse…");

        JPanel folderContent = new JPanel(new BorderLayout(10, 6));
        folderContent.setOpaque(false);
        JPanel folderInfo = new JPanel(new GridLayout(2, 1, 0, 4));
        folderInfo.setOpaque(false);
        folderInfo.add(inputDirLabel);
        folderInfo.add(imageCountLabel);
        folderContent.add(folderInfo,  BorderLayout.CENTER);
        folderContent.add(browseButton, BorderLayout.EAST);
        centerPanel.add(makeCard("1", "Select Image Folder", folderContent));
        centerPanel.add(vgap(12));

        // ── Step 2 — Sort ────────────────────────────────────────────────────
        sortCombo = styledCombo(new String[]{
                S_NAME_AZ, S_NAME_ZA,
                S_FDATE_OLD, S_FDATE_NEW,
                S_DT_DESC_TR_ASC, S_DT_DESC_TR_DESC,
                S_DT_ASC_TR_ASC,  S_DT_ASC_TR_DESC,
                S_TRAIL_ASC,      S_TRAIL_DESC,
                S_LEAD_ASC,       S_LEAD_DESC
        });

        JLabel sortHint = new JLabel(" ");
        sortHint.setFont(F_HINT);
        sortHint.setForeground(C_HINT);

        sortCombo.addActionListener(ev -> {
            String sel = (String) sortCombo.getSelectedItem();
            if (sel == null) return;
            sortHint.setText(switch (sel) {
                case S_NAME_AZ         -> "  ✦ apple.jpg  →  banana.jpg  →  cherry.jpg";
                case S_NAME_ZA         -> "  ✦ cherry.jpg  →  banana.jpg  →  apple.jpg";
                case S_FDATE_OLD       -> "  ✦ Uses OS last-modified timestamp — oldest file first";
                case S_FDATE_NEW       -> "  ✦ Uses OS last-modified timestamp — newest file first";
                case S_DT_DESC_TR_ASC  -> "  ✦ Year↓ Month↓ Day↓ Hour↓ Min↓ Sec↓  →  tie: _1 _2 _3…";
                case S_DT_DESC_TR_DESC -> "  ✦ Year↓ Month↓ Day↓ Hour↓ Min↓ Sec↓  →  tie: _3 _2 _1…";
                case S_DT_ASC_TR_ASC   -> "  ✦ Year↑ Month↑ Day↑ Hour↑ Min↑ Sec↑  →  tie: _1 _2 _3…";
                case S_DT_ASC_TR_DESC  -> "  ✦ Year↑ Month↑ Day↑ Hour↑ Min↑ Sec↑  →  tie: _3 _2 _1…";
                case S_TRAIL_ASC       -> "  ✦ Last number before extension  e.g. clip_1 < clip_2 < clip_10";
                case S_TRAIL_DESC      -> "  ✦ Last number before extension  e.g. clip_10 > clip_2 > clip_1";
                case S_LEAD_ASC        -> "  ✦ First number in filename  e.g. 001_photo < 002_photo < 010_photo";
                case S_LEAD_DESC       -> "  ✦ First number in filename  e.g. 010_photo > 002_photo > 001_photo";
                default -> " ";
            });
        });

        JPanel sortContent = new JPanel();
        sortContent.setLayout(new BoxLayout(sortContent, BoxLayout.Y_AXIS));
        sortContent.setOpaque(false);
        JPanel sortRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        sortRow.setOpaque(false);
        JLabel sortByLabel = styledLabel("Sort images by:", C_TEXT);
        sortByLabel.setBorder(new EmptyBorder(0, 0, 0, 10));
        sortRow.add(sortByLabel);
        sortRow.add(sortCombo);
        sortContent.add(sortRow);
        sortContent.add(Box.createVerticalStrut(6));
        sortContent.add(sortHint);

        sortCombo.setSelectedIndex(0);
        sortCombo.getActionListeners()[0].actionPerformed(null);
        centerPanel.add(makeCard("2", "Sort Order", sortContent));
        centerPanel.add(vgap(12));

        // ── Step 3 — Quality ─────────────────────────────────────────────────
        qualityCombo = styledCombo(new String[]{
                "Original (no compression)",
                "High quality   (90%)",
                "Medium quality (70%)",
                "Low quality    (50%)",
                "Very low       (30%)"
        });
        JLabel qualityNote = new JLabel("  ⚠  Lower quality = smaller file size, less detail");
        qualityNote.setFont(F_HINT);
        qualityNote.setForeground(new Color(0xB45309));

        JPanel qualityContent = new JPanel();
        qualityContent.setLayout(new BoxLayout(qualityContent, BoxLayout.Y_AXIS));
        qualityContent.setOpaque(false);
        JPanel qualityRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        qualityRow.setOpaque(false);
        JLabel qLabel = styledLabel("Output quality:", C_TEXT);
        qLabel.setBorder(new EmptyBorder(0, 0, 0, 10));
        qualityRow.add(qLabel);
        qualityRow.add(qualityCombo);
        qualityContent.add(qualityRow);
        qualityContent.add(Box.createVerticalStrut(6));
        qualityContent.add(qualityNote);
        centerPanel.add(makeCard("3", "Image Quality", qualityContent));
        centerPanel.add(vgap(12));

        // ── Step 4 — Output Mode ─────────────────────────────────────────────
        ButtonGroup modeGroup = new ButtonGroup();
        JRadioButton modeSingle    = styledRadio("Single PDF  — all images into one file");
        JRadioButton modeSliced    = styledRadio("Sliced PDFs  — split into multiple files");
        JRadioButton modeSubfolder = styledRadio("Per-folder  — root + each subfolder → separate PDFs");
        modeSingle.setSelected(true);
        for (JRadioButton rb : new JRadioButton[]{modeSingle, modeSliced, modeSubfolder})
            modeGroup.add(rb);

        imagesPerSpinner = new JSpinner(new SpinnerNumberModel(2000, 10, 100_000, 100));
        styleSpinner(imagesPerSpinner);

        JLabel sliceNote = styledItalicLabel("→ output: part_001.pdf, part_002.pdf…");
        JPanel sliceSubRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sliceSubRow.setOpaque(false);
        sliceSubRow.setBorder(new EmptyBorder(4, 24, 0, 0));
        sliceSubRow.add(styledLabel("Images per PDF:", C_SUBTEXT));
        sliceSubRow.add(imagesPerSpinner);
        sliceSubRow.add(sliceNote);
        sliceSubRow.setVisible(false);

        JPanel subSubRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        subSubRow.setOpaque(false);
        subSubRow.setBorder(new EmptyBorder(4, 24, 0, 0));
        subSubRow.add(styledItalicLabel("→ Creates one PDF per folder (root included). Empty folders are skipped."));
        subSubRow.setVisible(false);

        modeSliced   .addItemListener(ev -> sliceSubRow.setVisible(modeSliced.isSelected()));
        modeSubfolder.addItemListener(ev -> subSubRow.setVisible(modeSubfolder.isSelected()));

        sliceCheckbox     = new JCheckBox();
        subfolderCheckbox = new JCheckBox();

        JPanel modeContent = new JPanel();
        modeContent.setLayout(new BoxLayout(modeContent, BoxLayout.Y_AXIS));
        modeContent.setOpaque(false);
        modeContent.add(modeSingle);
        modeContent.add(Box.createVerticalStrut(6));
        modeContent.add(modeSliced);
        modeContent.add(sliceSubRow);
        modeContent.add(Box.createVerticalStrut(6));
        modeContent.add(modeSubfolder);
        modeContent.add(subSubRow);
        centerPanel.add(makeCard("4", "Output Mode", modeContent));
        centerPanel.add(vgap(12));

        // ── Step 5 — PDF Naming ──────────────────────────────────────────────
        ButtonGroup nameGroup = new ButtonGroup();
        nameAutoRadio   = styledRadio("Use folder name  (e.g. MyFolder.pdf)");
        nameCustomRadio = styledRadio("Custom name:");
        nameAutoRadio.setSelected(true);
        nameGroup.add(nameAutoRadio);
        nameGroup.add(nameCustomRadio);

        customNameField = new JTextField("merged_output");
        customNameField.setFont(F_MONO);
        customNameField.setEnabled(false);
        customNameField.setBackground(new Color(0xF3F4F6));
        customNameField.setForeground(C_TEXT);
        customNameField.setBorder(new CompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)));
        customNameField.setPreferredSize(new Dimension(220, 28));
        nameCustomRadio.addItemListener(ev -> {
            customNameField.setEnabled(nameCustomRadio.isSelected());
            customNameField.setBackground(nameCustomRadio.isSelected()
                    ? Color.WHITE : new Color(0xF3F4F6));
        });

        JLabel namingHint = styledItalicLabel(
                "ℹ  In per-folder mode the folder name is always used; custom name acts as a prefix.");

        JPanel customRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        customRow.setOpaque(false);
        customRow.add(nameCustomRadio);
        customRow.add(customNameField);

        JPanel namingContent = new JPanel();
        namingContent.setLayout(new BoxLayout(namingContent, BoxLayout.Y_AXIS));
        namingContent.setOpaque(false);
        namingContent.add(nameAutoRadio);
        namingContent.add(Box.createVerticalStrut(6));
        namingContent.add(customRow);
        namingContent.add(Box.createVerticalStrut(6));
        namingContent.add(namingHint);
        centerPanel.add(makeCard("5", "PDF File Naming", namingContent));
        centerPanel.add(vgap(12));

        // ── Step 6 — Output & Merge ──────────────────────────────────────────
        outputPdfLabel = styledMonoLabel("Output PDF: —", C_SUBTEXT);
        mergeButton    = styledPrimaryButton("⚙  Merge Images to PDF");
        mergeButton.setEnabled(false);

        JPanel mergeContent = new JPanel(new BorderLayout(12, 0));
        mergeContent.setOpaque(false);
        mergeContent.add(outputPdfLabel, BorderLayout.CENTER);
        mergeContent.add(mergeButton,   BorderLayout.EAST);
        centerPanel.add(makeCard("6", "Output & Merge", mergeContent));
        centerPanel.add(vgap(12));

        // ── Progress ─────────────────────────────────────────────────────────
        statusLabel   = styledLabel("Waiting…", C_SUBTEXT);
        progressBar   = buildProgressBar();
        progressDetail = styledItalicLabel("No operation running.");

        JPanel progressContent = new JPanel();
        progressContent.setLayout(new BoxLayout(progressContent, BoxLayout.Y_AXIS));
        progressContent.setOpaque(false);
        progressContent.add(statusLabel);
        progressContent.add(Box.createVerticalStrut(8));
        progressContent.add(progressBar);
        progressContent.add(Box.createVerticalStrut(6));
        progressContent.add(progressDetail);
        centerPanel.add(makeCard("✓", "Progress", progressContent));

        JScrollPane scroll = new JScrollPane(centerPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(C_BG);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 10));
        footer.setBackground(C_CARD);
        footer.setBorder(new MatteBorder(1, 0, 0, 0, C_BORDER));
        for (String fmt : new String[]{"JPG", "JPEG", "PNG", "BMP", "GIF", "TIFF"}) {
            JLabel chip = new JLabel(fmt);
            chip.setFont(new Font("SansSerif", Font.BOLD, 10));
            chip.setForeground(C_SUBTEXT);
            chip.setBorder(new CompoundBorder(
                    new LineBorder(C_BORDER, 1, true),
                    new EmptyBorder(2, 7, 2, 7)));
            footer.add(chip);
        }
        add(footer, BorderLayout.SOUTH);

        setSize(780, 860);
        setMinimumSize(new Dimension(660, 720));
        setLocationRelativeTo(null);

        browseButton.addActionListener(this::onBrowse);
        mergeButton.addActionListener(ev -> onMerge(
                modeSliced.isSelected(),
                modeSubfolder.isSelected(),
                (int) imagesPerSpinner.getValue()));
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private void applyGlobalUI() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("ComboBox.background",        Color.WHITE);
        UIManager.put("ComboBox.foreground",        C_TEXT);
        UIManager.put("ComboBox.selectionBackground", C_ACCENT);
        UIManager.put("ComboBox.selectionForeground", Color.WHITE);
        UIManager.put("RadioButton.background",     C_CARD);
        UIManager.put("RadioButton.foreground",     C_TEXT);
        UIManager.put("CheckBox.background",        C_CARD);
    }

    /** Card with coloured step-number badge and bold title. */
    private JPanel makeCard(String step, String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 10));
        card.setBackground(C_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                new EmptyBorder(16, 18, 16, 18)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        // header row
        JPanel hdr = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        hdr.setOpaque(false);

        JLabel stepBadge = new JLabel(step);
        stepBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
        stepBadge.setForeground(Color.WHITE);
        stepBadge.setOpaque(true);
        stepBadge.setBackground(C_STEP_NUM);
        stepBadge.setBorder(new EmptyBorder(2, 8, 2, 8));
        // round badge via panel trick
        JPanel badgePanel = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_STEP_NUM);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badgePanel.setOpaque(false);
        badgePanel.setLayout(new BorderLayout());
        badgePanel.add(stepBadge);
        stepBadge.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(F_STEP);
        titleLbl.setForeground(C_TEXT);

        hdr.add(badgePanel);
        hdr.add(titleLbl);

        // separator
        JSeparator sep = new JSeparator();
        sep.setForeground(C_BORDER);

        card.add(hdr,     BorderLayout.NORTH);
        card.add(sep,     BorderLayout.CENTER);

        JPanel contentWrap = new JPanel(new BorderLayout());
        contentWrap.setOpaque(false);
        contentWrap.setBorder(new EmptyBorder(8, 0, 0, 0));
        contentWrap.add(content, BorderLayout.CENTER);
        card.add(contentWrap, BorderLayout.SOUTH);

        return card;
    }

    private JLabel styledLabel(String text, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(F_LABEL);
        l.setForeground(fg);
        return l;
    }

    private JLabel styledMonoLabel(String text, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(F_MONO);
        l.setForeground(fg);
        return l;
    }

    private JLabel styledItalicLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(F_HINT);
        l.setForeground(C_SUBTEXT);
        return l;
    }

    private JRadioButton styledRadio(String text) {
        JRadioButton rb = new JRadioButton(text);
        rb.setFont(F_LABEL);
        rb.setForeground(C_TEXT);
        rb.setOpaque(false);
        rb.setBackground(C_CARD);
        rb.setFocusPainted(false);
        return rb;
    }

    private <T> JComboBox<T> styledCombo(T[] items) {
        JComboBox<T> cb = new JComboBox<>(items);
        cb.setFont(F_LABEL);
        cb.setBackground(Color.WHITE);
        cb.setForeground(C_TEXT);
        cb.setBorder(new CompoundBorder(
                new LineBorder(C_BORDER, 1, true),
                new EmptyBorder(2, 4, 2, 4)));
        cb.setFocusable(false);
        cb.setPreferredSize(new Dimension(300, 30));
        return cb;
    }

    private void styleSpinner(JSpinner sp) {
        sp.setFont(F_LABEL);
        sp.setPreferredSize(new Dimension(90, 28));
        ((JSpinner.DefaultEditor) sp.getEditor()).getTextField().setColumns(6);
        sp.setBorder(new LineBorder(C_BORDER, 1, true));
    }

    private JButton styledPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = !isEnabled() ? new Color(0xCBD5E1)
                        : getModel().isPressed()  ? C_ACCENT_HO
                        : getModel().isRollover() ? C_ACCENT_HO
                        : C_ACCENT;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(F_MERGE_BTN);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(220, 38));
        btn.setBorder(new EmptyBorder(0, 16, 0, 16));
        return btn;
    }

    private JButton styledSecondaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()  ? new Color(0xE2E8F0)
                        : getModel().isRollover() ? new Color(0xF1F5F9)
                        : Color.WHITE;
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(C_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth()-1, getHeight()-1, 8, 8));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(F_BTN);
        btn.setForeground(C_TEXT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setOpaque(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 34));
        return btn;
    }

    private JProgressBar buildProgressBar() {
        JProgressBar pb = new JProgressBar(0, 100) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // track
                g2.setColor(new Color(0xE2E8F0));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), getHeight(), getHeight()));
                // fill
                int fillW = (int)((getWidth() * (long) getValue()) / getMaximum());
                if (fillW > 0) {
                    g2.setColor(C_ACCENT);
                    g2.fill(new RoundRectangle2D.Float(0, 0, fillW, getHeight(), getHeight(), getHeight()));
                }
                // text
                g2.setColor(getValue() > 50 ? Color.WHITE : C_TEXT);
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                String s = getString();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s, (getWidth() - fm.stringWidth(s)) / 2,
                        (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        pb.setStringPainted(false);
        pb.setString("0%");
        pb.setPreferredSize(new Dimension(Integer.MAX_VALUE, 18));
        pb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        pb.setOpaque(false);
        pb.setBorderPainted(false);
        pb.setAlignmentX(Component.LEFT_ALIGNMENT);
        return pb;
    }

    private Component vgap(int h) {
        return Box.createVerticalStrut(h);
    }

    // ── onBrowse ──────────────────────────────────────────────────────────────
    private void onBrowse(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showDialog(this, "Select Folder") != JFileChooser.APPROVE_OPTION) return;

        File f = fc.getSelectedFile();
        imageDir   = f.toPath();
        imageCount = ImageUtils.imageFilesIn(imageDir).count();

        File[] subs   = f.listFiles(File::isDirectory);
        long subCount = subs == null ? 0
                : Arrays.stream(subs)
                .mapToLong(s -> ImageUtils.imageFilesIn(s.toPath()).count())
                .sum();
        long total = imageCount + subCount;

        inputDirLabel.setText(f.getAbsolutePath());
        inputDirLabel.setForeground(C_TEXT);
        customNameField.setText(f.getName());

        if (total == 0) {
            imageCountLabel.setText("⚠  No supported images found");
            imageCountLabel.setForeground(C_ERROR);
            mergeButton.setEnabled(false);
        } else {
            String detail = subCount > 0
                    ? imageCount + " in root  +  " + subCount + " in subfolders"
                    : imageCount + " image(s)";
            imageCountLabel.setText("✔  " + total + " image(s) found  (" + detail + ")");
            imageCountLabel.setForeground(C_SUCCESS);
            mergeButton.setEnabled(true);
        }
    }

    // ── onMerge ───────────────────────────────────────────────────────────────
    private void onMerge(boolean enableSlicing, boolean enableSubfolder, int imagesPerPdf) {
        if (imageDir == null) return;

        if (imageCount > 1000 && !enableSubfolder) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "You are about to merge " + imageCount + " images.\n"
                            + "This may take several minutes.\n\nContinue?",
                    "Large Batch Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION) return;
        }

        final boolean useAutoName = nameAutoRadio.isSelected();
        final String  customBase  = sanitize(customNameField.getText().trim());

        File outDir;
        File finalOut;

        if (enableSubfolder || useAutoName) {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select Output Folder");
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            fc.setSelectedFile(new File(System.getProperty("user.home") + "\\Desktop"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            outDir = fc.getSelectedFile();
            String rootName = useAutoName
                    ? sanitize(imageDir.getFileName().toString()) : customBase;
            finalOut = new File(outDir, rootName + ".pdf");
        } else {
            JFileChooser fc = new JFileChooser();
            fc.setDialogType(JFileChooser.SAVE_DIALOG);
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fc.setFileFilter(new FileNameExtensionFilter("PDF files (*.pdf)", "pdf"));
            String suggested = customBase.isEmpty() ? "merged_output" : customBase;
            fc.setSelectedFile(new File(System.getProperty("user.home")
                    + "\\Desktop\\" + suggested + ".pdf"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            finalOut = fc.getSelectedFile();
            if (!finalOut.getName().toLowerCase().endsWith(".pdf"))
                finalOut = new File(finalOut.getAbsolutePath() + ".pdf");
            outDir = finalOut.getParentFile();
        }

        if (!outDir.exists()) outDir.mkdirs();

        final File finalOutF = finalOut;
        final File outDirF   = outDir;

        mergeButton.setEnabled(false);
        browseButton.setEnabled(false);
        progressBar.setValue(0);
        progressBar.setString("0%");
        statusLabel.setText("Preparing…");
        statusLabel.setForeground(C_SUBTEXT);
        progressDetail.setText("Collecting images…");

        if (enableSubfolder)
            outputPdfLabel.setText("Output: " + outDir.getAbsolutePath() + "  (per-folder PDFs)");
        else if (enableSlicing)
            outputPdfLabel.setText("Output: " + outDir.getAbsolutePath() + "  (sliced PDFs)");
        else
            outputPdfLabel.setText("Output: " + finalOutF.getAbsolutePath());
        outputPdfLabel.setForeground(C_TEXT);

        final String sortKey    = (String) sortCombo.getSelectedItem();
        final String qualityKey = (String) qualityCombo.getSelectedItem();

        SwingWorker<Void, String> worker = new SwingWorker<>() {
            private Exception error        = null;
            private int       createdCount = 0;

            @Override
            protected Void doInBackground() {
                try {
                    if (enableSubfolder) {
                        doSubfolderMerge(imageDir.toFile(), sortKey, qualityKey,
                                outDirF, useAutoName, customBase);
                    } else {
                        List<Path> images = new ArrayList<>(
                                ImageUtils.imageFilesIn(imageDir).toList());
                        System.out.println("=== Images found: " + images.size() + " ===");
                        if (images.isEmpty()) { publish("STATUS:No images found."); return null; }
                        sortImages(images, sortKey);
                        if (enableSlicing)
                            doSlicedMerge(images, imagesPerPdf, qualityKey, finalOutF);
                        else
                            doSingleMerge(images, qualityKey, finalOutF);
                    }
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    error = new Exception(ex.getMessage(), ex);
                    publish("STATUS:❌ " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
                return null;
            }

            private void doSingleMerge(List<Path> images, String qKey, File outFile)
                    throws Exception {
                long total = images.size(); int skipped = 0;
                publish("STATUS:Merging " + total + " images…");
                try (PDDocument doc = new PDDocument()) {
                    for (int i = 0; i < images.size(); i++) {
                        if (isCancelled()) break;
                        Path path = images.get(i);
                        try { addImageToPdf(doc, path, qKey); }
                        catch (Throwable ex) {
                            skipped++;
                            System.err.println("⚠ Skipped [" + (i+1) + "]: "
                                    + path.getFileName() + " → " + ex.getMessage()); continue;
                        }
                        publish("STATUS:Added " + (i+1) + " / " + total + ":  " + path.getFileName());
                        publish("PERCENT:" + (int)(((i+1) * 100L) / total));
                    }
                    int added = doc.getNumberOfPages();
                    if (added == 0) {
                        error = new Exception("0 pages added. All " + skipped + " images failed.");
                        publish("STATUS:❌ 0 pages added — check console."); return;
                    }
                    publish("STATUS:Saving PDF (" + added + " pages)…");
                    doc.save(outFile);
                    publish("STATUS:✔ Saved " + added + " pages → " + outFile.getName()
                            + "  (" + skipped + " skipped)");
                    publish("PERCENT:100");
                    createdCount = 1;
                }
            }

            private void doSlicedMerge(List<Path> images, int perPdf, String qKey, File baseFile)
                    throws Exception {
                int total = images.size(), totalPdfs = (int) Math.ceil((double) total / perPdf);
                int totalDone = 0, totalSkip = 0;
                String base = baseFile.getName().replaceAll("(?i)\\.pdf$", "");
                File   dir  = baseFile.getParentFile();
                for (int part = 0; part < totalPdfs; part++) {
                    if (isCancelled()) break;
                    int startIdx = part * perPdf, endIdx = Math.min(startIdx + perPdf, total);
                    File partFile = new File(dir, String.format("%s_part_%03d.pdf", base, part+1));
                    publish("STATUS:PDF " + (part+1) + "/" + totalPdfs
                            + " — adding " + (endIdx - startIdx) + " images…");
                    try (PDDocument doc = new PDDocument()) {
                        for (int i = startIdx; i < endIdx; i++) {
                            if (isCancelled()) break;
                            Path path = images.get(i);
                            try { addImageToPdf(doc, path, qKey); totalDone++; }
                            catch (Throwable ex) {
                                totalSkip++;
                                System.err.println("⚠ Skipped [" + (i+1) + "]: "
                                        + path.getFileName() + " → " + ex.getMessage()); continue;
                            }
                            publish("STATUS:PDF " + (part+1) + "/" + totalPdfs
                                    + "  [" + (i-startIdx+1) + "/" + (endIdx-startIdx) + "]  "
                                    + path.getFileName());
                            publish("PERCENT:" + (int)((totalDone * 100L) / total));
                        }
                        doc.save(partFile); createdCount++;
                    }
                }
                publish("STATUS:✔ Done! " + createdCount + " PDFs  (" + totalSkip + " skipped)");
                publish("PERCENT:100");
            }

            private void doSubfolderMerge(File rootDir, String sKey, String qKey,
                                          File outDirectory, boolean autoName, String customPrefix)
                    throws Exception {
                List<FolderScope> scopes = new ArrayList<>();
                List<Path> rootImages = new ArrayList<>(ImageUtils.imageFilesIn(rootDir.toPath()).toList());
                if (!rootImages.isEmpty()) {
                    String pdfName = autoName ? sanitize(rootDir.getName()) + ".pdf"
                            : customPrefix + ".pdf";
                    scopes.add(new FolderScope(rootDir, rootImages, pdfName));
                }
                File[] subs = rootDir.listFiles(File::isDirectory);
                if (subs != null) {
                    Arrays.sort(subs, Comparator.comparing(File::getName));
                    for (File sub : subs) {
                        List<Path> imgs = new ArrayList<>(ImageUtils.imageFilesIn(sub.toPath()).toList());
                        if (imgs.isEmpty()) { System.out.println("⊘ Skipped: " + sub.getName()); continue; }
                        String pdfName = autoName ? sanitize(sub.getName()) + ".pdf"
                                : customPrefix + "_" + sanitize(sub.getName()) + ".pdf";
                        scopes.add(new FolderScope(sub, imgs, pdfName));
                    }
                }
                if (scopes.isEmpty()) { publish("STATUS:No images found anywhere."); return; }
                long grandTotal = scopes.stream().mapToLong(s -> s.images.size()).sum();
                long grandDone  = 0; int grandSkip = 0;
                for (FolderScope scope : scopes) {
                    if (isCancelled()) break;
                    sortImages(scope.images, sKey);
                    File pdfFile = new File(outDirectory, scope.pdfName);
                    publish("STATUS:[" + scope.folder.getName() + "]  "
                            + scope.images.size() + " images → " + scope.pdfName);
                    try (PDDocument doc = new PDDocument()) {
                        for (int i = 0; i < scope.images.size(); i++) {
                            if (isCancelled()) break;
                            Path path = scope.images.get(i);
                            try { addImageToPdf(doc, path, qKey); grandDone++; }
                            catch (Throwable ex) {
                                grandSkip++;
                                System.err.println("⚠ Skipped ["
                                        + scope.folder.getName() + "/" + (i+1) + "]: "
                                        + path.getFileName() + " → " + ex.getMessage()); continue;
                            }
                            int pct = grandTotal > 0 ? (int)((grandDone * 100L) / grandTotal) : 0;
                            publish("STATUS:[" + scope.folder.getName() + "]  "
                                    + (i+1) + "/" + scope.images.size() + "  " + path.getFileName());
                            publish("PERCENT:" + pct);
                        }
                        if (doc.getNumberOfPages() == 0) { System.out.println("⊘ 0 pages: " + scope.pdfName); continue; }
                        doc.save(pdfFile); createdCount++;
                    }
                }
                publish("STATUS:✔ Done! " + createdCount + " PDFs created  (" + grandSkip + " skipped)");
                publish("PERCENT:100");
            }

            private void sortImages(List<Path> images, String sKey) {
                if (sKey == null) return;
                switch (sKey) {
                    case S_NAME_AZ ->
                            images.sort(Comparator.comparing(p -> p.getFileName().toString().toLowerCase()));
                    case S_NAME_ZA ->
                            images.sort(Comparator.comparing(
                                    (Path p) -> p.getFileName().toString().toLowerCase()).reversed());
                    case S_FDATE_OLD ->
                            images.sort((p1, p2) -> {
                                try { return Long.compare(Files.getLastModifiedTime(p1).toMillis(),
                                        Files.getLastModifiedTime(p2).toMillis());
                                } catch (Exception ex) { return 0; }
                            });
                    case S_FDATE_NEW ->
                            images.sort((p1, p2) -> {
                                try { return -Long.compare(Files.getLastModifiedTime(p1).toMillis(),
                                        Files.getLastModifiedTime(p2).toMillis());
                                } catch (Exception ex) { return 0; }
                            });
                    case S_DT_DESC_TR_ASC ->
                            images.sort((a, b) -> {
                                int cmp = parseDateComponents(b.getFileName().toString())
                                        .compareTo(parseDateComponents(a.getFileName().toString()));
                                if (cmp != 0) return cmp;
                                return Long.compare(extractTrailingNumber(a.getFileName().toString()),
                                        extractTrailingNumber(b.getFileName().toString()));
                            });
                    case S_DT_DESC_TR_DESC ->
                            images.sort((a, b) -> {
                                int cmp = parseDateComponents(b.getFileName().toString())
                                        .compareTo(parseDateComponents(a.getFileName().toString()));
                                if (cmp != 0) return cmp;
                                return Long.compare(extractTrailingNumber(b.getFileName().toString()),
                                        extractTrailingNumber(a.getFileName().toString()));
                            });
                    case S_DT_ASC_TR_ASC ->
                            images.sort((a, b) -> {
                                int cmp = parseDateComponents(a.getFileName().toString())
                                        .compareTo(parseDateComponents(b.getFileName().toString()));
                                if (cmp != 0) return cmp;
                                return Long.compare(extractTrailingNumber(a.getFileName().toString()),
                                        extractTrailingNumber(b.getFileName().toString()));
                            });
                    case S_DT_ASC_TR_DESC ->
                            images.sort((a, b) -> {
                                int cmp = parseDateComponents(a.getFileName().toString())
                                        .compareTo(parseDateComponents(b.getFileName().toString()));
                                if (cmp != 0) return cmp;
                                return Long.compare(extractTrailingNumber(b.getFileName().toString()),
                                        extractTrailingNumber(a.getFileName().toString()));
                            });
                    case S_TRAIL_ASC ->
                            images.sort(Comparator.comparingLong(
                                    p -> extractTrailingNumber(p.getFileName().toString())));
                    case S_TRAIL_DESC ->
                            images.sort(Comparator.comparingLong(
                                    (Path p) -> extractTrailingNumber(p.getFileName().toString())).reversed());
                    case S_LEAD_ASC ->
                            images.sort(Comparator.comparingLong(
                                    p -> extractLeadingNumber(p.getFileName().toString())));
                    case S_LEAD_DESC ->
                            images.sort(Comparator.comparingLong(
                                    (Path p) -> extractLeadingNumber(p.getFileName().toString())).reversed());
                }
            }

            @Override
            protected void process(List<String> chunks) {
                for (String chunk : chunks) {
                    if (chunk.startsWith("STATUS:")) {
                        String msg = chunk.substring(7);
                        statusLabel.setText(msg);
                        progressDetail.setText(msg);
                        boolean ok  = msg.startsWith("✔");
                        boolean err = msg.startsWith("❌");
                        statusLabel.setForeground(ok ? C_SUCCESS : err ? C_ERROR : C_SUBTEXT);
                    } else if (chunk.startsWith("PERCENT:")) {
                        int pct = Integer.parseInt(chunk.substring(8));
                        progressBar.setValue(pct);
                        progressBar.setString(pct + "%");
                        progressBar.repaint();
                    }
                }
            }

            @Override
            protected void done() {
                mergeButton.setEnabled(true);
                browseButton.setEnabled(true);
                if (error != null) {
                    JOptionPane.showMessageDialog(ImageToPdfMerger.this,
                            "Operation failed:\n" + error.getMessage()
                                    + "\n\nCheck IntelliJ console for details.",
                            "Error", JOptionPane.ERROR_MESSAGE); return;
                }
                if (isCancelled()) { statusLabel.setText("Cancelled."); return; }
                if (enableSubfolder || enableSlicing) {
                    JOptionPane.showMessageDialog(ImageToPdfMerger.this,
                            "✅ Complete!\n\n" + createdCount + " PDF(s) created in:\n"
                                    + outDirF.getAbsolutePath(),
                            "Done", JOptionPane.INFORMATION_MESSAGE);
                    try { new ProcessBuilder("explorer", outDirF.getAbsolutePath()).start(); }
                    catch (Exception ignored) {}
                    return;
                }
                if (!finalOutF.exists() || finalOutF.length() == 0) {
                    JOptionPane.showMessageDialog(ImageToPdfMerger.this,
                            "PDF not found at:\n" + finalOutF.getAbsolutePath()
                                    + "\n\nCheck IntelliJ console.",
                            "File Not Found", JOptionPane.ERROR_MESSAGE); return;
                }
                int open = JOptionPane.showConfirmDialog(ImageToPdfMerger.this,
                        "✅ PDF created!\n\n" + finalOutF.getAbsolutePath()
                                + "\nSize: " + (finalOutF.length() / 1024) + " KB"
                                + "\n\nOpen it now?",
                        "Success", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (open == JOptionPane.YES_OPTION) {
                    try { new ProcessBuilder("cmd", "/c", "start", "",
                            finalOutF.getAbsolutePath()).start(); }
                    catch (Exception ex) {
                        JOptionPane.showMessageDialog(ImageToPdfMerger.this,
                                "Saved but could not open automatically.\nPath:\n"
                                        + finalOutF.getAbsolutePath(),
                                "Saved", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        };
        worker.execute();
    }

    // ── addImageToPdf ─────────────────────────────────────────────────────────
    private void addImageToPdf(PDDocument doc, Path imagePath, String qualityKey)
            throws Exception {
        String  name   = imagePath.getFileName().toString().toLowerCase();
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
            default                     -> 1.0f;
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

    // ── Sort helpers ──────────────────────────────────────────────────────────

    private record DateComponents(int year, int month, int day,
                                  int hour, int minute, int second)
            implements Comparable<DateComponents> {
        @Override
        public int compareTo(DateComponents o) {
            int c;
            if ((c = Integer.compare(this.year,   o.year))   != 0) return c;
            if ((c = Integer.compare(this.month,  o.month))  != 0) return c;
            if ((c = Integer.compare(this.day,    o.day))    != 0) return c;
            if ((c = Integer.compare(this.hour,   o.hour))   != 0) return c;
            if ((c = Integer.compare(this.minute, o.minute)) != 0) return c;
            return Integer.compare(this.second, o.second);
        }
        static final DateComponents MISSING = new DateComponents(-1,-1,-1,-1,-1,-1);
    }

    private static DateComponents parseDateComponents(String filename) {
        Matcher m1 = Pattern.compile(
                "(\\d{4})-(\\d{2})-(\\d{2})T(\\d{2}):(\\d{2}):(\\d{2})").matcher(filename);
        if (m1.find()) {
            try { return new DateComponents(
                    Integer.parseInt(m1.group(1)), Integer.parseInt(m1.group(2)),
                    Integer.parseInt(m1.group(3)), Integer.parseInt(m1.group(4)),
                    Integer.parseInt(m1.group(5)), Integer.parseInt(m1.group(6)));
            } catch (Exception ignored) {}
        }
        Matcher m2 = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})").matcher(filename);
        if (m2.find()) {
            try { return new DateComponents(
                    Integer.parseInt(m2.group(1)), Integer.parseInt(m2.group(2)),
                    Integer.parseInt(m2.group(3)), -1, -1, -1);
            } catch (Exception ignored) {}
        }
        Matcher m3 = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})").matcher(filename);
        if (m3.find()) {
            try {
                int y = Integer.parseInt(m3.group(1));
                if (y >= 1900 && y <= 2099)
                    return new DateComponents(y, Integer.parseInt(m3.group(2)),
                            Integer.parseInt(m3.group(3)), -1, -1, -1);
            } catch (Exception ignored) {}
        }
        return DateComponents.MISSING;
    }

    private static long extractTrailingNumber(String filename) {
        int dot = filename.lastIndexOf('.');
        String base = (dot >= 0) ? filename.substring(0, dot) : filename;
        int end = base.length() - 1;
        while (end >= 0 && !Character.isDigit(base.charAt(end))) end--;
        if (end < 0) return Long.MAX_VALUE;
        int start = end;
        while (start > 0 && Character.isDigit(base.charAt(start - 1))) start--;
        try { return Long.parseLong(base.substring(start, end + 1)); }
        catch (NumberFormatException e) { return Long.MAX_VALUE; }
    }

    private static long extractLeadingNumber(String filename) {
        StringBuilder sb = new StringBuilder(); boolean found = false;
        for (char c : filename.toCharArray()) {
            if (Character.isDigit(c)) { sb.append(c); found = true; }
            else if (found) { break; }
        }
        if (!found) return Long.MAX_VALUE;
        try { return Long.parseLong(sb.toString()); }
        catch (NumberFormatException e) { return Long.MAX_VALUE; }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private record FolderScope(File folder, List<Path> images, String pdfName) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageToPdfMerger().setVisible(true));
    }
}
