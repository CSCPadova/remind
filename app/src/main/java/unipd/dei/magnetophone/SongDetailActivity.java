package unipd.dei.magnetophone;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;
import android.view.View;

/**
 * An activity representing a single Song detail screen. This activity is only
 * used on handset devices. On tablet-size devices, item details are presented
 * side-by-side with a list of items in a {@link SongListActivity}.
 * <p>
 * This activity is mostly just a 'shell' activity containing nothing more than
 * a {@link SongDetailFragment}.
 */
public class SongDetailActivity extends FragmentActivity {

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_song_detail);

		//TODO commentato per ora
		// Show the Up button in the action bar.
		//getActionBar().setDisplayHomeAsUpEnabled(true);

		
		// http://developer.android.com/guide/components/fragments.html
		
		if (savedInstanceState == null) {
			// Create the detail fragment and add it to the activity
			// using a fragment transaction.
			Bundle arguments = new Bundle();
			
			//devo capire se questo fragment si crea perché qualcuno ha selezionato un brano o perché
			//c'è già un brano nel magnetofono
			
			//se qualcuno ha selezionato un brano dalla list, questo int sarà !=-1
			int fromTheIntent = getIntent().getIntExtra(SongDetailFragment.ARG_ITEM_ID, -1);
			
			//prendo l'id del brano che è nel magnetofono
			SharedPreferences songPref = this.getSharedPreferences("service", Context.MODE_PRIVATE);
			int id = songPref.getInt("song_id", -1);
			
			int toPass;
			//cerco di capire ora se sono stato chiamato da una selezione o se c'era già una canzone nel magnetofono
			if(fromTheIntent==-1)//significa che nessuno ha premuto un tasto nella lista
				toPass = id;
			else 
				toPass = fromTheIntent;
			
			arguments.putInt(SongDetailFragment.ARG_ITEM_ID, toPass);
			SongDetailFragment fragment = new SongDetailFragment();
			fragment.setArguments(arguments);
			getSupportFragmentManager().beginTransaction().add(R.id.song_detail_container, fragment).commit();
		}
	
	}//fine onCreate
	
	
	
	

	//chiamato quando qualche icona della action bar viene selezionata
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			//http://developer.android.com/design/patterns/navigation.html#up-vs-back
			NavUtils.navigateUpTo(this,new Intent(this, SongListActivity.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * metodo chiamato dal pulsante carica nastro per caricare il brano selezionato in lista
	 * @param v
	 */
	public void toTheMagnetophone(View v)
	{
		int id = getIntent().getIntExtra(SongDetailFragment.ARG_ITEM_ID, -1);
		Song s = DatabaseManager.getSongFromDatabase(id, this);
		
		//salvo nelle sharedPreferences i dati del brano selezionato al magnetofono
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
	
	/**
	 * inserisce nelle shared preferences i dati della canzone s
	 * @param s
	 * @param preferences
	 */
	public void fillPreferences(Song s, String preferences)
	{
		SharedPreferences songPref = this.getSharedPreferences(preferences, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = songPref.edit();
		//editor.putString("song_name", s.getTitle());
		//editor.putString("song_author", s.getAuthor());
		editor.putInt("song_id", s.getId());
		editor.putString("song_equalization", s.getEqualization());
		editor.putFloat("song_speed", s.getSpeed());
		//editor.putString("song_year", s.getYear());
		
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
	 * ritorna dalla visuale di dscrizione
	 * @param v
	 */
	public void goBackFromDescription(View v)
	{
		SharedPreferences songPref = this.getSharedPreferences("selected", Context.MODE_PRIVATE);
		int id = songPref.getInt("song_id", -1);
		
		Intent detailIntent = new Intent(this, SongDetailActivity.class);
		detailIntent.putExtra(SongDetailFragment.ARG_ITEM_ID, id);
		startActivity(detailIntent);	
	}
}
