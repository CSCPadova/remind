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
 * Implmentazione di un ArrayAdapter android per gestire ogni singolo elemento della lista dei brani
 * che viene gestito nella SongListActivity e SongListFragment. Appartenente alla prima versione del Magnetofono
 * da eliminare
 */
public class CustomAdapterList extends ArrayAdapter<Song> {
    /**
     * Costruttore dell'adapter, chiama il costruttore della classe padre
     *
     * @param context
     * @param textViewResourceId
     * @param objects
     */
    public CustomAdapterList(Context context, int textViewResourceId, List<Song> objects) {
        super(context, textViewResourceId, objects);
    }

    /**
     * sovrascrittura del metodo getView che chiamo il metodo ottimizzato
     * che si occuperà di creare il layout di ogni elemento che finirà nella lista di brani
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getViewOptimize(position, convertView, parent);
    }

    /**
     * metodo ottimizzato getView
     *
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    public View getViewOptimize(int position, View convertView, ViewGroup parent) {
        //ViewHolder definita alla fine della classe, contiene references agli elementi del layout
        ViewHolder viewHolder = null;

        if (convertView == null) //se siamo all'inizio della lista, dobbiamo preparare un po' tutto,
        {
            //creiamo l'inflater
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            //inseriamo il layout nella view tramite l'inflater, inserisce il layout del singolo elemento della lista
            convertView = inflater.inflate(R.layout.custom_layout, null);


            viewHolder = new ViewHolder();
            //riempiamo i campi del viewHolder con i riferimenti agli elementi del layout
            viewHolder.title = (TextView) convertView.findViewById(R.id.Title);
            viewHolder.author = (TextView) convertView.findViewById(R.id.Author);
            viewHolder.year = (TextView) convertView.findViewById(R.id.Year);

            convertView.setTag(viewHolder);
        } else
            viewHolder = (ViewHolder) convertView.getTag();

        Song song = getItem(position);

        //in questo punto si inseriscono gli elementi della view corrispondente al singolo elemento della lista
        //qui si potrebbe giocare un po' per renderla più stilosa
        //FIXME controllo sul fatto che non sia null la song
        if (song != null) {
            viewHolder.title.setText(song.getTitle());
            viewHolder.author.setText(song.getAuthor());
            viewHolder.year.setText(song.getYear());
        }

        return convertView;
    }

    private class ViewHolder {
        public TextView title;
        public TextView author;
        public TextView year;
    }

}
