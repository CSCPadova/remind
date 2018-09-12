package unipd.dei.magnetophone.graphics;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * @author daniele
 * Elemento base dell'interfaccia utente che può essere posizionato e dimensionato a piacere.
 */
public abstract class UIBaseElement extends UIComponent {

    protected Bitmap resource;
    protected Rect rect;

    protected Paint p;

    /**
     * @param x
     * @param y
     * @param zIndex
     * @param w
     * @param h
     * @param res
     */
    public UIBaseElement(int x, int y, int zIndex, int w, int h, Drawable res) {
        super(zIndex);

        if (res != null && w == 0) {
            // Mantengo le proporzioni dell'immagine
            w = h * res.getIntrinsicWidth() / res.getIntrinsicHeight();
        }

        if (res != null && h == 0) {
            // Mantengo le proporzioni dell'immagine
            h = w * res.getIntrinsicHeight() / res.getIntrinsicWidth();
        }

        rect = new Rect(x, y, x + w, y + h);

        p = new Paint(Paint.FILTER_BITMAP_FLAG);

        resource = prepareBitmap(res);
    }

    /**
     * Crea una bitmap grande quanto l'elemento.
     *
     * @param res Drawable da disegnare
     * @return L'oggetto Bitmap creato
     */
    protected Bitmap prepareBitmap(Drawable res) {
        return prepareBitmap(res, rect.width(), rect.height());
    }

    /**
     * Crea una bitmap delle dimensione indicate partendo da un Drawable
     *
     * @param res Drawable da disegnare
     * @param w   Larghezza della bitmap desiderata
     * @param h   Altezza della bitmap desiderata
     * @return L'oggetto Bitmap creato
     */
    protected Bitmap prepareBitmap(Drawable res, int w, int h) {
        if (res == null)
            return null;

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bmp);

        res.setBounds(0, 0, w, h);
        res.draw(canvas);

        return bmp;
    }

    /**
     * Ottiene la coordinata orizzontale del centro dell'elemento
     *
     * @return Coordinata x del centro
     */
    public int getCenterX() {
        return rect.centerX();
    }

    /**
     * Ottiene la coordinata verticale del centro dell'elemento
     *
     * @return Coordinata y del centro
     */
    public int getCenterY() {
        return rect.centerY();
    }

    /**
     * Converte una misura dai millimetri
     *
     * @param mm
     * @return
     */
    public float convertFromMetric(float mm) {
        // 800px (su un'area 2560x1600) corrispondono a 266mm reali
        return mm * 3.0f;
    }

    @Override
    public Rect getBoundingRect() {
        return rect;
    }

    /**
     * Metodo richiamato per disegnare sul canvas indicato il componente
     */
    @Override
    public void draw(Canvas canvas) {
        if (resource != null) {
            canvas.drawBitmap(resource, null, rect, p);
        }
    }

    /**
     * Animo l'oggetto
     */
    @Override
    public abstract boolean animate(float frameTime);


    /**
     * Evento generato quando c'è stato un evento touch in tutta la superficie.
     * Processa l'evento solo se all'interno dei confini dell'elemento. Vedi processTouch()
     */
    @Override
    public boolean onTouch(MotionEvent e, int x, int y) {
        // Fai qualcosa solo se c'è una callback da richiamare
        if (callback != null && inBounds(x, y)) {
            if (processTouch(e, x, y)) {
                // Richiama la callback
                callback.stateChanged(this);
            }

            return true;
        }
        return false;
    }

    /**
     * Questo metodo controlla se le coordinate indicate si trovano
     * all'interno del componente.
     * Da estendere nel caso l'oggetto non abbia forma rettangolare
     *
     * @param x
     * @param y
     * @return
     */
    protected boolean inBounds(int x, int y) {
        return rect.contains(x, y);
    }

    /**
     * Metodo richiamato quando avviene un evento touch all'interno del componente
     *
     * @param e MotionEvent che descrive l'ev
     *          Log.d("DEBUG","processTouch");ento
     * @param x Coordinata x scalata in cui è avvenuto l'evento
     * @param y Coordinata y scalata in cui è avvenuto l'evento
     * @return
     */
    protected abstract boolean processTouch(MotionEvent e, int x, int y);
}