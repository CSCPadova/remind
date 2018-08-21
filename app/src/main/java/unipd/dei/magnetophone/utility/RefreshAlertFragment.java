package unipd.dei.magnetophone.utility;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import unipd.dei.magnetophone.R;

public class RefreshAlertFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle SavedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(R.string.title_alert_activity_single_song_settings);
        builder.setMessage(R.string.refresh_alert);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                //niente
            }
        });


        return builder.create();
    }
}