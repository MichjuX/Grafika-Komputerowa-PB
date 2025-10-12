package ms.paint.model;

import java.awt.*;
import java.io.Serializable;

public abstract class ShapeObject implements Serializable {
    protected Color color = Color.BLACK;
    protected boolean selected = false;

    public abstract void draw(Graphics2D g, boolean scaleMode);
    public abstract boolean contains(Point p);
    public abstract void move(int dx, int dy);
    public abstract void resize(Point a, Point b);
    public abstract void scale(Point from, Point to);

    protected boolean isNear(Point a, Point b, int range) {
        return Math.abs(a.x - b.x) < range && Math.abs(a.y - b.y) < range;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}


