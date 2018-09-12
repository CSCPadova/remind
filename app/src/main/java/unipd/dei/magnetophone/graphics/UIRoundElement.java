package unipd.dei.magnetophone.graphics;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * @author daniele
 */
public class UIRoundElement extends UIBaseElement {

    /* Anche se posso risalire al raggio dividendo per due altezza o larghezza,
     * lo salvo in una variabile a sè così da evitare di dover eseguire molte divisioni
     * in animate che mi rallenterebbe il ciclo di refresh.
     */
    protected int radius, inverseSpeed;
    protected float angSpeed, linSpeed; // Velocità angolare in °/s e lineare in px/s

    protected boolean needUpdate;

    protected Matrix matrix;

    /**
     * @param x
     * @param y
     * @param zIndex
     * @param r
     * @param res
     */
    public UIRoundElement(int x, int y, int zIndex, int r, Drawable res) {
        super(x - r, y - r, zIndex, r * 2, r * 2, res);

        this.radius = r;

        this.angSpeed = 0;
        this.linSpeed = 0;

        this.inverseSpeed = 1;

        this.needUpdate = true;

        this.matrix = new Matrix();
    }

    /**
     * IL raggio dell'elemento corrente.
     * Può essere diverso dal raggio dell'immagine.
     *
     * @return Il raggio dell'elemento
     */
    public int getRadius() {
        return radius;
    }

    /**
     * Imposta il raggio dell'elemento rotondo.
     *
     * @param r Valore del raggio da impostare
     */
    public void setRadius(int r) {
        if (r == 0) return;

        // Ridimensiona il rect in tutte le direzioni, mantendo fermo il centro.
        rect.inset(radius - r, radius - r);

        // Imposta il nuovo raggop
        radius = r;

        // Segnala che bisogna rifare i calcoli
        needUpdate = true;

        /*
         * NOTA: Non è necessario scalare la matrice di trasformazione
         * poichè quando verrà richiamato il metodo draw() verrà impostato
         * il rettangolo di bound del Drawable a (x, y, x+w, y+h) che scalerà
         * e traslerà automaticamente la grafica senza bisogno di altre trasformazioni.
         */
    }

    /**
     * Ritorna Vero se il raggio o la velocità sono cambiati dall'ultima animazione
     *
     * @return
     */
    public boolean hasChanged() {
        return needUpdate;
    }

    /**
     * Imposta se l'oggetto dovrà routare nel senso opposto a quello indicato
     * dalla velocità
     *
     * @param invert Vero se deve invertire, Falso altrimenti
     */
    public void setInvertedSpeed(boolean invert) {
        inverseSpeed = invert ? -1 : 1;
    }

    /**
     * Ruota l'elemento in senso orario della quantità indicata
     *
     * @param deg Gradi di rotazione. Può essere negativo.
     */
    public void rotate(float deg) {
        // Routa attorno al centro dell'oggetto
        matrix.postRotate(deg, getCenterX(), getCenterY());
    }

    /**
     * Imposta la velocità di rotazione in gradi al secondo.
     *
     * @param speed
     */
    public void setAngularSpeed(float speed) {
        /* Converto la velocità in gradi al millisecondo.
         * Così evito di dover fare una divisione in animate
         * Che viene richiamato molte più volte
         */
        angSpeed = inverseSpeed * speed / 1000.0f;
    }

    /**
     * @param speed Velocità lineare in cm/s
     */
    public void setLinearSpeed(float speed) {
        // Salvo da mm/s a px/s
        linSpeed = convertFromMetric(speed * 10);

        needUpdate = true;
    }

    @Override
    public boolean animate(float frameTime) {
        if (needUpdate) {
            float speed = (float) Math.toDegrees(linSpeed / getRadius());

            setAngularSpeed(speed);
        }

        needUpdate = false;

        if (angSpeed != 0) {
            rotate(angSpeed * frameTime);

            return true;
        } else {
            return false;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        int savestate = canvas.save();

        canvas.concat(matrix);

        drawTransformed(canvas);

        canvas.restoreToCount(savestate);
    }

    @Override
    public boolean isPressed() {
        return false;
    }


    @Override
    public boolean isReleased() {
        return false;
    }

    protected void drawTransformed(Canvas canvas) {
        super.draw(canvas);
    }


    @Override
    protected boolean inBounds(int x, int y) {
        float dx = (float) (x - getCenterX()) / getRadius();
        float dy = (float) (y - getCenterY()) / getRadius();

        return (dx * dx + dy * dy) <= 1.0f;

    }

    @Override
    protected boolean processTouch(MotionEvent e, int x, int y) {
        return false;
    }
}
