package unipd.dei.magnetophone;

import unipd.dei.magnetophone.graphics.MagnetoCanvasView;
import unipd.dei.magnetophone.graphics.VideoView;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.support.v7.app.AppCompatActivity;

/**
 * Activity principale dell'applicazione dove viene mostrato il magnetofono
 */
public class MagnetophoneActivity extends AppCompatActivity
{
	// Istanza della View su cui verrà disegnato il magnetofono
	private MagnetoCanvasView canvasView = null;
	private VideoView videoView = null;
	
	private MusicPlayer player;

	//private View decorView;
	
	/**
	 * onCreate dell'activity
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.d("MagnetophoneActivity", "oncreate MagnetophoneActivity");
		setContentView(R.layout.activity_magnetophone);

		//getActionBar().hide();
		//if(getSupportActionBar()!=null)
		//	getSupportActionBar().hide();


		canvasView = (MagnetoCanvasView) findViewById(R.id.canvas);
		videoView  =         (VideoView) findViewById(R.id.video);

		canvasView.setVideoView(videoView);

		player = MusicPlayer.getInstance();

		startService(new Intent(this, MusicService.class));
		player.setContext(this);
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
	
	public void onBackPressed(){
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
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN |
        			                                     View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
        			                                     View.SYSTEM_UI_FLAG_LOW_PROFILE );
        
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

	//@Override
	//public void onWindowFocusChanged(boolean hasFocus) {
	//	super.onWindowFocusChanged(hasFocus);
	//	// ogni volta che l'activity sarà in primo piano vengono reimpostati tutti i flag in modo
	//	// che l'app sia a schermo intero senza la barra di navigazione e senza la barra di stato
	//	if (hasFocus) {
	//		decorView.setSystemUiVisibility(
	//				View.SYSTEM_UI_FLAG_LAYOUT_STABLE
	//						| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	//						| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	//						| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	//						| View.SYSTEM_UI_FLAG_FULLSCREEN
	//						| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
	//	}
	//}
}
