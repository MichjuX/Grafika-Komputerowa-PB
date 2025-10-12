package ms.paint.model;

import java.awt.*;

public class LineObject extends ShapeObject {
    private Point start, end;

    public LineObject(Point start, Point end) {
        this.start = new Point(start);
        this.end = new Point(end);
    }

    @Override
    public void draw(Graphics2D g, boolean scaleMode) {
        g.setColor(color);
        g.drawLine(start.x, start.y, end.x, end.y);

        if (selected || scaleMode) {
            g.setColor(Color.RED);
            int size = scaleMode ? 10 : 6;
            g.fillRect(start.x - size / 2, start.y - size / 2, size, size);
            g.fillRect(end.x - size / 2, end.y - size / 2, size, size);
        }
    }

    @Override
    public boolean contains(Point p) {
        double dist = ptLineDist(start, end, p);
        return dist < 5;
    }

    private double ptLineDist(Point a, Point b, Point p) {
        double num = Math.abs((b.y - a.y) * p.x - (b.x - a.x) * p.y + b.x * a.y - b.y * a.x);
        double den = a.distance(b);
        return num / den;
    }

    @Override
    public void move(int dx, int dy) {
        start.translate(dx, dy);
        end.translate(dx, dy);
    }

    @Override
    public void resize(Point a, Point b) {
        start = new Point(a);
        end = new Point(b);
    }

    @Override
    public void scale(Point from, Point to) {
        int dx = to.x - from.x;
        int dy = to.y - from.y;
        end.translate(dx, dy);
    }
}



