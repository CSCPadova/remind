package unipd.dei.magnetophone.graphics;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Toast;

import unipd.dei.magnetophone.R;
import unipd.dei.magnetophone.activity.LibraryActivity;
import unipd.dei.magnetophone.activity.MonitorSetupActivity;
import unipd.dei.magnetophone.activity.SettingsActivity;
import unipd.dei.magnetophone.graphics.UIConnector.Side;
import unipd.dei.magnetophone.utility.PlayerEqualization;
import unipd.dei.magnetophone.utility.Song;
import unipd.dei.magnetophone.utility.Song.SongSpeed;

import static java.lang.Thread.sleep;

//import unipd.dei.whatsnew.main.MainActivity;
//import it.unipd.dei.esp1314.magnetophone.R;

public class StuderTapeDeck extends TapeDeck {
    private final float VIDEO_OFFSET_STEP = 0.1f;
    private final PlayerEqualization[] eq_values = {
            PlayerEqualization.CCIR,
            PlayerEqualization.NAB
            //,PlayerEqualization.FLAT non piu' usata
    };
    private final SongSpeed speed_values[] = {
            SongSpeed.SONG_SPEED_30,
            SongSpeed.SONG_SPEED_15,
            SongSpeed.SONG_SPEED_7_5,
            SongSpeed.SONG_SPEED_3_75
    };
    // Componenti che cambieranno stato ma che sono simili tra loro
    private UILed[] controlLeds;
    private UILed tapeKnob;
    private UIConnector[] tapeChunks;
    // Componenti che userò spesso
    private UITapeReel leftReel, rightReel;    // Bobine
    private final UIKnob speedKnob, eqKnob;            // Manopole (per due non conveniva usare un'array)
    private UILcd lcd;                            // Display
    private UILcdCustom lcdOffset;
    private float video_offset_increment;
    private int thread_sleep;
    private boolean threadRun;
    private boolean songLoaded;
    private float songDuration;

    private float referenceTimestamp;

