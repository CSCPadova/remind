package unipd.dei.magnetophone;

//import it.unipd.dei.esp1314.magnetophone.R;

import java.io.File;
import java.security.InvalidParameterException;
import java.util.LinkedList;

import unipd.dei.magnetophone.Song.Track;
//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.app.DialogFragment;
//import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
//import android.preference.EditTextPreference;
//import android.preference.ListPreference;
//import android.preference.Preference;
//import android.preference.Preference.OnPreferenceChangeListener;
//import android.preference.Preference.OnPreferenceClickListener;
//import android.preference.PreferenceCategory;

import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

/**
 * Activity che si occupa di gestire l'importazione di brani.
 *
 * Una volta confermate le decisioni, si salveranno i dati sul database in maniera permanente.
 *
 */

public class ImportSongActivity extends FragmentActivity {

	private static int RESULT_LOAD_AUDIO_FILE = 2;    //Codice per l'intent di scelta brano
	private static int INDEX_OF_TRACK;                //indice della track che l'utente sta attulmente scegliendo
	private Fragment obligatoryFrag;                //Reference al primo fragment: campi obbligatori
	private Song songToAdd = new Song();            //Canzone da aggiungere al database

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_import_song);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		//Cambio titolo all'action bar
		getActionBar().setTitle(getString(R.string.action_bar_campi_obbligatori_importazione));

		if (savedInstanceState == null) {
			obligatoryFrag = new ObligatoryFragment();
			//Aggiungo i due fragment, mostro solo il primo
			getSupportFragmentManager().beginTransaction()
					.disallowAddToBackStack()
					.add(R.id.container, obligatoryFrag)
					.commit();
		}
	}

	/**
	 * Tasto back adattato ai fragment (permette lo scorrimento tra fragment)
	 */
	@Override
	public void onBackPressed() {
		goToSongsList();
	}

	/**
	 * ActionBar per tornare indietro
	 * HOME: back normale di android
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
			case android.R.id.home:
				goToSongsList();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}


	/**
	 * Gestisco lo scorrimento dei fragment con il metodo onClick chiamato dai bottoni indietro e prosegui, gestisco anche il click sull'immagine dei campi opzionali
	 *
	 * @param v
	 */
	public void onClick(View v) {
		//controllo che tutto vada bene per importare
		if (((ObligatoryFragment) obligatoryFrag).allRightForNextStep()) {
			endImportationProcess();
		} else {
			//Notifica di errore se non ho inserito i dati
			String warning;

			//Controllo i casi:
			//	1:se non ho inserito ne i files ne il nome della canzone
			//	2:se non ho inserito i file/files
			//	3:se non ho inserito il nome della canzone
			if (!((ObligatoryFragment) obligatoryFrag).filesAudioSelected() && (!((ObligatoryFragment) obligatoryFrag).songNameSelected() || !((ObligatoryFragment) obligatoryFrag).songProvenanceSelected()))
				warning = getString(R.string.toast1_activity_first_page_import);
			else if (!((ObligatoryFragment) obligatoryFrag).filesAudioSelected())
				warning = getString(R.string.toast3_activity_first_page_import);
			else if (!((ObligatoryFragment) obligatoryFrag).songNameSelected() || !((ObligatoryFragment) obligatoryFrag).songProvenanceSelected())
				warning = getString(R.string.toast2_activity_first_page_import);
			else if (!((ObligatoryFragment) obligatoryFrag).checkTheTracksCoerence())
				warning = getString(R.string.coerence_problem);
			else
				warning = getString(R.string.unknown_error);

			Toast.makeText(this, warning, Toast.LENGTH_LONG).show();

		}

	}


	/**
	 * Metodo che termina il processo di importazione, mostrando un popup di ultima conferma
	 */
	private void endImportationProcess() {
		//Creo un popup di conferma dati e lo mostro
		SaveAlertFragment save = new SaveAlertFragment();
		save.show(getSupportFragmentManager(), null);
	}

	/**
	 * Metodo che si occupa di uscire dall'activity e richiamare la lista dei brani
	 */
	private void goToSongsList() {
		//informo la lista che dovrà far vedere la nuova canzone importata
		SharedPreferences shared = this.getSharedPreferences("selected", Context.MODE_PRIVATE);
		Editor editor = shared.edit();
		editor.putInt("song_id", songToAdd.getId());
		editor.commit();

		Intent intent = new Intent(this, SongListActivity.class);    //Passo alla lista dei brani
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    //evito che premendo BACK dalla lista dei brani si ritorni all'importazione
		intent.putExtra("modify", 1);
		startActivity(intent);
	}

	// ###########################################   FRAGMENT  ############################################

	/**
	 * Questo fragment mostra le opzioni definite nel file xml ImportSongDatiObbligatori
	 */
	public static class ObligatoryFragment extends PreferenceFragmentCompat {
		private EditTextPreference prefSignature;
		private EditTextPreference prefProvenance;
		private ListPreference prefEqualization;
		private ListPreference prefSpeed;
		private ListPreference prefNumberOfTracks;
		private Preference prefTrack1;        //Rappresentano le varie preference del file xml
		private Preference prefTrack2;        //
		private Preference prefTrack3;
		private Preference prefTrack4;
		private ListPreference prefTapeWidth;
		//		private ListPreference prefSampleRate;
//		private ListPreference prefExtension;
//		private ListPreference prefBitDepth;
		private EditTextPreference prefAuthorName;
		private EditTextPreference prefDescription;
		private static Preference prefPhotos;
		private static Preference prefVideo;


		private EditTextPreference prefSongName;    //
		//		private boolean resultFromLeft = true;		//Per capire se devo impostare file audio per canale dx o sx quando si seleziona un brano
		private boolean firstSelected = false;        //File sx selezionato?
		private boolean secondSelected = false;        //File dx selezionato?
		private boolean thirdSelected = false;
		private boolean fourthSelected = false;
		private static XmlImport imp;


		@Override
		public void onCreatePreferences(Bundle bundle, String s) {
			//super.onCreate(savedInstanceState);
			//Creo una lista di preference dall'xml
			addPreferencesFromResource(R.xml.import_song_dati);

			//Creo dei collegamenti agli elementi prference dell'xml import_song_dati

			imp = new XmlImport(getActivity());

			// ------------ campi obbligatori ----------------------
			//Signature (no default)
			prefSignature = (EditTextPreference) findPreference("Signature");
			//Provenance (no default)
			prefProvenance = (EditTextPreference) findPreference("Provenance");

			//--------- impostazioni di riproduzione ------------------
			//Equalizzazione (default FLAT)
			prefEqualization = (ListPreference) findPreference("listaEqualizzazioneImportazioneBrano");
			//velocità (default 3.5)
			prefSpeed = (ListPreference) findPreference("listaVelocitaImportazioneBrano");


			//------------ scelta delle tracce -----------------------
			//default 1 sola traccia mono visibile
			prefNumberOfTracks = (ListPreference) findPreference("numberOfTracks");
			prefTrack1 = findPreference("Track1");
			prefTrack2 = findPreference("Track2");
			prefTrack3 = findPreference("Track3");
			prefTrack4 = findPreference("Track4");

			//---------------- Informazioni Generali -------------------
			//Author, default: unknown
			prefAuthorName = (EditTextPreference) findPreference("nomeAutoreImportazioneBrano");
			//title, default: unknown
			prefSongName = (EditTextPreference) findPreference("nomeCanzoneImportazioneBrano");
			//year, default: unknown
			final EditTextPreference prefYear = (EditTextPreference) findPreference("annoImportazioneBrano");
			//tape width, default: unknown
			prefTapeWidth = (ListPreference) findPreference("TapeWidth");

			//---------- prefereceCategory temporanea ------------------
//			//sample rate, default: 41000
//			prefSampleRate = (ListPreference)findPreference("listaSampleRate");
//			//Extension, default: .wav
//			prefExtension = (ListPreference)findPreference("Extension");
//			//BitDepth, degault: 16
//			prefBitDepth = (ListPreference)findPreference("BitDepth");

			//------------ descrizione ---------------
			prefDescription = (EditTextPreference) findPreference("Description");

			//--------- photos e video -----------------
			prefPhotos = findPreference("Photos");
			prefVideo = findPreference("Video");


			//################# la song da importare dovrà in ogni caso avere dei valori di default già pronti a parte per quelli obbligatori########
			// ad esclusione certo delle track, di signature e di provenance
			ImportSongActivity isa = (ImportSongActivity) getActivity();

			isa.songToAdd.setEqualization((String) prefEqualization.getEntryValues()[2]);
			isa.songToAdd.setSpeed(Float.parseFloat((String) prefSpeed.getEntryValues()[0]));

//			isa.songToAdd.setNumberOfTracks(Integer.parseInt((String)prefNumberOfTracks.getEntryValues()[0]));

			isa.songToAdd.setAuthor(getString(R.string.sconosciuto));
			isa.songToAdd.setTitle(getString(R.string.sconosciuto));
			isa.songToAdd.setYear(getString(R.string.sconosciuto));
			isa.songToAdd.setTapeWidth((String) prefTapeWidth.getEntryValues()[0]);

//			isa.songToAdd.setSampleRate(Integer.parseInt((String)prefSampleRate.getEntryValues()[2]));
//			isa.songToAdd.setExtension((String)prefExtension.getEntries()[0]);
//			isa.songToAdd.setBitDepth(Integer.parseInt((String)prefBitDepth.getEntryValues()[1]));

			// ######### Setto i valori di default per le preference #################
			prefSignature.setText("");
			prefProvenance.setText("");

			//setto le preference
			prefEqualization.setValue((String) prefEqualization.getEntryValues()[2]);
			prefSpeed.setValue((String) prefSpeed.getEntryValues()[0]);


			prefNumberOfTracks.setValue((String) prefNumberOfTracks.getEntryValues()[0]);

			//faccio scomparire le ultime 3 preference per l'importazione dei brani
			final PreferenceCategory pc = (PreferenceCategory) getPreferenceScreen().
					findPreference("howManyTracks");
			switch (Integer.parseInt((String) prefNumberOfTracks.getValue())) {
				case -1:
				case 1:
					pc.removePreference(prefTrack2);
				case 3:
					pc.removePreference(prefTrack3);
				case 4:
					pc.removePreference(prefTrack4);
					break;
				default:
					break;
			}

			prefAuthorName.setText("");
			prefSongName.setText("");
			prefYear.setText("");
			prefTapeWidth.setValue((String) prefTapeWidth.getEntryValues()[0]);

//			prefSampleRate.setValue((String)prefSampleRate.getEntryValues()[2]);
//			//Extension, default: .wav
//			prefExtension.setValue((String)prefExtension.getEntryValues()[0]);;
//			//BitDepth, degault: 16
//			prefBitDepth.setValue((String)prefBitDepth.getEntryValues()[1]);;

			prefDescription.setText("");


			//Setto la possibilità di inserire solo numeri in questa preference
			//TODO risolvere questa mancanza
			//prefYear.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);


			//########### setto i valori di default che appariranno come scritte nelle preference
			prefSignature.setTitle(getString(R.string.signature_title) + ": "
					+ getString(R.string.signature_canzone_default_importazione));
			prefProvenance.setTitle(getString(R.string.provenance_title) +
					": " + getString(R.string.provenance_canzone_default_importazione));

			prefEqualization.setTitle(getString(R.string.title_equalizzazione_importazione) + ": " + prefEqualization.getValue());
			prefSpeed.setTitle(getString(R.string.title_velocita_importazione) + ": " + prefSpeed.getEntry());


			prefAuthorName.setTitle(getString(R.string.song_author_title_activity_single_song_settings) +
					": " + getString(R.string.nome_canzone_default_importazione));

			prefSongName.setTitle(getString(R.string.song_name_title_activity_single_song_settings) +
					": " + getString(R.string.titolo_canzone_default_importazione));

			prefYear.setTitle(getString(R.string.song_year_title_activity_single_song_settings) +
					": " + getString(R.string.anno_canzone_default_importazione));

			prefTapeWidth.setTitle(getString(R.string.tape_width_title) +
					": " + prefTapeWidth.getEntry());

			prefNumberOfTracks.setTitle(getString(R.string.number_of_tracks_title) + ":" + prefNumberOfTracks.getEntry());

			prefTrack1.setTitle(getString(R.string.title_canale1_importazione) + ": ");
			prefTrack2.setTitle(getString(R.string.title_canale1_importazione) + ": ");
			prefTrack3.setTitle(getString(R.string.title_canale1_importazione) + ": ");
			prefTrack4.setTitle(getString(R.string.title_canale1_importazione) + ": ");

//			prefSampleRate.setTitle(getString(R.string.sample_rate_title) + ": " + prefSampleRate.getEntry());
//			prefExtension.setTitle(getString(R.string.extension_title) + ": " + prefExtension.getEntry());
//			prefBitDepth.setTitle(getString(R.string.bitdepth_title) + ": " + prefBitDepth.getEntry());


			//Creo il preferenceChangeListener per aggiornare i testi delle preference nella lista quando cambiano i contenuti
			Preference.OnPreferenceChangeListener eventChange = new Preference.OnPreferenceChangeListener() {
				@Override
				public boolean onPreferenceChange(Preference preference, Object newValue) {
					//azione in base alla preference che chiama l'evento, newValue è l'input dell'utente
					switch (preference.getKey()) {
						case "Signature":
							prefSignature.setText((String) newValue);
							preference.setTitle(getString(R.string.signature_title) +
									": " + (String) newValue);

							//Se non ho inserito niente metto unknown come valore nella songToAdd
							if (((String) newValue).replace(" ", "").equals(""))
								((ImportSongActivity) getActivity()).songToAdd.setSignature(getString(R.string.sconosciuto));
							else
								((ImportSongActivity) getActivity()).songToAdd.setSignature((String) newValue);
							break;
						case "Provenance":
							prefProvenance.setText((String) newValue);
							preference.setTitle(getString(R.string.provenance_title) +
									": " + (String) newValue);

							//Se non ho inserito niente metto unknown come valore nella songToAdd
							if (((String) newValue).replace(" ", "").equals(""))
								((ImportSongActivity) getActivity()).songToAdd.setProvenance(getString(R.string.sconosciuto));
							else
								((ImportSongActivity) getActivity()).songToAdd.setProvenance((String) newValue);
							break;


						case "listaEqualizzazioneImportazioneBrano":
							//aggiorno la preference
							prefEqualization.setValue((String) newValue);
							//aggiorno il titolo così l'utente viene a conoscenza dell'avvenuto mutamento
							preference.setTitle(getString(R.string.title_equalizzazione_importazione) + ": " + prefEqualization.getEntry());
							//aggiorno la canzone che alla fine si importerà
							((ImportSongActivity) getActivity()).songToAdd.setEqualization((String) newValue);
							break;
						case "listaVelocitaImportazioneBrano":
							prefSpeed.setValue((String) newValue);
							preference.setTitle(getString(R.string.title_velocita_importazione) + ": " + prefSpeed.getEntry());
							((ImportSongActivity) getActivity()).songToAdd.setSpeed(Float.parseFloat((String) prefSpeed.getValue()));
							break;

						case "numberOfTracks":
							prefNumberOfTracks.setValue((String) newValue);
							preference.setTitle(getString(R.string.number_of_tracks_title) + ": " + prefNumberOfTracks.getEntry());
							//							((ImportSongActivity)getActivity()).songToAdd.setNumberOfTracks(Integer.parseInt((String)newValue));
							int number = Integer.parseInt((String) newValue);
							//a seconda del numero del track scelte, mostro/nascondo le preference
							//per andare a segliere i file audio e cancello eventuali file già selezionati
							//se l'utente ha cambiato idea ed ha scelto di ridurre il numero di tracce che desidera
							//prendo le reference alle preference


							switch (number) {
								case -1: //1 traccia mono
									removeExtraTracks(1);

									if (prefTrack2 != null)
										pc.removePreference(prefTrack2);
									if (prefTrack3 != null)
										pc.removePreference(prefTrack3);
									if (prefTrack4 != null)
										pc.removePreference(prefTrack4);

									break;
								case 1: //1 traccia stereo
									removeExtraTracks(1);

									pc.removePreference(prefTrack2);
									pc.removePreference(prefTrack3);
									pc.removePreference(prefTrack4);

									break;
								case 2:
									removeExtraTracks(2);

									pc.addPreference(prefTrack2);
									pc.removePreference(prefTrack3);
									pc.removePreference(prefTrack4);

									break;
								case 4:

									pc.addPreference(prefTrack1);
									pc.addPreference(prefTrack2);
									pc.addPreference(prefTrack3);
									pc.addPreference(prefTrack4);
							}
							break;

						case "nomeCanzoneImportazioneBrano":
							preference.setTitle(getString(R.string.song_name_title_activity_single_song_settings) +
									": " + (String) newValue);
							prefSongName.setText((String) newValue);
							((ImportSongActivity) getActivity()).songToAdd.setTitle((String) newValue);
							break;
						case "nomeAutoreImportazioneBrano":
							preference.setTitle(getString(R.string.song_author_title_activity_single_song_settings) +
									": " + (String) newValue);
							prefAuthorName.setText((String) newValue);

							//Se non ho inserito niente metto unknown come valore nella songToAdd
							if (((String) newValue).replace(" ", "").equals(""))
								((ImportSongActivity) getActivity()).songToAdd.setAuthor(getString(R.string.sconosciuto));
							else
								((ImportSongActivity) getActivity()).songToAdd.setAuthor((String) newValue);
							break;
						case "annoImportazioneBrano":
							if (checkYear((String) newValue)) {
								preference.setTitle(getString(R.string.song_year_title_activity_single_song_settings) +
										": " + (String) newValue);
								prefYear.setText((String) newValue);
								if (((String) newValue).equals(""))
									((ImportSongActivity) getActivity()).songToAdd.setYear(getString(R.string.sconosciuto));
								else
									((ImportSongActivity) getActivity()).songToAdd.setYear((String) newValue);
							}
							break;
						case "TapeWidth":
							//aggiorno la preference
							prefTapeWidth.setValue((String) newValue);
							//aggiorno il titolo così l'utente viene a conoscenza dell'avvenuto mutamento
							preference.setTitle(getString(R.string.title_equalizzazione_importazione) + ": " + prefTapeWidth.getEntry());
							//aggiorno la canzone che alla fine si importerà
							((ImportSongActivity) getActivity()).songToAdd.setTapeWidth((String) newValue);
							break;

//						case "listaSampleRate":
//							prefSampleRate.setValue((String)newValue);
//							preference.setTitle(getString(R.string.sample_rate_title) + ": " + prefSampleRate.getEntry());
//							((ImportSongActivity)getActivity()).songToAdd.setSampleRate(Integer.parseInt((String)newValue));
//							break;
//						case "Extension":
//							prefExtension.setValue((String)newValue);
//							preference.setTitle(getString(R.string.extension_title) + ": " + prefExtension.getEntry());
//							((ImportSongActivity)getActivity()).songToAdd.setExtension((String)newValue);
//							break;
//						case "BitDepth":
//							prefBitDepth.setValue((String)newValue);
//							preference.setTitle(getString(R.string.bitdepth_title) + ": " + prefBitDepth.getEntry());
//							((ImportSongActivity)getActivity()).songToAdd.setBitDepth(Integer.parseInt((String)newValue));
//							break;

						case "Description":
							preference.setTitle(getString(R.string.description_title) +
									": " + (String) newValue);
							prefDescription.setText((String) newValue);
							if (((String) newValue).equals(""))
								((ImportSongActivity) getActivity()).songToAdd.setDescription(getString(R.string.sconosciuto));
							else
								((ImportSongActivity) getActivity()).songToAdd.setDescription((String) newValue);
							break;
					} //fine switch
					return false;
				}
			};

			//setto il listener sulla preference per la scelta del primo file audio. Lo stesso negli altri
			prefTrack1.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					getAudioFile(1);
					return false;
				}
			});

			prefTrack2.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					//prendo file audio per canale RIGHT
					getAudioFile(2);
					return false;
				}
			});

			prefTrack3.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					//prendo file audio per canale LEFT
					getAudioFile(3);
					return false;
				}
			});

			prefTrack4.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					//prendo file audio per canale LEFT
					getAudioFile(4);
					return false;
				}
			});

			prefPhotos.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {
					PhotosFragment prefPath = new PhotosFragment();//crea il nuovo fragment, definito a fine file
					prefPath.show(getFragmentManager(), null);
					return false;
				}
			});

			prefVideo.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
				@Override
				public boolean onPreferenceClick(Preference preference) {

					VideoFragment prefPath = new VideoFragment();//crea il nuovo fragment, definito a fine file
					prefPath.show(getFragmentManager(), null);
					return false;
				}
			});


			//Setto l'evento alle preference a cui è collegato
			prefSignature.setOnPreferenceChangeListener(eventChange);
			prefProvenance.setOnPreferenceChangeListener(eventChange);

			prefEqualization.setOnPreferenceChangeListener(eventChange);
			prefSpeed.setOnPreferenceChangeListener(eventChange);

			prefNumberOfTracks.setOnPreferenceChangeListener(eventChange);

			prefSongName.setOnPreferenceChangeListener(eventChange);
			prefAuthorName.setOnPreferenceChangeListener(eventChange);
			prefYear.setOnPreferenceChangeListener(eventChange);
			prefTapeWidth.setOnPreferenceChangeListener(eventChange);

