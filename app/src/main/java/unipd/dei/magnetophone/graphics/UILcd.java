package unipd.dei.magnetophone.graphics;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

public class UILcd extends UIBaseElement {

    protected Rect digitRect;
    protected Rect[] posRects;
    protected int[] digits;

    protected boolean changed;

    public UILcd(int x, int y, int zIndex, int w, int h, Drawable res, int n) {
        super(x, y, zIndex, w + 1, h + 1, res);
    }

    public UILcd(int x, int y, int zIndex, int w, int h, Drawable res) {
        super(x, y, zIndex, w + 1, h + 1, res);

        // Scalo le dimensioni del tile set all'altezza del compomente
        // e divido per 11 per avere la larghezza scalata di un tile.
        int tileWidth = Math.round((float) h * res.getIntrinsicWidth() / res.getIntrinsicHeight() / 12);

        // Crea il rect grande quanto un digit
        digitRect = new Rect(0, 0, res.getIntrinsicWidth() / 12, res.getIntrinsicHeight());

        // Preparo le varie posizioni delle cifre nel display
        posRects = new Rect[5];

        // Ho cinque digits
        for (int i = 0; i < 5; i++) {
            posRects[i] = new Rect(x + tileWidth * i, y, x + tileWidth * (i + 1), y + h);
        }

        // Preparo l'array con le cifre da visualizzare
        digits = new int[5];


        resource = prepareBitmap(res, res.getIntrinsicWidth(), res.getIntrinsicHeight());

        setTime(0);
    }

    public void setTime(float seconds) {
        int time = Math.abs((int) seconds);

        // Separo i minuti dai secondi
        int hour = (int) (time / 3600.0f);
        int min = (int) ((time % 3600.0f) / 60);
        int sec = (int) (time % 60);

        if ((int) seconds >= 0)
            digits[0] = hour;
        else
            digits[0] = 10;

        digits[1] = min / 10;
        digits[2] = min % 10;

        digits[3] = sec / 10;
        digits[4] = sec % 10;

        changed = true;
    }

    @Override
    public boolean animate(float frameTime) {
        if (changed) {
            changed = false;
            return true;
        }

        return false;
    }

    @Override
    public void draw(Canvas canvas) {

        int offset = digitRect.width();

        //draw the digits
        for (int i = 0; i < posRects.length; i++) {
            digitRect.offsetTo(offset * digits[i], 0);

            canvas.drawBitmap(resource, digitRect, posRects[i], p);
        }

        //draw the dots
        digitRect.offsetTo(offset * 11, 0);

        if (digits[0] != 10)
            canvas.drawBitmap(resource, digitRect, posRects[0], p);

        canvas.drawBitmap(resource, digitRect, posRects[2], p);

        if (digits.length > 5)
            canvas.drawBitmap(resource, digitRect, posRects[4], p);
    }

    @Override
    protected boolean processTouch(MotionEvent e, int x, int y) {
        return false;
    }

    @Override
    public boolean isPressed() {
        return false;
    }


    @Override
    public boolean isReleased() {
        return false;
    }

}
