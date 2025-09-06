import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Atbash Cipher를 위한 간단한 GUI 툴입니다.
 * 사용자는 텍스트를 입력하고 버튼을 눌러 암호화/복호화된 결과를 볼 수 있습니다.
 */
public class AtbashCipherGUI extends JFrame {

    private enum Theme {
        LIGHT(
                new Color(245, 247, 249), // background
                Color.WHITE,              // textAreaBg
                new Color(235, 237, 239), // outputTextAreaBg
                new Color(50, 50, 50),    // foreground
                new Color(0, 123, 255),   // primaryButton
                new Color(108, 117, 125), // secondaryButton
                new Color(220, 223, 226)  // border
        ),
        DARK(
                new Color(43, 43, 43),    // background
                new Color(60, 63, 65),    // textAreaBg
                new Color(50, 52, 54),    // outputTextAreaBg
                new Color(220, 220, 220), // foreground
                new Color(2, 117, 216),   // primaryButton
                new Color(90, 98, 104),   // secondaryButton
                new Color(80, 80, 80)     // border
        );

        final Color background;
        final Color textAreaBg;
        final Color outputTextAreaBg;
        final Color foreground;
        final Color primaryButton;
        final Color secondaryButton;
        final Color border;

        Theme(Color background, Color textAreaBg, Color outputTextAreaBg, Color foreground, Color primaryButton, Color secondaryButton, Color border) {
            this.background = background;
            this.textAreaBg = textAreaBg;
            this.outputTextAreaBg = outputTextAreaBg;
            this.foreground = foreground;
            this.primaryButton = primaryButton;
            this.secondaryButton = secondaryButton;
            this.border = border;
        }
    }

    private enum ThemeMode {
        LIGHT, DARK
    }

    private enum Language {
        KOREAN, ENGLISH
    }

    private enum UIText {
        WINDOW_TITLE("아트배쉬 암호화/복호화 툴 V2", "ArtBash Encryption/Decryption Tool V2"),
        FILE_MENU("파일", "File"),
        SAVE_MENU("저장...", "Save As..."),
        LOAD_MENU("불러오기...", "Open..."),
        THEME_MENU("테마", "Theme"),
        LIGHT_MODE_MENU("라이트 모드", "Light Mode"),
        DARK_MODE_MENU("다크 모드", "Dark Mode"),
        LANGUAGE_MENU("언어", "Language"),
        KOREAN_MENU("한국어", "한국어"),
        ENGLISH_MENU("English", "English"),
        INPUT_LABEL("원본 텍스트", "Input Text"),
        OUTPUT_LABEL("변환된 텍스트", "Output Text"),
        TRANSFORM_BUTTON("변환", "Transform"),
        CLEAR_BUTTON("초기화", "Clear"),
        COPY_BUTTON("복사", "Copy"),
        COPY_FEEDBACK("복사됨", "Copied");

        private final String korean;
        private final String english;

        UIText(String korean, String english) {
            this.korean = korean;
            this.english = english;
        }

        public String get(Language lang) {
            return lang == Language.KOREAN ? korean : english;
        }
    }

    private JTextArea inputTextArea;
    private JTextArea outputTextArea;
    private JButton transformButton;
    private JButton clearButton;
    private JButton copyButton;
    private JLabel feedbackLabel;
    private MenuIcon saveIcon;
    private MenuIcon loadIcon;
    private Timer feedbackTimer;

    private JMenuBar menuBar;
    private JPanel mainPanel;
    private JPanel buttonPanel;
    private JLabel inputLabel;
    private JLabel outputLabel;
    // Use custom RoundedScrollPane for rounded text areas
    private RoundedScrollPane inputScrollPane;
    private RoundedScrollPane outputScrollPane;
    private JMenu fileMenu;
    private JMenuItem saveMenuItem;
    private JMenuItem loadMenuItem;
    private JMenu themeMenu;
    private JRadioButtonMenuItem lightMenuItem;
    private JRadioButtonMenuItem darkMenuItem;
    private JMenu languageMenu;
    private JRadioButtonMenuItem koreanMenuItem;
    private JRadioButtonMenuItem englishMenuItem;
    private Language currentLanguage = Language.KOREAN;

    public AtbashCipherGUI() {
        initFrame();
        initComponents();
        initLayout();
        initMenu();
        initListeners();
        setTheme(ThemeMode.LIGHT);
        setLanguage(Language.KOREAN);
    }

    /**
     * A custom JButton with rounded corners and a hover effect.
     */
    private static class RoundedButton extends JButton {
        private final int cornerRadius;

