package unipd.dei.magnetophone;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.LinkedList;

/**
 * Questa Activity produce la lista dei brani.
 * i brani sono presi tramite importazione con l'apposita activity oppure tramite definizione
 * in file XML posti nella cartella Magnetophone
 * Per evitare troppi refresh, il controllo delle modifiche fatte nella cartella
 * Magnetophone viene fatto ogni volta che l'applicazione si apre per la prima volta
 * nel dispositivo, ogni volta che viene tolto/aggiunto un file nella cartella
 * ed ogni volta che è passato più di un giorno dall'ultimo aggiornamento.
 * <p>
 * L'activity si preoccupa altresì di creare una sorta di barra in cima la lista ogni volta
 * che l'utente seleziona una canzone con immagine di copertina, nome autore e titolo canzone,
 * oltre ad una progressbar che permette all'utente di seguire la progressione della traccia
 * anche quando non è nell'activity magnetofono ed a sapere sempre quale traccia sta riproducendo.
 * <p>
 * nota per i programmatori: le shared preferences usate qui dentro sono su diversi file i cui nomi sono:
 * •service: per la canzone che viene mandata nel servizio e quindi, di fatto, contiene i dati della canzone
 * il cui nastro è attualmente sul magnetofono
 * <p>
 * •song_bar: per la canzone che attualmente è sulla barra di stato mostrata in cima alla UI, e non necessariamente
 * caricata nel servizio, quindi non necessariamente il suo nastro si trova nel magnetofono
 * <p>
 * peraltro le chiave utilizzate in tutti gli shared peferences sono del tipo song_<tipo di dato>, come ad esempio
 * song_name, song_speed, song_equalization etc.
 * Tutti i caratteri nella chiave sono minuscoli
 */