    public StuderTapeDeck(Context ctx) {
        super(ctx);

        Resources r = context.getResources();

        System.gc();

        // IMMAGINI STATICHE

        // Frame del magnetofono: index auto
        // x y z w h resource
        addComponent(new UIStatic(75, 45, 0, 1542, 0, r.getDrawable(R.raw.background)));
        addComponent(new UIStatic(1757, 420, 0, 661, 453, r.getDrawable(R.raw.video_frame)));
        addComponent(new UIStatic(1881, 1056, 1, 612, 485, r.getDrawable(R.raw.project_logo)));

        addComponent(new UIStatic(419, 419, 3, 400, r.getDrawable(R.raw.reel_center)));
        addComponent(new UIStatic(1277, 419, 3, 400, r.getDrawable(R.raw.reel_center)));

        // OGGETTI ROTANTI

        UIRoundElement[] rotating = new UIRoundElement[6];

        // Bobine z radius outer inner
        // x y index res res
        rotating[0] = (UIRoundElement) addComponent(new UITapeReel(419, 419, 2, 400, r.getDrawable(R.raw.reel), r.getDrawable(R.raw.tape)));
        rotating[1] = (UIRoundElement) addComponent(new UITapeReel(1277, 419, 2, 400, r.getDrawable(R.raw.reel), r.getDrawable(R.raw.tape)));

        // Tamburi piccoli z radius
        // x y index resource
        rotating[2] = (UIRoundElement) addComponent(new UIDrum(250, 1016, 2, 35, 25, r.getDrawable(R.raw.small_drum_bg)));
        rotating[3] = (UIRoundElement) addComponent(new UIDrum(1438, 1016, 2, 35, 25, r.getDrawable(R.raw.small_drum_bg)));

        // Tamburi grandi
        rotating[4] = (UIRoundElement) addComponent(new UIDrum(424, 947, 2, 62, 40, r.getDrawable(R.raw.drum_bg)));
        rotating[5] = (UIRoundElement) addComponent(new UIDrum(1260, 947, 2, 62, 40, r.getDrawable(R.raw.drum_bg)));

        leftReel = (UITapeReel) rotating[0];
        rightReel = (UITapeReel) rotating[1];

        // CONNETTORI

        tapeChunks = new UIConnector[6];

        // bobina sx drum S sx
        tapeChunks[0] = (UIConnector) addComponent(new UIConnector(rotating[0], rotating[2], Side.LEFT_TO_LEFT));
        // drum S sx drum L sx
        tapeChunks[1] = (UIConnector) addComponent(new UIConnector(rotating[2], rotating[4], Side.RIGHT_TO_LEFT));
        // drum L sx punto fisso verso le testine
        tapeChunks[2] = (UIConnector) addComponent(new UIConnector(rotating[4], new UIPoint(590, 970, 2), Side.RIGHT_TO_LEFT));

        // bobina dx drum S dx
        tapeChunks[3] = (UIConnector) addComponent(new UIConnector(rotating[1], rotating[3], Side.RIGHT_TO_RIGHT));
        // drum S dx drum L dx
        tapeChunks[4] = (UIConnector) addComponent(new UIConnector(rotating[3], rotating[5], Side.LEFT_TO_RIGHT));
        // drum L dx punto fisso verso le testine
        tapeChunks[5] = (UIConnector) addComponent(new UIConnector(rotating[5], new UIPoint(1105, 970, 2), Side.LEFT_TO_RIGHT));

        // LCD
        lcd = (UILcd) addComponent(new UILcd(255, 1242, 3, 305, 85,
                r.getDrawable(R.raw.lcd)));

        // LED PLAYER
        controlLeds = new UILed[4];

        controlLeds[0] = (UILed) addComponent(new UILed(280, 1368, 3, 40, 40, r.getDrawable(R.raw.led_off), r.getDrawable(R.raw.led_on)));
        controlLeds[1] = (UILed) addComponent(new UILed(390, 1368, 3, 40, 40, r.getDrawable(R.raw.led_off), r.getDrawable(R.raw.led_on)));
        controlLeds[2] = (UILed) addComponent(new UILed(500, 1368, 3, 40, 40, r.getDrawable(R.raw.led_off), r.getDrawable(R.raw.led_on)));
        controlLeds[3] = (UILed) addComponent(new UILed(610, 1368, 3, 40, 40, r.getDrawable(R.raw.led_off), r.getDrawable(R.raw.led_on)));

        // PULSANTI PLAYER

        final UIButton[] controlButtons = new UIButton[5];

        controlButtons[0] = (UIButton) addComponent(new UIButton(250, 1412, 3, 97, 97, r.getDrawable(R.raw.btn_rew_up), r.getDrawable(R.raw.btn_rew_down)));
        controlButtons[1] = (UIButton) addComponent(new UIButton(360, 1412, 3, 97, 97, r.getDrawable(R.raw.btn_ff_up), r.getDrawable(R.raw.btn_ff_down)));
        controlButtons[2] = (UIButton) addComponent(new UIButton(470, 1412, 3, 97, 97, r.getDrawable(R.raw.btn_play_up), r.getDrawable(R.raw.btn_play_down)));
        controlButtons[3] = (UIButton) addComponent(new UIButton(580, 1412, 3, 97, 97, r.getDrawable(R.raw.btn_stop_up), r.getDrawable(R.raw.btn_stop_down)));
        controlButtons[4] = (UIButton) addComponent(new UIButton(579, 1235, 3, 97, 97, r.getDrawable(R.raw.btn_reset_up), r.getDrawable(R.raw.btn_reset_down)));

        // FAST REWIND: Callback del pulsante
        controlButtons[0].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if (controlButtons[0].isPressed())
                    player.startFastReverse();
            }
        });

        // FAST FORWARD: Callback del pulsante
        controlButtons[1].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if (controlButtons[1].isPressed())
                    player.startFastForward();
            }
        });

        // PLAY: Callback del pulsante
        controlButtons[2].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if (controlButtons[2].isPressed())
                    player.play();
            }
        });

        // STOP: Callback del pulsante
        controlButtons[3].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if (controlButtons[3].isPressed())
                    player.stop();
            }
        });

        // RESET: Callback del pulsante
        controlButtons[4].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if (controlButtons[4].isPressed()) {
                    referenceTimestamp = player.getCurrentTimestamp();
                    lcd.setTime(0);
                }
            }
        });

        // PULSANTI UI

        //UIButton[] uiButtons = new UIButton[5];
        final UIButton[] uiButtons = new UIButton[4];

        uiButtons[0] = (UIButton) addComponent(new UIButton(1854 - 75, 18, 4, 172, 172, r.getDrawable(R.raw.btn_settings_up), r.getDrawable(R.raw.btn_settings_down))); // -29px per l'ombra
        uiButtons[1] = (UIButton) addComponent(new UIButton(2074 - 75, 18, 4, 172, 172, r.getDrawable(R.raw.btn_library_up), r.getDrawable(R.raw.btn_library_down)));
        uiButtons[2] = (UIButton) addComponent(new UIButton(2294 - 75, 18, 4, 172, 172, r.getDrawable(R.raw.btn_setup_mon_up), r.getDrawable(R.raw.btn_setup_mon_down)));
        //uiButtons[3] = (UIButton) addComponent(new UIButton(2314 - 75, 18, 4, 172, 172, r.getDrawable(R.raw.btn_help_up), r.getDrawable(R.raw.btn_help_down)));
        //uiButtons[4] = (UIButton) addComponent(new UIButton(2464-75, 18, 4, 172, 172, r.getDrawable(R.raw.btn_whatsnew_up), r.getDrawable(R.raw.btn_whatsnew_down)));

        // SETTINGS: Callback del pulsante
        uiButtons[0].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if (uiButtons[0].isPressed())
                    context.startActivity(new Intent(context, SettingsActivity.class));
            }
        });

        // LIBRARY: Callback del pulsante
        uiButtons[1].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if (uiButtons[1].isPressed())
                    context.startActivity(new Intent(context, LibraryActivity.class));
            }
        });

        // SETUP MONITOR: Callback del pulsante
        uiButtons[2].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if (uiButtons[2].isPressed()) {
                    if (songLoaded)
                        context.startActivity(new Intent(context, MonitorSetupActivity.class));
                    else
                        Toast.makeText(context, context.getString(R.string.monitor_setup_unavailable), Toast.LENGTH_SHORT).show();
                }
            }
        });
        // WHAT'S NEW: Callback del pulsante
		/*uiButtons[4].setCallback(new ComponentCallback() {
			@Override
			public void stateChanged(UIComponent obj)
			{
					context.startActivity(new Intent(context, MainActivity.class));
			}
		});*/

        /*uiButtons[3].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj)
            {
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
                String file = "sample.pdf";

                Intent i = new Intent(context, PDFActivity.class);
                i.putExtra("path", path);
                i.putExtra("file", file);
                context.startActivity(i);
            }
        });*/

        //video offset UI


        int minus_x_pos = 1810, plus_x_pos = 2250, lcd_v_width = 250, y_pos = 320, width_heigth = 90;

        addComponent(new UIStatic(minus_x_pos - 14, y_pos - 10, 3, lcd_v_width + width_heigth * 2 + 160, width_heigth + 20, r.getDrawable(R.raw.background_video_offset)));

        lcdOffset = (UILcdCustom) addComponent(
                new UILcdCustom((minus_x_pos + width_heigth) + 49,
                        y_pos + 12, 3, lcd_v_width, (int) (width_heigth * 0.76),
                        r.getDrawable(R.raw.lcd), 6));

        final UIButton[] videoSyncButtons = new UIButton[2];

        videoSyncButtons[0] = (UIButton) addComponent(new UIButton(minus_x_pos+5,
                y_pos, 4, width_heigth, width_heigth,
                r.getDrawable(R.raw.btn_minus), r.getDrawable(R.raw.btn_minus)));
        videoSyncButtons[1] = (UIButton) addComponent(new UIButton(plus_x_pos+25,
                y_pos, 4, width_heigth, width_heigth,
                r.getDrawable(R.raw.btn_plus), r.getDrawable(R.raw.btn_plus)));

        videoSyncButtons[0].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if (songLoaded) {
                    if (videoSyncButtons[0].isPressed()) {
                        startIncrementing(false);
                    } else if (videoSyncButtons[0].isReleased()) {
                        stopIncrementing();
                    }
                } else
                    Toast.makeText(context, context.getString(R.string.monitor_setup_unavailable), Toast.LENGTH_SHORT).show();
            }
        });

        videoSyncButtons[1].setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if (songLoaded) {
                    if (videoSyncButtons[1].isPressed()) {
                        startIncrementing(true);
                    } else if (videoSyncButtons[1].isReleased()) {
                        stopIncrementing();
                    }

                } else
                    Toast.makeText(context, context.getString(R.string.monitor_setup_unavailable), Toast.LENGTH_SHORT).show();
            }
        });

        video_offset_increment = 0;
        threadRun = false;

        // MANOPOLE MAGNETOFONO
        speedKnob = (UIKnob) addComponent(new UIKnob(1331, 1296, 5, 40, r.getDrawable(R.raw.knob)));
        eqKnob = (UIKnob) addComponent(new UIKnob(1331, 1464, 5, 40, r.getDrawable(R.raw.knob)));

        speedKnob.setSteps(4, 135, 225);
        eqKnob.setSteps(2, 140, 220);

        // SPEED KNOB: Callback della manopola
        speedKnob.setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if(speedKnob.isPressed()) {
                    player.setPlayerSpeed(speed_values[speedKnob.getSelectedStep()]);

                    if (player.isPlaying())
                        updateRotatingElementSpeed(1);
                }
            }
        });

        // EQUALIZER KNOB: Callback della manopola
        eqKnob.setCallback(new ComponentCallback() {
            @Override
            public void stateChanged(UIComponent obj) {
                if(eqKnob.isPressed())
                player.setEqualization(eq_values[eqKnob.getSelectedStep()]);
            }
        });

        tapeKnob = (UILed) addComponent(new UILed(955, 1008, 1, 90, 110, r.getDrawable(R.raw.tape_knob_down), r.getDrawable(R.raw.tape_knob_up)));

        songLoaded = false;
        referenceTimestamp = 0;
    }

    private void startIncrementing(boolean forward) {
        video_offset_increment = VIDEO_OFFSET_STEP;

        Log.d("DEBUG", "startIncrementing");
        if (!forward)
            video_offset_increment = video_offset_increment * -1;
        setIsIncrementing(true);
        new Thread(new Runnable() {
            public void run() {

                Log.d("DEBUG", "THREAD run");
                int count = 0;
                while (isIncrementing()) {

                    Log.d("DEBUG", "while (isIncrementing())");
                    video_offset_increment = Math.min(video_offset_increment * 1.05f, 3000);
                    player.setVideoSyncOffset(player.getVideoSyncOffset() + video_offset_increment);
                    lcdOffset.setTime(player.getScaledTime(player.getVideoSyncOffset()));
                    if (count > 5) {
                        try {
                            sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            sleep(200);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    count++;
                }
            }
        }).start();
    }

    synchronized private void stopIncrementing() {
        Log.d("DEBUG", "stopIncrementing");
        video_offset_increment = 0;
        setIsIncrementing(false);
    }

    synchronized void setIsIncrementing(boolean newSetting) {
        threadRun = newSetting;
    }

    synchronized private boolean isIncrementing() {
        return threadRun;
    }

    @Override
    public boolean isPressed() {
        return false;
    }


    @Override
    public boolean isReleased() {
        return false;
    }

    @Override
    public Rect getVideoViewRect() {
        return new Rect(1808, 436, 1808 + 560, 436 + 420);
    }

    /**
     * Aggiorna la velocità di tutti gli oggetti rotanti in base alla velocità
     * di riproduzione, scalata di un fattore specificato
     *
     * @param multiplier Fattore moltiplicativo della velocità.
     */
    private void updateRotatingElementSpeed(float multiplier) {
        if (player.getCurrentSpeed() != null) {
            float s = -multiplier * Song.getFloatSpeed(player.getCurrentSpeed()) * 2.54f;

            leftReel.setLinearSpeed(s);
            rightReel.setLinearSpeed(s);
        }
    }

    private void setLeds(int index) {
        for (UILed led : controlLeds)
            led.setState(false);

        controlLeds[index].setState(true);
    }

    @Override
    public void onMusicPlay() {
        // Il nastro viagga a velocità normale
        updateRotatingElementSpeed(1);

        tapeKnob.setState(true);

        setLeds(2);
    }

    @Override
    public void onMusicStop() {
        // Il nastro è fermo
        updateRotatingElementSpeed(0);

        tapeKnob.setState(false);

        setLeds(3);
    }

    @Override
    public void onMusicFastForward() {
        // Il nastro viaggia veloce in avanti
        updateRotatingElementSpeed(player.getFastSpeedMultiplier());

        setLeds(1);
    }

    @Override
    public void onMusicFastReverse() {
        // Il nastro viaggia veloce all'indietro
        updateRotatingElementSpeed(-player.getFastSpeedMultiplier());

        setLeds(0);
    }

    @Override
    public void onSongChanged(Song newSong) {
        // L'evento viene richiamato anche quando vengono "rimosse le bobine"
        songLoaded = (newSong != null);

        // Se non c'è nessuna canzone, nascondi alcuni componenti
        leftReel.setVisibility(songLoaded);
        rightReel.setVisibility(songLoaded);

        for (UIConnector tape : tapeChunks)
            tape.setVisibility(songLoaded);

        // Resetta lo schermo
        lcd.setTime(0);

        if (songLoaded) {
            // Converti la velocità da in/s a cm/s
            float speed = newSong.getSpeed() * 2.54f;

            leftReel.setRecordingSpeed(speed);
            rightReel.setRecordingSpeed(speed);

            songDuration = newSong.getDuration();

            // Ripristina i valori di velocità ed equalizzazione a quelli
            // salvati

            for (int i = 0; i < speed_values.length; i++) {
                if (speed_values[i] == player.getCurrentSpeed()) {
                    speedKnob.setStep(i);
                    break;
                }
            }

            for (int i = 0; i < eq_values.length; i++) {
                if (eq_values[i] == player.getCurrentEqualization()) {
                    eqKnob.setStep(i);
                    break;
                }
            }
        } else
            updateRotatingElementSpeed(0);    // Ferma le bobine
    }

    @Override
    public void onSongProgress(float currentProgress, float currentTimestamp) {
        // Controllo se ho resettato il contatore
        if (referenceTimestamp != 0)
            currentProgress = player.getScaledTime(currentTimestamp - referenceTimestamp);

        // Imposto il display al tempo indicato
        lcd.setTime((int) Math.floor(currentProgress));

        // Aggiorno la quantità di nastro nelle bobine
        rightReel.setTapeTime(currentTimestamp);
        leftReel.setTapeTime(songDuration - currentTimestamp);
    }

    @Override
    public void onSongSpeedChanged(SongSpeed speed) {
        Log.d("StuderTapeDeck", "onSongSpeedChanged");
        for (int i = 0; i < speed_values.length; i++) {
            if (speed_values[i] == speed) {
                speedKnob.setStep(i);
                break;
            }
        }
        lcdOffset.setTime(player.getScaledTime(player.getVideoSyncOffset()));
    }
}
