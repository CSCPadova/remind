package unipd.dei.magnetophone.graphics;

import android.graphics.Rect;

import unipd.dei.magnetophone.Song;

public interface VideoController {

    public void onSongLoaded(Song song);

    public void onSeek(float position);

    public float getVideoOffset();

    public void setVideoOffset(float position);

    public void onPlaybackRateChanged(float multiplier, boolean doPlay);

    public void onProgress(float position);

    public void onTerminate();

    public void onViewSizeChanged(Rect rect);
}
