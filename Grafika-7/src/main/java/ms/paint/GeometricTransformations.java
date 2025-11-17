package ms.paint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

// --- GŁÓWNA KLASA APLIKACJI ---
public class Geometric  Transformations extends JFrame {

    private DrawingPanel drawingPanel;
    private JPanel controlPanel;

    // Pola tekstowe
    private JTextField tfTx, tfTy; // Translacja
    private JTextField tfRotAngle, tfRotX, tfRotY; // Rotacja
    private JTextField tfScaleFactor, tfScaleX, tfScaleY; // Skalowanie
    private JTextArea taPolyCoords; // Ręczne definiowanie figury

    public GeometricTransformations() {
        super("Transformacje 2D - Współrzędne Jednorodne");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        drawingPanel = new DrawingPanel();
        add(drawingPanel, BorderLayout.CENTER);

        controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.EAST);

        setVisible(true);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(300, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // --- SEKCJA: TRYB MYSZY ---
        panel.add(new JLabel("--- TRYB MYSZY ---"));
        String[] modes = {"Rysowanie", "Przesuwanie (Chwyć)", "Obrót (Klik=Środek, Ciągnij)", "Skalowanie (Klik=Środek, Ciągnij)"};
        JComboBox<String> modeBox = new JComboBox<>(modes);
        modeBox.addActionListener(e -> drawingPanel.setMode(modeBox.getSelectedIndex()));
        panel.add(modeBox);

        panel.add(Box.createVerticalStrut(15));

        // --- SEKCJA: RĘCZNE TWORZENIE FIGURY ---
        panel.add(new JLabel("--- DEFINIOWANIE FIGURY ---"));
        panel.add(new JLabel("Format: x1 y1 x2 y2 ..."));
        taPolyCoords = new JTextArea(3, 20);
        panel.add(new JScrollPane(taPolyCoords));
        JButton btnAddPoly = new JButton("Dodaj Figurę z tekstu");
        btnAddPoly.addActionListener(e -> addPolygonFromText());
        panel.add(btnAddPoly);
        JButton btnClear = new JButton("Wyczyść wszystko");
        btnClear.addActionListener(e -> drawingPanel.clearFigures());
        panel.add(btnClear);

        panel.add(Box.createVerticalStrut(15));

        // --- SEKCJA: TRANSLACJA TEKSTOWA ---
        panel.add(new JLabel("--- TRANSLACJA (Wektor) ---"));
        JPanel pTrans = new JPanel(new GridLayout(1, 2));
        tfTx = new JTextField("0"); tfTy = new JTextField("0");
        pTrans.add(new JLabel("Tx:")); pTrans.add(tfTx);
        pTrans.add(new JLabel("Ty:")); pTrans.add(tfTy);
        panel.add(pTrans);
        JButton btnApplyTrans = new JButton("Zatwierdź Przesunięcie");
        btnApplyTrans.addActionListener(e -> applyManualTranslation());
        panel.add(btnApplyTrans);

        panel.add(Box.createVerticalStrut(10));

        // --- SEKCJA: ROTACJA TEKSTOWA ---
        panel.add(new JLabel("--- OBRÓT (Stopnie) ---"));
        JPanel pRot = new JPanel(new GridLayout(3, 2));
        tfRotX = new JTextField("0"); tfRotY = new JTextField("0"); tfRotAngle = new JTextField("45");
        pRot.add(new JLabel("Środek X:")); pRot.add(tfRotX);
        pRot.add(new JLabel("Środek Y:")); pRot.add(tfRotY);
        pRot.add(new JLabel("Kąt:")); pRot.add(tfRotAngle);
        panel.add(pRot);
        JButton btnApplyRot = new JButton("Zatwierdź Obrót");
        btnApplyRot.addActionListener(e -> applyManualRotation());
        panel.add(btnApplyRot);

        panel.add(Box.createVerticalStrut(10));

        // --- SEKCJA: SKALOWANIE TEKSTOWE ---
        panel.add(new JLabel("--- SKALOWANIE (Współczynnik) ---"));
        JPanel pScale = new JPanel(new GridLayout(3, 2));
        tfScaleX = new JTextField("0"); tfScaleY = new JTextField("0"); tfScaleFactor = new JTextField("1.5");
        pScale.add(new JLabel("Środek X:")); pScale.add(tfScaleX);
        pScale.add(new JLabel("Środek Y:")); pScale.add(tfScaleY);
        pScale.add(new JLabel("Skala k:")); pScale.add(tfScaleFactor);
        panel.add(pScale);
        JButton btnApplyScale = new JButton("Zatwierdź Skalowanie");
        btnApplyScale.addActionListener(e -> applyManualScale());
        panel.add(btnApplyScale);

        panel.add(Box.createVerticalStrut(20));

        // --- SEKCJA: PLIKI ---
        JButton btnSave = new JButton("Zapisz (Serialize)");
        btnSave.addActionListener(e -> saveToFile());
        panel.add(btnSave);
        JButton btnLoad = new JButton("Wczytaj (Deserialize)");
        btnLoad.addActionListener(e -> loadFromFile());
        panel.add(btnLoad);

        return panel;
    }

