package unipd.dei.magnetophone.graphics;

import unipd.dei.magnetophone.Song;
import unipd.dei.magnetophone.Song.SongSpeed;

/**
 * Interfaccia che rappresenta un controller per l'animazione del magnetofono,
 * che riceve gli eventi di riproduzione dal MusicPlayer e attua l'operazione necessaria
 * a eseguire l'animazione per il relativo modello di magnetofono.
 */
public interface AnimationController {
    /**
     * Chiamato quando la riproduzione viene avviata
     */
    public void onMusicPlay();

    /**
     * Chiamato quando la riproduzione viene interrotta
     */
    public void onMusicStop();

    /**
     * Chiamato quando la riproduzione messa in riproduzione veloce
     */
    public void onMusicFastForward();

    /**
     * Chiamato quando la riproduzione messa in riavvolgimento veloce
     */
    public void onMusicFastReverse();

    /**
     * Chiamato quando la canzone da riprodurre viene cambiata
     *
     * @param newSong la nuova canzone selezionata
     */
    public void onSongChanged(Song newSong);

    /**
     * Chiamata quando la posizione corrente della canzone cambia
     *
     * @param currentProgress l'istante corrente nella canzone
     * @param curentTimestamp l'istante corrente non scalato alla velocit\`a di avanzamento
     */
    public void onSongProgress(float currentProgress, float currentTimestamp);


    public void onSongSpeedChanged(SongSpeed speed);
}
