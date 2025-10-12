package ms.paint.controller;

import ms.paint.view.CanvasPanel;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {
    private CanvasPanel canvas;

    public ControlPanel(CanvasPanel canvas) {
        this.canvas = canvas;
        setLayout(new GridLayout(22, 1, 5, 5));

        JButton lineBtn = new JButton("Linia");
        JButton rectBtn = new JButton("Prostokąt");
        JButton circleBtn = new JButton("Okrąg");

        JCheckBox scaleMode = new JCheckBox("Tryb skalowania");
        JCheckBox moveMode = new JCheckBox("Tryb przesuwania");

        JTextField x1 = new JTextField();
        JTextField y1 = new JTextField();
        JTextField x2 = new JTextField();
        JTextField y2 = new JTextField();

        JButton createBtn = new JButton("Utwórz z punktów");
        JButton saveBtn = new JButton("Zapisz do pliku");
        JButton loadBtn = new JButton("Wczytaj z pliku");

        // Narzędzia
        lineBtn.addActionListener(e -> canvas.setCurrentTool("LINE"));
        rectBtn.addActionListener(e -> canvas.setCurrentTool("RECT"));
        circleBtn.addActionListener(e -> canvas.setCurrentTool("CIRCLE"));

        // Tryby
        scaleMode.addActionListener(e -> {
            canvas.setScaleMode(scaleMode.isSelected());
            if (scaleMode.isSelected()) moveMode.setSelected(false);
        });

        moveMode.addActionListener(e -> {
            canvas.setMoveMode(moveMode.isSelected());
            if (moveMode.isSelected()) scaleMode.setSelected(false);
        });

        // Tworzenie z punktów
        createBtn.addActionListener(e -> {
            try {
                int px1 = Integer.parseInt(x1.getText());
                int py1 = Integer.parseInt(y1.getText());
                int px2 = Integer.parseInt(x2.getText());
                int py2 = Integer.parseInt(y2.getText());
                canvas.addShapeFromCoords(canvas.getCurrentTool(), px1, py1, px2, py2);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Podaj poprawne liczby!");
            }
        });

        // Zapis i odczyt
        saveBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION)
                canvas.saveShapes(fc.getSelectedFile());
        });

        loadBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
                canvas.loadShapes(fc.getSelectedFile());
        });

        // Układ
        add(new JLabel("Tryb rysowania:"));
        add(lineBtn);
        add(rectBtn);
        add(circleBtn);

        add(scaleMode);
        add(moveMode);

        add(new JLabel("Utwórz figurę z punktów:"));
        add(new JLabel("x1:")); add(x1);
        add(new JLabel("y1:")); add(y1);
        add(new JLabel("x2:")); add(x2);
        add(new JLabel("y2:")); add(y2);
        add(createBtn);

        add(saveBtn);
        add(loadBtn);
    }
}