    // --- LOGIKA UI ---

    private void addPolygonFromText() {
        try {
            String text = taPolyCoords.getText();
            StringTokenizer st = new StringTokenizer(text, " ,;");
            List<Point2D> points = new ArrayList<>();
            while (st.hasMoreTokens()) {
                double x = Double.parseDouble(st.nextToken());
                double y = Double.parseDouble(st.nextToken());
                points.add(new Point2D(x, y));
            }
            if (points.size() >= 3) {
                drawingPanel.addFigure(new Figure(points));
            } else {
                JOptionPane.showMessageDialog(this, "Minimum 3 punkty wymagane.");
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Błąd formatu danych.");
        }
    }

    private void applyManualTranslation() {
        try {
            double tx = Double.parseDouble(tfTx.getText());
            double ty = Double.parseDouble(tfTy.getText());
            drawingPanel.applyTransformationToSelected(MatrixUtils.translation(tx, ty));
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Błędne dane liczbowe.");
        }
    }

    private void applyManualRotation() {
        try {
            double x = Double.parseDouble(tfRotX.getText());
            double y = Double.parseDouble(tfRotY.getText());
            double angleDeg = Double.parseDouble(tfRotAngle.getText());
            double angleRad = Math.toRadians(angleDeg);

            // Złożenie: Przesuń do środka -> Obróć -> Przesuń z powrotem
            double[][] m1 = MatrixUtils.translation(-x, -y);
            double[][] m2 = MatrixUtils.rotation(angleRad);
            double[][] m3 = MatrixUtils.translation(x, y);

            double[][] result = MatrixUtils.multiply(m3, MatrixUtils.multiply(m2, m1));
            drawingPanel.applyTransformationToSelected(result);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Błędne dane liczbowe.");
        }
    }

    private void applyManualScale() {
        try {
            double x = Double.parseDouble(tfScaleX.getText());
            double y = Double.parseDouble(tfScaleY.getText());
            double k = Double.parseDouble(tfScaleFactor.getText());

            // Złożenie: Przesuń do środka -> Skaluj -> Przesuń z powrotem
            double[][] m1 = MatrixUtils.translation(-x, -y);
            double[][] m2 = MatrixUtils.scaling(k, k);
            double[][] m3 = MatrixUtils.translation(x, y);

            double[][] result = MatrixUtils.multiply(m3, MatrixUtils.multiply(m2, m1));
            drawingPanel.applyTransformationToSelected(result);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Błędne dane liczbowe.");
        }
    }

    private void saveToFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fc.getSelectedFile()))) {
                oos.writeObject(drawingPanel.getFigures());
                JOptionPane.showMessageDialog(this, "Zapisano pomyślnie.");
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Błąd zapisu: " + e.getMessage());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadFromFile() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fc.getSelectedFile()))) {
                List<Figure> figures = (List<Figure>) ois.readObject();
                drawingPanel.setFigures(figures);
                JOptionPane.showMessageDialog(this, "Wczytano pomyślnie.");
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Błąd odczytu: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GeometricTransformations::new);
    }
}

