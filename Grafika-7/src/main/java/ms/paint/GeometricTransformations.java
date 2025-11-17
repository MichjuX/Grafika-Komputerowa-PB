package ms.paint;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

// --- GŁÓWNA KLASA APLIKACJI ---
public class GeometricTransformations extends JFrame {

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

// --- KLASY DANYCH ---

class Point2D implements Serializable {
    double x, y;
    public Point2D(double x, double y) { this.x = x; this.y = y; }
}

