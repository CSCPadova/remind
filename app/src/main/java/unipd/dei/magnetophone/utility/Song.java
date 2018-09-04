package unipd.dei.magnetophone.utility;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.LinkedList;

/**
 * classe che rappresenta l'oggetto canzone, ossia l'"Opera" usufruibile
 * all'interno dell'applicazione Magnetofono
 * <p>
 * La canzone è composta da diversi metadati ed è correlata da un oggetto
 * Photos, uno Video che rappresentano rispettivamente la cartella delle foto
 * per lo slide show e il video correlato al nastro rappresentane l'opera.
 */

public class Song {
    public static final String INVALID = "[INVALID]";
    //valori =INVALID per mia scelta arbitraria
    private String author = INVALID; // nome dell'autore della canzone
    private String title = INVALID; // nome della canzone
    private String year = INVALID; // anno di pubblicazione
    private float speed; // velocità di riproduzione, anche nota come Tape
    // Transfer Rate
    private String equalization = INVALID;// equalizzazione
    private String signature = INVALID;// una sigla che va con la song
    private String provenance = INVALID;// fondo, archivio o altro luogo di provenienza
    // della song
    private float duration;// durata della song in secondi e millisecondi
    private String extension = INVALID;// estensione del file delle track che compongono
    private int bitdepth;// profondità di bit utilizzata per il singolo sample
    // della canzone, per noi 16
    private int sampleRate;// numero di sample letti al secondo per la canzone,
    private String tapeWidth = INVALID; // larghezza del nastro, sarà 1/4, 1/2 o 1 inch
    // la song, ad esempio .wav
    private String description = INVALID;// descrizione legata alla song
    private int xmlref; // id di riferimento all'xml associato
    // es. 96 kHz
    private int songId; // id di riferimento della song nel database
    private LinkedList<Track> trackList;
    private Video video;// video associato alla song
    private FilePDF filePDF;// video associato alla song
    private Photos photos;// cartella delle fotografie associata alla song

    /**
     * costruttore della canzone quadrifonica. I path delle tracce vengono
     * gestiti nel seguente modo: i primi due path sono considerti per il canale
     * sinistro e prendono indice 1 e 2, i secondi due path vengono considerati
     * per il canale destro e prendono indice 3 e 4. Successivamente si potranno
     * modificare sia canali che indici che path
     *
     * @param i  : id della canzone
     * @param t  : path della prima traccia, automaticamente impostato su
     *           indice 1
     * @param t2 : path della seconda traccia, automaticamente impostato su
     *           indice 2
     * @param t3 : path della terza traccia, automaticamente impostato su
     *           indice 3
     * @param t4 : path della seconda traccia, automaticamente impostato su
     *           indice 4
     */
    public Song(int i, String t, String t2, String t3, String t4) {
        this.songId = i;

        trackList = new LinkedList<Track>();

        if (t != null)
            setTrack(t, 1);
        if (t2 != null)
            setTrack(t2, 2);
        if (t3 != null)
            setTrack(t3, 3);
        if (t4 != null)
            setTrack(t4, 4);

        // numberOfTracks = trackList.size();
    }

    //private SongType songType;

    /**
     * costruttore che setta tutti i campi a null, utile per una costruzione
     * progressiva della song
     */
    public Song() {
        trackList = new LinkedList<Track>();
    }

    /**
     * Costruttore della canzone stereo (2 tracce)
     *
     * @param i      : id della song
     * @param track  : prima traccia della song
     * @param track2 : seconda traccia della song
     */
    public Song(int i, String track, String track2) {
        this.songId = i;

        trackList = new LinkedList<Track>();

        if (track != null)
            setTrack(track, 1);
        if (track2 != null)
            setTrack(track2, 2);

    }

    public void clearTrackList() {
        trackList = new LinkedList<Track>();
    }

    /**
     * Costruttore della canzone con una sola traccia, che può essere mono o
     * stereo
     *
     * @param i
     * @param t1 : unica traccia (mono o stereo) della song
     */
    public Song(int i, String t1) {
        this.songId = i;

        trackList = new LinkedList<Track>();

        if (t1 != null)
            setTrack(t1, 1);

        // numberOfTracks = trackList.size();
    }

    // ########### costruttori ################

    public static float getFloatSpeed(SongSpeed speed) {
        switch (speed) {
            case SONG_SPEED_3_75:
                return 3.75f;
            case SONG_SPEED_7_5:
                return 7.5f;
            case SONG_SPEED_15:
                return 15.0f;
            case SONG_SPEED_30:
                return 30.0f;
        }
        return 0.0f;
    }

