package unipd.dei.magnetophone.graphics;

import android.graphics.Canvas;

/**
 * Oggetto che non fa nulla e rappresenta un punto nello spazio.
 * Viene considerato rotondo ma tutto ciò che riguarda la rotazione non ha effetto.
 * @author daniele
 *
 */
public class UIPoint extends UIRoundElement {

	public UIPoint(int x, int y, int zIndex){
		super(x, y, zIndex, 0, null);
	}
	
	@Override public boolean hasChanged() { return false; }
	
	@Override public void setRadius(int r) { }
	
	@Override public void setLinearSpeed(float speed) { }
	
	@Override public boolean animate(float frameTime) { return false; }
	
	@Override public void draw(Canvas canvas) { }

}
