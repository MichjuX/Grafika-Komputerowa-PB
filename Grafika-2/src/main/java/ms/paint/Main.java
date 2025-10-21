package ms.paint;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.StringTokenizer;

public class Main extends JFrame {
    private BufferedImage image;
    private JLabel imageLabel;
    private double zoom = 1.0;
    private int offsetX = 0, offsetY = 0;
    private Point lastDrag;
    private JFileChooser chooser = new JFileChooser();

    private ImagePanel imagePanel;

    public Main() {
        super("PPM & JPEG Viewer");

        imagePanel = new ImagePanel();
        JScrollPane scroll = new JScrollPane(imagePanel);
        add(scroll, BorderLayout.CENTER);

        createMenu();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
    }

    // zamiast JLabel — własny panel
    private class ImagePanel extends JPanel {
        private Point lastDrag;

        public ImagePanel() {
            addMouseWheelListener(e -> {
                if (image == null) return;
                double oldZoom = zoom;
                if (e.getPreciseWheelRotation() < 0) zoom *= 1.25;
                else zoom /= 1.25;
                zoom = Math.max(0.1, Math.min(zoom, 100));

                // utrzymanie pozycji kursora przy zoomie
                if (lastDrag != null && oldZoom != zoom) {
                    double scale = zoom / oldZoom;
                    offsetX = (int) ((offsetX - e.getX()) * scale + e.getX());
                    offsetY = (int) ((offsetY - e.getY()) * scale + e.getY());
                }
                repaint();
            });

            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    lastDrag = e.getPoint();
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (lastDrag != null) {
                        offsetX += e.getX() - lastDrag.x;
                        offsetY += e.getY() - lastDrag.y;
                        lastDrag = e.getPoint();
                        repaint();
                    }
                }

                public void mouseMoved(MouseEvent e) {
                    if (image == null || zoom < 8) return;
                    int x = (int) ((e.getX() - offsetX) / zoom);
                    int y = (int) ((e.getY() - offsetY) / zoom);
                    if (x >= 0 && y >= 0 && x < image.getWidth() && y < image.getHeight()) {
                        int rgb = image.getRGB(x, y);
                        int r = (rgb >> 16) & 0xFF;
                        int g = (rgb >> 8) & 0xFF;
                        int b = rgb & 0xFF;
                        setTitle(String.format("x=%d y=%d | R=%d G=%d B=%d", x, y, r, g, b));
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (image == null) return;

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.translate(offsetX, offsetY);
            g2.scale(zoom, zoom);
            g2.drawImage(image, 0, 0, null);

//            if (zoom >= 8) {
//                g2.setFont(new Font("Consolas", Font.PLAIN, (int) (12 / zoom + 8)));
//
//                // oblicz widoczny zakres pikseli
//                Rectangle clip = g2.getClipBounds();
//                int startX = Math.max(0, (int) (clip.x / zoom));
//                int startY = Math.max(0, (int) (clip.y / zoom));
//                int endX = Math.min(image.getWidth(), (int) ((clip.x + clip.width) / zoom) + 1);
//                int endY = Math.min(image.getHeight(), (int) ((clip.y + clip.height) / zoom) + 1);
//
//                // rysuj tylko widoczne piksele
//                for (int y = startY; y < endY; y++) {
//                    for (int x = startX; x < endX; x++) {
//                        int rgb = image.getRGB(x, y);
//                        int r = (rgb >> 16) & 0xFF;
//                        int gcol = (rgb >> 8) & 0xFF;
//                        int b = rgb & 0xFF;
//
//                        g2.setColor(Color.BLACK);
//                        g2.drawString(String.format("%d,%d,%d", r, gcol, b), x + 0.1f, y + 0.9f);
//                    }
//                }
//            }
            g2.dispose();
        }


        @Override
        public Dimension getPreferredSize() {
            if (image == null) return new Dimension(800, 600);
            return new Dimension(
                    (int) (image.getWidth() * zoom + Math.abs(offsetX)),
                    (int) (image.getHeight() * zoom + Math.abs(offsetY))
            );
        }
    }

    private void createMenu() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("Plik");
        JMenuItem open = new JMenuItem("Otwórz...");
        JMenuItem saveJpg = new JMenuItem("Zapisz jako JPEG...");
        JMenuItem exit = new JMenuItem("Zamknij");

        open.addActionListener(e -> openFile());
        saveJpg.addActionListener(e -> saveAsJPEG());
        exit.addActionListener(e -> System.exit(0));

        file.add(open);
        file.add(saveJpg);
        file.addSeparator();
        file.add(exit);
        bar.add(file);
        setJMenuBar(bar);
    }