    public static SongSpeed getEnumSpeed(float speed) {
        if (speed == 3.75f) {
            return SongSpeed.SONG_SPEED_3_75;
        } else if (speed == 7.5f) {
            return SongSpeed.SONG_SPEED_7_5;
        } else if (speed == 15.0f) {
            return SongSpeed.SONG_SPEED_15;
        } else {
            return SongSpeed.SONG_SPEED_30;
        }
    }

    /**
     * Metodo statico che, data una canzone ed un intent, inserisce nell'intent
     * i dati della canzone
     *
     * @param s      : canzone
     * @param intent : Intent da dare alla canzone per riempirlo dei dati della
     *               canzone
     */
    public static void fillIntent(Song s, Intent intent) {
        intent.putExtra("Song_Id", s.getId());
        intent.putExtra("Song_Author", s.getAuthor());

        intent.putExtra("Song_Eq", s.getEqualization());
        intent.putExtra("Song_Speed", s.getSpeed());
        intent.putExtra("Song_Title", s.getTitle());
        intent.putExtra("Song_Year", s.getYear());

        intent.putExtra("Song_Signature", s.getSignature());
        intent.putExtra("Song_Provenance", s.getProvenance());
        intent.putExtra("Song_Duration", s.getDuration());
        intent.putExtra("Song_Extension", s.getExtension());

        intent.putExtra("Song_BitDepth", s.getBitDepth());
        intent.putExtra("Song_SampleRate", s.getSampleRate());
        intent.putExtra("Song_NumberOfTracks", s.getNumberOfTracks());

        intent.putExtra("Song_TapeWidth", s.getTapeWidth());
        intent.putExtra("Song_Description", s.getDescription());

        switch (s.getNumberOfTracks()) {
            case 1:
                intent.putExtra("Song_FirstTrack", s.getTrackList().get(0).getPath());
                break;

            case 2:
                intent.putExtra("Song_FirstTrack", s.getTrackList().get(0).getPath());
                intent.putExtra("Song_SecondTrack", s.getTrackList().get(1).getPath());
                break;

            case 4:
                intent.putExtra("Song_FirstTrack", s.getTrackList().get(0).getPath());
                intent.putExtra("Song_SecondTrack", s.getTrackList().get(1).getPath());
                intent.putExtra("Song_ThirdTrack", s.getTrackList().get(2).getPath());
                intent.putExtra("Song_FourthTrack", s.getTrackList().get(3).getPath());
                break;
        }
    }

    /**
     * Metodo che crea una Song a partire dagli extra dati di un intent Se non
     * ha un id, la Song e' errata e viene lanciata un'eccezione
     *
     * @param intent
     * @return la Song inserita nell'intent
     */
    public static Song songFromIntent(Intent intent)
            throws InvalidParameterException {
        // si va a prendere tutti i dati dall'intent
        int id = intent.getIntExtra("Song_Id", -1);
        if (id == -1)
            throw new InvalidParameterException("Song non valida");

        String title = intent.getStringExtra("Song_Title");
        String author = intent.getStringExtra("Song_Author");
        String year = intent.getStringExtra("Song_Year");
        String equalization = intent.getStringExtra("Song_Eq");
        float speed = intent.getFloatExtra("Song_Speed", (float) 3.75);

        String signature = intent.getStringExtra("Song_Signature");
        String provenance = intent.getStringExtra("Song_Provenance");
        float duration = intent.getFloatExtra("Song_Duration", -1);
        String extension = intent.getStringExtra("Song_Extension");

        int bitdepth = intent.getIntExtra("Song_BitDepth", -1);
        int samplerate = intent.getIntExtra("Song_SampleRate", -1);
        int numberOfTracks = intent.getIntExtra("Song_NumberOfTracks", -1);

        String tapeWidth = intent.getStringExtra("Song_TapeWidth");
        String description = intent.getStringExtra("Song_Description");

        String video = intent.getStringExtra("Song_Video");
        String photos = intent.getStringExtra("Song_Photos");
        String leftPath = intent.getStringExtra("Song_FirstTrack");

        // si costruisce la canzone a partire dai dati dell'intent
        Song s = null;

        if (numberOfTracks == 1)
            s = new Song(id, leftPath);
        else if (numberOfTracks == 2) {
            String rightPath = intent.getStringExtra("Song_SecondTrack");
            s = new Song(id, leftPath, rightPath);

        } else if (numberOfTracks == 4) {
            String leftPath2 = intent.getStringExtra("Song_SecondTrack");
            String rightPath = intent.getStringExtra("Song_ThirdTrack");
            String rightPath2 = intent.getStringExtra("Song_FourthTrack");

            s = new Song(id, leftPath, leftPath2, rightPath, rightPath2);
        }
        s.setTitle(title);
        s.setAuthor(author);
        s.setYear(year);
        s.setSpeed(speed);
        s.setEqualization(equalization);

        s.setSignature(signature);
        s.setProvenance(provenance);
        s.setDuration(duration);
        s.setExtension(extension);
        s.setBitDepth(bitdepth);
        s.setSampleRate(samplerate);
        s.setTapeWidth(tapeWidth);
        s.setDescription(description);

        s.setVideo(video);
        s.setPhotos(photos);

        return s;
    }

