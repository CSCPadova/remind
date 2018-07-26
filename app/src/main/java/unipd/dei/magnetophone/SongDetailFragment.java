package unipd.dei.magnetophone;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * A fragment representing a single Song detail screen. This fragment is either
 * contained in a {@link SongListActivity} in two-pane mode (on tablets) or a
 * {@link SongDetailActivity} on handsets.
 */
public class SongDetailFragment extends Fragment {
	/**
	 * Chiave che si usa tra activity e fragment per passare l'id della song da visualizzare nel dettaglio
	 */
	public static final String ARG_ITEM_ID = "item_id";

	private DemoCollectionPagerAdapter mDemoCollectionPagerAdapter;
	private ViewPager mViewPager;
	private TextView invalid;



	/**
	 * la canzone che il fragment rappresenta
	 */
	private static Song fragmentSong;

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public SongDetailFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//se siamo stati chiamati con la giusta chiave
		//getArguments ritorna il Bundle che ci è stato dato quando siamo nati nati, creati da un'activity, 
		//sempre che quell'activity abbia provveduto a darcene uno
		if (getArguments().containsKey(ARG_ITEM_ID)) 
		{
			//prendo la posizione dell'elemento scelto
			int id = getArguments().getInt(ARG_ITEM_ID);
			if(id==-1 )
				fragmentSong = null;
			else if(id>=0)//devo stare attento che non arrivino i valori -2 e -3, utilizzati per visualizzare
				//rispettivamente descrizione e layout di default
				fragmentSong = DatabaseManager.getSongFromDatabase(id, getActivity());
		}

	}//fine on create



	//metodo per creare l'aspetto del fragment
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) 
	{
		View rootView = null;
//		View photoView = null;
		if(getArguments().getInt(ARG_ITEM_ID)>=0)//necessario dettaglio della canzone selezionata dalla lista
		{
			rootView = inflater.inflate(R.layout.fragment_song_detail, container, false);
			fillTheDetail(fragmentSong, rootView);
		}
		else if(getArguments().getInt(ARG_ITEM_ID) == -2)//nel caso che sia -2 (chiamato il layout della descrizione)
		{
			rootView = inflater.inflate(R.layout.description_fragment_layout, container, false);	

			setTheDescriptionText(rootView);
		}
		else//se è == -3 (necessario il layout di default)
		{
			rootView = inflater.inflate(R.layout.detail_default_layout, container, false);
		}

		return rootView;
	}

	/**
	 * inserisce nel detail le informazioni della song selezionata dalla lista
	 * @param s
	 */
	private void fillTheDetail(Song s, View v)
	{
		if(s!=null)
		{

			((TextView) v.findViewById(R.id.song_signature)).setText(s.getSignature());
			((TextView) v.findViewById(R.id.my_signature)).setText(s.getSignature());
			((TextView) v.findViewById(R.id.info_provenance)).setText(s.getProvenance());
			((TextView) v.findViewById(R.id.info_equalization)).setText(s.getEqualization());
			((TextView) v.findViewById(R.id.info_speed)).setText(""+s.getSpeed());
			((TextView) v.findViewById(R.id.info_numberOfTacks)).setText(""+s.getNumberOfTracks());
			
			//a seconda del numero di tracce, rendo visibili o meno le view
			switch(s.getNumberOfTracks())
			{
				case 1:
					((TextView) v.findViewById(R.id.info_firstTrack)).setText(s.getTrackAtIndex(0).getName());
					((TextView) v.findViewById(R.id.info_firstTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_secondTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_secondTrack_subtex)).setVisibility(View.GONE);
					(v.findViewById(R.id.item_separator_track2)).setVisibility(View.GONE);

					((TextView) v.findViewById(R.id.info_thirdTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_thirdTrack_subtex)).setVisibility(View.GONE);
					(v.findViewById(R.id.item_separator_track3)).setVisibility(View.GONE);

					((TextView) v.findViewById(R.id.info_fourthTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_fourthTrack_subtex)).setVisibility(View.GONE);
					(v.findViewById(R.id.item_separator_track4)).setVisibility(View.GONE);
					break;
				case 2:
					((TextView) v.findViewById(R.id.info_firstTrack)).setText(s.getTrackAtIndex(0).getName());
					((TextView) v.findViewById(R.id.info_secondTrack)).setText(s.getTrackAtIndex(1).getName());
					((TextView) v.findViewById(R.id.info_firstTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_secondTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_secondTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_secondTrack_subtex)).setVisibility(View.GONE);
					(v.findViewById(R.id.item_separator_track2)).setVisibility(View.GONE);

					((TextView) v.findViewById(R.id.info_thirdTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_thirdTrack_subtex)).setVisibility(View.GONE);
					(v.findViewById(R.id.item_separator_track3)).setVisibility(View.GONE);

					((TextView) v.findViewById(R.id.info_fourthTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_fourthTrack_subtex)).setVisibility(View.GONE);
					( v.findViewById(R.id.item_separator_track4)).setVisibility(View.GONE);
					break;
				case 4:
					((TextView) v.findViewById(R.id.info_firstTrack)).setText(s.getTrackAtIndex(0).getName());
					((TextView) v.findViewById(R.id.info_secondTrack)).setText(s.getTrackAtIndex(1).getName());
					((TextView) v.findViewById(R.id.info_thirdTrack)).setText(s.getTrackAtIndex(2).getName());
					((TextView) v.findViewById(R.id.info_fourthTrack)).setText(s.getTrackAtIndex(3).getName());
					((TextView) v.findViewById(R.id.info_firstTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_secondTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_thirdTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_fourthTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_secondTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_secondTrack_subtex)).setVisibility(View.GONE);
					( v.findViewById(R.id.item_separator_track2)).setVisibility(View.GONE);

					((TextView) v.findViewById(R.id.info_thirdTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_thirdTrack_subtex)).setVisibility(View.GONE);
					( v.findViewById(R.id.item_separator_track3)).setVisibility(View.GONE);

					((TextView) v.findViewById(R.id.info_fourthTrack)).setVisibility(View.GONE);
					((TextView) v.findViewById(R.id.info_fourthTrack_subtex)).setVisibility(View.GONE);
					( v.findViewById(R.id.item_separator_track4)).setVisibility(View.GONE);
					break;
			}

			((TextView) v.findViewById(R.id.info_title)).setText(s.getTitle());
			((TextView) v.findViewById(R.id.info_title)).setVisibility(View.GONE);
			((TextView) v.findViewById(R.id.info_author)).setText(s.getAuthor());
			((TextView) v.findViewById(R.id.info_author)).setVisibility(View.GONE);
			((TextView) v.findViewById(R.id.info_year)).setText(s.getYear());
			((TextView) v.findViewById(R.id.info_year)).setVisibility(View.GONE);
			((TextView) v.findViewById(R.id.info_tapeWidth)).setText(s.getTapeWidth());
			((TextView) v.findViewById(R.id.info_tapeWidth)).setVisibility(View.GONE);
			((TextView) v.findViewById(R.id.info_sampleRate)).setText(""+s.getSampleRate());
			((TextView) v.findViewById(R.id.info_extension)).setText(s.getExtension());
			((TextView) v.findViewById(R.id.info_bitDepth)).setText(""+s.getBitDepth());
			((TextView) v.findViewById(R.id.info_video)).setText((s.isVideoValid()) ? s.getVideo().getName() : getString(R.string.video_not_available));
			((TextView) v.findViewById(R.id.info_video)).setVisibility(View.GONE);
			//faccio scomparire il bottone, in futuro rimuoverlo proprio dal layout
			((Button)v.findViewById(R.id.description_button)).setVisibility(View.GONE);

			TextView descriptionText = (TextView)v.findViewById(R.id.effective_description);

			if(s.getDescription()!=null)
			{
				descriptionText.setText(s.getDescription());
			}

			mViewPager = (ViewPager)v.findViewById(R.id.myPager);
			invalid =  (TextView)v.findViewById(R.id.info_photos_not_available);

			if(s.isPhotosValid())
			{
				//penso ad istanziale la ViewPager
				mDemoCollectionPagerAdapter = new DemoCollectionPagerAdapter(getActivity().getSupportFragmentManager());

				mViewPager.setAdapter(mDemoCollectionPagerAdapter);
				mViewPager.setVisibility(View.VISIBLE);

				invalid.setVisibility(View.GONE);
			}
			else
			{
				mViewPager.setVisibility(View.GONE);
				invalid.setVisibility(View.VISIBLE);
			}			
		}

	}

	//############# sessione per lo slide show ###############
	/**
	 * Un {@link android.support.v4.app.FragmentStatePagerAdapter} che ritorna un fragment rappresentante un oggetto
	 * della lista da visualizzare
	 */
	public static class DemoCollectionPagerAdapter extends FragmentStatePagerAdapter {

		public DemoCollectionPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			Fragment fragment = new DemoObjectFragment();
			Bundle args = new Bundle();
			args.putInt(DemoObjectFragment.ARG_OBJECT, i); // passo l'indice della foto che mi interessa visualizzare
			fragment.setArguments(args);
			return fragment;
		}

		@Override
		public int getCount() {
			//ritorno il numero di foto presenti nella cartella
			return SongDetailFragment.fragmentSong.getPhotosFiles().length;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return "Photo: " + (position + 1) + SongDetailFragment.fragmentSong.getPhotosFiles()[position].getName();
		}
	}

	/**
	 *Fragment che mostra una immagine
	 */
	public static class DemoObjectFragment extends Fragment {

		public static final String ARG_OBJECT = "object";

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) 
		{
			View rootView = inflater.inflate(R.layout.single_photo_layout, container, false);
			Bundle args = getArguments();
			int photoIndex = args.getInt(ARG_OBJECT);//prendo l'indice della foto

			//salvo il punto dove sono arrivato
//			SharedPreferences shared = getActivity().getSharedPreferences("selected", Context.MODE_PRIVATE);
//			Editor edit = shared.edit();
//			edit.putInt("photo_index", photoIndex);
//			edit.commit();

			try
			{
				File photoFile = fragmentSong.getPhotosFiles()[photoIndex];
				if(photoFile.exists()){

					Bitmap myBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

					ImageView myImage = (ImageView) rootView.findViewById(R.id.photoImage);
					
					//int height = myBitmap.getHeight(), width = myBitmap.getWidth();
					
//					######################################################
					//comressione delle foto, da tenere solo se necessario
					
//					if(height>4096 || width>4096)
//					{
//						BitmapFactory.Options options = new BitmapFactory.Options();
//				        options.inSampleSize = 4;
//				        
//				        Bitmap imgbitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath(), options);
//			            myImage.setImageBitmap(imgbitmap);
//						
//					}
//					else
//					{
						myImage.setImageBitmap(myBitmap);
//					}

				}
			}
			catch(NullPointerException e)
			{
				Log.d("SongDetailFragment", "photos directory not available for this Song");
			}

			return rootView;
		}
	}

	//#######################################################################################//

	/**
	 * inserisce la descrizione della canzone quando richiesto per mostrarla all'utente
	 */
	public void setTheDescriptionText(View v)
	{
		//prendo l'id dell'elemento selezionato nella lista
		SharedPreferences songPref = getActivity().getSharedPreferences("selected", Context.MODE_PRIVATE);
		int sId = songPref.getInt("song_id", -2);

		if(sId!=-2)
		{
			Song s = DatabaseManager.getSongFromDatabase(sId, getActivity());
			((TextView) v.findViewById(R.id.song_description)).setText((s.getDescription()==null || s.getDescription().equals("")) 
					? getString(R.string.description_not_available) : s.getDescription());
		}
	}

}
