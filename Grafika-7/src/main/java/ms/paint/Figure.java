package ms.paint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
        for (Point2D p : vertices) {
            sx += p.x;
            sy += p.y;
        }
        return new Point2D(sx / vertices.size(), sy / vertices.size());
    }
}
