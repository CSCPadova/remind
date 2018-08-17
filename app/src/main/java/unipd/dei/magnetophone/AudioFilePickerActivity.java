package unipd.dei.magnetophone;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import static unipd.dei.magnetophone.MusicService.EXT_STORAGE_EQU_FOLDER;
import static unipd.dei.magnetophone.Utility.showSupportActionBar;

/**
 * Activity che serve per scegliere un file audio .wav (il formato supportato da Pure Data)
 * Viene mostrata una lista di file audio, alla pressione di un brano viene fatto partire il player che fa
 * ascoltare il file scelto (viene comodo se non ci si ricorda la registrazione associata al file).
 * Dall'action bar e' possibile stoppare il brano in riproduzione come anteprima e premere un bottone di
 * conferma scelta del file.
 * Se sto gia' riproducendo con il magnetofono, il player non parte per evitare di disturbare l'ascolto.
 */
public class AudioFilePickerActivity extends AppCompatActivity {
    private String fileFormatFilter = ".wav";                //formato dei file da filtrare
    private ListView listView;                                //UI lista
    private String fileName;                                //Path assoluto del file scelto
    private MediaPlayer mp = new MediaPlayer();                //MediaPlayer per la riproduzione al click
    private MusicPlayer player = MusicPlayer.getInstance();    //Istanza del player del magnetofono

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_file_picker);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Permetto la pressione di back nell'action bar
        showSupportActionBar(this, null);

        listView = (ListView) findViewById(R.id.fileList);

        LinkedList<File> fileList = createListOfFilteredFiles(fileFormatFilter);    //Creo una lista di file audio wav

        AudioFileAdapter adapter = new AudioFileAdapter(this, R.layout.audio_picker_layout, fileList);
        listView.setAdapter(adapter);

        //Al click di una canzone la seleziono e la faccio riprodurre
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, final View component, int position, long id) {
                File file = (File) listView.getAdapter().getItem(position);
                fileName = file.getAbsolutePath();

                //Se non sto riproducendo gia' con il magnetofono faccio ascoltare il file
                //Altrimenti non disturbo la riproduzione
                if (!player.isPlaying()) {
                    try {
                        //Seleziono un altro brano, quindi resetto il file
                        if (mp.isPlaying())
                            mp.reset();

                        //Preparo il MediaPlayer
                        mp.setDataSource(fileName);
                        mp.prepare();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //Riproduco il file
                    mp.start();
                }
            }
        });

    }

    /**
     * Metodo che provvede a creare una lista di files filtrati tramite il formato : filter
     *
     * @param filter : formato dei file che si vuole avere
     */
    private LinkedList<File> createListOfFilteredFiles(String filter) {

        Uri externalUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(externalUri, filePathColumn, null, null, null);
        LinkedList<File> fileList = new LinkedList<File>();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String filePath;

        //Filtro i file con il filtro fileFormatFilter
        for (int i = 0; i < cursor.getCount(); i++) {
            cursor.moveToPosition(i);
            filePath = cursor.getString(columnIndex);
            //esclude i file .wav delle risposte impulsive che l'app copia nell'external storage
            if (filePath.endsWith(filter)&&!filePath.contains("/"+EXT_STORAGE_EQU_FOLDER+"/"))
                fileList.add(new File(filePath));
        }

        cursor.close();

        return fileList;
    }


    /**
     * Metodo che crea l'actionBar
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.audio_action_bar, menu);
        return true;
    }

    /**
     * Metodo chiamato alla sospensione dell'activity, si occupa di rilasciare il mediaPlayer
     * nel caso succeda
     */
    protected void onPause() {
        super.onPause();
        mp.release();
    }

    /**
     * Metodo chiamato tornando all'activity dopo averla messa in pausa, ricrea il mediaplayer
     * in quanto in onPause viene rilasciato
     */
    protected void onResume() {
        super.onResume();
        mp = new MediaPlayer();
    }

    /**
     * Metodo che controlla e gestisce i click sulle icone della actionBar
     * STOP: se si e' selezionata una canzone ed e' in riproduzione, la stoppa
     * OK: conferma il file selezionato
     * HOME: semplice back
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //BACK
            case android.R.id.home:
                onBackPressed();    //Eseguo il normale back di android
                return true;

            //CONFERMA CANZONE
            case R.id.ic_action_choose_audio:
                if (fileName != null) {
                    //Scelgo il file e rilascio MediaPlayer, tornando come risultato
                    //il filename tramite intent
                    Intent intent = new Intent();
                    intent.putExtra("fileAbsPath", fileName);
                    setResult(RESULT_OK, intent);
                    mp.release();
                    finish();
                } else //Non posso ancora premere OK perche' non ho scelto una canzone
                    Toast.makeText(this, getString(R.string.toast4_activity_audio_import), Toast.LENGTH_SHORT).show();
                return true;

            //STOP RIPRODUZIONE
            case R.id.ic_action_stop:
                if (mp.isPlaying())
                    mp.reset();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Adapter che contiene una lista di file audio, usato per la lista dell'activity
     */
    private class AudioFileAdapter extends ArrayAdapter<File> {

        public AudioFileAdapter(Context context, int resource, List<File> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //ViewHolder definita alla fine della classe, contiene references agli elementi del layout
            ViewHolder viewHolder = null;

            if (convertView == null) //se siamo all'inizio della lista, dobbiamo preparare un po' tutto,
            {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.audio_picker_layout, null);
                viewHolder = new ViewHolder();

                viewHolder.filename = (TextView) convertView.findViewById(R.id.Title);
                viewHolder.filename.setTextSize(20);

                convertView.setTag(viewHolder);
            } else
                viewHolder = (ViewHolder) convertView.getTag();

            File audioFile = getItem(position);
            viewHolder.filename.setText(audioFile.getName());
            return convertView;
        }

        private class ViewHolder {
            public TextView filename;
        }
    } //Fine adapter

}
