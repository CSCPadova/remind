package unipd.dei.magnetophone;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import unipd.dei.magnetophone.MusicService.MusicServiceBinder;

public class MonitorSetupActivity extends AppCompatActivity {
    private final float VOLUME_SEEKBAR_MAX_VALUE = 100;
    protected MusicServiceBinder musicServiceBinder;
    private TextView trackNamesTextViews[] = new TextView[4];
    private final ServiceConnection musicServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
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
            //LinearLayout trackNameListContainer = (LinearLayout) findViewById(R.id.monitor_tracks_list_container);
            //trackNameListContainer.removeAllViews();

            //for (Song.Track track : monitorSong.getTrackList()) {
            //    TextView trackNameTextView = new TextView(getApplicationContext());
            //    trackNameTextView.setText(" - " + track.getName());
            //    trackNameTextView.setTextAppearance(getApplicationContext(), R.style.MonitorSetupTrackListItem);
            //    trackNameListContainer.addView(trackNameTextView);
            //}

            /*
             * Caricamento pannello di controllo
             *
             * il pannello di controllo dei monitor differisce a seconda del tipo di tracce audio della song
             *
             * per cui carico un diverso layout per ciascuna delle tipologie di song - 1 mono - 1 stereo - 2 mono - 4 mono
             */
            LinearLayout mainLayout = (LinearLayout) findViewById(R.id.monitor_mixer);

