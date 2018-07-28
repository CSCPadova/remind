package unipd.dei.magnetophone;

/**
 * classe che realizza l'activity involucro della lista. Contiene solo la lista negli
 * smartphone, anche il detail nei tablet istanziando 1 o 2 fragment a seconda
 */

import java.util.LinkedList;

//import android.app.AlertDialog;
//import android.app.Dialog;
//import android.app.DialogFragment;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;


public class SongListActivity extends FragmentActivity implements
SongListFragment.Callbacks {

	/**
	 * Whether or not the activity is in two-pane mode, i.e. running on a tablet
	 * device.
	 */
	private boolean mTwoPane;
	private ProgressBar progress;//la mia barra di progresso

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_song_twopane);

		if(getActionBar()!=null)
			getActionBar().setDisplayHomeAsUpEnabled(true);

		if (findViewById(R.id.song_detail_container) != null) //se siamo con 2 fragment
		{
			//se è presente l'id song_detail_container, ossia se abbiamo il layout quello da tablet,
			//possiamo disporre i 2 pannel
			mTwoPane = true;

			//da questo momento si fa riferimento al file activity_song_twopane.xml

			//in questo stato, quando un elemento della lista viene toccato deve "attivarsi"
			((SongListFragment) getSupportFragmentManager().findFragmentById(R.id.song_list)).setActivateOnItemClick(true);

			//sempre nel caso che stiamo mostrando anche il detail
			//controllo se c'era un brano caricato nel magnetofono. In questo caso, invece di schermo vuoto mostro le informazioni lui riguardanti
			SharedPreferences songPref = this.getSharedPreferences("service", Context.MODE_PRIVATE);
			int id = songPref.getInt("song_id", -1);

			//mi informo anche se non sto tornando dal menu di ricerca o da altri menu
			Intent myIntent = getIntent();
			int fromSearch = -1;
			if(myIntent!=null)
			{
				fromSearch = myIntent.getIntExtra("modify", -1);
			}
			
			SharedPreferences songPrefSelected = this.getSharedPreferences("selected", Context.MODE_PRIVATE);
			int selectedId = songPrefSelected.getInt("song_id", -1);
			
			
			
			
			// Creo e aggiungo la progressbar, posizionandola all'inizio del content (sotto la ActionBar)
			final FrameLayout decorView = (FrameLayout)getWindow().getDecorView();
			
			progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
			progress.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 15));
					
			decorView.addView(progress);
			
			// Non posso posizionarlo ora, aspetto che tutte le view siano state posizionate nel layout
			ViewTreeObserver observer = progress.getViewTreeObserver();			
			observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
			    @Override
			    public void onGlobalLayout() {
			        View contentView = decorView.findViewById(android.R.id.content);
			        progress.setY(contentView.getY() - 5);

			        // Non rimuovo la callback perchè verrà chiamata più volte quando cambia il layout,
			        // e ogni volta aggiusto la posizione della progressbar.
			        // Serve in alcuni casi, come quando entrando nell'activity viene preparato il layout
			        // prima che la barra notifiche sia riapparsa (si sta uscendo dal fullscreen) e solo 
			        // successivamente viene spinto giù tutto tranne la progress bar che finisce a metà
			        // dell'action bar.
			    }
			});
			
			//se c'è un brano nel magnetofono e non sto tornando dalla ricerca, faccio vedere il brano nel magnetofono
			if(id!=-1 && fromSearch == -1)
			{

				//informo le shared preferences che, di fatto, è come se il brano fosse stato "selezionato" di nuovo
				//dalla lista essendo che torniamo dal magnetofono carico
				songPrefSelected = this.getSharedPreferences("selected", Context.MODE_PRIVATE);
				SharedPreferences.Editor editor = songPrefSelected.edit();
				editor.putInt("song_id", id);
				editor.commit();
				

				//creo un bundle che passerò al fragment detail con l'id della canzone
				//nella lista selezionata dall'utente
				Bundle arguments = new Bundle();
				//inserisco l'id della canzone come informazione nel bundle
				arguments.putInt(SongDetailFragment.ARG_ITEM_ID, id);
				//creo il fragment
				SongDetailFragment fragment = new SongDetailFragment();
				//gli do in consegna il bundle
				fragment.setArguments(arguments);
				//gli do il via
				getSupportFragmentManager().beginTransaction()
				.replace(R.id.song_detail_container, fragment).commit();
			}