public class SongList extends Activity {
    private static long lastModified;
    private LinkedList<Song> finalList;            //Lista di Song caricate dal database, passate all'adapter
    private ListView listView;                    //Layout della lista definito in list_layout
    private LinearLayout currentSongBarlayout;    //Layout utilizzato per la currentSongBar
    private LinearLayout currentSongDetail;
    private XmlImport imp;
    private ProgressBar progress;
    private Intent toMagnetophone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_layout);

        if(getActionBar()!=null)
            getActionBar().setDisplayHomeAsUpEnabled(true);

        imp = new XmlImport();

        //impostiamo il Layout con la lista
        listView = findViewById(R.id.listViewDemo);
        //verifichiamo se serve un refresh e, in caso serva, lo effettuiamo
        manageRefresh(this);

        CustomAdapterList adapter = new CustomAdapterList(this, R.layout.custom_layout, finalList);
        listView.setAdapter(adapter);

        createCurrentSongBar();

        //gestione dei click sugli elementi della lista
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adattatore, final View componente, int pos, long id) {
                Song s = finalList.get(pos);

                //avviso la barra che, essendo stato selezionato un brano, ha diritto a comparire
                SharedPreferences songPref = getSharedPreferences("song_bar", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = songPref.edit();
                editor.putBoolean("validBar", true);
                editor.commit();

                //aggiorno le informazioni sulla canzone selezionata nelle preferences
                fillPreferences(s, "song_bar");

                //controllo che la canzone scelta non sia già quella in riproduzione
                SharedPreferences songInCharge = getSharedPreferences("service", Context.MODE_PRIVATE);
                int idInCharge = songInCharge.getInt("song_id", -1);
                if (idInCharge == s.getId()) {
                    manageBar();
                } else
                    manageBarTemp();
            }
        });

        //registro la lista affinché quando un utente ci clicca per un lungo tempo, essa faccia apparire un context menu
        registerForContextMenu(listView);

        manageBar();
    }//fine onCreate


    //nuovo metodo: serve al bottone carica nastro per caricare il nastro sul magnetofono, in sostanza fa partire l'intent
    //attivato quando si preme il pulsante carica nastro
    public void toMagnetophone(View view) {
        //avviso la barra che, essendo stato selezionato un brano, ha diritto a comparire
        SharedPreferences songPref = getSharedPreferences("service", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = songPref.edit();
        editor.putBoolean("validBar", true);
        editor.commit();

        toMagnetophone = new Intent(this, MagnetophoneActivity.class);
        toMagnetophone.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //non si vuole che con back si torni alle opzioni dalla lista dei brani

        Song s = getSongFromPreferences("song_bar");
        fillPreferences(s, "service");
        Song.fillIntent(s, toMagnetophone);

        MusicPlayer player = MusicPlayer.getInstance();
        //Se seleziono la stessa canzone che sto già riproducendo non faccio niente
        if (!(player.getSong() != null && player.getSong().getId() == s.getId() && player.isPlaying())) {
            //player.restartService();	//Aggiorno il servizio
            player.setSong(s);
        }
        startActivity(toMagnetophone);
    }

    /**
     * metodo che restituisce una canzone creandola dai dati del file sharedPreferences il cui
     * nome è passato come parametro
     * @param preferences: il nome delle sharedPreferences da cui si prendono le informazioni
     * @return
     */
    public Song getSongFromPreferences(String preferences) {
        Song s = new Song();
        //prendo le sharedPreferences della canzone salvata solo sulla barra
        SharedPreferences songPref = this.getSharedPreferences(preferences, Context.MODE_PRIVATE);

        int id = songPref.getInt("song_id", 0);
        String name = songPref.getString("song_name", "");
        String author = songPref.getString("song_author", "");
        String equalization = songPref.getString("song_equalization", "-");
        Float speed = songPref.getFloat("song_speed", 3.75f);
        String year = songPref.getString("song_year", "-");

        String signature = songPref.getString("song_signature", "-");
        String provenance = songPref.getString("song_provenance", "-");
        float duration = songPref.getFloat("song_duration", -1);
        String extension = songPref.getString("song_extension", "-");

        int bitdepth = songPref.getInt("song_bitdepth", -1);
        int samplerate = songPref.getInt("song_samplerate", -1);
        int numberOfTracks = songPref.getInt("song_numberoftracks", -1);

        String tapewidth = songPref.getString("song_tapewidth", "-");
        String description = songPref.getString("song_description", "-");


        //setto tutti i valori per la song
        s.setId(id);
        s.setTitle(name);
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
//		s.setNumberOfTracks(numberOfTracks);
        s.setTapeWidth(tapewidth);
        s.setDescription(description);

        //per ogni traccia presente, la aggiungo nella song
        for (int i = 1; i <= numberOfTracks; i++) {
            s.setTrack(songPref.getString("song_track_" + i, ""), songPref.getInt("song_index_" + i, -1));
        }

        return s;
    }


    //nuovo metodo: gestisce la barra quando abbiamo selezionato un oggetto
    public void manageBarTemp() {
        Song s = getSongFromPreferences("song_bar");

        setCurrentSongBar(s);
        setProgressBar(0);
        showCurrentSongBar();
    }


    /**
     * inserisce nelle shared preferences i dati della canzone s
     * @param s
     * @param preferences
     */
    public void fillPreferences(Song s, String preferences) {
        SharedPreferences songPref = this.getSharedPreferences(preferences, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = songPref.edit();
        editor.putString("song_name", s.getTitle());
        editor.putString("song_author", s.getAuthor());
        editor.putInt("song_id", s.getId());
        editor.putString("song_equalization", s.getEqualization());
        editor.putFloat("song_speed", s.getSpeed());
        editor.putString("song_year", s.getYear());

        editor.putString("song_signature", s.getSignature());
        editor.putString("song_provenance", s.getProvenance());
        editor.putFloat("song_duration", s.getDuration());
        editor.putString("song_extension", s.getExtension());

        editor.putInt("song_bitdepth", s.getBitDepth());
        editor.putInt("song_samplerate", s.getSampleRate());
        editor.putInt("song_numberoftracks", s.getNumberOfTracks());

        editor.putString("song_tapewidth", s.getTapeWidth());
        editor.putString("song_description", s.getDescription());

        //salvo i dati che mi interessano per la track: path e canale
        for (int i = 1; i <= s.getNumberOfTracks(); i++) {
            editor.putString("song_track_" + i, s.getTrackAtIndex(i - 1).getPath());
            editor.putInt("song_index_" + i, s.getTrackAtIndex(i - 1).getIndex());
        }

        editor.commit();
    }

    //metodo che crea il menu ottenuto con il lungo tocco
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        //Inserisco nel menu elimina solo se attivo nelle impostazioni
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean("EliminareImpostazioni", false) && sharedPref.getBoolean("ModificareImpostazioni", false))//mostro tutto
            inflater.inflate(R.menu.menu2, menu);
        else if (!sharedPref.getBoolean("EliminareImpostazioni", false) && sharedPref.getBoolean("ModificareImpostazioni", false))
            inflater.inflate(R.menu.menu_singolo_brano_senza_elimina, menu);
        else if (sharedPref.getBoolean("EliminareImpostazioni", false) && !sharedPref.getBoolean("ModificareImpostazioni", false))
            inflater.inflate(R.menu.menu_senza_importa, menu);
        else
            inflater.inflate(R.menu.menu_solo_annulla, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        //gestiamo le scelte sul menù di opzioni della singola canzone
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        MusicPlayer player = MusicPlayer.getInstance();
        switch (item.getItemId()) {
            //ELIMINA
            case R.id.menu1:
                //Se sto riproducendo la canzone che voglio eliminare, non lo permetto
                if (player.isPlaying() && player.getSong().getId() == (((CustomAdapterList) listView.getAdapter()).getItem((int) info.id).getId()))
                    Toast.makeText(getApplicationContext(), getString(R.string.playing_song_alert), Toast.LENGTH_SHORT).show();
                else {
                    //Ottengo la posizione nella lista
                    int selectedPosition = info.position;
                    CustomAdapterList adapter = ((CustomAdapterList) listView.getAdapter());
                    Song s = adapter.getItem(selectedPosition);
                    adapter.remove(s); //Elimino la canzone dalla lista dell'adapter (finallist)
                    deleteSongFromDatabase(s);
                    adapter.notifyDataSetChanged(); //Refresh della lista

                    //Se è la canzone in riproduzione ed è in stop
                    if (player.getPlayerSong() != null && s.getId() == player.getPlayerSong().getId()) {
                        player.setSong(null);
                    }
                    //cancello la bar e setto il parametro che la avvisa se è autorizzata a comparire a false
                    currentSongBarlayout = (LinearLayout) findViewById(R.id.showMusic);
                    currentSongBarlayout.setVisibility(View.GONE);

                    currentSongDetail = (LinearLayout) findViewById(R.id.song_detail);
                    currentSongDetail.setVisibility(View.GONE);

                    SharedPreferences songPref = getSharedPreferences("service", Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = songPref.edit();
                    editor.putBoolean("validBar", false);
                    editor.commit();

                }
                break;

            //OPZIONI del singolo brano identificato dall'id
            case R.id.menu2:
                //Se sto riproducendo la canzone che voglio modificare, non lo permetto
                if (player.isPlaying() && player.getSong().getId() == (((CustomAdapterList) listView.getAdapter()).getItem((int) info.id).getId()))
                    Toast.makeText(getApplicationContext(), getString(R.string.playing_song_alert), Toast.LENGTH_SHORT).show();
                else
                    goToSingleSongSettingsActivity(info.id);

                break;
            //ANNULLA
            case R.id.menu3:
                //Non faccio nulla dato che e' il pulsante ANNULLA
                break;
        }
        return true;
    }

    /**
     * Metodo che si occupa di creare una barra per la canzone attualmente in
     * riproduzione, di default viene creata ma tenuta immediatamente nascosta
     * all'utente. Verra' mostrata alla selezione di una canzone.
     * Fa lo stesso con il dettaglio della canzone
     */
    private void createCurrentSongBar() {
        currentSongBarlayout = (LinearLayout) findViewById(R.id.showMusic);
        currentSongBarlayout.setVisibility(View.GONE);
        currentSongDetail = (LinearLayout) findViewById(R.id.song_detail);
        currentSongDetail.setVisibility(View.GONE);
    }

    /**
     * Cancella la canzone passata come parametro dal database
     * La canzone e' per forza presente nel DB perche' e' una canzone
     * selezionata da una lista di canzoni caricata precedentemente da esso.
     * @param s Song s da eliminare
     */
    private void deleteSongFromDatabase(Song s) {
        DatabaseManager dbManager = new DatabaseManager(this);
        dbManager.removeSingleSongFromDatabase(s);
    }

    /**
     * Carica nel database le canzoni presenti nei file XML nel caso di modifiche, dopodichè
     * riempie la finalList (variabile globale) con gli oggetti Song presi
     * dal database aggiornato
     */
    private void loadListFromDatabaseAndXml() {
        // creo/apro il database
        MagnetophoneOpenHelper dbHelper = new MagnetophoneOpenHelper(SongList.this);
        //invoco il metodo che si occupa di importre i file XML nuovi o modificati
        imp.importazioneXML(this, dbHelper);

        loadListFromDatabase();
    }

    /**
     * metodo che carica nella lista tutti i brani nel database
     */
    private void loadListFromDatabase() {
        MagnetophoneOpenHelper dbHelper = new MagnetophoneOpenHelper(SongList.this);
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        //faccio ora una richiesta per conoscere il numero di song nel database
//		String query = "SELECT " + MagnetophoneOpenHelper.SONGID + " FROM " + MagnetophoneOpenHelper.SONG + ";";
//		Cursor cursor = db.rawQuery(query, null);

        Cursor cursor = db.query(MagnetophoneOpenHelper.SONG, null, null, null, null, null, null, null);
        int cursorLength = cursor.getCount();

        finalList = new LinkedList<Song>();
        for (int j = 0; j < cursorLength; j++) {
            cursor.moveToPosition(j);
            int songid = cursor.getInt(0);
            Song new_song = DatabaseManager.getSongFromDatabase(songid, this);
            //carico nella lista la song ottenuta dal database con id songid
            finalList.add(new_song);
        }
        db.close();
    }


    /**
     * Passa all'activity delle opzioni del singolo brano passando le informazioni relative
     * @param id
     */
    public void goToSingleSongSettingsActivity(long id) {
        Intent intent = new Intent(this, SingleSongSettingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //non si vuole che con back si torni alle opzioni dalla lista dei brani
        Song s = (Song) listView.getAdapter().getItem((int) id);
        Song.fillIntent(s, intent);
        startActivity(intent);
    }

    /**
     * Metodo che crea l'actionBar, inserisco l'icona di importazione solo se abilitata nelle
     * opzioni generali del magnetofono
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        //Soltanto se ho importa canzoni attivo carico l'icona per importare!
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        if (sharedPref.getBoolean("ImportareImpostazioni", true)) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.action_bar_menu, menu);
        }
        return true;
    }

    //########## metodo per gestire il click sul menu di importazione  ##########

    /**
     * Metodo che controlla e gestisce i click sulle icone della actionBar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {    // BACK
            case android.R.id.home:
                onBackPressed();
                return true;
            //IMPORTA
            case R.id.ic_action_import:
                //qui si deve chiamare l'activity che permette di fare l'import
                Intent intent = new Intent(this, ImportSongActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * metodo che si preoccupa di controllare se ci sono le condizioni scelte per avviare il refresh
     * dei dati dalla cartella dei file xml.
     * Se così è, esegue il refresh del database e della lista dei brani
     */
    private void manageRefresh(Context context) {
        SharedPreferences sharedPref = this.getSharedPreferences("last_modified", Context.MODE_PRIVATE);
        lastModified = sharedPref.getLong("LastModified", 0);

        //creiamo o controlliamo che esista già la cartella contenente i file XML
        File xmlDirectory = new File(XmlImport.getCurrentDirectory(context));
        xmlDirectory.mkdir();

        long currentModified = xmlDirectory.lastModified();

        int time = checkTheFirstTime();//guardo se questa è la prima volta che l'applicazione parte

        long now = System.currentTimeMillis();
        long nextRefresh = sharedPref.getLong("NextRefresh", 0);

        if (currentModified > lastModified || time == 0 || now > nextRefresh) {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putLong("LastModified", currentModified);
            editor.putLong("NextRefresh", now + 86400000);//non si farà refresh se l'utente accederà ancora alla lista nelle
            //prossime 24 ore
            editor.commit();

            //Carichiamo tutte le canzoni del database con le relative informazioni
            loadListFromDatabaseAndXml();
        } else
            loadListFromDatabase();
    }

    private int checkTheFirstTime() {
        SharedPreferences pref = this.getSharedPreferences("first_time", Context.MODE_PRIVATE);
        int toReturn = pref.getInt("FirstTime", 0);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("FirstTime", toReturn + 1);
        editor.commit();

        return toReturn;
    }

    @Override
    public void onRestart() {
        super.onRestart();  // Always call the superclass method first
        manageBar();
    }

    public void onResume() {
        super.onResume();
        manageBar();
    }

    /**
     * gestisce la bar che mostra la song in riproduzione ogni volta che si ritorna sulla lista
     */
    public void manageBar() {
        MusicPlayer player = MusicPlayer.getInstance();
        Song s = player.getSong();

        SharedPreferences songPref = this.getSharedPreferences("service", Context.MODE_PRIVATE);
        boolean valid = songPref.getBoolean("validBar", false);

        if (valid) {
            if (s != null)//la canzone è in riproduzione
            {
                setCurrentSongBar(s);                    //Imposto la canzone da mostrare nella currentSongBar
                showCurrentSongBar();                    //Mostra la currentSongBar
                startProgressBar();                        //facciamo partire l'aggiornamento della progressBar
            } else//può essere che la canzone non sia in riproduzione attualmente, in quel caso getSong ritorna null
            {
                String song = songPref.getString("song_name", "");
                String author = songPref.getString("song_author", "");
                float position = songPref.getFloat("CurrentSecond", 0);

                Song shortSong = new Song();

                shortSong.setTitle(song);
                shortSong.setAuthor(author);

                setCurrentSongBar(shortSong);
                setProgressBar((int) position);
                //mostriamo la bar solo se effettivamente c'è qualcosa che è "sui nastri"
                if (!(song.equals("")))
                    showCurrentSongBar();
            }
        }
    }


    /**
     * Metodo che imposta la currentSongBar con la Song passata, impostando i valori
     * di nome autore, titolo canzone e foto. Inoltre gestisce il dettaglio a lato della canzone
     * @param s
     */
    private void setCurrentSongBar(Song s) {
        TextView runningSong = (TextView) findViewById(R.id.CurrentSong);
        String repSong = s.getTitle() + " " + s.getAuthor();
        runningSong.setText(repSong);

        //aggiorno il dettaglio della canzone
        TextView equalization = (TextView) findViewById(R.id.detail_equalization);
        equalization.setText("Equalizazione: " + s.getEqualization());

        TextView speed = (TextView) findViewById(R.id.detail_speed);
        speed.setText("Velocità: " + s.getSpeed());

        TextView signature = (TextView) findViewById(R.id.detail_signature);
        signature.setText("Signature: " + s.getSignature());

        TextView provenance = (TextView) findViewById(R.id.detail_provenance);
        provenance.setText("Provenienza: " + s.getProvenance());

        TextView duration = (TextView) findViewById(R.id.detail_duration);
        duration.setText("Lunghezza: " + s.getDuration());

        TextView extension = (TextView) findViewById(R.id.detail_extension);
        extension.setText("Estensione: " + s.getExtension());

        TextView bitdepth = (TextView) findViewById(R.id.detail_bitdepth);
        bitdepth.setText("Bit Depth: " + s.getBitDepth());

        TextView samplerate = (TextView) findViewById(R.id.detail_samplerate);
        samplerate.setText("Sample Rate: " + s.getSampleRate());

        TextView numberoftracks = (TextView) findViewById(R.id.detail_numberoftracks);
        numberoftracks.setText("Numero Tracce: " + s.getNumberOfTracks());

        TextView tapewidth = (TextView) findViewById(R.id.detail_tapewidth);
        tapewidth.setText("Larghezza Nastro: " + s.getTapeWidth());

    }

    /**
     * Metodo che mostra la currentSongBar all'utente
     */
    private void showCurrentSongBar() {
        currentSongBarlayout.setVisibility(View.VISIBLE);
        currentSongDetail.setVisibility(View.VISIBLE);
    }

    /**
     * da il via all'aggiornamento della progressBar, invocato solo se la canzone è in riproduzione
     */
    public void startProgressBar() {
        MusicPlayer player = MusicPlayer.getInstance();
        progress = (ProgressBar) findViewById(R.id.progressBar);
        progress.setMax((int) player.getCurrentSongLength());
        //progress.setProgress((int)player.getCurrentSongProgress());
        (new BarUpdaterThread()).start();
    }

    /**
     * setta la progress bar al valore passato come parametro
     * @param position
     */
    public void setProgressBar(int position) {
        progress = (ProgressBar) findViewById(R.id.progressBar);
        SharedPreferences songPref = getSharedPreferences("service", Context.MODE_PRIVATE);
        float maxLength = songPref.getFloat("song_recovery_length", 0);

        if (maxLength >= position && maxLength > 0) {
            progress.setMax((int) maxLength);
            progress.setProgress(position);
        } else {
            //l'utente ha caricato la canzone ed è tornato indietro subito alla libreria, non facendo nulla
            //mostra la barra vuota
        }
    }

    public class BarUpdaterThread extends Thread {
        SharedPreferences songPref = getSharedPreferences("service", Context.MODE_PRIVATE);

        @Override
        public void run() {
            MusicPlayer player = MusicPlayer.getInstance();
            float currentPosition = 0;
            int total = (int) player.getCurrentSongLength();//lunghezza totale della canzone
            SharedPreferences.Editor editor = songPref.edit();
            do {
                try {
                    Thread.sleep(500);
                    //currentPosition= player.getCurrentSongProgress();
                    editor.putFloat("CurrentSecond", currentPosition);
                    editor.commit();
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    return;
                }
                progress.setProgress((int) currentPosition);
            }
            while (currentPosition < total && player.isPlaying());

            progress.setProgress(total);
        }
    }

}
