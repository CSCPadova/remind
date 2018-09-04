package unipd.dei.magnetophone.activity.pdf;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
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
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import unipd.dei.magnetophone.R;

import static unipd.dei.magnetophone.utility.Utility.showSupportActionBar;

/**
 * Activity che serve per scegliere un file audio .wav (il formato supportato da Pure Data)
 * Viene mostrata una lista di file audio, alla pressione di un brano viene fatto partire il player che fa
 * ascoltare il file scelto (viene comodo se non ci si ricorda la registrazione associata al file).
 * Dall'action bar e' possibile stoppare il brano in riproduzione come anteprima e premere un bottone di
 * conferma scelta del file.
 * Se sto gia' riproducendo con il magnetofono, il player non parte per evitare di disturbare l'ascolto.
 */
public class PDFFilePickerActivity extends AppCompatActivity {
    private String fileFormatFilter = ".pdf";                //formato dei file da filtrare
    private ListView listView;                                //UI lista
    private String fileName;                                //Path assoluto del file scelto

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_file_picker);

        //Permetto la pressione di back nell'action bar
        showSupportActionBar(this, null, getWindow().getDecorView());

        listView = (ListView) findViewById(R.id.fileList);

        LinkedList<File> fileList = createListOfFilteredFiles(fileFormatFilter);    //Creo una lista di file pdf

        PDFFileAdapter adapter = new PDFFileAdapter(this, R.layout.pdf_picker_layout, fileList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, final View component, int position, long id) {
                File file = (File) listView.getAdapter().getItem(position);
                fileName = file.getAbsolutePath();
            }
        });

    }

    /**
     * Metodo che provvede a creare una lista di files filtrati tramite il formato : filter
     *
     * @param filter : formato dei file che si vuole avere
     */
    private LinkedList<File> createListOfFilteredFiles(String filter) {

        ContentResolver cr = this.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");

        // every column, although that is huge waste, you probably need
        // BaseColumns.DATA (the path) only.
        String[] projection = null;

        // exclude media files, they would be here also.
        String selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;
        String[] selectionArgs = null; // there is no ? in selection so null here

        String sortOrder = null; // unordered
        Cursor allNonMediaFiles = cr.query(uri, projection, selection, selectionArgs, sortOrder);

        // only pdf
        String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf");
        String[] selectionArgsPdf = new String[]{mimeType};
        Cursor allPdfFiles = cr.query(uri, projection, selectionMimeType, selectionArgsPdf, sortOrder);

        LinkedList<File> fileList = new LinkedList<File>();
        String[] filePathColumn = {MediaStore.Files.FileColumns.DATA};
        int columnIndex = allPdfFiles.getColumnIndex(filePathColumn[0]);
        String filePath;

        //Filtro i file con il filtro fileFormatFilter
        for (int i = 0; i < allPdfFiles.getCount(); i++) {
            allPdfFiles.moveToPosition(i);
            filePath = allPdfFiles.getString(columnIndex);
            //esclude i file .wav delle risposte impulsive che l'app copia nell'external storage
            fileList.add(new File(filePath));
        }

        allNonMediaFiles.close();
        allPdfFiles.close();
        return fileList;
    }


    /**
     * Metodo che crea l'actionBar
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.pdf_action_bar, menu);
        return true;
    }

    /**
     * Metodo chiamato alla sospensione dell'activity, si occupa di rilasciare il mediaPlayer
     * nel caso succeda
     */
    protected void onPause() {
        super.onPause();
    }

    /**
     * Metodo chiamato tornando all'activity dopo averla messa in pausa, ricrea il mediaplayer
     * in quanto in onPause viene rilasciato
     */
    protected void onResume() {
        super.onResume();
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

            //CONFERMA
            case R.id.ic_action_choose_pdf:
                if (fileName != null) {
                    //Scelgo il file e rilascio MediaPlayer, tornando come risultato
                    //il filename tramite intent
                    Intent intent = new Intent();
                    intent.putExtra("filePDFAbsPath", fileName);
                    setResult(RESULT_OK, intent);
                    finish();
                } else //Non posso ancora premere OK perche' non ho scelto un pdf
                    Toast.makeText(this, getString(R.string.toast4_activity_audio_import), Toast.LENGTH_SHORT).show();
                return true;

            //ANTEPRIMA
            case R.id.ic_action_preview:
                Intent i = new Intent(this, PDFActivity.class);
                String[] split=fileName.split("/");
                String path="";
                for (int j=0;j<split.length-1;j++)
                    path=path+"/"+split[j];
                i.putExtra("path", path);
                i.putExtra("file", split[split.length-1]);
                this.startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Adapter che contiene una lista di file audio, usato per la lista dell'activity
     */
    private class PDFFileAdapter extends ArrayAdapter<File> {

        public PDFFileAdapter(Context context, int resource, List<File> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //ViewHolder definita alla fine della classe, contiene references agli elementi del layout
            ViewHolder viewHolder = null;

            if (convertView == null) //se siamo all'inizio della lista, dobbiamo preparare un po' tutto,
            {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.pdf_picker_layout, null);
                viewHolder = new ViewHolder();

                viewHolder.filename = (TextView) convertView.findViewById(R.id.Title);
                viewHolder.filename.setTextSize(20);

                convertView.setTag(viewHolder);
            } else
                viewHolder = (ViewHolder) convertView.getTag();

            File pdfFile = getItem(position);
            viewHolder.filename.setText(pdfFile.getName());
            return convertView;
        }

        private class ViewHolder {
            public TextView filename;
        }
    } //Fine adapter

}