            switch (monitorSong.getNumberOfTracks()) {
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
            switch (monitorSong.getNumberOfTracks()) {
                case 1:
                    if (monitorSong.getTrackAtIndex(0).isMono()) {
                        /*
                         * initOneMonoControls();
                         *
                         * per ora il pannello per una traccia mono non ha alcun tipo di controllo
                         */
                    } else
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
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.monitor_setup_activity_layout);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    protected void onResume() {
        super.onResume();
        /*
         * eseguo il bind con il music service se è avviato
         */
        if (MusicService.RUNNING) {
            Log.d("MonitorSetupActivity", "Tentativo bind da MonitorSetupActivity");
            bindService(new Intent(getApplicationContext(), MusicService.class), musicServiceConnection, Context.BIND_ABOVE_CLIENT);
        }
    }

    protected void onPause() {
        /*
         * eseguo il bind con il music service se è avviato
         */
        Log.d("MonitorSetupActivity", "onPause MonitorSetupActivity");
        if (musicServiceBinder != null) {
            unbindService(musicServiceConnection);
            musicServiceBinder = null;
        }
        super.onPause();
    }

    /*
     * inizializzazione pannello controlli per una song con una traccia stereo
     */
    private void initOneStereoControls() {
        /*
         * recupero le 2 seekbar
         */
        final SeekBar seekbarSatelliteChannel1L = (SeekBar) findViewById(R.id.monitor_channel1_vol_l);
        final SeekBar seekbarSatelliteChannel1R = (SeekBar) findViewById(R.id.monitor_channel1_vol_r);
        final SeekBar seekbarSatelliteChannel2L = (SeekBar) findViewById(R.id.monitor_channel2_vol_l);
        final SeekBar seekbarSatelliteChannel2R = (SeekBar) findViewById(R.id.monitor_channel2_vol_r);

        seekbarSatelliteChannel1L.setProgress((int) (musicServiceBinder.getTrackVolumeL(1) * VOLUME_SEEKBAR_MAX_VALUE));
        seekbarSatelliteChannel1R.setProgress((int) (musicServiceBinder.getTrackVolumeR(1) * VOLUME_SEEKBAR_MAX_VALUE));
        seekbarSatelliteChannel2L.setProgress((int) (musicServiceBinder.getTrackVolumeL(2) * VOLUME_SEEKBAR_MAX_VALUE));
        seekbarSatelliteChannel2R.setProgress((int) (musicServiceBinder.getTrackVolumeR(2) * VOLUME_SEEKBAR_MAX_VALUE));

        seekbarSatelliteChannel1L.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeL(1, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbarSatelliteChannel1R.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeR(1, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbarSatelliteChannel2L.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeL(2, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbarSatelliteChannel2R.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeR(2, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Button equalVolumes1 = findViewById(R.id.buttonLR1);

        equalVolumes1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                seekbarSatelliteChannel1L.setProgress(seekbarSatelliteChannel1R.getProgress());
            }
        });

        Button equalVolumes2 = findViewById(R.id.buttonLR2);

        equalVolumes2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                seekbarSatelliteChannel2L.setProgress(seekbarSatelliteChannel2R.getProgress());
            }
        });
    }

    /*
     * inizializzazione pannello controlli per una song con due tracce mono
     *
     * sfrutta l'inizializzazione molto simile di una song con una traccia stereo
     */
    private void initTwoMonoControls() {
        initOneStereoControls();

        /*
         * recupero TextView
         */
        trackNamesTextViews[0] = (TextView) findViewById(R.id.monitor_channel1_track_name);
        trackNamesTextViews[1] = (TextView) findViewById(R.id.monitor_channel2_track_name);

        /*
         * popolo gli TextView con i nomi delle tracce
         */
        trackNamesTextViews[0].setText(musicServiceBinder.getSong().getTrackList().get(0).getName());
        trackNamesTextViews[1].setText(musicServiceBinder.getSong().getTrackList().get(1).getName());
    }

    /*
     * inizializzazione pannello controlli per una song con 4 tracce mono
     *
     * sfrutta l'inizializzazione molto simile di una song con due tracce mono
     */
    private void initFourMonoControls() {
        initTwoMonoControls();

        TextView txtView1 = (TextView) findViewById(R.id.monitor_channel1_track_name);
        TextView txtView2 = (TextView) findViewById(R.id.monitor_channel2_track_name);
        TextView txtView3 = (TextView) findViewById(R.id.monitor_channel3_track_name);
        TextView txtView4 = (TextView) findViewById(R.id.monitor_channel4_track_name);

        /*
         * recupero 2 trackNamesTextViews rimanenti
         */
        trackNamesTextViews[2] = (TextView) findViewById(R.id.monitor_channel3_track_name);
        trackNamesTextViews[3] = (TextView) findViewById(R.id.monitor_channel4_track_name);

        /*
         * popolo gli spinner con i nomi delle tracce
         */
        trackNamesTextViews[2].setText(musicServiceBinder.getSong().getTrackList().get(2).getName());
        trackNamesTextViews[3].setText(musicServiceBinder.getSong().getTrackList().get(3).getName());

        /*
         * recupero le 2 seekbar
         */
        final SeekBar seekbarSatelliteChannel3L = (SeekBar) findViewById(R.id.monitor_channel3_vol_l);
        final SeekBar seekbarSatelliteChannel3R = (SeekBar) findViewById(R.id.monitor_channel3_vol_r);
        final SeekBar seekbarSatelliteChannel4L = (SeekBar) findViewById(R.id.monitor_channel4_vol_l);
        final SeekBar seekbarSatelliteChannel4R = (SeekBar) findViewById(R.id.monitor_channel4_vol_r);

        seekbarSatelliteChannel3L.setProgress((int) (musicServiceBinder.getTrackVolumeL(3) * VOLUME_SEEKBAR_MAX_VALUE));
        seekbarSatelliteChannel3R.setProgress((int) (musicServiceBinder.getTrackVolumeR(3) * VOLUME_SEEKBAR_MAX_VALUE));
        seekbarSatelliteChannel4L.setProgress((int) (musicServiceBinder.getTrackVolumeL(4) * VOLUME_SEEKBAR_MAX_VALUE));
        seekbarSatelliteChannel4R.setProgress((int) (musicServiceBinder.getTrackVolumeR(4) * VOLUME_SEEKBAR_MAX_VALUE));

        seekbarSatelliteChannel3L.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeL(3, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbarSatelliteChannel3R.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeR(3, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbarSatelliteChannel4L.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeL(4, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbarSatelliteChannel4R.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeR(4, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        Button equalVolumes3 = findViewById(R.id.buttonLR3);

        equalVolumes3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                seekbarSatelliteChannel3L.setProgress(seekbarSatelliteChannel3R.getProgress());
            }
        });

        Button equalVolumes4 = findViewById(R.id.buttonLR4);

        equalVolumes4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                seekbarSatelliteChannel4L.setProgress(seekbarSatelliteChannel4R.getProgress());
            }
        });


    }

    private void setTrackVolumeL(int track, float volumeL) {
        if (musicServiceBinder != null)
            musicServiceBinder.setTrackVolumeL(track, volumeL);
    }

    private void setTrackVolumeR(int track, float volumeR) {
        if (musicServiceBinder != null)
            musicServiceBinder.setTrackVolumeR(track, volumeR);
    }

    /**
     * Metodo che controlla e gestisce i click sulle icone della actionBar
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) { // BACK
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return true;
        }
    }
}
