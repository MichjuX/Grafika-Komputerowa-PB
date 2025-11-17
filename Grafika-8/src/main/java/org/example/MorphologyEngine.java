package org.example;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Silnik logiki morfologicznej.
 * Zawiera statyczne metody do wykonywania operacji morfologicznych
 * od zera, bez zewnętrznych bibliotek.
 * * Używa konwencji:
 * Obraz: Czarny (0x000000) = Tło (0)
 * Biały (0xFFFFFF) = Pierwszy plan (1)
 * * SE (Element Strukturyzujący):
 * 1 = Pierwszy plan
 * 0 = Tło
 * -1 (z 'X') = Ignoruj (Don't Care)
 */
public class MorphologyEngine {

    private static final int FOREGROUND = 1;
    private static final int BACKGROUND = 0;
    private static final int DONT_CARE = -1;

    private static final int COLOR_WHITE = 0xFFFFFF;
    private static final int COLOR_BLACK = 0x000000;

    /**
     * Sprawdza, czy piksel na obrazie jest włączony (pierwszy plan / biały).
     */
    private static boolean isPixelOn(BufferedImage img, int x, int y) {
        // Sprawdzenie granic
        if (x < 0 || x >= img.getWidth() || y < 0 || y >= img.getHeight()) {
            return false; // Traktuj obszar poza obrazem jako tło
        }
        return (img.getRGB(x, y) & 0x00FFFFFF) == COLOR_WHITE;
    }

    /**
     * Konwertuje obraz RGB na obraz binarny (czarno-biały)
     * używając podanego progu.
     */
    public static BufferedImage binarize(BufferedImage input, int threshold) {
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                Color c = new Color(input.getRGB(x, y));
                // Prosta konwersja do skali szarości (uśredniona)
                int gray = (c.getRed() + c.getGreen() + c.getBlue()) / 3;
                if (gray > threshold) {
                    output.setRGB(x, y, COLOR_WHITE); // Pierwszy plan
                } else {
                    output.setRGB(x, y, COLOR_BLACK); // Tło
                }
            }
        }
        return output;
    }

    /**
     * Parsuje tekst z JTextArea na macierz int[][] dla SE.
     * "1" -> 1
     * "0" -> 0
     * "X" -> -1 (DON'T_CARE)
     */
    public static int[][] parseSE(String text) {
        String[] lines = text.trim().split("\n");
        int rows = lines.length;
        if (rows == 0) throw new IllegalArgumentException("SE jest puste.");

        String[][] cells = new String[rows][];
        for (int i = 0; i < rows; i++) {
            cells[i] = lines[i].trim().split("\\s+");
        }

        int cols = cells[0].length;
        int[][] se = new int[rows][cols];

        for (int y = 0; y < rows; y++) {
            if (cells[y].length != cols) {
                throw new IllegalArgumentException("Wiersze SE muszą mieć tę samą długość.");
            }
            for (int x = 0; x < cols; x++) {
                String cell = cells[y][x].toUpperCase();
                if (cell.equals("1")) {
                    se[y][x] = FOREGROUND;
                } else if (cell.equals("0")) {
                    se[y][x] = BACKGROUND;
                } else if (cell.equals("X")) {
                    se[y][x] = DONT_CARE;
                } else {
                    throw new NumberFormatException("Nieprawidłowy znak w SE: '" + cell + "'");
                }
            }
        }
        return se;
    }

    /**
     * Tworzy głęboką kopię obrazu.
     */
    private static BufferedImage deepCopy(BufferedImage input) {
        BufferedImage copy = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
        Graphics g = copy.getGraphics();
        g.drawImage(input, 0, 0, null);
        g.dispose();
        return copy;
    }

    // ========================================================================
    // GŁÓWNE OPERACJE MORFOLOGICZNE
    // ========================================================================

    /**
     * Stosuje Erozję (A ⊖ B).
     * Wynikowy piksel (x, y) jest 1, jeśli SE(B) z początkiem w (x, y)
     * w całości mieści się w obiekcie (A).
     */
    public static BufferedImage applyErosion(BufferedImage input, int[][] se, int originX, int originY) {
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
        int width = input.getWidth();
        int height = input.getHeight();
        int seRows = se.length;
        int seCols = se[0].length;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                boolean fits = true; // Zakładamy, że SE pasuje
                for (int j = 0; j < seRows; j++) {
                    for (int i = 0; i < seCols; i++) {

                        // Sprawdzamy tylko piksele '1' w SE
                        if (se[j][i] == FOREGROUND) {
                            int imgX = x + i - originX;
                            int imgY = y + j - originY;

                            // Jeśli piksel SE '1' trafi na tło '0' obrazu, to SE nie pasuje
                            if (!isPixelOn(input, imgX, imgY)) {
                                fits = false;
                                break;
                            }
                        }
                    }
                    if (!fits) break;
                }

                // Jeśli SE w całości pasowało, ustaw piksel wyjściowy na biały
                if (fits) {
                    output.setRGB(x, y, COLOR_WHITE);
                } else {
                    output.setRGB(x, y, COLOR_BLACK);
                }
            }
        }
        return output;
    }

    /**
     * Stosuje Dylatację (A ⊕ B).
     * Używa definicji z odbiciem: A ⊕ B = {z | (B̂)_z ∩ A ≠ Ø}
     * Piksel (x, y) jest 1, jeśli odbite SE(B̂) z początkiem w (x, y)
     * "zahacza" (ma część wspólną) o obiekt (A).
     */
    public static BufferedImage applyDilation(BufferedImage input, int[][] se, int originX, int originY) {
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
        int width = input.getWidth();
        int height = input.getHeight();
        int seRows = se.length;
        int seCols = se[0].length;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                boolean hits = false; // Zakładamy, że SE nie trafia
                for (int j = 0; j < seRows; j++) {
                    for (int i = 0; i < seCols; i++) {

                        // Sprawdzamy tylko piksele '1' w SE
                        if (se[j][i] == FOREGROUND) {
                            // Stosujemy odbicie B̂ (reflection)
                            // (i, j) w SE -> (originX - i, originY - j) względem początku
                            // Przesunięcie o (x, y):
                            int imgX = x + (originX - i);
                            int imgY = y + (originY - j);

                            // Jeśli piksel SE '1' trafi na '1' obrazu, to mamy trafienie
                            if (isPixelOn(input, imgX, imgY)) {
                                hits = true;
                                break;
                            }
                        }
                    }
                    if (hits) break;
                }

                // Jeśli SE trafiło, ustaw piksel wyjściowy na biały
                if (hits) {
                    output.setRGB(x, y, COLOR_WHITE);
                } else {
                    output.setRGB(x, y, COLOR_BLACK);
                }
            }
        }
        return output;
    }

    /**
     * Stosuje Otwarcie (A ○ B) = (A ⊖ B) ⊕ B
     * (Erozja, a następnie Dylatacja tym samym SE)
     */
    public static BufferedImage applyOpening(BufferedImage input, int[][] se, int originX, int originY) {
        BufferedImage eroded = applyErosion(input, se, originX, originY);
        return applyDilation(eroded, se, originX, originY);
    }

    /**
     * Stosuje Domknięcie (A ● B) = (A ⊕ B) ⊖ B
     * (Dylatacja, a następnie Erozja tym samym SE)
     */
    public static BufferedImage applyClosing(BufferedImage input, int[][] se, int originX, int originY) {
        BufferedImage dilated = applyDilation(input, se, originX, originY);
        return applyErosion(dilated, se, originX, originY);
    }

    /**
     * Stosuje transformatę Hit-or-Miss (A ⊛ B).
     * B musi zawierać 1 (pierwszy plan), 0 (tło) i opcjonalnie X (ignoruj).
     * * HMT = (A ⊖ B1) ∩ (A^c ⊖ B2)
     * Gdzie B1 to piksele '1' z SE, a B2 to piksele '0' z SE.
     * A^c to dopełnienie (negatyw) obrazu A.
     */
    public static BufferedImage applyHitOrMiss(BufferedImage input, int[][] se, int originX, int originY) {
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), input.getType());
        int width = input.getWidth();
        int height = input.getHeight();
        int seRows = se.length;
        int seCols = se[0].length;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                boolean hit = true;  // Dopasowanie do '1' (foreground)
                boolean miss = true; // Dopasowanie do '0' (background)

                for (int j = 0; j < seRows; j++) {
                    for (int i = 0; i < seCols; i++) {

                        int imgX = x + i - originX;
                        int imgY = y + j - originY;

                        // Sprawdź warunek 'Hit' (dla SE = 1)
                        if (se[j][i] == FOREGROUND) {
                            if (!isPixelOn(input, imgX, imgY)) {
                                hit = false;
                            }
                        }
                        // Sprawdź warunek 'Miss' (dla SE = 0)
                        else if (se[j][i] == BACKGROUND) {
                            if (isPixelOn(input, imgX, imgY)) {
                                miss = false;
                            }
                        }
                        // Piksele 'DONT_CARE' (-1) są ignorowane

                        if (!hit || !miss) break;
                    }
                    if (!hit || !miss) break;
                }

                // Piksel wyjściowy jest 1 tylko jeśli oba warunki (Hit i Miss) są spełnione
                if (hit && miss) {
                    output.setRGB(x, y, COLOR_WHITE);
                } else {
                    output.setRGB(x, y, COLOR_BLACK);
                }
            }
        }
        return output;
    }

    /**
     * Stosuje Pocienianie (A ⊗ B) = A \ (A ⊛ B)
     * (Różnica zbiorów: A ORAZ (NIE HMT))
     */
    public static BufferedImage applyThinning(BufferedImage input, int[][] se, int originX, int originY) {
        BufferedImage hmtResult = applyHitOrMiss(input, se, originX, originY);
        BufferedImage output = deepCopy(input); // Zacznij od kopii oryginału

        int width = input.getWidth();
        int height = input.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Jeśli HMT znalazło dopasowanie (biały piksel)...
                if (isPixelOn(hmtResult, x, y)) {
                    // ...to usuń ten piksel z obrazu wejściowego (ustaw na czarny)
                    output.setRGB(x, y, COLOR_BLACK);
                }
            }
        }
        return output;
    }

    /**
     * Stosuje Pogrubianie (A ⊙ B) = A ∪ (A ⊛ B)
     * (Suma zbiorów: A LUB HMT)
     */
    public static BufferedImage applyThickening(BufferedImage input, int[][] se, int originX, int originY) {
        // Uwaga: Pogrubianie zwykle działa na dopełnieniu obrazu.
        // Ta implementacja (A ∪ HMT) jest zgodna z definicją, ale
        // SE musi być zdefiniowane do znajdowania tła, które ma stać się obiektem.

        BufferedImage hmtResult = applyHitOrMiss(input, se, originX, originY);
        BufferedImage output = deepCopy(input); // Zacznij od kopii oryginału

        int width = input.getWidth();
        int height = input.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Jeśli HMT znalazło dopasowanie (biały piksel)...
                if (isPixelOn(hmtResult, x, y)) {
                    // ...to dodaj ten piksel do obrazu wejściowego (ustaw na biały)
                    output.setRGB(x, y, COLOR_WHITE);
                }
            }
        }
        return output;
    }
}