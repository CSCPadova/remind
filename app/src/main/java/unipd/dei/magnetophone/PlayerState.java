package unipd.dei.magnetophone;

public enum PlayerState
{
	UNKNOWN,		// Sconosciuto (es. il Servizio non è ancora attivo)
	NO_SONG,		// Nessuna canzone caricata
	SONG_CHANGED,	// La canzone è stata cambiata ma i suoi dati non sono ancora stati impostati in Pd
	INVALID_SONG,	// La canzone caricata ha dei file non trovati nella memoria
	STOPPED,		// Canzone non in riproduzione
	PLAYING,		// Canzone in riproduzione
	FASTFORWARD,	// Avanzamento veloce
	FASTREVERSE,	// Avvolgimento veloce
}
