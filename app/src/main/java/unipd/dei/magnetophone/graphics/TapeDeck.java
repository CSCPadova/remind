package unipd.dei.magnetophone.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.MotionEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ListIterator;

import unipd.dei.magnetophone.MusicPlayer;

public abstract class TapeDeck extends UIComponent implements AnimationController {
    // Oggetti usati nelle callback. Devono essere final per non avere errori
    protected final Context context;
    protected final MusicPlayer player;
    // TreeSet mantiene ordinati gli oggetti che vengono aggiunti
    // secondo il loro naturale ordinamento. UIComponent espone il metodo
    // compareTo() quindi può essere ordinato secondo il suo zIndex
    private ArrayList<UIComponent> components;
    private Rect updateRect;

    public TapeDeck(Context ctx) {
        super(0);

        this.context = ctx;

        this.player = MusicPlayer.getInstance();

        components = new ArrayList<UIComponent>();

        updateRect = new Rect();
    }

    public void enablePlayerEvents() {
        MusicPlayer.getInstance().setAnimationController(this);
    }

    public void disablePlayerEvents() {
        MusicPlayer.getInstance().setAnimationController(null);
    }

    protected UIComponent addComponent(UIComponent comp) {
        components.add(comp);

        Collections.sort(components);

        return comp;
    }

    @Override
    public Rect getBoundingRect() {
        return updateRect;
    }

    @Override
    public boolean animate(float frameTime) {
        updateRect.setEmpty();

        for (UIComponent component : components) {
            if (component.animate(frameTime)) {
                updateRect.union(component.getBoundingRect());
            }
        }

        return true;
    }

    @Override
    public void draw(Canvas canvas) {
        /* I componenti sono già ordinati per zIndex
         * quindi li posso disegnare come me li ritorna la lista.
         */
        for (UIComponent component : components) {
            if (component.isVisible())
                component.draw(canvas);
        }
    }

    @Override
    public boolean onTouch(MotionEvent e, int x, int y) {
        ListIterator<UIComponent> iter = components.listIterator(components.size());

        boolean processed;

        while (iter.hasPrevious()) {
            processed = iter.previous().onTouch(e, x, y);

            if (processed)
                return true;
        }

        return true;
    }

    public abstract Rect getVideoViewRect();
}
