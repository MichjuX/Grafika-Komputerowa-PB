package ms.paint;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

public class ImageProcessingApp extends JFrame {

    private BufferedImage originalImage;  // Kopia zapasowa do resetu
    private BufferedImage processedImage; // Obraz aktualnie wyświetlany/edytowany

    // Komponenty GUI
    private ZoomableImagePanel imagePanel; // Nasz nowy panel z zoomem
    private HistogramPanel histogramPanel;

    public ImageProcessingApp() {
        super("ImageProcessingApp");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLayout(new BorderLayout());

        // --- 1. GÓRNY PANEL (PRZYCISKI) ---
        JPanel topToolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        topToolBar.setBorder(BorderFactory.createEtchedBorder());

        // Grupa: Plik
        JButton btnLoad = new JButton("Wczytaj");
        JButton btnReset = new JButton("Resetuj");
        btnReset.setForeground(Color.RED); // Wyróżnienie kolorem

        // Grupa: Histogram
        JButton btnGrayscale = new JButton("Szarość");
        JButton btnStretch = new JButton("Rozszerz Hist.");
        JButton btnEqualize = new JButton("Wyrównaj Hist.");

        // Grupa: Binaryzacja
        JButton btnBinManual = new JButton("Bin. Ręczna");
        JButton btnBinPercent = new JButton("Bin. % Czarnego");
        JButton btnBinIterative = new JButton("Bin. Iteracyjna");

        // Dodawanie do paska (z separatorami dla czytelności)
        topToolBar.add(new JLabel("Plik:"));
        topToolBar.add(btnLoad);
        topToolBar.add(btnReset);
        topToolBar.add(new JSeparator(SwingConstants.VERTICAL));

        topToolBar.add(new JLabel("Norm:"));
        topToolBar.add(btnGrayscale);
        topToolBar.add(btnStretch);
        topToolBar.add(btnEqualize);
        topToolBar.add(new JSeparator(SwingConstants.VERTICAL));

        topToolBar.add(new JLabel("Bin:"));
        topToolBar.add(btnBinManual);
        topToolBar.add(btnBinPercent);
        topToolBar.add(btnBinIterative);

        add(topToolBar, BorderLayout.NORTH);

        // --- 2. CENTRALNY PANEL (OBRAZ + HISTOGRAM) ---
        // Używamy JSplitPane, żeby można było zmieniać proporcje
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.75); // Obraz zajmuje 75% miejsca

        // Lewa strona: Obraz z zoomem wewnątrz ScrollPane
        imagePanel = new ZoomableImagePanel();
        JScrollPane scrollPane = new JScrollPane(imagePanel); // Scrollbary pojawią się przy zoomie
        scrollPane.getViewport().setBackground(Color.DARK_GRAY); // Tło pod obrazem

        // Prawa strona: Histogram
        histogramPanel = new HistogramPanel();
        JPanel histContainer = new JPanel(new BorderLayout());
        histContainer.add(new JLabel("Histogram", SwingConstants.CENTER), BorderLayout.NORTH);
        histContainer.add(histogramPanel, BorderLayout.CENTER);

        splitPane.setLeftComponent(scrollPane);
        splitPane.setRightComponent(histContainer);

        add(splitPane, BorderLayout.CENTER);

        // --- 3. OBSŁUGA ZDARZEŃ ---

        btnLoad.addActionListener(e -> loadImage());

        // Resetowanie obrazu
        btnReset.addActionListener(e -> {
            if (originalImage != null) {
                processedImage = deepCopy(originalImage);
                imagePanel.setImage(processedImage);
                imagePanel.resetZoom(); // Resetujemy też powiększenie
                updateHistogram();
            }
        });

        btnGrayscale.addActionListener(e -> convertToGrayscale());

        btnStretch.addActionListener(e -> {
            if (ensureGrayscale()) {
                processedImage = ImageAlgorithms.stretchHistogram(processedImage);
                updateDisplay();
            }
        });

        btnEqualize.addActionListener(e -> {
            if (ensureGrayscale()) {
                processedImage = ImageAlgorithms.equalizeHistogram(processedImage);
                updateDisplay();
            }
        });

