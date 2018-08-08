package unipd.dei.magnetophone;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * nostra versione di OpenHelper
 * Presenti due tabelle nel database, una per le canzoni ed una per gli xml che eventualmente
 * le contengono
 */
public class MagnetophoneOpenHelper extends SQLiteOpenHelper {
    //###############COSTANTI################
    //#################### NOMI DELLE TABELLE #######################
    public static final String SONG = "SongsRecords";//nome della tabella dei brani
    public static final String XML = "xmlRecords";//nome della tabella degli XML
    public static final String TRACK = "songsTracks";
    public static final String VIDEO = "Video";
    public static final String PHOTOS = "Photos";
    //############ ATTRIBUTI DELLA TABELLA SONG #####################
    public static final String SONGID = "_id";
    public static final String TITLE = "title";
    public static final String AUTHOR = "author";
    public static final String YEAR = "year";
    public static final String SPEED = "speed";
    public static final String EQUALIZATION = "equalization";
    public static final String SIGNATURE = "Signature";
    public static final String PROVENANCE = "Provenance";
    public static final String DURATION = "Duration";
    public static final String EXTENSION = "Extension";
    public static final String BITDEPTH = "Bitdepth";
    public static final String SAMPLERATE = "Sample_Rate";
    public static final String NUMBEROFTRACKS = "Number_Of_Tracks";
    public static final String TAPEWIDTH = "Tape_Width";
    public static final String DESCRIPTION = "Description";
    public static final String XMLIDS = "respective_xml";
    // ########## ATTRIBUTI DELLA TABELLA XML ###############
    public static final String XMLID = "xmlid";//id del relativo XML
    public static final String NOMEFILE = "nome_file";//nome del file xml
    public static final String DATAMODIFICA = "data_modifica";//data dell'ultima modifica
    public static final String TRACKID = "Track_Id";
    public static final String TRACKNAME = "Track_Name";

    // ########### ATTRIBUTI DELLA TABELLA TRACK ###############
    public static final String TRACKPATH = "Track_Path";
    public static final String INDEX = "Track_Index";
    public static final String MONO = "Track_Mono";
    public static final String TRACKSONG = "Respective_Track_Song";
    public static final String VIDEOID = "Video_Id";
    public static final String VIDEONAME = "Video_Name";

    // ########### ATTRIBUTI DELLA TABELLA VIDEO ###############
    public static final String VIDEOPATH = "Video_Path";
    public static final String VIDEOSONG = "Respective_Video_Song";
    // ########## ATTRIBUTI DELLA TABELLA PHOTOS ###############
    public static final String PHOTOSID = "Photos_Id";
    public static final String PHOTOSNAME = "Photos_Name";
    public static final String PHOTOSPATH = "Photos_Path";
    public static final String PHOTOSSONG = "Respective_Photos_Song";
    private static final String DATABASE_NAME = "magnetophonedb.db";
    private static final int DATABASE_VERSION = 1;


    public MagnetophoneOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    public void onCreate(SQLiteDatabase db) {
        //tabella delle canzoni
        String sql = "create table " + SONG + "( " +
                SONGID + " integer primary key, " +
                TITLE + " text, " +
                AUTHOR + " text, " +
                YEAR + " text, " +
                SPEED + " float, " +
                EQUALIZATION + " text, " +
                SIGNATURE + " text, " +
                PROVENANCE + " text, " +
                DURATION + " float, " +
                EXTENSION + " text, " +
                BITDEPTH + " integer, " +
                SAMPLERATE + " integer, " +
                NUMBEROFTRACKS + " integer, " +
                TAPEWIDTH + " text," +
                DESCRIPTION + " text, " +
                XMLIDS + " integer," +
                "FOREIGN KEY( " + XMLIDS + " ) REFERENCES " +
                XML + "(" + XMLID + ") ON DELETE CASCADE ON UPDATE CASCADE);";
        db.execSQL(sql);

        //tabella dei file xml
        sql = "create table " + XML + "( " +
                XMLID + " integer primary key autoincrement, " +
                NOMEFILE + " text, " +
                DATAMODIFICA + " long);";
        db.execSQL(sql);

        //Tabella delle track
        sql = "create table " + TRACK + "( " +
                TRACKID + " integer primary key, " +
                TRACKNAME + " text, " +
                TRACKPATH + " text, " +
                INDEX + " integer, " +
                MONO + " integer, " +
                TRACKSONG + " integer, " +
                "FOREIGN KEY(" + TRACKSONG + ") REFERENCES " +
                SONG + "(" + SONGID + ") ON DELETE CASCADE ON UPDATE CASCADE"
                + ");";
        db.execSQL(sql);

        //Tabella dei Video
        sql = "create table " + VIDEO + "( " +
                VIDEOID + " integer primary key, " +
                VIDEONAME + " text, " +
                VIDEOPATH + " text, " +
                VIDEOSONG + " integer, " +
                "FOREIGN KEY(" + VIDEOSONG + ") REFERENCES " +
                SONG + "(" + SONGID + ") ON DELETE CASCADE ON UPDATE CASCADE"
                + ");";

        db.execSQL(sql);

        //Tabella delle foto
        sql = "create table " + PHOTOS + "( " +
                PHOTOSID + " integer primary key, " +
                PHOTOSNAME + " text, " +
                PHOTOSPATH + " text, " +
                PHOTOSSONG + " text, " +
                "FOREIGN KEY(" + PHOTOSSONG + ") REFERENCES " +
                SONG + "(" + SONGID + ") ON DELETE CASCADE ON UPDATE CASCADE"
                + ");";

        db.execSQL(sql);

        db.execSQL("PRAGMA foreign_keys=ON;");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int ewVersion) {
        //per il momento vuoto, siamo alla versione 1
    }

    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }
}
