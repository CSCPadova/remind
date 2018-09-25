package unipd.dei.magnetophone.database;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import unipd.dei.magnetophone.MusicPlayer;
import unipd.dei.magnetophone.utility.Song;
import unipd.dei.magnetophone.utility.Song.Photos;
import unipd.dei.magnetophone.utility.Song.Track;
import unipd.dei.magnetophone.utility.Song.Video;
import unipd.dei.magnetophone.xml.XMLfile;
import unipd.dei.magnetophone.xml.XmlConvertionManager;

/**
 * classe con metodi che svolgono funzioni utilizzate nel corso della vita
 * dell'applicazione sul database, ossia:
 * •importazione, modifica, cancellazione e ricerca di una song con oggetti Photos e Video correlati;
 * •importazione e cancellazione di un file xml.
 */
public class DatabaseManager {
    //id che serviranno da chiavi per ogni tupla nelle 4 tabelle diverse da XML
    private static int song_id;
    private static int track_id;
    private static int video_id;
    private static int photos_id;

    private Context context;

    /**
     * il costruttore inizializza, ogni volta che c'è questa classe è creata, gli id. è necessario in quanto
     * se non tenessimo traccia degli id a cui siamo arrivati, autoincrementanti, il rischio n cui incorriamo
     * è che ogni volta che l'applicazione si riavvia essi vengano settati a 0, con conseguenti collisioni
     * nel database.
     *
     * @param ctx: contesto dell'activity invocante
     */
    public DatabaseManager(Context ctx) {
        context = ctx;
        //Shared pref per tenere sempre aggiornato l'id della song senza mai ripartire da 0
        SharedPreferences sharedPref = context.getSharedPreferences("id_database", Context.MODE_PRIVATE);
        song_id = sharedPref.getInt("SongId", 0);
        track_id = sharedPref.getInt("TrackId", 0);
        video_id = sharedPref.getInt("VideoId", 0);
        photos_id = sharedPref.getInt("PhotosId", 0);
    }

    /**
     * restituisce una song dal database se è presente una song con l'id assengato, altrimenti null
     *
     * @param id: id della song richiesta
     * @return
     */
    public static Song getSongFromDatabase(int id, Context cont) {

        DatabaseHelper dbHelper = new DatabaseHelper(cont);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Song songFromCursor = null;
        //faccio una richiesta al mio database per avere tutti i miei bei dati
        String request = "SELECT * FROM " + DatabaseHelper.SONG +
                " INNER JOIN " + DatabaseHelper.TRACK + " ON " +
                DatabaseHelper.SONGID + " = " + DatabaseHelper.TRACKSONG +
//						" INNER JOIN " + DatabaseHelper.VIDEO + " ON " +
//						DatabaseHelper.SONGID + " = " + DatabaseHelper.VIDEOSONG +
//						" INNER JOIN " + DatabaseHelper.PHOTOS + " ON " +
//						DatabaseHelper.SONGID + " = " + DatabaseHelper.PHOTOSSONG +
                " WHERE " + DatabaseHelper.SONGID + " = " + id + ";";
        Cursor cursor = db.rawQuery(request, null);

        if (cursor.getCount() != 0)//nel caso che l'utente abbia cancellato precedentemente la canzone
        {
            songFromCursor = new Song();
            //prendo i metadati per la song
            cursor.moveToPosition(0);

            //utile per debug
            //String[] columnNames = cursor.getColumnNames();

            songFromCursor.setId(cursor.getInt(0));
            songFromCursor.setTitle(cursor.getString(1));
            songFromCursor.setAuthor(cursor.getString(2));
            songFromCursor.setYear(cursor.getString(3));
            songFromCursor.setSpeed(cursor.getFloat(4));
            songFromCursor.setEqualization(cursor.getString(5));
            songFromCursor.setSignature(cursor.getString(6));
            songFromCursor.setProvenance(cursor.getString(7));
            songFromCursor.setDuration(cursor.getFloat(8));
            songFromCursor.setExtension(cursor.getString(9));
            songFromCursor.setBitDepth(cursor.getInt(10));
            songFromCursor.setSampleRate(cursor.getInt(11));
            songFromCursor.setTapeWidth(cursor.getString(13));
            songFromCursor.setDescription(cursor.getString(14));
            songFromCursor.setXMLRef(cursor.getInt(15));
            songFromCursor.setPDF(cursor.getString(16));

            //inserisco track finché ve ne sono
            do {
                songFromCursor.setTrack(cursor.getString(19), cursor.getInt(20));
            }
            while (cursor.moveToNext());

            cursor.close();
            db.close();

            //vado a vedere se la song ha video o foto e in quel caso le inserisco nel database
            getPhotosFromDatabase(songFromCursor, cont);
            getVideoFromDatabase(songFromCursor, cont);
        }
        return songFromCursor;
    }

