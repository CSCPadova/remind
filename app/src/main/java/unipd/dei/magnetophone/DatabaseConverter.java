package unipd.dei.magnetophone;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.Xml;
import android.widget.Toast;

import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Classe che si occupa di creare un file xml contenente i dati del database delle canzoni
 * ed inserirlo nella cartella attuale dell'applicazione
 */

public class DatabaseConverter {

    private static final String SONG = "song";
    private static final String SONGSDATA = "songsdata";
    private static final String YEAR = "year";
    private static final String SPEED = "speed";
    private static final String EQUALIZATION = "equalization";
    private static final String SIGNATURE = "signature";
    private static final String PROVENANCE = "provenance";
    private static final String DURATION = "duration";
    private static final String EXTENSION = "extension";
    private static final String BITDEPTH = "bitdepth";
    private static final String SAMPLERATE = "samplerate";
    private static final String NUMBEROFTRACKS = "numberoftracks";
    private static final String TAPEWIDTH = "tapewidth";
    private static final String DESCRIPTION = "description";
    private static final String XMLID = "xml_id";
    private static final String TRACK = "track";
    private static final String NAME = "name";
    private static final String PATH = "path";
    private static final String INDEX = "index";
    private static final String MONO = "mono";
    private static final String RESPID = "respective_song_id";
    private static final String PHOTOS = "photos";
    private static final String VIDEO = "video";
    private static String DATABASE = "database.xml";
    private static String ID = "id";
    private static String TITLE = "title";
    private static String AUTHOR = "author";

