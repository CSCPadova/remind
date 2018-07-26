package unipd.dei.magnetophone.graphics;

import unipd.dei.magnetophone.Song;
import android.graphics.Rect;

public interface VideoController {
	
	public void onSongLoaded(Song song);
	
	public void onSeek(float position);
	
	public void onPlaybackRateChanged(float multiplier, boolean doPlay);
	
	public void onProgress(float position);
	
	public void onTerminate();
	
	public void onViewSizeChanged(Rect rect);
}