        public RoundedButton(String text, int radius) {
            super(text);
            this.cornerRadius = radius;
            // We paint our own background, so disable the default painting
            setContentAreaFilled(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            boolean isPressed = getModel().isArmed();
            int yOffset = isPressed ? 1 : 0;

            // Draw the darker "bottom edge" layer.
            g2.setColor(getBackground().darker());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

            // Determine the color for the top layer
            Color topColor;
            if (getModel().isRollover() && !isPressed) {
                topColor = getBackground().brighter();
            } else {
                topColor = getBackground();
            }

            // Draw the main, top layer of the button.
            // When not pressed, it's 1px shorter, revealing the bottom edge.
            // When pressed, it's shifted down 1px and covers the edge.
            g2.setColor(topColor);
            g2.fillRoundRect(0, yOffset, getWidth(), getHeight() - 1 - yOffset, cornerRadius, cornerRadius);

            // Let the superclass paint the text
            super.paintComponent(g);
            g2.dispose();
        }
    }

    /**
     * A custom JScrollPane with rounded corners.
     * The view component (e.g., JTextArea) should be set to non-opaque for this to work correctly.
     */
    private static class RoundedScrollPane extends JScrollPane {
        private final int cornerRadius;
        private Color borderColor;

        public RoundedScrollPane(Component view, int radius) {
            super(view);
            this.cornerRadius = radius;
            // Make the scroll pane and its viewport transparent so we can paint our own background
            setOpaque(false);
            getViewport().setOpaque(false);
            // Remove the default border, we'll paint our own
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Fill background with a semi-transparent color from the theme
            g2.setColor(getViewport().getView().getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

            // 2. Let the original scrollpane paint its content (the text area)
            super.paintComponent(g);

            // 3. Paint a slightly darker border
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, cornerRadius, cornerRadius);

            g2.dispose();
        }

        public void setBorderColor(Color color) {
            this.borderColor = color;
            repaint();
        }
    }

    /**
     * A custom Icon implementation to draw menu icons programmatically.
     */
    private static class MenuIcon implements Icon {
        enum IconType { SAVE, LOAD }

        private final IconType type;
        private Color color = Color.BLACK;
        private static final int ICON_SIZE = 16;

        public MenuIcon(IconType type) {
            this.type = type;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.color);
            g2.setStroke(new BasicStroke(1.5f));

            if (type == IconType.SAVE) {
                // Downward arrow into a tray
                g2.drawLine(x + 8, y + 2, x + 8, y + 10); // Arrow shaft
                g2.drawLine(x + 5, y + 7, x + 8, y + 10); // Arrow left head
                g2.drawLine(x + 11, y + 7, x + 8, y + 10); // Arrow right head
                // Tray
                g2.drawLine(x + 2, y + 13, x + 14, y + 13); // Bottom
                g2.drawLine(x + 2, y + 10, x + 2, y + 13); // Left side
                g2.drawLine(x + 14, y + 10, x + 14, y + 13); // Right side
            } else if (type == IconType.LOAD) {
                // Simple folder icon
                g2.drawRect(x + 1, y + 3, ICON_SIZE - 3, ICON_SIZE - 5); // Folder back
                g2.drawPolyline(new int[]{x, x, x + 5, x + 7, x + ICON_SIZE - 1, x + ICON_SIZE - 1},
                                new int[]{y + ICON_SIZE - 2, y + 5, y + 5, y + 3, y + 3, y + ICON_SIZE - 2}, 6); // Folder front
            }
            g2.dispose();
        }

        @Override public int getIconWidth() { return ICON_SIZE; }
        @Override public int getIconHeight() { return ICON_SIZE; }
    }

    private void initFrame() {
        // Title is set by applyLanguage()
        setSize(600, 500);
        setMinimumSize(new Dimension(450, 350));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 창을 화면 중앙에 배치
    }
 
