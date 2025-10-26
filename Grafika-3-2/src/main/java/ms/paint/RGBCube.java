package ms.paint;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class RGBCube {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RGBCube::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Kostka RGB");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        int voxelSteps = 50;

        CubePanel cubePanel = new CubePanel(voxelSteps);
        frame.add(cubePanel, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 5));

        controlPanel.add(new JLabel("Przekrój:"));

        JRadioButton rbNone = new JRadioButton("Brak", true);
        JRadioButton rbX = new JRadioButton("X");
        JRadioButton rbY = new JRadioButton("Y");
        JRadioButton rbZ = new JRadioButton("Z");

        ButtonGroup axisGroup = new ButtonGroup();
        axisGroup.add(rbNone);
        axisGroup.add(rbX);
        axisGroup.add(rbY);
        axisGroup.add(rbZ);

        controlPanel.add(rbNone);
        controlPanel.add(rbX);
        controlPanel.add(rbY);
        controlPanel.add(rbZ);

        JSlider sliceSlider = new JSlider(0, voxelSteps - 1, voxelSteps - 1);
        sliceSlider.setPreferredSize(new Dimension(200, 20));
        controlPanel.add(sliceSlider);

        sliceSlider.addChangeListener(e -> {
            cubePanel.setSliceValue(sliceSlider.getValue());
        });

        rbNone.addActionListener(e -> cubePanel.setSliceAxis('N'));
        rbX.addActionListener(e -> cubePanel.setSliceAxis('X'));
        rbY.addActionListener(e -> cubePanel.setSliceAxis('Y'));
        rbZ.addActionListener(e -> cubePanel.setSliceAxis('Z'));

        frame.add(controlPanel, BorderLayout.SOUTH);

        // Zakończenie konfiguracji okna
        frame.setSize(800, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}

class CubePanel extends JPanel {

    private final List<Point3D> voxels = new ArrayList<>();
    private final int voxelSteps;

    private double rotX = 0.4; // Obrót góra/dół
    private double rotY = 0.6; // Obrót lewo/prawo

    private int lastMouseX, lastMouseY;

    // Ustawienia przekroju
    private char sliceAxis = 'N'; // N-X-Y-Z
    private int sliceValue = -1; // Ile z przekroju pokazujemy

    public CubePanel(int steps) {
        this.voxelSteps = steps;
        this.sliceValue = steps - 1;
        initializeVoxels();

        setBackground(Color.BLACK);

        // Obracanie
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouseX = e.getX();
                lastMouseY = e.getY();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMouseX;
                int dy = e.getY() - lastMouseY;

                // Zmiana kąta obrotu
                rotY += dx * 0.01;
                rotX += dy * 0.01;

                lastMouseX = e.getX();
                lastMouseY = e.getY();

                repaint();
            }
        };

        addMouseListener(mouseAdapter);
        addMouseMotionListener(mouseAdapter);
    }

    // Siatka voxeli
    private void initializeVoxels() {
        voxels.clear();
        for (int i = 0; i < voxelSteps; i++) {
            for (int j = 0; j < voxelSteps; j++) {
                for (int k = 0; k < voxelSteps; k++) {
                    // Normalizacja współrzędnych do zakresu 0.0 - 1.0
                    double x = (double) i / (voxelSteps - 1);
                    double y = (double) j / (voxelSteps - 1);
                    double z = (double) k / (voxelSteps - 1);
                    voxels.add(new Point3D(x, y, z));
                }
            }
        }
    }

    // PRZEKRÓJ
    public void setSliceAxis(char axis) {
        this.sliceAxis = axis;
        repaint();
    }

    public void setSliceValue(int value) {
        this.sliceValue = value;
        repaint();
    }

    // RENDER
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Wygładzanie krawędzi
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        int centerX = getWidth() / 2;
        int centerY =getHeight() / 2;

        double scale = Math.min(getWidth(), getHeight()) * 0.6;

        int voxelSize = Math.max(2, (int)(scale / voxelSteps * 0.8));

        double cosX = Math.cos(rotX);
        double sinX = Math.sin(rotX);
        double cosY = Math.cos(rotY);
        double sinY = Math.sin(rotY);

        List<RenderableVoxel> renderList = new ArrayList<>();

        for (Point3D p : voxels) {

            // SPRAWDZENIE PRZEKROJU
            int px = (int) Math.round(p.x * (voxelSteps - 1));
            int py = (int) Math.round(p.y * (voxelSteps - 1));
            int pz = (int) Math.round(p.z * (voxelSteps - 1));

            if (sliceAxis == 'X' && px > sliceValue) continue;
            if (sliceAxis == 'Y' && py > sliceValue) continue;
            if (sliceAxis == 'Z' && pz > sliceValue) continue;

            // CENTROWANIE
            // Przesuwamy kostkę tak, aby jej środek był w (0,0,0)
            double x = p.x - 0.5;
            double y = p.y - 0.5;
            double z = p.z - 0.5;

            // OBRÓT
            // oś X
            double y_rot = y * cosX - z * sinX;
            double z_rot = y * sinX + z * cosX;

            // oś Y
            double x_rot = x * cosY - z_rot * sinY;
            double z_final = x * sinY + z_rot * cosY; // To jest nasza głębia (depth)

            // RZUTOWANIE
            // Zamiana (x_rot, y_rot) na współrzędne ekranu (sx, sy)
            int sx = (int) (x_rot * scale) + centerX;
            int sy = (int) (y_rot * scale) + centerY;

            // KOLOROWANIE
            Color color = new Color((float)p.x, (float)p.y, (float)p.z);

            // Dodajemy do listy renderowania
            renderList.add(new RenderableVoxel(sx, sy, z_final, color));
        }

        // SORTOWANIE - Algorytm Malarza
        // Sortujemy wszystkie punkty od tyłu do przodu (od najmniejszego 'z' do największego)
        // To zapewnia, że bliższe punkty poprawnie zakrywają te dalsze.
        Collections.sort(renderList, Comparator.comparingDouble(v -> v.z));

        // RYSOWANIE
        for (RenderableVoxel v : renderList) {
            g2d.setColor(v.color);
            // Odejmujemy połowę rozmiaru, aby wyśrodkować go na jego (sx, sy)
            g2d.fillRect(v.sx - voxelSize / 2, v.sy - voxelSize / 2, voxelSize, voxelSize);
        }
    }
}

class Point3D {
    double x, y, z;
    public Point3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
}

class RenderableVoxel {
    int sx, sy;   // Współrzędne ekranowe 2D
    double z;     // Głębia (do sortowania)
    Color color;  // Kolor woksla

    public RenderableVoxel(int sx, int sy, double z, Color color) {
        this.sx = sx;
        this.sy = sy;
        this.z = z;
        this.color = color;
    }
}