        btnBinManual.addActionListener(e -> {
            if (ensureGrayscale()) {
                String input = JOptionPane.showInputDialog(this, "Podaj próg (0-255):", "128");
                if (input != null) {
                    try {
                        int threshold = Integer.parseInt(input);
                        processedImage = ImageAlgorithms.binarizeManual(processedImage, threshold);
                        updateDisplay();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Nieprawidłowa liczba.");
                    }
                }
            }
        });

        btnBinPercent.addActionListener(e -> {
            if (ensureGrayscale()) {
                String input = JOptionPane.showInputDialog(this, "Podaj procent czarnego (np. 0.4):", "0.5");
                if (input != null) {
                    try {
                        double percent = Double.parseDouble(input);
                        processedImage = ImageAlgorithms.binarizePercentBlack(processedImage, percent);
                        updateDisplay();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Nieprawidłowa liczba.");
                    }
                }
            }
        });

        btnBinIterative.addActionListener(e -> {
            if (ensureGrayscale()) {
                processedImage = ImageAlgorithms.binarizeIterativeMean(processedImage);
                updateDisplay();
            }
        });
    }

    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                originalImage = ImageIO.read(file);
                // Tworzymy kopię roboczą
                processedImage = deepCopy(originalImage);

                imagePanel.setImage(processedImage);
                imagePanel.resetZoom();
                updateHistogram();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Błąd wczytywania pliku.");
            }
        }
    }

    private void convertToGrayscale() {
        if (originalImage == null) return;
        processedImage = ImageAlgorithms.toGrayscale(processedImage); // Pracujemy na current image
        updateDisplay();
    }

    private boolean ensureGrayscale() {
        if (processedImage == null) {
            JOptionPane.showMessageDialog(this, "Najpierw wczytaj obraz.");
            return false;
        }
        // Dla pewności konwertuj, jeśli to RGB
        if (processedImage.getType() != BufferedImage.TYPE_BYTE_GRAY) {
            processedImage = ImageAlgorithms.toGrayscale(processedImage);
        }
        return true;
    }

    private void updateDisplay() {
        if (processedImage != null) {
            imagePanel.setImage(processedImage);
            updateHistogram();
        }
    }

    private void updateHistogram() {
        if (processedImage != null) {
            int[] hist = ImageAlgorithms.calculateHistogram(processedImage);
            histogramPanel.setHistogram(hist);
        }
    }

    // Głęboka kopia obrazu (żeby reset działał poprawnie)
    private BufferedImage deepCopy(BufferedImage bi) {
        ColorModel cm = bi.getColorModel();
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        WritableRaster raster = bi.copyData(null);
        return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ImageProcessingApp().setVisible(true));
    }
}

// --- NOWOŚĆ: Panel z obsługą ZOOM (Scroll) ---
class ZoomableImagePanel extends JPanel {
    private BufferedImage image;
    private double zoomFactor = 1.0;
    private final double ZOOM_MULTIPLIER = 1.1; // Prędkość przybliżania

    public ZoomableImagePanel() {
        // Obsługa kółka myszy
        addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (image == null) return;

                // Ujemne obroty = góra (zoom in), Dodatnie = dół (zoom out)
                if (e.getWheelRotation() < 0) {
                    zoomFactor *= ZOOM_MULTIPLIER;
                } else {
                    zoomFactor /= ZOOM_MULTIPLIER;
                }

                // Ograniczenie zooma, żeby nie zniknął
                if (zoomFactor < 0.1) zoomFactor = 0.1;
                if (zoomFactor > 20.0) zoomFactor = 20.0;

                revalidate(); // Przelicz rozmiar panelu (dla scrollbarów)
                repaint();    // Narysuj ponownie
            }
        });
    }

    public void setImage(BufferedImage img) {
        this.image = img;
        revalidate();
        repaint();
    }

    public void resetZoom() {
        this.zoomFactor = 1.0;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (image != null) {
            int w = (int) (image.getWidth() * zoomFactor);
            int h = (int) (image.getHeight() * zoomFactor);
            return new Dimension(w, h);
        }
        return new Dimension(600, 400);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            Graphics2D g2d = (Graphics2D) g.create();

            // Skalowanie
            g2d.scale(zoomFactor, zoomFactor);

            // Opcjonalnie: Wyłączenie wygładzania, żeby widzieć piksele przy dużym zoomie (przydatne przy binaryzacji)
            // g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);

            g2d.drawImage(image, 0, 0, this);
            g2d.dispose();
        }
    }
}