    private void initMenu() {
        menuBar = new JMenuBar();

        fileMenu = new JMenu();
        saveMenuItem = new JMenuItem();
        saveMenuItem.setIcon(saveIcon);
        loadMenuItem = new JMenuItem();
        loadMenuItem.setIcon(loadIcon);
        fileMenu.add(loadMenuItem);
        fileMenu.add(saveMenuItem);

        themeMenu = new JMenu();
        ButtonGroup themeGroup = new ButtonGroup();

        lightMenuItem = new JRadioButtonMenuItem();
        lightMenuItem.setSelected(true);
        lightMenuItem.addActionListener(e -> setTheme(ThemeMode.LIGHT));

        darkMenuItem = new JRadioButtonMenuItem();
        darkMenuItem.addActionListener(e -> setTheme(ThemeMode.DARK));

        themeGroup.add(lightMenuItem);
        themeGroup.add(darkMenuItem);
        themeMenu.add(lightMenuItem);
        themeMenu.add(darkMenuItem);

        languageMenu = new JMenu();
        ButtonGroup languageGroup = new ButtonGroup();

        koreanMenuItem = new JRadioButtonMenuItem();
        koreanMenuItem.setSelected(true);
        koreanMenuItem.addActionListener(e -> setLanguage(Language.KOREAN));

        englishMenuItem = new JRadioButtonMenuItem();
        englishMenuItem.addActionListener(e -> setLanguage(Language.ENGLISH));

        languageGroup.add(koreanMenuItem);
        languageGroup.add(englishMenuItem);
        languageMenu.add(koreanMenuItem);
        languageMenu.add(englishMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(themeMenu);
        menuBar.add(languageMenu);

        setJMenuBar(menuBar);
    }

    private void setTheme(ThemeMode mode) {
        // Update radio button selection state
        lightMenuItem.setSelected(mode == ThemeMode.LIGHT);
        darkMenuItem.setSelected(mode == ThemeMode.DARK);

        Theme themeToApply = (mode == ThemeMode.DARK) ? Theme.DARK : Theme.LIGHT;
        applyTheme(themeToApply);
    }

    private void setLanguage(Language lang) {
        this.currentLanguage = lang;
        // Update radio button selection state
        koreanMenuItem.setSelected(lang == Language.KOREAN);
        englishMenuItem.setSelected(lang == Language.ENGLISH);

        applyLanguage(lang);
    }

    private void applyLanguage(Language lang) {
        setTitle(UIText.WINDOW_TITLE.get(lang));
        fileMenu.setText(UIText.FILE_MENU.get(lang));
        saveMenuItem.setText(UIText.SAVE_MENU.get(lang));
        loadMenuItem.setText(UIText.LOAD_MENU.get(lang));
        themeMenu.setText(UIText.THEME_MENU.get(lang));
        lightMenuItem.setText(UIText.LIGHT_MODE_MENU.get(lang));
        darkMenuItem.setText(UIText.DARK_MODE_MENU.get(lang));
        languageMenu.setText(UIText.LANGUAGE_MENU.get(lang));
        koreanMenuItem.setText(UIText.KOREAN_MENU.get(lang));
        englishMenuItem.setText(UIText.ENGLISH_MENU.get(lang));
        inputLabel.setText(UIText.INPUT_LABEL.get(lang));
        outputLabel.setText(UIText.OUTPUT_LABEL.get(lang));
        transformButton.setText(UIText.TRANSFORM_BUTTON.get(lang));
        clearButton.setText(UIText.CLEAR_BUTTON.get(lang));
        copyButton.setText(UIText.COPY_BUTTON.get(lang));
        // Clear feedback message on language change to avoid showing it in the wrong language
        if (feedbackLabel != null) {
            feedbackLabel.setText("");
        }
    }

    private void applyTheme(Theme theme) {
        // Main Panel and Frame
        getContentPane().setBackground(theme.background);
        if (mainPanel != null) mainPanel.setBackground(theme.background);
        if (buttonPanel != null) buttonPanel.setBackground(theme.background);

        // Text Areas
        inputTextArea.setBackground(theme.textAreaBg);
        inputTextArea.setForeground(theme.foreground);
        inputTextArea.setCaretColor(theme.foreground);
        outputTextArea.setBackground(theme.outputTextAreaBg);
        outputTextArea.setForeground(theme.foreground);
        if (inputScrollPane != null) inputScrollPane.setBorderColor(theme.border);
        if (outputScrollPane != null) outputScrollPane.setBorderColor(theme.border);

        // Labels
        if (inputLabel != null) inputLabel.setForeground(theme.foreground);
        if (outputLabel != null) outputLabel.setForeground(theme.foreground);
        if (feedbackLabel != null) feedbackLabel.setForeground(theme.primaryButton);

        // Icons
        if (saveIcon != null) saveIcon.setColor(theme.foreground);
        if (loadIcon != null) loadIcon.setColor(theme.foreground);
        // Repaint menu bar to reflect icon color change
        if (menuBar != null) menuBar.repaint();

        // Buttons
        configureFlatButton(transformButton, transformButton.getFont(), theme.primaryButton);
        configureFlatButton(clearButton, clearButton.getFont(), theme.secondaryButton);
        configureFlatButton(copyButton, copyButton.getFont(), theme.secondaryButton);

        // Menu Bar
        if (menuBar != null) menuBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, theme.border));