//			prefSampleRate.setOnPreferenceChangeListener(eventChange);
//			prefExtension.setOnPreferenceChangeListener(eventChange);
//			prefBitDepth.setOnPreferenceChangeListener(eventChange);

			prefDescription.setOnPreferenceChangeListener(eventChange);


		} //fine oncreate


		/**
		 * metodo che si preoccupa di rimuovere tracce già scelte tr quelle per formare la song
		 * nel caso che l'utente cambi idea
		 *
		 * @param n
		 */
		private void removeExtraTracks(int n) {
			ImportSongActivity isa = (ImportSongActivity) getActivity();
			switch (n) {
				case 1://l'utente ha scelto di tenere 1 sola traccia
					//se prima ne aveva scelte più di una
					if (isa.songToAdd.getNumberOfTracks() > 1) {
						//azzero i titoli
						prefTrack2.setTitle(getString(R.string.title_canale1_importazione) + ": ");
						prefTrack3.setTitle(getString(R.string.title_canale1_importazione) + ": ");
						prefTrack4.setTitle(getString(R.string.title_canale1_importazione) + ": ");

						LinkedList<Track> list = isa.songToAdd.getTrackList();
						switch (list.size())//a seconda di quante tracce ci sono elimino quelle in eccesso
						{
							case 4:
								list.pollLast();
							case 3:
								list.pollLast();
							case 2:
								list.pollLast();
								break;
						}

					}
					break;
				case 2://l'utente ha scelto di tenere 2 sole tracce
					//se prima ne avevo più di due
					//se prima ne aveva scelte più di una
					if (isa.songToAdd.getNumberOfTracks() > 2) {
						//azzero i titoli
						prefTrack3.setTitle(getString(R.string.title_canale1_importazione) + ": ");
						prefTrack4.setTitle(getString(R.string.title_canale1_importazione) + ": ");

						LinkedList<Track> list = isa.songToAdd.getTrackList();

						switch (list.size())//a seconda di quante tracce ci sono elimino quelle in eccesso
						{
							case 4:
								list.pollLast();
							case 3:
								list.pollLast();
								break;
						}
					}
					break;
			}
		}


		/**
		 * Metodo che chiama l'activity AudioFilePickerActivity per scegliere un file audio,
		 *
		 * @param index del track
		 */
		private void getAudioFile(int index)
				throws InvalidParameterException {

			if (index == 1 || index == 2 || index == 3 || index == 4)
				INDEX_OF_TRACK = index;
			else
				throw new InvalidParameterException("Il parametro del metodo è errato, inserire un indice valido");

			Intent intent = new Intent(getActivity(), AudioFilePickerActivity.class);
			startActivityForResult(intent, RESULT_LOAD_AUDIO_FILE);
		}

		/**
		 * metodo chiamato al ritorno dalla scelta del file audio. Setta i valori del file audio leggendone i dati.
		 */
		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
			{
				if (requestCode == RESULT_LOAD_AUDIO_FILE && resultCode == RESULT_OK && data != null) {

					String filePath = data.getStringExtra("fileAbsPath");    //prendo la path assoluta
					File temp = new File(filePath);
					String fileName = temp.getName();        //prendo il nome del file

					//setto l'estensione del file
					String ext = "";
					int j = filePath.lastIndexOf('.');
					if (j > 0) {
						ext = filePath.substring(j);
					}

					//setto il valore dell'estensione
//					prefExtension.setValue(ext);
//					prefExtension.setTitle(getString(R.string.extension_title) + ": " + prefExtension.getEntry());
					((ImportSongActivity) getActivity()).songToAdd.setExtension(ext);

					//eseguo il parsing dell'header del file e ne ottengo le informazioni che vengono inserite nel wavHeader
					WaveHeader wavHeader = new WaveHeader(temp);
					setTheWaveHeaderValues(wavHeader);

					//alcuni log di debug
					Log.d("ImportSongActivity", "    chunkId: " + wavHeader.getChunkId());
					Log.d("ImportSongActivity", "  chunkSize: " + wavHeader.getChunkSize());
					Log.d("ImportSongActivity", "     format: " + wavHeader.getFormat());
					Log.d("ImportSongActivity", "subChunk1Id: " + wavHeader.getSubChunk1Id());
					Log.d("ImportSongActivity", "   channels: " + wavHeader.getChannels());
					Log.d("ImportSongActivity", "sample rate: " + wavHeader.getSampleRate());
					Log.d("ImportSongActivity", "   bitDepth: " + wavHeader.getBitsPerSample());
					Log.d("ImportSongActivity", "   duration: " + wavHeader.getDuration());


					//si inserisce ora la track nella song
					//nel caso l'utente avesse già selezionato una track e poi avesse cambiato idea, mi premuro di eliminarla
					//dalla canzone
					((ImportSongActivity) getActivity()).songToAdd.deleteTrackOfIndex(INDEX_OF_TRACK);
					//inserisco il track nella song e tengo traccia del fatto che è stato selezionato il file corrispondente
					//a quell'indice di track
					switch (INDEX_OF_TRACK) {
						case 1:
							prefTrack1.setTitle(getString(R.string.title_canale1_importazione) + ": " + fileName);
							((ImportSongActivity) getActivity()).songToAdd.setTrack(filePath, 1); //salvo nome del file della track
							firstSelected = true;
							break;
						case 2:
							prefTrack2.setTitle(getString(R.string.title_canale1_importazione) + ": " + fileName);
							((ImportSongActivity) getActivity()).songToAdd.setTrack(filePath, 2); //salvo nome del file
							secondSelected = true;
							break;
						case 3:
							prefTrack3.setTitle(getString(R.string.title_canale1_importazione) + ": " + fileName);
							((ImportSongActivity) getActivity()).songToAdd.setTrack(filePath, 3); //salvo nome del file
							thirdSelected = true;
							break;
						case 4:
							prefTrack4.setTitle(getString(R.string.title_canale1_importazione) + ": " + fileName);
							((ImportSongActivity) getActivity()).songToAdd.setTrack(filePath, 4); //salvo nome del file
							fourthSelected = true;
							break;
					}

				}
			}
		}

		/**
		 * Metodo per controllare se è possibile passare alla seconda pagina di importazione (ovvero dati obbligatori inseriti)
		 *
		 * @return un booleano
		 */
		private boolean allRightForNextStep() {
			return (filesAudioSelected() && songNameSelected() && songProvenanceSelected() && this.checkTheTracksCoerence());
		}


		/**
		 * ritorna true se tutti i file audio sono stati selezionati
		 *
		 * @return
		 */
		private boolean filesAudioSelected() {

			int number = Integer.parseInt(prefNumberOfTracks.getValue());
			switch (number) {
				case -1:
					return firstSelected;
				case 1:
					return firstSelected;
				case 2:
					return firstSelected && secondSelected;
				case 4:
					return firstSelected && secondSelected && thirdSelected && fourthSelected;
				default:
					return false;
			}

		}

		/**
		 * Ritorna true se ho scritto un nome della canzone valido
		 *
		 * @return
		 */
		private boolean songNameSelected() {
			return !prefSignature.getText().replace(" ", "").equals("");    //Evito il caso in cui ho solo spazi!
		}

		/**
		 * Ritorna true se ho scritto una provenienza per la canzone valida
		 *
		 * @return
		 */
		private boolean songProvenanceSelected() {
			return !prefProvenance.getText().replace(" ", "").equals("");    //Evito il caso in cui ho solo spazi!
		}

		/**
		 * Controlla se una stringa è effettivamente un anno: controlla se lancia un eccezione se parsato a intero e se è un numero da 0 a 9999
		 *
		 * @param year stringa rappresentante l'anno
		 * @return booleano che dice se è un anno valido o no
		 */
		private boolean checkYear(String year) {
			int numericYear;
			Toast toast;
			int duration = Toast.LENGTH_SHORT;
			toast = Toast.makeText(getActivity(), getString(R.string.toast_activity_single_song_settings), duration);
			//Controllo che l'anno sia effettivamente solo numerico
			try {
				numericYear = Integer.parseInt(year);
			} catch (NumberFormatException nfe) {
				toast.show();    //se ho l'eccezione mostro il toast di errore
				return false;
			}

			//se l'anno è da 0 a 9999 e non inizia per 0, faccio un piccolo controllo di bound dei numeri
			if (numericYear < 9999 && numericYear > 0 && !year.startsWith("0"))
				return true;

			toast.show();        //se ho un anno fuori dai numeri massimi mostro toast di errore
			return false;
		}

		/**
		 * metodo che si preoccupa di controllare se le tracce hanno 4 header con gli stessi dati
		 *
		 * @return
		 */
		public boolean checkTheTracksCoerence() {
			LinkedList<Track> trackList = ((ImportSongActivity) getActivity()).songToAdd.getTrackList();
			int trackLength = trackList.size();
			Track track1, track2, track3, track4;
			WaveHeader wave1, wave2, wave3, wave4;
			switch (trackLength) {
				case 1:
					return true;//un solo brano è coerente con se stesso
				case 2:
					//controllo che i due brani abbiano stesso bit depth, sample rate, duration e channels
					track1 = trackList.get(0);
					track2 = trackList.get(1);
					wave1 = new WaveHeader(new File(track1.getPath()));
					wave2 = new WaveHeader(new File(track2.getPath()));
					return wave1.equals(wave2);
				case 4:
					track1 = trackList.get(0);
					track2 = trackList.get(1);
					wave1 = new WaveHeader(new File(track1.getPath()));
					wave2 = new WaveHeader(new File(track2.getPath()));
					track3 = trackList.get(2);
					track4 = trackList.get(3);
					wave3 = new WaveHeader(new File(track3.getPath()));
					wave4 = new WaveHeader(new File(track4.getPath()));
					return wave1.equals(wave2) && wave1.equals(wave3) && wave1.equals(wave4);//non faccio altri controlli
				//per proprietà transitiva
			}
			return false;//c'è qualcosa che non va nel numero di tracce
		}


		//dialog che si preoccupa di farci vedere le cartelle foto e di salvare la nostra scelta
		public static class PhotosFragment extends DialogFragment {
			@Override
			public AppCompatDialog onCreateDialog(Bundle SavedInstanceState) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.photos_choice);

				//TODO