// --- Panel Histogramu (Bez zmian logicznych, tylko wygląd) ---
class HistogramPanel extends JPanel {
    private int[] histogram;

    public HistogramPanel() {
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
    }

    public void setHistogram(int[] histogram) {
        this.histogram = histogram;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (histogram == null) return;

        int w = getWidth();
        int h = getHeight();
        int maxVal = 0;
        for (int v : histogram) maxVal = Math.max(maxVal, v);

        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0,0,w,h);

        g.setColor(Color.BLACK);
        double xStep = (double) w / 256.0;

        for (int i = 0; i < 256; i++) {
            int barH = (int) (((double) histogram[i] / maxVal) * (h - 20));
            int x = (int) (i * xStep);
            int y = h - barH;
            g.drawLine(x, h, x, y);
        }
    }
}



// Algorytmy
class ImageAlgorithms {
    public static BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage res = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics g = res.getGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return res;
    }

    public static int[] calculateHistogram(BufferedImage img) {
        int[] h = new int[256];
        for (int y=0; y<img.getHeight(); y++)
            for (int x=0; x<img.getWidth(); x++)
                h[img.getRGB(x,y) & 0xFF]++;
        return h;
    }

    public static BufferedImage stretchHistogram(BufferedImage img) {
        int[] h = calculateHistogram(img);
        int min=0, max=255;
        for(int i=0;i<256;i++) if(h[i]>0){min=i; break;}
        for(int i=255;i>=0;i--) if(h[i]>0){max=i; break;}
        if(max==min) return img;

        int[] lut = new int[256];
        for(int i=0; i<256; i++) {
            int v = (int)((255.0*(i-min))/(max-min));
            lut[i] = Math.max(0, Math.min(255, v));
        }
        return applyLut(img, lut);
    }

    public static BufferedImage equalizeHistogram(BufferedImage img) {
        int[] h = calculateHistogram(img);
        int total = img.getWidth()*img.getHeight();
        int[] cdf = new int[256];
        cdf[0] = h[0];
        for(int i=1; i<256; i++) cdf[i] = cdf[i-1] + h[i];

        int minCdf = 0;
        for(int c : cdf) if(c>0) { minCdf = c; break; }

        int[] lut = new int[256];
        for(int i=0; i<256; i++) {
            lut[i] = (int) Math.round(((double)(cdf[i]-minCdf)/(total-minCdf))*255);
            lut[i] = Math.max(0, Math.min(255, lut[i]));
        }
        return applyLut(img, lut);
    }

    public static BufferedImage binarizeManual(BufferedImage img, int thr) {
        int w=img.getWidth(), h=img.getHeight();
        BufferedImage res = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY);
        for(int y=0; y<h; y++){
            for(int x=0; x<w; x++){
                int val = img.getRGB(x,y)&0xFF;
                int newV = (val<thr)?0:255;
                res.setRGB(x,y, (newV<<16)|(newV<<8)|newV);
            }
        }
        return res;
    }

    public static BufferedImage binarizePercentBlack(BufferedImage img, double p) {
        int[] h = calculateHistogram(img);
        int total = img.getWidth()*img.getHeight();
        int target = (int)(total*p);
        int sum=0, thr=128;
        for(int i=0; i<256; i++){
            sum+=h[i];
            if(sum>=target){ thr=i; break;}
        }
        return binarizeManual(img, thr);
    }

    public static BufferedImage binarizeIterativeMean(BufferedImage img) {
        int[] h = calculateHistogram(img);
        double t = 127, oldT;
        do {
            oldT = t;
            double sB=0, wB=0, sF=0, wF=0;
            for(int i=0; i<256; i++) {
                if(i<t) { sB+=i*h[i]; wB+=h[i]; }
                else    { sF+=i*h[i]; wF+=h[i]; }
            }
            if(wB==0) wB=1; if(wF==0) wF=1;
            t = ((sB/wB) + (sF/wF))/2.0;
        } while(Math.abs(t-oldT)>0.5);
        return binarizeManual(img, (int)t);
    }

    private static BufferedImage applyLut(BufferedImage src, int[] lut) {
        BufferedImage res = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        for(int y=0;y<src.getHeight();y++)
            for(int x=0;x<src.getWidth();x++) {
                int v = lut[src.getRGB(x,y)&0xFF];
                res.setRGB(x,y, (v<<16)|(v<<8)|v);
            }
        return res;
    }
}