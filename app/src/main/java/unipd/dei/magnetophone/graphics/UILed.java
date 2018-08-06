package unipd.dei.magnetophone.graphics;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

public class UILed extends UIBaseElement {
	
	private Bitmap offRes, onRes;
	private boolean state, changed;

	public UILed(int x, int y, int zIndex, int w, int h, Drawable offRes, Drawable onRes) {
		super(x, y, zIndex, w, h, offRes);
		
		this.offRes = resource;
		this.onRes  = onRes == null ? resource : prepareBitmap(onRes);
		
		this.state = false;
	}
	
	public void setState(boolean state) {
		this.state = state;
		
		changed = true;
	}

	@Override
	public boolean animate(float frameTime) {
		if(changed) {
			resource = state ? onRes : offRes;
			
			changed = false;
			
			return true;
		}
		
		return false;
	}

	@Override
	protected boolean processTouch(MotionEvent e, int x, int y) {
		return false;
	}

	@Override
	public boolean isPressed() {
		return false;
	}

}
