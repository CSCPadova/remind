package unipd.dei.magnetophone;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import unipd.dei.magnetophone.graphics.MagnetoCanvasView;
import unipd.dei.magnetophone.graphics.VideoView;

import static unipd.dei.magnetophone.MusicService.EXT_STORAGE_EQU_FOLDER;

/**
 * Activity principale dell'applicazione dove viene mostrato il magnetofono
 */
public class MagnetophoneActivity extends AppCompatActivity {
    // Istanza della View su cui verrà disegnato il magnetofono
    private MagnetoCanvasView canvasView = null;
    private VideoView videoView = null;

    private MusicPlayer player;

    /**
     * onCreate dell'activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("MagnetophoneActivity", "oncreate MagnetophoneActivity");
        setContentView(R.layout.activity_magnetophone);

        fullscreen();
        isStoragePermissionGranted();
        createNotificationChannel();

        getSupportActionBar().hide();


        canvasView = (MagnetoCanvasView) findViewById(R.id.canvas);
        videoView = (VideoView) findViewById(R.id.video);

        canvasView.setVideoView(videoView);

        player = MusicPlayer.getInstance();

        startService(new Intent(this, MusicService.class));
        player.setContext(this);

        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        //SharedPreferences.Editor editor = sharedPref.edit();
        //editor.putBoolean("ImportareImpostazioni", true);
        //editor.commit();

        copyAssets();
    }

    /**
     * Salvo la velocità utilizzata nel caso in cui si richiami la stessa canzone
     */
    @Override
    public void onPause() {
        Log.d("MagnetophoneActivity", "onPause MagnetophoneActivity");
        canvasView.disableAnimation();

        //player.setVideoController(null);

        //Salvo i dati della riproduzione
        player.saveState();
        player.onPause();
        super.onPause();
    }

    public void onBackPressed() {
        player.onBackPressed();
        super.onBackPressed();
    }
	/*@Override
	public void onStop() {
		MusicPlayer.getInstance().stop();
		
		super.onStop();
	}*/

    /**
     * Chiamato quando il magnetofono viene richiamato dalla pausa, devo ricaricare i dati
     */
    @Override
    public void onResume() {
        super.onResume();

        Log.d("MagnetophoneActivity", "onResume MagnetophoneActivity");

        canvasView.enableAnimation();
        player.setVideoController(videoView);

        /*
         * Avverto il player di ricollegarsi al servizio per riprendere a funzionare
         * NOTA: lo metto in onResume() poichè se fosse in onStart(),
         * nel caso che l'activity fosse messa in pausa ma non stoppata,
         * non verrebbe fatto ripartire il player una volta tornata in primo piano, portando ad eventuali errori.
         * Vedi https://developer.android.com/training/basics/activity-lifecycle/starting.html
         */

        player.onResume();
        fullscreen();

		/*
		SharedPreferences preferences = getSharedPreferences("service", Context.MODE_PRIVATE);
		int songId = preferences.getInt("song_id", -1);
		
		// Se l'utente aveva gia'� selezionato una canzone
		if(songId != -1) {
			Song songFromDatabase = DatabaseManager.getSongFromDatabase(songId, this);
			if(songFromDatabase != null){
				player.setSong(songFromDatabase);
			}
		}*/
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v("DEBUG", "Permission is granted");
                return true;
            } else {

                Log.v("DEBUG", "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("DEBUG", "Permission is granted");
            return true;
        }
    }

    //copy i file wav di equalizzazione nell'external storage per facilitare la vita nel codice nativo
    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("equ");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }
        for (String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open("equ/" + filename);

                String outDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + EXT_STORAGE_EQU_FOLDER;

                File dir = new File(outDir);
                if (dir.mkdir()) {
                    System.out.println("Folder created");
                } else {
                    System.out.println("Folder is not created");
                }

                File outFile = new File(outDir, filename);

                if (!outFile.exists()) {
                    out = new FileOutputStream(outFile);
                    copyFile(in, out);
                    in.close();
                    in = null;
                    out.flush();
                    out.close();
                    out = null;
                }
            } catch (IOException e) {
                Log.e("tag", "Failed to copy asset file: " + filename, e);
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private void fullscreen() {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void createNotificationChannel() {
        String channelID = getResources().getString(R.string.CHANNEL_ID);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
