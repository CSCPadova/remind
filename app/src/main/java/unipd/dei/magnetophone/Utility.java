package unipd.dei.magnetophone;

import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;

public class Utility {

    public static void showSupportActionBar(AppCompatActivity activity, String title, View decorView)
    {
        if(activity!=null) {
            if (title != null)
                activity.getSupportActionBar().setTitle(title);

            activity.getSupportActionBar().setDisplayShowTitleEnabled(true);
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            activity.getSupportActionBar().setDisplayShowCustomEnabled(true);
            activity.getSupportActionBar().setDisplayUseLogoEnabled(true);
            activity.getSupportActionBar().setIcon(R.mipmap.ic_launcher);


            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                            // Hide the nav bar and status bar
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);

            decorView.getViewTreeObserver().addOnWindowFocusChangeListener(new ViewTreeObserver.OnWindowFocusChangeListener() {
                @Override
                public void onWindowFocusChanged(final boolean hasFocus) {
                    showSupportActionBar(null, "", null);
                }
            });
        }
    }
}
