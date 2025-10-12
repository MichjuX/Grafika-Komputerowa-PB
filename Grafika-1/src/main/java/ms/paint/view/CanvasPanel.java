package ms.paint.view;

import ms.paint.model.CircleObject;
import ms.paint.model.LineObject;
import ms.paint.model.RectangleObject;
import ms.paint.model.ShapeObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.io.*;

public class CanvasPanel extends JPanel {
    private ArrayList<ShapeObject> shapes = new ArrayList<>();
    private ShapeObject selected = null;
    private Point prevMouse = null;

    private String currentTool = "LINE";
    private boolean scaleMode = false;
    private boolean moveMode = false;

    private ShapeObject drawingShape = null;
    private Point startPoint = null;

    public CanvasPanel() {
        setBackground(Color.WHITE);

        // Klikanie
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();

                // Tryb rysowania
                if (!scaleMode && !moveMode) {
                    startPoint = p;
                    drawingShape = switch (currentTool) {
                        case "RECT" -> new RectangleObject(p, p);
                        case "CIRCLE" -> new CircleObject(p, p);
                        default -> new LineObject(p, p);
                    };
                    shapes.add(drawingShape);
                }

                // Tryb skalowania
                else if (scaleMode) {
                    selected = getShapeAt(p);
                    if (selected != null) {
                        selected.setSelected(true);
                        prevMouse = p;
                    } else {
                        clearSelection();
                    }
                }

                // Tryb przesuwania
                else if (moveMode) {
                    selected = getShapeAt(p);
                    if (selected != null) {
                        selected.setSelected(true);
                        prevMouse = p;
                    } else {
                        clearSelection();
                    }
                }

                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                drawingShape = null;
                startPoint = null;
                prevMouse = null;
            }
        });


        // Ciągnięcie
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point curr = e.getPoint();

                // Rysowanie
                if (drawingShape != null && startPoint != null && !scaleMode && !moveMode) {
                    drawingShape.resize(startPoint, curr);
                }

                // Skalowanie
                else if (scaleMode && selected != null && prevMouse != null) {
                    selected.scale(prevMouse, curr);
                    prevMouse = curr;
                }

                // Przesuwanie
                else if (moveMode && selected != null && prevMouse != null) {
                    int dx = curr.x - prevMouse.x;
                    int dy = curr.y - prevMouse.y;
                    selected.move(dx, dy);
                    prevMouse = curr;
                }

                repaint();
            }
        });
    }

    private void clearSelection() {
        for (ShapeObject s : shapes) s.setSelected(false);
        selected = null;
    }

    private ShapeObject getShapeAt(Point p) {
        for (int i = shapes.size() - 1; i >= 0; i--) {
            ShapeObject s = shapes.get(i);
            if (s.contains(p)) return s;
        }
        return null;
    }

    public void setCurrentTool(String tool) {
        this.currentTool = tool;
    }

    public String getCurrentTool() {
        return this.currentTool;
    }

    // USTAWIANIE TRYBÓW
    public void setScaleMode(boolean enabled) {
        this.scaleMode = enabled;
        if (enabled) moveMode = false;
        repaint();
    }

    public void setMoveMode(boolean enabled) {
        this.moveMode = enabled;
        if (enabled) scaleMode = false;
        repaint();
    }

    // Dodawanie figury z punktów
    public void addShapeFromCoords(String type, int x1, int y1, int x2, int y2) {
        ShapeObject newShape = switch (type) {
            case "RECT" -> new RectangleObject(new Point(x1, y1), new Point(x2, y2));
            case "CIRCLE" -> new CircleObject(new Point(x1, y1), new Point(x2, y2));
            default -> new LineObject(new Point(x1, y1), new Point(x2, y2));
        };
        shapes.add(newShape);
        repaint();
    }

    // SERIALIZACJA
    // Zapis
    public void saveShapes(File file) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(shapes);
            JOptionPane.showMessageDialog(this, "Zapisano obiekty do pliku.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Odczyt
    public void loadShapes(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            shapes = (ArrayList<ShapeObject>) ois.readObject();
            repaint();
            JOptionPane.showMessageDialog(this, "Wczytano obiekty z pliku.");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        for (ShapeObject s : shapes) {
            s.draw(g2, scaleMode);
        }
    }
}
