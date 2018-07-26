package unipd.dei.magnetophone;

import java.util.ArrayList;

import unipd.dei.magnetophone.MusicService.MusicServiceBinder;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MonitorSetupActivity extends Activity
{
	protected MusicServiceBinder musicServiceBinder;
	private Spinner spinners[] = new Spinner[4];

	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.monitor_setup_activity_layout);
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	protected void onResume()
	{
		super.onResume();
		/*
		 * eseguo il bind con il music service se è avviato
		 */
		if (MusicService.RUNNING)
		{
			Log.d("MonitorSetupActivity", "Tentativo bind da MonitorSetupActivity");
			bindService(new Intent(getApplicationContext(), MusicService.class), musicServiceConnection, Context.BIND_ABOVE_CLIENT);
		}
	}

	protected void onPause()
	{
		/*
		 * eseguo il bind con il music service se è avviato
		 */
		Log.d("MonitorSetupActivity", "onPause MonitorSetupActivity");
		if (musicServiceBinder != null)
		{
			unbindService(musicServiceConnection);
			musicServiceBinder = null;
		}
		super.onPause();
	}

	private final ServiceConnection musicServiceConnection = new ServiceConnection()
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder binder)
		{
			musicServiceBinder = (MusicServiceBinder) binder;
			Log.d("MonitorSetupActivity", "MusicService connesso da MonitorSetupActivity");

			/*
			 * effettuato il bind, posso accedere ai parametri del musicservice e quindi popolare l'interfaccia
			 */
			Song monitorSong = musicServiceBinder.getSong();

			// riempio i dati in cima all'activity
			TextView monitorSignature = (TextView) findViewById(R.id.monitor_signature_field);
			monitorSignature.setText(monitorSong.getSignature());

			TextView monitorType = (TextView) findViewById(R.id.monitor_type_field);
			monitorType.setText(monitorSong.getTrackAtIndex(0).isMono() ? "MONO" : "STEREO");

			TextView monitorNumber = (TextView) findViewById(R.id.monitor_tracks_number_field);
			monitorNumber.setText("" + monitorSong.getNumberOfTracks());

			/*
			 * visualizzo la lista delle tracce
			 */
			LinearLayout trackNameListContainer = (LinearLayout) findViewById(R.id.monitor_tracks_list_container);
			trackNameListContainer.removeAllViews();

			for (Song.Track track : monitorSong.getTrackList())
			{
				TextView trackNameTextView = new TextView(getApplicationContext());
				trackNameTextView.setText(" - " + track.getName());
				trackNameTextView.setTextAppearance(getApplicationContext(), R.style.MonitorSetupTrackListItem);
				trackNameListContainer.addView(trackNameTextView);
			}

			/*
			 * Caricamento pannello di controllo
			 * 
			 * il pannello di controllo dei monitor differisce a seconda del tipo di tracce audio della song
			 * 
			 * per cui carico un diverso layout per ciascuna delle tipologie di song - 1 mono - 1 stereo - 2 mono - 4 mono
			 */
			LinearLayout mainLayout = (LinearLayout) findViewById(R.id.monitor_setup_main);

			switch (monitorSong.getNumberOfTracks())
			{
				case 1:
					/*
					 * se ho una sola traccia questa può essere di tipo mono oppure di tipo stereo
					 */
					if (monitorSong.getTrackAtIndex(0).isMono())
						getLayoutInflater().inflate(R.layout.monitor_setup_control_panel_1m, mainLayout);
					else
						getLayoutInflater().inflate(R.layout.monitor_setup_control_panel_1s, mainLayout);
					break;

				case 2:
					/*
					 * 2 tracce
					 */
					getLayoutInflater().inflate(R.layout.monitor_setup_control_panel_2m, mainLayout);
					break;

				case 4:
					/*
					 * 4 tracce
					 */
					getLayoutInflater().inflate(R.layout.monitor_setup_control_panel_4m, mainLayout);
					break;

				default:
					/*
					 * nessun altro caso per ora
					 */
					break;
			}

			/*
			 * Inizializzazione del pannello di controllo
			 * 
			 * Come detto a seconda del tipo di song differisce il tipo di controlli quindi anche la gestione dei vari eventi per l'interfaccia deve essere diversificata
			 */
			switch (monitorSong.getNumberOfTracks())
			{
				case 1:
					if (monitorSong.getTrackAtIndex(0).isMono())
					{
						/*
						 * initOneMonoControls();
						 * 
						 * per ora il pannello per una traccia mono non ha alcun tipo di controllo
						 */
					}
					else
						initOneStereoControls();
					break;

				case 2:
					initTwoMonoControls();
					break;

				case 4:
					initFourMonoControls();
					break;

				default:
					/*
					 * nessun altro caso per ora
					 */
					break;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
		}
	};

	/*
	 * inizializzazione pannello controlli per una song con una traccia stereo
	 */
	private void initOneStereoControls()
	{
		/*
		 * recupero le 2 seekbar
		 */
		SeekBar seekbarSatelliteChannel1 = (SeekBar) findViewById(R.id.monitor_channel1_satellite_position_seekbar);
		SeekBar seekbarSatelliteChannel2 = (SeekBar) findViewById(R.id.monitor_channel2_satellite_position_seekbar);

		seekbarSatelliteChannel1.setProgress(musicServiceBinder.getChannelSatellitePosition(1));
		seekbarSatelliteChannel2.setProgress(100 - musicServiceBinder.getChannelSatellitePosition(2));

		/*
		 * recupero i due switch
		 */
		ToggleButton switchChannel1 = (ToggleButton) findViewById(R.id.monitor_channel1_enable_switch);
		ToggleButton switchChannel2 = (ToggleButton) findViewById(R.id.monitor_channel2_enable_switch);

		switchChannel1.setChecked(musicServiceBinder.getChannelEnabled(1));
		switchChannel2.setChecked(musicServiceBinder.getChannelEnabled(2));

		/*
		 * configurazione seekbar satellite 1
		 */
		seekbarSatelliteChannel1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				changeSatellitePosition(1, progress);	// variazione della posizione della seekbar
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		/*
		 * configurazione seekbar satellite 2
		 */
		seekbarSatelliteChannel2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				changeSatellitePosition(2, 100 - progress);	// variazione della posizione della seekbar
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}

		});

		/*
		 * configurazione switch canale 1
		 */
		switchChannel1.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setChannelEnabled(1, isChecked);
			}
		});

		/*
		 * configurazione switch canale 2
		 */
		switchChannel2.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setChannelEnabled(2, isChecked);
			}
		});
	}

	/*
	 * inizializzazione pannello controlli per una song con due tracce mono
	 * 
	 * sfrutta l'inizializzazione molto simile di una song con una traccia stereo
	 */
	private void initTwoMonoControls()
	{
		initOneStereoControls();

		/*
		 * recupero spinners
		 */
		spinners[0] = (Spinner) findViewById(R.id.monitor_channel1_track_spinner);
		spinners[1] = (Spinner) findViewById(R.id.monitor_channel2_track_spinner);

		/*
		 * popolo gli spinner con i nomi delle tracce
		 */
		ArrayList<String> trackNamesList = new ArrayList<String>();
		for (Song.Track track : musicServiceBinder.getSong().getTrackList())
			trackNamesList.add(track.getName());

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, trackNamesList);

		spinners[0].setAdapter(adapter);
		spinners[1].setAdapter(adapter);

		/*
		 * imposto le tracce riprodotte nei giusti canali
		 */
		spinners[0].setSelection(0);
		spinners[1].setSelection(1);

		/*
		 * imposto eventi
		 */
		spinners[0].setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				changeChannelTrack(1, position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		spinners[1].setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				changeChannelTrack(2, position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	/*
	 * inizializzazione pannello controlli per una song con 4 tracce mono
	 * 
	 * sfrutta l'inizializzazione molto simile di una song con due tracce mono
	 */
	private void initFourMonoControls()
	{
		initTwoMonoControls();

		/*
		 * recupero le 2 seekbar restanti
		 */
		SeekBar seekbarSatelliteChannel3 = (SeekBar) findViewById(R.id.monitor_channel3_satellite_position_seekbar);
		SeekBar seekbarSatelliteChannel4 = (SeekBar) findViewById(R.id.monitor_channel4_satellite_position_seekbar);

		seekbarSatelliteChannel3.setProgress(musicServiceBinder.getChannelSatellitePosition(3));
		seekbarSatelliteChannel4.setProgress(100 - musicServiceBinder.getChannelSatellitePosition(4));
		/*
		 * recupero i due switch restanti
		 */
		ToggleButton switchChannel3 = (ToggleButton) findViewById(R.id.monitor_channel3_enable_switch);
		ToggleButton switchChannel4 = (ToggleButton) findViewById(R.id.monitor_channel4_enable_switch);

		switchChannel3.setChecked(musicServiceBinder.getChannelEnabled(3));
		switchChannel4.setChecked(musicServiceBinder.getChannelEnabled(4));

		/*
		 * configurazione seekbar satellite 3
		 */
		seekbarSatelliteChannel3.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				changeSatellitePosition(3, progress);	// variazione della posizione della seekbar
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		/*
		 * configurazione seekbar satellite 4
		 */
		seekbarSatelliteChannel4.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				changeSatellitePosition(4, 100 - progress);	// variazione della posizione della seekbar
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
		});

		/*
		 * configurazione switch canale 3
		 */
		switchChannel3.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setChannelEnabled(3, isChecked);
			}
		});

		/*
		 * configurazione switch canale 4
		 */
		switchChannel4.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				setChannelEnabled(4, isChecked);
			}
		});

		/*
		 * recupero 2 spinners rimanenti
		 */
		spinners[2] = (Spinner) findViewById(R.id.monitor_channel3_track_spinner);
		spinners[3] = (Spinner) findViewById(R.id.monitor_channel4_track_spinner);

		/*
		 * popolo gli spinner con i nomi delle tracce
		 */
		spinners[2].setAdapter(spinners[1].getAdapter());
		spinners[3].setAdapter(spinners[1].getAdapter());

		/*
		 * imposto le tracce riprodotte nei giusti canali
		 */
		spinners[2].setSelection(2);
		spinners[3].setSelection(3);

		/*
		 * imposto eventi
		 */
		spinners[2].setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
				changeChannelTrack(3, position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		spinners[3].setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id){
				changeChannelTrack(4, position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	/*
	 * metodo che andrà a modificare la posizione virtuale di un satellite
	 */
	private void changeSatellitePosition(int satelliteNumber, int position)
	{
		if (musicServiceBinder != null)
			musicServiceBinder.setSatellitePosition(satelliteNumber, position);
	}

	/*
	 * metodo che abilita o disabilita un canale audio
	 */
	protected void setChannelEnabled(int channelNumber, boolean enabled)
	{
		if (musicServiceBinder != null)
			musicServiceBinder.setChannelEnabled(channelNumber, enabled);
	}

	protected void changeChannelTrack(int channelNumber, int trackNumber)
	{
		if (musicServiceBinder != null)
		{
			musicServiceBinder.setTrackChannel(channelNumber, trackNumber);
			int[] map = musicServiceBinder.getTrackMap();

			for (int i = 0; i < musicServiceBinder.getSong().getNumberOfTracks(); i++)
				spinners[i].setSelection(map[i]);
		}
	}

	/**
	 * Metodo che controlla e gestisce i click sulle icone della actionBar
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{ // BACK
			case android.R.id.home:
				onBackPressed();
				return true;
			default:
				return true;
		}
	}
}
