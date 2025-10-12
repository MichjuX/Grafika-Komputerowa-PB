package ms.paint.model;

import java.awt.*;

public class CircleObject extends ShapeObject {
    private Point center, edge;

    public CircleObject(Point center, Point edge) {
        this.center = new Point(center);
        this.edge = new Point(edge);
    }

    @Override
    public void draw(Graphics2D g, boolean scaleMode) {
        int r = (int) center.distance(edge);
        g.setColor(color);
        g.drawOval(center.x - r, center.y - r, 2 * r, 2 * r);

        if (selected || scaleMode) {
            g.setColor(Color.RED);
            int size = scaleMode ? 10 : 6;
            g.fillRect(center.x - size / 2, center.y - size / 2, size, size);
            g.fillRect(edge.x - size / 2, edge.y - size / 2, size, size);
        }
    }

    @Override
    public boolean contains(Point p) {
        return center.distance(p) <= center.distance(edge);
    }

    @Override
    public void move(int dx, int dy) {
        center.translate(dx, dy);
        edge.translate(dx, dy);
    }

    @Override
    public void resize(Point a, Point b) {
        center = new Point(a);
        edge = new Point(b);
    }

    @Override
    public void scale(Point from, Point to) {
        edge = new Point(to);
    }
}