// --- KLASA POMOCNICZA: MACIERZE I WSPÓŁRZĘDNE JEDNORODNE (3x3) ---
class MatrixUtils {
    // Mnożenie macierzy 3x3
    public static double[][] multiply(double[][] A, double[][] B) {
        double[][] C = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    C[i][j] += A[i][k] * B[k][j];
                }
            }
        }
        return C;
    }

    // Przekształcenie punktu [x, y, 1] przez macierz M
    public static Point2D transformPoint(Point2D p, double[][] M) {
        // Wektor kolumnowy v = [x, y, 1] - lub wierszowy w zależności od konwencji.
        // Tutaj stosujemy konwencję: v' = M * v (gdzie v to wektor kolumnowy)
        // x' = m00*x + m01*y + m02*1
        // y' = m10*x + m11*y + m12*1
        double nx = M[0][0] * p.x + M[0][1] * p.y + M[0][2];
        double ny = M[1][0] * p.x + M[1][1] * p.y + M[1][2];
        return new Point2D(nx, ny);
    }

    public static double[][] identity() {
        return new double[][]{
                {1, 0, 0},
                {0, 1, 0},
                {0, 0, 1}
        };
    }

    public static double[][] translation(double tx, double ty) {
        return new double[][]{
                {1, 0, tx},
                {0, 1, ty},
                {0, 0, 1}
        };
    }

    public static double[][] rotation(double angleRad) {
        double c = Math.cos(angleRad);
        double s = Math.sin(angleRad);
        return new double[][]{
                {c, -s, 0},
                {s, c, 0},
                {0, 0, 1}
        };
    }

    public static double[][] scaling(double sx, double sy) {
        return new double[][]{
                {sx, 0, 0},
                {0, sy, 0},
                {0, 0, 1}
        };
    }
}

// --- KLASY DANYCH ---

class Point2D implements Serializable {
    double x, y;
    public Point2D(double x, double y) { this.x = x; this.y = y; }
}

class Figure implements Serializable {
    List<Point2D> vertices;

    public Figure(List<Point2D> vertices) {
        this.vertices = new ArrayList<>();
        for (Point2D p : vertices) {
            this.vertices.add(new Point2D(p.x, p.y));
        }
    }

    public void transform(double[][] matrix) {
        for (Point2D p : vertices) {
            Point2D np = MatrixUtils.transformPoint(p, matrix);
            p.x = np.x;
            p.y = np.y;
        }
    }

    public boolean contains(double x, double y) {
        // Algorytm Ray Casting do sprawdzania czy punkt jest w wielokącie
        int i, j;
        boolean result = false;
        for (i = 0, j = vertices.size() - 1; i < vertices.size(); j = i++) {
            if ((vertices.get(i).y > y) != (vertices.get(j).y > y) &&
                    (x < (vertices.get(j).x - vertices.get(i).x) * (y - vertices.get(i).y) / (vertices.get(j).y - vertices.get(i).y) + vertices.get(i).x)) {
                result = !result;
            }
        }
        return result;
    }

    // Zwraca środek ciężkości (uproszczony) do celów pomocniczych
    public Point2D getCenter() {
        double sx = 0, sy = 0;
        for(Point2D p : vertices) { sx += p.x; sy += p.y; }
        return new Point2D(sx / vertices.size(), sy / vertices.size());
    }
}

// --- PANEL RYSOWANIA I OBSŁUGA MYSZY ---
class DrawingPanel extends JPanel implements MouseListener, MouseMotionListener {

    public static final int MODE_DRAW = 0;
    public static final int MODE_MOVE = 1;
    public static final int MODE_ROTATE = 2;
    public static final int MODE_SCALE = 3;

    private int currentMode = MODE_DRAW;
    private List<Figure> figures = new ArrayList<>();
    private List<Point2D> currentPolyPoints = new ArrayList<>(); // Punkty aktualnie rysowanej figury

