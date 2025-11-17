package ms.paint;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class BezierCurveApp extends JFrame {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new BezierCurveApp().setVisible(true);
        });
    }

    private DrawingPanel drawingPanel;
    private ControlPanel controlPanel;
    private ArrayList<Point> points;
    private int resolutionK = 1000; // Liczba punktów obliczeniowych krzywej

    public BezierCurveApp() {
        points = new ArrayList<>();

        setTitle("Krzywa Béziera");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Inicjalizacja komponentów
        drawingPanel = new DrawingPanel();
        controlPanel = new ControlPanel();

        add(drawingPanel, BorderLayout.CENTER);
        add(new JScrollPane(controlPanel), BorderLayout.EAST);

        // Pasek statusu
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(new JLabel("Info: Stopień n wymaga n+1 punktów. Modyfikacja myszą lub polami tekstowymi działa na żywo."));
        add(statusPanel, BorderLayout.SOUTH);
    }

    // ==========================================
    // PANEL RYSOWANIA (CANVAS)
    // ==========================================
    class DrawingPanel extends JPanel {
        private int draggedPointIndex = -1;

        public DrawingPanel() {
            setBackground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            MouseAdapter ma = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    // 1. Sprawdź czy kliknięto w punkt (edycja)
                    draggedPointIndex = getPointAt(e.getPoint());

                    // 2. Jeśli nie trafiono w punkt -> dodajemy nowy
                    if (draggedPointIndex == -1) {
                        points.add(e.getPoint());
                        controlPanel.refreshInputs();
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    draggedPointIndex = -1;
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedPointIndex != -1) {
                        points.set(draggedPointIndex, e.getPoint());
                        controlPanel.updateInputValues(draggedPointIndex);
                        repaint();
                    }
                }
            };

            addMouseListener(ma);
            addMouseMotionListener(ma);
        }

        private int getPointAt(Point p) {
            int radius = 10;
            for (int i = 0; i < points.size(); i++) {
                if (points.get(i).distance(p) <= radius) return i;
            }
            return -1;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // --- Rysowanie siatki kontrolnej ---
            if (points.size() > 0) {
                g2.setColor(Color.LIGHT_GRAY);
                for (int i = 0; i < points.size() - 1; i++) {
                    g2.drawLine(points.get(i).x, points.get(i).y, points.get(i + 1).x, points.get(i + 1).y);
                }
            }

            // --- Rysowanie punktów charakterystycznych ---
            for (int i = 0; i < points.size(); i++) {
                Point p = points.get(i);
                g2.setColor(Color.BLUE);
                g2.fillOval(p.x - 6, p.y - 6, 12, 12);
                g2.setColor(Color.BLACK);
                g2.drawString("P" + i, p.x + 8, p.y - 8);
            }

            // --- Rysowanie Krzywej Béziera ---
            if (points.size() < 2) return;

            g2.setColor(Color.RED);
            g2.setStroke(new BasicStroke(2));

            int n = points.size() - 1; // Stopień krzywej
            int k = resolutionK;       // Rozdzielczość

            Point prev = calculateBezierPoint(n, 0, k);

            for (int j = 1; j < k; j++) {
                Point curr = calculateBezierPoint(n, j, k);
                g2.drawLine(prev.x, prev.y, curr.x, curr.y);
                prev = curr;
            }
        }

        // B_j = Suma (n po i) * (1 - t)^(n-i) * t^i * P_i
        private Point calculateBezierPoint(int n, int j, int k) {
            double t = (double) j / (double) (k - 1);
            double sumX = 0;
            double sumY = 0;

            // Rozpisany uproszczony wzór z ceza bo wywalamy j/k-1 do zmiennej t
            for (int i = 0; i <= n; i++) {
                long newton = combinations(n, i);
                double basis = newton * Math.pow(1 - t, n - i) * Math.pow(t, i);
                sumX += basis * points.get(i).x;
                sumY += basis * points.get(i).y;
            }
            return new Point((int) Math.round(sumX), (int) Math.round(sumY));
        }

        private long combinations(int n, int k) {
            if (k < 0 || k > n) return 0;
            if (k == 0 || k == n) return 1;
            if (k > n / 2) k = n - k;
            long res = 1;
            for (int i = 1; i <= k; i++) res = res * (n - i + 1) / i;
            return res;
        }
    }

    // ==========================================
    // PANEL STEROWANIA
    // ==========================================
    class ControlPanel extends JPanel {
        // Settings
        private JTextField kField;

        // Ustawianie stopnia
        private JTextField degreeField;

        // Dodawanie punktu
        private JTextField newPtX, newPtY;

        // Lista punktów
        private JPanel pointsListPanel;
        private List<JTextField[]> pointInputs = new ArrayList<>();

        public ControlPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setPreferredSize(new Dimension(280, 0));
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            // --- 0. Ustawienia (PRZYWRÓCONE) ---
            JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            settingsPanel.setBorder(new TitledBorder("0. Ustawienia"));
            settingsPanel.add(new JLabel("Rozdzielczość (k):"));

            kField = new JTextField(String.valueOf(resolutionK), 5);
            kField.addActionListener(e -> {
                try {
                    int val = Integer.parseInt(kField.getText());
                    if(val > 1) {
                        resolutionK = val;
                        drawingPanel.repaint();
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Podaj liczbę całkowitą!");
                }
            });
            settingsPanel.add(kField);
            add(settingsPanel);
            add(Box.createVerticalStrut(10));


            // --- 1. Zdefiniuj Stopień ---
            JPanel degreePanel = new JPanel(new GridLayout(0, 1, 5, 5));
            degreePanel.setBorder(new TitledBorder("1. Ustaw stopień krzywej"));

            JPanel degRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            degRow.add(new JLabel("Stopień (n):"));
            degreeField = new JTextField("3", 3);
            degRow.add(degreeField);
            JButton setDegreeBtn = new JButton("Ustaw / Resetuj");

            setDegreeBtn.addActionListener(e -> {
                try {
                    int deg = Integer.parseInt(degreeField.getText());
                    if (deg < 1) {
                        JOptionPane.showMessageDialog(this, "Stopień musi być >= 1");
                        return;
                    }
                    initializeCurveByDegree(deg);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Podaj liczbę całkowitą!");
                }
            });
            degRow.add(setDegreeBtn);

            degreePanel.add(degRow);
            degreePanel.add(new JLabel("<html><i>Uwaga: Ustawienie stopnia n<br>stworzy automatycznie n+1 punktów.</i></html>"));

            add(degreePanel);
            add(Box.createVerticalStrut(10));

            // --- 2. Dodaj Punkt Ręcznie ---
            JPanel addPtPanel = new JPanel(new GridLayout(0, 1, 5, 5));
            addPtPanel.setBorder(new TitledBorder("2. Dodaj punkt (Tekstowo)"));

            JPanel coordsRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
            newPtX = new JTextField("100", 3);
            newPtY = new JTextField("100", 3);
            JButton addPtBtn = new JButton("Dodaj");

            addPtBtn.addActionListener(e -> {
                try {
                    int x = Integer.parseInt(newPtX.getText());
                    int y = Integer.parseInt(newPtY.getText());
                    points.add(new Point(x, y));
                    refreshInputs();
                    drawingPanel.repaint();
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Błędne współrzędne!");
                }
            });

            coordsRow.add(new JLabel("X:")); coordsRow.add(newPtX);
            coordsRow.add(new JLabel("Y:")); coordsRow.add(newPtY);
            coordsRow.add(addPtBtn);
            addPtPanel.add(coordsRow);

            add(addPtPanel);
            add(Box.createVerticalStrut(10));

            // --- 3. Lista Punktów (Edycja) ---
            JLabel listLabel = new JLabel("3. Edycja punktów:");
            listLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            add(listLabel);

            pointsListPanel = new JPanel();
            pointsListPanel.setLayout(new BoxLayout(pointsListPanel, BoxLayout.Y_AXIS));

            JScrollPane scroll = new JScrollPane(pointsListPanel);
            scroll.setPreferredSize(new Dimension(250, 300));
            add(scroll);
        }

        // Metoda realizująca wymaganie "Stopień podany przez użytkownika"
        private void initializeCurveByDegree(int n) {
            points.clear();
            int startX = 100, startY = 500;
            int endX = 600, endY = 100;

            for (int i = 0; i <= n; i++) {
                double ratio = (double)i / n;
                int x = (int)(startX + (endX - startX) * ratio);
                int y = (int)(startY + (endY - startY) * ratio);

                // Lekkie wygięcie dla lepszego efektu wizualnego
                if (i > 0 && i < n) y += 100;

                points.add(new Point(x, y));
            }
            refreshInputs();
            drawingPanel.repaint();
        }

        // Odświeża listę pól tekstowych na podstawie punktów na liście
        public void refreshInputs() {
            pointsListPanel.removeAll();
            pointInputs.clear();

            int n = points.size();
            if (n > 0) {
                JLabel info = new JLabel("Aktualny stopień krzywej: " + (n - 1));
                info.setForeground(Color.BLUE);
                pointsListPanel.add(info);
            }

            for (int i = 0; i < n; i++) {
                Point p = points.get(i);
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
                row.add(new JLabel("P" + i + ": "));

                JTextField xField = new JTextField(String.valueOf(p.x), 3);
                JTextField yField = new JTextField(String.valueOf(p.y), 3);

                int index = i;
                DocumentListener dl = new DocumentListener() {
                    public void insertUpdate(DocumentEvent e) { update(); }
                    public void removeUpdate(DocumentEvent e) { update(); }
                    public void changedUpdate(DocumentEvent e) { update(); }
                    void update() {
                        try {
                            int valX = Integer.parseInt(xField.getText());
                            int valY = Integer.parseInt(yField.getText());
                            points.set(index, new Point(valX, valY));
                            drawingPanel.repaint();
                        } catch (Exception ex) {}
                    }
                };
                xField.getDocument().addDocumentListener(dl);
                yField.getDocument().addDocumentListener(dl);

                row.add(new JLabel("x")); row.add(xField);
                row.add(new JLabel("y")); row.add(yField);

                // Przycisk usuwania punktu
                JButton delBtn = new JButton("X");
                delBtn.setMargin(new Insets(0,2,0,2));
                delBtn.addActionListener(e -> {
                    points.remove(index);
                    refreshInputs();
                    drawingPanel.repaint();
                });
                row.add(delBtn);

                pointsListPanel.add(row);
                pointInputs.add(new JTextField[]{xField, yField});
            }
            pointsListPanel.revalidate();
            pointsListPanel.repaint();
        }

        // Aktualizacja pól tekstowych gdy ruszamy myszką
        public void updateInputValues(int index) {
            if (index >= 0 && index < pointInputs.size()) {
                Point p = points.get(index);
                JTextField[] fields = pointInputs.get(index);
                if (!fields[0].getText().equals(String.valueOf(p.x)))
                    fields[0].setText(String.valueOf(p.x));
                if (!fields[1].getText().equals(String.valueOf(p.y)))
                    fields[1].setText(String.valueOf(p.y));
            }
        }
    }
}