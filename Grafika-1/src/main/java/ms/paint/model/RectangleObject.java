package ms.paint.model;

import java.awt.*;

public class RectangleObject extends ShapeObject {
    private Point start, end;

    public RectangleObject(Point a, Point b) {
        this.start = new Point(a);
        this.end = new Point(b);
    }

    @Override
    public void draw(Graphics2D g, boolean scaleMode) {
        int x = Math.min(start.x, end.x);
        int y = Math.min(start.y, end.y);
        int w = Math.abs(end.x - start.x);
        int h = Math.abs(end.y - start.y);

        g.setColor(color);
        g.drawRect(x, y, w, h);

        if (selected || scaleMode) {
            g.setColor(Color.RED);
            int size = scaleMode ? 10 : 6;
            g.fillRect(start.x - size / 2, start.y - size / 2, size, size);
            g.fillRect(end.x - size / 2, end.y - size / 2, size, size);
        }
    }

    @Override
    public boolean contains(Point p) {
        Rectangle r = new Rectangle(
                Math.min(start.x, end.x),
                Math.min(start.y, end.y),
                Math.abs(end.x - start.x),
                Math.abs(end.y - start.y)
        );
        return r.contains(p);
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



