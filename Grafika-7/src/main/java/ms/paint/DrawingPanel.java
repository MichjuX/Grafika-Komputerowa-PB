package ms.paint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.List;

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

    public List<Figure> getFigures() {
        return figures;
    }

    public void setFigures(List<Figure> figures) {
        this.figures = figures;
        repaint();
    }

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
        g2.drawLine(getWidth() / 2, 0, getWidth() / 2, getHeight());
        g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);

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
                g2.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
            }
            // Linia do kursora (opcjonalnie można dodać w mouseMoved)
        }

        // Rysowanie punktu transformacji (pivot)
        if (transformCenter != null && (currentMode == MODE_ROTATE || currentMode == MODE_SCALE)) {
            g2.setColor(Color.MAGENTA);
            int r = 4;
            g2.fillOval((int) transformCenter.x - r, (int) transformCenter.y - r, 2 * r, 2 * r);
            g2.drawString("Środek transf.", (int) transformCenter.x + 5, (int) transformCenter.y);
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
        } else if (currentMode == MODE_MOVE) {
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
        } else if (currentMode == MODE_ROTATE || currentMode == MODE_SCALE) {
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
                startDist = Math.sqrt(dx * dx + dy * dy);
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
        } else if (currentMode == MODE_ROTATE && transformCenter != null && !SwingUtilities.isLeftMouseButton(e)) {
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
        } else if (currentMode == MODE_SCALE && transformCenter != null && !SwingUtilities.isLeftMouseButton(e)) {
            double dx = e.getX() - transformCenter.x;
            double dy = e.getY() - transformCenter.y;
            double currentDist = Math.sqrt(dx * dx + dy * dy);

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

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }
}
