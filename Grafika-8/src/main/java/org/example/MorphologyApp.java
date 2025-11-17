package org.example;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;

/**
 * Główna klasa aplikacji GUI do morfologicznego przetwarzania obrazu.
 */
public class MorphologyApp extends JFrame {

    private BufferedImage originalImage;
    private BufferedImage processedImage;

    private JLabel originalImageLabel;
    private JLabel processedImageLabel;

    private JTextArea seTextArea;
    private JSpinner originXSpinner;
    private JSpinner originYSpinner;
    private JSlider thresholdSlider;

    public MorphologyApp() {
        setTitle("Morfologiczne Przetwarzanie Obrazu");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // === Panel Wyświetlania Obrazów ===
        JSplitPane imageSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        imageSplitPane.setResizeWeight(0.5);

        originalImageLabel = new JLabel();
        originalImageLabel.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane originalScrollPane = new JScrollPane(originalImageLabel);
        originalScrollPane.setBorder(new TitledBorder("Oryginał"));
        imageSplitPane.setLeftComponent(originalScrollPane);

        processedImageLabel = new JLabel();
        processedImageLabel.setHorizontalAlignment(JLabel.CENTER);
        JScrollPane processedScrollPane = new JScrollPane(processedImageLabel);
        processedScrollPane.setBorder(new TitledBorder("Przetworzony"));
        imageSplitPane.setRightComponent(processedScrollPane);

        add(imageSplitPane, BorderLayout.CENTER);

        // === Panel Sterowania ===
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
        controlPanel.setPreferredSize(new Dimension(300, 800));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- Panel Elementu Strukturyzującego (SE) ---
        JPanel sePanel = new JPanel(new BorderLayout(5, 5));
        sePanel.setBorder(new TitledBorder("Element Strukturyzujący (SE)"));

        seTextArea = new JTextArea(5, 10);
        seTextArea.setText("1 1 1\n1 1 1\n1 1 1"); // Domyślny SE 3x3
        seTextArea.setToolTipText("Wpisz macierz SE. Użyj '1' (pierwszy plan), '0' (tło), 'X' (ignoruj).");
        sePanel.add(new JScrollPane(seTextArea), BorderLayout.CENTER);

        JPanel originPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        originPanel.add(new JLabel("Początek X:"));
        originXSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
        originPanel.add(originXSpinner);
        originPanel.add(new JLabel("Początek Y:"));
        originYSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
        originPanel.add(originYSpinner);
        sePanel.add(originPanel, BorderLayout.SOUTH);

        controlPanel.add(sePanel);

        // --- Panel Binaryzacji ---
        JPanel binarizePanel = new JPanel(new BorderLayout());
        binarizePanel.setBorder(new TitledBorder("Binaryzacja"));
        thresholdSlider = new JSlider(0, 255, 128);
        thresholdSlider.setMajorTickSpacing(50);
        thresholdSlider.setPaintTicks(true);
        thresholdSlider.setPaintLabels(true);
        binarizePanel.add(thresholdSlider, BorderLayout.CENTER);
        JButton binarizeButton = new JButton("Binaryzuj");
        binarizeButton.addActionListener(this::performBinarization);
        binarizePanel.add(binarizeButton, BorderLayout.SOUTH);

        controlPanel.add(binarizePanel);

        // --- Panel Operacji Morfologicznych ---
        JPanel operationsPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        operationsPanel.setBorder(new TitledBorder("Operacje"));

        String[] operations = {"Dylatacja", "Erozja", "Otwarcie", "Domknięcie", "Pocienianie (Thin)", "Pogrubianie (Thick)"};
        for (String opName : operations) {
            JButton button = new JButton(opName);
            button.addActionListener(this::performOperation);
            operationsPanel.add(button);
        }

        controlPanel.add(operationsPanel);

        // --- Panel Resetowania ---
        JButton resetButton = new JButton("Resetuj obraz");
        resetButton.addActionListener(e -> {
            if (originalImage != null) {
                // Po binaryzacji, resetuj do wersji binarnej, a nie kolorowej
                performBinarization(null);
            }
        });
        controlPanel.add(resetButton);

        add(controlPanel, BorderLayout.WEST);

        // === Pasek Menu ===
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Plik");
        JMenuItem loadItem = new JMenuItem("Wczytaj obraz...");
        loadItem.addActionListener(this::loadImage);
        fileMenu.add(loadItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);
    }

    private void loadImage(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                originalImage = ImageIO.read(file);
                updateImageLabels(originalImage, originalImage);
                // Automatycznie binaryzuj po wczytaniu
                performBinarization(null);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Błąd podczas wczytywania obrazu.", "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performBinarization(ActionEvent e) {
        if (originalImage == null) return;
        int threshold = thresholdSlider.getValue();
        processedImage = MorphologyEngine.binarize(originalImage, threshold);
        updateImageLabels(originalImage, processedImage);
    }

    private void performOperation(ActionEvent e) {
        if (processedImage == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wczytaj i binaryzuj obraz.", "Brak obrazu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 1. Pobierz i sparsuj SE z pola tekstowego
            int[][] se = MorphologyEngine.parseSE(seTextArea.getText());

            // 2. Pobierz początek (origin)
            int originX = (int) originXSpinner.getValue();
            int originY = (int) originYSpinner.getValue();

            // 3. Walidacja początku
            if (originY >= se.length || originX >= se[0].length) {
                JOptionPane.showMessageDialog(this, "Początek (Origin) jest poza zakresem SE!", "Błąd SE", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // 4. Wykonaj operację
            String command = ((JButton) e.getSource()).getText();
            BufferedImage result = null;

            switch (command) {
                case "Dylatacja":
                    result = MorphologyEngine.applyDilation(processedImage, se, originX, originY);
                    break;
                case "Erozja":
                    result = MorphologyEngine.applyErosion(processedImage, se, originX, originY);
                    break;
                case "Otwarcie":
                    result = MorphologyEngine.applyOpening(processedImage, se, originX, originY);
                    break;
                case "Domknięcie":
                    result = MorphologyEngine.applyClosing(processedImage, se, originX, originY);
                    break;
                case "Pocienianie (Thin)":
                    // Do pocieniania/pogrubiania potrzebne jest SE z 0, 1 i X (don't care)
                    // Przykład dla pocieniania: znajdź prawy dolny róg
                    // X 0
                    // 1 0
                    result = MorphologyEngine.applyThinning(processedImage, se, originX, originY);
                    break;
                case "Pogrubianie (Thick)":
                    // Przykład dla pogrubiania: znajdź tło przy lewej krawędzi
                    // 0 1
                    // 0 X
                    result = MorphologyEngine.applyThickening(processedImage, se, originX, originY);
                    break;
            }

            if (result != null) {
                processedImage = result;
                updateImageLabels(null, processedImage); // Nie aktualizuj oryginału
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Błędny format SE! Używaj tylko '1', '0', 'X' i spacji.", "Błąd SE", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Wystąpił błąd: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateImageLabels(BufferedImage original, BufferedImage processed) {
        if (original != null) {
            originalImageLabel.setIcon(new ImageIcon(original));
        }
        if (processed != null) {
            processedImageLabel.setIcon(new ImageIcon(processed));
        }
        this.validate();
        this.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MorphologyApp app = new MorphologyApp();
            app.setLocationRelativeTo(null);
            app.setVisible(true);
        });
    }
}