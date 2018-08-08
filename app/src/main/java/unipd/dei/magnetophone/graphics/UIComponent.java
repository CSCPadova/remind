package unipd.dei.magnetophone.graphics;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;

/**
 * @author daniele
 * Interfaccia che espone i metodi necessari per fare parte degli oggetti disegnabili
 * dell'interfaccia utente di un magnetofono
 */
public abstract class UIComponent implements Comparable<UIComponent> {

    protected int zIndex;
    protected boolean visible;

    protected ComponentCallback callback;
    protected ComponentCallback callbackActUp;


    protected UIComponent(int zIndex) {
        this.zIndex = zIndex;

        this.callback = null;
        this.visible = true;
    }

    /**
     * @return
     */
    public boolean isVisible() {
        return visible;
    }

    public void setVisibility(boolean show) {
        visible = show;
    }

    /**
     * Implementando l'interfaccia Comparable dò la possibilità
     * a questa classe di essere ordinata naturalmente
     * (cioè posso comparare due oggetti UIComponent senza l'uso di Comparator esterni)
     * Il criterio di ordinamento è il valore di zIndex.
     * Questo mi è comodo quando andrò a creare una lista di componenti da disegnare:
     * infatti usando contenitori tipo TreeSet posso fare in modo che gli elementi vengano
     * ordinati all'inserimento (e quindi una volta per tutte), pronti per essere disegnati.
     */
    @Override
    public int compareTo(UIComponent arg0) {
        return zIndex - arg0.zIndex;
    }

    public void setCallback(ComponentCallback callback) {
        this.callback = callback;
    }

    public void setCallbackActUp(ComponentCallback callback) {
        this.callbackActUp = callback;
    }

    public abstract Rect getBoundingRect();

    public abstract boolean animate(float frameTime);

    public abstract void draw(Canvas canvas);

    public abstract boolean onTouch(MotionEvent e, int x, int y);

    public abstract boolean isPressed();
}
