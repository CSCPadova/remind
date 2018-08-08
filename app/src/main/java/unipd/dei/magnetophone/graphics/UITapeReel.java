package unipd.dei.magnetophone.graphics;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/**
 * Classe cherappresenta una bobina con nastro in quantità variabile.
 * Viene implementate come un oggetto rotondo (nastro arrotolato) che contiene un altro oggetto rotondo (bobina),
 * su cui si riflettono posizione e rotazione del primo.
 * In questo modo tutti i metodi esposti si riferiscono al nastro, che può quindi variare raggio.
 * Questo permette ad oggetti con UIConnector di usare le giuste misure e avere il nastro uscente
 * tangente al nastro arrotolato e non alla bobina che manterrà sempre le stesse dimensioni.
 *
 * @author daniele
 */
public class UITapeReel extends UIRoundElement {
    private static final float TAPE_CAPACITY = 200; // Capacità max in metri di nastro nella bobina
    private static final float MIN_RADIUS_RATIO = 0.29f;
    private static final float MAX_RADIUS_RATIO = 0.95f;

    private UIStatic reel;

    private float k1, k2;
    private float recordingSpeed; // Velocità con cui è stato registrato il nastro. Diversa dalla linear speed attuale

    public UITapeReel(int x, int y, int zIndex, int outR, Drawable res, Drawable innerRes) {
        super(x, y, zIndex, outR, innerRes);

        reel = new UIStatic(x, y, zIndex, outR, res);

        k2 = (float) Math.pow(MIN_RADIUS_RATIO * outR, 2);
        k1 = (float) (Math.pow(MAX_RADIUS_RATIO * outR, 2) - k2) / convertFromMetric(TAPE_CAPACITY * 1000);
    }

    /**
     * Ritorna il rect dell'oggetto più grande (la bobina)
     */
    @Override
    public Rect getBoundingRect() {
        return reel.getBoundingRect();
    }

    /**
     * Imposta la volecità con cui è stato registrato il nastro
     *
     * @param speed Velocità in cm/s
     */
    public void setRecordingSpeed(float speed) {
        recordingSpeed = convertFromMetric(speed * 10);
    }

    public void setTapeTime(float second) {
        float len = recordingSpeed * second;
        float r = (float) Math.sqrt(k1 * len + k2);

        setRadius(Math.round(r));
    }


    @Override
    public void drawTransformed(Canvas canvas) {
        super.drawTransformed(canvas);

        reel.draw(canvas);
    }
}