    private static void getPhotosFromDatabase(Song s, Context cont) {

        //prendo l'id per la ricerca
        int songId = s.getId();

        DatabaseHelper dbHelper = new DatabaseHelper(cont);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        //faccio la richiesta al database
        String selection = DatabaseHelper.PHOTOSSONG + " = " + songId;
        Cursor cursor = db.query(DatabaseHelper.PHOTOS, null, selection, null, null, null, null, null);
        if (cursor.getCount() == 1)//se abbiamo 1 cartella foto
        {
            //l'aggiungo alla mia song
            cursor.moveToFirst();
            s.setPhotos(cursor.getString(2));
        }
        //altrimenti la song resterà senza photos, come è giusto che sia
        cursor.close();
        db.close();
    }

    private static void getVideoFromDatabase(Song s, Context cont) {
        //prendo l'id per la ricerca
        int songId = s.getId();

        DatabaseHelper dbHelper = new DatabaseHelper(cont);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        //faccio la richiesta al database
        String selection = DatabaseHelper.VIDEOSONG + " = " + songId;
        Cursor cursor = db.query(DatabaseHelper.VIDEO, null, selection, null, null, null, null, null);
        if (cursor.getCount() == 1)//se abbiamo 1 cartella foto
        {
            //l'aggiungo alla mia song
            cursor.moveToFirst();
            s.setVideo(cursor.getString(2));
        }
        //altrimenti la song resterà senza photos, come è giusto che sia
        cursor.close();
        db.close();
    }


    /**
     * salva tutte le canzoni contenute nella list passata come parametro nel database
     *
     * @param: id:
     * è l'id dell'xml a cui appartengono le canzoni
     * @param: list:
     * lista concatenata di canzoni che si desiderainserire ne database
     */
    public void insertSongInDatabase(LinkedList<Song> list, long id) {
        //prendo la lunghezza della lista
        int listSize = list.size();

        //ora prendo canzone per canzone e la metto nella lista
        for (int i = 0; i < listSize; i++) {
            insertSingleSongInDatabase(list.get(i), (int) id);
        }
    }

    /**
     * inserisce il file XML nel database, insieme a tutte le canzoni che porta in sè
     *
     * @param XML: file da inserire nel database. Deve essere ottenuto da un file XML
     */
    public void insertXMLInDatabase(File XML) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //********* fase 1: si importano i dat dell'XML nel database ***********//
        //creo l'oggettoXMLfile
        XMLfile xml = new XMLfile();
        xml.setNomeFile(XML.getName());
        xml.setData(XML.lastModified());

