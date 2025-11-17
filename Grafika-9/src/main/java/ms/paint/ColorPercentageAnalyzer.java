package ms.paint;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.util.concurrent.ExecutionException;

/**
 * Aplikacja w Java Swing do analizy procentowego udziału
 * wybranego koloru na obrazie.
 *
 * Spełnia wymagania:
 * 1. Wczytanie obrazu (przez JFileChooser).
 * 2. Obliczenie procentu (w SwingWorker).
 * 3. Wydajność (obliczenia w tle, wydajny algorytm).
 * 4. Dokładność (obliczenia na 'long' i 'double').
 * 5. Parametryzacja (JColorChooser do wyboru koloru, JSlider do tolerancji).
 */
public class ColorPercentageAnalyzer extends JFrame {

    private JButton loadButton, pickColorButton, calculateButton;
    private JSlider toleranceSlider;
    private JLabel originalImageLabel, processedImageLabel, resultLabel, toleranceLabel;
    private JPanel colorPreviewPanel;

    private BufferedImage originalImage;
    private Color targetColor = Color.GREEN; // Domyślny kolor

    public ColorPercentageAnalyzer() {
        setTitle("Analizator Kolorów Obrazu");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        initComponents();
        addListeners();
    }

    private void initComponents() {
        // --- Panel Sterowania (Góra) ---
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        loadButton = new JButton("Wczytaj Obraz...");
        pickColorButton = new JButton("Wybierz Kolor");

        // Podgląd wybranego koloru
        colorPreviewPanel = new JPanel();
        colorPreviewPanel.setBackground(targetColor);
        colorPreviewPanel.setPreferredSize(new Dimension(25, 25));
        colorPreviewPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // Suwak tolerancji
        toleranceLabel = new JLabel("Tolerancja (0-255): 50");
        toleranceSlider = new JSlider(0, 255, 50);
        toleranceSlider.setPreferredSize(new Dimension(200, 40));
        toleranceSlider.setMajorTickSpacing(50);
        toleranceSlider.setMinorTickSpacing(10);
        toleranceSlider.setPaintTicks(true);
        toleranceSlider.setPaintLabels(true);

        calculateButton = new JButton("Oblicz Procent");

        controlPanel.add(loadButton);
        controlPanel.add(pickColorButton);
        controlPanel.add(colorPreviewPanel);
        controlPanel.add(toleranceLabel);
        controlPanel.add(toleranceSlider);
        controlPanel.add(calculateButton);
        add(controlPanel, BorderLayout.NORTH);

        // --- Panel Obrazów (Centrum) ---
        JPanel imagePanel = new JPanel(new GridLayout(1, 2, 10, 10));
        imagePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        originalImageLabel = new JLabel("Wczytaj obraz...", SwingConstants.CENTER);
        originalImageLabel.setBorder(new TitledBorder("Oryginał"));
        imagePanel.add(new JScrollPane(originalImageLabel));

        processedImageLabel = new JLabel("Czekam na obliczenia...", SwingConstants.CENTER);
        processedImageLabel.setBorder(new TitledBorder("Przetworzony (dopasowane piksele)"));
        imagePanel.add(new JScrollPane(processedImageLabel));

        add(imagePanel, BorderLayout.CENTER);

        // --- Panel Wyników (Dół) ---
        resultLabel = new JLabel("Wynik: Czekam na wczytanie obrazu i obliczenia.");
        resultLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(resultLabel, BorderLayout.SOUTH);
    }

