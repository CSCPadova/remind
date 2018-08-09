package unipd.dei.magnetophone;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import unipd.dei.magnetophone.Song.SongSpeed;
import unipd.dei.magnetophone.Song.SongType;

public class MusicService extends Service {
    public static final int ONGOING_NOTIFICATION_ID = 222;
    public static boolean RUNNING = false;
    public static int PLAYBACK_STATE_INITIALIZED = 0;
    public static int PLAYBACK_STATE_PAUSED = 1;
    public static int PLAYBACK_STATE_PLAYING = 2;

    public static final String EXT_STORAGE_EQU_FOLDER = "MagnetophoneEqu";

    static {
        System.loadLibrary("native-player");
    }

    public SongSpeed currentSpeed = SongSpeed.SONG_SPEED_3_75;
    public PlayerEqualization playerEqualization = PlayerEqualization.FLAT;
    Notification not;
    private Song currentSong;
    private OnTimeUpdateListener listener = null;
    private OnSongLoadedListener songLoadedListener = null;
    private OnPlaybackChangeListener playbackChangeListener = null;
    private OnSpeedChangeListener speedChangeListener = null;

    public native void init();

    public native void loadSong(String paths[], int songType, int songSpeed, String equalization, String equPath);

    public native void unloadSong();

    public native void play();

    public native void stop();

    public native int getPlaybackState();

    public native int getTime();

    public native int setChannelEnabled(int channel, int enabled);

    public native void setTrackChannel(int track, int channel);

    public native int[] getTrackMap();

    public native int getChannelEnabled(int i);

    public native void setSpeed(int speed);

    public native void setEqualization(String eq);

    public native void terminate();

    public native void fastForward();

    public native void fastReverse();

    public native float getRatio();

    public native void setTrackVolume(int track, float volumeL, float volumeR);

    public native float getTrackVolumeL(int track);

    public native float getTrackVolumeR(int track);