        SwingUtilities.updateComponentTreeUI(this);
    }

    private void initComponents() {
        // Define a modern font
        Font baseFont = new Font("SansSerif", Font.PLAIN, 14);
 
        // Input Text Area
        inputTextArea = new JTextArea();
        inputTextArea.setFont(baseFont);
        inputTextArea.setLineWrap(true);
        inputTextArea.setWrapStyleWord(true);
        inputTextArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); // Inner padding
        inputTextArea.setOpaque(false); // Crucial for rounded corners to show through
 
        // Output Text Area
        outputTextArea = new JTextArea();
        outputTextArea.setFont(baseFont);
        outputTextArea.setLineWrap(true);
        outputTextArea.setWrapStyleWord(true);
        outputTextArea.setEditable(false);
        outputTextArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8)); // Inner padding
        outputTextArea.setOpaque(false); // Crucial for rounded corners to show through
 
        // Buttons
        transformButton = new RoundedButton("", 15);
        clearButton = new RoundedButton("", 15);
        copyButton = new RoundedButton("", 15);

        // Feedback Label
        feedbackLabel = new JLabel();
        feedbackLabel.setFont(new Font("SansSerif", Font.BOLD, 13));

        // Icons
        saveIcon = new MenuIcon(MenuIcon.IconType.SAVE);
        loadIcon = new MenuIcon(MenuIcon.IconType.LOAD);
    }

    private void configureFlatButton(JButton button, Font font, Color backgroundColor) {
        button.setFont(font);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setMargin(new Insets(8, 20, 8, 20));
    }
 
    private void initLayout() {
        mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Outer padding
        GridBagConstraints gbc = new GridBagConstraints();
 
        // --- Row 0: Input Label ---
        inputLabel = new JLabel();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 5, 5, 0);
        mainPanel.add(inputLabel, gbc);
 
        // --- Row 1: Input TextArea ---
        inputScrollPane = new RoundedScrollPane(inputTextArea, 15);
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        mainPanel.add(inputScrollPane, gbc);
 
        // --- Row 2: Output Label ---
        outputLabel = new JLabel();
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(0, 5, 5, 0);
        mainPanel.add(outputLabel, gbc);
 
        // --- Row 3: Output TextArea ---
        outputScrollPane = new RoundedScrollPane(outputTextArea, 15);
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 15, 0);
        mainPanel.add(outputScrollPane, gbc);
 
        // --- Row 4: Bottom Panel (Feedback + Buttons) ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 0));
        bottomPanel.setOpaque(false); // Inherit background from mainPanel

        feedbackLabel.setHorizontalAlignment(SwingConstants.LEFT);
        feedbackLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0)); // Padding
        bottomPanel.add(feedbackLabel, BorderLayout.CENTER);

        buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false); // Inherit background from mainPanel
        buttonPanel.add(copyButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(transformButton);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);
 
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);
        mainPanel.add(bottomPanel, gbc);
 
        add(mainPanel);
    }
 
    private void initListeners() {
        transformButton.addActionListener(e -> {
            String inputText = inputTextArea.getText();
            String transformedText = AtbashCipher.transform(inputText);
            outputTextArea.setText(transformedText);
        });
 
        clearButton.addActionListener(e -> {
            inputTextArea.setText("");
            outputTextArea.setText("");
        });

       copyButton.addActionListener(e -> {
            String textToCopy = outputTextArea.getText();
            if (textToCopy != null && !textToCopy.isEmpty()) {
                StringSelection stringSelection = new StringSelection(textToCopy);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(stringSelection, null);

                // Show feedback message
                feedbackLabel.setText(UIText.COPY_FEEDBACK.get(currentLanguage));

                // Stop any existing timer to reset the hide delay
                if (feedbackTimer != null && feedbackTimer.isRunning()) {
                    feedbackTimer.stop();
                }

                // Create and start a timer to hide the message after 2 seconds
                feedbackTimer = new Timer(2000, event -> feedbackLabel.setText(""));
                feedbackTimer.setRepeats(false); // Ensure it only runs once
                feedbackTimer.start();
            }
        });

        saveMenuItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(UIText.SAVE_MENU.get(currentLanguage));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));

            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                // Ensure the file has a .txt extension
                if (!fileToSave.getName().toLowerCase().endsWith(".txt")) {
                    fileToSave = new File(fileToSave.getParentFile(), fileToSave.getName() + ".txt");
                }

                try (FileWriter writer = new FileWriter(fileToSave)) {
                    writer.write(outputTextArea.getText());
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error saving file: " + ex.getMessage(),
                            "Save Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        loadMenuItem.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle(UIText.LOAD_MENU.get(currentLanguage));
            fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files (*.txt)", "txt"));

            int userSelection = fileChooser.showOpenDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToLoad = fileChooser.getSelectedFile();
                try {
                    String content = Files.readString(fileToLoad.toPath());
                    inputTextArea.setText(content);
                    // Clear the output area when loading new text
                    outputTextArea.setText("");
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error loading file: " + ex.getMessage(),
                            "Load Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }
 
    /**
     * Main method to run the GUI application.
     */
    public static void main(String[] args) {
        // It's safer to run UI-related tasks on the Event Dispatch Thread (EDT).
        SwingUtilities.invokeLater(() -> {
            try {
                // Use the system's L&F for better integration (e.g., native menu bar on macOS)
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            AtbashCipherGUI frame = new AtbashCipherGUI();
            frame.setVisible(true);
        });
    }
}