//<<<<<<< 
			else if(fromSearch==1 && selectedId!=-1)//stiamo tornando da una ricerca o da un import o da una modifica e c'è 
				//un brano selezionato
			{
//=======
//			else if(fromSearch==1)//stiamo tornando da una ricerca o da un import o da una modifica
//			{	

				
				
				songPrefSelected = this.getSharedPreferences("selected", Context.MODE_PRIVATE);
				
				id = songPrefSelected.getInt("song_id", -1);
				Bundle arguments = new Bundle();
				arguments.putInt(SongDetailFragment.ARG_ITEM_ID, id);
				SongDetailFragment fragment = new SongDetailFragment();
				fragment.setArguments(arguments);
				getSupportFragmentManager().beginTransaction()
				.replace(R.id.song_detail_container, fragment).commit();
			}
			else//non c'è nulla nel magnetofono e non stiamo tornado da alcuna ricerca, 
				//dunque faccio comparire un detail che inviti a scegliere un nuovo brano
			{

				Bundle arguments = new Bundle();
				//inserisco nel bundle l'info -3, che per mia convenzione considero come l'informazione di
				//mostrare il default detail
				arguments.putInt(SongDetailFragment.ARG_ITEM_ID, -3);
				//creo il fragment
				SongDetailFragment fragment = new SongDetailFragment();
				//gli do in consegna il bundle
				fragment.setArguments(arguments);
				//gli do il via
				getSupportFragmentManager().beginTransaction()
				.replace(R.id.song_detail_container, fragment).commit();
			}

		}

		manageBar();

	}//fine onCreate

	/**
	 * Da il via all'aggiornamento della progressBar, invocato solo se la canzone è in riproduzione
	 */
	public void startProgressBar()
	{
		MusicPlayer player = MusicPlayer.getInstance();
		progress.setMax((int)player.getCurrentSongLength());
		//progress.setProgress((int)player.getCurrentSongProgress());
		(new BarUpdaterThread()).start();
	}

	/**
	 * setta la progress bar al valore passato come parametro
	 * @param position
	 */
	public void setProgressBar(int position)
	{
		SharedPreferences songPref = getSharedPreferences("service", Context.MODE_PRIVATE);
		float maxLength = songPref.getFloat("song_recovery_length", 0);

		if(maxLength>=position && maxLength>0)
		{
			progress.setMax((int)maxLength);
			progress.setProgress(position);
		}
		else
		{
			//l'utente ha caricato la canzone ed è tornato indietro subito alla libreria, non facendo nulla
			//mostra la barra vuota
		}
	}

	/**
	 * gestisce la bar che mostra la song in riproduzione ogni volta che si ritorna sulla lista
	 */
	public void manageBar()
	{
		MusicPlayer player = MusicPlayer.getInstance();
		Song s = player.getSong();

		SharedPreferences songPref = this.getSharedPreferences("service", Context.MODE_PRIVATE);

		if(s!=null)//la canzone è in riproduzione
		{
			startProgressBar();						//facciamo partire l'aggiornamento della progressBar 
		}
		else//può essere che la canzone non sia in riproduzione attualmente, in quel caso getSong ritorna null
		{
			float position = songPref.getFloat("CurrentSecond", 0);	
			setProgressBar((int)position);
		}
	}

	/**
	 * Callback invocato dal fragment quando viene selezionato un elemento della lista
	 */
	@Override
	public void onItemSelected(ListView listView, View view, int position) 
	{
		

		Song currentSong = SongListFragment.finalList.get(position);
		//prendo l'id della song scelta
		int id = currentSong.getId();

		//salvo l'id della canzone selezionata per ora
		SharedPreferences songPref = this.getSharedPreferences("selected", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = songPref.edit();
		editor.putInt("song_id", id);
		editor.putInt("position", position);
		editor.putInt("view", view.getId());
		editor.putInt("listview", listView.getId());
		editor.commit();
				
		//evidenzio l'elemento selezionato nella lista
				for(int i = 0; i< listView.getChildCount(); i++)
					listView.getChildAt(i).setSelected(false);
				
				StateListDrawable states = new StateListDrawable();
				states.addState(new int[] {android.R.attr.state_selected},
						getResources().getDrawable(R.drawable.list_pressed));
				states.addState(new int[] { },
						getResources().getDrawable(R.drawable.list_altered));
				view.setBackground(states);
				view.setSelected(true);


		if (mTwoPane)//se siamo nella modalità a 2 pannelli 
		{
			//creo un bundle che passerò al fragment detail con l'id della canzone
			//nella lista selezionata dall'utente
			Bundle arguments = new Bundle();
			//inserisco l'id della canzone come informazione nel bundle
			arguments.putInt(SongDetailFragment.ARG_ITEM_ID, id);
			//creo il fragment
			SongDetailFragment fragment = new SongDetailFragment();
			//gli do in consegna il bundle
			fragment.setArguments(arguments);
			//gli do il via
			getSupportFragmentManager().beginTransaction()
			.replace(R.id.song_detail_container, fragment).commit();

		} 
		else 
		{
			//nella modalità a singolo pannello, semplicemente attiva l'activity che gestisce separatamente
			//l'altro fragment
			Intent detailIntent = new Intent(this, SongDetailActivity.class);
			//comunque passo all'activity l'indice, affinché sia lei a passarlo al fragment
			detailIntent.putExtra(SongDetailFragment.ARG_ITEM_ID, id);
			startActivity(detailIntent);
		}

	}

	/**
	 * Metodo che crea l'actionBar, inserisco l'icona di importazione solo se abilitata nelle
	 * opzioni generali del magnetofono
	 */
	public boolean onCreateOptionsMenu(Menu menu) 
	{

		MenuInflater inflater = getMenuInflater();
		//Soltanto se ho importa canzoni attivo carico l'icona per importare!
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		if (sharedPref.getBoolean("Maintenance", false))
		{
			inflater.inflate(R.menu.action_bar_menu, menu);
		}
		else
		{
			inflater.inflate(R.menu.action_bar_little_menu, menu);
		}

		//inserisco il layout prescelto nella action bar 

		//mi prendo il searchManager
		SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);

		//prendo l'oggetto "widget di ricerca"
		MenuItem searchItem = menu.findItem(R.id.ic_action_search);
		SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
		//informo il sistema che la classe SearchActivity si preoccupa della gestione della ricerca
		searchView.setSearchableInfo(searchManager.getSearchableInfo(
				new ComponentName("it.unipd.dei.esp1314.magnetophone", SearchActivity.class.getName())));
		searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
		searchView.setSubmitButtonEnabled(true);

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Metodo che controlla e gestisce i click sulle icone della actionBar
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
		//preerences del brano attualmente in riproduzione

		switch (item.getItemId()) 
		{	// BACK
			case android.R.id.home:
				onBackPressed();
				return true;
				//IMPORTA
			case R.id.ic_action_import:
				AppCompatDialogFragment importDialog = new AlertImportFragment();
				importDialog.show(getSupportFragmentManager(), "import_alert");
				return true;
				//EDIT
			case R.id.ic_action_edit:
				AppCompatDialogFragment edit = new AlertEditFragment();
				edit.show(getSupportFragmentManager(), "edit_alert");
				return true;
				//ELIMINA
			case R.id.ic_action_discard:
				//mostro il popup che si occupa della cosa
				AppCompatDialogFragment alert = new AlertDeleteFragment();
				alert.show(getSupportFragmentManager(), "delete_alert");
				return true;
				//REFRESH
			case R.id.ic_action_refresh:
				AppCompatDialogFragment refresh = new AlertRefreshFragment();
				refresh.show(getSupportFragmentManager(), "refresh_alert");
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * metodo chiamato dal pulsante carica nastro per caricare il brano selezionato in lista
	 * @param v
	 */
	public void onChargeTapePressed(View v)
	{
		//prendo l'id della canzone selezionata
		SharedPreferences songPref = this.getSharedPreferences("selected", Context.MODE_PRIVATE);
		int id = songPref.getInt("song_id", -1);
		Song s = DatabaseManager.getSongFromDatabase(id, this);
		
		////
		if(s.isValid())
		{
			//salvo nelle sharedPreferences i dati del brano caricato nel magnetofono
			fillPreferences(s, "service");
			Intent toMagnetophone = new Intent(this, MagnetophoneActivity.class);
			toMagnetophone.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

			Song.fillIntent(s, toMagnetophone);
			MusicPlayer player = MusicPlayer.getInstance();
			//Se seleziono la stessa canzone che sto già riproducendo non faccio niente
			if(!(player.getSong() != null && player.getSong().getId() == s.getId() && player.isPlaying()))
			{
				//player.restartService();	//Aggiorno il servizio
				player.setSong(s);
			}
			startActivity(toMagnetophone);
		}
		else
		{
			Toast.makeText(getApplicationContext(), getString(R.string.charging_song_alert), Toast.LENGTH_SHORT).show();
		}
		////

		
	}

	/**
	 * inserisce nelle shared preferences i dati della canzone s
	 * @param s
	 * @param preferences
	 */
	public void fillPreferences(Song s, String preferences)
	{
		SharedPreferences songPref = getSharedPreferences(preferences, Context.MODE_PRIVATE);
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

		//salvo i dati che mi interessano per la track: path
		for(int i = 1; i<=s.getNumberOfTracks(); i++)
		{
			editor.putString("song_track_" + i, s.getTrackAtIndex(i-1).getPath());
		}

		editor.commit();
	}

	/**
	 * metodo invocato quando viene premuto il bottone "vedi descrizione".
	 * Seguo una mia convenzione per cui passo il valore -3 per avvisare che desidero mostrare la descrizione
	 * @param v
	 */
	public void onDescriptionButtonPressed(View v)
	{

		Bundle arguments = new Bundle();
		//inserisco -2 nel bundle, il fragment, leggendolo, comprenderà che cosa deve fare
		arguments.putInt(SongDetailFragment.ARG_ITEM_ID, -2);
		//creo il fragment
		SongDetailFragment fragment = new SongDetailFragment();
		//gli do in consegna il bundle
		fragment.setArguments(arguments);
		//gli do il via
		getSupportFragmentManager().beginTransaction()
		.replace(R.id.song_detail_container, fragment).commit();
	}

	/**
	 * invocato dal tasto "torna al dettaglio" per consentire il ritorno al fragment del dettaglio
	 * con la panoramica sui metadati della song a partire dalla View sulla sua descrizione
	 * @param v
	 */
	public void goBackFromDescription(View v)
	{
		SharedPreferences songPref = this.getSharedPreferences("selected", Context.MODE_PRIVATE);
		int id = songPref.getInt("song_id", -1);

		Bundle arguments = new Bundle();
		//inserisco -2 nel bundle, il fragment, leggendolo, comprenderà che cosa deve fare
		arguments.putInt(SongDetailFragment.ARG_ITEM_ID, id);
		//creo il fragment
		SongDetailFragment fragment = new SongDetailFragment();
		//gli do in consegna il bundle
		fragment.setArguments(arguments);
		//gli do il via
		getSupportFragmentManager().beginTransaction()
		.replace(R.id.song_detail_container, fragment).commit();
	}

	/**
	 * metodo che elimina la canzone eliminata dal database
	 * dalla lista dei brani mostrata all'utente
	 */
	public static void deleteSongFromList(Song s)
	{
		//prendo l'adapter
		CustomAdapterListFragment ad = SongListFragment.adapter;

		deleteSongFromLinkedList(s);
		ad.notifyDataSetChanged();

	}

	/**
	 * metodo che elimina il brano passato dalla LinkedList interna
	 * @param s
	 */
	private static void deleteSongFromLinkedList(Song s)
	{
		LinkedList<Song> list = SongListFragment.finalList;
		int id = s.getId();
		int length = list.size();
		for(int i=0; i<length; i++)
		{
			if(list.get(i).getId() == id)
			{
				list.remove(i);
				break;
			}	
		}
	}

	//##################################################################################//

	//############################## DIALOG ###########################################//

	/**
	 * Dialog che ci chiede se siamo certi di voler eliminare la canzone scelta
	 * @author dennisdosso
	 *
	 */
	public static class AlertDeleteFragment extends AppCompatDialogFragment
	{
		@Override
		public AppCompatDialog onCreateDialog(Bundle SavedInstanceState)
		{
			final MusicPlayer player = MusicPlayer.getInstance();


			//preferences del brano attualmente in evidenza sul detail
			final SharedPreferences sharedPrefSelected = getActivity().getSharedPreferences("selected", Context.MODE_PRIVATE);
			//preferences del brano attualmente in riproduzione
			final SharedPreferences sharedPrefService = getActivity().getSharedPreferences("service", Context.MODE_PRIVATE);

			//canzone selezionata sulla lista
			int id = sharedPrefSelected.getInt("song_id", -1);
			final Song selectedSong;
			if(id!=-1)
			{
				selectedSong = DatabaseManager.getSongFromDatabase(id, getActivity());
			}
			else
				selectedSong=null;

			id = sharedPrefService.getInt("song_id", -1);
			final Song chargedSong;	
			if(id!=-1)
			{
				//canzone sul magnetofono
				chargedSong = DatabaseManager.getSongFromDatabase(sharedPrefService.getInt("song_id", -1), getActivity());				
			}
			else
				chargedSong=null;


			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			builder.setTitle(R.string.sure_to_delete_song);
			builder.setMessage(R.string.delete_is_irreversible);

			//codice per il bottone di OK all'eliminazione
			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) 
				{
					//se stiamo attualmente riproducendo la canzone che si vuole eliminare, fermiamo l'utente
					if(chargedSong!=null && player.isPlaying() && (player.getSong().getId() == chargedSong.getId()))
					{
						Toast.makeText(getActivity().getApplicationContext(), getString(R.string.playing_song_alert), Toast.LENGTH_SHORT).show();
						return;
					}
					else//se non stiamo riproducendo la canzone che voglio eliminare
					{
						DatabaseManager dbManager = new DatabaseManager(getActivity());
						if(selectedSong!=null)
						{
							//elimio dal database se presente
							dbManager.removeSingleSongFromDatabase(selectedSong);

							//							deleteSongFromList(selectedSong);

							//Se è la canzone in riproduzione ed è in stop la tolgo dal magnetofono
							if(player.getPlayerSong() != null && chargedSong.getId() == player.getPlayerSong().getId())
							{
								player.setSong(null);
							}

							//salvo in memoria che non c'è più né una song sul magnetofono né selezionata
							//-1 è il numero scelto per mia convenzione per dire che non c'è song
							signSongIsDeleted(getActivity());

							//faccio ripartire l'activity con la lista dei brani affinché mostri il brano eliminato 
							Intent restartIntent = new Intent(getActivity(), SongListActivity.class);
							restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
							getActivity().startActivity(restartIntent);							
						}
						else
						{
							String message = getActivity().getString(R.string.unable_to_delete);
							Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
						}

					} 

				}
			});
			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User cancelled the dialog
				}
			});

			return builder.create();
		}
	}

	/**
	 * dialog che ci permette di andare al menù di modifica
	 * @author dennisdosso
	 *
	 */
	public static class AlertEditFragment extends AppCompatDialogFragment
	{
		@Override
		public AppCompatDialog onCreateDialog(Bundle SavedInstanceState)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
			final MusicPlayer player = MusicPlayer.getInstance();
			final SharedPreferences sharedPrefSelected = getActivity().getSharedPreferences("selected", Context.MODE_PRIVATE);

			builder.setTitle(R.string.edit);
			builder.setMessage(R.string.pass_to_edit_activity);

			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) 
				{

					int idSong = sharedPrefSelected.getInt("song_id", -1);

					Song s;
					if(idSong==-1)
						s=null;
					else
						s = DatabaseManager.getSongFromDatabase(idSong, getActivity());

					if(s!=null)//se la canzone è presente nel database
					{
						//se sto riproducendo la canzone che voglio modificare, lo impedisco
						if(player.isPlaying() && player.getSong().getId() == idSong)
							Toast.makeText(getActivity().getApplicationContext(), getString(R.string.playing_song_alert), Toast.LENGTH_SHORT).show();
						else//tutto ok, procediamo
						{
							Intent intent = new Intent(getActivity(), SingleSongSettingsActivity.class);
							intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //non si vuole che con back si torni alle opzioni dalla lista dei brani
							Song.fillIntent(s, intent);
							startActivity(intent);
						}
					}
					else
					{
						String message = getActivity().getString(R.string.unable_to_edit);
						Toast.makeText(getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show();
					}

				}
			});

			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User cancelled the dialog
				}
			});

			return builder.create();
		}
	}

	/**
	 * dialog che chiede conferma del passaggio all'activity di importazione brani
	 * @author dennisdosso
	 *
	 */
	public static class AlertImportFragment extends AppCompatDialogFragment
	{
		@Override
		public AppCompatDialog onCreateDialog(Bundle SavedInstanceState)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			builder.setTitle(R.string.import_setting);
			builder.setMessage(R.string.pass_to_import_activity);

			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) 
				{
					//qui si deve chiamare l'activity che permette di fare l'import
					Intent intent = new Intent(getActivity(), ImportSongActivity.class);
					startActivity(intent);
				}
			});

			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User cancelled the dialog
				}
			});

			return builder.create();
		}
	}

	/**
	 * dialog che chiede conferma del desiderio di fare il refresh del database attingendo dalla cartella delle song
	 * @author dennisdosso
	 *
	 */
	public static class AlertRefreshFragment extends AppCompatDialogFragment
	{
		@Override
		public AppCompatDialog onCreateDialog(Bundle SavedInstanceState)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

			builder.setTitle(R.string.refresh_setting);
			builder.setMessage(R.string.pass_to_refresh_activity);

			builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) 
				{
					XmlImport imp = new XmlImport(getActivity());
					// creo/apro il database
					MagnetophoneOpenHelper dbHelper = new MagnetophoneOpenHelper(getActivity());
					//invoco il metodo che si occupa di importare le modifiche ai file XML
					imp.importazioneXML(getActivity(), dbHelper);

					//faccio ripartire l'activity con la lista dei brani affinché mostri eventuali modifiche
					Intent restartIntent = new Intent(getActivity(), SongListActivity.class);
					restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					getActivity().startActivity(restartIntent);
				}
			});

			builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					// User cancelled the dialog
				}
			});

			return builder.create();
		}
	}
	//##################################### FINE DEI DIALOG ########################################//

	/**
	 * Pone a -1 l'info sugli id delle song nelle sharedPreferences per far sapere all'activity che non ci sono
	 * più brani validi da visualizzare nel detail
	 * @param cont
	 */
	private static void signSongIsDeleted(Context cont)
	{
		//preferences del brano attualmente in evidenza sul detail
		SharedPreferences sharedPrefSelected = cont.getSharedPreferences("selected", Context.MODE_PRIVATE);
		//preerences del brano attualmente in riproduzione
		SharedPreferences sharedPrefService = cont.getSharedPreferences("service", Context.MODE_PRIVATE);

		SharedPreferences.Editor editor2 = sharedPrefService.edit();
		editor2.putInt("song_id", -1);
		editor2.commit();

		SharedPreferences.Editor editor = sharedPrefSelected.edit();
		editor.putInt("song_id", -1);
		editor.putInt("position", -1);
		editor.putInt("view", -1);
		editor.putInt("listview", -1);
		editor.commit();

	}

	public void onPhotosButtonPressed(View v)
	{
		Intent detailIntent = new Intent(this, SlideShowActivity.class);
		SharedPreferences shared = this.getSharedPreferences("selected", Context.MODE_PRIVATE);
		int id = shared.getInt("song_id", -1);
		int index = shared.getInt("photo_index", -1);
		detailIntent.putExtra("song_id", id);
		detailIntent.putExtra("photo_index", index);
		startActivity(detailIntent);
	}


	//########################## THREAD ################################

	public class BarUpdaterThread extends Thread
	{
		SharedPreferences songPref = getSharedPreferences("service", Context.MODE_PRIVATE);
		@Override
		public void run()
		{
			MusicPlayer player = MusicPlayer.getInstance();
			float currentPosition= 0;
			int total = (int)player.getCurrentSongLength();//lunghezza totale della canzone
			SharedPreferences.Editor editor = songPref.edit();
			do {
				try {
					Thread.sleep(250);
					//currentPosition= player.getCurrentSongProgress();
					editor.putFloat("CurrentSecond", currentPosition);
					editor.commit();
				} catch (InterruptedException e) {
					return;
				} catch (Exception e) {
					return;
				}   
					progress.setProgress((int)currentPosition);
			}
			while(currentPosition<total && player.isPlaying());

				progress.setProgress(total);
		}
	}

	public void onResume()
	{
		super.onResume();

		SharedPreferences shared = getSharedPreferences("service", Context.MODE_PRIVATE);
		boolean refreshed = shared.getBoolean("refreshed", false);
		if(refreshed)
		{
			RefreshAlertFragment alert = new RefreshAlertFragment();
			alert.show(getFragmentManager(), "refresh_alert");
			Editor editor = shared.edit();
			editor.putBoolean("refreshed", false);
			editor.commit();
		}
		manageBar();
	}
}
