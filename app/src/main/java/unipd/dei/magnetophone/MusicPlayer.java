package unipd.dei.magnetophone;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import unipd.dei.magnetophone.MusicService.MusicServiceBinder;
import unipd.dei.magnetophone.database.DatabaseManager;
import unipd.dei.magnetophone.graphics.AnimationController;
import unipd.dei.magnetophone.graphics.VideoController;
import unipd.dei.magnetophone.utility.PlayerEqualization;
import unipd.dei.magnetophone.utility.Song;
import unipd.dei.magnetophone.utility.Song.SongSpeed;

/**
 * La classe che si preoccupa di gestire la riproduzione dell'audio e
 * simulazione del magnetofono. Poichè solo un oggetto di questo tipo può
 * esistere nell'applicazione, si deve ottenere l'istanza con il metodo statico
 * getInstance().
 */
public class MusicPlayer {
    // ... un singleton ...
    private static MusicPlayer instance = null;
    private final Object serviceAccessLock = new Object(); // Lock per l'accesso al servizio
    private Context context;
    private AnimationController actrl = null;
    private VideoController vctrl = null;
    private float videoOffset = 0;
    private Song songToPlay = null;
    private PlayerEqualization lastEqUsed = PlayerEqualization.NAB;
    private SongSpeed currentSpeed = SongSpeed.SONG_SPEED_30;
    private MusicService.MusicServiceBinder musicServiceBinder = null;
    /**
     * Connessione al servizio in background
     */
    private final ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            musicServiceBinder = (MusicServiceBinder) binder;
            musicServiceBinder.stopForeground();

            /*
             * carico la canzone se è il caso
             */
            SharedPreferences preferences = context.getSharedPreferences("service", Context.MODE_PRIVATE);
            int songId = preferences.getInt("song_id", -1);

            if (songId == -1) {
                //siccome non c'e' nessun nastro pronto a partire, allora nasconde le bobine
                actrl.onSongChanged(null);
                return;
            }

            Song songFromDatabase = DatabaseManager.getSongFromDatabase(songId,
                    context);

            if (songFromDatabase == null)
                return;

            setSong(songFromDatabase);

            Log.d("MusicPlayer", "bind music player");

            musicServiceBinder.setOnSongLoadedListener(new MusicService.OnSongLoadedListener() {
                @Override
                public void execute(Song song) {
                    synchronizeVideoWithSong();
                    if (actrl != null)
                        actrl.onSongChanged(song);

                    if (vctrl != null)
                        vctrl.onSongLoaded(song);
                }
            });

            musicServiceBinder.setOnPlaybackChangeListener(new MusicService.OnPlaybackChangeListener() {
                @Override
                public void onPlay() {
                    synchronizeVideoWithSong();

                    if (actrl != null)
                        actrl.onMusicPlay();

                    float speed = calculateAudioSpeed();

                    if (vctrl != null)
                        vctrl.onPlaybackRateChanged(speed, true);
                }

                @Override
                public void onStop() {
                    if (actrl != null)
                        actrl.onMusicStop();

                    if (vctrl != null) {
                        vctrl.onProgress(getCurrentTimestamp());
                        vctrl.onPlaybackRateChanged(0, false);
                    }
                }

                @Override
                public void onFast(boolean reverse) {
                    if (actrl != null) {
                        float fastSpeed = getFastSpeedMultiplier();

                        if (reverse) {
                            actrl.onMusicFastReverse();
                            vctrl.onPlaybackRateChanged(-fastSpeed, true);
                        } else {
                            actrl.onMusicFastForward();
                            vctrl.onPlaybackRateChanged(fastSpeed, true);
                        }
                    }
                }
            });

            musicServiceBinder.setOnSpeedChangeListener(new MusicService.OnSpeedChangeListener() {
                @Override
                public void onSpeedChange(SongSpeed speed) {
                    currentSpeed = speed;
                    Log.d("MusicPlayer", "onSpeedChange");
                    if (actrl != null) {
                        float seconds = getCurrentTimestamp();
                        actrl.onSongSpeedChanged(speed);
                        actrl.onSongProgress(getCurrentSongProgress(seconds), seconds);
                    }

                    if (vctrl != null)
                        vctrl.onPlaybackRateChanged(calculateAudioSpeed(), isPlaying());
                }
            });

