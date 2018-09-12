package unipd.dei.magnetophone.graphics;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.view.MotionEvent;

public class UIConnector extends UIComponent {

    private UIRoundElement a, b;

    ;
    private Side side;
    private float dist, angle0, angle;
    private int xc0, yc0, xc1, yc1;
    private int x0, y0, x1, y1;
    private Paint p;

    /**
     * Costruttore di classe
     *
     * @param a    Elemento da connettere. Deve avere coordinata y più bassa
     * @param b    Elemento da connettere. Deve avere coordinata y più alta
     * @param side
     */
    public UIConnector(UIRoundElement a, UIRoundElement b, Side side) {
        super(Math.min(a.zIndex, b.zIndex) - 1);

        // TODO Invertire gli elementi se dati nell'ordine sbagliato

        this.a = a;
        this.b = b;

        this.side = side;

        p = new Paint();
        p.setColor(Color.BLACK);
        p.setStyle(Style.STROKE);
        p.setAntiAlias(true);
        p.setStrokeWidth(5);

        // Per ora dò per scontanto che gli oggetti non traslino

        xc0 = a.getCenterX();
        yc0 = a.getCenterY();
        xc1 = b.getCenterX();
        yc1 = b.getCenterY();

        // distanza fra i centri
        dist = (float) Math.sqrt(Math.pow(yc0 - yc1, 2) + Math.pow(xc0 - xc1, 2));

        // angolo non dipendente dai raggi
        if (yc0 != yc1) {
            angle0 = (float) Math.atan((xc0 - xc1) / (yc0 - yc1));
        } else {
            angle0 = (float) Math.PI / 2;
        }
    }

    @Override
    public boolean animate(float frameTime) {
        // Faccio i conti solo se ne vale la pena
        if (this.isVisible() && a.isVisible() && b.isVisible() && (a.hasChanged() || b.hasChanged())) {

            int r0 = a.getRadius();
            int r1 = b.getRadius();

            // angolo dipendente dai raggi
            angle = (float) Math.acos((r1 - r0) / dist);

            if (r0 > 0) {
                if (side == Side.LEFT_TO_LEFT || side == Side.LEFT_TO_RIGHT) {
                    x0 = (int) (xc0 - Math.sin(angle + angle0) * r0);
                    y0 = (int) (yc0 - Math.cos(angle + angle0) * r0);
                } else {
                    x0 = (int) (xc0 + Math.sin(angle - angle0) * r0);
                    y0 = (int) (yc0 - Math.cos(angle - angle0) * r0);
                }
            } else {
                x0 = xc0;
                y0 = yc0;
            }

            if (r1 > 0) {
                if (side == Side.RIGHT_TO_LEFT || side == Side.LEFT_TO_LEFT) {
                    x1 = (int) (xc1 - Math.sin(angle + angle0) * r1);
                    y1 = (int) (yc1 - Math.cos(angle + angle0) * r1);
                } else {
                    x1 = (int) (xc1 + Math.sin(angle - angle0) * r1);
                    y1 = (int) (yc1 - Math.cos(angle - angle0) * r1);
                }
            } else {
                x1 = xc1;
                y1 = yc1;
            }

            return true;
        }

        return false;
    }

    @Override
    public void draw(Canvas canvas) {
        if (this.isVisible() && a.isVisible() && b.isVisible()) {
            canvas.drawLine(x0, y0, x1, y1, p);
        }
    }

    @Override
    public boolean onTouch(MotionEvent e, int x, int y) {
        return false;
    }

    @Override
    public Rect getBoundingRect() {
        return new Rect(x0, y0, x1, y1);
    }

    @Override
    public boolean isPressed() {
        return false;
    }

    @Override
    public boolean isReleased() {
        return false;
    }

    public enum Side {LEFT_TO_LEFT, LEFT_TO_RIGHT, RIGHT_TO_LEFT, RIGHT_TO_RIGHT}

}