//				imp = new XmlImport(getActivity());
				//mi prendo i path e i nomi delle cartelle
				LinkedList<String> directoriesName = new LinkedList<String>();//lista che conterrà solo i nomi delle cartelle
				final LinkedList<String> pathList = getTheFilesList(directoriesName);//lista che conterrà i path

				final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.select_dialog_singlechoice, directoriesName);

				builder.setAdapter(adapter,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int which) {
								// The 'which' argument contains the index position
								// of the selected item
								String choice = pathList.get(which);
								prefPhotos.setSummary(getString(R.string.photos_path_summary) + choice);
								((ImportSongActivity) getActivity()).songToAdd.setPhotos(choice); //salvo nella song il path della cartella delle foto

							}
						});
				return builder.create();
			}
		}

		/**
		 * dialog che visualizza le cartelle video e salva la nostra scelta
		 */
		public static class VideoFragment extends DialogFragment {
			@Override
			public AppCompatDialog onCreateDialog(Bundle SavedInstanceState) {
				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle(R.string.video_choice);

				final LinkedList<String> directoriesName = new LinkedList<String>();
				final LinkedList<String> pathList = getTheFilesList(directoriesName);

				final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.select_dialog_singlechoice, directoriesName);

				builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						//prendo il path scelto dall'utente
						String choice = pathList.get(which);

						//prendo il file nel path
						File fileChoice = new File(choice);
						File[] videos = fileChoice.listFiles();//in teoria dovrebbe esserci solo 1 video

						prefVideo.setSummary(getString(R.string.video_path_summary) + " : " + choice);
						((ImportSongActivity) getActivity()).songToAdd.setVideo(videos[0].getAbsolutePath()); //salvo nella song il path della cartella delle foto

					}
				});
				return builder.create();
			}
		}

		/**
		 * metodo che si preoccupa di settare i valori di sample rate, bitdept e duration
		 * in base al file selezionato e passato come parametro
		 *
		 * @param header: oggetto WaveHeader con le informazioni dell'header del file(s) audio selezionato(i)
		 */
		public void setTheWaveHeaderValues(WaveHeader header) {
//			this.prefSampleRate.setValue(""+header.getSampleRate());
//			this.prefSampleRate.setTitle(getString(R.string.sample_rate_title) + ": " + prefSampleRate.getEntry());
			((ImportSongActivity) getActivity()).songToAdd.setSampleRate(header.getSampleRate());
//
//			prefBitDepth.setValue(""+header.getBitsPerSample());
//			prefBitDepth.setTitle(getString(R.string.bitdepth_title) + ": " + prefBitDepth.getEntry());
			((ImportSongActivity) getActivity()).songToAdd.setBitDepth(header.getBitsPerSample());

			((ImportSongActivity) getActivity()).songToAdd.setDuration(header.getDuration());

		}
	} //fine fragment


	// ###########################################   POPUP DI SALVATAGGIO   ############################################

	/**
	 * Classe che si occupa di richiedere un'ulteriore conferma dell'importazione tramite un popup.
	 * Mostra l'avvenuta importazione all'utente tramite un toast, e richiama la lista dei brani
	 */
	public static class SaveAlertFragment extends DialogFragment {
		@Override
		public AppCompatDialog onCreateDialog(Bundle savedInstanceState) {
			//Uso un builder per creare il popup
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(getString(R.string.title_alert_activity_single_song_settings));
			builder.setMessage(getString(R.string.message_alert_activity_importazione));

			//Setto il tasto SI e le sue azioni
			builder.setPositiveButton(getString(R.string.yes_alert_activity_single_song_settings), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					//Salvataggio dei dati su database
					DatabaseManager db = new DatabaseManager(getActivity());
					//Setto l'id creato dal database per la canzone
					((ImportSongActivity) getActivity()).songToAdd.setId(
							db.insertSingleSongInDatabase(((ImportSongActivity) getActivity()).songToAdd));

					//Notifica di avvenuta importazione
					Toast.makeText(getActivity(), getString(R.string.toast_activity_second_page_import), Toast.LENGTH_SHORT).show();

					//Passo alla lista dei brani (esco dall'importazione)
					((ImportSongActivity) getActivity()).goToSongsList();
				}
			});    //fine setPositiveButton

			//Setto il tasto ANNULLA, non faccio niente in sostanza, rimango nella schermata di importazione
			builder.setNeutralButton(getString(R.string.neutral_alert_activity_single_song_settings), null);

			builder.setNegativeButton(getString(R.string.no_alert_activity_single_song_settings), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					Toast.makeText(getActivity(), getString(R.string.toast_activity_cancel_import), Toast.LENGTH_SHORT).show();
					//Passo alla lista dei brani (esco dall'importazione)
					((ImportSongActivity) getActivity()).goToSongsList();
				}
			}); //fine setNegativeButton
			return builder.create();
		}
	}


	/**
	 * ritorna una lista con tutti i path delle sottocartelle presenti nella cartella magnetofono, dalle quali si
	 * possono scegliere quelle per foto e video
	 *
	 * @return
	 */
	public static LinkedList<String> getTheFilesList(LinkedList<String> nameList) {
		LinkedList<String> fileList = new LinkedList<String>();

		//prendo la cartella
		File magnetophone = new File(XmlImport.getCurrentDirectory());
		//dentro questa cartella, prendo le sottocartelle che rappresentano le song
		File[] songsDirectories = magnetophone.listFiles();
		//dentro queste cartelle, prendo le altre cartelle, che rappresentano i componenti delle song
		for (int i = 0; i < songsDirectories.length; i++) {

			File currentSongDirectory = songsDirectories[i];
			currentSongDirectory.mkdir();
			//se è effettivamente una directory e non un file svolazzante
			if (currentSongDirectory.isDirectory()) {
				//prendo i path delle directory
				File[] directoriesOfTheSong = currentSongDirectory.listFiles();
				//inserisco i path delle directory nella lista
				for (int g = 0; g < directoriesOfTheSong.length; g++) {
					directoriesOfTheSong[g].mkdir();
					if (directoriesOfTheSong[g].isDirectory()) {
						//mi prendo il path assoluto
						String pa = directoriesOfTheSong[g].getAbsolutePath();
						String name = directoriesOfTheSong[g].getName();
						//prendo il path delle foto e lo aggiungo alla lista
						fileList.add(pa);
						//setto anche il suo nome
						nameList.add(name);

					}
				}
			}
		}

		return fileList;
	}
}