            musicServiceBinder.addOnTimeUpdateListener(new MusicService.OnTimeUpdateListener() {
                @Override
                public void onTimeUpdate(double time) {
                    double seconds = time / 100.0;
                    if (actrl != null)
                        actrl.onSongProgress(getCurrentSongProgress(seconds), (float) seconds);

                    if (vctrl != null)
                        vctrl.onProgress((float) seconds);
                }
            });

            currentSpeed = musicServiceBinder.getSpeed();
            float seconds = getCurrentTimestamp();
            actrl.onSongChanged(musicServiceBinder.getSong());
            actrl.onSongProgress(getCurrentSongProgress(seconds), seconds);
            actrl.onSongSpeedChanged(currentSpeed);

            vctrl.onSongLoaded(musicServiceBinder.getSong());
            vctrl.setVideoOffset(videoOffset);

            synchronizeVideoWithSong();

            if (musicServiceBinder.getPlaybackState() == MusicService.PLAYBACK_STATE_PLAYING)
                play();
            else {
                if (actrl != null)
                    actrl.onMusicStop();

                if (vctrl != null) {
                    vctrl.onProgress(getCurrentTimestamp());
                    vctrl.onPlaybackRateChanged(0, false);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // Funzione chiamata quando il processo del service crasha.
            // Non possiamo fare nient'altro che resettare il riferimento
            musicServiceBinder = null;
        }
    };

    /**
     * Ottiene l'istanza globale di questa classe.
     */
    public synchronized static MusicPlayer getInstance() {
        if (instance == null)
            instance = new MusicPlayer();
        return instance;
    }

    /**
     * Metodo per distruggere il servizio, usato prima di chiamare l'activity
     * del magnetofono che lo crea
     */
    public void restartService() {
        // unbindService();
        context.stopService(new Intent(context, MusicService.class));
        musicServiceBinder = null;
    }

    /**
     * Imposta il contesto da usare per le varie chiamate Android
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Imposta l'AnimationController che deve ricevere gli eventi di stato
     *
     * @param actrl
     */
    public void setAnimationController(AnimationController actrl) {
        this.actrl = actrl;
    }

    /**
     * Imposta il VideoController che deve ricevere gli eventi di stato
     *
     * @param vctrl
     */
    public void setVideoController(VideoController vctrl) {
        this.vctrl = vctrl;
    }

    /**
     * onStart di MagnetophoneActivity
     */
    public void onResume() {
        Log.d("MusicPlayer", "onresume player");
        context.bindService(new Intent(context, MusicService.class), musicServiceConnection, Context.BIND_ABOVE_CLIENT);
    }

    /**
     * onPause di MagnetophoneActivity
     */
    public void onPause() {
        Log.d("MusicPlayer", "onpause player");
        if (musicServiceBinder != null) {
            if (musicServiceBinder.getPlaybackState() == MusicService.PLAYBACK_STATE_PLAYING)
                musicServiceBinder.startForeground();
            else {
                musicServiceBinder.removeOnTimeUpdateListener();
                context.unbindService(musicServiceConnection);
                musicServiceBinder = null;
            }
        }
    }

    /**
     * onBackPressed di MagnetophoneActivity
     */
    public void onBackPressed() {
        if (musicServiceBinder != null && musicServiceBinder.getPlaybackState() != MusicService.PLAYBACK_STATE_PLAYING) {
            musicServiceBinder.stopService();
            context.unbindService(musicServiceConnection);
            musicServiceBinder = null;
        }
    }

    public void synchronizeVideoWithSong() {
        float currentSeconds = getCurrentTimestamp();
        if (vctrl != null)
            vctrl.onSeek(currentSeconds);

    }

    // #####################################################################################################