    public void onCreate() {
        super.onCreate();
        Log.d("MusicService", "creato musicservice");

        Intent notificationIntent = new Intent(MusicService.this, MagnetophoneActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        PendingIntent pendingIntent = PendingIntent.getActivity(MusicService.this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        not = new Notification.Builder(MusicService.this)
                .setContentTitle("Magnetofono")
                .setContentText("magnetofono in riproduzione")
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentIntent(pendingIntent).build();

        init();
        MusicService.RUNNING = true;
    }

    public void onDestroy() {
        Log.d("MusicService", "distrutto musicservice");
        songLoadedListener = null;
        playbackChangeListener = null;
        speedChangeListener = null;
        stop();
        unloadSong();
        terminate();
        super.onDestroy();
    }

    public void onTimeUpdate(double time) {
        if (listener != null)
            listener.onTimeUpdate(time);
    }

    public void playbackStateCallback(int type, boolean stop) {
        switch (type) {
            case 0:
                if (playbackChangeListener != null) {
                    if (stop)
                        playbackChangeListener.onStop();
                    else
                        playbackChangeListener.onPlay();
                }
                break;

            default:
                playbackChangeListener.onFast(stop);
                break;
        }
    }

    public void songSpeedCallback() {
        if (speedChangeListener != null) {
            Log.d("MusicService", "onSongSpeedChanged service");
            speedChangeListener.onSpeedChange(currentSpeed);
        }
    }

    public void songLoadedCallback() {
        if (songLoadedListener != null)
            songLoadedListener.execute(currentSong);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new MusicServiceBinder();
    }

    public interface OnTimeUpdateListener {
        public void onTimeUpdate(double time);
    }

    public interface OnSongLoadedListener {
        public void execute(Song song);
    }

    public interface OnPlaybackChangeListener {
        public void onPlay();

        public void onStop();

        public void onFast(boolean reverse);
    }

    public interface OnSpeedChangeListener {
        public void onSpeedChange(SongSpeed speed);
    }

    public class MusicServiceBinder extends Binder {
        public int getPlaybackState() {
            return MusicService.this.getPlaybackState();
        }

        public void play() {
            MusicService.this.play();
        }

        public void setTrackVolume(int track, float volumeL, float volumeR) {
            MusicService.this.setTrackVolume(track, volumeL, volumeR);
        }

        public void setTrackVolumeL(int track, float volumeL) {
            MusicService.this.setTrackVolume(track, volumeL, MusicService.this.getTrackVolumeR(track));
        }

        public void setTrackVolumeR(int track, float volumeR) {
            MusicService.this.setTrackVolume(track, MusicService.this.getTrackVolumeL(track), volumeR);
        }

        public float getTrackVolumeL(int track) {
            return MusicService.this.getTrackVolumeL(track);
        }

        public float getTrackVolumeR(int track) {
            return MusicService.this.getTrackVolumeR(track);
        }

        public void stop() {
            MusicService.this.stop();
        }

        public Song getSong() {
            return MusicService.this.currentSong;
        }

        public void setSong(Song songToPlay) {
            if (songToPlay == null || (currentSong != null && songToPlay.getId() == currentSong.getId()))
                return;

            Log.d("MusicService", "chiamato setSong");
            MusicService.this.stop();
            MusicService.this.unloadSong();
            MusicService.this.currentSong = songToPlay;

            // verifico il tipo di song
            SongType songType = currentSong.getSongType();
            if (songType == null)
                return;

            String pathsArray[] = new String[currentSong.getTrackList().size()];

            for (int i = 0; i < currentSong.getTrackList().size(); i++)
                pathsArray[i] = currentSong.getTrackList().get(i).getPath();

            currentSpeed = Song.getEnumSpeed(currentSong.getSpeed());
            Log.d("MusicService", "speed " + currentSpeed.getValue());

            String outDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + EXT_STORAGE_EQU_FOLDER;
            MusicService.this.loadSong(pathsArray, songType.getValue(), currentSpeed.getValue(), playerEqualization.name(), outDir);
        }

        public void addOnTimeUpdateListener(OnTimeUpdateListener l) {
            MusicService.this.listener = l;
        }

        public void removeOnTimeUpdateListener() {
            MusicService.this.listener = null;
        }

        public int getTime() {
            return MusicService.this.getTime();
        }

        public static final String NOTIFICATION_CHANNEL_ID_SERVICE = "dunipd.dei.magnetophone.MusicService";
        public static final String NOTIFICATION_CHANNEL_ID_INFO = "com.package.download_info";

        public void startForeground() {
            //MusicService.this.startForeground(ONGOING_NOTIFICATION_ID, not);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                nm.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID_SERVICE, "App Service", NotificationManager.IMPORTANCE_DEFAULT));
            } else {
                MusicService.this.startForeground(1, not);
            }
        }

        public void stopForeground() {
            MusicService.this.stopForeground(true);
        }

        public void stopService() {
            MusicService.this.stopSelf();
        }

        public void setEqualization(PlayerEqualization eq) {
            MusicService.this.playerEqualization = eq;
            MusicService.this.setEqualization(eq.name());
        }

        public SongSpeed getSpeed() {
            return currentSpeed;
        }

        public void setSpeed(SongSpeed s) {
            MusicService.this.currentSpeed = s;
            MusicService.this.setSpeed(s.getValue());
        }

        public void fastForward() {
            MusicService.this.fastForward();
        }

        public void fastReverse() {
            MusicService.this.fastReverse();
        }

        public void setOnSongLoadedListener(OnSongLoadedListener onSongLoadedListener) {
            MusicService.this.songLoadedListener = onSongLoadedListener;
        }

        public void setOnPlaybackChangeListener(OnPlaybackChangeListener onPlaybackChangeListener) {
            MusicService.this.playbackChangeListener = onPlaybackChangeListener;
        }

        public void setOnSpeedChangeListener(OnSpeedChangeListener onSpeedChangeListener) {
            MusicService.this.speedChangeListener = onSpeedChangeListener;
        }

        public float getRatio() {
            return MusicService.this.getRatio();
        }
    }
}
