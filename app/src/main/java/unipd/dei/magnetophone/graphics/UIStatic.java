package unipd.dei.magnetophone.graphics;

import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * Classe-farsa che rappresenta un componente statico dell'interfaccia,
 * come uno sfondo o un logo.
 * @author daniele
 *
 */
public class UIStatic extends UIBaseElement {

	public UIStatic(int x, int y, int zIndex, int w, int h, Drawable res) {
		super(x, y, zIndex, w, h, res);
	}
	
	public UIStatic(int x, int y, int zIndex, int r, Drawable res) {
		super(x - r, y - r, zIndex, r*2, r*2, res);
	}
	
	public UIStatic(int zIndex, Drawable res) {
		super(0, 0, zIndex, 0, 1600, res);
	}

	@Override
	public boolean animate(float frameTime) {
		// Immagine statica -> nessuna animazione!
		return false;
	}
	
	@Override
	protected boolean processTouch(MotionEvent e, int x, int y) { return false; }

}
