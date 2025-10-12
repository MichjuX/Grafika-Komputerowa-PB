package ms.paint;

import ms.paint.controller.ControlPanel;
import ms.paint.view.CanvasPanel;

import javax.swing.*;
import java.awt.*;


public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Projekt 1 – Rysowanie");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(900, 600);
            frame.setLayout(new BorderLayout());

            CanvasPanel canvas = new CanvasPanel();
            ControlPanel controls = new ControlPanel(canvas);

            frame.add(canvas, BorderLayout.CENTER);
            frame.add(controls, BorderLayout.EAST);

            frame.setVisible(true);
        });
    }
}