    private void openFile() {
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".ppm")) {
                    image = readPPM(f);
                } else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
                    image = ImageIO.read(f);
                } else {
                    throw new IOException("Nieobsługiwany format pliku!");
                }
                zoom = 1.0;
                offsetX = offsetY = 0;
                imagePanel.repaint();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Błąd", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveAsJPEG() {
        if (image == null) return;
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            try {
                float quality = askJPEGQuality();
                saveJPEG(image, f, quality);
                JOptionPane.showMessageDialog(this, "Zapisano pomyślnie!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Błąd zapisu", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private float askJPEGQuality() {
        String q = JOptionPane.showInputDialog(this, "Stopień kompresji (0.1–1.0):", "0.9");
        try {
            float val = Float.parseFloat(q);
            return Math.min(1f, Math.max(0.1f, val));
        } catch (Exception e) {
            return 0.9f;
        }
    }

    private void updateImageDisplay() {
        if (image == null) {
            imageLabel.setIcon(null);
            return;
        }
        int w = (int) (image.getWidth() * zoom);
        int h = (int) (image.getHeight() * zoom);
        Image scaled = image.getScaledInstance(w, h, Image.SCALE_FAST);

        ImageIcon icon = new ImageIcon(scaled);
        imageLabel.setIcon(icon);
        imageLabel.setBounds(offsetX, offsetY, w, h);
        imageLabel.repaint();
    }

    // ---------------- PPM -------------------
    private BufferedImage readPPM(File f) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
             PushbackInputStream pis = new PushbackInputStream(bis, 1)) {

            String magic = readToken(pis);
            if (magic == null) throw new IOException("Brak nagłówka");
            if (!magic.equals("P3") && !magic.equals("P6"))
                throw new IOException("Nieobsługiwany format: " + magic);
            boolean ascii = magic.equals("P3");

            int width = Integer.parseInt(readToken(pis));
            int height = Integer.parseInt(readToken(pis));
            int maxVal = Integer.parseInt(readToken(pis));
            if (maxVal <= 0) throw new IOException("Niepoprawny maxVal");

            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            if (ascii) {
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        int r = Integer.parseInt(readToken(pis));
                        int g = Integer.parseInt(readToken(pis));
                        int b = Integer.parseInt(readToken(pis));
                        r = scaleTo8Bit(r, maxVal);
                        g = scaleTo8Bit(g, maxVal);
                        b = scaleTo8Bit(b, maxVal);
                        img.setRGB(x, y, (r << 16) | (g << 8) | b);
                    }
                }
            } else {
                skipWhitespace(pis);
                int pixelBytes = (maxVal < 256) ? 1 : 2;
                byte[] row = new byte[width * 3 * pixelBytes];

                for (int y = 0; y < height; y++) {
                    int read = pis.read(row);
                    if (read < row.length)
                        throw new IOException("Nieoczekiwany koniec danych");
                    int idx = 0;
                    for (int x = 0; x < width; x++) {
                        int r, g, b;
                        if (pixelBytes == 1) {
                            r = row[idx++] & 0xFF;
                            g = row[idx++] & 0xFF;
                            b = row[idx++] & 0xFF;
                        } else {
                            r = ((row[idx++] & 0xFF) << 8) | (row[idx++] & 0xFF);
                            g = ((row[idx++] & 0xFF) << 8) | (row[idx++] & 0xFF);
                            b = ((row[idx++] & 0xFF) << 8) | (row[idx++] & 0xFF);
                        }
                        r = scaleTo8Bit(r, maxVal);
                        g = scaleTo8Bit(g, maxVal);
                        b = scaleTo8Bit(b, maxVal);
                        img.setRGB(x, y, (r << 16) | (g << 8) | b);
                    }
                }
            }
            return img;
        }
    }

    private void skipWhitespace(InputStream in) throws IOException {
        int c;
        while ((c = in.read()) != -1) {
            if (!Character.isWhitespace(c)) {
                if (in instanceof PushbackInputStream)
                    ((PushbackInputStream) in).unread(c);
                break;
            }
        }
    }

    private String readToken(InputStream in) throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        // pomijamy komentarze i białe znaki
        while (true) {
            c = in.read();
            if (c == '#') { // komentarz
                while (c != '\n' && c != -1) c = in.read();
            } else if (c == -1) return null;
            else if (!Character.isWhitespace(c)) break;
        }
        sb.append((char) c);
        while ((c = in.read()) != -1 && !Character.isWhitespace(c))
            sb.append((char) c);
        return sb.toString();
    }

    private int scaleTo8Bit(int value, int maxVal) {
        if (maxVal == 255) return value;
        return (int) Math.round((value / (double) maxVal) * 255);
    }

    private void saveJPEG(BufferedImage img, File file, float quality) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) throw new IOException("Brak obsługi JPEG!");
        var writer = writers.next();
        var ios = ImageIO.createImageOutputStream(file);
        writer.setOutput(ios);
        var param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);
        writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
        ios.close();
        writer.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}
