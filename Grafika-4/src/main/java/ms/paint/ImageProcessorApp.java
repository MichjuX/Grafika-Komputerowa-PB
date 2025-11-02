package ms.paint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.IIOImage;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class ImageProcessorApp extends JFrame {

    private BufferedImage originalImage;
    private BufferedImage processedImage;
    private JLabel imageLabel;
    private JScrollPane scrollPane;
    private JPanel mainControlPanel;

    // Kontrolki dla sekcji A i eksportu
    private JTextField valueField;
    private JTextField factorField;
    private JSlider compressionSlider;
    private JLabel qualityLabel;

    // Kontrolka dla funkcji zoom
    private double zoomFactor = 1.0;

    public ImageProcessorApp() {
        setTitle("Przetwarzanie Obrazów - Projekt (Finalny)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        imageLabel = new JLabel("Brak wczytanego obrazu.");
        imageLabel.setHorizontalAlignment(JLabel.CENTER);
        scrollPane = new JScrollPane(imageLabel);
        add(scrollPane, BorderLayout.CENTER);

        mainControlPanel = new JPanel();
        mainControlPanel.setLayout(new BoxLayout(mainControlPanel, BoxLayout.X_AXIS));
        mainControlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(mainControlPanel, BorderLayout.NORTH);

        addLoadControls();
        addPointTransformationControls();
        addConvolutionFilterControls();
        addExportControls();
        addZoomControls(); // Nowa sekcja Zoom

        pack();
        setSize(1400, 900);
        setLocationRelativeTo(null);
    }

    // --- Metody pomocnicze GUI i Logika Obrazu ---

    private JPanel createTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private void addLoadControls() {
        JPanel loadPanel = createTitledPanel("Wczytywanie");
        JButton loadButton = new JButton("Wczytaj Obraz");
        loadButton.addActionListener(e -> loadImage());
        loadPanel.add(loadButton);

        JButton resetButton = new JButton("Resetuj");
        resetButton.addActionListener(e -> {
            if (originalImage != null) {
                processedImage = copyImage(originalImage);
                zoomFactor = 1.0; // Reset zoomu przy resecie obrazu
                displayImage(processedImage);
            } else {
                JOptionPane.showMessageDialog(this, "Brak oryginalnego obrazu do resetowania.", "Informacja", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        loadPanel.add(resetButton);

        mainControlPanel.add(loadPanel);
        mainControlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
    }

    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                originalImage = ImageIO.read(selectedFile);
                processedImage = copyImage(originalImage);
                zoomFactor = 1.0; // Reset zoomu przy ładowaniu
                displayImage(processedImage);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Błąd wczytywania obrazu: " + e.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // *** ZMODYFIKOWANA metoda displayImage - obsługa zoomu ***
    private void displayImage(BufferedImage image) {
        if (image != null) {
            int newWidth = (int) (image.getWidth() * zoomFactor);
            int newHeight = (int) (image.getHeight() * zoomFactor);

            // Skalowanie obrazu dla wyświetlenia
            Image scaledImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);

            ImageIcon icon = new ImageIcon(scaledImage);
            imageLabel.setIcon(icon);
            imageLabel.setText("");

            // Wymuszanie odświeżenia ScrollPane i JLabel
            imageLabel.setPreferredSize(new Dimension(newWidth, newHeight));
            imageLabel.revalidate();
            imageLabel.repaint();
            scrollPane.revalidate();
        } else {
            imageLabel.setIcon(null);
            imageLabel.setText("Brak wczytanego obrazu.");
        }
    }

    private BufferedImage copyImage(BufferedImage source) {
        if (source == null) return null;
        int type = source.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : source.getType();
        BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), type);
        Graphics g = b.getGraphics();
        g.drawImage(source, 0, 0, null);
        g.dispose();
        return b;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ImageProcessorApp().setVisible(true);
        });
    }

    // ************************************************************
    // *** OBSŁUGA KRAWĘDZI (Replicate Padding) ***
    // ************************************************************

    private int clampCoordinate(int coord, int max) {
        return Math.max(0, Math.min(max - 1, coord));
    }

    private int getSafeRGB(BufferedImage image, int x, int y) {
        int w = image.getWidth();
        int h = image.getHeight();
        int safeX = clampCoordinate(x, w);
        int safeY = clampCoordinate(y, h);
        return image.getRGB(safeX, safeY);
    }

    // ************************************************************
    // *** SEKCJA ZOOM ***
    // ************************************************************

    private void addZoomControls() {
        JPanel zoomPanel = createTitledPanel("Zoom");

        JButton zoomInButton = new JButton("+");
        zoomInButton.addActionListener(e -> changeZoom(1.25));
        zoomPanel.add(zoomInButton);

        JButton zoomOutButton = new JButton("-");
        zoomOutButton.addActionListener(e -> changeZoom(1 / 1.25));
        zoomPanel.add(zoomOutButton);

        JButton resetZoomButton = new JButton("100%");
        resetZoomButton.addActionListener(e -> changeZoom(1.0));
        zoomPanel.add(resetZoomButton);

        mainControlPanel.add(zoomPanel);
        mainControlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
    }

    private void changeZoom(double factor) {
        if (processedImage == null) {
            JOptionPane.showMessageDialog(this, "Wczytaj obraz, aby użyć funkcji zoom.", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (factor == 1.0) {
            zoomFactor = 1.0;
        } else if (factor > 1.0) {
            zoomFactor *= factor;
        } else {
            zoomFactor *= factor;
        }

        // Ograniczenia zoomu
        if (zoomFactor < 0.1) {
            zoomFactor = 0.1;
        }
        if (zoomFactor > 8.0) {
            zoomFactor = 8.0;
        }

        displayImage(processedImage);
    }


    // ************************************************************
    // *** SEKCJA A: PRZEKSZTAŁCENIA PUNKTOWE ***
    // ************************************************************

    private void addPointTransformationControls() {
        JPanel pointPanel = createTitledPanel("a. Przekształcenia Punktowe");
        valueField = new JTextField("50", 4);
        pointPanel.add(new JLabel("Wartość (Int):"));
        pointPanel.add(valueField);

        factorField = new JTextField("1.5", 4);
        pointPanel.add(new JLabel("Mnożnik/Dzielnik (Float):"));
        pointPanel.add(factorField);

        String[] pointOps = {
                "Wybierz Operację...", "Dodawanie (+Int)", "Odejmowanie (-Int)", "Mnożenie (*Float)",
                "Dzielenie (/Float)", "Zmiana Jasności (+/-Int)",
                "Skala Szarości (Średnia)", "Skala Szarości (Ważona)"
        };

        JComboBox<String> opComboBox = new JComboBox<>(pointOps);

        opComboBox.addActionListener(e -> {
            String selectedOp = (String) opComboBox.getSelectedItem();
            if (originalImage == null && !selectedOp.equals("Wybierz Operację...")) {
                JOptionPane.showMessageDialog(ImageProcessorApp.this, "Wczytaj obraz przed operacją.", "Błąd", JOptionPane.WARNING_MESSAGE);
                opComboBox.setSelectedIndex(0);
                return;
            }
            switch (selectedOp) {
                case "Dodawanie (+Int)": applyPointOp(valueField, this::addValue); break;
                case "Odejmowanie (-Int)": applyPointOp(valueField, this::subtractValue); break;
                case "Mnożenie (*Float)": applyPointOpFloat(factorField, this::multiplyValue); break;
                case "Dzielenie (/Float)": applyPointOpFloat(factorField, this::divideValue); break;
                case "Zmiana Jasności (+/-Int)": applyPointOp(valueField, this::changeBrightness); break;
                case "Skala Szarości (Średnia)": applyGrayscale(this::toGrayscaleAverage); break;
                case "Skala Szarości (Ważona)": applyGrayscale(this::toGrayscaleWeighted); break;
            }
            opComboBox.setSelectedIndex(0);
        });
        pointPanel.add(new JLabel("Operacja:"));
        pointPanel.add(opComboBox);
        mainControlPanel.add(pointPanel);
        mainControlPanel.add(Box.createRigidArea(new Dimension(10, 0)));
    }

    @FunctionalInterface interface PointOperation { int apply(int colorValue, int value); }
    @FunctionalInterface interface PointOperationFloat { int apply(int colorValue, float value); }
    @FunctionalInterface interface GrayscaleOperation { int apply(int rgb); }

    private void applyPointOp(JTextField field, PointOperation operation) {
        if (processedImage == null) return;
        try {
            int value = Integer.parseInt(field.getText());
            BufferedImage source = processedImage;
            BufferedImage resultImage = copyImage(source);
            int w = resultImage.getWidth(); int h = resultImage.getHeight();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = source.getRGB(x, y);
                    int a = (rgb >> 24) & 0xFF; int r = (rgb >> 16) & 0xFF; int g = (rgb >> 8) & 0xFF; int b = rgb & 0xFF;
                    r = operation.apply(r, value); g = operation.apply(g, value); b = operation.apply(b, value);
                    int newRgb = (a << 24) | (r << 16) | (g << 8) | b;
                    resultImage.setRGB(x, y, newRgb);
                }
            }
            processedImage = resultImage; displayImage(processedImage);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Wprowadź poprawną wartość całkowitą dla operacji.", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyPointOpFloat(JTextField field, PointOperationFloat operation) {
        if (processedImage == null) return;
        try {
            float value = Float.parseFloat(field.getText());
            BufferedImage source = processedImage;
            BufferedImage resultImage = copyImage(source);
            int w = resultImage.getWidth(); int h = resultImage.getHeight();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int rgb = source.getRGB(x, y);
                    int a = (rgb >> 24) & 0xFF; int r = (rgb >> 16) & 0xFF; int g = (rgb >> 8) & 0xFF; int b = rgb & 0xFF;
                    r = operation.apply(r, value); g = operation.apply(g, value); b = operation.apply(b, value);
                    int newRgb = (a << 24) | (r << 16) | (g << 8) | b;
                    resultImage.setRGB(x, y, newRgb);
                }
            }
            processedImage = resultImage; displayImage(processedImage);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Wprowadź poprawną wartość zmiennoprzecinkową dla operacji.", "Błąd", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int clamp(int value) { return Math.max(0, Math.min(255, value)); }
    private int addValue(int colorValue, int value) { return clamp(colorValue + value); }
    private int subtractValue(int colorValue, int value) { return clamp(colorValue - value); }
    private int multiplyValue(int colorValue, float factor) { return clamp((int) (colorValue * factor)); }
    private int divideValue(int colorValue, float divisor) { return (divisor == 0) ? 0 : clamp((int) (colorValue / divisor)); }
    private int changeBrightness(int colorValue, int level) { return clamp(colorValue + level); }

    private void applyGrayscale(GrayscaleOperation operation) {
        if (processedImage == null) return;
        BufferedImage source = processedImage;
        BufferedImage resultImage = copyImage(source);
        int w = resultImage.getWidth(); int h = resultImage.getHeight();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = source.getRGB(x, y);
                int grayValue = operation.apply(rgb);
                int a = (rgb >> 24) & 0xFF;
                int newRgb = (a << 24) | (grayValue << 16) | (grayValue << 8) | grayValue;
                resultImage.setRGB(x, y, newRgb);
            }
        }
        processedImage = resultImage; displayImage(processedImage);
    }

    private int toGrayscaleAverage(int rgb) {
        int r = (rgb >> 16) & 0xFF; int g = (rgb >> 8) & 0xFF; int b = rgb & 0xFF; return (r + g + b) / 3;
    }
    private int toGrayscaleWeighted(int rgb) {
        int r = (rgb >> 16) & 0xFF; int g = (rgb >> 8) & 0xFF; int b = rgb & 0xFF; return (int) (0.2126 * r + 0.7152 * g + 0.0722 * b);
    }

    // ************************************************************
    // *** SEKCJA B: FILTRY KONWOLUCYJNE ***
    // ************************************************************

    private static final String CUSTOM_MASK_ACTION = "Własna Maska (Opcjonalne)";

    private void addConvolutionFilterControls() {
        JPanel filterPanel = createTitledPanel("b. Filtry Konwolucyjne");
        String[] filterNames = {
                "Wybierz Filtr...", "Wygładzający (Średnia)", "Filtr Medianowy (3x3)",
                "Wykrywanie Krawędzi (Sobel)", "Wyostrzający (Górnoprzepustowy)", "Rozmycie Gaussa", CUSTOM_MASK_ACTION
        };
        JComboBox<String> filterComboBox = new JComboBox<>(filterNames);
        filterComboBox.addActionListener(e -> {
            String selectedFilter = (String) filterComboBox.getSelectedItem();
            if (originalImage == null && !selectedFilter.equals("Wybierz Filtr...")) {
                JOptionPane.showMessageDialog(ImageProcessorApp.this, "Wczytaj obraz przed aplikacją filtru.", "Błąd", JOptionPane.WARNING_MESSAGE);
                filterComboBox.setSelectedIndex(0);
                return;
            }
            switch (selectedFilter) {
                case "Wygładzający (Średnia)": applyFilter(FilterMasks.SMOOTHING_3X3); break;
                case "Filtr Medianowy (3x3)": applyMedianFilter(3); break;
                case "Wykrywanie Krawędzi (Sobel)": applySobelFilter(); break;
                case "Wyostrzający (Górnoprzepustowy)": applyFilter(FilterMasks.SHARPENING_3X3); break;
                case "Rozmycie Gaussa": applyFilter(FilterMasks.GAUSSIAN_3X3_UNNORM); break;
                case CUSTOM_MASK_ACTION: showCustomMaskDialog(); break;
            }
            if (!selectedFilter.equals(CUSTOM_MASK_ACTION)) { filterComboBox.setSelectedIndex(0); }
        });
        filterPanel.add(new JLabel("Filtr:"));
        filterPanel.add(filterComboBox);
        mainControlPanel.add(filterPanel);
    }

    private static class FilterMasks {
        public static final float[][] SMOOTHING_3X3 = { {1, 1, 1}, {1, 1, 1}, {1, 1, 1} };
        public static final float[][] SHARPENING_3X3 = { {-1, -1, -1}, {-1, 9, -1}, {-1, -1, -1} };
        public static final float[][] GAUSSIAN_3X3_UNNORM = { {1, 2, 1}, {2, 4, 2}, {1, 2, 1} };
        public static final float[][] SOBEL_V = { {-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1} };
        public static final float[][] SOBEL_H = { {1, 2, 1}, {0, 0, 0}, {-1, -2, -1} };
    }

    private void applyFilter(float[][] kernel) {
        if (processedImage == null) return;
        BufferedImage source = processedImage;
        BufferedImage resultImage = copyImage(source);
        int kWidth = kernel[0].length; int kHeight = kernel.length;
        int halfKWidth = kWidth / 2; int halfKHeight = kHeight / 2;
        int w = source.getWidth(); int h = source.getHeight();

        float sumOfWeights = 0;
        for (float[] row : kernel) { for (float weight : row) { sumOfWeights += weight; } }
        float finalDivisor = (Math.abs(sumOfWeights) < 0.001f) ? 1.0f : sumOfWeights;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float rSum = 0; float gSum = 0; float bSum = 0;
                for (int ky = 0; ky < kHeight; ky++) {
                    for (int kx = 0; kx < kWidth; kx++) {
                        int pixelX = x + kx - halfKWidth;
                        int pixelY = y + ky - halfKHeight;
                        int rgb = getSafeRGB(source, pixelX, pixelY); // Bezpieczny odczyt z krawędzi

                        int r = (rgb >> 16) & 0xFF; int g = (rgb >> 8) & 0xFF; int b = rgb & 0xFF;
                        float weight = kernel[ky][kx];
                        rSum += r * weight; gSum += g * weight; bSum += b * weight;
                    }
                }

                int newR = clamp((int) (rSum / finalDivisor));
                int newG = clamp((int) (gSum / finalDivisor));
                int newB = clamp((int) (bSum / finalDivisor));

                int a = (source.getRGB(x, y) >> 24) & 0xFF;
                int newRgb = (a << 24) | (newR << 16) | (newG << 8) | newB;
                resultImage.setRGB(x, y, newRgb);
            }
        }
        processedImage = resultImage; displayImage(processedImage);
    }

    private void applySobelFilter() {
        if (processedImage == null) return;
        BufferedImage source = processedImage;
        BufferedImage grayImage = copyImage(source);
        int w = grayImage.getWidth(); int h = grayImage.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = grayImage.getRGB(x, y);
                int grayValue = toGrayscaleWeighted(rgb);
                int a = (rgb >> 24) & 0xFF;
                int newRgb = (a << 24) | (grayValue << 16) | (grayValue << 8) | grayValue;
                grayImage.setRGB(x, y, newRgb);
            }
        }

        BufferedImage resultImage = copyImage(grayImage);
        float[][] kx = FilterMasks.SOBEL_V; float[][] ky = FilterMasks.SOBEL_H;
        int kSize = 3; int halfK = kSize / 2;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                float gx = 0; float gy = 0;
                for (int ky_i = 0; ky_i < kSize; ky_i++) {
                    for (int kx_i = 0; kx_i < kSize; kx_i++) {
                        int pixelX = x + kx_i - halfK;
                        int pixelY = y + ky_i - halfK;

                        int rgb = getSafeRGB(grayImage, pixelX, pixelY); // Bezpieczny odczyt z krawędzi

                        int grayValue = (rgb >> 16) & 0xFF;
                        gx += grayValue * kx[ky_i][kx_i];
                        gy += grayValue * ky[ky_i][kx_i];
                    }
                }
                int magnitude = (int) Math.sqrt(gx * gx + gy * gy);
                int finalValue = clamp(magnitude);
                int a = (grayImage.getRGB(x, y) >> 24) & 0xFF;
                int newRgb = (a << 24) | (finalValue << 16) | (finalValue << 8) | finalValue;
                resultImage.setRGB(x, y, newRgb);
            }
        }
        processedImage = resultImage; displayImage(processedImage);
    }

    private void applyMedianFilter(int size) {
        if (processedImage == null) return;
        BufferedImage source = processedImage;
        BufferedImage resultImage = copyImage(source);
        int halfSize = size / 2;
        int w = source.getWidth(); int h = source.getHeight();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                List<Integer> rValues = new ArrayList<>();
                List<Integer> gValues = new ArrayList<>();
                List<Integer> bValues = new ArrayList<>();

                for (int ky = 0; ky < size; ky++) {
                    for (int kx = 0; kx < size; kx++) {
                        int pixelX = x + kx - halfSize;
                        int pixelY = y + ky - halfSize;

                        int rgb = getSafeRGB(source, pixelX, pixelY); // Bezpieczny odczyt z krawędzi

                        rValues.add((rgb >> 16) & 0xFF);
                        gValues.add((rgb >> 8) & 0xFF);
                        bValues.add(rgb & 0xFF);
                    }
                }

                Collections.sort(rValues);
                Collections.sort(gValues);
                Collections.sort(bValues);

                int medianIndex = rValues.size() / 2;
                int newR = rValues.get(medianIndex);
                int newG = gValues.get(medianIndex);
                int newB = bValues.get(medianIndex);

                int a = (source.getRGB(x, y) >> 24) & 0xFF;
                int newRgb = (a << 24) | (newR << 16) | (newG << 8) | newB;
                resultImage.setRGB(x, y, newRgb);
            }
        }
        processedImage = resultImage; displayImage(processedImage);
    }

    private void showCustomMaskDialog() {
        if (processedImage == null) {
            JOptionPane.showMessageDialog(this, "Wczytaj obraz przed definiowaniem maski.", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String sizeInput = JOptionPane.showInputDialog(this,
                "Wprowadź nieparzysty rozmiar maski (np. 3, 5, 7):",
                "Rozmiar Własnej Maski", JOptionPane.PLAIN_MESSAGE);

        if (sizeInput == null || sizeInput.trim().isEmpty()) return;
        int size;
        try {
            size = Integer.parseInt(sizeInput.trim());
            if (size <= 1 || size % 2 == 0) {
                JOptionPane.showMessageDialog(this, "Rozmiar musi być nieparzysty i większy niż 1.", "Błąd Rozmiaru", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Niepoprawny format rozmiaru.", "Błąd", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JPanel inputPanel = new JPanel(new GridLayout(size, size, 5, 5));
        JTextField[][] fields = new JTextField[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                JTextField field = new JTextField("0", 4);
                if (i == size / 2 && j == size / 2) {
                    field.setText("1");
                }
                fields[i][j] = field;
                inputPanel.add(field);
            }
        }

        JScrollPane scrollPane = new JScrollPane(inputPanel);
        scrollPane.setPreferredSize(new Dimension(size * 60, Math.min(size * 50, 400)));

        int option = JOptionPane.showConfirmDialog(this, scrollPane,
                "Wprowadź wartości dla maski " + size + "x" + size + ":",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            float[][] customKernel = new float[size][size];
            try {
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        customKernel[i][j] = Float.parseFloat(fields[i][j].getText().trim());
                    }
                }
                applyFilter(customKernel);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Błąd parsowania: Wszystkie pola muszą zawierać liczby (całkowite lub zmiennoprzecinkowe).", "Błąd", JOptionPane.ERROR_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Wystąpił nieoczekiwany błąd: " + e.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ************************************************************
    // *** SEKCJA EKSPORTU: Kompresja JPG ***
    // ************************************************************

    private void addExportControls() {
        JPanel exportPanel = createTitledPanel("Eksport (JPG)");
        exportPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        compressionSlider = new JSlider(JSlider.HORIZONTAL, 0, 100, 90);
        compressionSlider.setMajorTickSpacing(25);
        compressionSlider.setMinorTickSpacing(5);
        compressionSlider.setPaintTicks(true);
        compressionSlider.setPaintLabels(true);

        qualityLabel = new JLabel("Jakość: 90%");

        compressionSlider.addChangeListener(e -> {
            qualityLabel.setText("Jakość: " + compressionSlider.getValue() + "%");
        });

        exportPanel.add(new JLabel("Kompresja (0-100):"));
        exportPanel.add(compressionSlider);
        exportPanel.add(qualityLabel);

        JButton exportButton = new JButton("Eksportuj do JPG");
        exportButton.addActionListener(e -> exportImageAsJpeg());
        exportPanel.add(exportButton);

        mainControlPanel.add(exportPanel);
    }

    private void exportImageAsJpeg() {
        if (processedImage == null) {
            JOptionPane.showMessageDialog(this, "Brak przetworzonego obrazu do eksportu.", "Błąd", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("processed_image.jpg"));
        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            if (!fileToSave.getName().toLowerCase().endsWith(".jpg")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".jpg");
            }

            float quality = compressionSlider.getValue() / 100.0f;

            try {
                Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
                if (!writers.hasNext()) {
                    throw new IOException("Brak dostępnego ImageWritera dla formatu JPEG.");
                }
                ImageWriter writer = writers.next();

                ImageWriteParam param = writer.getDefaultWriteParam();
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(quality);

                try (FileImageOutputStream output = new FileImageOutputStream(fileToSave)) {
                    writer.setOutput(output);
                    // Sprawdzamy, czy obraz ma odpowiedni typ dla zapisu JPG (bez kanału Alpha)
                    BufferedImage imageForExport = processedImage;
                    if (imageForExport.getType() == BufferedImage.TYPE_INT_ARGB) {
                        // Konwersja na typ RGB, jeśli jest ARGB
                        imageForExport = new BufferedImage(imageForExport.getWidth(), imageForExport.getHeight(), BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2d = imageForExport.createGraphics();
                        g2d.drawImage(processedImage, 0, 0, null);
                        g2d.dispose();
                    }

                    writer.write(null, new IIOImage(imageForExport, null, null), param);
                }
                writer.dispose();

                JOptionPane.showMessageDialog(this, "Obraz zapisano pomyślnie jako: " + fileToSave.getName() +
                        " z jakością: " + (int)(quality * 100) + "%", "Sukces", JOptionPane.INFORMATION_MESSAGE);

            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Błąd zapisu pliku: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}