    // ######## metodi interrogatori #########

    /**
     * Metodo che si occupa di verificare se i file audio collegati alla canzone
     * esistono realmente. Se è mono controlla solo il file sinistro, altrimenti
     * sia dx che sx
     *
     * @return true se è una canzone valida
     */
    public boolean isValid() {

        int lunghezza = this.getNumberOfTracks();
        switch (lunghezza) {
            case 1:
                boolean one = isValid(this.getTrackList().get(0));
                return one;

            case 2:
                one = isValid(this.getTrackList().get(0));
                boolean two = isValid(this.getTrackList().get(1));
                return one && two;

            case 4:
                one = isValid(this.getTrackList().get(0));
                two = isValid(this.getTrackList().get(1));
                boolean three = isValid(this.getTrackList().get(2));
                boolean four = isValid(this.getTrackList().get(3));
                return one && two && three && four;
        }
        return false;
    }

    /**
     * controlla la validità del file audio associato alla traccia passata come
     * parametro, ossia che esiste effettivamente e che sia di un formato da noi
     * supportato
     *
     * @param t
     * @return
     */
    public boolean isValid(Track t) {

        File temp = new File(t.getPath());
        // controlla che esiste ed invoca il metodo della track isValid per
        // controllare che
        // sia un formato da noi supportato

        if (temp.exists() && t.isValid()) {
            return true;
        }
        return false;
    }

