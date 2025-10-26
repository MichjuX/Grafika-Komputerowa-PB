package ms.paint;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

public class ColorConverter extends JFrame {
    private final JSlider[] rgbSliders = new JSlider[3];
    private final JTextField[] rgbFields = new JTextField[3];
    private final String[] rgbNames = {"R", "G", "B"};

    private final JSlider[] cmykSliders = new JSlider[4];
    private final JTextField[] cmykFields = new JTextField[4];
    private final String[] cmykNames = {"C", "M", "Y", "K"};

    private final JPanel colorPreview = new JPanel();
    private final JLabel hexLabel = new JLabel("#000000");

    private boolean isUpdating = false;

    public ColorConverter() {
        super("RGB ↔ CMYK Converter");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        initUI();
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(700, 400));
    }

    private void initUI() {
        JPanel main = new JPanel(new GridLayout(1, 2, 8, 8));
        main.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        main.add(createRGBPanel());
        main.add(createCMYKPanel());
        main.add(createColorMapPanel(), BorderLayout.WEST);
        add(main, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(8, 8));
        colorPreview.setPreferredSize(new Dimension(100, 80));
        colorPreview.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        bottom.add(colorPreview, BorderLayout.WEST);

        JPanel info = new JPanel(new FlowLayout(FlowLayout.LEFT));
        info.add(new JLabel("Hex: "));
        info.add(hexLabel);
        bottom.add(info, BorderLayout.CENTER);

        add(bottom, BorderLayout.SOUTH);

        setRGBValues(255, 0, 0);
    }

    // COLOR PICKER
    private JPanel createColorMapPanel() {
        JPanel panel = new JPanel() {
            private BufferedImage colorMap;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        pickColor(e);
                    }
                });
                addMouseMotionListener(new MouseMotionAdapter() {
                    @Override
                    public void mouseDragged(MouseEvent e) {
                        pickColor(e);
                    }
                });
            }

            private void pickColor(MouseEvent e) {
                if (colorMap == null) return;
                int x = Math.max(0, Math.min(e.getX(), colorMap.getWidth() - 1));
                int y = Math.max(0, Math.min(e.getY(), colorMap.getHeight() - 1));
                int rgb = colorMap.getRGB(x, y);
                Color c = new Color(rgb);
                setRGBValues(c.getRed(), c.getGreen(), c.getBlue());
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                int w = getWidth();
                int h = getHeight();

                if (colorMap == null || colorMap.getWidth() != w || colorMap.getHeight() != h) {
                    colorMap = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                    for (int x = 0; x < w; x++) {
                        float hue = (float) x / w; // 0–1 = cały zakres tęczy
                        for (int y = 0; y < h; y++) {
                            float brightness = 1f - ((float) y / h); // od góry jasny → dół ciemny
                            float saturation = 1f;
                            Color c = Color.getHSBColor(hue, saturation, brightness);
                            colorMap.setRGB(x, y, c.getRGB());
                        }
                    }
                }
                g.drawImage(colorMap, 0, 0, null);
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(300, 200);
            }
        };

        panel.setBorder(BorderFactory.createTitledBorder("Mapa kolorów"));
        return panel;
    }



    private JPanel createRGBPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("RGB (0–255)"));
        JPanel controls = new JPanel(new GridLayout(3, 1, 6, 6));

        for (int i = 0; i < 3; i++) {
            JPanel row = new JPanel(new BorderLayout(6, 6));
            JLabel label = new JLabel(rgbNames[i]);
            label.setPreferredSize(new Dimension(20, 20));
            row.add(label, BorderLayout.WEST);

            rgbSliders[i] = new JSlider(0, 255, 0);
            rgbSliders[i].setMajorTickSpacing(64);
            rgbSliders[i].setPaintTicks(true);
            row.add(rgbSliders[i], BorderLayout.CENTER);

            rgbFields[i] = new JTextField("0", 4);
            row.add(rgbFields[i], BorderLayout.EAST);

            final int idx = i;
            rgbSliders[i].addChangeListener(e -> {
                if (!rgbSliders[idx].getValueIsAdjusting()) {
                    updateFromRGBSlider();
                } else {
                    updateFromRGBSlider();
                }
            });

            rgbFields[i].addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    updateFromRGBField();
                }
            });
            rgbFields[i].addActionListener(e -> updateFromRGBField());

            controls.add(row);
        }

        panel.add(controls, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCMYKPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createTitledBorder("CMYK (0–100%)"));
        JPanel controls = new JPanel(new GridLayout(4, 1, 6, 6));

        for (int i = 0; i < 4; i++) {
            JPanel row = new JPanel(new BorderLayout(6, 6));
            JLabel label = new JLabel(cmykNames[i]);
            label.setPreferredSize(new Dimension(20, 20));
            row.add(label, BorderLayout.WEST);

            cmykSliders[i] = new JSlider(0, 100, 0);
            cmykSliders[i].setMajorTickSpacing(25);
            cmykSliders[i].setPaintTicks(true);
            row.add(cmykSliders[i], BorderLayout.CENTER);

            cmykFields[i] = new JTextField("0", 4);
            row.add(cmykFields[i], BorderLayout.EAST);

            final int idx = i;
            cmykSliders[i].addChangeListener(e -> {
                if (!cmykSliders[idx].getValueIsAdjusting()) {
                    updateFromCMYKSlider();
                } else {
                    updateFromCMYKSlider();
                }
            });

            cmykFields[i].addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    updateFromCMYKField();
                }
            });
            cmykFields[i].addActionListener(e -> updateFromCMYKField());

            controls.add(row);
        }

        panel.add(controls, BorderLayout.CENTER);
        return panel;
    }

    private void updateFromRGBSlider() {
        if (isUpdating) return;
        try {
            isUpdating = true;
            int r = rgbSliders[0].getValue();
            int g = rgbSliders[1].getValue();
            int b = rgbSliders[2].getValue();

            for (int i = 0; i < 3; i++) {
                safeSetText(rgbFields[i], String.valueOf(rgbSliders[i].getValue()));
            }

            double[] cmyk = rgbToCmyk(r, g, b);
            setCMYKValues(cmyk);
            updatePreview(r, g, b);
        } finally {
            isUpdating = false;
        }
    }

    private void updateFromRGBField() {
        if (isUpdating) return;
        try {
            isUpdating = true;
            int[] vals = new int[3];
            for (int i = 0; i < 3; i++) {
                vals[i] = parseIntOr(rgbFields[i].getText(), rgbSliders[i].getValue());
                vals[i] = clamp(vals[i], 0, 255);
                rgbSliders[i].setValue(vals[i]);
                safeSetText(rgbFields[i], String.valueOf(vals[i]));
            }
            double[] cmyk = rgbToCmyk(vals[0], vals[1], vals[2]);
            setCMYKValues(cmyk);
            updatePreview(vals[0], vals[1], vals[2]);
        } finally {
            isUpdating = false;
        }
    }

    private void updateFromCMYKSlider() {
        if (isUpdating) return;
        try {
            isUpdating = true;
            double[] cmyk = new double[4];
            for (int i = 0; i < 4; i++) {
                cmyk[i] = cmykSliders[i].getValue() / 100.0;
                safeSetText(cmykFields[i], String.valueOf(cmykSliders[i].getValue()));
            }
            int[] rgb = cmykToRgb(cmyk[0], cmyk[1], cmyk[2], cmyk[3]);
            setRGBValues(rgb[0], rgb[1], rgb[2]);
            updatePreview(rgb[0], rgb[1], rgb[2]);
        } finally {
            isUpdating = false;
        }
    }

    private void updateFromCMYKField() {
        if (isUpdating) return;
        try {
            isUpdating = true;
            double[] vals = new double[4];
            for (int i = 0; i < 4; i++) {
                int v = parseIntOr(cmykFields[i].getText(), cmykSliders[i].getValue());
                v = clamp(v, 0, 100);
                cmykSliders[i].setValue(v);
                safeSetText(cmykFields[i], String.valueOf(v));
                vals[i] = v / 100.0;
            }
            int[] rgb = cmykToRgb(vals[0], vals[1], vals[2], vals[3]);
            setRGBValues(rgb[0], rgb[1], rgb[2]);
            updatePreview(rgb[0], rgb[1], rgb[2]);
        } finally {
            isUpdating = false;
        }
    }

    private void setRGBValues(int r, int g, int b) {
        for (int i = 0; i < 3; i++) {
            rgbSliders[i].setValue(i == 0 ? r : i == 1 ? g : b);
        }
        safeSetText(rgbFields[0], String.valueOf(r));
        safeSetText(rgbFields[1], String.valueOf(g));
        safeSetText(rgbFields[2], String.valueOf(b));
        double[] cmyk = rgbToCmyk(r, g, b);
        setCMYKValues(cmyk);
        updatePreview(r, g, b);
    }

    private void setCMYKValues(double[] cmyk) {
        for (int i = 0; i < 4; i++) {
            int percent = (int) Math.round(clampDouble(cmyk[i], 0.0, 1.0) * 100);
            cmykSliders[i].setValue(percent);
            safeSetText(cmykFields[i], String.valueOf(percent));
        }
    }

    private void updatePreview(int r, int g, int b) {
        Color col = new Color(clamp(r, 0, 255), clamp(g, 0, 255), clamp(b, 0, 255));
        colorPreview.setBackground(col);
        hexLabel.setText(String.format("#%02X%02X%02X", col.getRed(), col.getGreen(), col.getBlue()));
    }

    private int parseIntOr(String s, int fallback) {
        try {
            s = s.trim();
            if (s.endsWith("%")) s = s.substring(0, s.length() - 1);
            return Integer.parseInt(s);
        } catch (Exception ex) {
            return fallback;
        }
    }

    private int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private double[] rgbToCmyk(int r, int g, int b) {
        double R = r / 255.0;
        double G = g / 255.0;
        double B = b / 255.0;
        double K = 1.0 - Math.max(R, Math.max(G, B));
        double C, M, Y;
        if (Math.abs(1.0 - K) < 1e-9) {
            C = M = Y = 0;
            K = 1;
        } else {
            C = (1 - R - K) / (1 - K);
            M = (1 - G - K) / (1 - K);
            Y = (1 - B - K) / (1 - K);
        }
        return new double[]{clampDouble(C, 0, 1), clampDouble(M, 0, 1), clampDouble(Y, 0, 1), clampDouble(K, 0, 1)};
    }

    private int[] cmykToRgb(double c, double m, double y, double k) {
        int R = (int) Math.round(255 * (1 - c) * (1 - k));
        int G = (int) Math.round(255 * (1 - m) * (1 - k));
        int B = (int) Math.round(255 * (1 - y) * (1 - k));
        return new int[]{clamp(R, 0, 255), clamp(G, 0, 255), clamp(B, 0, 255)};
    }

    private void safeSetText(JTextField field, String text) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> safeSetText(field, text));
            return;
        }

        if (text.equals(field.getText())) return;

        boolean oldState = isUpdating;
        isUpdating = true;
        try {
            field.setText(text);
        } finally {
            isUpdating = oldState;
        }
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ColorConverter().setVisible(true));
    }
}