        //si inserisce il file xml nel database
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.NOMEFILE, xml.getNomeFile());
        values.put(DatabaseHelper.DATAMODIFICA, xml.getData());
        db.insert(DatabaseHelper.XML, null, values);

        //mi prendo l'id dell'xml or ora inserito nel db
        String[] select2 = {DatabaseHelper.XMLID};
        Cursor query = db.query(DatabaseHelper.XML, select2, DatabaseHelper.NOMEFILE + "= \"" + xml.getNomeFile() + "\"", null, null, null, null, null);
        query.moveToPosition(0);
        long idForSong = query.getLong(0);

        //********* fase 2: si importano i dati delle canzoni nell'xml nel database ************//
        DocumentBuilderFactory strumentiDati = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder manipoloDati = strumentiDati.newDocumentBuilder();
            Document doc = manipoloDati.parse(XML);//rendo document il file con le canzoni

            LinkedList<Song> xmlsongs = XmlConvertionManager.XmlToDataConvertion(doc, context);//prendo i dati delle canzoni
            this.insertSongInDatabase(xmlsongs, idForSong);
            db.close();
        } catch (ParserConfigurationException e) {
            Log.e("DatabaseManager", "ParserConfigurationException");
        } catch (SAXException e) {
            Log.e("DatabaseManager", "SAXException");
        } catch (IOException e) {
            Log.e("DatabaseManager", "IOException");
        } finally {
            query.close();
            if (db != null)
                db.close();
        }
    }

    /**
     * metodo che elimina un file XML dal database
     *
     * @param nomeFile: nome del file XML da eliminare dal database
     * @return vero se il file è correttamente eliminato
     */
    public boolean removeXmlFromDatabase(String nomeFile) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.query(DatabaseHelper.XML, null, DatabaseHelper.NOMEFILE + " = \"" + nomeFile + "\"", null, null, null, null, null);
        ;
        int xmlId = -1;
        if (cursor.moveToFirst())
            xmlId = cursor.getInt(0);
        //controllo se la canzone che ho sul magnetofono è una di quelle che ho rimosso in questo modo
        cursor =
                db.query(DatabaseHelper.SONG, null, DatabaseHelper.XMLIDS + " = \"" + xmlId + "\"", null, null, null, null, null);
        cursor.moveToFirst();
        SharedPreferences shared = context.getSharedPreferences("service", Context.MODE_PRIVATE);
        int magnetoId = shared.getInt("song_id", -1);
        for (int i = 0; i < cursor.getCount(); i++) {
            if (magnetoId == cursor.getInt(15)) {
                Editor editor = shared.edit();
                editor.putInt("song_id", -1);
                editor.putBoolean("refreshed", true);
                editor.commit();
                final MusicPlayer player = MusicPlayer.getInstance();
                player.setSong(null);
                break;
            }
        }

        int f = db.delete(DatabaseHelper.XML, DatabaseHelper.NOMEFILE + " = \"" + nomeFile + "\"", null);
        cursor.close();
        db.close();
        return (f != 0);
    }

    /**
     * rimuove una singola canzone dal database
     *
     * @param s: oggetto Song che rappresenta la canzone che si vuole eliminare, importante campo id
     * @return vero se la canzone viene correttamente cancellata, falso altrimenti
     */
    public boolean removeSingleSongFromDatabase(Song s) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        int idToDelete = s.getId();
        int f = db.delete(DatabaseHelper.SONG, DatabaseHelper.SONGID + "=" + idToDelete, null);
        db.close();
        return (f != 0);

    }

    /**
     * Metodo chiamato dall'import per aggiungere una canzone
     * al database, non serve l'id dell'xml quindi lo mettiamo
     * per convenzione a -1.
     *
     * @param songToAdd: la canzone da aggiungere al database dell'applicazione
     */
    public int insertSingleSongInDatabase(Song songToAdd) {

        return this.insertSingleSongInDatabase(songToAdd, -1);
    }

    /**
     * Metodo per inserire una singola canzone in un database proveniente da un XML, quindi serve
     * l' id dell'xml nella quale è definita.
     *
     * @param songToAdd
     * @param xmlId
     */
    public int insertSingleSongInDatabase(Song songToAdd, int xmlId) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        ContentValues values = new ContentValues();//contenitore dei dati della tupla che andrà inserita nella tabella
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //############## inserimento tupla nella tabella SONG ########################
        values.put(DatabaseHelper.SONGID, song_id);

        values.put(DatabaseHelper.TITLE, songToAdd.getTitle());
        values.put(DatabaseHelper.AUTHOR, songToAdd.getAuthor());
        values.put(DatabaseHelper.YEAR, songToAdd.getYear());
        values.put(DatabaseHelper.EQUALIZATION, songToAdd.getEqualization());
        values.put(DatabaseHelper.SPEED, songToAdd.getSpeed());

        values.put(DatabaseHelper.SIGNATURE, songToAdd.getSignature());
        values.put(DatabaseHelper.PROVENANCE, songToAdd.getProvenance());
        values.put(DatabaseHelper.DURATION, songToAdd.getDuration());
        values.put(DatabaseHelper.EXTENSION, songToAdd.getExtension());
        values.put(DatabaseHelper.BITDEPTH, songToAdd.getBitDepth());
        values.put(DatabaseHelper.SAMPLERATE, songToAdd.getSampleRate());
        values.put(DatabaseHelper.NUMBEROFTRACKS, songToAdd.getNumberOfTracks());
        values.put(DatabaseHelper.TAPEWIDTH, songToAdd.getTapeWidth());
        values.put(DatabaseHelper.DESCRIPTION, songToAdd.getDescription());
        values.put(DatabaseHelper.PDF, songToAdd.getPdf().getPath());

        //gestione del fatto che la canzone sia importata o definita in un XML
        if (xmlId != -1)
            values.put(DatabaseHelper.XMLIDS, xmlId);
        else
            values.putNull(DatabaseHelper.XMLIDS);

        //abbiamo inserito tutti i metadati della song, ora la mettiamo nel db
        if(db.insert(DatabaseHelper.SONG, null, values)!=-1) {
            db.close();

            //################ inserimento tupla/e nella tabella TRACK ##############

            if (songToAdd.getNumberOfTracks() == 1) {
                insertTrackInDatabase(songToAdd.getTrackAtIndex(0));
            } else if (songToAdd.getNumberOfTracks() == 2) {
                insertTrackInDatabase(songToAdd.getTrackAtIndex(0));
                insertTrackInDatabase(songToAdd.getTrackAtIndex(1));
            } else if (songToAdd.getNumberOfTracks() == 4) {
                insertTrackInDatabase(songToAdd.getTrackAtIndex(0));
                insertTrackInDatabase(songToAdd.getTrackAtIndex(1));
                insertTrackInDatabase(songToAdd.getTrackAtIndex(2));
                insertTrackInDatabase(songToAdd.getTrackAtIndex(3));
            }

            //########### inserimento tupla nella tabella VIDEO ########################
            if (songToAdd.getVideo() != null)
                insertVideoInDatabase(songToAdd.getVideo());

            //########### inserimento tupla nella tabella PHOTOS
            if (songToAdd.getPhotos() != null)
                insertPhotosInDatabase(songToAdd.getPhotos());

            //Salvo nelle sharedPreferences l'id della song per non farlo ripartire da 0 quando
            //riapro il magnetofono
            SharedPreferences sharedPref = context.getSharedPreferences("id_database", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("SongId", ++song_id);
            editor.commit();

            return (song_id - 1); //Ritorno l'id della canzone creata
        }
        else
        {
            return -1;
        }
    }

    /**
     * Si preoccupa di aggiornare l'oggetto canzone nel database con lo stesso id del parametro passato
     * con i parametri presenti nel parametro stesso
     *
     * @param s: oggetto song che contiene i dati per l'update. Importate che sia presente l'id
     *           della canzone nel db. Non è necessario risettare i file dei canali in quanto non possono essere cambiati nelle impostazioni
     */
    public void updateSong(Song s) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //aggiornamento in tabella song
        String sql = "UPDATE " + DatabaseHelper.SONG + " SET " +
                DatabaseHelper.TITLE + "=\"" + s.getTitle() + "\", " +
                DatabaseHelper.AUTHOR + "=\"" + s.getAuthor() + "\", " +
                DatabaseHelper.YEAR + "=\"" + s.getYear() + "\", " +
                DatabaseHelper.SPEED + "=" + s.getSpeed() + ", " +
                DatabaseHelper.EQUALIZATION + "=\"" + s.getEqualization() + "\", " +

                DatabaseHelper.SIGNATURE + "=\"" + s.getSignature() + "\", " +
                DatabaseHelper.PROVENANCE + "=\"" + s.getProvenance() + "\", " +
                DatabaseHelper.DURATION + "=" + s.getDuration() + ", " +
                DatabaseHelper.EXTENSION + "=\"" + s.getExtension() + "\", " +
                DatabaseHelper.BITDEPTH + "=" + s.getBitDepth() + ", " +
                DatabaseHelper.SAMPLERATE + "=" + s.getSampleRate() + ", " +
                DatabaseHelper.NUMBEROFTRACKS + "=" + s.getNumberOfTracks() + ", " +
                DatabaseHelper.TAPEWIDTH + "=\"" + s.getTapeWidth() + "\", " +
                DatabaseHelper.DESCRIPTION + "=\"" + s.getDescription() + "\" ";

        sql = sql + " WHERE " + DatabaseHelper.SONGID + "=" + s.getId() + ";";
        db.execSQL(sql);
        db.close();

        //aggiornamento in tabella track
        updateTracks(s);
        //aggiornamento in tabella video
        if (s.getVideo() != null)
            updateVideo(s.getVideo());

        //aggiornamento in tabella PHOTO
        if (s.getPhotos() != null)
            updatePhotos(s.getPhotos());

    }

    /**
     * aggiorna nel database la canzone passata come parametro
     *
     * @param s
     */
    public void updateTracks(Song s) {
        //distinzione dell'aggiornamento a seconda del numero di tracce che possiede la song
        if (s.getNumberOfTracks() == 1) {
            updateSingleTrack(s.getTrackAtIndex(0));
        } else if (s.getNumberOfTracks() == 2) {
            updateSingleTrack(s.getTrackAtIndex(0));
            updateSingleTrack(s.getTrackAtIndex(1));
        } else if (s.getNumberOfTracks() == 4) {
            updateSingleTrack(s.getTrackAtIndex(0));
            updateSingleTrack(s.getTrackAtIndex(1));
            updateSingleTrack(s.getTrackAtIndex(2));
            updateSingleTrack(s.getTrackAtIndex(3));
        }
    }

    /**
     * aggiorna una unica track nel database
     *
     * @param t: track da aggiornare (l'id fa da riferimento)
     */
    public void updateSingleTrack(Track t) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //aggiornamento in tabella song
        String sql = "UPDATE " + DatabaseHelper.TRACK + " SET " +
                DatabaseHelper.TRACKNAME + "=\"" + t.getName() + "\", " +
                DatabaseHelper.TRACKPATH + "=\"" + t.getPath() + "\", " +
                DatabaseHelper.INDEX + "=\"" + t.getIndex() + "\", " +
                DatabaseHelper.TRACKSONG + "=" + t.getForeignKey() + " ";

        sql = sql + " WHERE " + DatabaseHelper.TRACKID + "=" + t.getId() + ";";
        db.execSQL(sql);
        db.close();
    }

    public void updateVideo(Video v) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //aggiornamento in tabella song
        String sql = "UPDATE " + DatabaseHelper.VIDEO + " SET " +
                DatabaseHelper.VIDEONAME + "=\"" + v.getName() + "\", " +
                DatabaseHelper.VIDEOPATH + "=\"" + v.getPath() + "\", " +
                DatabaseHelper.VIDEOSONG + "=" + v.getForeignKey() + " ";

        sql = sql + " WHERE " + DatabaseHelper.VIDEOID + "=" + v.getId() + ";";
        db.execSQL(sql);
        db.close();
    }

    public void updatePhotos(Photos p) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        //aggiornamento in tabella song
        String sql = "UPDATE " + DatabaseHelper.PHOTOS + " SET " +
                DatabaseHelper.PHOTOSNAME + "=\"" + p.getName() + "\", " +
                DatabaseHelper.PHOTOSPATH + "=\"" + p.getPath() + "\", " +
                DatabaseHelper.PHOTOSSONG + "=" + p.getForeignKey() + " ";

        sql = sql + " WHERE " + DatabaseHelper.PHOTOSID + "=" + p.getId() + ";";
        db.execSQL(sql);
        db.close();
    }

    /**
     * inserisce nel database il track passato a parametro
     *
     * @param t
     */
    public int insertTrackInDatabase(Track t) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(DatabaseHelper.TRACKID, track_id);

        String p = t.getPath();//preso il path
        try {
            Uri trackUri = Uri.parse(p);
            String trackName = trackUri.getLastPathSegment();//prendo il nome del file alla fine del path
            values.put(DatabaseHelper.TRACKNAME, trackName);
        } catch (NullPointerException e) {
            Log.e("DatabaseManager", "insertTrackInDatabase: path non trovato/non valido");
            values.put(DatabaseHelper.TRACKNAME, "@string/invalid");
            //qui volendo si può essere più fiscali impedendo proprio l'importazione del file
        }

        values.put(DatabaseHelper.TRACKPATH, p);
        values.put(DatabaseHelper.INDEX, t.getIndex());
        values.put(DatabaseHelper.TRACKSONG, song_id);

        if(db.insert(DatabaseHelper.TRACK, null, values)!=-1) {
            db.close();

            track_id++;//incremento l'id dei track di uno
            //ora salva in memoria il nuovo ultimo id disponibile
            SharedPreferences sharedPref = context.getSharedPreferences("id_database", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt("TrackId", track_id);
            editor.commit();
            return 0;
        }
        else
        {
            return -1;
        }
    }

    /**
     * inserisce un singolo video nel database
     *
     * @param v
     */
    public void insertVideoInDatabase(Video v) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.VIDEOID, video_id);

        String p = v.getPath();//preso il path

        try {
            Uri trackUri = Uri.parse(p);
            String videoName = trackUri.getLastPathSegment();//prendo il nome del file alla fine del path
            values.put(DatabaseHelper.VIDEONAME, videoName);
        } catch (NullPointerException e) {
            //in questo caso non importo proprio il video
            Log.e("DatabaseManager", "insertVideoInDatabase: path non trovato/non valido");
            db.close();
            return;
        }

        values.put(DatabaseHelper.VIDEOPATH, p);
        values.put(DatabaseHelper.VIDEOSONG, song_id);

        db.insert(DatabaseHelper.VIDEO, null, values);
        db.close();

        video_id++;
        //ora salva in memoria il nuovo ultimo id disponibile
        SharedPreferences sharedPref = context.getSharedPreferences("id_database", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("VideoId", video_id);
        editor.commit();
    }

    /**
     * @param p
     */
    public void insertPhotosInDatabase(Photos p) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.PHOTOSID, photos_id);

        String pa = p.getPath();//preso il path

        try {
            Uri trackUri = Uri.parse(pa);
            String photosName = trackUri.getLastPathSegment();//prendo il nome del file alla fine del path
            values.put(DatabaseHelper.PHOTOSNAME, photosName);
        } catch (NullPointerException e) {
            //in questo caso non importo proprio il video
            Log.e("DatabaseManager", "insertPhotosInDatabase: path non trovato/non valido");
            db.close();
            return;
        }

        values.put(DatabaseHelper.PHOTOSPATH, pa);
        values.put(DatabaseHelper.PHOTOSSONG, song_id);

        db.insert(DatabaseHelper.PHOTOS, null, values);
        db.close();

        //ora salva in memoria il nuovo ultimo id disponibile
        SharedPreferences sharedPref = context.getSharedPreferences("id_database", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt("PhotosId", ++photos_id);
        editor.commit();
    }
}
