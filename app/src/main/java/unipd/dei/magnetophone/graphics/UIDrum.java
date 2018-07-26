package unipd.dei.magnetophone.graphics;

import android.graphics.drawable.Drawable;

/**
 * 
 * @author daniele
 *
 */
public class UIDrum extends UIRoundElement {
	private int innerRadius;
	
	/**
	 * Inizializza un nuovo oggetto Tamburo.
	 * 
	 * @param x Posizione orizzontale del centro del tamburo cilindrico
	 * @param y Posizione verticale del centro del tamburo cilindrico
	 * @param zIndex
	 * @param outR Misura del raggio esterno del tamburo
	 * @param inR Misura del raggio interno del tamburo, a cui passerà il nastro
	 * @param res Drawable rappresentante la grafica da disegnare
	 */
	public UIDrum(int x, int y, int zIndex, int outR, int inR, Drawable res) {
		super(x, y, zIndex, outR, res);
		
		innerRadius = inR;
	}

	/**
	 * Ritorna il raggio interno del tamburo, cioè non il bordo più esterno
	 * ma il raggio a cui passa il nasto.
	 * Il tamburo ha una forma "a clessidra" che nella visuale dall'alto non si nota.
	 * Per cui si vedrà solo la parte più larga dell'oggetto e il nastro che gli andrà sotto e non tangente.
	 * 
	 *  @return Il raggio interno del tamburo.
	 */
	@Override
	public int getRadius() { return innerRadius; }

	public int getOuterRadius() { return radius; }
}
