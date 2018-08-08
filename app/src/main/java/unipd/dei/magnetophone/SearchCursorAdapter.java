package unipd.dei.magnetophone;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * un CursorAdapter scritto ad hoc per generare la lista dei risultati della ricerca dell'utente
 * in base alla signature
 *
 * @author dennisdosso
 */
public class SearchCursorAdapter extends CursorAdapter {
    //private LayoutInflater mInflater;

    public SearchCursorAdapter(Context context, Cursor c, int flag) {
        super(context, c, flag);
        /*mInflater = (LayoutInflater)*/
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(android.R.layout.two_line_list_item, parent, false);
        return v;
    }

    @Override
    public void bindView(View v, Context context, Cursor c) {

        String signature = c.getString(6);
        int id = c.getInt(0);

        TextView signature_text = (TextView) v.findViewById(android.R.id.text1);
        TextView id_text = (TextView) v.findViewById(android.R.id.text2);

        signature_text.setText(signature);
        id_text.setText("" + id);
    }


}