    private Figure selectedFigure = null;

    // Zmienne pomocnicze do interakcji myszą
    private Point lastMousePos;
    private Point2D transformCenter; // Punkt obrotu/skalowania
    private double startAngle;
    private double startDist;

    public DrawingPanel() {
        setBackground(Color.WHITE);
        setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setMode(int mode) {
        this.currentMode = mode;
        currentPolyPoints.clear();
        transformCenter = null;
        repaint();
    }

    public void addFigure(Figure f) {
        figures.add(f);
        repaint();
    }

    public void clearFigures() {
        figures.clear();
        currentPolyPoints.clear();
        selectedFigure = null;
        repaint();
    }

    public List<Figure> getFigures() { return figures; }
    public void setFigures(List<Figure> figures) { this.figures = figures; repaint(); }

    public void applyTransformationToSelected(double[][] matrix) {
        if (selectedFigure != null) {
            selectedFigure.transform(matrix);
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Rysowanie osi współrzędnych (opcjonalne)
        g2.setColor(Color.LIGHT_GRAY);
        g2.drawLine(getWidth()/2, 0, getWidth()/2, getHeight());
        g2.drawLine(0, getHeight()/2, getWidth(), getHeight()/2);

        // Rysowanie wszystkich figur
        for (Figure f : figures) {
            if (f == selectedFigure) {
                g2.setColor(new Color(200, 200, 255)); // Wypełnienie zaznaczonej
                fillFigure(g2, f);
                g2.setColor(Color.RED); // Obrys zaznaczonej
                g2.setStroke(new BasicStroke(2));
            } else {
                g2.setColor(Color.BLACK);
                g2.setStroke(new BasicStroke(1));
            }
            drawFigure(g2, f);
        }

        // Rysowanie aktualnie tworzonej figury
        if (!currentPolyPoints.isEmpty()) {
            g2.setColor(Color.BLUE);
            for (int i = 0; i < currentPolyPoints.size() - 1; i++) {
                Point2D p1 = currentPolyPoints.get(i);
                Point2D p2 = currentPolyPoints.get(i + 1);
                g2.drawLine((int)p1.x, (int)p1.y, (int)p2.x, (int)p2.y);
            }
            // Linia do kursora (opcjonalnie można dodać w mouseMoved)
        }

        // Rysowanie punktu transformacji (pivot)
        if (transformCenter != null && (currentMode == MODE_ROTATE || currentMode == MODE_SCALE)) {
            g2.setColor(Color.MAGENTA);
            int r = 4;
            g2.fillOval((int)transformCenter.x - r, (int)transformCenter.y - r, 2*r, 2*r);
            g2.drawString("Środek transf.", (int)transformCenter.x + 5, (int)transformCenter.y);
        }
    }

    private void drawFigure(Graphics2D g2, Figure f) {
        if (f.vertices.size() < 2) return;
        int[] x = new int[f.vertices.size()];
        int[] y = new int[f.vertices.size()];
        for (int i = 0; i < f.vertices.size(); i++) {
            x[i] = (int) f.vertices.get(i).x;
            y[i] = (int) f.vertices.get(i).y;
        }
        g2.drawPolygon(x, y, f.vertices.size());
    }

    private void fillFigure(Graphics2D g2, Figure f) {
        int[] x = new int[f.vertices.size()];
        int[] y = new int[f.vertices.size()];
        for (int i = 0; i < f.vertices.size(); i++) {
            x[i] = (int) f.vertices.get(i).x;
            y[i] = (int) f.vertices.get(i).y;
        }
        g2.fillPolygon(x, y, f.vertices.size());
    }

    // --- OBSŁUGA ZDARZEŃ MYSZY ---

    @Override
    public void mousePressed(MouseEvent e) {
        lastMousePos = e.getPoint();

        if (currentMode == MODE_DRAW) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                currentPolyPoints.add(new Point2D(e.getX(), e.getY()));
                repaint();
            } else if (SwingUtilities.isRightMouseButton(e) && currentPolyPoints.size() >= 3) {
                // Zakończ rysowanie
                addFigure(new Figure(new ArrayList<>(currentPolyPoints)));
                currentPolyPoints.clear();
            }
        }
        else if (currentMode == MODE_MOVE) {
            // Próba zaznaczenia figury
            boolean clickedOnFigure = false;
            // Iterujemy od końca, żeby łapać te na wierzchu
            for (int i = figures.size() - 1; i >= 0; i--) {
                if (figures.get(i).contains(e.getX(), e.getY())) {
                    selectedFigure = figures.get(i);
                    clickedOnFigure = true;
                    break;
                }
            }
            if (!clickedOnFigure) selectedFigure = null;
            repaint();
        }
        else if (currentMode == MODE_ROTATE || currentMode == MODE_SCALE) {
            if (selectedFigure == null) {
                // Wybierz figurę jeśli nie ma
                for (int i = figures.size() - 1; i >= 0; i--) {
                    if (figures.get(i).contains(e.getX(), e.getY())) {
                        selectedFigure = figures.get(i);
                        break;
                    }
                }
            }

            // Jeśli mamy figurę i klikniemy LEWYM - ustawiamy punkt obrotu/skalowania
            if (SwingUtilities.isLeftMouseButton(e)) {
                transformCenter = new Point2D(e.getX(), e.getY());
            }
            // Jeśli PRAWY lub ŚRODKOWY (lub ciągniemy po ustawieniu) - inicjujemy pomiary
            else if (transformCenter != null) {
                double dx = e.getX() - transformCenter.x;
                double dy = e.getY() - transformCenter.y;
                startAngle = Math.atan2(dy, dx);
                startDist = Math.sqrt(dx*dx + dy*dy);
            }
            repaint();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (selectedFigure == null) return;

        if (currentMode == MODE_MOVE) {
            double dx = e.getX() - lastMousePos.getX();
            double dy = e.getY() - lastMousePos.getY();
            // Tworzenie macierzy translacji
            double[][] T = MatrixUtils.translation(dx, dy);
            selectedFigure.transform(T);
            lastMousePos = e.getPoint();
            repaint();
        }
        else if (currentMode == MODE_ROTATE && transformCenter != null && !SwingUtilities.isLeftMouseButton(e)) {
            double dx = e.getX() - transformCenter.x;
            double dy = e.getY() - transformCenter.y;
            double currentAngle = Math.atan2(dy, dx);
            double deltaAngle = currentAngle - startAngle;

            // Macierz złożona: T(-center) * R(delta) * T(center)
            double[][] m1 = MatrixUtils.translation(-transformCenter.x, -transformCenter.y);
            double[][] m2 = MatrixUtils.rotation(deltaAngle);
            double[][] m3 = MatrixUtils.translation(transformCenter.x, transformCenter.y);

            double[][] total = MatrixUtils.multiply(m3, MatrixUtils.multiply(m2, m1));

            selectedFigure.transform(total);
            startAngle = currentAngle; // Aktualizuj kąt startowy, by unikać skoków
            repaint();
        }
        else if (currentMode == MODE_SCALE && transformCenter != null && !SwingUtilities.isLeftMouseButton(e)) {
            double dx = e.getX() - transformCenter.x;
            double dy = e.getY() - transformCenter.y;
            double currentDist = Math.sqrt(dx*dx + dy*dy);

            if (startDist > 1.0) { // Unikaj dzielenia przez 0
                double scaleFactor = currentDist / startDist;

                // Macierz złożona
                double[][] m1 = MatrixUtils.translation(-transformCenter.x, -transformCenter.y);
                double[][] m2 = MatrixUtils.scaling(scaleFactor, scaleFactor);
                double[][] m3 = MatrixUtils.translation(transformCenter.x, transformCenter.y);

                double[][] total = MatrixUtils.multiply(m3, MatrixUtils.multiply(m2, m1));

                selectedFigure.transform(total);
                startDist = currentDist;
                repaint();
            }
        }
    }

    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseMoved(MouseEvent e) {}
}