package unipd.dei.magnetophone;

import unipd.dei.magnetophone.Song.SongSpeed;
import unipd.dei.magnetophone.Song.SongType;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service
{
	public static final int ONGOING_NOTIFICATION_ID = 222;

	static {
		System.loadLibrary("native-player");
	}

	public static boolean RUNNING = false;

	public static int PLAYBACK_STATE_INITIALIZED = 0;
	public static int PLAYBACK_STATE_PAUSED = 1;
	public static int PLAYBACK_STATE_PLAYING = 2;

	public native void init();
	public native void loadSong(String paths[], int songType, int songSpeed, String equalization);
	public native void unloadSong();
	public native void play();
	public native void stop();
	public native int  getPlaybackState();
	public native int  getTime();
	public native int  setSatellitePosition(int channel, int position);
	public native int  setChannelEnabled(int channel, int enabled);
	public native void setTrackChannel(int track, int channel);
	public native int[] getTrackMap();
	public native int  getChannelSatellitePosition(int channel);
	public native int  getChannelEnabled(int i);
	public native void setSpeed(int speed);
	public native void setEqualization(String eq);
	public native void terminate();
	public native void fastForward();
	public native void fastReverse();
	public native float getRatio();

	private Song currentSong;
	Notification not;

	private OnTimeUpdateListener listener = null;
	private OnSongLoadedListener songLoadedListener = null;
	private OnPlaybackChangeListener playbackChangeListener = null;
	private OnSpeedChangeListener speedChangeListener = null;

	public SongSpeed currentSpeed = SongSpeed.SONG_SPEED_3_75;
	public PlayerEqualization playerEqualization = PlayerEqualization.FLAT;

	public void onCreate()
	{
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

	public void onDestroy()
	{
		Log.d("MusicService", "distrutto musicservice");
		songLoadedListener = null;
		playbackChangeListener = null;
		speedChangeListener = null;
		stop();
		unloadSong();
		terminate();
		super.onDestroy();
	}

	public void onTimeUpdate(double time)
	{
		if (listener != null)
			listener.onTimeUpdate(time);
	}

	public void playbackStateCallback(int type, boolean stop)
	{
		switch (type)
		{
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

	public void songSpeedCallback()
	{
		if (speedChangeListener != null)
		{
			Log.d("MusicService", "onSongSpeedChanged service");
			speedChangeListener.onSpeedChange(currentSpeed);
		}
	}

	public void songLoadedCallback()
	{
		if (songLoadedListener != null)
			songLoadedListener.execute(currentSong);
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return new MusicServiceBinder();
	}

	public class MusicServiceBinder extends Binder
	{
		public int getPlaybackState()
		{
			return MusicService.this.getPlaybackState();
		}

		public void play()
		{
			MusicService.this.play();
		}

		public void setSong(Song songToPlay)
		{
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

		public void setSatellitePosition(int satelliteNumber, int position)
		{
			MusicService.this.setSatellitePosition(satelliteNumber, position);
		}

		public void setChannelEnabled(int satelliteNumber, boolean enabled)
		{
			MusicService.this.setChannelEnabled(satelliteNumber, enabled ? 1 : 0);
		}

		public void setTrackChannel(int channelNumber, int trackNumber)
		{
			MusicService.this.setTrackChannel(trackNumber, channelNumber);
		}

		public int[] getTrackMap()
		{
			return MusicService.this.getTrackMap();
		}

		public void stop()
		{
			MusicService.this.stop();
		}

		public Song getSong()
		{
			return MusicService.this.currentSong;
		}

		public int getChannelSatellitePosition(int channel)
		{
			return MusicService.this.getChannelSatellitePosition(channel);
		}

		public boolean getChannelEnabled(int channel)
		{
			return MusicService.this.getChannelEnabled(channel) != 0 ? true : false;
		}

		public void addOnTimeUpdateListener(OnTimeUpdateListener l)
		{
			MusicService.this.listener = l;
		}

		public void removeOnTimeUpdateListener()
		{
			MusicService.this.listener = null;
		}

		public int getTime()
		{
			return MusicService.this.getTime();
		}

		public void startForeground()
		{
			MusicService.this.startForeground(ONGOING_NOTIFICATION_ID, not);
		}

		public void stopForeground()
		{
			MusicService.this.stopForeground(true);
		}

		public void stopService()
		{
			MusicService.this.stopSelf();
		}

		public void setSpeed(SongSpeed s)
		{
			MusicService.this.currentSpeed = s;
			MusicService.this.setSpeed(s.getValue());
		}

		public void setEqualization(PlayerEqualization eq)
		{
			MusicService.this.playerEqualization = eq;
			MusicService.this.setEqualization(eq.name());
		}

		public SongSpeed getSpeed()
		{
			return currentSpeed;
		}

		public void fastForward()
		{
			MusicService.this.fastForward();
		}

		public void fastReverse()
		{
			MusicService.this.fastReverse();
		}

		public void setOnSongLoadedListener(OnSongLoadedListener onSongLoadedListener)
		{
			MusicService.this.songLoadedListener = onSongLoadedListener;
		}

		public void setOnPlaybackChangeListener(OnPlaybackChangeListener onPlaybackChangeListener)
		{
			MusicService.this.playbackChangeListener = onPlaybackChangeListener;
		}

		public void setOnSpeedChangeListener(OnSpeedChangeListener onSpeedChangeListener)
		{
			MusicService.this.speedChangeListener = onSpeedChangeListener;
		}

		public float getRatio()
		{
			return MusicService.this.getRatio();
		}
	}

	public interface OnTimeUpdateListener
	{
		public void onTimeUpdate(double time);
	}

	public interface OnSongLoadedListener
	{
		public void execute(Song song);
	}

	public interface OnPlaybackChangeListener
	{
		public void onPlay();

		public void onStop();

		public void onFast(boolean reverse);
	}

	public interface OnSpeedChangeListener
	{
		public void onSpeedChange(SongSpeed speed);
	}
}
