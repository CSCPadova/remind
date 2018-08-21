package unipd.dei.magnetophone.utility;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import unipd.dei.magnetophone.R;

/**
 * Implementazione di un semplice ArrayAdapter di Song per la lista delle opere
 * in memoria che appare all'utente utilizzato da LibraryFragment
 */
public class CustomAdapterListFragment extends ArrayAdapter<Song> {
    // private Context m_cContext;

    /**
     * Costruttore dell'adapter, chiama il costruttore della classe padre
     *
     * @param context
     * @param textViewResourceId
     * @param objects
     */
    public CustomAdapterListFragment(Context context, int textViewResourceId, List<Song> objects) {
        super(context, textViewResourceId, objects);
        // this.m_cContext = context;
    }

    /**
     * sovrascrittura del metodo getView che chiamo il metodo ottimizzato che si
     * occuperà di creare il layout di ogni elemento che finirà nella lista di
     * brani
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // ViewHolder definita alla fine della classe, contiene references agli
        // elementi del layout
        ViewHolder viewHolder = null;

        if (convertView == null) // se siamo all'inizio della lista, dobbiamo preparare un po' tutto,
        {
            // creiamo l'inflater
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            // inseriamo il layout nella view tramite l'inflater, inserisce il
            // layout del singolo elemento della lista
            convertView = inflater.inflate(R.layout.element_of_the_fragment_list_layout, null);

            viewHolder = new ViewHolder();
            // riempiamo i campi del viewHolder con i riferimenti agli elementi
            // del layout
            viewHolder.signature = (TextView) convertView.findViewById(R.id.list_signature);
            viewHolder.duration = (TextView) convertView.findViewById(R.id.list_duration);

            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        Song song = getItem(position);

        // in questo punto si inseriscono gli elementi della view corrispondente
        // al singolo elemento della lista
        // qui si potrebbe giocare un po' per renderla più stilosa
        if (song != null) {
            viewHolder.signature.setText(song.getSignature());
            viewHolder.duration.setText(song.getDurationInMinutes());
        }

        return convertView;
    }

    /**
     * classe di comodo per una maggiore comodità nel grstire i campi di layout
     *
     * @author dennisdosso
     */
    private class ViewHolder {
        public TextView signature;
        public TextView duration;
    }
}