    // ######### metodi getter ##########

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String a) {
        author = a;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String a) {
        title = a;
    }

    public String getYear() {
        if (year == null)
            return "0000";
        return year;
    }

    public void setYear(String a) {
        year = a;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float a) {
        speed = a;
    }

    public String getEqualization() {
        return equalization;
    }

    public void setEqualization(String a) {
        equalization = a;
    }

    public int getXmlIdReference() {
        return xmlref;
    }

    public int getId() {
        return songId;
    }

    public void setId(int i) {
        songId = i;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String s) {
        signature = s;
    }

    public String getProvenance() {
        return provenance;
    }

    public void setProvenance(String p) {
        provenance = p;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float d) {
        duration = d;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String e) {
        extension = e;
    }

    public int getBitDepth() {
        return bitdepth;
    }

    public void setBitDepth(int bd) {
        bitdepth = bd;
    }

    // ########################### metodi setter ###########################

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sr) {
        sampleRate = sr;
    }

    public int getNumberOfTracks() {
        return trackList.size();
    }

    public String getTapeWidth() {
        return tapeWidth;
    }

    public void setTapeWidth(String t) {
        tapeWidth = t;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        description = d;
    }

    public int getXMLRef() {
        return xmlref;
    }

    public void setXMLRef(int i) {
        xmlref = i;
    }

    /**
     * ritorna la lista delle tracce della canzone
     *
     * @return
     */
    public LinkedList<Track> getTrackList() {
        return trackList;
    }

    /**
     * ritorna l'elemento nella lista dei Track di indice i
     *
     * @param i
     * @return Track di indice i nella lista
     */
    public Track getTrackAtIndex(int i) throws NullPointerException {
        return trackList.get(i);
    }

    /**
     * ritorna l'elemento track della lista con index pari ad i, null se non è
     * presente
     *
     * @param i
     * @return
     */
    public Track getTrackOfIndex(int i) {
        int listLength = trackList.size();
        for (int l = 0; l < listLength; l++) {
            if (trackList.get(l).getIndex() == i)
                return trackList.get(l);
        }
        return null;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(String path) {
        video = (path == null) ? null : new Video(path);
    }

    public FilePDF getPdf() {
        if (filePDF != null)
            return filePDF;

        return new FilePDF("");
    }

    public void setPDF(String path) {
        filePDF = (path == null) ? null : new FilePDF(path);
    }

    public Photos getPhotos() {
        return photos;
    }

    public void setPhotos(String path) {
        photos = (path == null) ? null : new Photos(path);
    }

    /**
     * metodo che restituisce la durata della canzone non in secondi come arebbe
     * getDuration ma in formato MM:SS
     *
     * @return
     */
    public String getDurationInMinutes() {
        float dur = this.duration;
        int minutes = (int) dur / 60;
        int seconds = (int) dur % 60;

        return minutes + " : " + seconds;
    }

    /**
     * crea un nuovo track e lo aggiunge alla lista dei track della song,
     * inserisce i metadati della track nella song
     *
     * @param p   path della traccia
     * @param ind indice che si desidera far avere alla traccia
     */
    public void setTrack(String p, int ind) {
        if (p != null) {
            Track t = new Track(p, ind);
            WaveHeader wave = new WaveHeader(p);
            this.setDuration(wave.getDuration());
            this.setBitDepth(wave.getBitsPerSample());
            this.setSampleRate(wave.getSampleRate());
            trackList.add(t);
        }
    }

    public boolean isAlreadyPresentTrackOfIndex(int i) {
        int n = trackList.size();
        for (int l = 0; i < n; i++) {
            if (trackList.get(l).getIndex() == i)
                return true;
        }
        return false;
    }

    /**
     * elimina la track che presenta l'indice passato a parametro. Se non è
     * presente il metodo non fa nulla
     *
     * @param i
     */
    public void deleteTrackOfIndex(int i) {
        int n = trackList.size();
        for (int l = 0; l < n; l++) {
            if (trackList.get(l).getIndex() == i)
                trackList.remove(l);
        }
    }

    /**
     * dice se la cartella delle foto è valida o meno
     *
     * @return
     */
    public boolean isPhotosValid() {
        if (photos == null)
            return false;

        return this.getPhotos().isValid();

    }

    /**
     * dice se il video esiste o meno
     *
     * @return
     */
    public boolean isVideoValid() {
        if (video == null)
            return false;

        return this.getVideo().isValid();

    }

    /**
     * dice se il file pdf e' valido o meno
     *
     * @return
     */
    public boolean isPdfValid() {
        if (filePDF == null)
            return false;

        return this.getPdf().isValid();

    }

    /**
     * ritorna il numero di foto presenti nella cartella se è valida
     *
     * @return
     */
    public int getNumberOfPhotos() {
        if (this.isPhotosValid()) {
            // prendo il file photo
            File pho = new File(this.getPhotos().getPath());
            File[] photos = pho.listFiles();
            return photos.length;
        } else
            return 0;
    }

    /**
     * ritorna la lista dei file foto presenti nella cartella se essa esiste
     *
     * @return
     */
    public File[] getPhotosFiles() {
        if (this.isPhotosValid()) {
            // prendo il file photo
            File pho = new File(this.getPhotos().getPath());
            File[] photos = pho.listFiles();
            return photos;
        } else
            return null;
    }

    public SongType getSongType() {
        switch (getTrackList().size()) {
            case 1:
                if (getTrackList().get(0).isMono())
                    return SongType.TYPE_1M;
                else
                    return SongType.TYPE_1S;
            case 2:
                return SongType.TYPE_2M;
            case 4:
                return SongType.TYPE_4M;
            default:
                return null;
        }
    }

    public enum SongType {
        TYPE_1M(0), TYPE_1S(1), TYPE_2M(2), TYPE_4M(3);

        private int value;

        private SongType(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    public enum SongSpeed {
        SONG_SPEED_3_75(0), SONG_SPEED_7_5(1), SONG_SPEED_15(2), SONG_SPEED_30(3);

        private int value;

        private SongSpeed(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    /**
     * Classe track, rappresenta una singola traccia della song e può essere
     * riprodotta su canali diversi
     */
    public static class Track {
        private String path;
        private int id;
        private int index;
        private String name;
        private int foreign_key;// id della song di appartenenza
        private boolean mono;// posto a true se la track è di tipo mono, false
        // se è stereo
        private boolean isFileValid;

        public Track(String p, int ind) {
            path = p;
            index = ind;
            Uri u = Uri.parse(path);
            String name = u.getLastPathSegment();
            setName(name);
            WaveHeader wave = new WaveHeader(p);
            mono = wave.isMono();

            isFileValid = wave.isValid();
        }

        public String getPath() {
            return path;
        }

        public void setPath(String p) {
            path = p;
        }

        public int getId() {
            return id;
        }

        public void setId(int i) {
            id = i;
        }

        public String getName() {
            return name;
        }

        public void setName(String n) {
            name = n;
        }

        public int getForeignKey() {
            return foreign_key;
        }

        public void setForeignKey(int i) {
            foreign_key = i;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int ind) {
            index = id;
        }

        public boolean isMono() {
            return mono;
        }

        public boolean isValid() {
            return isFileValid;
        }

        public void setIfMono(boolean m) {
            mono = m;
        }
    } // fine classe Track

    /**
     * classe video, rappresenza un video legato ad una song
     */
    public static class Video {
        private String path;
        private String name;
        private int id;
        private int foreign_key;// id della song di appartenenza
        private boolean valid;

        /**
         * costruttore, setta il path del video ed il suo nome in base al nome
         * del file video
         *
         * @param p : il path del file video
         */
        public Video(String p) {
            try {
                Uri.parse(p);
            } catch (NullPointerException e) {
                Log.e("Song", "Video: file non trovato");
                path = INVALID;
                name = INVALID;
                id = -1;
                valid = false;
            }

            // controllo l'esistenza del video
            File vid = new File(p);
            if (vid.exists()) {
                valid = true;
                String name = vid.getName();
                setName(name);
                path = vid.getAbsolutePath();
            } else
                valid = false;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String p) {
            this.path = p;
        }

        public int getId() {
            return id;
        }

        public void setId(int i) {
            id = i;
        }

        public String getName() {
            return name;
        }

        public void setName(String n) {
            name = n;
        }

        public int getForeignKey() {
            return foreign_key;
        }

        public void setForeignKey(int i) {
            foreign_key = i;
        }

        public boolean isValid() {
            return valid;
        }
    }

    /**
     * classe che rappresenta per ora la cartella con presenti le foto correlate
     * ad una song, che serviranno per essere visualizzate in uno slideshow.
     * Possibili future modifiche
     */
    public static class Photos {
        private String path;
        private String name;
        private int id;
        private int foreign_key;
        private boolean valid;

        public Photos(String p) {
            path = p;

            try {
                Uri u = Uri.parse(p);
                String name = u.getLastPathSegment();
                setName(name);
            } catch (NullPointerException e) {
                Log.e("Song", "Photos: file non trovato");
                path = INVALID;
                name = INVALID;
                id = -1;
                valid = false;
            }

            File pho = new File(path);
            pho.mkdir();
            valid = pho.exists() && pho.isDirectory();
        }

        // metodi getter

        public String getPath() {
            return path;
        }

        // metodi setter
        public void setPath(String p) {
            this.path = p;
        }

        public String getName() {
            return name;
        }

        public void setName(String n) {
            name = n;
        }

        public int getId() {
            return id;
        }

        public void setId(int i) {
            id = i;
        }

        public int getForeignKey() {
            return foreign_key;
        }

        public void setForeignKey(int i) {
            foreign_key = i;
        }

        public boolean isValid() {
            return valid;
        }
    }// fine Photos

    /**
     * classe FilePDF, rappresenza un pdf legato ad una song
     */
    public static class FilePDF {
        private String path;
        private String name;
        private boolean valid;

        /**
         * costruttore, setta il path del pdf ed il suo nome in base al nome
         * del file
         *
         * @param p : il path del file pdf
         */
        public FilePDF(String p) {
            try {
                Uri.parse(p);
            } catch (NullPointerException e) {
                Log.e("Song", "PDF: file non trovato");
                path = INVALID;
                name = INVALID;
                valid = false;
            }

            // controllo l'esistenza del pdf
            File pdf = new File(p);
            if (pdf.exists()) {
                valid = true;
                String name = pdf.getName();
                setName(name);
                path = pdf.getAbsolutePath();
            } else
                valid = false;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String p) {
            this.path = p;
        }

        public String getName() {
            return name;
        }

        public void setName(String n) {
            name = n;
        }

        public boolean isValid() {
            return valid;
        }
    }

}
