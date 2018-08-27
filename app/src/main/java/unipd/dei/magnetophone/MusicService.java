package unipd.dei.magnetophone;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import unipd.dei.magnetophone.activity.MagnetophoneActivity;
import unipd.dei.magnetophone.utility.PlayerEqualization;
import unipd.dei.magnetophone.utility.Song;
import unipd.dei.magnetophone.utility.Song.SongSpeed;
import unipd.dei.magnetophone.utility.Song.SongType;

public class MusicService extends Service {
    public static final int ONGOING_NOTIFICATION_ID = 222;
    public static boolean RUNNING = false;
    public static int PLAYBACK_STATE_INITIALIZED = 0;
    public static int PLAYBACK_STATE_PAUSED = 1;
    public static int PLAYBACK_STATE_PLAYING = 2;

    private NotificationCompat.Builder mBuilder;

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

    public native void loadSong(String paths[], int songType, int songSpeed, String equalization);

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

        String channelID = getResources().getString(R.string.CHANNEL_ID);

        mBuilder = new NotificationCompat.Builder(this, channelID)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Magnetofono")
                .setContentText("magnetofono in riproduzione")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

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
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
            notificationManager.cancel(ONGOING_NOTIFICATION_ID);
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

            MusicService.this.loadSong(pathsArray, songType.getValue(), currentSpeed.getValue(), playerEqualization.name());
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
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
            notificationManager.notify(ONGOING_NOTIFICATION_ID, mBuilder.build());
        }

        public void stopForeground() {
            MusicService.this.stopForeground(true);
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
            notificationManager.cancel(ONGOING_NOTIFICATION_ID);
        }

        public void stopService() {
            MusicService.this.stopSelf();
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getBaseContext());
            notificationManager.cancel(ONGOING_NOTIFICATION_ID);
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
