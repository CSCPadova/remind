package unipd.dei.magnetophone.activity;

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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import unipd.dei.magnetophone.MusicService;
import unipd.dei.magnetophone.MusicService.MusicServiceBinder;
import unipd.dei.magnetophone.R;
import unipd.dei.magnetophone.utility.Song;

import static unipd.dei.magnetophone.utility.Utility.showSupportActionBar;

public class MonitorSetupActivity extends AppCompatActivity {
    private final float VOLUME_SEEKBAR_MAX_VALUE = 200.0f;
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

            final SeekBar seekbarMasterVolume = (SeekBar) findViewById(R.id.monitor_master_volume);

            seekbarMasterVolume.setProgress((int) (getMasterVolume() * VOLUME_SEEKBAR_MAX_VALUE));

            seekbarMasterVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    setMasterVolume((float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                }
            });

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
                        getLayoutInflater().inflate(R.layout.monitor_setup_control_panel_2m, mainLayout);
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
                        initOneMonoControls();
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

        setContentView(R.layout.monitor_setup_activity_layout);
        showSupportActionBar(this, null, getWindow().getDecorView());
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
     * inizializzazione pannello controlli per una song con una traccia mono
     */
    private void initOneMonoControls() {
        trackNamesTextViews[0] = (TextView) findViewById(R.id.monitor_channel1_track_name);

        /*
         * popolo gli TextView con i nomi delle tracce
         */
        trackNamesTextViews[0].setText(musicServiceBinder.getSong().getTrackList().get(0).getName());

        /*
         * recupero le 2 seekbar
         */
        final SeekBar seekbarSatelliteChannel1Pan = (SeekBar) findViewById(R.id.monitor_channel1_pan);

        seekbarSatelliteChannel1Pan.setProgress((int) (musicServiceBinder.getTrackVolumeR(1) * VOLUME_SEEKBAR_MAX_VALUE));

        seekbarSatelliteChannel1Pan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeL(1, (VOLUME_SEEKBAR_MAX_VALUE - (float) (progress)) / VOLUME_SEEKBAR_MAX_VALUE);
                setTrackVolumeR(1, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final Button equalVolumes1 = findViewById(R.id.buttonLR1);

        equalVolumes1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                seekbarSatelliteChannel1Pan.setProgress((int) (VOLUME_SEEKBAR_MAX_VALUE / 2));
            }
        });

        CheckBox checkBoxTrack1 = (CheckBox) findViewById(R.id.checkBox_enable_track1);

        checkBoxTrack1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                seekbarSatelliteChannel1Pan.setVisibility(isChecked == true ? View.VISIBLE : View.GONE);
                equalVolumes1.setVisibility(isChecked == true ? View.VISIBLE : View.GONE);
                setTrackEnable(1, isChecked);
            }
        });

        checkBoxTrack1.setChecked(getTrackEnable(1));
        seekbarSatelliteChannel1Pan.setVisibility(getTrackEnable(1) == true ? View.VISIBLE : View.GONE);
        equalVolumes1.setVisibility((getTrackEnable(1) == true) ? View.VISIBLE : View.GONE);
    }

    /*
     * inizializzazione pannello controlli per una song con una traccia stereo
     */
    private void initOneStereoControls() {

        initOneMonoControls();

        trackNamesTextViews[1] = (TextView) findViewById(R.id.monitor_channel2_track_name);

        /*
         * popolo gli TextView con i nomi delle tracce
         */
        trackNamesTextViews[1].setText(musicServiceBinder.getSong().getTrackList().get(0).getName());

        /*
         * recupero la seekbar
         */
        final SeekBar seekbarSatelliteChannel2Pan = (SeekBar) findViewById(R.id.monitor_channel2_pan);

        seekbarSatelliteChannel2Pan.setProgress((int) (musicServiceBinder.getTrackVolumeR(2) * VOLUME_SEEKBAR_MAX_VALUE));

        seekbarSatelliteChannel2Pan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeL(2, (VOLUME_SEEKBAR_MAX_VALUE - (float) (progress)) / VOLUME_SEEKBAR_MAX_VALUE);
                setTrackVolumeR(2, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final Button equalVolumes2 = findViewById(R.id.buttonLR2);

        equalVolumes2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                seekbarSatelliteChannel2Pan.setProgress((int) (VOLUME_SEEKBAR_MAX_VALUE / 2));
            }
        });

        CheckBox checkBoxTrack2 = (CheckBox) findViewById(R.id.checkBox_enable_track2);

        checkBoxTrack2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                seekbarSatelliteChannel2Pan.setVisibility(isChecked == true ? View.VISIBLE : View.GONE);
                equalVolumes2.setVisibility(isChecked == true ? View.VISIBLE : View.GONE);
                setTrackEnable(2, isChecked);
            }
        });

        checkBoxTrack2.setChecked(getTrackEnable(2));
        seekbarSatelliteChannel2Pan.setVisibility(getTrackEnable(2) == true ? View.VISIBLE : View.GONE);
        equalVolumes2.setVisibility((getTrackEnable(2) == true) ? View.VISIBLE : View.GONE);
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
        final SeekBar seekbarSatelliteChannel3Pan = (SeekBar) findViewById(R.id.monitor_channel3_pan);
        final SeekBar seekbarSatelliteChannel4Pan = (SeekBar) findViewById(R.id.monitor_channel4_pan);

        seekbarSatelliteChannel3Pan.setProgress((int) (musicServiceBinder.getTrackVolumeR(3) * VOLUME_SEEKBAR_MAX_VALUE));
        seekbarSatelliteChannel4Pan.setProgress((int) (musicServiceBinder.getTrackVolumeR(4) * VOLUME_SEEKBAR_MAX_VALUE));

        seekbarSatelliteChannel3Pan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeL(3, (VOLUME_SEEKBAR_MAX_VALUE - (float) (progress)) / VOLUME_SEEKBAR_MAX_VALUE);
                setTrackVolumeR(3, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbarSatelliteChannel4Pan.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                setTrackVolumeL(4, (VOLUME_SEEKBAR_MAX_VALUE - (float) (progress)) / VOLUME_SEEKBAR_MAX_VALUE);
                setTrackVolumeR(4, (float) (progress) / VOLUME_SEEKBAR_MAX_VALUE);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        final Button equalVolumes3 = findViewById(R.id.buttonLR3);

        equalVolumes3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                seekbarSatelliteChannel3Pan.setProgress((int) (VOLUME_SEEKBAR_MAX_VALUE / 2));
            }
        });

        final Button equalVolumes4 = findViewById(R.id.buttonLR4);

        equalVolumes4.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                seekbarSatelliteChannel4Pan.setProgress((int) (VOLUME_SEEKBAR_MAX_VALUE / 2));
            }
        });

        CheckBox checkBoxTrack3 = (CheckBox) findViewById(R.id.checkBox_enable_track3);

        checkBoxTrack3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                seekbarSatelliteChannel3Pan.setVisibility(isChecked == true ? View.VISIBLE : View.GONE);
                equalVolumes3.setVisibility(isChecked == true ? View.VISIBLE : View.GONE);
                setTrackEnable(3, isChecked);
            }
        });

        CheckBox checkBoxTrack4 = (CheckBox) findViewById(R.id.checkBox_enable_track4);

        checkBoxTrack4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                seekbarSatelliteChannel4Pan.setVisibility(isChecked == true ? View.VISIBLE : View.GONE);
                equalVolumes4.setVisibility(isChecked == true ? View.VISIBLE : View.GONE);
                setTrackEnable(4, isChecked);
            }
        });

        checkBoxTrack3.setChecked(getTrackEnable(3));
        seekbarSatelliteChannel3Pan.setVisibility(getTrackEnable(3) == true ? View.VISIBLE : View.GONE);
        equalVolumes3.setVisibility((getTrackEnable(3) == true) ? View.VISIBLE : View.GONE);


        checkBoxTrack4.setChecked(getTrackEnable(4));
        seekbarSatelliteChannel4Pan.setVisibility(getTrackEnable(4) == true ? View.VISIBLE : View.GONE);
        equalVolumes4.setVisibility((getTrackEnable(4) == true) ? View.VISIBLE : View.GONE);
    }

    private void setMasterVolume(float volume) {
        if (musicServiceBinder != null)
            musicServiceBinder.setMasterVolume(volume);
    }

    private float getMasterVolume() {
        if (musicServiceBinder != null)
            return musicServiceBinder.getMasterVolume();
        return 0;
    }

    private void setTrackVolumeL(int track, float volumeL) {
        if (musicServiceBinder != null)
            musicServiceBinder.setTrackVolumeL(track, volumeL);
    }

    private void setTrackVolumeR(int track, float volumeR) {
        if (musicServiceBinder != null)
            musicServiceBinder.setTrackVolumeR(track, volumeR);
    }

    private void setTrackEnable(int track, boolean enable) {
        if (musicServiceBinder != null)
            musicServiceBinder.setTrackEnable(track, enable);
    }

    private boolean getTrackEnable(int track) {
        if (musicServiceBinder != null)
            return musicServiceBinder.getTrackEnable(track);
        return false;
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