    private void addListeners() {
        // 1. Wczytywanie obrazu
        loadButton.addActionListener(e -> loadImage());

        // 2. Wybór koloru
        pickColorButton.addActionListener(e -> pickColor());

        // 3. Aktualizacja etykiety suwaka
        toleranceSlider.addChangeListener(e -> {
            toleranceLabel.setText(String.format("Tolerancja (0-255): %d", toleranceSlider.getValue()));
        });

        // 4. Rozpoczęcie obliczeń
        calculateButton.addActionListener(e -> startCalculation());
    }

    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Wybierz plik obrazu");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Obrazy (jpg, png, bmp)", "jpg", "png", "bmp"));

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                originalImage = ImageIO.read(file);
                if (originalImage == null) {
                    throw new Exception("Nie można wczytać obrazu.");
                }

                // Wyświetl obraz w panelu
                originalImageLabel.setIcon(new ImageIcon(scaleImage(originalImage, 450)));
                originalImageLabel.setText(null); // Usuń tekst "Wczytaj obraz..."

                // Zresetuj panel przetworzony i wynik
                processedImageLabel.setIcon(null);
                processedImageLabel.setText("Czekam na obliczenia...");
                resultLabel.setText("Wynik: Obraz wczytany. Wybierz kolor i naciśnij 'Oblicz'.");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Błąd podczas wczytywania obrazu: " + ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
                originalImage = null;
            }
        }
    }

    private void pickColor() {
        Color newColor = JColorChooser.showDialog(this, "Wybierz kolor docelowy", targetColor);
        if (newColor != null) {
            targetColor = newColor;
            colorPreviewPanel.setBackground(targetColor);
        }
    }

    private void startCalculation() {
        if (originalImage == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wczytaj obraz!", "Brak Obrazu", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Pobierz parametry przed uruchomieniem wątku
        int tolerance = toleranceSlider.getValue();

        // Zablokuj przyciski i poinformuj o pracy
        calculateButton.setEnabled(false);
        loadButton.setEnabled(false);
        pickColorButton.setEnabled(false);
        resultLabel.setText("Wynik: Przetwarzam... To może chwilę potrwać.");

        // Użyj SwingWorker do wykonania ciężkich obliczeń w tle
        // To jest klucz do "wydajności" - interfejs nie zamarza
        ImageCalculatorTask task = new ImageCalculatorTask(originalImage, targetColor, tolerance);

        task.execute();
    }

    // Klasa wewnętrzna SwingWorker
    // <Wynik, Cząstkowe> - Zwracamy obiekt 'CalculationResult'
    private class ImageCalculatorTask extends SwingWorker<CalculationResult, Void> {

        private BufferedImage image;
        private Color color;
        private int tolerance;
        private long toleranceSq; // Kwadrat tolerancji dla wydajności

        public ImageCalculatorTask(BufferedImage image, Color color, int tolerance) {
            this.image = image;
            this.color = color;
            this.tolerance = tolerance;

            // Obliczamy kwadrat tolerancji raz, aby nie robić tego miliony razy w pętli
            // Używamy odległości euklidesowej (w kwadracie) w przestrzeni RGB
            this.toleranceSq = (long) tolerance * tolerance;
        }

        @Override
        protected CalculationResult doInBackground() throws Exception {
            int width = image.getWidth();
            int height = image.getHeight();
            long totalPixels = (long) width * height;
            long matchCount = 0;

            // Tworzymy nowy obraz do wizualizacji dopasowań
            BufferedImage processed = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

            int targetR = color.getRed();
            int targetG = color.getGreen();
            int targetB = color.getBlue();

            // Iterujemy przez każdy piksel - to jest najbardziej kosztowna operacja
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {

                    int pixel = image.getRGB(x, y);
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >> 8) & 0xFF;
                    int b = pixel & 0xFF;

                    // Obliczanie kwadratu odległości w przestrzeni RGB
                    // (r1-r2)^2 + (g1-g2)^2 + (b1-b2)^2
                    // Jest to znacznie szybsze niż liczenie pierwiastka (Math.sqrt)
                    long distSq = (long) (r - targetR) * (r - targetR) +
                            (long) (g - targetG) * (g - targetG) +
                            (long) (b - targetB) * (b - targetB);

                    // Jeśli kwadrat odległości jest mniejszy lub równy kwadratowi tolerancji,
                    // piksel pasuje.
                    if (distSq <= toleranceSq) {
                        matchCount++;
                        // Zaznacz pasujący piksel na obrazie przetworzonym (na czerwono dla kontrastu)
                        // Można też ustawić oryginalny kolor: processed.setRGB(x, y, pixel);
                        processed.setRGB(x, y, 0xFFFF0000); // Czerwony
                    } else {
                        // Opcjonalnie: ustaw niepasujące piksele na szaro
                        processed.setRGB(x, y, 0xFF404040); // Ciemnoszary
                    }
                }
            }

            double percentage = (double) matchCount / totalPixels * 100.0;

            return new CalculationResult(percentage, processed);
        }

        @Override
        protected void done() {
            try {
                // Pobierz wynik z wątku tła
                CalculationResult result = get();

                // Aktualizuj UI - to już dzieje się w głównym wątku Swing (EDT)
                resultLabel.setText(String.format("Wynik: Znaleziono %d pasujących pikseli (%.2f%%)",
                        (long)(result.percentage / 100.0 * originalImage.getWidth() * originalImage.getHeight()),
                        result.percentage));

                processedImageLabel.setIcon(new ImageIcon(scaleImage(result.processedImage, 450)));
                processedImageLabel.setText(null);

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(ColorPercentageAnalyzer.this,
                        "Wystąpił błąd podczas obliczeń: " + e.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
                resultLabel.setText("Wynik: Błąd obliczeń.");
            } finally {
                // Zawsze odblokuj przyciski po zakończeniu
                calculateButton.setEnabled(true);
                loadButton.setEnabled(true);
                pickColorButton.setEnabled(true);
            }
        }
    }

    /**
     * Klasa pomocnicza do przekazywania wyników z SwingWorker.
     */
    private static class CalculationResult {
        final double percentage;
        final BufferedImage processedImage;

        CalculationResult(double percentage, BufferedImage processedImage) {
            this.percentage = percentage;
            this.processedImage = processedImage;
        }
    }

    /**
     * Funkcja pomocnicza do skalowania obrazu, aby zmieścił się w etykiecie.
     */
    private Image scaleImage(BufferedImage src, int size) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= size && h <= size) {
            return src; // Nie ma potrzeby skalować
        }

        // Zachowaj proporcje
        if (w > h) {
            return src.getScaledInstance(size, -1, Image.SCALE_SMOOTH);
        } else {
            return src.getScaledInstance(-1, size, Image.SCALE_SMOOTH);
        }
    }

    /**
     * Metoda main do uruchomienia aplikacji.
     */
    public static void main(String[] args) {
        // Uruchom GUI w wątku dystrybucji zdarzeń (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            new ColorPercentageAnalyzer().setVisible(true);
        });
    }
}