package unipd.dei.magnetophone.activity.deprecated;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.widget.Toast;

import unipd.dei.magnetophone.R;
import unipd.dei.magnetophone.activity.LibraryActivity;
import unipd.dei.magnetophone.database.DatabaseManager;
import unipd.dei.magnetophone.utility.Song;

import static unipd.dei.magnetophone.utility.Utility.showSupportActionBar;

/**
 * Activity che gestisce le impostazioni di un singolo brano, e' possibile cambiare i seguenti campi:
 * <p>
 * ++++ Impostazioni Generali ++++
 * - Nome del brano
 * - Nome dell'autore
 * - Anno di composizione
 * <p>
 * ++++ Impostazioni Riproduzione +++
 * - Equalizzazione
 * - Velocita' di riproduzione
 * - Tipo di nastro usato
 * <p>
 * Inoltre e' possibile vedere informazioni sui file usati per la riproduzione (uno se mono, due se stereo).
 * <p>
 * Per salvare i dati, l'activity mette a disposizione un popup che chiede di salvare o meno le modifiche all'uscita dell'activity.
 * <p>
 * Le modifiche fatte saranno salvate direttamente su database in maniera permanente.
 */

public class SongEditActivity extends AppCompatActivity {
    private static int MAX_CHARACTER = 30;            //Numero massimo di caratteri per l'inserimento del nome dell'autore e della canzone
    private SettingsFragment frag;                    //Fragment che contiene la lista

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        showSupportActionBar(this, null, getWindow().getDecorView());
        frag = new SettingsFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content, frag).commit();
    }

    /**
     * ActionBar per tornare indietro
     * HOME: Se ho modificato qualcosa chiamo il popup, altrimenti faccio back normale
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home:
                if (!callSaveAlertFragment())
                    onBackPressed(); //altrimenti proseguo normalmente con il tasto BACK
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Metodo che permette di capire se si e' modificato qualcosa di una canzone
     *
     * @return booleano
     */
    private boolean needToSaveData() {
        return frag.isChanged();
    }