    /**
     * esporta il database nell'attuale cartella dove il magnetofono prende i suoi dati
     * in file formato xml
     */
    public static void exportTheDatabase(Context cont) {
        //interrogo il database
        MagnetophoneOpenHelper dbHelper = new MagnetophoneOpenHelper(cont);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        //prendo tutte le info sulle canzoni
        Cursor songs = db.query(MagnetophoneOpenHelper.SONG, null, null, null, null, null, null, null);
        songs.moveToFirst();


        //creiamo un nuovo file nella attuale cartella da dove si prendono gli XML
        File newxmlfile = new File(XmlImport.getCurrentDirectory(cont) + DATABASE);

        try {
            newxmlfile.createNewFile();
        } catch (IOException e) {
            Log.e("DatabaseConverter", "IOException: exception in createNewFile() method");
        }

        //colleghiamo il file con un OutputStream
        FileOutputStream fileos = null;
        try {
            fileos = new FileOutputStream(newxmlfile);
        } catch (FileNotFoundException e) {
            Log.e("DatabaseConverter", "FileNotFoundException: can't create FileOutputStream");
        }
        //creiamo un XmlSerializer per poter scrivere i nostri dati
        XmlSerializer serializer = Xml.newSerializer();

        try {
            //settiamo il FileOutputStram come uotput del serializer, usando l'UTF-8 encoding
            serializer.setOutput(fileos, "UTF-8");

            //Write <?xml declaration with encoding (if encoding not null) and standalone flag (if standalone not null)
            serializer.startDocument(null, true);

            //set indentation option
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);

            //iniziamo con la tag <songsdata>
            serializer.startTag(null, SONGSDATA);

            //per ogni canzone presente nel database
            for (int i = 0; i < songs.getCount(); i++) {
                //apriamo la song
                serializer.startTag(null, SONG);
                //iniziamo ad inserire gli attributi

                //inseriamo gli attributi. A parte gli id, potrebbe essere che gli altri siano null, allora
                //facciamo apparire [UNKNOWN]
                serializer.attribute(null, ID, "" + songs.getInt(0));

                if (songs.getString(1) != null)
                    serializer.attribute(null, TITLE, songs.getString(1));
                else
                    serializer.attribute(null, TITLE, cont.getString(R.string.sconosciuto));

                if (songs.getString(2) != null)
                    serializer.attribute(null, AUTHOR, songs.getString(2));
                else
                    serializer.attribute(null, AUTHOR, cont.getString(R.string.sconosciuto));

                if (songs.getString(3) != null)
                    serializer.attribute(null, YEAR, songs.getString(3));
                else
                    serializer.attribute(null, YEAR, cont.getString(R.string.sconosciuto));

                if (songs.getString(4) != null)
                    serializer.attribute(null, SPEED, "" + songs.getFloat(4));
                else
                    serializer.attribute(null, SPEED, cont.getString(R.string.sconosciuto));

                if (songs.getString(5) != null)
                    serializer.attribute(null, EQUALIZATION, songs.getString(5));
                else
                    serializer.attribute(null, EQUALIZATION, cont.getString(R.string.sconosciuto));

                if (songs.getString(6) != null)
                    serializer.attribute(null, SIGNATURE, songs.getString(6));
                else
                    serializer.attribute(null, SIGNATURE, cont.getString(R.string.sconosciuto));

                if (songs.getString(7) != null)
                    serializer.attribute(null, PROVENANCE, songs.getString(7));
                else
                    serializer.attribute(null, PROVENANCE, cont.getString(R.string.sconosciuto));

                if (songs.getString(8) != null)
                    serializer.attribute(null, DURATION, "" + songs.getFloat(8));
                else
                    serializer.attribute(null, DURATION, cont.getString(R.string.sconosciuto));

                if (songs.getString(9) != null)
                    serializer.attribute(null, EXTENSION, songs.getString(9));
                else
                    serializer.attribute(null, EXTENSION, cont.getString(R.string.sconosciuto));

                if (songs.getString(10) != null)
                    serializer.attribute(null, BITDEPTH, "" + songs.getInt(10));
                else
                    serializer.attribute(null, BITDEPTH, cont.getString(R.string.sconosciuto));

                if (songs.getString(11) != null)
                    serializer.attribute(null, SAMPLERATE, "" + songs.getInt(11));
                else
                    serializer.attribute(null, SAMPLERATE, cont.getString(R.string.sconosciuto));


                serializer.attribute(null, NUMBEROFTRACKS, "" + songs.getInt(12));

                if (songs.getString(13) != null)
                    serializer.attribute(null, TAPEWIDTH, songs.getString(13));
                else
                    serializer.attribute(null, TAPEWIDTH, cont.getString(R.string.sconosciuto));

                if (songs.getString(15) != null)
                    serializer.attribute(null, XMLID, "" + songs.getInt(15));
                else
                    serializer.attribute(null, XMLID, cont.getString(R.string.sconosciuto));

                //prendo l'id della canzone
                int songId = songs.getInt(0);

                //interrogo il database per conoscere le track legate alla canzone
                String selection = MagnetophoneOpenHelper.TRACKSONG + " = " + songId;
                Cursor tracks = db.query(MagnetophoneOpenHelper.TRACK, null, selection, null, null, null, null, null);
                tracks.moveToFirst();
                for (int j = 0; j < tracks.getCount(); j++) {
                    //apriamo la track
                    serializer.startTag(null, TRACK);

                    serializer.attribute(null, ID, "" + tracks.getInt(0));
                    serializer.attribute(null, NAME, tracks.getString(1));
                    serializer.attribute(null, PATH, tracks.getString(2));
                    serializer.attribute(null, INDEX, "" + tracks.getInt(3));
                    serializer.attribute(null, MONO, "" + tracks.getInt(4));
                    serializer.attribute(null, "RESPID", "" + tracks.getInt(5));

                    serializer.endTag(null, TRACK);
                    tracks.moveToNext();
                }
                tracks.close();

                //interrogo il database per conoscere il video correlato
                selection = MagnetophoneOpenHelper.VIDEOSONG + " = " + songId;
                Cursor video = db.query(MagnetophoneOpenHelper.VIDEO, null, selection, null, null, null, null, null);
                if (video.getCount() > 0) {
                    video.moveToFirst();

                    serializer.startTag(null, VIDEO);

                    serializer.attribute(null, ID, "" + video.getInt(0));
                    serializer.attribute(null, NAME, video.getString(1));
                    serializer.attribute(null, PATH, video.getString(2));
                    serializer.attribute(null, RESPID, "" + video.getInt(3));

                    serializer.endTag(null, VIDEO);
                }
                video.close();

                //interrogo il database per conoscere la cartella foto correlata
                selection = MagnetophoneOpenHelper.PHOTOSSONG + " = " + songId;
                Cursor photos = db.query(MagnetophoneOpenHelper.PHOTOS, null, selection, null, null, null, null, null);
                if (photos.getCount() > 0) {
                    photos.moveToFirst();

                    serializer.startTag(null, PHOTOS);

                    serializer.attribute(null, ID, "" + photos.getInt(0));
                    serializer.attribute(null, NAME, photos.getString(1));
                    serializer.attribute(null, PATH, photos.getString(2));
                    serializer.attribute(null, RESPID, "" + photos.getInt(3));

                    serializer.endTag(null, PHOTOS);
                }

                serializer.startTag(null, DESCRIPTION);
                if (songs.getString(14) != null)
                    serializer.text(songs.getString(14));
                else
                    serializer.text(cont.getString(R.string.description_not_available));
                serializer.endTag(null, DESCRIPTION);

                //chiudiamo il tag song
                serializer.endTag(null, SONG);

                songs.moveToNext();

            }
            songs.close();
            db.close();

            //chiudo con il tag songsdata
            serializer.endTag(null, SONGSDATA);

            serializer.endDocument();
            //write xml data into the FileOutputStream
            serializer.flush();
            //finally we close the file stream
            fileos.close();

            String text = cont.getString(R.string.xml_correctly_written);
            int duration = Toast.LENGTH_SHORT;
            //do conferma dell'avvenuta trascrittura
            Toast toast = Toast.makeText(cont, text, duration);
            toast.show();
        } catch (Exception e) {
            Log.e("DatabaseConverter", "Exception " + e.getMessage() + ":error occurred while creating xml file");
            String text = "Problemi nella scrittura";
            int duration = Toast.LENGTH_SHORT;
            //do conferma dell'avvenuta trascrittura
            Toast toast = Toast.makeText(cont, text, duration);
            toast.show();
        }


    }//fine exportTheDatabase

}

