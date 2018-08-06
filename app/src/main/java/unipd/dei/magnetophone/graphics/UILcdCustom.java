package unipd.dei.magnetophone.graphics;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;

import java.util.concurrent.TimeUnit;

public class UILcdCustom extends UILcd {

    public UILcdCustom(int x, int y, int zIndex, int w, int h, Drawable res, int numberDigits) {
        super(x, y, zIndex, w + 1, h + 1, res);

        if (numberDigits < 5)
            return;

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
        long millis = Math.abs((long) (seconds * 1000));

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);

        long hour = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hour);

        long min = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(min);

        long sec = TimeUnit.MILLISECONDS.toSeconds(millis);

        if(seconds >= 0)
            digits[0] = (int)hour;
        else
            digits[0] = 10;

        digits[1]=(int)(min / 10);
        digits[2]=(int)(min % 10);
        digits[3]=(int)(sec / 10);
        digits[4]=(int)(sec % 10);
        int i=digits.length-1;
        while (i>=5 ) {
            digits[i]=(int)( millis % 10);
            millis = millis / 10;
            i--;
        }
        //Log.d("DEBUG", "DIGITS: " + digits[0] + "|" + digits[1] + "|" + digits[2] + "|"
        //        + digits[3] + "|" + digits[4] + "|" + digits[5] + "|" + digits[6] + "|" + digits[7]);

        changed = true;
    }
}
