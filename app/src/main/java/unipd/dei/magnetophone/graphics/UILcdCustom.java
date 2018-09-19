package unipd.dei.magnetophone.graphics;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;

import java.util.concurrent.TimeUnit;

public class UILcdCustom extends UILcd {

    public UILcdCustom(int x, int y, int zIndex, int w, int h, Drawable res) {
        super(x, y, zIndex, w + 1, h + 1, res, 0);
        int numberDigits=6;

        // Scalo le dimensioni del tile set all'altezza del compomente
        // e divido per 11 per avere la larghezza scalata di un tile.
        int tileWidth = Math.round((float) h * res.getIntrinsicWidth() / res.getIntrinsicHeight() / 12);

        // Crea il rect grande quanto un digit
        digitRect = new Rect(0, 0, res.getIntrinsicWidth() / 12, res.getIntrinsicHeight());

        // Preparo le varie posizioni delle cifre nel display
        posRects = new Rect[numberDigits];

        // Ho #digits digits
        for (int i = 0; i < numberDigits; i++) {
            posRects[i] = new Rect(x + tileWidth * i, y, x + tileWidth * (i + 1), y + h);
        }

        // Preparo l'array con le cifre da visualizzare
        digits = new int[numberDigits];

        resource = prepareBitmap(res, res.getIntrinsicWidth(), res.getIntrinsicHeight());

        setTime(0);
    }

    @Override
    public void setTime(float seconds) {
        Log.d("TAG", "TIME: "+ seconds);
        long millis = Math.abs((long) (seconds * 1000));

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);

        long hour = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hour);

        long min = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(min);

        long sec = TimeUnit.MILLISECONDS.toSeconds(millis);

        if (seconds >= 0) {
            digits[0] = 12;//(int) hour;
        } else {
            digits[0] = 10;
        }
        digits[1] = (int) (min % 10);


        digits[2] = (int) (sec / 10);
        digits[3] = (int) (sec % 10);

        int i = digits.length - 1;
        millis = millis / 10;//toglie le unita' dei millisecondi
        while (i >= 4) {
            digits[i] = (int) (millis % 10);
            millis = millis / 10;
            i--;
        }

        changed = true;
    }

    @Override
    public void draw(Canvas canvas) {

        int offset = digitRect.width();

        //draw the digits
        for (int i = 1; i < posRects.length; i++) {
            digitRect.offsetTo(offset * digits[i], 0);
            canvas.drawBitmap(resource, digitRect, posRects[i], p);
        }
        if (digits[0] == 10){
            digitRect.offsetTo(offset * digits[0], 0);
            canvas.drawBitmap(resource, digitRect, posRects[0], p);
        }

        //draw the dots
        digitRect.offsetTo(offset * 11, 0);

        canvas.drawBitmap(resource, digitRect, posRects[1], p);
        canvas.drawBitmap(resource, digitRect, posRects[3], p);
    }
}
