package unipd.dei.magnetophone.graphics;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

/**
 * View su cui verrà disegnata la grafica del magnetofono.
 * Questa classa si occuppa di gestire tutti i componenti dell'interfaccia,
 * animandoli e disengnandoli ad un frame rate costante.
 *
 * @author daniele
 */
public class MagnetoCanvasView extends View implements Runnable {

    public final static int SCREEN_WIDTH = 2560;
    public final static int SCREEN_HEIGHT = 1600;

    // Handler che riceverà i messaggi di refresh dal thread
    private Handler handler;
    // Thread che si occuperà del timing dei refresh
    private Thread thread;
    // Flag che indica se il thread di refreh è attivo
    private boolean running;

    // Matrice di trasformazione. Scala tutto da SCREEN_WIDTHxSCREEN_HEIGHT.
    private Matrix matrix;

    // Ultimo istante in cui ho animato l'interfaccia
    private long lastFrame;

    // Rapporto fra le dimensioni scalate e quelle effettive della view
    private float sizeRatio;

    private TapeDeck tapeDeck;

    private VideoController videoView;

    private Rect updateArea;

    /**
     * Costruttore di classe
     *
     * @param context
     * @param attrs
     */
    public MagnetoCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);

        matrix = new Matrix();

        running = false;

        updateArea = new Rect();

        // Handler che si occuperà di invalidare la view quando il thread glielo comunicherà
        // Non posso invalidare direttamente dal thread secondario perchè è una operazione che va fatta dal
        // thread che ha creato la view.
        handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                long now;

                // Un piccolo controllo, forse non necessario
                if (message.what == 64) {
                    // TODO: Animare qui gli oggetti va bene?
                    now = System.currentTimeMillis();

                    tapeDeck.animate(now - lastFrame);

                    lastFrame = System.currentTimeMillis();

                    // Forza la view a ridisegnarsi solo dove è effettivamente cambiata
                    invalidate(mapRect(tapeDeck.getBoundingRect()));
                }
            }
        };

        tapeDeck = new StuderTapeDeck(context);
    }

    private Rect mapRect(Rect rect) {
        updateArea.set((int) (rect.left / sizeRatio),
                (int) (rect.top / sizeRatio),
                (int) (rect.right / sizeRatio),
                (int) (rect.bottom / sizeRatio));

        return updateArea;
    }

    /**
     * Evento generato quando cambia la dimensione della view (es: viene girato lo schermo)
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        /* Per il canvas a quanto pare (0,0) è il bordo dello schermo e non
         * quello della view a cui appartiene. Quindi devo traslare la trasformazione
         * alla posizione della view.
         */
        int l = this.getLeft();
        int t = this.getTop();

        // Mappo l'area disegnabile ad un rettangolo pari alla risoluzione dello schermo del tablet di riferimento
        matrix.setRectToRect(new RectF(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT), new RectF(l, t, l + w, t + h), ScaleToFit.CENTER);

        // Basta calcolarlo per una dimensione visto la view mantiene (per design) lo stesso aspect ratio delle dimensioni scelte
        sizeRatio = SCREEN_WIDTH / (float) w;

        if (videoView != null) {
            RectF r = new RectF(tapeDeck.getVideoViewRect());
            Rect dest = new Rect();

            matrix.mapRect(r);

            r.round(dest);

            Log.d("MagnetoCanvasView", "onSizeChanged: Setting videoview to " + dest);

            videoView.onViewSizeChanged(dest);
        }
    }

    /**
     * Evento richiamato dal sistema per misurare le dimensioni della view.
     * Forzo le proporzioni a 16:10 (come sarebbe uno schermo 2560x1600)
     * modificando la larghezza della view.
     * NOTA: Questo mi serve per poter ricevere gli eventi touch solo nell'area utile,
     * evitando di dover fare molti calcoli.
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Lascio misurare normalmente...
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // ...così ho l'altezza della view...
        int height = getMeasuredHeight();
        // ...e posso calcolarmi la larghezza proporzionata...
        int width = Math.round(height * (float) SCREEN_WIDTH / (float) SCREEN_HEIGHT);

        // ...per comunicare al sistema le nuove dimensioni.
        setMeasuredDimension(width, height);
    }

    /**
     * Evento generato quando è richiesto un ridisegno della view.
     * Qui avvengono tutte le operazioni grafiche e viene aggiornato lo stato dei componenti
     * dell'interfaccia (rotazioni, traslazioni, ecc)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Imposto la matrice di trasformazione al canvas.
        canvas.setMatrix(matrix);

        tapeDeck.draw(canvas);
    }


    public void setVideoView(VideoController v) {
        this.videoView = v;
    }


    /**
     * Abilita le animazioni dell'interfaccia e il thread di refresh della view
     */
    public void enableAnimation() {
        // Indico che il thread deve correre
        running = true;

        // TODO Controllare che il thread non sia già in esecuzione (nel caso fosse possibile che accada)

        // Creo il thread e lo faccio partire
        thread = new Thread(this);
        thread.start();

        tapeDeck.enablePlayerEvents();
    }

    /**
     * Disabilita le animazioni dell'interfaccia e il thread di refresh della view
     */
    public void disableAnimation() {
        boolean retry = true;

        // Comunico che il thread deve smettere di girare
        running = false;

        // Continuo a provare finchè non riesco a farlo smettere
        while (retry) {
            try {
                // Attendo che il thread finisca
                thread.join();

                // Se arrivo qui, è tutto ok
                retry = false;
            } catch (InterruptedException e) {
                // Se invece arrivo qui qualcosa non è andato, riprovo
            }
        }
    }

    /**
     * Ciclo che viene eseguito dal thread.
     * Qui viene invalidata la view a intervalli regolari
     */
    @Override
    public void run() {
        System.err.println("Thread started");

        while (running) {
            // Crea un nuovo messaggio da mandare all'handler (con parametro che verrà controllato poi)...
            Message message = handler.obtainMessage(64);

            // ...e lo manda
            handler.sendMessage(message);

            try {
                // Metto in pausa il thread per il tempo necessario per mantenere il framerate
                Thread.sleep(16); // 16ms sono circa 60fps
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.err.println("Thread stopped");
    }

    /**
     * Evento generato dal sistema quando viene toccato lo schermo.
     * Passo le informazioni all'oggetto Magnetofono dopo aver scalato
     * le coordinate alle dimensioni dello schermo di riferimento
     */
    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int x = Math.round(e.getX() * sizeRatio);
        int y = Math.round(e.getY() * sizeRatio);

        tapeDeck.onTouch(e, x, y);

        return true;
    }
}
