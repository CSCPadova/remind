package unipd.dei.magnetophone.graphics;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;

/**
 * @author daniele
 */
public class UIButton extends UIBaseElement {

    private Bitmap downRes, upRes;
    private boolean pressed, latching, changed, released;

    public UIButton(int x, int y, int zIndex, int w, int h, Drawable normal, Drawable pressed) {
        super(x, y, zIndex, w, h, normal);

        this.upRes = resource;
        this.downRes = pressed == null ? resource : prepareBitmap(pressed);

        this.pressed = false;
        this.latching = false;
    }

    /**
     * Imposta il comportamento del pulsante.
     * Un pulsante "latched" una volta premuto rimane in questo stato
     * Un pulsante "momentary" ritorna allo stato iniziale dopo aver alzato il dito.
     *
     * @param latch
     */
    public void setLatching(boolean latch) {
        latching = latch;
    }

    public void release() {
        pressed = false;
    }

    @Override
    public boolean animate(float frameTime) {
        if (changed) {
            resource = pressed ? downRes : upRes;

            changed = false;

            return true;
        }

        return false;
    }

    @Override
    public boolean isPressed() {
        return pressed;
    }

    @Override
    public boolean isReleased() {
        return released;
    }


    @Override
    protected boolean processTouch(MotionEvent e, int x, int y) {
        boolean old = pressed;
        if (e.getAction() == MotionEvent.ACTION_DOWN && !pressed) {
            Log.d("DEBUG TOUCH", "ACTION_DOWN");
            pressed = true;
            released = false;
        } else if (e.getAction() == MotionEvent.ACTION_UP && !released) {

            Log.d("DEBUG TOUCH", "MotionEvent.ACTION_UP");
            pressed = false;
            released = true;
        } else {
            return false;
        }

        changed = (old != pressed);

        return true;
    }

}
