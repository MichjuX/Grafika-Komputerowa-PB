package ms.paint;

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
