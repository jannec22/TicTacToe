package s18749.Player.views;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;

public class FieldView extends JComponent {
    private static final long serialVersionUID = 1L;
    private boolean _marked = false;
    private boolean _cross = false;
    private int _x;
    private int _y;

    FieldView(int x, int y) {
        super();
        _x = x;
        _y = y;

        Border border = BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(99, 110, 114));
        setBorder(border);
        setFocusable(false);
        setForeground(Color.LIGHT_GRAY);
    }

    public void setMarked(boolean cross) {
        _marked = true;
        _cross = cross;
    }

    public boolean isMarked() {
        Border border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.LIGHT_GRAY);
        setBorder(border);
        return _marked;
    }

    public char type() {
        return _cross ? 'x' : 'o';
    }

    public String position() {
        return _x + " " + _y;
    }

    public void clear() {
        _cross = false;
        _marked = false;
        setForeground(Color.LIGHT_GRAY);
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Rectangle r = g.getClipBounds();
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fillRect(r.x, r.y, r.width, r.height);

        if (_marked) {
            g2.setColor(getForeground());
            g2.setStroke(new BasicStroke(6));

            if (_cross) {
                g2.drawLine((int) (r.x + r.width * 0.25), (int) (r.x + r.height * 0.25), (int) (r.x + r.width * 0.75),
                        (int) (r.x + r.height * 0.75));
                g2.drawLine((int) (r.x + r.width * 0.25), (int) (r.x + r.height * 0.75), (int) (r.x + r.width * 0.75),
                        (int) (r.x + r.height * 0.25));
            } else {
                g2.drawOval(r.x + 25, r.y + 25, r.width - 50, r.height - 50);
            }
        }

    }

}