//	/**
//	 * Metodo chiamato al ritorno da ImageFilePickerActivity
//	 * Ritorna il path assoluto del file scelto.
//	 */
//	@Override
//	public void onActivityResult(int requestCode, int resultCode, Intent data)
//	{
//		super.onActivityResult(requestCode, resultCode, data);
//		//Se e' andato tutto bene ed e' relativo alle foto
//		if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) 
//		{
//			String filePath = data.getStringExtra("fileAbsPath");	//Prendo il path assoluto
//			frag.prefImageView.setImage(filePath);					//Mostro l'immagine nella view
//			frag.setModified();										//Informo che ho avuto modifiche
//		}
//	}


    /**
     * Metodo che se necessario (dati da salvare) chiama il popup quando si esce,
     * ritorna un boolean che dice se è stato chiamato il popup o meno
     */
    private boolean callSaveAlertFragment() {
        if (needToSaveData()) { //se c'e' stata una modifica chiamo il popup di salvataggio modifiche
            SaveAlertFragment save = new SaveAlertFragment();
            save.show(getFragmentManager(), null);
            return true;
        }
        return false;
    }

    /**
     * Metodo che gestisce la pressione del tasto BACK: richiama il popup di conferma delle modifiche
     * se sono state effettuate, altrimenti normale back.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) //se premo il tasto BACK
        {
            if (!callSaveAlertFragment())
                onBackPressed(); //altrimenti proseguo normalmente con il tasto BACK
            return false; //serve per bloccare sul popup, senno' proseguirebbe subito all'activity precedente
        }
        return super.onKeyDown(keyCode, event);
    }

    //****************************** POPUP FRAGMENT*******************************

    /**
     * Fragment che visualizza un popup menu per salvare o no le modifiche delle impostazioni di un brano
     */
    public static class SaveAlertFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            //Creo un builder per fare il popup
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(R.string.title_alert_activity_single_song_settings));
            builder.setMessage(getString(R.string.message_alert_activity_single_song_settings));

            //Setto il tasto SI e le sue azioni
            builder.setPositiveButton(getString(R.string.yes_alert_activity_single_song_settings), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Song s = ((SongEditActivity) getActivity()).frag.songToUpdate;
                    DatabaseManager db = new DatabaseManager(getActivity());
                    db.updateSong(s);    // Aggiorno la canzone

                    //mostro messaggio di salvataggio effettuato
                    Toast.makeText(getActivity(), getString(R.string.toast2_activity_single_song_settings), Toast.LENGTH_SHORT).show();

                    //informo la lista che dovrà far vedere la nuova canzone importata
                    SharedPreferences shared = getActivity().getSharedPreferences("selected", Context.MODE_PRIVATE);
                    Editor editor = shared.edit();
                    editor.putInt("song_id", s.getId());
                    editor.commit();

                    //Chiamo l'activity della lista dei brani, devo ricaricare la lista quindi la faccio partire da zero
                    Intent intent = new Intent(getActivity(), LibraryActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //con questa rig avviso il detail che deve mostrare la mia song appena modificata
                    intent.putExtra("modify", 1);
                    startActivity(intent);
                }
            });

            builder.setNeutralButton(getString(R.string.neutral_alert_activity_single_song_settings), null);

            //Setto il tasto NO e le sue azioni
            builder.setNegativeButton(getString(R.string.no_alert_activity_single_song_settings), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    getActivity().onBackPressed();  //eseguo l'effettivo BACK
                }
            });
            return builder.create();
        }
    } //fine SaveAlertDialog


    //––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––//
    //############################### SETTINGS FRAGMENT ######################################//
    //––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––//

    /**
     * Questo fragment mostra le opzioni definite nel file xml singlesongsettings
     */
    public static class SettingsFragment extends PreferenceFragment {
        private boolean save_changes = false;        //flag delle modifiche effettuate sul brano, serve per il popup
        private Song songToUpdate;                    //Oggetto canzone che uso per modificare i dati del database

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

//			File temp; //Variabile per ricavare i nomi delle canzoni a partire da path assoluti

            //Carico la lista delle preference
            addPreferencesFromResource(R.xml.singlesongsettings);
            //Prendo l'intent dell'activity precedente per prendere i dati
            Intent intent = getActivity().getIntent();

            songToUpdate = Song.songFromIntent(intent);

            // Accedo a tutte le preferenze
            final EditTextPreference prefSongName = (EditTextPreference) findPreference("nomeCanzoneImpostazioniBrano");
            final EditTextPreference prefAuthorName = (EditTextPreference) findPreference("nomeAutoreImpostazioniBrano");
            final EditTextPreference prefYear = (EditTextPreference) findPreference("annoImpostazioniBrano");

            final ListPreference prefSpeed = (ListPreference) findPreference("listaVelocitaImpostazioniBrano");
            final ListPreference prefEqualization = (ListPreference) findPreference("listaEqualizzazioneImpostazioniBrano");

            final ListPreference prefTapeWidth = (ListPreference) findPreference("SettingsTapeWidth");
            final EditTextPreference prefDescription = (EditTextPreference) findPreference("SettingsDescription");


            // Setto i titoli di default in base al brano
            prefSongName.setTitle(checkUnknownValue(getString(R.string.song_name_title_activity_single_song_settings) +
                    ":" + songToUpdate.getTitle()));

            prefAuthorName.setTitle(getString(R.string.song_author_title_activity_single_song_settings) +
                    ": " + checkUnknownValue(songToUpdate.getAuthor()));

            prefYear.setTitle(getString(R.string.song_year_title_activity_single_song_settings) +
                    ": " + checkUnknownValue(songToUpdate.getYear()));

            prefEqualization.setTitle(getString(R.string.title_equalizzazione_importazione) + ": " + checkUnknownValue(songToUpdate.getEqualization()));
            prefSpeed.setTitle(getString(R.string.title_velocita_importazione) + ": " + checkUnknownValue(Float.toString(songToUpdate.getSpeed()) + " cm/s"));

            prefTapeWidth.setTitle(getString(R.string.tape_width_title) +
                    ": " + checkUnknownValue(songToUpdate.getTapeWidth()));


            //Metto i valori di default alle liste
            prefEqualization.setValue(songToUpdate.getEqualization());
            prefSpeed.setValue(Float.toString(songToUpdate.getSpeed()));
            prefTapeWidth.setValue(songToUpdate.getTapeWidth());

            //setto i contenuti predefiniti dei testi (quelli che si aprono in popup)
            if (songToUpdate!=null && !songToUpdate.getYear().equals(getString(R.string.sconosciuto)))
                prefYear.setText(songToUpdate.getYear());
            else
                prefYear.setText("");

            if (!songToUpdate.getAuthor().equals(getString(R.string.sconosciuto)))
                prefAuthorName.setText(songToUpdate.getAuthor());
            else
                prefAuthorName.setText("");

            if (!songToUpdate.getTitle().equals(getString(R.string.sconosciuto)))
                prefSongName.setText(songToUpdate.getTitle());
            else
                prefSongName.setText("");

            if (songToUpdate.getDescription() == null)
                prefDescription.setText("");
            else
                prefDescription.setText(songToUpdate.getDescription());

            //imposto i vincoli sull'input del testo o dei numeri
            prefYear.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);    //Input di soli numeri
            //creo un array di filtri, in realtà fatto da un solo elemento perché mi basta 1 filtro
            InputFilter[] filterArray = new InputFilter[1];
            filterArray[0] = new InputFilter.LengthFilter(MAX_CHARACTER);        //Lunghezza massima
            //settiamo il filtro: nome del brano e dell'autore non potranno avere più di MAX_CHARACTER caratteri
            prefSongName.getEditText().setFilters(filterArray);
            prefAuthorName.getEditText().setFilters(filterArray);

            prefSongName.getEditText().setSingleLine();                            //Unica riga di testo
            prefAuthorName.getEditText().setSingleLine();


            //evento chiamato quando vengono cambiate le preference con l'input dell'utente
            OnPreferenceChangeListener eventChange = new OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {

                    //azione in base alla preference chiamante l'evento
                    switch (preference.getKey()) {
                        case "nomeAutoreImpostazioniBrano":
                            if (!(((String) newValue).replace(" ", "").equals(""))) {
                                save_changes = true; //ho effettuato una modifica quindi dopo chiamero' un popup all'uscita dell'activity
                                prefAuthorName.setTitle(getString(R.string.song_author_title_activity_single_song_settings) +
                                        ": " + (String) newValue);
                                songToUpdate.setAuthor((String) newValue);
                                prefAuthorName.setText(songToUpdate.getAuthor());
                            } else
                                Toast.makeText(getActivity(), getString(R.string.toast_activity_single_song_settings), Toast.LENGTH_SHORT).show();
                            break;
                        case "nomeCanzoneImpostazioniBrano":
                            if (!(((String) newValue).replace(" ", "").equals(""))) {
                                save_changes = true; //ho effettuato una modifica quindi dopo chiamero' un popup all'uscita dell'activity
                                prefSongName.setTitle(getString(R.string.song_name_title_activity_single_song_settings) + ":" + (String) newValue);
                                songToUpdate.setTitle((String) newValue);
                                prefSongName.setText(songToUpdate.getTitle());
                            } else
                                Toast.makeText(getActivity(), getString(R.string.toast_activity_single_song_settings), Toast.LENGTH_SHORT).show();
                            break;
                        case "listaVelocitaImpostazioniBrano":
                            prefSpeed.setTitle((String) newValue);
                            save_changes = true; //ho effettuato una modifica quindi dopo chiamero' un popup all'uscita dell'activity
                            songToUpdate.setSpeed(Float.parseFloat((String) newValue));
                            prefSpeed.setTitle(getString(R.string.title_velocita_importazione) + ": " + Float.toString(songToUpdate.getSpeed()) + " cm/s");
                            prefSpeed.setValue(Float.toString(songToUpdate.getSpeed()));
                            break;
                        case "listaEqualizzazioneImpostazioniBrano":
                            prefEqualization.setTitle(getString(R.string.title_equalizzazione_importazione) + ": " + (String) newValue);
                            save_changes = true; //ho effettuato una modifica quindi dopo chiamero' un popup all'uscita dell'activity
                            songToUpdate.setEqualization((String) newValue);
                            prefEqualization.setValue(songToUpdate.getEqualization());
                            break;
                        case "annoImpostazioniBrano": //controllo se viene inserito un anno effettivo, caso patologico gestito a parte

                            if (checkYear((String) newValue)) //solo se l'anno e' valido
                            {
                                songToUpdate.setYear((String) newValue);
                                preference.setTitle(getString(R.string.song_year_title_activity_single_song_settings) +
                                        ": " + songToUpdate.getYear());
                                prefYear.setText(songToUpdate.getYear());
                                save_changes = true;
                            }

                            break;
                        case "SettingsTapeWidth":
                            songToUpdate.setTapeWidth((String) newValue);
                            prefTapeWidth.setValue(songToUpdate.getTapeWidth());
                            prefTapeWidth.setTitle(getString(R.string.tape_width_title) +
                                    ": " + prefTapeWidth.getEntry());
                            save_changes = true;
                            break;
                        case "SettingsDescription":
                            prefDescription.setText((String) newValue);
                            songToUpdate.setDescription((String) newValue);
                            save_changes = true;
                            break;
                    } //fine switch
                    return false;
                }
            };

            //setto l'evento per tutte le preference a cui serve (quelle dove l'utente puo' modificare in sostanza)
            prefAuthorName.setOnPreferenceChangeListener(eventChange);
            prefEqualization.setOnPreferenceChangeListener(eventChange);
            prefSpeed.setOnPreferenceChangeListener(eventChange);
            prefSongName.setOnPreferenceChangeListener(eventChange);
            prefYear.setOnPreferenceChangeListener(eventChange);
            prefTapeWidth.setOnPreferenceChangeListener(eventChange);
            prefDescription.setOnPreferenceChangeListener(eventChange);
        }


