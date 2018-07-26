package unipd.dei.magnetophone.graphics;

import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

/**
 * 
 * @author daniele
 *
 */
public class UIKnob extends UIRoundElement {
	private int steps, currentStep;
	
	private float fromAngle, toAngle, stepAngle;
	private float touchStartPos;
	
	private boolean moving;
	
	/**
	 * Inizializza una nuova manopola.
	 * Per impostare le sue proprietà usare setSteps()
	 * @param x Posizione orizzontale del centro
	 * @param y Posizione vericale del centro
	 * @param zIndex Indice di profondità
	 * @param r Raggio della manopola
	 * @param res Drawable da disegnare
	 */
	public UIKnob(int x, int y, int zIndex, int r, Drawable res) {
		super(x, y, zIndex, r, res);
		
		setSteps(2, 0, 0);
	}
	
	/**
	 * Riporta il raggio dell'oggetto. Ritornando un raggio 3 volte superiore a quello dell'immagine
	 * rendo più ampia l'area di tocco. Se sto muovendo la manopola allarga a tutto lo schermo.
	 * TODO: ottenere la larghezza dello spazio dal contenitore invece di usare una costante.
	 */
	@Override
	public int getRadius() { return moving ? 2560 : 3 * radius; }

	public int getSelectedStep() { return currentStep; }
	
	public void setStep(int step) {
		currentStep = step;
		
		currentStep = Math.min(currentStep, steps - 1); // currentstep parte da 0
		currentStep = Math.max(currentStep, 0);
		
		needUpdate = true;
	}
	
	/**
	 * Imposta il numero di steps e l'angolo tra il primo e l'ultimo
	 * @param steps Numero di steps totali. Non può essere minore di 2
	 * @param fromAngle Angolo a cui si troverà il primo step
	 * @param toAngle Angolo a cui si troverà l'ultimo step
	 */
	public void setSteps(int steps, float fromAngle, float toAngle) {
		// Limita ad almeno due gli step.
		this.steps = Math.max(steps, 2);  
		
		// Salva i due angoli in ordine crescente
		this.fromAngle = Math.min(fromAngle, toAngle);
		this.toAngle   = Math.max(fromAngle, toAngle);
		
		// Calcola la distanza angolare fra due step
		this.stepAngle = (this.toAngle - this.fromAngle) / (this.steps - 1);
		
		// Imposta come selezionato il primo step
		this.currentStep = 0;
		
		// Non stiamo ancora muovendo il dito sopra l'oggetto
		this.moving = false;
	}
	
	/**
	 * Aggiorna la rotazione dell'oggetto se è cambiato lo step selezionato
	 */
	@Override
	public boolean animate(float frameTime) {
		// Riutilizzo questo flag della classe padre che non uso in questa implementazione
		// (l'oggetto manopola non ruota a velocità costante)
		if(needUpdate) {
			float angle = fromAngle + currentStep * stepAngle;
			
			// Inverto l'angolo perchè per la matrice un angolo positivo voul dire rotazione oraria,
			// mentre per me vuol dire antioraria (perchè uso la convezione della circonferenza goniometrica).
			matrix.reset();
			rotate(-angle);
			
			// Aggiornato, non serve fare di nuovo la stessa cosa
			needUpdate = false;
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Elabora l'evento touch che è stato generato su questo oggetto. 
	 */
	@Override
	protected boolean processTouch(MotionEvent e, int x, int y) {
		// Coordinate relative al centro dell'oggetto
		int dx = x - getCenterX();
		int dy = y - getCenterY();
		
		if(e.getAction() == MotionEvent.ACTION_DOWN) {
			// L'utente ha messo il dito sul controllo
			
			touchStartPos = getAngleFromPosition(dx, dy);
			
			// Segnalo che un dito si è appoggiato e che si muoverà.
			// In questo modo elaboro l'evento MOVE solo quando
			// viene toccato proprio questo oggetto.
			moving = true;
		
		} else if(moving && e.getAction() == MotionEvent.ACTION_MOVE) {
			// L'utente ha mosso il dito
			
			// Calcolo l'angolo a cui mi trovo
			float newAngle = getAngleFromPosition(dx, dy);
			
			// Calcolo di quando mi sono spostato e in che direzione
			// Separo le due cose per semplificare le operazioni successive
			float angleDiff = Math.abs(newAngle - touchStartPos);
			int direction = newAngle >= touchStartPos ? 1 : -1;
			
			/**
			 * Ad ogni step viene aggiornata la posizione iniziale a cui facciamo riferimento.
			 * Questo vuol dire che angleDiff non potrà mai essere oltre stepAngle.
			 * stepAngle sarà 180 o più solo in casi poco utili. (2 step in 180° o 3 step in 360°)
			 */
			
			// Se ho fatto più di mezzo giro probabilmente c'è stato un overflow 0<->360
			if(Math.abs(angleDiff) > 180) {
				angleDiff = 360 - angleDiff;
				direction = -direction;
			}
			
			if(angleDiff > stepAngle) {
				setStep(currentStep + direction);
				
				touchStartPos = newAngle;
				
				return true;
			}
		}
		// Quando l'utente alza il dito
		else if(e.getAction() == MotionEvent.ACTION_UP) {
			moving = false;
		}
		
		return false;
	}

	/**
	 * Calcola l'angolo tra il tocco dell'utente e il centro del knob
	 * @param x posizione x del tocco relativa al centro del knob
	 * @param y posizione y del tocco relativa al centro del knob
	 * @return angolo (in radianti)
	 */
	private float getAngleFromPosition(float x, float y)
	{
		// calcolo il versore del vettore (x, y)
		// il dot product col versore (1,0) mi da il coseno dell'angolo (=> quindi è solo la x del versore)
		// con arccos ottengo l'angolo
		double xVer = x / Math.sqrt(x*x + y*y);
		double ret;
		
		if(y <= 0)
			ret = Math.acos(xVer);
		else
			ret = (2*Math.PI - Math.acos(xVer));
		
		return (float) Math.toDegrees(ret);
	}
}
