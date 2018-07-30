package unipd.dei.magnetophone;

import unipd.dei.magnetophone.graphics.MagnetoCanvasView;
import unipd.dei.magnetophone.graphics.VideoView;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.support.v7.app.AppCompatActivity;

import static android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

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

		canvasView = (MagnetoCanvasView) findViewById(R.id.canvas);
		videoView = (VideoView) findViewById(R.id.video);

		canvasView.setVideoView(videoView);

		player = MusicPlayer.getInstance();

		startService(new Intent(this, MusicService.class));
		player.setContext(this);


		isStoragePermissionGranted();
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
}