//		/**
//		 * true se la canzone e' stereo, false se e' mono
//		 * @return boolean
//		 */
//		private boolean isStereo()
//		{
//			return songToUpdate.isStereo();
//		}

        /**
         * Controlla se una stringa e' effettivamente un anno: controlla se lancia un eccezione se parsato a intero e se e' un numero da 0 a 9999
         *
         * @param year stringa rappresentante l'anno
         * @return booleano che dice se e' un anno valido o no
         */
        private boolean checkYear(String year) {
            int numericYear;
            Toast toast;
            int duration = Toast.LENGTH_SHORT;
            toast = Toast.makeText(getActivity(), getString(R.string.toast_activity_single_song_settings), duration);

            try {
                numericYear = Integer.parseInt(year);
            } catch (NumberFormatException nfe) {
                toast.show(); //se ho l'eccezione mostro il toast di errore
                return false;
            }

            //faccio un piccolo controllo di bound dei numeri
            if (numericYear < 9999 && numericYear > 0)
                return true;

            toast.show(); //se ho anno fuori dai numeri mostro toast di errore
            return false;
        }

        /**
         * Verifica se e' stata effettuata una modifica alle preference
         *
         * @return ritorna un booleano true se ci sono state modifiche, false altrimenti
         */
        public boolean isChanged() {
            return save_changes;
        }

//		/**
//		 * Metodo per dire al fragment che si e' modificato qualcosa (uso per l'activity principale)
//		 * @param b booleano
//		 */
//		private void setModified()
//		{
//			save_changes = true;
//		}

        /**
         * Metodo che controlla se i parametri passati sono null o meno, se null torna la stringa [UNKNOWN]
         *
         * @param input stringa da controllare
         * @return ritorna la stringa stessa se non e' null altrimenti torna [UNKNOWN]
         */
        private String checkUnknownValue(String input) {
            return (input == null ? getString(R.string.sconosciuto) : input);
        }
    }
}
	