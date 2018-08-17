package unipd.dei.magnetophone;

import android.support.v7.app.AppCompatActivity;

public class Utility {

    public static void showSupportActionBar(AppCompatActivity activity, String title)
    {
        if(title!=null)
            activity.getSupportActionBar().setTitle(title);

        activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        activity.getSupportActionBar().setDisplayShowCustomEnabled(true);
        activity.getSupportActionBar().setDisplayUseLogoEnabled(true);
        activity.getSupportActionBar().setIcon(R.mipmap.ic_launcher);
    }
}