    /**
     * Ritorna la canzone correntemente caricata
     *
     * @return
     */
    public Song getSong() {
        if (musicServiceBinder != null)
            return musicServiceBinder.getSong();
        return null;
        //return songToPlay;
    }

    /**
     * Imposta il file da riprodurre
     *
     * @param song la canzone da usare
     */
    public void setSong(Song song) {
        Log.d("MusicPlayer", "setSong");
        if (musicServiceBinder != null)
            musicServiceBinder.setSong(song);
        else
            songToPlay = song;
    }

    /**
     * Ritorna la canzone temporanea salvata nel player
     *
     * @return
     */
    public Song getPlayerSong() {
        return songToPlay;
    }

    /**
     * Avvia la riproduzione del file audio
     */
    public void play() {
        if (musicServiceBinder == null)
            return;
        musicServiceBinder.play();
    }

    /**
     * Ritorna true se il riproduttore sta riproducendo una canzone.
     *
     * @return
     */
    public boolean isPlaying() {
        if (isBind())
            return musicServiceBinder.getPlaybackState() == MusicService.PLAYBACK_STATE_PLAYING;
        return false;
    }

    /**
     * Ferma la riproduzione dell'audio
     */
    public void stop() {
        musicServiceBinder.stop();
    }

    /**
     * Mette il riproduttore in riproduzione veloce.
     */
    public void startFastForward() {
        if (musicServiceBinder != null)
            musicServiceBinder.fastForward();
    }

    /**
     * Mette il riproduttore in avvolgimento veloce
     */
    public void startFastReverse() {
        if (musicServiceBinder != null)
            musicServiceBinder.fastReverse();
    }

    /**
     * Ritorna la velocità corrente di riproduzione.
     *
     * @return
     */
    public SongSpeed getCurrentSpeed() {
        if (musicServiceBinder != null)
            return musicServiceBinder.getSpeed();
        return null;
    }

    public float getFastSpeedMultiplier() {
        if (musicServiceBinder != null)
            return musicServiceBinder.getRatio();

        return 1.0f;
    }

    /**
     * Ritorna la velocità corrente di riproduzione.
     *
     * @return
     */
    public PlayerEqualization getCurrentEqualization() {
        synchronized (serviceAccessLock) {
            return lastEqUsed;
        }
    }

    /**
     * Imposta la velocità di riproduzione
     *
     * @param speed la velocità in inch/s
     */
    public void setPlayerSpeed(SongSpeed speed) {
        currentSpeed = speed;
        if (musicServiceBinder != null)
            musicServiceBinder.setSpeed(currentSpeed);
    }

    /**
     * Imposta l'equalizzazione desiderata
     */
    public void setEqualization(PlayerEqualization eq) {
        synchronized (serviceAccessLock) {
            lastEqUsed = eq;
            if (musicServiceBinder != null)
                musicServiceBinder.setEqualization(eq);
        }
    }

    public float getCurrentSongProgress(double seconds) {
        return getScaledTime(seconds);
    }

    public float getScaledTime(double seconds) {
        return (float) (seconds / calculateAudioSpeed());
    }

    /**
     * Calcola la velocità di riproduzione in base alla velocità di
     * registrazione della canzone e la velocità di riproduzione selezionata.
     *
     * @return il fattore moltiplicativo della velocità da impostare (es. 2 = 2 volte più veloce del normale)
     */
    private float calculateAudioSpeed() {
        if (musicServiceBinder.getSong() == null)
            return 1;

        return Song.getFloatSpeed(currentSpeed) / musicServiceBinder.getSong().getSpeed();
    }

    public float getCurrentTimestamp() {
        if (isBind())
            return (float) musicServiceBinder.getTime() / 100.0f;

        return 0;
    }

    public float getVideoSyncOffset() {
        return vctrl.getVideoOffset();
    }

    public void setVideoSyncOffset(float offset) {
        vctrl.setVideoOffset(offset);
    }

    private boolean isBind() {
        return musicServiceBinder != null;
    }
}
