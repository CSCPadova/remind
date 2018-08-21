package unipd.dei.magnetophone.graphics;

import android.content.Context;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import unipd.dei.magnetophone.utility.Song;

public class VideoView extends SurfaceView implements SurfaceHolder.Callback, Runnable, VideoController {
    //private static final String SAMPLE = Environment.getExternalStorageDirectory() + "/Magnetophone/video.mp4";

    private Thread thread;

    private MediaExtractor extractor;
    private MediaCodec decoder;

    private boolean running, seeking, breakPause;
    private long seekTo, musicPos;
    private float videoOffset;

    private float playbackRate;

    private Rect positionRect;

    private boolean surfaceCreated, pendingPlay;
    private String pendingVideo;
    private long pendingSeek;

    private boolean clearSurfaceView;

    public VideoView(Context context, AttributeSet attrs) {
        super(context, attrs);

        Log.d("VideoView", "Thread created");

        this.getHolder().addCallback(this);

        running = false;
        breakPause = false;

        playbackRate = 1.0f;

        surfaceCreated = false;

        pendingVideo = null;
        pendingSeek = -1;
        pendingPlay = false;
        videoOffset = 0;

        clearSurfaceView = false;
    }


    /* Caricamento ed inizializzazione */


    /**
     * Inizializza la decodifica del video
     *
     * @param videoPath Filename del video da caricare
     */
    private void init(String videoPath) throws IOException {
        if (!surfaceCreated) {
            pendingVideo = videoPath;

            return;
        }

        // Creo un nuovo extractor
        extractor = new MediaExtractor();

        // Imposto il file video sorgente
        try {
            File file = new File(videoPath);
            FileInputStream fis = new FileInputStream(file);

            extractor.setDataSource(fis.getFD());
            fis.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        // Cerco fra tutti gli stream prensenti nel contenitore quello video
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);

            // Controllo il MIME-type dello stream
            String mime = format.getString(MediaFormat.KEY_MIME);

            // dev'essere del tipo video/<formato>
            if (mime.startsWith("video/")) {
                // Uso i sample di questo stream
                extractor.selectTrack(i);

                // Configuro il decoder per il formato di questo stream, disegnandolo sulla surface di questa view.
                decoder = MediaCodec.createDecoderByType(mime);
                decoder.configure(format, getHolder().getSurface(), null, 0);

                // Faccio partire il decoder.
                /* NOTA: all'inizio questo era tra le prime istruzioni in run().
                 *       A quanto pare però dava dei problemi, quindi per ora è stato spostato qui.
                 *       Quello che si voleva ottenere con start()/stop() nel thread era di "spegnere"
                 *       momentaneamente il decoder finchè non ce ne fosse stato il bisogno.
                 */
                decoder.start();

                break;
            }
        }
    }

    /**
     * Rilascia le risorse usate dalla decodifica del video.
     */
    private void release() {
        // Fermo la riproduzione nel caso fosse in corso.
        stop();

        // Rilascio le risorse del decoder.
        if (decoder != null) {
            /* NOTA: Vedi nota per start() in init().
             *       Questo era alla fine di run()
             */
            decoder.stop();
            decoder.release();
        }

        // Rilascio le risorse dell'extractor
        if (extractor != null)
            extractor.release();

        // Segno che non sono più disponibili gli oggetti,
        // onde evitare che eventi esterni generino errori non voluti.
        decoder = null;
        extractor = null;
    }


    /* Controllo della riproduzione */


    /**
     * Inizia/riprende la riproduzione del video.
     * Da usare solo dopo aver chiamato init().
     */
    public void play() {
        if (!surfaceCreated) {
            pendingPlay = true;

            return;
        }

        // Se c'è un video caricato e se il thread non sta già girando
        if (thread == null && extractor != null && decoder != null) {
            // Segnalo al thread che deve girare
            running = true;
            breakPause = true;

            // Creo il thread che farà girare la funzione run() di questa classe
            thread = new Thread(this);

            // Dò il via alla riproduzione
            thread.start();
        }
    }

    /**
     * Ferma la riproduzione del video
     * Da usare solo dopo aver chiamato init().
     */
    public void stop() {
        // Se il thread sta girando
        if (thread != null) {
            boolean retry = true;

            // Comunico che il thread deve smettere di girare
            running = false;
            breakPause = true;

            // Continuo a provare finchè non riesco a farlo smettere
            while (retry) {
                try {
                    // Attendo che il thread finisca
                    thread.join();

                    // Elimino completamente l'oggetto
                    thread = null;

                    // Se arrivo qui, è tutto ok
                    retry = false;
                } catch (InterruptedException e) {
                    // Se invece arrivo qui qualcosa non è andato, riprovo
                }
            }
        }
    }

    /**
     * Cambia la velocità di riproduzione della quantità indicata.
     *
     * @param speed Moltiplicatore di velocità. Può essere negativo.
     */
    public void changeSpeed(float speed) {
        playbackRate = speed;

        // Indico al thread di fermare la pausa
        // nel caso fosse nel periodo di sleep per mantenere il timing
        breakPause = true;
    }

    /**
     * Sposta il punto corrente di riproduzione all'istante indicato.
     *
     * @param to Timestamp a cui saltare, espresso in millisecondi
     */
    public void seek(long to) {
        if (!surfaceCreated) {
            pendingSeek = to;

            return;
        }

        // Se c'è un video caricato
        if (extractor != null && decoder != null) {
            // Trasformo in microsecondi
            seekTo = to * 1000 + (long) (videoOffset * 1000);
            breakPause = true;
        }
    }

    public float getVideoOffset() {

        return videoOffset / 1000.0f;
    }

    public void setVideoOffset(float offset) {
        videoOffset = offset * 1000.0f;
        seeking = true;

        seek(musicPos);
        if (seekTo + musicPos < 0) {
            clearSurfaceView = true;
        }
    }

    /* Eventi della SurfaceView */

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.v("VideoView", "Surface Created");

        surfaceCreated = true;

        if (pendingVideo != null) {
            Log.v("VideoView", "Resuming pending video init");

            try {
                init(pendingVideo);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (pendingSeek > 0)
                seek(pendingSeek);

            if (pendingPlay)
                play();

            pendingVideo = null;
            pendingSeek = -1;
            pendingPlay = false;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v("Resizing", "Surface changed to " + positionRect);

        if (positionRect != null)
            this.layout(positionRect.left, positionRect.top, positionRect.right, positionRect.bottom);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        release();
        surfaceCreated = false;
        Log.v("VideoView", "Surface destroyed");
    }


    /* Codice del thread */

    @Override
    public void run() {
        Log.d("VideoView", "Thread started");

        if (decoder == null) {
            Log.e("VideoView", "Can't find video info!");
            return;
        }

        // Variabili utili al decoding

        ByteBuffer[] inputBuffers = decoder.getInputBuffers();

        boolean doFlush = false;
        int bufferIndex, sampleSize;
        long seekTime = 0, lastPresentationTime = extractor.getSampleTime(), lastNanos = System.currentTimeMillis();

        BufferInfo info = new BufferInfo();

        // Ciclo di deconding.
        // Se è stato dato il segnale di stop (running = false),
        // continua finchè non viene completatata un'eventuale ricerca (seeking = true)
        while (running || seeking) {
            /* - LETTURA ------------------- */

            // Richiedi al decoder su quale buffer dobbiamo caricare il sample corrente
            bufferIndex = decoder.dequeueInputBuffer(10000);

            // Se è un valore valido
            if (bufferIndex >= 0) {
                // Carica il sample nel buffer indicato
                sampleSize = extractor.readSampleData(inputBuffers[bufferIndex], 0);

                // sampleSize è negativo se non ci sono più sample disponibili. Siamo arrivati alla fine del video.
                if (sampleSize < 0) {
                    Log.d("VideoView", "InputBuffer BUFFER_FLAG_END_OF_STREAM");

                    // Segnalo al decoder che siamo arrivati alla fine.
                    decoder.queueInputBuffer(bufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    // Segnalo al decoder che il sample è stato caricato nel buffer indicato.
                    decoder.queueInputBuffer(bufferIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                }
            }

            /* - DECODIFICA ------------------- */

            // Richiedo al decoder su quale buffer in uscita troverò il frame decodificato.
            bufferIndex = decoder.dequeueOutputBuffer(info, 10000);

            // Il valore ritornato può avere più significati
            switch (bufferIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    //Log.d("VideoView", "INFO_OUTPUT_BUFFERS_CHANGED");
                    break;

                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    //Log.d("VideoView", "New format " + decoder.getOutputFormat());
                    break;

                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    //Log.d("VideoView", "No frame ready to be rendered!");
                    // Viva il log!
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    break;

                default:

                    /* - TIMING ------------------- */

                    /* Se arrivo qui il frame è stato decodificato ed è pronto ad essere visualizzato o,
                     * se sono in fase di salto temporale, scartato perchè non siamo vicini abbastanza
                     * al timestamp che cerchiamo.
                     * Infatti è possibile che il timestamp a cui vogliamo saltare non corrisponda ad un keyframe,
                     * per cui per evitare artifatti dobbiamo saltare al keyframe precedente ed elaborare tutti i frame
                     * che seguono fino ad arrivare a quello più vicino all'istante desiderato.
                     */

                    if (seeking) {
                        if (info.presentationTimeUs >= seekTime) {

                            // Non serve cercare più
                            seeking = false;

                            Log.d("VideoView", "Found");
                        }
                    }

                    /* - RENDERING ------------------- */

                    // Segnalo al decoder che ho finito col frame corrente.
                    // Se non sono in ricerca può essere visualizzato.
                    decoder.releaseOutputBuffer(bufferIndex, !seeking);

                    /* - TIMING parte II ------------------- */

                    if (!seeking) {
                        long delta = System.nanoTime() - lastNanos;
                        long waitTime = Math.abs((info.presentationTimeUs - lastPresentationTime)) * 1000;
                        long startTime = System.nanoTime();

                        // Scala waitTime al rate di riproduzione corrente
                        waitTime = Math.round(waitTime / Math.abs(playbackRate)) - delta;

                        //Log.d("VideoView", new SimpleDateFormat("mm:ss.").format(info.presentationTimeUs / 1000) + String.format("%06d - %d", info.presentationTimeUs % 1000000, waitTime));

                        // We use a very simple clock to keep the video FPS, or the video
                        // playback will be too fast
                        while (System.nanoTime() - startTime < waitTime && !breakPause && running) {
                            try {
                                Thread.sleep(0, 10000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                break;
                            }
                        }

                        breakPause = false;
                    }

                    // Aggiorna l'ultima posizione temporale.
                    lastPresentationTime = info.presentationTimeUs;
                    lastNanos = System.nanoTime();
            }


            /* - AVANZAMENTO ------------------- */

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                Log.d("VideoView", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            } else {
                // Se non sono in reverse
                if (playbackRate >= 0) {
                    // Se dall'esterno è stato segnalato di saltare a un timestamp specifico
                    //prestando attenzione all'offset audio-video che si puo' settare
                    //antecedente a zero
                    if (seekTo < -1) {
                        if (seekTo + musicPos * 1000 >= 0) {
                            seekTo = seekTo + musicPos * 1000;
                        }
                        if (clearSurfaceView) {
                            //TODO dovrebbe pulirla invece di mostrare un frame video statico ma non ci riesco
                            clearSurfaceViewCanvas();
                            clearSurfaceView = false;
                            extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                            doFlush = true;
                        }
                    } else if (seekTo >= 0) {
                        // Salto ad keyframe subito precedente al timestamp voluto
                        extractor.seekTo(seekTo, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

                        // Entro in modalità ricerca per cercare di avvicinarmi il più possibile al timestamp senza fare rendering.
                        seeking = true;

                        // Indico di eliminare il lavoro corrente per passare subito alla nuova posizione
                        doFlush = true;

                        // Salvo dove voglio arrivare
                        seekTime = seekTo;

                        // Resetto il tramite con l'esterno così il prossimo giro non ricominicio.
                        seekTo = -1;
                    } else {
                        // Avanza al sample successivo
                        extractor.advance();
                    }

                } else if (bufferIndex >= 0) {
                    // Sono in reverse e posso tornare indietro,
                    // lo faccio saltando al keyframe precedente.
                    extractor.seekTo(info.presentationTimeUs - 1000, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);

                    // Segnalo di fare pulizia, così che vengano elaborati direttamente i frame a cui sono saltato.
                    doFlush = true;

                }

                // Solo ora posso fare un flush del decoder senza incorrere in errori.
                if (doFlush) {
                    decoder.flush();
                    doFlush = false;
                }
            }
        }

        Log.d("VideoView", "Thread ended");
    }

    private void clearSurfaceViewCanvas() {
        //TODO non so come fare
    }

    /* Eventi del MusicService */

    /**
     * Al caricamento di una nuova opera
     */
    @Override
    public void onSongLoaded(Song song) {
        release();

        if (song != null && song.isVideoValid()) {
            String path = song.getVideo().getPath();
            try {
                init(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Quando viene effettuato un salto nella traccia audio
     */
    @Override
    public void onSeek(float position) {
        int pos = (int) (position * 1000);
        seek(pos);
        musicPos = pos;
    }

    @Override
    public void onProgress(float position) {
        musicPos = Math.round(position * 1000);
        //Log.d("VideoView", " musicPos: " + musicPos + "with offset" + videoOffset);
    }

    /**
     * Quando viene cambiata velocità di riproduzione.
     * Sia via controlli audio (Play, Stop, FF, REW),
     * sia cambiando la velocità di scorrimento del nastro.
     *
     * @param multiplier Moltiplicatore di velocità.
     * @param doPlay     Indica se il cambio di velocità comporta anche la riproduzione (ed: FF e REW, ma non STOP o cambio velocità)
     */
    @Override
    public void onPlaybackRateChanged(float multiplier, boolean doPlay) {
        // Al cambio di velocità ne approfitto per sincronizzarmi con la musica.
        // Evito di farlo in fastreverse per evitare problemi.
        if (multiplier >= 0) {
            seek(musicPos);
            if (seekTo < -1 && multiplier > 0)
                seekTo = (long) (seekTo * multiplier);
        }


        Log.d("VideoView", "Change speed to " + multiplier);

        // Cambio velocità
        changeSpeed(multiplier);

        // Se l'evento comporta anche la riproduzione,
        // avvia il video. Serve quando ad esempio finisce il brano,
        // viene fermato audio e video, e viene premuto REW. Il video deve ripartire insieme all'audio.
        // Ferma invece il video solo quando la velocità è nulla.
        if (doPlay)
            play();
        else if (multiplier == 0)
            stop();
    }


    @Override
    public void onTerminate() {
        release();
    }


    @Override
    public void onViewSizeChanged(Rect rect) {
        positionRect = rect;
    }
}
