package unipd.dei.magnetophone.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.io.File;

import unipd.dei.magnetophone.R;
import unipd.dei.magnetophone.database.DatabaseManager;
import unipd.dei.magnetophone.utility.Song;

import static unipd.dei.magnetophone.utility.Utility.showSupportActionBar;

/**
 * Activity che realizza lo slide show delle foto della song a tutto schermo
 */

public class SlideShowActivity extends AppCompatActivity {

    private static Song slideSong;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide fragments representing
     * each object in a collection. We use a {@link android.support.v4.app.FragmentStatePagerAdapter}
     * derivative, which will destroy and re-create fragments as needed, saving and restoring their
     * state in the process. This is important to conserve memory and is a best practice when
     * allowing navigation between objects in a potentially large collection.
     */
    DemoCollectionPagerAdapter mDemoCollectionPagerAdapter;
    /**
     * The {@link android.support.v4.view.ViewPager} that will display the object collection.
     */
    ViewPager mViewPager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.slide_show);

        showSupportActionBar(this, null, getWindow().getDecorView());

        Intent myIntent = getIntent();
        int id = myIntent.getIntExtra("song_id", -1);
//        int index = myIntent.getIntExtra("photo_index", -1);
//        View photoView = findViewById(R.id.photoImage);

        slideSong = null;
        //prendo la canzone il cui id mi è stato passato
        if (id != -1) {
            slideSong = DatabaseManager.getSongFromDatabase(id, this);
        }

        mDemoCollectionPagerAdapter = new DemoCollectionPagerAdapter(getSupportFragmentManager());



        //TODO vedere se è possibile far vedere direttamente l'index desiderato

        // Specify that the Home button should show an "Up" caret, indicating that touching the
        // button will take the user one step up in the application's hierarchy.

        // Set up the ViewPager, attaching the adapter.
        mViewPager = findViewById(R.id.pager);
        mViewPager.setAdapter(mDemoCollectionPagerAdapter);
    }

    //chiamato quando qualche icona della action bar viene selezionata
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //http://developer.android.com/design/patterns/navigation.html#up-vs-back
            NavUtils.navigateUpTo(this, new Intent(this, LibraryActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link android.support.v4.app.FragmentStatePagerAdapter} that returns a fragment
     * representing an object in the collection.
     */
    public static class DemoCollectionPagerAdapter extends FragmentStatePagerAdapter {

        private DemoCollectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new DemoObjectFragment();
            Bundle args = new Bundle();
            args.putInt(DemoObjectFragment.ARG_OBJECT, i);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            //ritorno il numero di foto presenti nella cartella
            return slideSong.getPhotosFiles().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return "Photo: " + (position + 1) + slideSong.getPhotosFiles()[position].getName();
        }
    }

    /**
     * Un fragment che si occuperà di rivestire una posizione nello slide show, facendo comparire
     * una delle foto della cartella
     */
    public static class DemoObjectFragment extends Fragment {
        public static final String ARG_OBJECT = "object";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.single_photo_full_screen_layout, container, false);
            Bundle args = getArguments();
            int photoIndex=0;
            if(args!=null)
                photoIndex = args.getInt(ARG_OBJECT);//prendo l'indice della foto

            try {
                File photoFile = slideSong.getPhotosFiles()[photoIndex];

                if (photoFile.exists()) {
                    Bitmap myBitmap = BitmapFactory.decodeFile(photoFile.getAbsolutePath());

                    ImageView myImage = rootView.findViewById(R.id.photoImage);

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
            } catch (NullPointerException e) {
                Log.e("SlideShowActivity", "photos directory not available for this Song");
            }
            catch(java.lang.OutOfMemoryError e){
                e.printStackTrace();
                System.gc();
            }

            return rootView;
        }
    }
}
